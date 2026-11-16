package com.example.aistudyassistant;

import com.example.aistudyassistant.service.EmbeddingService;
import com.example.aistudyassistant.service.JwtService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// 使用随机 HTTP 端口和本地模拟 DeepSeek，绝不调用真实云接口。
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DeepSeekHttpFailureTest {
    private static final AtomicInteger MODEL_REQUESTS = new AtomicInteger();
    private static volatile boolean closeConnection;
    private static volatile boolean returnSuccess;
    private static final HttpServer STUB = createStub();
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtService jwtService;
    @MockitoBean private EmbeddingService embeddingService;
    @Value("${spring.ai.ollama.embedding.model}") private String modelName;
    private String username;
    private Long userId;
    private Long sessionId;

    private static HttpServer createStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", DeepSeekHttpFailureTest::failRequest);
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("无法启动隔离模型服务", e);
        }
    }

    private static void failRequest(HttpExchange exchange) throws IOException {
        try {
            exchange.getRequestBody().readAllBytes();
            MODEL_REQUESTS.incrementAndGet();
            if (closeConnection) return;
            if (returnSuccess) {
                byte[] success = ("{\"id\":\"test-id\",\"object\":\"chat.completion\",\"created\":1700000000,\"model\":\"test-model\","
                        + "\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"测试回答\"},\"finish_reason\":\"stop\"}],"
                        + "\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":1,\"total_tokens\":2}}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, success.length);
                exchange.getResponseBody().write(success);
                return;
            }
            byte[] body = "{\"error\":{\"message\":\"TEST_UPSTREAM_DIAGNOSTIC\",\"type\":\"test_error\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, body.length);
            exchange.getResponseBody().write(body);
        } finally {
            exchange.close();
        }
    }

    @DynamicPropertySource
    static void configureStub(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.deepseek.base-url", () -> "http://127.0.0.1:" + STUB.getAddress().getPort());
        registry.add("spring.ai.deepseek.chat.base-url", () -> "http://127.0.0.1:" + STUB.getAddress().getPort());
        registry.add("spring.ai.deepseek.api-key", () -> "test-key-not-real");
        // 仅测试环境关闭长时间重试，避免连接故障测试等待过久。
        registry.add("spring.ai.retry.max-attempts", () -> 1);
    }

    @BeforeEach
    void createTestData() {
        MODEL_REQUESTS.set(0); closeConnection = false; returnSuccess = false;
        username = "ds_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbcTemplate.update("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, "test-only");
        userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
        jdbcTemplate.update("INSERT INTO chat_sessions (title, user_id) VALUES (?, ?)", "临时会话", userId);
        sessionId = jdbcTemplate.queryForObject("SELECT id FROM chat_sessions WHERE user_id = ?", Long.class, userId);
        jdbcTemplate.update("INSERT INTO notes (title, content, user_id) VALUES (?, ?, ?)", "临时笔记", "Java 接口定义行为", userId);
        Long noteId = jdbcTemplate.queryForObject("SELECT id FROM notes WHERE user_id = ?", Long.class, userId);
        jdbcTemplate.update("INSERT INTO note_chunks (note_id, chunk_index, content, embedding, model_name) VALUES (?, ?, ?, ?, ?)",
                noteId, 0, "Java 接口定义行为", "[1.0]", modelName);
        when(embeddingService.embed(anyString())).thenReturn(new float[]{1.0f});
        when(embeddingService.cosineSimilarity(any(float[].class), any(float[].class))).thenReturn(1.0);
    }

    @AfterEach
    void cleanup() {
        if (username != null) {
            jdbcTemplate.update("DELETE FROM chat_messages WHERE session_id IN (SELECT id FROM chat_sessions WHERE user_id IN (SELECT id FROM users WHERE username = ?))", username);
            jdbcTemplate.update("DELETE FROM chat_sessions WHERE user_id IN (SELECT id FROM users WHERE username = ?)", username);
            jdbcTemplate.update("DELETE FROM notes WHERE user_id IN (SELECT id FROM users WHERE username = ?)", username);
            jdbcTemplate.update("DELETE FROM users WHERE username = ?", username);
        }
    }

    @AfterAll static void stopStub() { STUB.stop(0); }

    private HttpResponse<String> post(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + jwtService.generateToken(userId))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"Java 接口是什么？\"}", StandardCharsets.UTF_8))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void assertModelFailure(HttpResponse<String> response) {
        assertEquals(502, response.statusCode());
        var json = new JsonMapper().readTree(response.body());
        assertEquals("MODEL_SERVICE_ERROR", json.get("code").asString());
        assertEquals("对话模型调用失败，请检查服务配置或稍后重试", json.get("message").asString());
        assertFalse(response.body().contains("TEST_UPSTREAM_DIAGNOSTIC"));
        assertTrue(MODEL_REQUESTS.get() > 0, "请求必须到达本地模拟模型服务");
    }

    @Test void singleChatShouldReportModelFailure() throws Exception {
        assertModelFailure(post("/chat"));
    }

    @Test void conversationShouldNotSaveMessagesAfterModelFailure() throws Exception {
        assertModelFailure(post("/chat/sessions/" + sessionId + "/messages"));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chat_messages WHERE session_id = ?", Integer.class, sessionId));
    }

    @Test void chunkAskShouldReportModelFailure() throws Exception {
        assertModelFailure(post("/notes/chunks/ask"));
    }

    @Test void closedModelConnectionShouldReportModelFailure() throws Exception {
        closeConnection = true;
        assertModelFailure(post("/chat"));
    }

    // 重构单轮调用后，正常模型回答仍能返回。
    @Test void singleChatShouldReturnSuccessfulAnswer() throws Exception {
        returnSuccess = true;
        HttpResponse<String> response = post("/chat");
        assertEquals(200, response.statusCode());
        assertEquals("测试回答", response.body());
        assertTrue(MODEL_REQUESTS.get() > 0);
    }

    // 正常模型回答后，会话应保存用户问题和助手回答。
    @Test void conversationShouldSaveSuccessfulTurn() throws Exception {
        returnSuccess = true;
        HttpResponse<String> response = post("/chat/sessions/" + sessionId + "/messages");
        assertEquals(200, response.statusCode());
        assertEquals("测试回答", response.body());
        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chat_messages WHERE session_id = ?", Integer.class, sessionId));
    }

    // 正常片段问答应返回模型答案与原始笔记来源。
    @Test void chunkAskShouldReturnSuccessfulAnswerAndSource() throws Exception {
        returnSuccess = true;
        HttpResponse<String> response = post("/notes/chunks/ask");
        assertEquals(200, response.statusCode());
        var json = new JsonMapper().readTree(response.body());
        assertEquals("测试回答", json.get("answer").asString());
        assertEquals("临时笔记", json.get("sources").get(0).get("noteTitle").asString());
        assertEquals(0, json.get("sources").get(0).get("chunkIndex").asInt());
    }
}
