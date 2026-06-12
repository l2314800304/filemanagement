package com.filemgmt.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_metadata")
public class FileMetadataDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;

    private Long fileSize;

    private String fileHash;

    private String mimeType;

    private String storagePath;

    private String uploadStatus;

    private Long userId;

    private Integer totalChunks;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
