/**
 * 跨域配置与Web服务器配置类
 *
 * 功能概述：
 * • 配置CORS跨域资源共享策略，支持前后端分离架构
 * • 自定义Tomcat服务器参数，处理特殊字符路径
 * • 提供双重CORS保障：MVC配置和过滤器配置
 *
 * 跨域配置特性：
 * • 支持所有源地址访问（生产环境建议限定具体域名）
 * • 允许常见HTTP方法（GET/POST/PUT/DELETE）
 * • 开放所有请求头信息，支持身份认证
 * • 暴露Authorization头部用于JWT令牌传输
 *
 * 安全配置：
 * • 允许凭证传输（Cookie、Authorization等）
 * • 设置预检请求缓存时间减少OPTIONS请求
 * • 过滤器最高优先级确保跨域处理顺序
 *
 * 服务器优化：
 * • 放宽URL路径字符限制，支持特殊符号
 * • 适配前端框架可能使用的非常规字符
 */
package com.example.jpaspringboot.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins:http://localhost:8080,http://localhost:8090}") String origins) {
        this.allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty() && !"*".equals(value))
                .toList();
        if (this.allowedOrigins.isEmpty()) {
            throw new IllegalStateException("app.cors.allowed-origins must contain at least one explicit origin");
        }
    }

    /**
     * 配置Spring MVC跨域映射规则
     * 适用于@Controller注解的处理器方法
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                          // 匹配所有请求路径
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE") // 允许的HTTP方法
                .maxAge(168000)                             // 预检请求缓存时间（秒）
                .allowedHeaders("*")                        // 允许所有请求头
                .allowCredentials(true)                     // 允许携带认证信息
                .maxAge(3600)                               // 重复设置，实际以最后一个为准
                .exposedHeaders("Authorization");           // 暴露Authorization头部
    }

    /**
     * 注册CORS过滤器作为备用跨域解决方案
     * 优先级最高，确保在其他过滤器之前执行
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.addAllowedHeader("*");                       // 允许所有请求头
        config.addAllowedMethod("*");                       // 允许所有HTTP方法
        config.setAllowCredentials(true);                   // 允许凭证
        config.setMaxAge(3600L);                            // 预检缓存1小时
        source.registerCorsConfiguration("/**", config);    // 应用到所有路径

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);          // 设置最高优先级
        return bean;
    }

    /**
     * 自定义Tomcat服务器配置
     * 放宽URL路径字符限制，支持特殊符号
     */
    @Bean
    public TomcatServletWebServerFactory webServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers((Connector connector) -> {
            // 允许URL路径中包含特殊字符
            connector.setProperty("relaxedPathChars", "\"<>[\\]^`{|}");
            // 允许查询参数中包含特殊字符  
            connector.setProperty("relaxedQueryChars", "\"<>[\\]^`{|}");
        });
        return factory;
    }
}
