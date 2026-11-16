package com.example.aistudyassistant.service;
import com.example.aistudyassistant.dto.ChunkBuildJob;
import com.example.aistudyassistant.mapper.ChunkBuildJobMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

@Service
public class ChunkBuildJobService {
    private static final Logger log=LoggerFactory.getLogger(ChunkBuildJobService.class);
    private final ChunkBuildJobMapper mapper;
    private final NoteChunkBuildService build;
    private final NoteVectorBuildService vectors;
    public ChunkBuildJobService(ChunkBuildJobMapper mapper, NoteChunkBuildService build, NoteVectorBuildService vectors) {
        this.mapper=mapper; this.build=build; this.vectors=vectors;
    }
    public Long create(Long userId, Long noteId) {
        Map<String,Object> params=new HashMap<>();
        params.put("userId",userId); params.put("noteId",noteId);
        if (mapper.create(params)==0) return null;
        return ((Number) params.get("id")).longValue();
    }
    public ChunkBuildJob find(Long userId, Long id) { return mapper.find(userId,id); }
    public boolean retry(Long userId, Long id) { return mapper.retry(userId,id)>0; }
    // 数据库存储任务；领取操作带状态条件，避免两个线程领取同一任务。
    @Scheduled(fixedDelayString="${app.chunk-jobs.poll-ms:5000}")
    public void processNext() {
        mapper.recoverExpired();
        Long id=mapper.nextId();
        if (id==null) return;
        String token=UUID.randomUUID().toString();
        if (mapper.claim(id,token)==0) return;
        long start=System.nanoTime();
        try {
            Long owner=mapper.owner(id,token), note=mapper.note(id,token);
            if (owner==null || note==null) throw new IllegalStateException("无法读取任务");
            vectors.build(owner,note);
            if (!build.buildChunks(owner,note)) throw new IllegalStateException("无法生成片段");
            mapper.finish(id,token,"SUCCEEDED");
            log.info("片段任务 {} 已完成，耗时 {} 毫秒", id, (System.nanoTime()-start)/1_000_000);
        } catch (RuntimeException error) {
            log.warn("片段任务 {} 执行失败：{}", id, error.getClass().getSimpleName());
            mapper.finish(id,token,"FAILED");
        }
    }
}
