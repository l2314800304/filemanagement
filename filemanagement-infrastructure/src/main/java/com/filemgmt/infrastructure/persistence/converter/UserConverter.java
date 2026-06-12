package com.filemgmt.infrastructure.persistence.converter;

import com.filemgmt.domain.auth.entity.User;
import com.filemgmt.infrastructure.persistence.entity.UserDO;

public class UserConverter {

    public static User toDomain(UserDO userDO) {
        if (userDO == null) return null;
        User user = new User();
        user.setId(userDO.getId());
        user.setUsername(userDO.getUsername());
        user.setPasswordHash(userDO.getPasswordHash());
        user.setToken(userDO.getToken());
        user.setCreatedAt(userDO.getCreatedAt());
        user.setUpdatedAt(userDO.getUpdatedAt());
        return user;
    }

    public static UserDO toDO(User user) {
        if (user == null) return null;
        UserDO userDO = new UserDO();
        userDO.setId(user.getId());
        userDO.setUsername(user.getUsername());
        userDO.setPasswordHash(user.getPasswordHash());
        userDO.setToken(user.getToken());
        userDO.setCreatedAt(user.getCreatedAt());
        userDO.setUpdatedAt(user.getUpdatedAt());
        return userDO;
    }
}
