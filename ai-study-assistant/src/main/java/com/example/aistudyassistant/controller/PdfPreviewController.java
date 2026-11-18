package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.PdfPreviewResponse;
import com.example.aistudyassistant.service.PdfTextExtractor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@RestController
public class PdfPreviewController {

    private static final long MAX_PDF_BYTES = 2L * 1024 * 1024;
    private static final int MAX_PREVIEW_CODE_POINTS = 1000;

    private final PdfTextExtractor pdfTextExtractor;

    // 初始化 PdfPreviewController 实例。
    public PdfPreviewController(PdfTextExtractor pdfTextExtractor) {
        this.pdfTextExtractor = pdfTextExtractor;
    }

    // 提取 PDF 文字并返回前 1000 个码点作为预览。
    @PostMapping("/documents/pdf/preview")
    public ResponseEntity<?> preview(@RequestParam("file") MultipartFile file)
            throws IOException {
        String fileName = file.getOriginalFilename();

        if (fileName == null
                || !fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return ResponseEntity.badRequest().body("请上传 .pdf 文件");
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("PDF 文件不能为空");
        }

        if (file.getSize() > MAX_PDF_BYTES) {
            return ResponseEntity.badRequest().body("PDF 文件不能超过 2 MiB");
        }

        // 在大小检查通过后才读取文件内容。
        byte[] bytes = file.getBytes();

        try {
            String text = pdfTextExtractor.extractText(bytes);
            int total = text.codePointCount(0, text.length());
            int previewLength = Math.min(total, MAX_PREVIEW_CODE_POINTS);
            int endIndex = text.offsetByCodePoints(0, previewLength);

            return ResponseEntity.ok(new PdfPreviewResponse(
                    text.substring(0, endIndex),
                    total,
                    total > MAX_PREVIEW_CODE_POINTS
            ));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        } catch (IOException exception) {
            return ResponseEntity.badRequest()
                    .body("无法读取 PDF，请检查文件是否损坏或需要密码");
        }
    }
}