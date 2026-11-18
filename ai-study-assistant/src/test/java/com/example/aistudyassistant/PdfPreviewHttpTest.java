package com.example.aistudyassistant;

import com.example.aistudyassistant.service.JwtService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// 随机端口完整 HTTP 测试，PDF 数据在内存生成，不调用模型和数据库。
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PdfPreviewHttpTest {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    @LocalServerPort private int port;
    @Autowired private JwtService jwtService;

    // 配置当前测试所需的环境或依赖。
    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.deepseek.api-key", () -> "test-key-not-real");
        registry.add("app.jwt.secret", () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        registry.add("spring.datasource.password", () -> "test-unused");
    }

    // 构造或发送测试使用的 PDF 数据。
    private byte[] pdf(int pageCount, int linesPerPage) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (linesPerPage > 0) {
                    try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                        stream.beginText();
                        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                        stream.newLineAtOffset(40, 700);
                        for (int i = 0; i < linesPerPage; i++) {
                            stream.showText("Page " + (pageIndex + 1) + " learning note number " + i + ": Java interface.");
                            stream.newLineAtOffset(0, -14);
                        }
                        stream.endText();
                    }
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    // 发送测试 PDF 预览请求。
    private HttpResponse<String> preview(String filename, byte[] file, boolean authenticated) throws Exception {
        String boundary = "TestBoundary" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + filename
                + "\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(file);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/documents/pdf/preview"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (authenticated) request.header("Authorization", "Bearer " + jwtService.generateToken(123L));
        return CLIENT.send(request.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    // 验证接口要求身份认证。
    @Test void requiresAuthentication() throws Exception {
        assertEquals(401, preview("test.pdf", pdf(1, 1), false).statusCode());
    }

    // 验证提取 PDF 文本并限制预览长度。
    @Test void extractsTextAndCapsPreview() throws Exception {
        HttpResponse<String> response = preview("test.PDF", pdf(1, 35), true);
        assertEquals(200, response.statusCode());
        var json = new JsonMapper().readTree(response.body());
        String text = json.get("preview").asString();
        assertTrue(text.contains("Java interface"));
        assertEquals(1000, text.codePointCount(0, text.length()));
        assertTrue(json.get("totalCodePoints").asInt() > 1000);
        assertTrue(json.get("truncated").asBoolean());
    }

    // 验证较小的 PDF 返回未截断文本。
    @Test void smallPdfReturnsUntruncatedText() throws Exception {
        HttpResponse<String> response = preview("test.pdf", pdf(2, 1), true);
        assertEquals(200, response.statusCode());
        var json = new JsonMapper().readTree(response.body());
        String text = json.get("preview").asString();
        assertTrue(text.contains("Page 1"));
        assertTrue(text.contains("Page 2"));
        assertEquals(text.codePointCount(0, text.length()), json.get("totalCodePoints").asInt());
        assertFalse(json.get("truncated").asBoolean());
    }

    // 验证接口拒绝无效输入。
    @Test void rejectsInvalidInputs() throws Exception {
        assertEquals(400, preview("test.txt", pdf(1, 1), true).statusCode());
        assertEquals(400, preview("test.pdf", new byte[0], true).statusCode());
        assertEquals(400, preview("test.pdf", "not a PDF".getBytes(StandardCharsets.UTF_8), true).statusCode());
        assertEquals(400, preview("test.pdf", pdf(1, 0), true).statusCode());
        assertEquals(400, preview("test.pdf", pdf(51, 0), true).statusCode());
    }

    // 验证超大文件在上传处理层被拒绝。
    @Test void rejectsOversizeAtMultipartLayer() throws Exception {
        assertEquals(413, preview("test.pdf", new byte[2 * 1024 * 1024 + 1], true).statusCode());
    }
}
