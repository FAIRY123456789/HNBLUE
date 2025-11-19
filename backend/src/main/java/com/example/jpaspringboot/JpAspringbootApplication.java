/**
 * Spring Boot 应用主启动类
 *
 * 功能概述：
 * • 应用启动入口，初始化Spring Boot容器
 * • 启用自动配置和组件扫描
 * • 开启Spring缓存功能支持
 *
 * 核心注解：
 * • @SpringBootApplication - 标记为主启动类，包含配置、扫描、自动装配
 * • @EnableCaching - 启用Spring缓存抽象，支持注解驱动的缓存管理
 *
 * 应用特性：
 * • 自动扫描@Component, @Service, @Repository, @Controller等注解
 * • 内嵌Web服务器（Tomcat/Jetty/Undertow）支持
 * • 自动配置数据源、事务管理、MVC等基础设施
 * • 集成缓存管理器（Redis/Ehcache等）
 *
 * 启动流程：
 * 1. 加载应用配置和依赖组件
 * 2. 初始化Spring应用上下文
 * 3. 启动内嵌Web容器
 * 4. 部署REST API端点和服务
 */
package com.example.jpaspringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching  // 这里启用缓存功能
@SpringBootApplication
public class JpAspringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(JpAspringbootApplication.class, args);
    }

}
