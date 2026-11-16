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
class UpdateNoteHttpFailureTest {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private static final AtomicInteger EMBEDDING_REQUESTS = new AtomicInteger();
    private static volatile int failAt = Integer.MAX_VALUE;
    private static final HttpServer OLLAMA_PROXY = startProxy();

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtService jwtService;
    private String username;
    private Long userId;

    private static HttpServer startProxy() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", UpdateNoteHttpFailureTest::proxy);
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

    private HttpResponse<String> upload() throws Exception {
        String boundary = "TestBoundary" + UUID.randomUUID().toString().replace("-", "");
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"title\"\r\n\r\n上传事务测试\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n"
                + "Content-Type: text/plain; charset=utf-8\r\n\r\n这是用于验证上传故障处理的测试文本。\r\n"
                + "--" + boundary + "--\r\n";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/notes/upload"))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + jwtService.generateToken(userId))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> update(long noteId, String content) throws Exception {
        String body = new JsonMapper().writeValueAsString(java.util.Map.of("title", "更新测试", "content", content));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/notes/" + noteId))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + jwtService.generateToken(userId))
                .header("Content-Type", "application/json; charset=utf-8")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private long createUploadedNote() throws Exception {
        HttpResponse<String> response = upload();
        assertEquals(201, response.statusCode());
        var json = new JsonMapper().readTree(response.body());
        assertTrue(json.get("chunksGenerated").asBoolean());
        long noteId = json.get("noteId").asLong();
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM note_chunks WHERE note_id = ?", Integer.class, noteId));
        return noteId;
    }

    // 片段模型请求失败时，更新未提交，原正文和原片段都保留。
    @Test
    void shouldKeepOldContentAndChunksWhenNewChunkEmbeddingFails() throws Exception {
        long noteId = createUploadedNote();
        EMBEDDING_REQUESTS.set(0);
        failAt = 2;
        HttpResponse<String> response = update(noteId, "更新之后的新正文");
        assertEquals(502, response.statusCode());
        var error = new JsonMapper().readTree(response.body());
        assertEquals("MODEL_SERVICE_ERROR", error.get("code").asString());
        assertFalse(response.body().contains("simulated embedding failure"));
        assertEquals("这是用于验证上传故障处理的测试文本。", jdbcTemplate.queryForObject("SELECT content FROM notes WHERE id = ?", String.class, noteId));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM note_embeddings WHERE note_id = ? AND JSON_LENGTH(embedding) = 1024", Integer.class, noteId));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM note_chunks WHERE note_id = ?", Integer.class, noteId));
        assertTrue(EMBEDDING_REQUESTS.get() >= 2);
    }

    // 更新整篇向量失败：笔记内容与旧片段都应保持原状。
    @Test
    void shouldRollbackUpdateWhenInitialEmbeddingFails() throws Exception {
        long noteId = createUploadedNote();
        EMBEDDING_REQUESTS.set(0);
        failAt = 1;
        HttpResponse<String> response = update(noteId, "更新之后的新正文");
        assertEquals(502, response.statusCode());
        var error = new JsonMapper().readTree(response.body());
        assertEquals("MODEL_SERVICE_ERROR", error.get("code").asString());
        assertEquals("本地向量模型调用失败，请检查 Ollama 后重试", error.get("message").asString());
        assertFalse(response.body().contains("simulated embedding failure"));
        assertEquals("这是用于验证上传故障处理的测试文本。", jdbcTemplate.queryForObject("SELECT content FROM notes WHERE id = ?", String.class, noteId));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM note_chunks WHERE note_id = ?", Integer.class, noteId));
        assertTrue(EMBEDDING_REQUESTS.get() >= 1);
    }
}
