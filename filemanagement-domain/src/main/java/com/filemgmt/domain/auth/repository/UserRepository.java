package com.filemgmt.domain.auth.repository;

import com.filemgmt.domain.auth.entity.User;

/**
 * 用户仓储接口
 */
public interface UserRepository {

    User save(User user);

    User findByUsername(String username);

    User findById(Long id);

    User findByToken(String token);

    boolean existsByUsername(String username);
}
