package com.filemgmt.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "filemanagement.storage")
public class StorageProperties {

    private String basePath = "./storage";
    private String tempPath = "./storage/temp";
    private long chunkSize = 5242880; // 5MB
}
