package com.example.aistudyassistant;

import com.example.aistudyassistant.service.JwtService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
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

// 测试同步 DeepSeek 客户端的读取超时；使用本地模拟服务，不访问真实 DeepSeek。
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DeepSeekReadTimeoutHttpTest {
    private static final AtomicInteger REQUESTS = new AtomicInteger();
    private static final HttpServer STUB = createStub();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    @LocalServerPort private int port;
    @Autowired private JwtService jwtService;

    // 启动用于模拟上游服务的测试桩。
    private static HttpServer createStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                try {
                    exchange.getRequestBody().readAllBytes();
                    REQUESTS.incrementAndGet();
                    Thread.sleep(4000);
                    byte[] body = ("{\"id\":\"test\",\"object\":\"chat.completion\",\"created\":1700000000,\"model\":\"test\","
                            + "\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"不应收到\"},\"finish_reason\":\"stop\"}]}")
                            .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (IOException ignored) {
                    // 测试客户端超时后可能关闭连接。
                } finally {
                    exchange.close();
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("无法启动延迟响应测试服务", e);
        }
    }

    // 配置当前测试所需的环境或依赖。
    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.deepseek.base-url", () -> "http://127.0.0.1:" + STUB.getAddress().getPort());
        registry.add("spring.ai.deepseek.chat.base-url", () -> "http://127.0.0.1:" + STUB.getAddress().getPort());
        registry.add("spring.ai.deepseek.api-key", () -> "test-key-not-real");
        registry.add("app.jwt.secret", () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        registry.add("spring.datasource.password", () -> "test-unused");
        // 缩短测试超时并关闭重试，以单独验证一次请求的读取超时。
        registry.add("spring.http.clients.connect-timeout", () -> "1s");
        registry.add("spring.http.clients.read-timeout", () -> "1s");
        registry.add("spring.ai.retry.max-attempts", () -> 0);
    }

    // 关闭测试桩并释放资源。
    @AfterAll static void stopStub() { STUB.stop(0); }

    // 验证模型响应超时时返回网关错误。
    @Test
    void slowDeepSeekResponseShouldReturnGatewayError() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/chat"))
                .timeout(Duration.ofSeconds(12))
                .header("Authorization", "Bearer " + jwtService.generateToken(123L))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString("{\"message\":\"测试问题\"}", StandardCharsets.UTF_8))
                .build();

        long started = System.nanoTime();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        long elapsedMs = Duration.ofNanos(System.nanoTime() - started).toMillis();

        assertEquals(502, response.statusCode());
        assertEquals("MODEL_SERVICE_ERROR", new JsonMapper().readTree(response.body()).get("code").asString());
        assertTrue(REQUESTS.get() >= 1, "请求必须到达模拟模型服务");
        assertTrue(elapsedMs >= 500 && elapsedMs < 3500, "等待应接近测试配置的超时，而不是等到模拟服务正常回答：" + elapsedMs + "ms");
    }
}
