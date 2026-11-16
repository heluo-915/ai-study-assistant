package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.*;
import com.example.aistudyassistant.entity.Note;
import com.example.aistudyassistant.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.util.Optional;

@RestController
public class NoteController {
    private final NoteService noteService;
    private final ChatService chatService;
    private final NoteSearchAskService noteSearchAskService;
    private final NoteSemanticSearchService noteSemanticSearchService;
    private final NoteSemanticAskService noteSemanticAskService;
    private final NoteChunkBuildService noteChunkBuildService;
    private final NoteChunkSearchService noteChunkSearchService;
    private final NoteChunkAskService noteChunkAskService;
    private final NoteChunkBatchService noteChunkBatchService;
    private static final Logger log = LoggerFactory.getLogger(NoteController.class);
    private final NoteChunkPreviewService noteChunkPreviewService;
    private final NoteCreationService noteCreationService;
    // 通过构造方法接收并保存所需依赖或初始化字段。


    public NoteController(NoteService noteService, ChatService chatService, NoteSearchAskService noteSearchAskService, NoteSemanticSearchService noteSemanticSearchService, NoteSemanticAskService noteSemanticAskService, NoteChunkBuildService noteChunkBuildService, NoteChunkSearchService noteChunkSearchService, NoteChunkAskService noteChunkAskService, NoteChunkBatchService noteChunkBatchService, NoteChunkPreviewService noteChunkPreviewService, NoteCreationService noteCreationService) {
        this.noteService = noteService;
        this.chatService = chatService;
        this.noteSearchAskService = noteSearchAskService;
        this.noteSemanticSearchService = noteSemanticSearchService;
        this.noteSemanticAskService = noteSemanticAskService;
        this.noteChunkBuildService = noteChunkBuildService;
        this.noteChunkSearchService = noteChunkSearchService;
        this.noteChunkAskService = noteChunkAskService;
        this.noteChunkBatchService = noteChunkBatchService;
        this.noteChunkPreviewService = noteChunkPreviewService;
        this.noteCreationService = noteCreationService;
    }

    @PostMapping("/notes")
    // 校验标题和正文后为当前用户创建笔记。
    public ResponseEntity<?> create(@RequestBody CreateNoteRequest request, @RequestAttribute("userId") Long userId) {
        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body("标题不能为空");
        } else if (title.length() > 50) {
            return ResponseEntity.badRequest().body("标题长度不能大于50字符");
        }
        String content = request.getContent();
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body("正文不能为空");
        }
        CreateNoteResponse response =
                noteCreationService.create(userId, title, content);

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/notes")
    // 根据用户 ID 查询所属笔记。
    public List<Note> findByUserId(@RequestAttribute("userId") Long userId) {
        return noteService.findByUserId(userId);
    }

    @GetMapping("/notes/{noteId}")
    // 根据记录 ID 和用户 ID 联合查找，限制数据归属。
    public ResponseEntity<Note> findByIdAndUserId(@RequestAttribute("userId") Long userId, @PathVariable("noteId") Long noteId) {
        Note note = noteService.findByIdAndUserId(userId, noteId);
        if (note == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(note);
    }

    @DeleteMapping("/notes/{noteId}")
    // 按笔记 ID 和用户 ID 删除笔记。
    public ResponseEntity<Void> deleteByIdAndUserId(
            @RequestAttribute("userId") Long userId,
            @PathVariable("noteId") Long noteId
    ) {
        int rows = noteService.deleteByIdAndUserId(userId, noteId);

        if (rows == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    // 更新当前用户的笔记，并尝试重新生成片段。
    @PutMapping("/notes/{noteId}")
    public ResponseEntity<?> updateNoteByIdAndUserId(
            @RequestBody CreateNoteRequest request,
            @PathVariable("noteId") Long noteId,
            @RequestAttribute("userId") Long userId
    ) {
        String title = request.getTitle();
        String content = request.getContent();

        if (title == null || title.isBlank() || title.length() > 50
                || content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        int rows = noteService.updateNoteByIdAndUserId(
                title, content, noteId, userId
        );

        if (rows == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new UpdateNoteResponse(noteId, true, "笔记和片段更新成功"));
    }

    @PostMapping("/notes/{noteId}/ask")
    // 检查笔记归属后基于笔记正文回答问题。
    public ResponseEntity<NoteAnswerResponse> ask(
            @PathVariable("noteId") Long noteId,
            @RequestAttribute("userId") Long userId,
            @RequestBody ChatRequest request
    ) {
        String question = request.getMessage();

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Note note = noteService.findByIdAndUserId(userId, noteId);
        if (note == null) {
            return ResponseEntity.notFound().build();
        }
        String answer = chatService.askAboutNote(note.getContent(), question);
        NoteAnswerResponse response =new NoteAnswerResponse(answer, note.getId(), note.getTitle());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/notes/search")
    // 在当前用户笔记中按关键词匹配标题或正文。
    public ResponseEntity<List<Note>> searchByKeyword(
            @RequestAttribute("userId") Long userId,
            @RequestParam(value = "keyword", required = false) String keyword
    ){
        if(keyword==null||keyword.isBlank()){
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().body(noteService.searchByKeyword(userId, keyword));

    }

    @PostMapping("/notes/ask")
    // 检查关键词和问题，返回笔记检索问答结果及来源。
    public ResponseEntity<SearchAnswerResponse> askByKeyword(
            @RequestAttribute("userId") Long userId,
            @RequestBody SearchAskRequest request
    ) {
        String keyword = request.getKeyword();
        String question = request.getQuestion();

        if (keyword == null || keyword.isBlank()
                || question == null || question.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        SearchAnswerResponse response =
                noteSearchAskService.ask(userId, keyword, question);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/notes/semantic-search")
    public ResponseEntity<List<NoteSearchResult>> semanticSearch(
            @RequestAttribute("userId") Long userId,
            @RequestParam(value = "question", required = false) String question
    ) {
        // 1. question 为 null 或空白时，返回 400
        if(question==null||question.isBlank()){
            return ResponseEntity.badRequest().build();
        }
        // 2. 调用 noteSemanticSearchService.search(userId, question)
        List<NoteSearchResult> results=noteSemanticSearchService.search(userId,question);
        //    并以 200 返回结果
        return ResponseEntity.ok().body(results);
    }

    // 根据当前用户的相关笔记回答问题。
    @PostMapping("/notes/semantic-ask")
    public ResponseEntity<SearchAnswerResponse> askBySemantics(
            @RequestAttribute("userId") Long userId,
            @RequestBody ChatRequest request
    ) {
        String question = request.getMessage();

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        SearchAnswerResponse response =
                noteSemanticAskService.ask(userId, question);

        return ResponseEntity.ok(response);
    }

    // 为当前用户的指定笔记重新生成向量。
    @PostMapping("/notes/{noteId}/embedding")
    public ResponseEntity<Void> rebuildEmbedding(
            @RequestAttribute("userId") Long userId,
            @PathVariable("noteId") Long noteId
    ) {
        boolean success = noteService.rebuildEmbedding(userId, noteId);

        if (!success) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    // 为当前用户缺少向量的笔记批量生成向量。
    @PostMapping("/notes/embeddings/rebuild-missing")
    public ResponseEntity<BatchEmbeddingResult> rebuildMissingEmbeddings(
            @RequestAttribute("userId") Long userId
    ) {
        BatchEmbeddingResult result = noteService.rebuildMissingEmbeddings(userId);
        return ResponseEntity.ok(result);
    }

    // 接收并读取 UTF-8 文本文件。
    // 接收文本文件并创建笔记。
    @PostMapping("/notes/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestAttribute("userId") Long userId
    ) throws IOException {
        if (title.isBlank() || title.length() > 50) {
            return ResponseEntity.badRequest().body("标题不能为空且不能超过 50 个字符");
        }
        String fileName = file.getOriginalFilename();

        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            return ResponseEntity.badRequest().body("请上传 .txt 文件");
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件不能为空");
        }

        if (file.getSize() > 10 * 1024) {
            return ResponseEntity.badRequest().body("文件不能超过 10 KB");
        }

        String content;

        try {
            content = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(file.getBytes()))
                    .toString();
        } catch (CharacterCodingException exception) {
            return ResponseEntity.badRequest().body("文件必须是有效的 UTF-8 文本");
        }

        if (content.isBlank()) {
            return ResponseEntity.badRequest().body("文件内容不能为空");
        }

        CreateNoteResponse response =
                noteCreationService.create(userId, title, content);

        return ResponseEntity.status(201).body(response);
    }
    // 为当前用户的笔记首次生成片段。
    @PostMapping("/notes/{noteId}/chunks")
    public ResponseEntity<Void> buildChunks(
            @RequestAttribute("userId") Long userId,
            @PathVariable("noteId") Long noteId
    ) {
        boolean success = noteChunkBuildService.buildChunks(userId, noteId);

        if (!success) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
    // 搜索当前用户的笔记片段。
    @GetMapping("/notes/chunks/search")
    public ResponseEntity<List<ChunkSearchResult>> searchChunks(
            @RequestAttribute("userId") Long userId,
            @RequestParam(value = "question", required = false) String question
    ) {
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<ChunkSearchResult> results =
                noteChunkSearchService.search(userId, question);

        return ResponseEntity.ok(results);
    }
    // 根据当前用户的相关笔记片段回答问题。
    @PostMapping("/notes/chunks/ask")
    public ResponseEntity<ChunkAnswerResponse> askByChunks(
            @RequestAttribute("userId") Long userId,
            @RequestBody ChatRequest request
    ) {
        String question = request.getMessage();

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ChunkAnswerResponse response =
                noteChunkAskService.ask(userId, question);

        return ResponseEntity.ok(response);
    }

    // 为当前用户缺少片段的笔记批量生成片段。
    @PostMapping("/notes/chunks/build-missing")
    public ResponseEntity<BatchEmbeddingResult> buildMissingChunks(
            @RequestAttribute("userId") Long userId
    ) {
        BatchEmbeddingResult result =
                noteChunkBatchService.buildMissingChunks(userId);

        return ResponseEntity.ok(result);
    }
    // 查看当前用户指定笔记的片段。
    @GetMapping("/notes/{noteId}/chunks")
    public ResponseEntity<List<ChunkPreview>> previewChunks(
            @RequestAttribute("userId") Long userId,
            @PathVariable("noteId") Long noteId
    ) {
        Optional<List<ChunkPreview>> result =
                noteChunkPreviewService.findPreviews(userId, noteId);

        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result.get());
    }
}
