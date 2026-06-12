package com.filemgmt.domain.policy.valueobject;

import lombok.Getter;

/**
 * 文件大小限制值对象
 */
@Getter
public class FileSizeLimit {

    private final Long maxBytes;

    public FileSizeLimit(Long maxBytes) {
        this.maxBytes = maxBytes;
    }

    /**
     * 检查文件大小是否超过限制
     */
    public boolean isExceeded(Long fileSize) {
        if (maxBytes == null || maxBytes <= 0) {
            return false;
        }
        return fileSize > maxBytes;
    }

    /**
     * 格式化最大文件大小为可读字符串
     */
    public String toReadableString() {
        if (maxBytes == null) return "无限制";
        if (maxBytes >= 1073741824) return String.format("%.1f GB", maxBytes / 1073741824.0);
        if (maxBytes >= 1048576) return String.format("%.1f MB", maxBytes / 1048576.0);
        if (maxBytes >= 1024) return String.format("%.1f KB", maxBytes / 1024.0);
        return maxBytes + " B";
    }
}
