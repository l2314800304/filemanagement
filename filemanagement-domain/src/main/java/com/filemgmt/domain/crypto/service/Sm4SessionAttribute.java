package com.filemgmt.domain.crypto.service;

/**
 * SM4加密通信常量定义。
 * 用于在拦截器(Infrastructure)和控制器(Interfaces)之间共享SM4密钥的request属性名。
 */
public final class Sm4SessionAttribute {

    private Sm4SessionAttribute() {}

    /**
     * 存储在HttpServletRequest attribute中的SM4密钥(hex字符串)的属性名
     */
    public static final String SM4_KEY = "sm4_key";

    /**
     * 请求头名称：SM2加密后的SM4密钥
     */
    public static final String ENCRYPTED_SM4_KEY_HEADER = "X-Encrypted-SM4-Key";
}
