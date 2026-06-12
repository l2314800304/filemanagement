# 文件管理中心 (File Management Center)

基于 **Spring Boot 3 + Vue 3 + DDD领域驱动设计** 构建的文件管理中心，支持国密算法(SM2/SM4)加密通信、分片上传、断点续传、秒传、文件格式与大小拦截。

## 功能特性

- **国密加密通信** — 基于 SM2/SM4 国密算法的请求/响应加密，保障数据传输安全
- **用户认证** — 用户名密码注册/登录，Token 鉴权，SM3 密码哈希
- **分片上传** — 大文件切割为 5MB 分片，支持并发上传（3路并发）
- **断点续传** — 上传中断后可从已上传的分片继续
- **秒传** — 基于 SM3 文件哈希检测，已存在文件直接秒传
- **文件格式拦截** — 数据库可配置的白名单/黑名单机制
- **文件大小限制** — 可配置的最大文件大小限制（默认 5GB）
- **Range 断点续传下载** — 支持 HTTP 206 Partial Content，下载中断可续传
- **完整前端界面** — Vue 3 + Element Plus 构建的管理界面

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | Java 运行环境 |
| Spring Boot | 3.3.6 | Web 框架 |
| MyBatis-Plus | 3.5.9 | ORM 框架 |
| MySQL | 8.x | 关系型数据库 |
| BouncyCastle | 1.78.1 | 国密算法实现 |
| Maven | 3.9.x | 项目构建 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.x | UI 框架 |
| Vite | latest | 构建工具 |
| Element Plus | latest | UI 组件库 |
| Axios | latest | HTTP 客户端 |
| sm-crypto | latest | 前端国密算法 |
| Pinia | latest | 状态管理 |
| Vue Router | 4.x | 路由管理 |

## 项目结构 (DDD 四层架构)

```
filemanagement/
├── pom.xml                                 # Maven 父 POM
├── filemanagement-domain/                  # 领域层（纯 Java，零框架依赖）
│   └── com.filemgmt.domain
│       ├── auth/       (User 实体, UserRepository, UserDomainService)
│       ├── file/       (FileMetadata, FileChunk, FileUploadDomainService, FileDownloadDomainService)
│       ├── policy/     (FileFormatPolicy, FileSizeLimit, FilePolicyDomainService)
│       └── crypto/     (CryptoDomainService, Sm2KeyPair, Sm2KeyPort)
├── filemanagement-application/             # 应用层
│   └── com.filemgmt.application
│       ├── auth/       (AuthApplicationService)
│       └── file/       (FileApplicationService)
├── filemanagement-interfaces/              # 用户接口层
│   └── com.filemgmt.interfaces
│       ├── auth/       (AuthController)
│       ├── file/       (FileController)
│       ├── crypto/     (CryptoController)
│       └── common/     (Result, GlobalExceptionHandler)
├── filemanagement-infrastructure/          # 基础设施层
│   └── com.filemgmt.infrastructure
│       ├── crypto/     (BouncyCastleCryptoService, Sm2KeyManager)
│       ├── storage/    (LocalFileStorageAdapter, ChunkStorageService)
│       ├── persistence/(DO, Mapper, Converter, RepositoryImpl)
│       ├── interceptor/(CryptoInterceptor, CryptoRequestFilter, CryptoResponseAdvice)
│       └── config/     (MybatisPlusConfig, DomainServiceConfig, StorageProperties)
├── filemanagement-start/                   # 启动模块
│   ├── FileManagementApplication.java
│   └── resources/
│       ├── application.yml
│       └── schema.sql                      # 建表 SQL
├── frontend/                               # Vue 3 前端项目
│   └── src/
│       ├── api/        (request.js, auth.js, file.js)
│       ├── views/      (Login, Register, FileList, Upload, FileDetail)
│       ├── stores/     (Pinia auth store)
│       └── utils/      (sm-crypto.js)
└── docs/
    └── api-doc.md                          # API 接口文档
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Node.js 18+ & npm

### 1. 数据库初始化

执行 `filemanagement-start/src/main/resources/schema.sql` 创建数据库和表：

```bash
mysql -u root -p < filemanagement-start/src/main/resources/schema.sql
```

### 2. 配置数据库连接

编辑 `filemanagement-start/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/filemanagement?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
    username: root
    password: your_password   # 修改为你的密码
```

### 3. 启动后端

```bash
cd filemanagement
mvn spring-boot:run -pl filemanagement-start
```

后端启动在 `http://localhost:8080`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动在 `http://localhost:3000`

## 加密通信协议

```
前端                                          后端
 │                                              │
 │──GET /api/crypto/public-key────────────────>│  返回 SM2 公钥
 │<────────── { publicKey } ──────────────────│
 │                                              │
 │  生成随机 SM4 密钥                            │
 │  SM2 公钥加密 SM4 密钥                        │
 │  SM4 加密请求体                               │
 │                                              │
 │──POST (Header: X-Encrypted-SM4-Key)────────>│  SM2 私钥解密 → SM4 密钥
 │   (Body: SM4 加密的 JSON)                     │  SM4 解密 → 明文 JSON
 │                                              │  业务处理
 │<──(Body: SM4 加密的响应) ───────────────────│  SM4 加密响应
 │                                              │
 │  SM4 解密响应体                               │
```

## API 接口概览

| 方法 | 路径 | 说明 | 加密 |
|------|------|------|------|
| `GET` | `/api/crypto/public-key` | 获取 SM2 公钥 | 否 |
| `POST` | `/api/auth/register` | 用户注册 | 是 |
| `POST` | `/api/auth/login` | 用户登录 | 是 |
| `POST` | `/api/file/upload/init` | 初始化上传 / 秒传检测 | 是 |
| `POST` | `/api/file/upload/chunk` | 上传分片 (multipart) | 否 |
| `POST` | `/api/file/upload/merge` | 合并分片 | 是 |
| `POST` | `/api/file/upload/cancel` | 取消上传 | 是 |
| `GET` | `/api/file/list` | 文件列表 (分页) | 是 |
| `GET` | `/api/file/{id}` | 文件详情 | 是 |
| `GET` | `/api/file/{id}/download` | 下载 (Range 断点续传) | 否 |

详细接口文档请参阅 [docs/api-doc.md](docs/api-doc.md)

## 分片上传流程

```
前端                                         后端
 │                                             │
 │ 1. 计算文件 SM3 哈希                          │
 │                                             │
 │──POST /api/file/upload/init───────────────>│ 格式/大小校验 + 秒传检测
 │<──{ uploadId, uploadedChunks } ───────────│
 │                                             │
 │ 2. 并发上传分片 (跳过已上传的)                 │
 │──POST /api/file/upload/chunk (i=0,1,2)──>│ 保存分片到临时目录
 │<──{ chunkIndex, uploaded:true } ──────────│
 │                                             │
 │ 3. 所有分片上传完毕，请求合并                   │
 │──POST /api/file/upload/merge──────────────>│ 合并分片 + SM3 校验
 │<──{ fileId, merged:true } ────────────────│ 清理临时文件
 │                                             │
 │ 4. 完成 ✓                                   │
```

## 文件策略配置

文件格式和大小限制通过数据库 `file_policy` 表配置，支持运行时动态修改：

- **白名单**: `jpg, jpeg, png, gif, pdf, doc, docx, xls, xlsx, ppt, pptx, txt, zip, rar, 7z, mp4, mp3` 等
- **黑名单**: `exe, bat, cmd, sh, msi, dll, com, vbs, ps1` 等可执行文件
- **大小限制**: 默认 5GB

## 许可证

MIT License
