/**
 * Spring MVC 拦截器配置类
 *
 * 功能概述：
 * • 注册和管理自定义拦截器到Spring MVC拦截链
 * • 配置拦截器的路径匹配规则和排除规则
 * • 实现访问控制和安全防护功能
 *
 * 拦截器配置：
 * • AccessLimitInterceptor - 访问频率限制拦截器
 * • 拦截路径：/access/accessLimit（测试接口）、/api/login（登录接口）
 * • 排除路径：/access/login（测试登录页面）
 *
 * 安全策略：
 * • 对关键接口实施访问频率控制
 * • 防止暴力破解和恶意请求攻击
 * • 排除公开接口避免误拦截
 *
 * 技术实现：
 * • 实现WebMvcConfigurer接口自定义MVC配置
 * • 使用InterceptorRegistry管理拦截器链
 * • 支持Ant风格路径模式匹配
 */
package com.example.jpaspringboot.config;

import com.example.jpaspringboot.interceptor.AccessLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    // 注入自定义访问限制拦截器
    @Autowired
    private AccessLimitInterceptor accessLimitInterceptor;

    /**
     * 配置拦截器注册规则
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessLimitInterceptor)
                // 配置需要拦截的请求路径
                .addPathPatterns("/access/accessLimit","/api/login")
                // 配置排除拦截的请求路径
                .excludePathPatterns("/access/login");
    }
}