> 本文记录了手动验证阶段。当前业务接入与运行方式请以 [Qdrant 接入与验证](Qdrant接入与验证.md) 为准。

# Qdrant 同步任务：基础记录

`note_vector_sync_tasks` 为每篇笔记记录一条任务。`requested_version` 表示最近请求版本，`claimed_version` 是处理者领取时的版本；`requested_action` 可为 UPSERT 或 DELETE。

- PENDING：等待处理。
- RUNNING：已被某个令牌领取；新请求仍可递增版本，但不会覆盖已领取版本。
- SUCCEEDED：所领取版本完成，且没有新请求。
- FAILED：当前版本失败，可以重新排队。

处理期间产生新版本时，当前任务结束后会重新进入 PENDING。结束任务必须匹配领取令牌；用户查询和状态操作都带 userId。DELETE 记录不设置对 notes 的级联外键，使笔记删除之后仍有机会清理向量。

`NoteVectorSyncTaskService.schedule` 只登记记录，当前并不执行向量写入或删除，也没有自动调度或 HTTP 接口。删除任务必须在笔记删除前通过所有权校验，并与删除操作放在同一个 MySQL 事务中；写入任务也应与相关业务变更处于同一事务。这里仅提供基础状态机制，尚未连接业务流程。

剩余工作包括：任务消费与异常恢复、领取超时的安全处理、Qdrant 部分失败重试、旧片段清理、并发写入顺序及搜索结果对账。现阶段不要将该记录状态视为 MySQL 与 Qdrant 已经一致。

已有数据库运行 `database/migrations/004_note_vector_sync_tasks.sql` 一次。新数据库先执行 `database/schema.sql`，再按编号执行 migrations。已执行过的迁移不要重复执行。
