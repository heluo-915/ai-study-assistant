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
class UploadNoteHttpFailureTest {
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

    // 启动测试使用的代理服务。
    private static HttpServer startProxy() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", UploadNoteHttpFailureTest::proxy);
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("无法启动隔离测试代理", e);
        }
    }

    // 处理测试代理收到的请求。
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

    // 为测试配置模拟的 Ollama 服务地址。
    @DynamicPropertySource
    static void configureOllama(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.ollama.base-url", () -> "http://127.0.0.1:" + OLLAMA_PROXY.getAddress().getPort());
    }

    // 创建供测试使用的用户。
    @BeforeEach
    void createUser() {
        EMBEDDING_REQUESTS.set(0);
        failAt = Integer.MAX_VALUE;
        closeConnection = false;
        username = "http_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbcTemplate.update("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, "test-only");
        userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    // 清理本次测试创建的数据。
    @AfterEach
    void cleanup() {
        if (username != null) {
            jdbcTemplate.update("DELETE FROM notes WHERE user_id IN (SELECT id FROM users WHERE username = ?)", username);
            jdbcTemplate.update("DELETE FROM users WHERE username = ?", username);
        }
    }

    // 关闭测试代理服务。
    @AfterAll
    static void stopProxy() {
        OLLAMA_PROXY.stop(0);
    }

    // 发送测试使用的文件上传请求。
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

    // 整篇向量真实生成成功，片段向量请求通过测试代理返回 HTTP 400。
    @Test
    void shouldReturnCreatedWithPartialFailureOverHttp() throws Exception {
        failAt = 2;
        HttpResponse<String> response = upload();
        assertEquals(201, response.statusCode());
        var json = new JsonMapper().readTree(response.body());
        assertFalse(json.get("chunksGenerated").asBoolean());
        long noteId = json.get("noteId").asLong();
        assertTrue(EMBEDDING_REQUESTS.get() >= 2);
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notes WHERE id = ? AND user_id = ?", Integer.class, noteId, userId));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM note_embeddings WHERE note_id = ? AND JSON_LENGTH(embedding) = 1024", Integer.class, noteId));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM note_chunks WHERE note_id = ?", Integer.class, noteId));
    }

    // 第一阶段的向量请求失败时，HTTP 返回错误且笔记插入回滚。
    @Test
    void shouldRollbackNoteWhenInitialEmbeddingHttpFails() throws Exception {
        failAt = 1;
        HttpResponse<String> response = upload();
        assertEquals(502, response.statusCode());
        var error = new JsonMapper().readTree(response.body());
        assertEquals("MODEL_SERVICE_ERROR", error.get("code").asString());
        assertEquals("本地向量模型调用失败，请检查 Ollama 后重试", error.get("message").asString());
        assertFalse(response.body().contains("simulated embedding failure"));
        assertTrue(EMBEDDING_REQUESTS.get() >= 1);
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notes WHERE user_id = ?", Integer.class, userId));
    }

    // 本地代理提前关闭连接，模拟模型服务传输中断。
    @Test
    void shouldReturnGatewayErrorWhenOllamaConnectionCloses() throws Exception {
        closeConnection = true;
        HttpResponse<String> response = upload();
        assertEquals(502, response.statusCode());
        var error = new JsonMapper().readTree(response.body());
        assertEquals("MODEL_SERVICE_ERROR", error.get("code").asString());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notes WHERE user_id = ?", Integer.class, userId));
    }
}
