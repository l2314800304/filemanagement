package com.filemgmt.application.auth;

import com.filemgmt.domain.auth.entity.User;
import com.filemgmt.domain.auth.service.UserDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService {

    private final UserDomainService userDomainService;

    public AuthApplicationService(UserDomainService userDomainService) {
        this.userDomainService = userDomainService;
    }

    @Transactional
    public User register(String username, String password) {
        return userDomainService.register(username, password);
    }

    @Transactional
    public User login(String username, String password) {
        return userDomainService.login(username, password);
    }

    public User findByToken(String token) {
        return userDomainService.findByToken(token);
    }
}
