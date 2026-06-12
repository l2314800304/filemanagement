package com.filemgmt.domain.file.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文件分片实体
 */
@Getter
@Setter
public class FileChunk {

    private Long id;
    private Long fileMetadataId;
    private Integer chunkIndex;
    private Long chunkSize;
    private String chunkHash;
    private String storagePath;
    private Boolean uploaded;
    private LocalDateTime createdAt;

    public FileChunk() {}

    public FileChunk(Long fileMetadataId, Integer chunkIndex, Long chunkSize) {
        this.fileMetadataId = fileMetadataId;
        this.chunkIndex = chunkIndex;
        this.chunkSize = chunkSize;
        this.uploaded = false;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 标记分片已上传
     */
    public void markUploaded(String storagePath, String chunkHash) {
        this.storagePath = storagePath;
        this.chunkHash = chunkHash;
        this.uploaded = true;
    }

    /**
     * 校验分片完整性
     */
    public boolean verifyIntegrity(String computedHash) {
        return this.chunkHash != null && this.chunkHash.equals(computedHash);
    }
}
