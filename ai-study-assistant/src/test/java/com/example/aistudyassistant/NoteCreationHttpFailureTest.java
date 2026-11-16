package com.example.aistudyassistant;

import com.example.aistudyassistant.service.JwtService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

// 隔离应用使用随机 HTTP 端口；代理转发本地 Ollama，并按测试设置模拟 HTTP 故障。
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoteCreationHttpFailureTest {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private static final AtomicInteger EMBEDDING_REQUESTS = new AtomicInteger();
    private static volatile int failAt = Integer.MAX_VALUE;
    private static volatile boolean closeConnection;
    private static final HttpServer OLLAMA_PROXY = startProxy();

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtService jwtService;
    private String username;
    private Long userId;

    private static HttpServer startProxy() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", NoteCreationHttpFailureTest::proxy);
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("无法启动隔离测试代理", e);
        }
    }

    private static void proxy(HttpExchange exchange) throws IOException {
        try {
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            String path = exchange.getRequestURI().toString();
            if (path.startsWith("/api/embed") && closeConnection) {
                return;
            }
            if (path.startsWith("/api/embed") && EMBEDDING_REQUESTS.incrementAndGet() >= failAt) {
                byte[] failure = "{\"error\":\"simulated embedding failure\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, failure.length);
                exchange.getResponseBody().write(failure);
                return;
            }
            HttpRequest.Builder forward = HttpRequest.newBuilder(URI.create("http://127.0.0.1:11434" + path))
                    .timeout(Duration.ofSeconds(60))
                    .method(exchange.getRequestMethod(), HttpRequest.BodyPublishers.ofByteArray(requestBody));
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType != null) forward.header("Content-Type", contentType);
            HttpResponse<byte[]> result = CLIENT.send(forward.build(), HttpResponse.BodyHandlers.ofByteArray());
            result.headers().firstValue("Content-Type").ifPresent(v -> exchange.getResponseHeaders().set("Content-Type", v));
            exchange.sendResponseHeaders(result.statusCode(), result.body().length);
            exchange.getResponseBody().write(result.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } finally {
            exchange.close();
        }
    }

    @DynamicPropertySource
    static void configureOllama(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.ollama.base-url", () -> "http://127.0.0.1:" + OLLAMA_PROXY.getAddress().getPort());
    }

    @BeforeEach
    void createUser() {
        EMBEDDING_REQUESTS.set(0);
        failAt = Integer.MAX_VALUE;
        closeConnection = false;
        username = "http_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbcTemplate.update("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, "test-only");
        userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    @AfterEach
    void cleanup() {
        if (username != null) {
            jdbcTemplate.update("DELETE FROM notes WHERE user_id IN (SELECT id FROM users WHERE username = ?)", username);
            jdbcTemplate.update("DELETE FROM users WHERE username = ?", username);
        }
    }

    @AfterAll
    static void stopProxy() {
        OLLAMA_PROXY.stop(0);
    }

    private HttpResponse<String> createNote() throws Exception {
        String body = new JsonMapper().writeValueAsString(java.util.Map.of(
                "title", "普通创建测试", "content", "这是用于验证普通创建故障处理的笔记。"));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/notes"))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + jwtService.generateToken(userId))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    // 普通创建的片段阶段失败时返回部分成功，笔记和整篇向量保留。
    @Test void shouldKeepCreatedNoteWhenChunkGenerationFails() throws Exception {
        failAt = 2;
        HttpResponse<String> response = createNote();
        assertEquals(201, response.statusCode());
        var body = new JsonMapper().readTree(response.body());
        assertFalse(body.get("chunksGenerated").asBoolean());
        long noteId = body.get("noteId").asLong();
        assertTrue(EMBEDDING_REQUESTS.get() >= 2);
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notes WHERE id = ? AND user_id = ?", Integer.class, noteId, userId));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM note_embeddings WHERE note_id = ? AND JSON_LENGTH(embedding) = 1024", Integer.class, noteId));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM note_chunks WHERE note_id = ?", Integer.class, noteId));
    }

    // 普通创建的整篇向量失败时返回统一错误，笔记写入回滚。
    @Test void shouldRollbackWhenInitialEmbeddingFails() throws Exception {
        failAt = 1;
        HttpResponse<String> response = createNote();
        assertEquals(502, response.statusCode());
        var body = new JsonMapper().readTree(response.body());
        assertEquals("MODEL_SERVICE_ERROR", body.get("code").asString());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notes WHERE user_id = ?", Integer.class, userId));
    }
}
