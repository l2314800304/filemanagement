# 文件管理中心 API 接口文档

## 基础信息

- **Base URL**: `http://localhost:8080`
- **通信加密**: SM2/SM4 国密算法
- **身份认证**: Token (通过 `X-Auth-Token` 请求头传递)

## 加密通信协议

### 加密流程

```
1. 前端请求 GET /api/crypto/public-key 获取SM2公钥
2. 前端生成随机SM4密钥(32位hex)
3. 前端用SM2公钥加密SM4密钥 → 放入请求头 X-Encrypted-SM4-Key
4. 前端用SM4密钥加密请求体(JSON) → 发送加密后的hex字符串
5. 后端用SM2私钥解密SM4密钥，再用SM4解密请求体
6. 后端用SM4密钥加密响应体 → 返回加密后的hex字符串
7. 前端用SM4密钥解密响应体
```

### 不需要加密的接口

| 接口 | 原因 |
|------|------|
| `GET /api/crypto/public-key` | 获取公钥本身不能加密 |
| `POST /api/file/upload/chunk` | 二进制文件流不适合加密 |
| `GET /api/file/{id}/download` | 二进制文件流不适合加密 |

---

## 1. 加密相关

### 1.1 获取SM2公钥

**请求**

```
GET /api/crypto/public-key
```

**响应** (明文JSON)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "publicKey": "04a3d1e8..."  // SM2公钥Hex编码(130字符)
  }
}
```

---

## 2. 用户认证

> 以下接口的请求体和响应体均经过SM4加密

### 2.1 用户注册

**请求**

```
POST /api/auth/register
Headers:
  X-Encrypted-SM4-Key: <SM2加密后的SM4密钥>
  Content-Type: text/plain
Body: <SM4加密后的JSON字符串>
```

明文请求体:

```json
{
  "username": "string (3-32字符)",
  "password": "string (6-32字符)"
}
```

**响应** (SM4加密)

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 1,
    "username": "testuser"
  }
}
```

### 2.2 用户登录

**请求**

```
POST /api/auth/login
Headers:
  X-Encrypted-SM4-Key: <SM2加密后的SM4密钥>
  Content-Type: text/plain
Body: <SM4加密后的JSON字符串>
```

明文请求体:

```json
{
  "username": "string",
  "password": "string"
}
```

**响应** (SM4加密)

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "a1b2c3d4e5f6...",  // 32位UUID Token
    "userId": 1,
    "username": "testuser"
  }
}
```

---

## 3. 文件上传

### 3.1 初始化上传（秒传检测）

**请求**

```
POST /api/file/upload/init
Headers:
  X-Auth-Token: <用户Token>
  X-Encrypted-SM4-Key: <SM2加密后的SM4密钥>
  Content-Type: text/plain
Body: <SM4加密后的JSON字符串>
```

明文请求体:

```json
{
  "fileName": "example.pdf",        // 原始文件名
  "fileSize": 104857600,             // 文件大小(字节)
  "fileHash": "sm3hex...",          // 文件SM3哈希(64位hex)
  "totalChunks": 20,                 // 总分片数
  "chunkSize": 5242880               // 分片大小(字节, 默认5MB)
}
```

**响应** (SM4加密)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "uploadId": 123,                 // 上传任务ID (后续上传分片使用)
    "fileId": 456,                   // 文件ID
    "skipUpload": false,             // true=秒传成功,无需上传分片
    "uploadedChunks": [0, 1, 2]     // 已上传的分片索引(断点续传用)
  }
}
```

### 3.2 上传单个分片

**请求** (不加密, multipart/form-data)

```
POST /api/file/upload/chunk
Content-Type: multipart/form-data
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `uploadId` | Long | 是 | 上传任务ID (来自初始化上传返回) |
| `chunkIndex` | Integer | 是 | 分片序号 (从0开始) |
| `chunkHash` | String | 否 | 分片SM3哈希 (用于完整性校验) |
| `file` | File | 是 | 分片二进制数据 |

**响应** (明文JSON)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "chunkIndex": 3,
    "uploaded": true
  }
}
```

### 3.3 合并分片

**请求**

```
POST /api/file/upload/merge
Headers:
  X-Auth-Token: <用户Token>
  X-Encrypted-SM4-Key: <SM2加密后的SM4密钥>
  Content-Type: text/plain
Body: <SM4加密后的JSON字符串>
```

明文请求体:

```json
{
  "uploadId": 123,
  "fileHash": "sm3hex..."
}
```

**响应** (SM4加密)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fileId": 456,
    "fileName": "example.pdf",
    "fileSize": 104857600,
    "merged": true
  }
}
```

### 3.4 取消上传

**请求**

```
POST /api/file/upload/cancel
Headers:
  X-Auth-Token: <用户Token>
  X-Encrypted-SM4-Key: <SM2加密后的SM4密钥>
  Content-Type: text/plain
Body: <SM4加密后的JSON字符串>
```

明文请求体:

```json
{
  "uploadId": 123
}
```

**响应** (SM4加密)

```json
{
  "code": 200,
  "message": "上传已取消",
  "data": null
}
```

---

## 4. 文件访问

### 4.1 文件列表（分页）

**请求**

```
GET /api/file/list?page=1&size=20
Headers:
  X-Auth-Token: <用户Token>
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 页码 |
| `size` | int | 20 | 每页数量 |

**响应** (SM4加密)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "files": [
      {
        "id": 1,
        "fileName": "example.pdf",
        "fileSize": 104857600,
        "mimeType": "application/pdf",
        "createdAt": "2026-06-12T12:00:00"
      }
    ],
    "total": 100,
    "page": 1,
    "size": 20
  }
}
```

### 4.2 文件详情

**请求**

```
GET /api/file/{id}
Headers:
  X-Auth-Token: <用户Token>
```

**响应** (SM4加密)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "fileName": "example.pdf",
    "fileSize": 104857600,
    "fileHash": "a1b2c3d4...",
    "mimeType": "application/pdf",
    "createdAt": "2026-06-12T12:00:00",
    "updatedAt": "2026-06-12T12:05:00"
  }
}
```

### 4.3 文件下载（支持断点续传）

**请求** (不加密, 二进制流)

```
GET /api/file/{id}/download
Headers:
  X-Auth-Token: <用户Token>
  Range: bytes=0-1048575    (可选, 用于断点续传)
```

**响应** (二进制文件流)

| 响应头 | 值 | 说明 |
|--------|-----|------|
| `Content-Type` | `application/octet-stream` 或具体MIME | 文件类型 |
| `Content-Disposition` | `attachment; filename="example.pdf"` | 下载文件名 |
| `Accept-Ranges` | `bytes` | 支持Range |
| `Content-Range` | `bytes 0-1048575/104857600` | 仅Range请求返回 |
| `Content-Length` | `1048576` | 本次返回的字节数 |
| `HTTP Status` | `206 Partial Content` | Range请求; 无Range时为200 |

**Range请求格式示例**:

```
# 下载前1MB
Range: bytes=0-1048575

# 从第1MB开始到结尾
Range: bytes=1048576-

# 下载最后1MB
Range: bytes=-1048576

# 下载第1MB到第2MB
Range: bytes=1048576-2097151
```

---

## 5. 错误码

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误/业务异常 |
| 403 | 文件策略违规（格式/大小限制） |
| 500 | 服务器内部错误 |

**错误响应格式** (SM4加密，除下载接口外):

```json
{
  "code": 400,
  "message": "用户名已存在: testuser",
  "data": null
}
```

---

## 6. 文件格式与大小限制

### 允许的文件格式（白名单）

`jpg, jpeg, png, gif, bmp, webp, pdf, doc, docx, xls, xlsx, ppt, pptx, txt, zip, rar, 7z, mp4, mp3, mkv, avi, mov`

### 禁止的文件格式（黑名单）

`exe, bat, cmd, sh, msi, dll, com, vbs, ps1`

### 文件大小限制

默认最大 **5GB** (5,368,709,120 字节)

---

## 7. 分片上传完整流程

```
前端                                    后端
 │                                        │
 │ 1. 计算文件SM3哈希                       │
 │                                        │
 │──POST /api/file/upload/init──────────>│ 格式/大小校验 + 秒传检测
 │<──{ uploadId, uploadedChunks } ──────│
 │                                        │
 │ 2. 遍历分片(跳过已上传的)                │
 │──POST /api/file/upload/chunk (i=0)──>│ 保存分片到临时目录
 │<──{ chunkIndex:0, uploaded:true } ───│
 │──POST /api/file/upload/chunk (i=1)──>│
 │<──{ chunkIndex:1, uploaded:true } ───│
 │  ... 重复直到所有分片上传完毕 ...         │
 │                                        │
 │ 3. 请求合并                             │
 │──POST /api/file/upload/merge─────────>│ 合并分片 + SM3校验
 │<──{ fileId, merged:true } ───────────│ 清理临时文件
 │                                        │
 │ 4. 完成！可在文件列表查看                │
```

## 8. 前端SM4加密伪代码

```javascript
import { sm2, sm4 } from 'sm-crypto'

// 1. 获取SM2公钥
const publicKey = await fetch('/api/crypto/public-key').then(r => r.json()).then(d => d.data.publicKey)

// 2. 生成SM4密钥 (32位hex)
const sm4Key = generateRandomHex(32)

// 3. SM2加密SM4密钥
const encryptedSm4Key = sm2.doEncrypt(sm4Key, publicKey, 1)  // 1=C1C3C2模式

// 4. SM4加密请求体
const encryptedBody = sm4.encrypt(JSON.stringify(requestData), sm4Key)

// 5. 发送请求
fetch('/api/auth/login', {
  method: 'POST',
  headers: {
    'X-Encrypted-SM4-Key': encryptedSm4Key,
    'Content-Type': 'text/plain'
  },
  body: encryptedBody
})

// 6. 解密响应
const encryptedResponse = await response.text()
const decryptedJson = sm4.decrypt(encryptedResponse, sm4Key)
const data = JSON.parse(decryptedJson)
```
