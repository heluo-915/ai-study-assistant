package com.example.aistudyassistant;

import com.example.aistudyassistant.service.PdfTextExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PdfChineseExtractionTest {
    private final PdfTextExtractor extractor = new PdfTextExtractor();
    private static final File CHINESE_FONT = new File("C:/Windows/Fonts/simhei.ttf");

    // 构造测试使用的文本数据。
    private static void text(PDPageContentStream stream, PDType0Font font, float x, float y, String value) throws IOException {
        stream.beginText(); stream.setFont(font, 13); stream.newLineAtOffset(x, y); stream.showText(value); stream.endText();
    }

    // 使用本机字体生成两页合成中文 PDF；字体不存在时跳过。
    @Test void shouldExtractChineseFromMultiplePages() throws IOException {
        assumeTrue(CHINESE_FONT.isFile(), "系统测试字体不可用");
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType0Font font = PDType0Font.load(doc, CHINESE_FONT);
            PDPage first = new PDPage(); doc.addPage(first);
            try (PDPageContentStream stream = new PDPageContentStream(doc, first)) {
                text(stream, font, 40, 700, "青藤学习法：知识地图、自测和间隔复习。");
            }
            PDPage second = new PDPage(); doc.addPage(second);
            try (PDPageContentStream stream = new PDPageContentStream(doc, second)) {
                text(stream, font, 40, 700, "复习时应查看错误并记录疑问。");
            }
            doc.save(output);
            String result = extractor.extractText(output.toByteArray());
            assertTrue(result.contains("知识地图"));
            assertTrue(result.contains("自测"));
            assertTrue(result.contains("间隔复习"));
            assertTrue(result.indexOf("青藤学习法") < result.indexOf("记录疑问"));
        }
    }

    // 双栏都能提取，但不把当前读取顺序当作可靠的排版规则。
    @Test void shouldExtractBothColumnsWithoutAssumingOrder() throws IOException {
        assumeTrue(CHINESE_FONT.isFile(), "系统测试字体不可用");
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType0Font font = PDType0Font.load(doc, CHINESE_FONT);
            PDPage page = new PDPage(); doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                text(stream, font, 320, 700, "右栏内容：完成学习后复习。");
                text(stream, font, 40, 700, "左栏内容：先制定学习计划。");
            }
            doc.save(output);
            String result = extractor.extractText(output.toByteArray());
            assertTrue(result.contains("右栏内容"));
            assertTrue(result.contains("左栏内容"));
        }
    }

    // 仅含图片、没有文字层的 PDF 当前不会被当作可提取文本。
    @Test void shouldRejectImageOnlyPdf() throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(); doc.addPage(page);
            BufferedImage image = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            try { graphics.setColor(Color.BLACK); graphics.fillRect(0, 0, 100, 50); }
            finally { graphics.dispose(); }
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.drawImage(LosslessFactory.createFromImage(doc, image), 40, 500, 100, 50);
            }
            doc.save(output);
            assertThrows(IllegalArgumentException.class, () -> extractor.extractText(output.toByteArray()));
        }
    }
}
