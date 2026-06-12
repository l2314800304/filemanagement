package com.filemgmt.domain.file.valueobject;

import lombok.Getter;

/**
 * 文件扩展名值对象
 */
@Getter
public class FileExtension {

    private final String extension;

    public FileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        this.extension = dotIndex >= 0 ? fileName.substring(dotIndex + 1).toLowerCase() : "";
    }

    public FileExtension(String extension, boolean raw) {
        this.extension = extension.toLowerCase();
    }

    public boolean matches(String pattern) {
        return extension.equalsIgnoreCase(pattern.toLowerCase());
    }

    @Override
    public String toString() {
        return extension;
    }
}
