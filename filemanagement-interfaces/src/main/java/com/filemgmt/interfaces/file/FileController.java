package com.filemgmt.interfaces.file;

import com.filemgmt.application.auth.AuthApplicationService;
import com.filemgmt.application.file.FileApplicationService;
import com.filemgmt.domain.auth.entity.User;
import com.filemgmt.domain.crypto.service.CryptoDomainService;
import com.filemgmt.domain.crypto.service.Sm4SessionAttribute;
import com.filemgmt.domain.file.entity.FileMetadata;
import com.filemgmt.domain.file.service.FileStoragePort;
import com.filemgmt.interfaces.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileApplicationService fileApplicationService;
    private final AuthApplicationService authApplicationService;
    private final CryptoDomainService cryptoService;

    public FileController(FileApplicationService fileApplicationService,
                          AuthApplicationService authApplicationService,
                          CryptoDomainService cryptoService) {
        this.fileApplicationService = fileApplicationService;
        this.authApplicationService = authApplicationService;
        this.cryptoService = cryptoService;
    }

    /**
     * 初始化上传（秒传检测）
     */
    @PostMapping("/upload/init")
    public Result<Map<String, Object>> initUpload(@Valid @RequestBody InitUploadRequest request,
                                                   HttpServletRequest httpReq) {
        User user = getCurrentUser(httpReq);
        Map<String, Object> result = fileApplicationService.initUpload(
                request.getFileName(), request.getFileSize(), request.getFileHash(),
                user.getId(), request.getTotalChunks());
        return Result.success(result);
    }

    /**
     * 上传单个分片（JSON格式，分片数据经SM4加密后以base64传输）
     */
    @PostMapping("/upload/chunk")
    public Result<Map<String, Object>> uploadChunk(
            @Valid @RequestBody ChunkUploadRequest request,
            HttpServletRequest httpReq) throws IOException {
        // 从请求属性中获取SM4密钥（由CryptoInterceptor设置）
        String sm4Key = (String) httpReq.getAttribute(Sm4SessionAttribute.SM4_KEY);
        if (sm4Key == null) {
            throw new IllegalStateException("缺少SM4加密密钥，请检查请求头 X-Encrypted-SM4-Key");
        }

        // 解密分片数据：SM4解密 → base64字符串 → 原始字节
        String chunkBase64 = cryptoService.sm4Decrypt(request.getChunkData(), sm4Key);
        byte[] chunkBytes = Base64.getDecoder().decode(chunkBase64);

        Map<String, Object> result = fileApplicationService.uploadChunk(
                request.getUploadId(), request.getChunkIndex(), request.getChunkHash(), chunkBytes);
        return Result.success(result);
    }

    /**
     * 合并分片
     */
    @PostMapping("/upload/merge")
    public Result<Map<String, Object>> mergeChunks(@Valid @RequestBody MergeRequest request) throws IOException {
        Map<String, Object> result = fileApplicationService.mergeChunks(request.getUploadId(), request.getFileHash());
        return Result.success(result);
    }

    /**
     * 取消上传
     */
    @PostMapping("/upload/cancel")
    public Result<Void> cancelUpload(@Valid @RequestBody CancelRequest request) throws IOException {
        fileApplicationService.cancelUpload(request.getUploadId());
        return Result.success("上传已取消", null);
    }

    /**
     * 文件列表（分页）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> listFiles(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量最小为1") @Max(value = 100, message = "每页数量最大为100") int size,
            HttpServletRequest httpReq) {
        User user = getCurrentUser(httpReq);
        Map<String, Object> result = fileApplicationService.listFiles(user.getId(), page, size);

        @SuppressWarnings("unchecked")
        List<FileMetadata> files = (List<FileMetadata>) result.get("files");
        List<Map<String, Object>> fileVOs = files.stream().map(f -> {
            Map<String, Object> vo = new HashMap<>();
            vo.put("id", f.getId());
            vo.put("fileName", f.getFileName());
            vo.put("fileSize", f.getFileSize());
            vo.put("mimeType", f.getMimeType());
            vo.put("createdAt", f.getCreatedAt());
            return vo;
        }).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("files", fileVOs);
        data.put("total", result.get("total"));
        data.put("page", result.get("page"));
        data.put("size", result.get("size"));
        return Result.success(data);
    }

    /**
     * 文件详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getFileDetail(@PathVariable @NotNull(message = "文件ID不能为空") Long id) {
        FileMetadata metadata = fileApplicationService.getFileDetail(id);
        Map<String, Object> data = new HashMap<>();
        data.put("id", metadata.getId());
        data.put("fileName", metadata.getFileName());
        data.put("fileSize", metadata.getFileSize());
        data.put("fileHash", metadata.getFileHash());
        data.put("mimeType", metadata.getMimeType());
        data.put("createdAt", metadata.getCreatedAt());
        data.put("updatedAt", metadata.getUpdatedAt());
        return Result.success(data);
    }

    /**
     * 文件下载（文件数据经SM4加密后以base64返回）
     */
    @GetMapping("/{id}/download")
    public Result<Map<String, Object>> downloadFile(@PathVariable Long id,
                                                     HttpServletRequest httpReq) throws IOException {
        FileMetadata metadata = fileApplicationService.getFileDetail(id);
        FileStoragePort storagePort = fileApplicationService.getFileStoragePort();

        // 读取文件全部字节
        byte[] fileBytes;
        try (RandomAccessFile raf = storagePort.openRandomAccess(metadata.getStoragePath())) {
            fileBytes = new byte[Math.toIntExact(metadata.getFileSize())];
            raf.readFully(fileBytes);
        }

        // SM4加密文件数据
        String sm4Key = (String) httpReq.getAttribute(Sm4SessionAttribute.SM4_KEY);
        if (sm4Key == null) {
            throw new IllegalStateException("缺少SM4加密密钥，请检查请求头 X-Encrypted-SM4-Key");
        }
        String fileBase64 = Base64.getEncoder().encodeToString(fileBytes);
        String encryptedHex = cryptoService.sm4Encrypt(fileBase64, sm4Key);

        Map<String, Object> data = new HashMap<>();
        data.put("fileName", metadata.getFileName());
        data.put("fileSize", metadata.getFileSize());
        data.put("mimeType", metadata.getMimeType() != null ? metadata.getMimeType() : "application/octet-stream");
        data.put("fileData", encryptedHex);
        return Result.success(data);
    }

    private User getCurrentUser(HttpServletRequest request) {
        String token = request.getHeader("X-Auth-Token");
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("未登录，请先登录");
        }
        return authApplicationService.findByToken(token);
    }

    // ==================== 请求DTO ====================

    @Data
    public static class InitUploadRequest {
        @NotBlank(message = "文件名不能为空")
        @Size(max = 255, message = "文件名不能超过255个字符")
        private String fileName;

        @NotNull(message = "文件大小不能为空")
        @Min(value = 1, message = "文件大小必须大于0")
        private Long fileSize;

        @Size(min = 32, max = 128, message = "文件哈希长度不合法")
        private String fileHash;

        @NotNull(message = "总分片数不能为空")
        @Min(value = 1, message = "总分片数至少为1")
        private Integer totalChunks;

        @Min(value = 1, message = "分片大小必须大于0")
        private Long chunkSize;
    }

    @Data
    public static class ChunkUploadRequest {
        @NotNull(message = "uploadId不能为空")
        private Long uploadId;

        @NotNull(message = "chunkIndex不能为空")
        @Min(value = 0, message = "chunkIndex不能小于0")
        private Integer chunkIndex;

        private String chunkHash;

        @NotBlank(message = "分片数据不能为空")
        private String chunkData; // SM4加密后的hex字符串
    }

    @Data
    public static class MergeRequest {
        @NotNull(message = "uploadId不能为空")
        private Long uploadId;

        @Size(min = 32, max = 128, message = "文件哈希长度不合法")
        private String fileHash;
    }

    @Data
    public static class CancelRequest {
        @NotNull(message = "uploadId不能为空")
        private Long uploadId;
    }
}
