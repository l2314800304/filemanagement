package com.filemgmt.domain.crypto.service;

import com.filemgmt.domain.crypto.valueobject.Sm2KeyPair;

import java.io.InputStream;

/**
 * 国密算法服务接口
 */
public interface CryptoDomainService {

    /**
     * 生成SM2密钥对
     */
    Sm2KeyPair generateSm2KeyPair();

    /**
     * SM2加密
     */
    String sm2Encrypt(String plainText, String publicKeyHex);

    /**
     * SM2解密
     */
    String sm2Decrypt(String cipherText, String privateKeyHex);

    /**
     * SM4加密
     */
    String sm4Encrypt(String plainText, String sm4KeyHex);

    /**
     * SM4解密
     */
    String sm4Decrypt(String cipherText, String sm4KeyHex);

    /**
     * SM3哈希 - 字符串
     */
    String sm3Hash(String content);

    /**
     * SM3哈希 - 流
     */
    String sm3Hash(InputStream inputStream);

    /**
     * SM3哈希 - 字节数组
     */
    String sm3Hash(byte[] data);
}
