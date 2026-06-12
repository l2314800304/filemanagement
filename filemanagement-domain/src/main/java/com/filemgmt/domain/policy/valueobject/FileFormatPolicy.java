package com.filemgmt.domain.policy.valueobject;

import com.filemgmt.domain.file.valueobject.FileExtension;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件格式策略值对象
 */
@Getter
public class FileFormatPolicy {

    private final Set<String> allowedExtensions;
    private final Set<String> blockedExtensions;

    public FileFormatPolicy(String allowedStr, String blockedStr) {
        this.allowedExtensions = parseExtensions(allowedStr);
        this.blockedExtensions = parseExtensions(blockedStr);
    }

    private Set<String> parseExtensions(String str) {
        if (str == null || str.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 检查扩展名是否在黑名单中
     */
    public boolean isBlocked(FileExtension ext) {
        return blockedExtensions.contains(ext.getExtension());
    }

    /**
     * 检查扩展名是否在白名单中
     */
    public boolean isAllowed(FileExtension ext) {
        return allowedExtensions.contains(ext.getExtension());
    }

    /**
     * 是否有白名单限制
     */
    public boolean hasAllowList() {
        return !allowedExtensions.isEmpty();
    }
}
