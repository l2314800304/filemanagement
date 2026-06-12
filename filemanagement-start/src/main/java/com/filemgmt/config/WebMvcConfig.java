package com.filemgmt.config;

import com.filemgmt.infrastructure.interceptor.CryptoInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CryptoInterceptor cryptoInterceptor;

    public WebMvcConfig(CryptoInterceptor cryptoInterceptor) {
        this.cryptoInterceptor = cryptoInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Range", "Accept-Ranges", "Content-Disposition")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cryptoInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/crypto/public-key");
    }
}
