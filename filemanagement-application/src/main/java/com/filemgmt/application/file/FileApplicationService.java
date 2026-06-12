package com.filemgmt.application.file;

import com.filemgmt.domain.crypto.service.CryptoDomainService;
import com.filemgmt.domain.file.entity.FileChunk;
import com.filemgmt.domain.file.entity.FileMetadata;
import com.filemgmt.domain.file.repository.FileMetadataRepository;
import com.filemgmt.domain.file.service.FileDownloadDomainService;
import com.filemgmt.domain.file.service.FileStoragePort;
import com.filemgmt.domain.file.service.FileUploadDomainService;
import com.filemgmt.domain.file.valueobject.ChunkRange;
import com.filemgmt.domain.policy.service.FilePolicyDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileApplicationService {

    private final FileUploadDomainService uploadDomainService;
    private final FileDownloadDomainService downloadDomainService;
    private final FilePolicyDomainService policyDomainService;
    private final FileMetadataRepository fileMetadataRepository;
    private final CryptoDomainService cryptoService;
    private final FileStoragePort fileStoragePort;

    public FileApplicationService(FileUploadDomainService uploadDomainService,
                                   FileDownloadDomainService downloadDomainService,
                                   FilePolicyDomainService policyDomainService,
                                   FileMetadataRepository fileMetadataRepository,
                                   CryptoDomainService cryptoService,
                                   FileStoragePort fileStoragePort) {
        this.uploadDomainService = uploadDomainService;
        this.downloadDomainService = downloadDomainService;
        this.policyDomainService = policyDomainService;
        this.fileMetadataRepository = fileMetadataRepository;
        this.cryptoService = cryptoService;
        this.fileStoragePort = fileStoragePort;
    }

    /**
     * 初始化上传 - 秒传检测
     */
    @Transactional
    public Map<String, Object> initUpload(String fileName, Long fileSize, String fileHash,
                                           Long userId, Integer totalChunks) {
        policyDomainService.validateFile(fileName, fileSize);

        Map<String, Object> result = new HashMap<>();

        // 秒传检测
        FileMetadata existing = uploadDomainService.checkInstantUpload(fileHash);
        if (existing != null) {
            result.put("skipUpload", true);
            result.put("fileId", existing.getId());
            result.put("uploadId", existing.getId());
            result.put("uploadedChunks", List.of());
            return result;
        }

        // 创建上传任务
        FileMetadata metadata = uploadDomainService.createUploadTask(fileName, fileSize, fileHash, userId, totalChunks);
        List<Integer> uploadedChunks = uploadDomainService.getUploadedChunkIndexes(metadata.getId());

        result.put("skipUpload", false);
        result.put("uploadId", metadata.getId());
        result.put("fileId", metadata.getId());
        result.put("uploadedChunks", uploadedChunks);
        return result;
    }

    /**
     * 上传单个分片
     */
    @Transactional
    public Map<String, Object> uploadChunk(Long uploadId, Integer chunkIndex, String chunkHash,
                                            byte[] chunkData) throws IOException {
        // 保存分片到临时目录
        String chunkPath = fileStoragePort.saveChunk(new java.io.ByteArrayInputStream(chunkData), uploadId, chunkIndex);

        // 计算分片SM3哈希
        String computedHash = cryptoService.sm3Hash(chunkData);

        // 保存分片信息
        uploadDomainService.saveChunkInfo(uploadId, chunkIndex, (long) chunkData.length, chunkPath, computedHash);

        Map<String, Object> result = new HashMap<>();
        result.put("chunkIndex", chunkIndex);
        result.put("uploaded", true);
        return result;
    }

    /**
     * 合并分片
     */
    @Transactional
    public Map<String, Object> mergeChunks(Long uploadId, String fileHash) throws IOException {
        FileMetadata metadata = fileMetadataRepository.findById(uploadId);
        if (metadata == null) {
            throw new IllegalStateException("上传任务不存在: " + uploadId);
        }

        // 校验分片完整性
        if (!uploadDomainService.isAllChunksUploaded(uploadId, metadata.getTotalChunks())) {
            throw new IllegalStateException("分片不完整，请上传所有分片");
        }

        // 标记为合并中
        uploadDomainService.markMerging(uploadId);

        try {
            // 生成目标路径
            String targetRelativePath = fileStoragePort.generateFilePath(
                    metadata.getUserId(), metadata.getId(), metadata.getFileName());

            // 获取有序的分片列表
            List<FileChunk> chunks = uploadDomainService.getOrderedChunks(uploadId);

            // 合并分片
            fileStoragePort.mergeChunks(chunks, targetRelativePath);

            // 校验最终文件哈希
            String finalHash;
            try (InputStream is = new FileInputStream(fileStoragePort.getFullPath(targetRelativePath))) {
                finalHash = cryptoService.sm3Hash(is);
            }

            if (!finalHash.equals(fileHash)) {
                uploadDomainService.markFailed(uploadId);
                throw new IllegalStateException("文件哈希校验失败，文件可能损坏");
            }

            // 标记完成
            uploadDomainService.markCompleted(uploadId, targetRelativePath);

            // 清理临时分片
            fileStoragePort.cleanupChunks(uploadId);
            uploadDomainService.cleanupChunks(uploadId);

            Map<String, Object> result = new HashMap<>();
            result.put("fileId", metadata.getId());
            result.put("fileName", metadata.getFileName());
            result.put("fileSize", metadata.getFileSize());
            result.put("merged", true);
            return result;
        } catch (IOException e) {
            uploadDomainService.markFailed(uploadId);
            throw e;
        }
    }

    /**
     * 取消上传
     */
    @Transactional
    public void cancelUpload(Long uploadId) throws IOException {
        FileMetadata metadata = fileMetadataRepository.findById(uploadId);
        if (metadata != null) {
            metadata.markFailed();
            fileMetadataRepository.save(metadata);
            fileStoragePort.cleanupChunks(uploadId);
            uploadDomainService.cleanupChunks(uploadId);
        }
    }

    /**
     * 获取文件列表
     */
    public Map<String, Object> listFiles(Long userId, int page, int size) {
        List<FileMetadata> files = fileMetadataRepository.findByUserId(userId, page, size);
        long total = fileMetadataRepository.countByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("files", files);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /**
     * 获取文件详情
     */
    public FileMetadata getFileDetail(Long fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId);
        if (metadata == null || !metadata.isCompleted()) {
            throw new IllegalStateException("文件不存在或未完成上传");
        }
        return metadata;
    }

    /**
     * 计算下载范围
     */
    public ChunkRange calculateDownloadRange(String rangeHeader, long fileSize) {
        return downloadDomainService.calculateRange(rangeHeader, fileSize);
    }

    /**
     * 获取存储端口（供Controller层下载使用）
     */
    public FileStoragePort getFileStoragePort() {
        return fileStoragePort;
    }
}
