package com.example.aistudyassistant.service;
import com.example.aistudyassistant.entity.Note;
import com.example.aistudyassistant.mapper.NoteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AsyncNoteImportService {
    private final NoteMapper notes;
    private final ChunkBuildJobService jobs;
    // 初始化 AsyncNoteImportService 实例。
    public AsyncNoteImportService(NoteMapper notes, ChunkBuildJobService jobs) { this.notes=notes; this.jobs=jobs; }
    public record ImportedNote(Long noteId, Long jobId) {}
    // 创建用户笔记和对应的异步构建任务。
    @Transactional
    public ImportedNote create(Long userId, String title, String content) {
        Note note=new Note(); note.setUserId(userId); note.setTitle(title); note.setContent(content);
        if (notes.insert(note)!=1 || note.getId()==null) throw new IllegalStateException("创建笔记失败");
        Long jobId=jobs.create(userId,note.getId());
        if (jobId==null) throw new IllegalStateException("创建任务失败");
        return new ImportedNote(note.getId(),jobId);
    }
}
