package org.example.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration  // 标记为配置类，Spring 会自动扫描并生效
public class CorsConfig {

    @Bean  // 将 CorsFilter 注入 Spring 容器，全局生效
    public CorsFilter corsFilter() {
        // 1. 创建 CORS 配置对象
        CorsConfiguration config = new CorsConfiguration();
        // 允许前端的源地址（替换为你的前端实际地址，如 http://localhost:8081）
        config.addAllowedOrigin("http://localhost:8081");
        // 允许所有请求方法（GET、POST、PUT、DELETE 等）
        config.addAllowedMethod("*");
        // 允许所有请求头（如 Content-Type、Authorization 等）
        config.addAllowedHeader("*");
        // 允许携带 Cookie（如果前端需要传递身份信息，如登录状态）
        config.setAllowCredentials(true);
        // 设置预检请求的有效期（单位：秒），避免频繁预检
        config.setMaxAge(3600L);

        // 2. 配置 URL 映射（对所有接口生效）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);  // /** 表示匹配所有接口

        // 3. 返回 CorsFilter 实例
        return new CorsFilter(source);
    }
}