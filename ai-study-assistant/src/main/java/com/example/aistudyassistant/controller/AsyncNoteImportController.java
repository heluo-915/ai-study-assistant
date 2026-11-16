package com.example.aistudyassistant.controller;
import com.example.aistudyassistant.service.AsyncNoteImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.Locale;
@RestController
public class AsyncNoteImportController {
    private final AsyncNoteImportService service;
    public AsyncNoteImportController(AsyncNoteImportService service) { this.service=service; }
    @PostMapping("/notes/upload-async")
    public ResponseEntity<?> upload(@RequestAttribute("userId") Long userId,
            @RequestParam("title") String title,@RequestParam("file") MultipartFile file) throws IOException {
        String name=file.getOriginalFilename();
        if (title.isBlank() || title.length()>50 || name==null || !name.toLowerCase(Locale.ROOT).endsWith(".txt")
                || file.isEmpty() || file.getSize()>10*1024) return ResponseEntity.badRequest().body("标题或文本文件无效");
        String content;
        try { content=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(file.getBytes())).toString(); }
        catch (CharacterCodingException ex) { return ResponseEntity.badRequest().body("文件必须是 UTF-8 文本"); }
        if (content.isBlank()) return ResponseEntity.badRequest().body("文件内容不能为空");
        return ResponseEntity.accepted().body(service.create(userId,title,content));
    }
}
