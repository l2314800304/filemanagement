package com.filemgmt.domain.file.valueobject;

import lombok.Getter;

/**
 * HTTP Range下载范围值对象
 */
@Getter
public class ChunkRange {

    private final long start;
    private final long end;
    private final long total;

    public ChunkRange(long start, long end, long total) {
        this.start = start;
        this.end = end;
        this.total = total;
    }

    public long getContentLength() {
        return end - start + 1;
    }

    public String toContentRangeHeader() {
        return "bytes " + start + "-" + end + "/" + total;
    }
}
