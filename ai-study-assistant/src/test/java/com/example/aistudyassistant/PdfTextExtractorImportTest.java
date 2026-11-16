package com.example.aistudyassistant;

import com.example.aistudyassistant.service.PdfTextExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PdfTextExtractorImportTest {
    private final PdfTextExtractor extractor = new PdfTextExtractor();

    // 构造内存 PDF，不读取用户文件。
    private byte[] pdf(int pages, int lines) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
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
            document.save(output);
            return output.toByteArray();
        }
    }

    // 使用真实 PDF 测试较短文本的导入提取。
    @Test void shouldExtractShortPdfForImport() throws IOException {
        String text = extractor.extractForImport(pdf(1, 1));
        assertTrue(text.contains("Java interfaces"));
    }

    // 使用多字节文字验证按 UTF-8 字节数判断，而非 Java 字符数量。
    @Test void shouldAcceptExactly10KiBUtf8() throws IOException {
        String text = "中".repeat(3413) + "a";
        assertEquals(10 * 1024, text.getBytes(StandardCharsets.UTF_8).length);
        PdfTextExtractor stub = new PdfTextExtractor() {
            @Override public String extractText(byte[] ignored) { return text; }
        };
        assertEquals(text, stub.extractForImport(new byte[]{1}));
    }

    // 超出一个字节时拒绝导入。
    @Test void shouldRejectMoreThan10KiBUtf8() {
        String text = "中".repeat(3413) + "ab";
        assertEquals(10 * 1024 + 1, text.getBytes(StandardCharsets.UTF_8).length);
        PdfTextExtractor stub = new PdfTextExtractor() {
            @Override public String extractText(byte[] ignored) { return text; }
        };
        assertThrows(IllegalArgumentException.class, () -> stub.extractForImport(new byte[]{1}));
    }

    // 长 PDF 可用于预览，但不能进入当前笔记导入流程。
    @Test void shouldAllowPreviewButRejectLargeImportText() throws IOException {
        byte[] bytes = pdf(35, 8);
        String previewText = extractor.extractText(bytes);
        assertTrue(previewText.getBytes(StandardCharsets.UTF_8).length > 10 * 1024);
        assertThrows(IllegalArgumentException.class, () -> extractor.extractForImport(bytes));
    }

    // 文件解析异常应传递给调用方，不能被当作空正文。
    @Test void shouldPropagateInvalidPdfError() {
        assertThrows(IOException.class, () -> extractor.extractForImport("not a PDF".getBytes(StandardCharsets.UTF_8)));
    }
}
