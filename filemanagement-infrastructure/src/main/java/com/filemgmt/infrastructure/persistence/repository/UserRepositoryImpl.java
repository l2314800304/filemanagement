package com.filemgmt.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.filemgmt.domain.auth.entity.User;
import com.filemgmt.domain.auth.repository.UserRepository;
import com.filemgmt.infrastructure.persistence.converter.UserConverter;
import com.filemgmt.infrastructure.persistence.entity.UserDO;
import com.filemgmt.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    public UserRepositoryImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User save(User user) {
        UserDO userDO = UserConverter.toDO(user);
        if (userDO.getId() == null) {
            userMapper.insert(userDO);
            user.setId(userDO.getId());
        } else {
            userMapper.updateById(userDO);
        }
        return user;
    }

    @Override
    public User findByUsername(String username) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getUsername, username);
        UserDO userDO = userMapper.selectOne(wrapper);
        return UserConverter.toDomain(userDO);
    }

    @Override
    public User findById(Long id) {
        UserDO userDO = userMapper.selectById(id);
        return UserConverter.toDomain(userDO);
    }

    @Override
    public User findByToken(String token) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getToken, token);
        UserDO userDO = userMapper.selectOne(wrapper);
        return UserConverter.toDomain(userDO);
    }

    @Override
    public boolean existsByUsername(String username) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getUsername, username);
        return userMapper.selectCount(wrapper) > 0;
    }
}
