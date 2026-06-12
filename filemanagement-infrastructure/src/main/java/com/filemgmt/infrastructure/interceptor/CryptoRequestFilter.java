package com.filemgmt.infrastructure.interceptor;

import com.filemgmt.domain.crypto.service.CryptoDomainService;
import com.filemgmt.infrastructure.crypto.Sm2KeyManager;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * SM4请求体解密过滤器
 */
@Component
@Order(1)
public class CryptoRequestFilter implements Filter {

    private final CryptoDomainService cryptoService;
    private final Sm2KeyManager sm2KeyManager;

    public CryptoRequestFilter(CryptoDomainService cryptoService, Sm2KeyManager sm2KeyManager) {
        this.cryptoService = cryptoService;
        this.sm2KeyManager = sm2KeyManager;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpReq) {
            String path = httpReq.getRequestURI();

            // 排除不需要解密的路径
            if (path.contains("/crypto/public-key") || path.contains("/upload/chunk")
                    || path.contains("/download")) {
                chain.doFilter(request, response);
                return;
            }

            String encryptedSm4Key = httpReq.getHeader(CryptoInterceptor.ENCRYPTED_SM4_KEY_HEADER);
            if (encryptedSm4Key != null && !encryptedSm4Key.isEmpty()) {
                // 解密SM4密钥
                String sm4KeyHex = cryptoService.sm2Decrypt(encryptedSm4Key, sm2KeyManager.getPrivateKeyHex());

                // 读取加密的请求体
                String requestBody = new String(httpReq.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                if (!requestBody.isEmpty()) {
                    // SM4解密请求体
                    String decryptedBody = cryptoService.sm4Decrypt(requestBody, sm4KeyHex);
                    // 包装请求，返回解密后的body
                    chain.doFilter(new DecryptedRequestWrapper(httpReq, decryptedBody), response);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 解密后的请求包装器
     */
    private static class DecryptedRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;
        private final String contentType;

        public DecryptedRequestWrapper(HttpServletRequest request, String decryptedBody) {
            super(request);
            this.body = decryptedBody.getBytes(StandardCharsets.UTF_8);
            // 解密后的请求体实际是JSON格式,强制设置为application/json
            // 这样可以避免Spring MVC因Content-Type不匹配而拒绝处理
            this.contentType = "application/json;charset=UTF-8";
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() { return bais.available() == 0; }
                @Override
                public boolean isReady() { return true; }
                @Override
                public void setReadListener(ReadListener readListener) {}
                @Override
                public int read() { return bais.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() { return body.length; }

        @Override
        public long getContentLengthLong() { return body.length; }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getHeader(String name) {
            if ("Content-Type".equalsIgnoreCase(name)) {
                return contentType;
            }
            return super.getHeader(name);
        }

        @Override
        public java.util.Enumeration<String> getHeaders(String name) {
            if ("Content-Type".equalsIgnoreCase(name)) {
                return java.util.Collections.enumeration(java.util.Collections.singletonList(contentType));
            }
            return super.getHeaders(name);
        }
    }
}
