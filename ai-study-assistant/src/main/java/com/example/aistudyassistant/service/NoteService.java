package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.BatchEmbeddingResult;
import com.example.aistudyassistant.entity.Note;
import com.example.aistudyassistant.mapper.NoteChunkMapper;
import com.example.aistudyassistant.mapper.NoteMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NoteService {
    private final NoteMapper noteMapper;
    private final NoteEmbeddingService noteEmbeddingService;
    private final NoteChunkMapper noteChunkMapper;
    private final NoteVectorSyncTaskService vectorSyncTasks;
    private final NoteUpdateService noteUpdateService;
    private static final Logger log = LoggerFactory.getLogger(NoteService.class);
    // 通过构造方法接收并保存所需依赖或初始化字段。

    public NoteService(NoteMapper noteMapper, NoteEmbeddingService noteEmbeddingService, NoteChunkMapper noteChunkMapper, NoteVectorSyncTaskService vectorSyncTasks, NoteUpdateService noteUpdateService) {
        this.noteMapper = noteMapper;
        this.noteEmbeddingService = noteEmbeddingService;
        this.noteChunkMapper = noteChunkMapper;
        this.vectorSyncTasks = vectorSyncTasks;
        this.noteUpdateService = noteUpdateService;
    }

    // 创建笔记和整篇向量，返回新笔记的 ID。
    @Transactional
    public Long create(String title, String content, Long userId) {
        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        note.setUserId(userId);

        int rows = noteMapper.insert(note);

        if (rows != 1 || note.getId() == null) {
            throw new IllegalStateException("笔记创建失败，未取得笔记 ID");
        }

        noteEmbeddingService.saveEmbedding(note.getId(), note.getContent());

        return note.getId();
    }
    // 根据用户 ID 查询所属笔记。
    public List<Note> findByUserId(Long userId){
        return noteMapper.findByUserId(userId);
    }
    // 根据记录 ID 和用户 ID 联合查找，限制数据归属。
    public Note findByIdAndUserId(Long userId,Long noteId){
        return noteMapper.findByIdAndUserId(userId, noteId);
    }
    // 按笔记 ID 和用户 ID 删除笔记。
    @Transactional
    public int deleteByIdAndUserId(Long userId,Long noteId){
        if (noteMapper.findByIdAndUserIdForUpdate(userId, noteId) == null) return 0;
        if (!vectorSyncTasks.schedule(userId, noteId, NoteVectorSyncTaskService.Action.DELETE))
            throw new IllegalStateException("无法登记向量删除任务");
        int rows = noteMapper.deleteByIdAndUserId(userId, noteId);
        if (rows != 1) throw new IllegalStateException("删除笔记失败");
        return rows;
    }
    // 按笔记 ID 和用户 ID 更新标题、正文及已有向量。
    public int updateNoteByIdAndUserId(
            String title, String content, Long noteId, Long userId
    ) {
        return noteUpdateService.update(userId, noteId, title, content);
    }
    // 在当前用户笔记中按关键词匹配标题或正文。
    public List<Note> searchByKeyword(Long userId ,String keyword){
        return noteMapper.searchByKeyword(userId, keyword);
    }

    // 为属于当前用户的笔记重新生成整篇向量。
    public boolean rebuildEmbedding(Long userId, Long noteId){
        Note note=findByIdAndUserId(userId, noteId);
        if(note==null){
            return false;
        }
        noteEmbeddingService.updateEmbedding(noteId,note.getContent());
        return true;
    }
    // 为当前用户缺少向量的笔记逐篇生成向量。
    public BatchEmbeddingResult rebuildMissingEmbeddings(Long userId) {
        List<Note> notes = noteMapper.findMissingEmbeddingByUserId(userId);

        int succeeded = 0;
        List<Long> failedNoteIds = new ArrayList<>();

        for (Note note : notes) {
            try {
                noteEmbeddingService.updateEmbedding(note.getId(), note.getContent());
                succeeded++;
            } catch (RuntimeException exception) {
                log.warn("笔记向量补建失败，笔记 ID：{}", note.getId(), exception);
                failedNoteIds.add(note.getId());
            }
        }

        return new BatchEmbeddingResult(
                notes.size(),
                succeeded,
                failedNoteIds.size(),
                failedNoteIds
        );
    }
}
