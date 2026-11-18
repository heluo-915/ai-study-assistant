# 知页 · AI 学习助手

一个面向个人学习资料的 AI 辅助平台。你可以创建或导入笔记，围绕资料提问并查看参考来源，也可以生成练习题、记录作答和错题、安排学习任务，与学习助手对话。

项目包含 Vue 前端和 Java 后端，主要用于学习全栈开发、检索增强生成（RAG）和 AI 应用集成。

## 功能

- **账号与笔记**：注册、登录、JWT 身份验证；创建、查看、修改和删除本人笔记。
- **资料导入**：支持 TXT 导入、可提取文字的 PDF 预览与导入；后端提供异步导入任务。
- **基于资料问答**：支持关键词匹配与片段语义检索，回答附带供模型参考的笔记来源。
- **对话与流式输出**：创建会话、查看历史，使用 SSE 逐段显示模型回答。
- **练习与复习**：根据笔记生成单选题，记录作答、查看错题和统计，生成学习任务。
- **学习助手**：通过 Tool Calling 按需查询当前用户的笔记片段、错题和统计。
- **向量检索**：使用 Ollama 生成文本向量，以 Qdrant 构建片段索引，并在检查失败或服务异常时回退 MySQL 检索。

> 生成内容和题目可能有误，回答中显示的来源是提供给模型的参考材料，不代表每句话都经过逐句验证。

## 技术栈

| 部分 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite、JavaScript |
| 后端 | Java 21、Spring Boot、Spring AI、MyBatis、JWT |
| 存储 | MySQL 8；Qdrant 片段向量索引 |
| 模型 | DeepSeek 对话模型；Ollama `qwen3-embedding:0.6b` 向量模型 |
| 文档处理 | Apache PDFBox |

```text
浏览器 → Vue 前端 → Spring Boot 后端 → MySQL
                                ├── Ollama（文本向量）
                                ├── Qdrant（片段向量索引）
                                └── DeepSeek（回答、出题、工具调用）
```

MySQL 保存用户、笔记、片段及学习记录，是内容与归属的依据。Qdrant 是附加索引，不能独立替代 MySQL。不同功能使用不同的检索或对话路径，上图概括主要依赖。

## 项目结构

```text
.
├── ai-study-assistant/        # Spring Boot 后端、数据库脚本、HTTP 示例和项目文档
└── ai-study-assistant-front/  # Vue 3 前端
```

- [后端说明与数据库初始化](ai-study-assistant/README.md)
- [前端启动与功能说明](ai-study-assistant-front/README.md)
- [项目文档索引](ai-study-assistant/docs/README.md)
- [源码逐步阅读路线](ai-study-assistant/docs/项目源码逐步阅读路线.md)
- [完整自学手册](ai-study-assistant/docs/AI学习助手-完整自学手册.md)
- [Qdrant 接入与验证](ai-study-assistant/docs/Qdrant接入与验证.md)

## 本地运行

运行前准备 JDK 21、Maven、Node.js、MySQL 8、Ollama、Qdrant 和 DeepSeek API Key。具体配置与初始化步骤以[后端 README](ai-study-assistant/README.md)为准。

1. 启动 MySQL、Ollama 和 Qdrant；安装 Ollama 模型 `qwen3-embedding:0.6b`。默认 MySQL 端口 3307，Ollama 端口 11434，Qdrant 的 gRPC 端口 6334。
2. **首次使用空数据库**时，执行 `ai-study-assistant/database/schema.sql`，再按顺序执行 `database/migrations/002` 至 `005`。已有数据库不要重复建表；升级前备份并确认已执行的迁移。
3. 创建 Qdrant 集合 `study_note_chunks_v1`，向量维度为 1024，距离方式为 Cosine；项目默认不会自动创建集合。具体操作见 [Qdrant 文档](ai-study-assistant/docs/Qdrant接入与验证.md)。
4. 为后端配置环境变量 `DB_PASSWORD`、`JWT_SECRET`、`DEEPSEEK_API_KEY`，然后在 `ai-study-assistant` 目录运行 `mvn spring-boot:run`，或在 IDEA 中运行启动类。默认后端地址为 `http://localhost:8080`。
5. 在另一个终端启动前端：

   ```powershell
   cd ai-study-assistant-front
   npm install
   npm run dev
   ```

   用浏览器打开终端显示的地址。开发环境中 Vite 将 `/api` 请求代理到本地后端。

DeepSeek 请求会联网并可能产生费用；提问时所选资料或检索片段会发送给模型服务。不要将真实密钥、密码或登录令牌提交到仓库。

## 测试与当前边界

后端测试位于 `ai-study-assistant/src/test`；前端包含测试文件，可通过 [前端说明](ai-study-assistant-front/README.md) 查看。涉及数据库或外部服务的测试应使用专用测试库和测试集合，并检查是否需要额外启用。

- MySQL 和 Qdrant 不共享事务。普通同步失败支持有限次数重试，但程序中断后任务可能停留在 `RUNNING`，目前需确认旧执行者停止后手动恢复。
- 检索时的状态、数量和命中内容检查不能发现所有索引异常，也不构成跨库强一致性保证。
- PDF 主要面向可提取文字的文件；扫描件 OCR 与复杂排版尚未完整支持。
- 本项目用于学习和本地演示，公开部署前还需要针对鉴权、安全、并发、运维和模型效果进行进一步验证。
