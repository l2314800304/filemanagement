package com.filemgmt.infrastructure.interceptor;

import com.filemgmt.domain.crypto.service.CryptoDomainService;
import com.filemgmt.infrastructure.crypto.Sm2KeyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 国密加密通信拦截器
 */
@Component
public class CryptoInterceptor implements HandlerInterceptor {

    public static final String SM4_KEY_ATTRIBUTE = "sm4_key";
    public static final String ENCRYPTED_SM4_KEY_HEADER = "X-Encrypted-SM4-Key";
    public static final String AUTH_TOKEN_HEADER = "X-Auth-Token";

    private final CryptoDomainService cryptoService;
    private final Sm2KeyManager sm2KeyManager;

    public CryptoInterceptor(CryptoDomainService cryptoService, Sm2KeyManager sm2KeyManager) {
        this.cryptoService = cryptoService;
        this.sm2KeyManager = sm2KeyManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String encryptedSm4Key = request.getHeader(ENCRYPTED_SM4_KEY_HEADER);
        if (encryptedSm4Key != null && !encryptedSm4Key.isEmpty()) {
            // 用SM2私钥解密SM4密钥
            String sm4KeyHex = cryptoService.sm2Decrypt(encryptedSm4Key, sm2KeyManager.getPrivateKeyHex());
            request.setAttribute(SM4_KEY_ATTRIBUTE, sm4KeyHex);
        }
        return true;
    }

    /**
     * 从请求中获取SM4密钥
     */
    public static String getSm4Key(HttpServletRequest request) {
        Object key = request.getAttribute(SM4_KEY_ATTRIBUTE);
        return key != null ? key.toString() : null;
    }
}
