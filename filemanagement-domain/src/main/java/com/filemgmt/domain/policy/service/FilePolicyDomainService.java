package com.filemgmt.domain.policy.service;

import com.filemgmt.domain.file.valueobject.FileExtension;
import com.filemgmt.domain.policy.repository.FilePolicyRepository;
import com.filemgmt.domain.policy.valueobject.FileFormatPolicy;
import com.filemgmt.domain.policy.valueobject.FileSizeLimit;

/**
 * 文件策略领域服务
 */
public class FilePolicyDomainService {

    private final FilePolicyRepository policyRepository;

    public FilePolicyDomainService(FilePolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    /**
     * 校验文件格式和大小
     */
    public void validateFile(String fileName, Long fileSize) {
        FileFormatPolicy formatPolicy = policyRepository.findActiveFormatPolicy();
        FileExtension ext = new FileExtension(fileName);

        // 黑名单优先
        if (formatPolicy.isBlocked(ext)) {
            throw new FilePolicyViolationException("文件格式被禁止: " + ext);
        }
        // 白名单检查
        if (formatPolicy.hasAllowList() && !formatPolicy.isAllowed(ext)) {
            throw new FilePolicyViolationException("文件格式不在允许列表: " + ext);
        }
        // 大小检查
        FileSizeLimit sizeLimit = policyRepository.findActiveSizeLimit();
        if (sizeLimit.isExceeded(fileSize)) {
            throw new FilePolicyViolationException(
                    "文件大小 " + formatSize(fileSize) + " 超过限制 " + sizeLimit.toReadableString());
        }
    }

    private String formatSize(Long size) {
        if (size >= 1073741824) return String.format("%.1f GB", size / 1073741824.0);
        if (size >= 1048576) return String.format("%.1f MB", size / 1048576.0);
        if (size >= 1024) return String.format("%.1f KB", size / 1024.0);
        return size + " B";
    }

    /**
     * 文件策略违规异常
     */
    public static class FilePolicyViolationException extends RuntimeException {
        public FilePolicyViolationException(String message) {
            super(message);
        }
    }
}
