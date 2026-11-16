package com.example.aistudyassistant;

import com.example.aistudyassistant.service.JwtService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

// 完整 HTTP 测试：本机模拟模型，不访问真实 DeepSeek 或数据库。
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DeepSeekRetryHttpTest {
    private static final AtomicInteger REQUESTS = new AtomicInteger();
    private static volatile int modelStatus;
    private static volatile long firstAttempt;
    private static volatile long secondAttempt;
    private static final HttpServer STUB = createStub();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @LocalServerPort private int port;
    @Autowired private JwtService jwtService;

    private static HttpServer createStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                try {
                    exchange.getRequestBody().readAllBytes();
                    int attempt = REQUESTS.incrementAndGet();
                    if (attempt == 1) firstAttempt = System.nanoTime();
                    if (attempt == 2) secondAttempt = System.nanoTime();
                    byte[] body = "{\"error\":{\"message\":\"TEST_INTERNAL_DIAGNOSTIC\",\"type\":\"test_error\"}}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(modelStatus, body.length);
                    exchange.getResponseBody().write(body);
                } finally {
                    exchange.close();
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("无法启动隔离模型服务", e);
        }
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.deepseek.base-url", () -> "http://127.0.0.1:" + STUB.getAddress().getPort());
        registry.add("spring.ai.deepseek.chat.base-url", () -> "http://127.0.0.1:" + STUB.getAddress().getPort());
        registry.add("spring.ai.deepseek.api-key", () -> "test-key-not-real");
        registry.add("app.jwt.secret", () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        registry.add("spring.datasource.password", () -> "test-unused");
        // 固定测试条件，避免依赖用户的实际配置。
        registry.add("spring.ai.retry.max-attempts", () -> 2);
        registry.add("spring.ai.retry.backoff.initial-interval", () -> "1s");
        registry.add("spring.ai.retry.backoff.multiplier", () -> 1);
    }

    @BeforeEach void reset() { REQUESTS.set(0); modelStatus = 400; firstAttempt = 0; secondAttempt = 0; }
    @AfterAll static void stopStub() { STUB.stop(0); }

    private HttpResponse<String> ask() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/chat"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + jwtService.generateToken(123L))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"测试问题\"}", StandardCharsets.UTF_8)).build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void assertGatewayError(HttpResponse<String> response) {
        assertEquals(502, response.statusCode());
        var json = new JsonMapper().readTree(response.body());
        assertEquals("MODEL_SERVICE_ERROR", json.get("code").asString());
        assertFalse(response.body().contains("TEST_INTERNAL_DIAGNOSTIC"));
    }

    // Spring AI 2.0.2：参数 2 代表首次调用之外再重试两次。
    @Test void retries503TwiceAfterInitialRequest() throws Exception {
        modelStatus = 503;
        assertGatewayError(ask());
        assertEquals(3, REQUESTS.get());
        assertTrue(secondAttempt - firstAttempt >= 700_000_000L);
    }

    // 默认不对 HTTP 400 客户端错误重试。
    @Test void doesNotRetry400() throws Exception {
        modelStatus = 400;
        assertGatewayError(ask());
        assertEquals(1, REQUESTS.get());
    }
}
