package com.filemgmt.domain.file.service;

import com.filemgmt.domain.file.valueobject.ChunkRange;

/**
 * 文件下载领域服务
 */
public class FileDownloadDomainService {

    /**
     * 解析HTTP Range头，计算下载范围
     */
    public ChunkRange calculateRange(String rangeHeader, long fileSize) {
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            return new ChunkRange(0, fileSize - 1, fileSize);
        }

        String rangeValue = rangeHeader.substring("bytes=".length());
        String[] parts = rangeValue.split("-", 2);

        long start;
        long end;

        if (parts[0].isEmpty()) {
            // 格式: bytes=-500 (最后500字节)
            long suffix = Long.parseLong(parts[1]);
            start = Math.max(0, fileSize - suffix);
            end = fileSize - 1;
        } else if (parts[1].isEmpty()) {
            // 格式: bytes=500- (从500到结尾)
            start = Long.parseLong(parts[0]);
            end = fileSize - 1;
        } else {
            // 格式: bytes=500-999
            start = Long.parseLong(parts[0]);
            end = Long.parseLong(parts[1]);
        }

        if (start < 0) start = 0;
        if (end >= fileSize) end = fileSize - 1;
        if (start > end) {
            throw new IllegalArgumentException("无效的Range范围: " + rangeHeader);
        }

        return new ChunkRange(start, end, fileSize);
    }
}
