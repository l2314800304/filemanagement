package com.filemgmt.domain.file.service;

import com.filemgmt.domain.file.entity.FileChunk;
import com.filemgmt.domain.file.entity.FileMetadata;
import com.filemgmt.domain.file.repository.FileChunkRepository;
import com.filemgmt.domain.file.repository.FileMetadataRepository;
import com.filemgmt.domain.file.valueobject.UploadStatus;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件上传领域服务
 */
public class FileUploadDomainService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileChunkRepository fileChunkRepository;

    public FileUploadDomainService(FileMetadataRepository fileMetadataRepository,
                                    FileChunkRepository fileChunkRepository) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileChunkRepository = fileChunkRepository;
    }

    /**
     * 检测秒传 - 通过文件哈希查找已存在的文件
     */
    public FileMetadata checkInstantUpload(String fileHash) {
        FileMetadata existing = fileMetadataRepository.findByFileHash(fileHash);
        if (existing != null && existing.isCompleted()) {
            return existing;
        }
        return null;
    }

    /**
     * 创建上传任务
     */
    public FileMetadata createUploadTask(String fileName, Long fileSize, String fileHash,
                                          Long userId, Integer totalChunks) {
        FileMetadata metadata = new FileMetadata(fileName, fileSize, fileHash, userId, totalChunks);
        return fileMetadataRepository.save(metadata);
    }

    /**
     * 获取已上传的分片索引列表
     */
    public List<Integer> getUploadedChunkIndexes(Long fileMetadataId) {
        List<FileChunk> chunks = fileChunkRepository.findByFileMetadataId(fileMetadataId);
        return chunks.stream()
                .filter(FileChunk::getUploaded)
                .map(FileChunk::getChunkIndex)
                .collect(Collectors.toList());
    }

    /**
     * 保存分片信息
     */
    public FileChunk saveChunkInfo(Long fileMetadataId, Integer chunkIndex, Long chunkSize,
                                    String storagePath, String chunkHash) {
        FileChunk chunk = fileChunkRepository.findByFileMetadataIdAndChunkIndex(fileMetadataId, chunkIndex);
        if (chunk == null) {
            chunk = new FileChunk(fileMetadataId, chunkIndex, chunkSize);
        }
        chunk.markUploaded(storagePath, chunkHash);
        return fileChunkRepository.save(chunk);
    }

    /**
     * 校验分片是否完整
     */
    public boolean isAllChunksUploaded(Long fileMetadataId, Integer totalChunks) {
        long uploadedCount = fileChunkRepository.countUploadedChunks(fileMetadataId);
        return uploadedCount == totalChunks;
    }

    /**
     * 标记文件为合并中
     */
    public void markMerging(Long fileMetadataId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileMetadataId);
        if (metadata != null) {
            metadata.markMerging();
            fileMetadataRepository.save(metadata);
        }
    }

    /**
     * 标记文件上传完成
     */
    public void markCompleted(Long fileMetadataId, String storagePath) {
        FileMetadata metadata = fileMetadataRepository.findById(fileMetadataId);
        if (metadata != null) {
            metadata.markCompleted(storagePath);
            fileMetadataRepository.save(metadata);
        }
    }

    /**
     * 标记文件上传失败
     */
    public void markFailed(Long fileMetadataId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileMetadataId);
        if (metadata != null) {
            metadata.markFailed();
            fileMetadataRepository.save(metadata);
        }
    }

    /**
     * 获取所有分片（按索引排序）
     */
    public List<FileChunk> getOrderedChunks(Long fileMetadataId) {
        List<FileChunk> chunks = fileChunkRepository.findByFileMetadataId(fileMetadataId);
        chunks.sort((a, b) -> a.getChunkIndex().compareTo(b.getChunkIndex()));
        return chunks;
    }

    /**
     * 清理分片记录
     */
    public void cleanupChunks(Long fileMetadataId) {
        fileChunkRepository.deleteByFileMetadataId(fileMetadataId);
    }
}
