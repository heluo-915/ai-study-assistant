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
class PdfImportHttpFailureTest {
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
            server.createContext("/", PdfImportHttpFailureTest::proxy);
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

    // 构造或发送测试使用的 PDF 数据。
    private byte[] pdf() throws Exception {
        try (var document = new org.apache.pdfbox.pdmodel.PDDocument(); var output = new java.io.ByteArrayOutputStream()) {
            var page = new org.apache.pdfbox.pdmodel.PDPage();
            document.addPage(page);
            try (var stream = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(40, 700);
                stream.showText("PDF test content about Java interfaces.");
                stream.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    // 发送测试 PDF 导入请求。
    private HttpResponse<String> importPdf() throws Exception {
        String boundary = "TestBoundary" + UUID.randomUUID().toString().replace("-", "");
        var body = new java.io.ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"title\"\r\n\r\nPDF test\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.pdf\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(pdf());
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/documents/pdf/import"))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + jwtService.generateToken(userId))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    // PDF 导入的片段阶段失败时保留笔记与整篇向量，并返回部分成功。
    @Test void shouldKeepImportedNoteWhenChunkGenerationFails() throws Exception {
        failAt = 2;
        HttpResponse<String> response = importPdf();
        assertEquals(201, response.statusCode());
        var body = new JsonMapper().readTree(response.body());
        assertFalse(body.get("chunksGenerated").asBoolean());
        long noteId = body.get("noteId").asLong();
        assertTrue(EMBEDDING_REQUESTS.get() >= 2);
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notes WHERE id = ? AND user_id = ?", Integer.class, noteId, userId));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM note_embeddings WHERE note_id = ? AND JSON_LENGTH(embedding) = 1024", Integer.class, noteId));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM note_chunks WHERE note_id = ?", Integer.class, noteId));
    }

    // PDF 导入的整篇向量失败时应返回 502 并回滚笔记。
    @Test void shouldRollbackWhenInitialEmbeddingFails() throws Exception {
        failAt = 1;
        HttpResponse<String> response = importPdf();
        assertEquals(502, response.statusCode());
        var body = new JsonMapper().readTree(response.body());
        assertEquals("MODEL_SERVICE_ERROR", body.get("code").asString());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notes WHERE user_id = ?", Integer.class, userId));
    }
}
