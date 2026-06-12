package com.filemgmt.domain.auth.service;

import com.filemgmt.domain.auth.entity.User;
import com.filemgmt.domain.auth.repository.UserRepository;
import com.filemgmt.domain.crypto.service.CryptoDomainService;

import java.util.UUID;

/**
 * 用户领域服务
 */
public class UserDomainService {

    private final UserRepository userRepository;
    private final CryptoDomainService cryptoService;

    public UserDomainService(UserRepository userRepository, CryptoDomainService cryptoService) {
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
    }

    /**
     * 注册用户
     */
    public User register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException("用户名已存在: " + username);
        }
        String passwordHash = cryptoService.sm3Hash(password);
        User user = new User(username, passwordHash);
        return userRepository.save(user);
    }

    /**
     * 用户登录
     */
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalStateException("用户不存在: " + username);
        }
        String passwordHash = cryptoService.sm3Hash(password);
        if (!user.verifyPassword(passwordHash)) {
            throw new IllegalStateException("密码不正确");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        user.assignToken(token);
        return userRepository.save(user);
    }

    /**
     * 通过令牌查找用户
     */
    public User findByToken(String token) {
        User user = userRepository.findByToken(token);
        if (user == null) {
            throw new IllegalStateException("无效的令牌");
        }
        return user;
    }
}
