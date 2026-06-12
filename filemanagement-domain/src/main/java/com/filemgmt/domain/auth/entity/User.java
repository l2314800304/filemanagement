package com.filemgmt.domain.auth.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Getter
@Setter
public class User {

    private Long id;
    private String username;
    private String passwordHash;
    private String token;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {}

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 验证密码
     */
    public boolean verifyPassword(String inputHash) {
        return this.passwordHash != null && this.passwordHash.equals(inputHash);
    }

    /**
     * 修改密码
     */
    public void changePassword(String oldHash, String newHash) {
        if (!verifyPassword(oldHash)) {
            throw new IllegalStateException("原密码不正确");
        }
        this.passwordHash = newHash;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置登录令牌
     */
    public void assignToken(String token) {
        this.token = token;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 清除登录令牌
     */
    public void clearToken() {
        this.token = null;
        this.updatedAt = LocalDateTime.now();
    }
}
