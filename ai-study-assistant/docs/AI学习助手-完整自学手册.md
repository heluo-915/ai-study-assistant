# AI 学习助手后端：完整自学手册

> 适用项目：`D:\Projects\mySecondProject\ai-study-assistant`。更新于 2026-11-16，依据当前源码、配置与文档整理。项目以后修改时，以实际代码和测试为准。

这份手册的目标是让你能独立运行、解释、追踪、测试和修改本项目。读完不等于掌握：每章都有需要自己完成的任务和验收，遇到解释不清的地方要回到代码、运行结果和测试。没有任何文档能保证跳过练习后就掌握一个项目。

## 一、怎么使用这份手册

### 1. 掌握程度的三个等级

| 等级 | 要求 | 自我检查 |
| --- | --- | --- |
| 了解 | 知道是什么、解决什么问题、项目在哪里用到 | 能用两三句话说出用途，能定位文件 |
| 掌握 | 能追踪输入输出、主要分支及数据流 | 不看答案画调用链，解释正常和失败场景 |
| 熟练运用 | 能独立小范围修改，说明影响并验证 | 完成练习，测试覆盖正常、失败及归属边界 |

本项目最重要的链路要达到“掌握”；本手册明确标出的修改练习要达到“熟练运用”；第三方库底层、Reactor 内部实现和完整密码学数学证明了解即可。你不需要背下所有方法和类名，但要知道如何找到它们。

### 2. 每次学习按这个顺序

1. 先读本章“为什么学”和前置知识，拿一个具体请求理解业务。
2. 按“文件阅读顺序”打开 IDEA；用 Ctrl+单击进入方法，用查找用法反向追踪。
3. 自己画出“输入 → 调用 → 判断 → 结果”；输入、输出、数据库变化分别记下来。
4. 先预测练习的结果，再运行或阅读对应测试，核对差异。
5. 脱离代码回答验收题；答不上来就回看相关方法，而不是强行进入下一章。
6. 隔天、3 天后、1 周后分别花 10 分钟复述；错题记录原因和正确思路。

每次 45–90 分钟，中间休息。卡住 20 分钟时先缩小到一个具体变量或方法，写下“我知道什么、不知道什么”，再查资料或提问。建议建立本地学习笔记，模板见文末。

### 3. 建议排期与依赖

这是深入自学，不按固定天数保证完成。以下以每天 60–90 分钟估算约 5–8 周；基础薄弱、需要反复动手时可以更久。每周安排一次复盘，不要求连续学完。

| 顺序 | 章节 | 参考时长 | 阶段产出 |
| --- | --- | --- | --- |
| 0 | 环境、目录与项目地图 | 2–3 小时 | 一张架构图、能安全启动 |
| 1 | Java、HTTP、Spring、SQL 必要基础 | 4–8 小时，可按需补 | 看懂方法、对象、HTTP 请求和基本 SQL |
| 2 | 注册、登录、JWT、权限 | 4–6 小时 | 认证和用户隔离两张流程图 |
| 3 | 表结构与笔记 CRUD | 4–6 小时 | 基础表及迁移表关系图、笔记链路与状态码 |
| 4 | TXT/PDF 导入与文本切分 | 3–5 小时 | 上传到笔记的流程图、分片练习 |
| 5 | embedding 和整篇语义检索 | 4–6 小时 | 相似度手算、整篇检索调用链 |
| 6 | 分片 RAG 与一致性 | 5–8 小时 | 从笔记到回答的双向流程图 |
| 7 | 同步会话与 SSE | 5–8 小时 | 同步和流式时序图、异常处理表 |
| 8 | AI 练习题闭环 | 5–8 小时 | 出题到统计的链路和 SQL 说明 |
| 9 | 测试、异常处理与排错 | 4–6 小时 | 三类测试对照表、一次调试记录 |
| 10 | 综合项目演练 | 5–10 小时 | 一次安全改动及测试、最终自测 |

先修关系：2 依赖 0–1；3 依赖 1–2；4–6 依赖 3 且 5 在 6 之前；7 依赖 2–3；8 依赖 2–3 和基本模型调用；9 可贯穿始终；10 放最后。你已经复习了认证的一部分，可以从第 2 章的验收继续，不会的回第 1 章。

## 二、项目全貌与运行准备（阶段 0）

### 为什么学

不要把项目当作一百多个互不相关的文件。它是一套通过 HTTP 接收请求、处理业务、读写数据库并调用模型的后端。

```text
客户端 / IDEA HTTP 文件
  ↓ HTTP、JSON、JWT
拦截器 → Controller → Service → Mapper / Repository → MySQL
                          ├→ Ollama：文本转向量
                          ├→ DeepSeek：生成回答或题目
                          └→ Qdrant：片段向量索引，异常时回退 MySQL
```

登录令牌由应用签发和验证；JWT 不是一个单独运行的服务。Ollama 是本机服务；DeepSeek 是云服务；数据库存持久数据。模型输出会受随机性影响，提供来源不等于模型每句话都准确。

### 你要达到

- **掌握**：区分 Controller 接收请求、Service 处理业务、Mapper/Repository 访问数据库、DTO 传输数据、Entity 承载数据、config/security 配置与鉴权。
- **掌握**：知道请求中路径参数、请求体、Authorization 头分别是什么。
- **了解**：`pom.xml` 依赖，`application.yml` 配置和启动类；运行配置中的环境变量不是硬编码在代码里的业务数据。
- **了解**：MySQL 3307、服务 8080、Ollama 11434、Qdrant HTTP 6333 / gRPC 6334 是本项目默认配置，并非永远固定。

### 阅读与实操

1. 阅读项目 `README.md`、`docs/当前项目说明.md` 和 `docs/Qdrant接入与验证.md`；定位 `pom.xml`、`src/main/java`、`src/main/resources`、`src/test/java`、`database/schema.sql`、`http/学习示例.http`。
2. 阅读 `AiStudyAssistantApplication`；说出它是启动入口，但不会在这里写所有业务。
3. 检查 IDEA 运行配置和本机依赖；不用展示或粘贴数据库密码、JWT 密钥和 API 密钥。大文件和模型按你的偏好放在 E 盘。
4. 在合适的测试环境运行 `http/学习示例.http`，记录方法、路径、请求体、状态码、响应字段。注册、上传、答题会新增数据；真实 DeepSeek 调用联网且可能计费。不要运行对已有数据有破坏性的操作。
5. 若只学习阅读，可以先不运行云模型。启动成功本身不证明数据库、Ollama、DeepSeek 的所有请求都成功。

**验收**：不看图画出请求到 MySQL 的主要层次；说出 Ollama 和 DeepSeek 的区别；说出 `@RequestBody` 数据从哪里来。测试库和现有数据库用途要分开。

## 三、项目需要的基础（阶段 1）

### Java：必须掌握

- 类、对象、字段、构造方法、接口；`private final`、构造器注入、方法参数和返回值。
- `String`、`Long`、`boolean`、`float[]`、`List<T>`、`Optional<T>`，包括空集合与 `Optional.empty()` 的区别。
- `if`、循环、异常、`try/catch`、`throw`；能够跟踪 `return` 后哪些代码不会执行。
- `List.of()`、`ArrayList`、`Comparator`、lambda、方法引用会读即可；`record` 的 `content()` 不是普通 JavaBean 的 `getContent()`。
- UTF-8 字节长度与 Java 字符/Unicode 码点不总相等；中文和 emoji 是好例子。

**小练习**：给 `Optional<User>` 写存在和不存在两个分支；给 `{1, 2}` 的数组算平方和；说出 `List` 空和变量为 `null` 的区别。

### HTTP 与 Spring：必须掌握

- GET 查询、POST 创建/触发操作、PUT 更新、DELETE 删除的常见语义；方法本身不自动保证权限。
- 路径 `/notes/{noteId}`、查询参数 `?question=...`、JSON 请求体、`Authorization: Bearer ...` 的区别。
- 200、201、204、400、401、404、409、422、502 的含义；结合具体 Controller 判断，不要假设所有接口错误格式一致。
- `@RestController`、`@GetMapping`、`@PostMapping`、`@PathVariable`、`@RequestParam`、`@RequestBody`、`@RequestAttribute`；`ResponseEntity` 可组合状态码、响应头和响应体。
- Spring 管理 Bean、构造器注入；`@Service`、`@Repository`、`@Mapper` 的职责；`@Transactional` 的原子性与回滚要结合异常和代理实际调用理解。

### SQL 与数据库：必须掌握

- `SELECT/INSERT/UPDATE/DELETE`、`WHERE`、`AND/OR`、`ORDER BY`、`LIMIT`、`JOIN`、聚合 `MAX` 和 `GROUP BY`。
- 主键、外键、唯一约束、索引、自增；事务、提交、回滚与行锁；参数占位符 `?` 和 MyBatis 的 `#{...}`。
- `noteId` 和 `userId` 分别标识笔记和用户，二者在业务上不能互换。

**验收**：解释 `WHERE id = ? AND user_id = ?`；举例说明为什么括号会改变 `AND/OR` 的逻辑；写出一次插入和一次按用户查询的 SQL。

## 四、注册、登录、JWT 与授权（阶段 2）

### 阅读顺序

1. `controller/AuthController.java`：`register`、`login`，记录请求参数、校验和状态码。
2. `service/AuthService.java`：`register` 先 `encode`，`authenticate` 查询并 `matches`。
3. `repository/UserRepository.java`：`save` 的 `INSERT`、`findByUsername` 的 `SELECT`、`JdbcTemplate` 的参数绑定。
4. `config/PasswordConfig.java`：找到 BCrypt；`entity/User.java`：字段与数据库列的关系。
5. `service/JwtService.java`：`generateToken` 的 subject、签发/过期时间、签名；`parseUserId` 的验证与解析。
6. `security/JwtInterceptor.java`：请求头、异常处理、`setAttribute("userId", ...)`；`config/WebConfig.java`：`addPathPatterns`；找一个使用 `@RequestAttribute` 的 Controller。

### 必须真正理解

注册：输入密码 → BCrypt `encode` → 哈希写入 `users.password_hash`。同一密码再次编码通常不同，因 BCrypt 使用随机盐；`matches(原始密码, 已存哈希)` 利用已存参数验证，不是解密。

登录：按用户名查找 → 用户不存在或密码错误返回失败 → 成功时按用户 ID 签发 JWT。`Optional.empty()` 是容器中没有值；直接对空 Optional 调用 `get()` 会抛异常。

身份认证：受保护路径到达 Controller 前，拦截器验证签名与有效期；通过后在当前请求中放入 `userId`。令牌内容能解码但不能在不知道密钥的情况下随意修改且通过签名验证；签名不等于加密。不要把 JWT 密钥或真实令牌写进文档。

数据授权：令牌有效只能证明身份，查询别人的数据仍需业务和 SQL 校验归属。`WHERE id = #{noteId} AND user_id = #{userId}` 是第二道边界。未登录通常 401；笔记不存在或无权访问通常 404。

`WebConfig` 当前列出需要拦截的路径，新增受保护接口时要核对是否匹配；不要只记“白名单/黑名单”名称。演示接口可能公开，不能直接面向公网使用。

### 动手与验收

- 在纸上画出注册、登录、携带令牌查询笔记的三条链，标出每个方法和数据类型。
- 两个测试账号分别创建/查询笔记，预测无令牌、自己的笔记、另一用户笔记、不存在 ID 的结果；只操作专用测试数据。
- 不看代码回答：为什么 `encode` 两次不能直接 `equals`？为什么不能相信请求体里的 `userId`？修改 JWT payload 为什么验签失败？`@RequestAttribute` 的值是谁放进去的？
- 阅读 `JwtServiceTest` 和鉴权相关 Controller 测试，分别指出正常及过期令牌的验证。

## 五、数据库表、笔记 CRUD 和数据隔离（阶段 3）

### 表之间的关系

先以 `database/schema.sql` 画出 8 张基础表，再阅读 `database/migrations/002` 至 `005`：它们加入学习任务、分片构建任务、向量同步任务及重试字段。

```text
users → notes → note_embeddings（每篇最多一条整篇向量）
              → note_chunks（每篇可以有多条片段）
              → quiz_questions → quiz_attempts
      → chat_sessions → chat_messages
      → quiz_attempts
```

箭头表示业务关联；外键具体删除行为以 SQL 定义为准。`note_chunks` 的 `(note_id, chunk_index)` 唯一约束避免同一笔记出现重复序号；外键的 `ON DELETE CASCADE` 只适用于实际定义了该选项的关系，不能推断全部表都级联删除。

### 阅读顺序

1. `entity/Note.java`，对照 `notes` 表的 `id`、`user_id`、`title`、`content`、`created_at`。
2. `controller/NoteController.java` 的创建、查询、修改、删除；写下每个入口的参数校验和响应。
3. `service/NoteCreationService.java`、`NoteService.java`；分辨负责协调创建与负责具体笔记业务的类。
4. `mapper/NoteMapper.java`：找 `INSERT`、生成主键、带用户 ID 的查询、更新、删除。
5. `NoteEmbeddingMapper`、`NoteChunkMapper`：了解关联表；结合 schema 看外键和唯一约束。

### 必须掌握的区别

- `id` 唯一标识笔记；`user_id` 标识所属用户。一位用户可以有多篇不同 ID 的笔记。
- `@Options(useGeneratedKeys = true, keyProperty = "id")` 用于把数据库生成的主键回填到对象；要先插入再使用 ID。
- 受影响行数为 0 代表 SQL 没有更新/删除匹配行；具体接口如何解释，回到实现核对。
- 数据库列 `user_id` 到 Java `userId` 的映射可以靠 SQL 别名或 MyBatis 配置；不能笼统断言漏写别名一定是 null，要结合配置和查询结果。
- 笔记更新涉及整篇向量和旧片段；不要只看更新 SQL 就假定所有派生数据自动同步。

### 任务与验收

画一张表关系图，标出 1:1 与 1:N。分别追踪创建、查询、修改、删除：谁调用谁，执行哪些 SQL，数据在哪些表变化，跨用户会怎样。用两篇属于同一用户的笔记说明为什么 ID 不同、user_id 相同。结合 `NoteServiceTest` 和持久化测试理解事务。另追踪 `NoteUpdateService` 和 `NoteUpdatePersistenceService`：更新前准备向量，事务内锁定并复核旧内容后保存正文、向量、片段及同步任务；模型故障或并发修改如何处理，以代码为准。

## 六、文件导入和文本分片（阶段 4）

### 阅读顺序

1. `NoteController` 中 `/notes/upload`；找标题、文件、字节大小、文件类型、严格 UTF-8 解码和用户 ID。
2. `PdfPreviewController`、`PdfImportController`、`PdfTextExtractor`；对照预览和导入的区别。
3. `TextChunkService.split` 与 `TextChunkServiceTest`；弄清 `chunkSize`、`overlap`、空输入与非法参数。
4. 回到 `NoteCreationService.create`，追踪导入文字怎样复用建笔记过程。

当前文档给出的常用限制：TXT 最大 10 KiB，PDF 最大 2 MiB、50 页，导入文本还有大小限制；以实际代码为准。PDF 可提取文本不等于具备 OCR，扫描件和复杂排版是已知限制。

### 必须掌握

例如 `chunkSize=5`、`overlap=2`，对 `ABCDEFGHIJ` 切分，尝试预测 `ABCDE`、`DEFGH`、`GHIJ`；随后用代码核对。重叠减少信息被边界切断的概率，但不能保证上下文一定完整。这里的长度涉及 Unicode 码点，不能简单等同于 UTF-8 字节数或模型 token 数。

严格 UTF-8 解码遇到非法字节可以报告错误；直接 `new String(bytes, UTF_8)` 可能以替换字符处理错误输入。不要把“文件扩展名是 txt”理解为内容一定有效。

**验收**：画出上传到保存的链路；列出空文件、错误后缀、超限、错误编码、空正文等失败输入；说出分片与重叠的目的及其缺点。

## 七、向量、余弦相似度和整篇检索（阶段 5）

### 阅读顺序

`EmbeddingService.embed` → `cosineSimilarity` → `textSimilarity` → `NoteEmbeddingService` → `NoteEmbeddingMapper` → `NoteSemanticSearchService` → `NoteSemanticAskService`。先理解整篇向量，再学分片。

### 概念

Embedding 把文本变为数字数组，向量表达模型学到的语义特征。Ollama 中的 `qwen3-embedding:0.6b` 是项目当前的向量模型；它不直接代替 DeepSeek 生成最终答案。模型、维度和生成方式不同的向量不能想当然地混用；更换模型需考虑重建已有向量。

余弦相似度：`点积 / (向量 A 的长度 × 向量 B 的长度)`。自己计算：`[1,0]` 与 `[1,0]` 为 1；`[1,0]` 与 `[0,1]` 为 0；`[1,2]` 与 `[2,1]` 为 0.8。空数组、维度不一致和零向量应检查，观察项目如何处理。相似度不是正确率，也不是“相同的百分比”。

### 你要掌握

- 一次 `textSimilarity(a,b)` 在当前实现中需要分别为两个文本生成向量；追踪两次调用。
- `note_embeddings` 保存整篇笔记的向量和模型名；JSON 字符串如何被解析回 `float[]`。
- 问题向量与当前用户的笔记向量逐一比较、阈值过滤、降序并取前几项；读代码确认当前阈值及数量。
- 长文整篇向量可能稀释局部知识；阈值受模型和数据影响，0.5 不代表通用标准。

**练习**：用合成的相似句、不相关句和同义问法比较分数，先预测高低再记录。不要把单次分数当成普遍规律。检查 `EmbeddingServiceTest` 中的已知向量与边界。

## 八、分片 RAG、事务与一致性（阶段 6，重点）

### 两条链先分清

```text
写入：笔记正文 → TextChunkService 切分 → 逐片 Ollama embedding
     → 锁定并复核笔记 → 保存 note_chunks 并登记向量同步任务 → Qdrant

提问：问题 → 优先 Qdrant 检索并回 MySQL 核验，异常时回退 MySQL 比较
     → 将命中片段组装为参考 → DeepSeek → 回答 + sources
```

### 逐步阅读

1. `NoteCreationService.create`：先 `noteService.create`，再尝试 `buildChunks`；观察成功和失败返回的 `chunksGenerated`。
2. `NoteService.create`：查看笔记插入和整篇 embedding 的事务边界。不要误以为整个创建过程里的所有模型调用都在事务外。
3. `NoteChunkBuildService.buildChunks`：读当前笔记、保留原文、事务外生成片段、调用持久化服务复核。
4. `NoteChunkService.generateChunks`：500/50 的切分配置、每片向量、序号和模型名。
5. `NoteChunkPersistenceService.insertChunksIfUnchanged`：短事务内 `FOR UPDATE`、验证归属和正文、删除旧片段并插入新片段。
6. `NoteChunkBatchService` / `NoteChunkPreviewService`：补建缺失片段和预览用途。
7. `NoteChunkSearchService.search`：按配置尝试 Qdrant 并核验状态、数量和命中内容；未通过时读取本人 MySQL 片段并算分排序；`NoteChunkAskService.ask`：空结果直接返回，非空才拼参考并问模型。
8. `ChatService.buildNotePrompt` 和 `askAboutNote`：区分参考文本、用户问题、系统指令、模型返回。

### 必须掌握的一致性例子

假设 10:00 读取正文 A，10:01 正在生成 A 的向量，10:02 用户把正文改成 B，10:03 生成完成。如果直接保存 A 的片段，检索到的就会是旧内容。当前片段生成后会在事务内重新锁定并比较正文，不一致时拒绝保存；旧片段删除和新片段写入在同一事务中，失败应回滚。这样安排还能避免在等待每次片段向量生成时长期持有这一段持久化事务的锁。

两阶段有不同失败结果：笔记创建成功后片段生成失败时，`chunksGenerated=false` 允许稍后补建；但笔记创建阶段本身的失败行为要看 `NoteService.create` 的事务和实际调用。不能把所有失败都概括成“笔记一定保留”。

### 测试与验收

- 画写入与查询两张时序图，在图上标注事务、用户 ID 和每个表。
- 解释“查询为空”和“模型调用失败”是两种不同情况；找到无命中时不调 DeepSeek 的代码。
- 阅读 `NoteChunkServiceTest`、`NoteChunkPersistenceServiceTest`、`NoteChunkAskServiceTest`：找正常、失败、正文变化及隔离分支。
- 自己设计一篇多主题合成笔记，记录问题、Top 结果、得分、来源与回答；尝试改变问法。不修改阈值时解释相似片段挤占、切分边界和来源不保证逐句对应。
- 解释 RAG 与 Tool Calling 的区别：RAG 用检索资料辅助回答；项目的 `StudyAssistantService` 注册了 `StudyAssistantTools` 的只读工具，模型可选择调用。用户身份来自服务端 `ToolContext`，不是模型指定。

### Qdrant 同步与恢复：按当前实现学习

**先区分两种数据库。** MySQL 保存笔记、片段及业务数据，是归属与内容依据；Qdrant 保存片段检索索引，不能独立代替 MySQL。当前集合为 `study_note_chunks_v1`，向量维度 1024，距离方式 Cosine；HTTP 端口 6333，应用使用 gRPC 6334。实际配置以 `application.yml` 为准。

1. 从 `NoteChunkPersistenceService`、`NoteUpdatePersistenceService` 追踪业务事务中登记同步任务；`NoteVectorSyncTaskMapper` 保存用户、笔记、版本、动作和状态。
2. 看 `NoteVectorSyncScheduler` → `NoteVectorSyncWorker`：领取后 UPSERT 先删除旧向量，再写片段；DELETE 清理对应笔记向量。状态可能是 PENDING、RUNNING、SUCCEEDED、FAILED；普通失败有限次数重试。
3. 看 `NoteChunkSearchService`：核验用户、模型、任务和数量，以及返回片段与 MySQL 的一致性；检查未通过或查询异常时回退 MySQL。数量相等和少量命中校验不能发现所有索引异常。
4. 模拟中断时只在隔离测试环境操作：RUNNING 不会自动安全恢复；确认旧工作进程停止后按 `docs/Qdrant任务手动恢复.md` 操作。旧令牌限制数据库状态更新，但不能阻止旧执行者继续写 Qdrant。
5. 思考题：如果旧任务在超时之后仍执行，为什么简单重置 RUNNING 会产生覆盖风险？怎样设计隔离、对账和补偿？项目目前没有解决这一问题，也不保证跨库强一致。

### Tool Calling：跟踪一次只读工具调用

从 `StudyAssistantController` 的 `POST /study/assistant` 开始，阅读 `StudyAssistantService` 如何提供工具与 `ToolContext`，再阅读 `StudyAssistantTools` 的三个 `@Tool`：搜索本人笔记片段、查询最近仍未答对的题、查询答题统计。模型可决定是否调用，服务端传入用户身份；工具结果仍需作为模型输入处理，回答需要核对。普通 `POST /chat` 或固定 RAG 检索并不意味着每次都调用工具。

## 九、多轮会话和 SSE（阶段 7）

### 同步会话

读 `ChatSessionController` → `ChatSessionService` / `ChatSessionMapper` → `ChatMessageService` / `ChatMessageMapper` → `ChatConversationService` → `ChatService`。

理解会话归属校验、按会话 ID 查询消息、最近 20 条按正确顺序组成 `UserMessage` / `AssistantMessage`、追加本轮问题并请求模型，再保存 user/assistant 两条消息。模型并不会自动记住你数据库中的会话，历史是应用每次组装并发送的。理解 `Optional.empty()` 与空历史列表代表的不同情况。

### 流式会话

读 `ChatConversationStreamController` → `ChatConversationStreamService.prepare` → `ChatStreamPersistenceService.save` → `Utf8LengthCounter`；再比较 `ChatStreamController` 和 `ChatStreamService` 的无历史流。

- SSE 的 `message` 是输出片段；有历史的会话流在正常完成并成功保存后发 `done`；流处理失败会发 `error`。流建立前的参数和归属校验可能返回其他 HTTP 状态；流开始后的错误不能只靠 HTTP 200 判断。
- 本代码在 `prepare` 中先查历史并构建 Prompt，然后才返回 `Flux.defer(...)`；`defer` 内的新 `StringBuilder`、计数器和流处理状态按订阅创建。不要误认为这里每次订阅都重新读数据库。
- 当前实现检查 finish reason 为 `stop` 后才保存并发 `done`。代理环境、客户端断连、并发及所有缺失终止标记场景并未完全验证。
- 数据库有字节容量限制，`Utf8LengthCounter` 处理片段累计大小及跨片段代理对；汉字和 emoji 的字节数不要按 Java 字符数估算。
- 保存调用在适合执行阻塞操作的调度器上进行，且在短事务内复核会话归属；先问“保证了什么”，再区分“没有验证什么”。

**练习**：画出一次成功流与模型中断流的时序；比较普通 `/chat` 和会话流的历史、保存和响应方式。阅读流式 HTTP 故障测试，找出“有 message 后异常时，不应发送 done 或保存半轮”的断言。实测流时优先使用项目 HTTP 示例，准备专用账号；真实调用可能产生费用。

**验收**：解释 `Flux` 在本项目扮演的角色、`defer` 的实际范围、为什么 `done` 在保存后、为什么 `error` 可能伴随 HTTP 200。了解 Reactor 完整背压协议即可，无需背全部操作符。

## 十、练习题、作答、错题和统计（阶段 8）

### 主链阅读顺序

1. `QuizController.generate`：笔记归属、调用生成、保存、不同分支状态码。
2. `QuizService.generateRawQuestion/generateQuestion`：提示模型输出 JSON，解析并校验题干、A/B/C/D、答案和解释。结构通过不代表知识正确。
3. `QuizPersistenceService`、`QuizQuestionMapper`：生成期间原笔记可能修改，查看条件插入如何避免保存与旧正文不一致的题目。
4. `QuizReviewService` / `QuizReviewController`：查看题目时不要提前暴露正确答案和解析；核对查询列及 DTO。
5. `QuizAnswerController` → `QuizAnswerService.answer` → `QuizAttemptMapper.insert`：校验、归属、比较答案、事务内记录、返回对错及解释。
6. `WrongQuizService`、`QuizQuestionMapper` 的错题查询：曾答错、最近一次作答状态、先筛选再限制；错题是已有作答记录的查询结果，不需要单独一张错题表。
7. `NoteQuizListService`、`QuizAttemptHistoryService`、`QuizStatisticsService`：`beforeId` 游标与统计。

### 你要掌握

- 一次模型生成和一次作答是不同数据：`quiz_questions` 存题，`quiz_attempts` 留每次作答记录。
- 题目通过所属笔记与用户关联；查询时需要按用户 ID 限制。
- `beforeId` 的语义是获取 ID 更早的一批；比较 offset 分页时要考虑排序、数据变动与适用场景，不能说它能解决所有分页问题。
- SQL 的 `MAX(id)` 分组和连接如何找到每道题的最近一次作答；区分“曾经答错”与“最近一次答错”。
- 状态码以实际 Controller 分支为准；模型内容不足、题目格式错误、内容变化和无权限各自回到实现核实。

### 练习与验收

在测试数据中生成题目，查看题目不含答案，提交一个选项，查看历史、错题与统计；再答一次正确选项，比较“曾答错”和 `latestCorrect`。在纸上模拟同一题三条作答，算最近结果和总次数。阅读 Quiz 相关单元、数据库及 HTTP 测试，说明为什么必须同时测隐藏答案和用户隔离。

## 十一、测试、调试和故障定位（阶段 9）

### 三种主要测试

| 类型 | 替身与真实依赖 | 重点 |
| --- | --- | --- |
| 单元测试 | 可 mock Mapper、模型等 | 快速验证业务分支和算法边界 |
| 数据库集成测试 | 真实 MySQL | SQL、约束、事务、回滚、归属 |
| HTTP 测试 | 经 HTTP 调应用；上游模型可为模拟服务 | 状态码、鉴权、序列化、网络故障路径 |

`@Test` 标识测试；`assertEquals` 检查值，浮点数需考虑误差；`assertThrows` 检查异常；Mockito 的 `mock/when/verify` 用于构造替身和验证交互。mock 成功不代表真实模型、网络、数据库也成功。HTTP 测试可能使用假上游，不等同真实云模型验收。

项目曾于 2026-11-14 在独立副本和空测试库中完成 98 项测试及真实 HTTP 验收；这是当时结果，不保证你修改代码或环境后仍全部通过，也不代表已经验证生产性能。

### 环境与安全

- 测试前确认 `SPRING_DATASOURCE_URL` 指向专用测试数据库；完整测试可能写数据库，不能只看测试名就认定安全。不要对现有项目数据库重复执行建表脚本。
- `application.yml`、IDEA 运行配置和环境变量共同决定连接；不要把真实密钥、JWT 和数据库密码写入学习笔记或提交。
- 模型/网络错误需区分 Ollama 未启动、数据库不可用、DeepSeek 配置/网络、输入校验和业务归属；不要只凭一个状态码猜原因。
- `GlobalExceptionHandler` 和 `ModelServiceException` 的映射要结合接口阅读；项目存在不同错误响应格式。

### IDEA 断点练习

在测试库和测试账号下，以查看笔记为例：在 Controller 方法、Service 调用、Mapper 方法前设置断点；发请求；观察 `noteId`、`userId`、返回对象；使用 Step Into/Over，不查看或复制密钥。再比较不存在的 ID。为 RAG 和 SSE 各画一张对应断点路线。

**验收**：能写出至少 3 种不同类型的故障排查步骤；解释事务回滚应该由真实数据库测试验证；指出某个测试的准备、执行、断言和它没有覆盖的边界。

## 十二、完成标准与综合实践（阶段 10）

### 必做综合演练

1. 用一页纸画出项目模块与基础表、迁移表关系，不看代码口述登录、笔记、RAG、会话、练习题五条流程。
2. 用专用测试库、两个测试账号走通项目示例；记下请求、状态码、数据库变化和用户隔离，不写真实凭据。
3. 任选一次 `/notes/chunks/ask`，从 Controller 一直追到检索、Prompt、模型和响应；能指出空结果和模型故障在哪里分叉。
4. 任选一次流式会话，说明读取历史、发送片段、完成原因、保存和事件；画成功及失败两个时序图。
5. 任选一道题，说明题目怎么创建、答案何时隐藏与返回、作答怎样记录、错题和统计如何查询。
6. 独立做一个小改动：例如提取一处可配置的检索阈值。先记录当前行为，设计缺省值、非法值和测试；在副本或有版本管理的环境改动；跑相关测试并报告兼容性。不要直接改变线上或有价值的数据。
7. 用自己的话说明至少五个限制：分片召回与阈值、模型幻觉、来源不保证逐句、PDF OCR、并发/断连、性能或公网安全。

### 自评标准

每项按 0–3 分：0 不知道；1 能认出术语；2 能独立追踪正常和主要失败流程；3 能在测试保护下修改并解释取舍。认证、笔记 CRUD、分片 RAG、会话、练习闭环、测试与安全各至少 2 分；选择一个改造任务达到 3 分。发现某一模块没达标就回对应章节，不用以一次考试决定是否“学会”。

### 最终自测题

1. 一次携带令牌的请求如何得到 userId？签名验证和数据归属校验分别在哪里？
2. 同一明文密码两次 BCrypt 哈希为什么通常不同？`matches` 如何验证？
3. `?` 和 `#{...}` 为什么用参数绑定？`id` 和 `user_id` 为什么都要检查？
4. 画出一篇笔记、一个整篇向量和多个片段的关系及唯一约束。
5. 笔记创建成功但片段生成失败，如何从响应看出来，后续怎么处理？
6. 两个向量维度不一致或有零向量时，余弦计算为什么不能照常进行？
7. 问题和分片如何变成检索结果？阈值和 Top K 会带来什么局限？
8. 片段生成过程中正文变化，项目怎样避免保存旧内容的片段？
9. `sources` 究竟表示什么，为什么不能证明回答每句话正确？
10. 为什么第二次聊天需要应用重新提供历史？最多读取多少条？
11. `message/done/error` 各表示什么？为什么完成后仍需保存成功？
12. `Flux.defer` 在当前方法里具体包住哪些工作？历史读取在哪里？
13. 生成题目为何要解析和校验 JSON？为什么查看题目不能提前返回答案？
14. `quiz_attempts` 如何支撑错题、最近正确状态和统计？
15. Mock 模型测试和真实模型调用各能、不能证明什么？
16. 如何在不影响已有数据的前提下运行数据库测试？
17. 如何区分 401、404、409、422 和模型服务错误？先看哪里？
18. 本项目目前有没有 Tool Calling、专用向量数据库或 OCR？依据代码和文档回答。

### 参考答案提示

1. `JwtInterceptor` → `JwtService.parseUserId` 验签与校验 → 请求属性；SQL/服务按 userId 约束资源。2. 随机盐；`matches` 利用已保存哈希里的参数验证。3. 将输入作为参数，避免把输入拼成 SQL；一个限定资源，一个限定所属用户。4. `note_embeddings.note_id` 唯一；`note_chunks` 的 `(note_id,chunk_index)` 唯一。5. 看 `chunksGenerated`，检查模型并用项目补建接口。6. 维度无法对应或分母为零。7. 问题与片段向量算余弦、过滤排序；有漏召回和挤占。8. 事务内锁定并复核原文。9. 是模型收到的参考来源。10. 模型请求需要带上下文，项目取最近 20 条。11. 文本、完成、失败；持久化是完整会话的一部分。12. 查 `prepare`：历史和 Prompt 在 defer 外，累积状态及调用在内。13. 模型输出不保证格式；避免作答前泄题。14. 每次作答一条，以查询/聚合得出。15. mock 验证可控逻辑，真实调用覆盖实际依赖但结果可能波动。16. 指向专用库并确认测试会做什么。17. 从 Controller、拦截器、服务和异常处理看分支。18. 已接入 Qdrant 作为可回退的片段向量索引，也有只读 Tool Calling；扫描件 OCR 未完整支持。

## 十三、学习笔记模板

```text
日期 / 章节：
目标（我应该做到）：
请求或场景：
入口 Controller / 参数：
Service 调用链：
Mapper / SQL / 涉及的表：
正常结果及状态码：
失败、边界、用户隔离：
我自己解释的流程：
验证方法和结果（不写密钥）：
仍不理解的问题：
明天复述 / 3 天后 / 1 周后复述：
```

## 十四、关于原路线的使用

`LEARNING-ROADMAP.md` 的模块划分、真实测试案例和参考文件很有价值；本手册把基础、掌握目标、动手与验收展开了。原路线里的 16–18 小时适合快速回看，不作为自学掌握的期限；测试数量是特定日期的统计。遇到“每次订阅都会重查历史”“漏写别名一定为 null”等概括，应按实际调用位置和 MyBatis 配置验证。原路线的改造清单是可选练习，不要求在复习初期更改项目。
