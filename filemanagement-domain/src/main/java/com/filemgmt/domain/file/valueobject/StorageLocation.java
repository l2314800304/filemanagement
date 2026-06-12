package com.filemgmt.domain.file.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 存储位置值对象
 */
@Getter
@AllArgsConstructor
public class StorageLocation {

    private final String basePath;
    private final String relativePath;

    public String getFullPath() {
        return basePath + "/" + relativePath;
    }
}
