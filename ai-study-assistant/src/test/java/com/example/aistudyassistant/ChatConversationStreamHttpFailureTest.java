package com.example.aistudyassistant;

import com.example.aistudyassistant.service.JwtService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatConversationStreamHttpFailureTest {
    private static final AtomicInteger REQUESTS = new AtomicInteger();
    private static final CopyOnWriteArrayList<String> REQUEST_BODIES = new CopyOnWriteArrayList<>();
    private static volatile boolean truncate;
    private static volatile boolean cleanCloseWithoutFinish;
    private static volatile String finishReason;
    private static final HttpServer STUB = startStub();
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;
    private String username;
    private Long userId;
    private Long sessionId;

    // 启动用于模拟上游服务的测试桩。
    private static HttpServer startStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", ChatConversationStreamHttpFailureTest::respond);
            server.start();
            return server;
        } catch (IOException e) { throw new IllegalStateException(e); }
    }

    // 处理模拟服务收到的请求并返回响应。
    private static void respond(HttpExchange exchange) throws IOException {
        try {
            REQUEST_BODIES.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            REQUESTS.incrementAndGet();
            if (!truncate && !cleanCloseWithoutFinish) {
                byte[] body = "{\"error\":{\"message\":\"TEST_SECRET_DIAGNOSTIC\"}}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, body.length);
                exchange.getResponseBody().write(body);
            } else {
                byte[] chunk = ("data: {\"id\":\"test\",\"object\":\"chat.completion.chunk\",\"created\":1700000000,"
                        + "\"model\":\"test\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"partial\"},\"finish_reason\":null}]}\n\n")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                exchange.sendResponseHeaders(200, cleanCloseWithoutFinish ? 0 : 4096);
                exchange.getResponseBody().write(chunk);
                if (finishReason != null) {
                    String end = "data: {\"id\":\"test\",\"object\":\"chat.completion.chunk\",\"created\":1700000000,"
                            + "\"model\":\"test\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\""
                            + finishReason + "\"}]}\n\ndata: [DONE]\n\n";
                    exchange.getResponseBody().write(end.getBytes(StandardCharsets.UTF_8));
                }
                exchange.getResponseBody().flush();
            }
        } finally { exchange.close(); }
    }

    // 为测试提供动态配置属性。
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.ai.deepseek.base-url", () -> "http://127.0.0.1:" + STUB.getAddress().getPort());
        r.add("spring.ai.deepseek.chat.base-url", () -> "http://127.0.0.1:" + STUB.getAddress().getPort());
        r.add("spring.ai.deepseek.api-key", () -> "test-key-not-real");
        r.add("spring.ai.retry.max-attempts", () -> 1);
    }

    // 设置up 字段。
    @BeforeEach
    void setup() {
        truncate = false; cleanCloseWithoutFinish = false; finishReason = null; REQUESTS.set(0); REQUEST_BODIES.clear();
        username = "sse_http_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO users(username,password_hash) VALUES (?,?)", username, "test-only");
        userId = jdbc.queryForObject("SELECT id FROM users WHERE username=?", Long.class, username);
        jdbc.update("INSERT INTO chat_sessions(user_id,title) VALUES (?,?)", userId, "test");
        sessionId = jdbc.queryForObject("SELECT id FROM chat_sessions WHERE user_id=?", Long.class, userId);
    }

    // 清理本次测试创建的数据。
    @AfterEach
    void cleanup() {
        if (sessionId != null) {
            jdbc.update("DELETE FROM chat_messages WHERE session_id=?", sessionId);
            jdbc.update("DELETE FROM chat_sessions WHERE id=?", sessionId);
        }
        if (username != null) jdbc.update("DELETE FROM users WHERE username=?", username);
    }
    // 关闭测试服务并释放资源。
    @AfterAll static void stop() { STUB.stop(0); }

    // 向测试接口发送提问请求。
    private HttpResponse<String> ask() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/chat/sessions/" + sessionId + "/messages/stream"))
                .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + jwt.generateToken(userId))
                .header("Content-Type", "application/json").header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"question\"}"))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    // 验证请求的失败结果。
    private void assertFailure(HttpResponse<String> response) {
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("event:error"), response.body());
        assertFalse(response.body().contains("event:done"), response.body());
        assertFalse(response.body().contains("TEST_SECRET_DIAGNOSTIC"));
        assertTrue(REQUESTS.get() > 0);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM chat_messages WHERE session_id=?", Integer.class, sessionId));
    }

    // 验证上游 HTTP 错误时不会保存会话消息。
    @Test void upstreamHttpErrorDoesNotSave() throws Exception { assertFailure(ask()); }
    // 验证上游连接中断时不会保存会话消息。
    @Test void interruptedUpstreamDoesNotSave() throws Exception {
        truncate = true;
        var response = ask();
        assertFailure(response);
        assertTrue(response.body().contains("event:message"), response.body());
        assertTrue(response.body().contains("data:partial"), response.body());
    }

    // 验证缺少完成标记时不会保存会话消息。
    @Test void missingFinishMarkerDoesNotSave() throws Exception {
        cleanCloseWithoutFinish = true;
        var response = ask();
        assertFailure(response);
        assertTrue(response.body().contains("data:partial"), response.body());
    }

    // 验证输出达到模型限制时不会保存不完整回答。
    @Test void tokenLimitDoesNotSavePartialAnswer() throws Exception {
        cleanCloseWithoutFinish = true;
        finishReason = "length";
        assertFailure(ask());
    }

    // 验证正常完成时保存用户消息和助手消息。
    @Test void normalFinishSavesBothMessages() throws Exception {
        cleanCloseWithoutFinish = true;
        finishReason = "stop";
        var response = ask();
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("data:partial"), response.body());
        assertTrue(response.body().contains("event:done"), response.body());
        assertFalse(response.body().contains("event:error"), response.body());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM chat_messages WHERE session_id=?", Integer.class, sessionId));
    }

    // 验证第二次请求按正确顺序携带第一轮对话。
    @Test void secondRequestContainsFirstTurnInCorrectOrder() throws Exception {
        cleanCloseWithoutFinish = true;
        finishReason = "stop";
        assertTrue(ask().body().contains("event:done"));
        assertTrue(ask().body().contains("event:done"));
        assertEquals(2, REQUEST_BODIES.size());
        var request = new JsonMapper().readTree(REQUEST_BODIES.get(1));
        var messages = request.get("messages");
        assertEquals(3, messages.size());
        assertEquals("user", messages.get(0).get("role").asString());
        assertEquals("question", messages.get(0).get("content").asString());
        assertEquals("assistant", messages.get(1).get("role").asString());
        assertEquals("partial", messages.get(1).get("content").asString());
        assertEquals("user", messages.get(2).get("role").asString());
        assertEquals("question", messages.get(2).get("content").asString());
        assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM chat_messages WHERE session_id=?", Integer.class, sessionId));
    }
}
