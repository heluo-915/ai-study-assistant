package com.example.aistudyassistant.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

@Service
public class PdfTextExtractor {

    // 学习阶段的临时限制。
    private static final int MAX_PDF_BYTES = 2 * 1024 * 1024;
    private static final int MAX_PAGES = 50;
    private static final int MAX_IMPORT_TEXT_BYTES = 10 * 1024;

    // 从 PDF 字节中提取文本。
    public String extractText(byte[] pdfBytes) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF 文件不能为空");
        }

        if (pdfBytes.length > MAX_PDF_BYTES) {
            throw new IllegalArgumentException("PDF 文件不能超过 2 MiB");
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.getNumberOfPages() > MAX_PAGES) {
                throw new IllegalArgumentException("PDF 不能超过 50 页");
            }

            if (!document.getCurrentAccessPermission().canExtractContent()) {
                throw new IllegalArgumentException("该 PDF 不允许提取文本");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text.isBlank()) {
                throw new IllegalArgumentException(
                        "PDF 中没有提取到文字，可能是扫描件"
                );
            }

            return text;
        }
    }
    // 提取 PDF 文本，并检查是否适合当前的笔记导入流程。
    public String extractForImport(byte[] pdfBytes) throws IOException {
        String text = extractText(pdfBytes);

        int byteLength = text.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength > MAX_IMPORT_TEXT_BYTES) {
            throw new IllegalArgumentException("PDF 提取的文字不能超过 10 KiB");
        }

        return text;
    }

}