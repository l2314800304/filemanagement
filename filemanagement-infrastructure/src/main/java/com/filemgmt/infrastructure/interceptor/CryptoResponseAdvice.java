package com.filemgmt.infrastructure.interceptor;

import com.filemgmt.domain.crypto.service.CryptoDomainService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应体SM4加密拦截器
 */
@RestControllerAdvice(basePackages = "com.filemgmt.interfaces")
public class CryptoResponseAdvice implements ResponseBodyAdvice<Object> {

    private final CryptoDomainService cryptoService;
    private final ObjectMapper objectMapper;

    public CryptoResponseAdvice(CryptoDomainService cryptoService, ObjectMapper objectMapper) {
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 对需要加密的接口进行加密（排除public-key和download接口）
        String methodName = returnType.getMethod() != null ? returnType.getMethod().getName() : "";
        return !methodName.equals("getPublicKey") && !methodName.equals("downloadFile")
                && !methodName.equals("uploadChunk");
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpReq = servletRequest.getServletRequest();
            String sm4Key = CryptoInterceptor.getSm4Key(httpReq);
            if (sm4Key != null) {
                try {
                    String json = objectMapper.writeValueAsString(body);
                    return cryptoService.sm4Encrypt(json, sm4Key);
                } catch (Exception e) {
                    throw new RuntimeException("响应加密失败", e);
                }
            }
        }
        return body;
    }
}
