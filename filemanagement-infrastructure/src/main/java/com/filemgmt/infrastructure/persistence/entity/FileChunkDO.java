package com.filemgmt.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_chunk")
public class FileChunkDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileMetadataId;

    private Integer chunkIndex;

    private Long chunkSize;

    private String chunkHash;

    private String storagePath;

    private Boolean uploaded;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
