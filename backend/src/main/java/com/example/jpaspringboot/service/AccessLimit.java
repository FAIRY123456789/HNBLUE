/**
 * 接口访问频率限制自定义注解
 *
 * 功能概述：
 * • 基于方法级别的访问频率控制注解
 * • 支持时间窗口内的最大请求次数限制
 * • 提供登录状态校验的灵活配置
 *
 * 使用场景：
 * • API接口防刷限流保护
 * • 敏感操作频率控制
 * • 资源访问速率限制
 *
 * 核心参数：
 * • seconds - 时间窗口长度（单位：秒）
 * • maxCount - 时间窗口内允许的最大访问次数
 * • needLogin - 是否需要登录验证（默认true）
 *
 * 技术实现：
 * • 需配合拦截器或AOP切面实现具体限流逻辑
 * • 通常结合Redis或Guava Cache实现分布式限流
 * • 支持运行时注解解析
 */
package com.example.jpaspringboot.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // 注解在运行时保留，可通过反射读取
@Target(ElementType.METHOD) // 注解仅可用于方法级别
public @interface AccessLimit {
    int seconds(); // 限流时间窗口（单位：秒）
    int maxCount(); // 时间窗口内最大允许访问次数
    boolean needLogin() default true; // 是否需要登录验证（默认需要）
}