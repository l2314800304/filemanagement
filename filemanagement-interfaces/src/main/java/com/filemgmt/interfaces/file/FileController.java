package com.filemgmt.interfaces.file;

import com.filemgmt.application.auth.AuthApplicationService;
import com.filemgmt.application.file.FileApplicationService;
import com.filemgmt.domain.auth.entity.User;
import com.filemgmt.domain.file.entity.FileMetadata;
import com.filemgmt.domain.file.service.FileStoragePort;
import com.filemgmt.domain.file.valueobject.ChunkRange;
import com.filemgmt.interfaces.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileApplicationService fileApplicationService;
    private final AuthApplicationService authApplicationService;

    public FileController(FileApplicationService fileApplicationService,
                          AuthApplicationService authApplicationService) {
        this.fileApplicationService = fileApplicationService;
        this.authApplicationService = authApplicationService;
    }

    /**
     * 初始化上传（秒传检测）
     */
    @PostMapping("/upload/init")
    public Result<Map<String, Object>> initUpload(@RequestBody InitUploadRequest request,
                                                   HttpServletRequest httpReq) {
        User user = getCurrentUser(httpReq);
        Map<String, Object> result = fileApplicationService.initUpload(
                request.getFileName(), request.getFileSize(), request.getFileHash(),
                user.getId(), request.getTotalChunks());
        return Result.success(result);
    }

    /**
     * 上传单个分片（不加密，二进制流）
     */
    @PostMapping("/upload/chunk")
    public Result<Map<String, Object>> uploadChunk(
            @RequestParam("uploadId") Long uploadId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam(value = "chunkHash", required = false) String chunkHash,
            @RequestParam("file") MultipartFile file) throws IOException {
        byte[] data = file.getBytes();
        Map<String, Object> result = fileApplicationService.uploadChunk(uploadId, chunkIndex, chunkHash, data);
        return Result.success(result);
    }

    /**
     * 合并分片
     */
    @PostMapping("/upload/merge")
    public Result<Map<String, Object>> mergeChunks(@RequestBody MergeRequest request) throws IOException {
        Map<String, Object> result = fileApplicationService.mergeChunks(request.getUploadId(), request.getFileHash());
        return Result.success(result);
    }

    /**
     * 取消上传
     */
    @PostMapping("/upload/cancel")
    public Result<Void> cancelUpload(@RequestBody CancelRequest request) throws IOException {
        fileApplicationService.cancelUpload(request.getUploadId());
        return Result.success("上传已取消", null);
    }

    /**
     * 文件列表（分页）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> listFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
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
    public Result<Map<String, Object>> getFileDetail(@PathVariable Long id) {
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
     * 文件下载（支持Range断点续传，不加密）
     */
    @GetMapping("/{id}/download")
    public void downloadFile(@PathVariable Long id,
                             HttpServletRequest request,
                             HttpServletResponse response) throws IOException {
        FileMetadata metadata = fileApplicationService.getFileDetail(id);
        FileStoragePort storagePort = fileApplicationService.getFileStoragePort();

        String rangeHeader = request.getHeader("Range");
        ChunkRange range = fileApplicationService.calculateDownloadRange(rangeHeader, metadata.getFileSize());

        response.setContentType(metadata.getMimeType() != null ? metadata.getMimeType() : "application/octet-stream");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(metadata.getFileName(), StandardCharsets.UTF_8) + "\"");

        if (rangeHeader != null) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", range.toContentRangeHeader());
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }
        response.setHeader("Content-Length", String.valueOf(range.getContentLength()));

        try (RandomAccessFile raf = storagePort.openRandomAccess(metadata.getStoragePath());
             OutputStream os = response.getOutputStream()) {
            raf.seek(range.getStart());
            byte[] buffer = new byte[8192];
            long remaining = range.getContentLength();
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = raf.read(buffer, 0, toRead);
                if (read == -1) break;
                os.write(buffer, 0, read);
                remaining -= read;
            }
        }
    }

    private User getCurrentUser(HttpServletRequest request) {
        String token = request.getHeader("X-Auth-Token");
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("未登录，请先登录");
        }
        return authApplicationService.findByToken(token);
    }

    @Data
    public static class InitUploadRequest {
        private String fileName;
        private Long fileSize;
        private String fileHash;
        private Integer totalChunks;
        private Long chunkSize;
    }

    @Data
    public static class MergeRequest {
        private Long uploadId;
        private String fileHash;
    }

    @Data
    public static class CancelRequest {
        private Long uploadId;
    }
}
