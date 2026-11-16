# AI 学习助手（后端第一版）

这是一个用于学习 Java 后端和 AI 应用开发的单体项目。它支持用户登录、笔记与资料导入、基于资料的问答、单选练习、错题记录，以及流式会话。前端不属于本次后端交付范围。

## 技术与数据流

- Java 21、Spring Boot 4.1.2、Spring AI 2.0.2、MyBatis、MySQL 8、JWT。
- DeepSeek 负责回答和出题；本地 Ollama 的 `qwen3-embedding:0.6b` 负责生成文本向量。
- MySQL 保存笔记、片段和向量，是内容与归属的权威来源；Qdrant 保存片段检索索引。正文切分后生成向量，检索结果作为回答的参考资料。

```text
TXT/PDF 或笔记 → 正文切分 → Ollama 向量化 → MySQL → 同步任务 → Qdrant
用户问题 → Qdrant 检索并核验（异常时回退 MySQL）→ DeepSeek → 回答和参考来源
笔记 → DeepSeek 出单选题 → 作答 → 作答记录和错题
```

## 运行环境

1. 安装 JDK 21、MySQL 8、Maven 3.9.x、Ollama 和 Qdrant。默认 MySQL 地址是 `localhost:3307`；如不相同，修改 `src/main/resources/application.yml` 的 `spring.datasource.url`，或设置 `SPRING_DATASOURCE_URL`。
2. 启动 Ollama，确认 `ollama list` 中有 `qwen3-embedding:0.6b`，服务地址为 `http://localhost:11434`。没有时可运行 `ollama pull qwen3-embedding:0.6b`。
3. 如需将模型放到非 C 盘，在启动 Ollama **之前**设置 `OLLAMA_MODELS` 为模型目录，再重启 Ollama；设置路径要使用你自己的目录。应用只连接 Ollama，不负责启动它。
4. 准备可用的 DeepSeek API 密钥；模型请求会联网并可能产生费用。上传资料用于问答时，所选笔记内容或检索片段会发送给模型服务。

## 初始化空数据库

**只在新建的空数据库执行一次。不要在已有项目数据库中重复执行建表脚本。**

在 MySQL 客户端中创建并选中数据库：

```sql
CREATE DATABASE ai_study_assistant
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE ai_study_assistant;
```

然后执行 [`database/schema.sql`](database/schema.sql)。脚本只创建第一版的 8 张表及索引、外键，不创建数据库、不删除已有数据，也不包含测试账号。随后还需按顺序执行 `database/migrations/002_study_tasks.sql`、`003_chunk_build_jobs.sql`、`004_note_vector_sync_tasks.sql` 和 `005_note_vector_sync_retry.sql`。已有数据库不需要重建第一版的表；升级前先备份，再执行尚未执行过的迁移。

如使用其他数据库名，可设置例如：

```text
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/你的数据库名
```

默认数据库用户名是 `root`；若使用其他账号，修改 YAML 或设置 `SPRING_DATASOURCE_USERNAME`。账号需有对应数据库权限。

## Qdrant 配置与同步边界

本地 Qdrant 使用 HTTP 端口 6333、gRPC 端口 6334；项目连接 gRPC。预先创建 `study_note_chunks_v1` 集合，维度 1024，距离类型 Cosine；项目配置不自动创建集合。启动前确认容器配置了持久化存储。`QDRANT_SYNC_ENABLED` 和 `QDRANT_SEARCH_ENABLED` 默认均为 `true`，可分别设为 `false`。同步任务会失败重试；异常中断后 RUNNING 的任务仍需确认旧执行者停止后手动恢复。

MySQL 与 Qdrant 不共享事务，当前检查不能发现所有异常，也不保证强一致。详细启动、回填和限制见 [Qdrant 接入与验证](docs/Qdrant接入与验证.md)，历史验收记录中的功能范围以验收日期为准。

## 启动应用

在 IDEA 的 `AiStudyAssistantApplication` 运行配置中填写环境变量：

| 名称 | 用途 |
| --- | --- |
| `DB_PASSWORD` | MySQL 用户密码 |
| `JWT_SECRET` | Base64 编码的随机签名密钥；解码后至少 32 字节 |
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 |

可在 PowerShell 生成 32 个随机字节的 Base64 密钥：

```powershell
$jwtKeyBytes = New-Object byte[] 32
$jwtRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtKeyBytes)
$jwtRng.Dispose()
[Convert]::ToBase64String($jwtKeyBytes)
```

完整复制输出，包括末尾可能出现的 `=`。不要把真实密钥、密码或 JWT 粘贴到公开文档中。修改 JWT 密钥会使旧令牌失效。配置好环境变量后运行启动类；默认 HTTP 地址为 `http://localhost:8080`。

也可在已经设置上述环境变量的 PowerShell 中进入项目根目录运行 `mvn spring-boot:run`。应用需要 MySQL、Ollama 和 DeepSeek 的配置可用；HTTP 服务启动成功不等于这些外部依赖都已验证。

## 最小验证流程

项目提供 [`http/学习示例.http`](http/学习示例.http) 和合成示例文本。可用 IDEA HTTP 客户端依次执行：

1. 注册、登录，复制登录响应中的 `token` 到示例文件的 `@token`；不要提交包含真实令牌的文件。
2. 创建笔记，或上传示例 TXT。成功响应含 `noteId`、`chunksGenerated`。若 `chunksGenerated` 为 `false`，表示笔记已创建但片段未全部生成，可检查服务后调用批量补建接口。
3. 将笔记 ID 填入 `@noteId`，查询片段或基于资料提问；返回的 `sources` 表示提供给模型的参考片段，并不保证逐句引用正确。
4. 根据该笔记生成题目，把返回的 `quizId` 填入 `@quizId`。生成和查看题目时不返回答案；提交选项后返回对错和解析，并写入作答记录。
5. 创建会话，将响应中的 ID 填入 `@sessionId`，调用流式会话接口。`message` 事件承载片段，保存成功后发 `done`；出现 `error` 表示这一轮没有正常完成。

JWT 缺失通常返回 401；本人无权访问或不存在的笔记、题目、会话返回 404。主要示例见 `.http` 文件；开发时出现 502 可先检查本地 Ollama 或云端模型配置。

## 测试与限制

- `mvn test` 包含单元测试和使用真实 MySQL 的集成测试；部分测试会使用本地 Ollama 及临时模拟 HTTP 服务。应使用**专用测试数据库**，通过 `SPRING_DATASOURCE_URL` 指向它，不要用有价值的正式数据运行测试。
- PDF 导入主要面向可提取文字的 PDF；扫描图片、OCR 和复杂排版没有完整支持。上传 TXT 最大 10 KiB；PDF 文件最大 2 MiB、最多 50 页，导入后的文本还受 10 KiB 限制。
- 开启 Qdrant 检索时优先使用索引并核验结果；不满足检查条件或发生异常时回退到 MySQL，逐条计算片段相似度。相似资料可能挤掉真正的证据；AI 答案和练习题可能出错，需要核对。
- 会话流使用最近 20 条历史；只在模型完成且成功保存后发送 `done`，失败发送 `error`。同一会话并发、客户端断连时序及所有代理环境尚未完整验证。
- 错题列表保留曾经答错的题，`latestCorrect` 只表示最近一次是否答对；历史和列表提供游标或数量限制，见示例。
- Redis、消息队列、OCR 和完整 Docker 一键部署不在当前交付范围内。Qdrant 可以单独通过 Docker 运行。`/hello`、`/database-check`、`/users/count` 是学习阶段保留的演示接口，不应直接将本项目未经加固地暴露到公网。
- 项目用于学习，不对数据安全、生成结果或高并发生产使用作保证。

当前文档目录见 [文档索引](docs/README.md)；历史验收过程见 [验收与限制](docs/验收与限制.md)。


## 第二阶段扩展

- 资料异步导入：`POST /notes/upload-async` 返回笔记编号和任务编号；`GET /chunk-jobs/{id}` 查看状态，失败时可用 `POST /chunk-jobs/{id}/retry` 重试。任务使用数据库持久化，后台生成笔记向量与分片。
- 学习复习：`POST /study/tasks/generate` 根据当前用户最近仍答错的题生成今日任务；`GET /study/tasks` 查看；`PUT /study/tasks/{id}` 修改完成状态。重复生成不会重复添加同日同题任务。
- 学习助手：`POST /study/assistant` 使用只读工具查询当前用户相关笔记、错题和答题统计；身份由后端传入，不由模型指定。工具调用与回答具有随机性。
- RAG 候选由最多 5 个增加至最多 10 个。既有 12 个合成问题的证据对照中，标注问题全部证据命中由 10/11 增加到 11/11，参考正文总量上升约 18%；样本量小，不能推广为实际准确率或费用结论。

本机运行、演示及边界见 [第二阶段部署与演示](docs/第二阶段部署与演示.md) 和 [HTTP 演示](http/第二阶段演示.http)。

## Qdrant 向量检索

项目已提供 Qdrant 索引、同步任务、失败重试和 MySQL 兜底。安装、旧笔记回填、配置开关和一致性边界见 [Qdrant 接入与验证](docs/Qdrant接入与验证.md)，操作示例见 [Qdrant 同步示例](http/Qdrant同步示例.http)。
