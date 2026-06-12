package com.filemgmt.infrastructure.interceptor;

import com.filemgmt.domain.crypto.service.CryptoDomainService;
import com.filemgmt.domain.crypto.service.SkipEncryption;
import com.filemgmt.domain.crypto.service.Sm4SessionAttribute;
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
 * 对所有接口响应进行SM4加密，除非方法标注了 @SkipEncryption
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
        // 标注了 @SkipEncryption 的方法不加密
        if (returnType.hasMethodAnnotation(SkipEncryption.class)) {
            return false;
        }
        // 其余所有接口都加密
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpReq = servletRequest.getServletRequest();
            Object keyObj = httpReq.getAttribute(Sm4SessionAttribute.SM4_KEY);
            String sm4Key = keyObj != null ? keyObj.toString() : null;
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
