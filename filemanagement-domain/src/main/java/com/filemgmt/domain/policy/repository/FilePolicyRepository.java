package com.filemgmt.domain.policy.repository;

import com.filemgmt.domain.policy.valueobject.FileFormatPolicy;
import com.filemgmt.domain.policy.valueobject.FileSizeLimit;

/**
 * 文件策略仓储接口
 */
public interface FilePolicyRepository {

    /**
     * 获取当前生效的文件格式策略
     */
    FileFormatPolicy findActiveFormatPolicy();

    /**
     * 获取当前生效的文件大小限制
     */
    FileSizeLimit findActiveSizeLimit();
}
