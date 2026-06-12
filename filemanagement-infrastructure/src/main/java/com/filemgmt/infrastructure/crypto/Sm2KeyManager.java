package com.filemgmt.infrastructure.crypto;

import com.filemgmt.domain.crypto.service.CryptoDomainService;
import com.filemgmt.domain.crypto.service.Sm2KeyPort;
import com.filemgmt.domain.crypto.valueobject.Sm2KeyPair;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * SM2密钥对管理器 - 应用启动时生成密钥对
 */
@Component
public class Sm2KeyManager implements Sm2KeyPort {

    private final CryptoDomainService cryptoService;

    @Getter
    private Sm2KeyPair keyPair;

    public Sm2KeyManager(CryptoDomainService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @PostConstruct
    public void init() {
        this.keyPair = cryptoService.generateSm2KeyPair();
    }

    @Override
    public String getPublicKeyHex() {
        return keyPair.getPublicKeyHex();
    }

    @Override
    public String getPrivateKeyHex() {
        return keyPair.getPrivateKeyHex();
    }
}
