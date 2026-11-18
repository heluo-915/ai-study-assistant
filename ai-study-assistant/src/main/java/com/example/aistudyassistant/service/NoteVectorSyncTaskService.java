package com.example.aistudyassistant.service;

import com.example.aistudyassistant.dto.NoteVectorSyncTask;
import com.example.aistudyassistant.mapper.NoteVectorSyncTaskMapper;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;

@Service
public class NoteVectorSyncTaskService {
    private final NoteVectorSyncTaskMapper mapper;
    /* 保存同步任务的数据库访问对象。 */ public NoteVectorSyncTaskService(NoteVectorSyncTaskMapper mapper) { this.mapper=mapper; }
    // 在删除笔记之前安排 DELETE，供未来后台处理；不执行 Qdrant 操作。
    public boolean schedule(Long userId, Long noteId, Action action) {
        if (userId==null || noteId==null || action==null) throw new IllegalArgumentException("参数不能为空");
        return mapper.schedule(userId,noteId,action.name())>0;
    }
    /* 查询指定用户和笔记的同步任务状态。 */ public Optional<NoteVectorSyncTask> find(Long userId, Long noteId) {
        return Optional.ofNullable(mapper.find(userId,noteId));
    }
    /* 为当前用户有片段但缺少同步任务的笔记登记任务。 */ public void scheduleMissing(Long userId) {
        if (userId==null) throw new IllegalArgumentException("参数不能为空");
        mapper.scheduleMissing(userId);
    }
    // 操作前必须由调用方确认旧工作进程已经停止；不提供 HTTP 接口或自动超时恢复。
    public boolean recoverAfterWorkerStopped(Long userId, Long noteId, UUID expectedToken) {
        if (userId==null || noteId==null || expectedToken==null)
            throw new IllegalArgumentException("参数不能为空");
        return mapper.recoverInterrupted(userId,noteId,expectedToken.toString())>0;
    }
    public enum Action { UPSERT, DELETE }
}
