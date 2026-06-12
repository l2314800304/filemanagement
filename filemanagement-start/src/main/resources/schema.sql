-- File Management Center Database Schema

CREATE DATABASE IF NOT EXISTS `filemanagement` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `filemanagement`;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`      VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password_hash` VARCHAR(128) NOT NULL COMMENT 'SM3哈希后的密码',
    `token`         VARCHAR(128) DEFAULT NULL COMMENT '登录令牌',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_token` (`token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 文件元数据表
CREATE TABLE IF NOT EXISTS `file_metadata` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_name`     VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_size`     BIGINT       NOT NULL COMMENT '文件大小(字节)',
    `file_hash`     VARCHAR(64)  NOT NULL COMMENT '文件SM3哈希',
    `mime_type`     VARCHAR(128) DEFAULT NULL COMMENT 'MIME类型',
    `storage_path`  VARCHAR(512) DEFAULT NULL COMMENT '存储路径',
    `upload_status` VARCHAR(20)  NOT NULL DEFAULT 'UPLOADING' COMMENT '上传状态: UPLOADING/MERGING/COMPLETED/FAILED',
    `user_id`       BIGINT       NOT NULL COMMENT '所属用户ID',
    `total_chunks`  INT          NOT NULL DEFAULT 0 COMMENT '总分片数',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_file_hash` (`file_hash`),
    KEY `idx_upload_status` (`upload_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元数据表';

-- 文件分片表
CREATE TABLE IF NOT EXISTS `file_chunk` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_metadata_id` BIGINT       NOT NULL COMMENT '关联文件元数据ID',
    `chunk_index`      INT          NOT NULL COMMENT '分片序号(从0开始)',
    `chunk_size`       BIGINT       NOT NULL COMMENT '分片大小(字节)',
    `chunk_hash`       VARCHAR(64)  DEFAULT NULL COMMENT '分片SM3哈希',
    `storage_path`     VARCHAR(512) DEFAULT NULL COMMENT '分片存储路径',
    `uploaded`         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已上传: 0-否 1-是',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_metadata_chunk` (`file_metadata_id`, `chunk_index`),
    KEY `idx_file_metadata_id` (`file_metadata_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分片表';

-- 文件策略配置表
CREATE TABLE IF NOT EXISTS `file_policy` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `policy_type`        VARCHAR(20)  NOT NULL COMMENT '策略类型: FORMAT/SIZE',
    `allowed_extensions` VARCHAR(1024) DEFAULT NULL COMMENT '允许的文件扩展名(逗号分隔)',
    `blocked_extensions` VARCHAR(1024) DEFAULT NULL COMMENT '禁止的文件扩展名(逗号分隔)',
    `max_file_size`      BIGINT       DEFAULT NULL COMMENT '最大文件大小(字节)',
    `is_active`          TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否生效',
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件策略配置表';

-- 初始化策略数据
INSERT INTO `file_policy` (`policy_type`, `allowed_extensions`, `blocked_extensions`, `max_file_size`, `is_active`)
VALUES ('FORMAT', 'jpg,jpeg,png,gif,bmp,webp,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,zip,rar,7z,mp4,mp3,mkv,avi,mov', 'exe,bat,cmd,sh,msi,dll,com,vbs,ps1', NULL, 1);

INSERT INTO `file_policy` (`policy_type`, `allowed_extensions`, `blocked_extensions`, `max_file_size`, `is_active`)
VALUES ('SIZE', NULL, NULL, 5368709120, 1);
