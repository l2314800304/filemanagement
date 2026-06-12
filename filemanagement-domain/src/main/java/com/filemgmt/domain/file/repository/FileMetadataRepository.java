package com.filemgmt.domain.file.repository;

import com.filemgmt.domain.file.entity.FileMetadata;

import java.util.List;

/**
 * 文件元数据仓储接口
 */
public interface FileMetadataRepository {

    FileMetadata save(FileMetadata fileMetadata);

    FileMetadata findById(Long id);

    FileMetadata findByFileHash(String fileHash);

    List<FileMetadata> findByUserId(Long userId, int page, int size);

    long countByUserId(Long userId);

    void deleteById(Long id);
}
