package com.filemgmt.domain.file.service;

import com.filemgmt.domain.file.entity.FileChunk;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 文件存储端口接口 - 由基础设施层实现
 */
public interface FileStoragePort {

    /**
     * 生成分片存储路径
     */
    String saveChunk(InputStream inputStream, Long uploadId, Integer chunkIndex) throws IOException;

    /**
     * 合并分片到最终文件
     */
    void mergeChunks(List<FileChunk> chunks, String targetRelativePath) throws IOException;

    /**
     * 生成文件最终存储路径
     */
    String generateFilePath(Long userId, Long fileId, String fileName);

    /**
     * 获取文件完整路径
     */
    String getFullPath(String relativePath);

    /**
     * 清理分片临时文件
     */
    void cleanupChunks(Long uploadId) throws IOException;

    /**
     * 打开随机访问文件（用于Range下载）
     */
    java.io.RandomAccessFile openRandomAccess(String relativePath) throws IOException;
}
