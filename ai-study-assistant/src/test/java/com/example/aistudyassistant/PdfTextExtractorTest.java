package com.example.aistudyassistant;

import com.example.aistudyassistant.service.PdfTextExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PdfTextExtractorTest {
    private final PdfTextExtractor extractor = new PdfTextExtractor();

    // 创建只存在于内存中的测试 PDF，不读取用户文件。
    private byte[] pdf(int pageCount, boolean addText, boolean passwordProtected) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int i = 0; i < pageCount; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (addText) {
                    try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                        stream.beginText();
                        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        stream.newLineAtOffset(40, 700);
                        stream.showText("Test page " + (i + 1));
                        stream.endText();
                    }
                }
            }
            if (passwordProtected) {
                StandardProtectionPolicy protection = new StandardProtectionPolicy("owner-password", "required-password", new AccessPermission());
                protection.setEncryptionKeyLength(128);
                document.protect(protection);
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    // 验证两页中的文字都能被提取。
    @Test void shouldExtractTextFromMultiplePages() throws IOException {
        String text = extractor.extractText(pdf(2, true, false));
        assertTrue(text.contains("Test page 1"));
        assertTrue(text.contains("Test page 2"));
    }

    // 验证缺少输入和超过大小限制的输入被拒绝。
    @Test void shouldRejectNullEmptyAndOversize() {
        assertThrows(IllegalArgumentException.class, () -> extractor.extractText(null));
        assertThrows(IllegalArgumentException.class, () -> extractor.extractText(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> extractor.extractText(new byte[2 * 1024 * 1024 + 1]));
    }

    // 验证无效文件不会被当成 PDF。
    @Test void shouldRejectInvalidPdf() {
        assertThrows(IOException.class, () -> extractor.extractText("not a PDF".getBytes(StandardCharsets.UTF_8)));
    }

    // 验证没有可提取文字的 PDF 被拒绝。
    @Test void shouldRejectPdfWithoutText() throws IOException {
        byte[] bytes = pdf(1, false, false);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> extractor.extractText(bytes));
        assertTrue(error.getMessage().contains("没有提取到文字"));
    }

    // 验证超出页数上限的 PDF 被拒绝。
    @Test void shouldRejectTooManyPages() throws IOException {
        byte[] bytes = pdf(51, false, false);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> extractor.extractText(bytes));
        assertTrue(error.getMessage().contains("50 页"));
    }

    // 验证需要打开密码的 PDF 不会被当作普通文档读取。
    @Test void shouldRejectPasswordProtectedPdf() throws IOException {
        byte[] bytes = pdf(1, true, true);
        assertThrows(IOException.class, () -> extractor.extractText(bytes));
    }
}
