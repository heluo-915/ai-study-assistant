package com.example.aistudyassistant;
import com.example.aistudyassistant.service.ChunkBuildJobService;
import com.example.aistudyassistant.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="auth.default.integration.enabled",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class DefaultAuthenticationHttpTest {
    @LocalServerPort private int port;
    @Autowired private JwtService jwt;
    @MockitoBean private ChunkBuildJobService jobs;
    private final HttpClient client=HttpClient.newHttpClient();
    // 构造或发送测试请求。
    private HttpResponse<String> request(String path,String token) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET();
        if (token!=null) b.header("Authorization","Bearer "+token);
        return client.send(b.build(),HttpResponse.BodyHandlers.ofString());
    }
    // 验证公开路径仍可访问，未知路径默认受保护。
    @Test void publicPathsRemainPublicAndUnknownPathsAreProtected() throws Exception {
        assertEquals(200,request("/hello",null).statusCode());
        assertEquals(401,request("/notes",null).statusCode());
        assertEquals(401,request("/new-unregistered-path",null).statusCode());
        assertEquals(404,request("/new-unregistered-path",jwt.generateToken(1L)).statusCode());
    }
}
