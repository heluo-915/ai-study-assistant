package com.example.aistudyassistant;

import com.example.aistudyassistant.service.JwtService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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

// 在隔离 HTTP 端口使用真实 MySQL 和本地 Ollama；只创建并清理临时数据。
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PdfImportHttpTest {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtService jwtService;
    private String username;
    private String otherUsername;
    private Long userId;
    private Long otherUserId;

    // 配置当前测试所需的环境或依赖。
    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.deepseek.api-key", () -> "test-key-not-real");
    }

    // 创建供测试使用的用户。
    @BeforeEach void createUsers() {
        String prefix = "pdf_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        username = prefix + "a";
        otherUsername = prefix + "b";
        jdbcTemplate.update("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, "test-only");
        jdbcTemplate.update("INSERT INTO users (username, password_hash) VALUES (?, ?)", otherUsername, "test-only");
        userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
        otherUserId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, otherUsername);
    }

    // 清理本次测试创建的数据。
    @AfterEach void cleanup() {
        if (username != null) {
            jdbcTemplate.update("DELETE FROM notes WHERE user_id IN (SELECT id FROM users WHERE username IN (?, ?))", username, otherUsername);
            jdbcTemplate.update("DELETE FROM users WHERE username IN (?, ?)", username, otherUsername);
        }
    }

    // 构造内存 PDF，无需个人文件。
    private byte[] pdf(int pages, int lines) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage(); document.addPage(page);
                if (lines > 0) {
                    try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                        stream.beginText();
                        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                        stream.newLineAtOffset(40, 700);
                        for (int j = 0; j < lines; j++) {
                            stream.showText("Study notes explain Java interfaces and database transactions.");
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

    // 发送测试 PDF 导入请求。
    private HttpResponse<String> importPdf(String title, String filename, byte[] pdf, Long tokenUserId) throws Exception {
        String boundary = "TestBoundary" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        if (title != null) {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"title\"\r\n\r\n" + title + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + filename
                + "\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(pdf); body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/documents/pdf/import"))
                .timeout(Duration.ofSeconds(120)).header("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (tokenUserId != null) request.header("Authorization", "Bearer " + jwtService.generateToken(tokenUserId));
        return CLIENT.send(request.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    // 获取Note 字段。
    private HttpResponse<String> getNote(long id, Long tokenUserId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/notes/" + id))
                .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + jwtService.generateToken(tokenUserId)).GET().build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    // 验证接口要求登录。
    @Test void requiresLogin() throws Exception {
        assertEquals(401, importPdf("标题", "test.pdf", pdf(1, 1), null).statusCode());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notes WHERE user_id = ?", Integer.class, userId));
    }

    // 验证导入文本后按用户归属创建向量。
    @Test void importsTextAndCreatesVectorsWithOwnership() throws Exception {
        HttpResponse<String> response = importPdf("PDF 学习笔记", "notes.PDF", pdf(2, 1), userId);
        assertEquals(201, response.statusCode());
        var json = new JsonMapper().readTree(response.body());
        assertTrue(json.get("chunksGenerated").asBoolean());
        long noteId = json.get("noteId").asLong();
        String saved = jdbcTemplate.queryForObject("SELECT content FROM notes WHERE id = ? AND user_id = ?", String.class, noteId, userId);
        assertTrue(saved.contains("Java interfaces"));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM note_embeddings WHERE note_id = ? AND JSON_LENGTH(embedding) = 1024", Integer.class, noteId));
        assertTrue(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM note_chunks WHERE note_id = ? AND JSON_LENGTH(embedding) = 1024", Integer.class, noteId) > 0);
        assertEquals(404, getNote(noteId, otherUserId).statusCode());
    }

    // 验证无效输入被拒绝且不创建笔记。
    @Test void rejectsInvalidInputWithoutCreatingNotes() throws Exception {
        byte[] valid = pdf(1, 1);
        assertEquals(400, importPdf(null, "test.pdf", valid, userId).statusCode());
        assertEquals(400, importPdf(" ", "test.pdf", valid, userId).statusCode());
        assertEquals(400, importPdf("x".repeat(51), "test.pdf", valid, userId).statusCode());
        assertEquals(400, importPdf("标题", "test.txt", valid, userId).statusCode());
        assertEquals(400, importPdf("标题", "test.pdf", new byte[0], userId).statusCode());
        assertEquals(400, importPdf("标题", "test.pdf", "not a PDF".getBytes(StandardCharsets.UTF_8), userId).statusCode());
        assertEquals(400, importPdf("标题", "test.pdf", pdf(1, 0), userId).statusCode());
        assertEquals(400, importPdf("标题", "test.pdf", pdf(51, 0), userId).statusCode());
        assertEquals(400, importPdf("标题", "test.pdf", pdf(35, 8), userId).statusCode());
        assertEquals(413, importPdf("标题", "test.pdf", new byte[2 * 1024 * 1024 + 1], userId).statusCode());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notes WHERE user_id = ?", Integer.class, userId));
    }
}
