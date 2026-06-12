package com.filemgmt.domain.file.repository;

import com.filemgmt.domain.file.entity.FileChunk;

import java.util.List;

/**
 * 文件分片仓储接口
 */
public interface FileChunkRepository {

    FileChunk save(FileChunk fileChunk);

    FileChunk findByFileMetadataIdAndChunkIndex(Long fileMetadataId, Integer chunkIndex);

    List<FileChunk> findByFileMetadataId(Long fileMetadataId);

    long countUploadedChunks(Long fileMetadataId);

    void deleteByFileMetadataId(Long fileMetadataId);
}
