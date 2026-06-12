package com.filemgmt.domain.crypto.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记不参与 SM4 加密通信的接口方法。
 * 被标记的方法的请求体和响应体将跳过 SM4 加解密处理。
 * 典型用途：获取 SM2 公钥的引导接口（客户端尚无可用的 SM4 密钥）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipEncryption {
}
