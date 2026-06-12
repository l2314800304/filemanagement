package com.filemgmt.domain.crypto.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SM2密钥对值对象
 */
@Getter
@AllArgsConstructor
public class Sm2KeyPair {

    private final String publicKeyHex;
    private final String privateKeyHex;
}
