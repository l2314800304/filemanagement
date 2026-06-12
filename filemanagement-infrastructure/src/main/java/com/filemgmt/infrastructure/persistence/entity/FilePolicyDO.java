package com.filemgmt.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_policy")
public class FilePolicyDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String policyType;

    private String allowedExtensions;

    private String blockedExtensions;

    private Long maxFileSize;

    private Boolean isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
