package com.filemgmt.domain.crypto.service;

import com.filemgmt.domain.crypto.valueobject.Sm2KeyPair;

/**
 * SM2密钥管理端口 - 由基础设施层实现
 */
public interface Sm2KeyPort {

    /**
     * 获取SM2公钥（Hex编码）
     */
    String getPublicKeyHex();

    /**
     * 获取SM2私钥（Hex编码）
     */
    String getPrivateKeyHex();

    /**
     * 获取SM2密钥对
     */
    Sm2KeyPair getKeyPair();
}
