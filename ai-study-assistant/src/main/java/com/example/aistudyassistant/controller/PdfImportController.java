package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.CreateNoteResponse;
import com.example.aistudyassistant.service.NoteCreationService;
import com.example.aistudyassistant.service.PdfTextExtractor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@RestController
public class PdfImportController {

    private static final long MAX_PDF_BYTES = 2L * 1024 * 1024;

    private final PdfTextExtractor pdfTextExtractor;
    private final NoteCreationService noteCreationService;

    // 初始化 PdfImportController 实例。
    public PdfImportController(
            PdfTextExtractor pdfTextExtractor,
            NoteCreationService noteCreationService
    ) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.noteCreationService = noteCreationService;
    }

    // 将当前用户上传的 PDF 文字导入为笔记。
    @PostMapping("/documents/pdf/import")
    public ResponseEntity<?> importPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestAttribute("userId") Long userId
    ) throws IOException {
        if (title.isBlank() || title.length() > 50) {
            return ResponseEntity.badRequest().body("标题不能为空且不能超过 50 个字符");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return ResponseEntity.badRequest().body("请上传 .pdf 文件");
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("PDF 文件不能为空");
        }

        if (file.getSize() > MAX_PDF_BYTES) {
            return ResponseEntity.badRequest().body("PDF 文件不能超过 2 MiB");
        }

        byte[] bytes = file.getBytes();
        String content;
        try {
            content = pdfTextExtractor.extractForImport(bytes);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        } catch (IOException exception) {
            return ResponseEntity.badRequest().body("无法读取 PDF，请检查文件是否损坏或需要密码");
        }

        CreateNoteResponse response = noteCreationService.create(userId, title, content);
        return ResponseEntity.status(201).body(response);
    }
}
