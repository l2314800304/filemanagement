package com.filemgmt.infrastructure.interceptor;

import com.filemgmt.domain.crypto.service.CryptoDomainService;
import com.filemgmt.domain.crypto.service.Sm4SessionAttribute;
import com.filemgmt.infrastructure.crypto.Sm2KeyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 国密加密通信拦截器
 * 从请求头 X-Encrypted-SM4-Key 取出SM2加密的SM4密钥，
 * 用服务端SM2私钥解密后存入request attribute供后续使用。
 */
@Component
public class CryptoInterceptor implements HandlerInterceptor {

    private final CryptoDomainService cryptoService;
    private final Sm2KeyManager sm2KeyManager;

    public CryptoInterceptor(CryptoDomainService cryptoService, Sm2KeyManager sm2KeyManager) {
        this.cryptoService = cryptoService;
        this.sm2KeyManager = sm2KeyManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String encryptedSm4Key = request.getHeader(Sm4SessionAttribute.ENCRYPTED_SM4_KEY_HEADER);
        if (encryptedSm4Key != null && !encryptedSm4Key.isEmpty()) {
            // 用SM2私钥解密SM4密钥
            String sm4KeyHex = cryptoService.sm2Decrypt(encryptedSm4Key, sm2KeyManager.getPrivateKeyHex());
            request.setAttribute(Sm4SessionAttribute.SM4_KEY, sm4KeyHex);
        }
        return true;
    }

    /**
     * 从请求中获取SM4密钥（hex字符串）
     */
    public static String getSm4Key(HttpServletRequest request) {
        Object key = request.getAttribute(Sm4SessionAttribute.SM4_KEY);
        return key != null ? key.toString() : null;
    }
}
