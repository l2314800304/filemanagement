package com.filemgmt.domain.file.entity;

import com.filemgmt.domain.file.valueobject.UploadStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文件元数据实体（聚合根）
 */
@Getter
@Setter
public class FileMetadata {

    private Long id;
    private String fileName;
    private Long fileSize;
    private String fileHash;
    private String mimeType;
    private String storagePath;
    private UploadStatus uploadStatus;
    private Long userId;
    private Integer totalChunks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FileMetadata() {}

    public FileMetadata(String fileName, Long fileSize, String fileHash, Long userId, Integer totalChunks) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileHash = fileHash;
        this.userId = userId;
        this.totalChunks = totalChunks;
        this.uploadStatus = UploadStatus.UPLOADING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记上传完成
     */
    public void markCompleted(String path) {
        this.storagePath = path;
        this.uploadStatus = UploadStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为合并中
     */
    public void markMerging() {
        this.uploadStatus = UploadStatus.MERGING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记上传失败
     */
    public void markFailed() {
        this.uploadStatus = UploadStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 验证文件归属
     */
    public boolean isOwnedBy(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    /**
     * 是否上传完成
     */
    public boolean isCompleted() {
        return this.uploadStatus == UploadStatus.COMPLETED;
    }
}
