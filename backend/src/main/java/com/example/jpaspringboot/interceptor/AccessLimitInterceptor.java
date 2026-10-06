/**
 * 接口访问频率限制拦截器
 *
 * 功能概述：
 * • 基于注解的接口访问频率控制
 * • 支持IP级别的请求限流和惩罚机制
 * • 防止恶意请求和API滥用
 *
 * 限流策略：
 * • Redis Lua 原子固定时间窗口计数算法
 * • 两级防护：频率检测 + 惩罚期
 * • 可配置时间窗口和最大请求次数
 *
 * 拦截逻辑：
 * 1. 检查方法级别的@AccessLimit注解
 * 2. 统计IP在时间窗口内的请求次数
 * 3. 超限请求进入惩罚期并返回429状态
 * 4. 惩罚期内所有请求直接拒绝
 */
package com.example.jpaspringboot.interceptor;

import com.example.jpaspringboot.service.AccessLimit;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.net.InetAddress;
import java.util.List;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AccessLimitInterceptor implements HandlerInterceptor {

    private static final int BAN_SECONDS = 60;
    private static final DefaultRedisScript<Long> ACCESS_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[2]) == 1 then
                return -1
            end
            local count = redis.call('INCR', KEYS[1])
            if count == 1 or redis.call('TTL', KEYS[1]) < 0 then
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
            end
            if count > tonumber(ARGV[2]) then
                redis.call('SET', KEYS[2], '1', 'EX', tonumber(ARGV[3]))
                return -1
            end
            return count
            """, Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 请求前置拦截处理
     *
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param handler 处理方法对象
     * @return 是否允许继续处理请求
     * @throws Exception 处理过程中的异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        AccessLimit accessLimit = handlerMethod.getMethodAnnotation(AccessLimit.class);
        if (accessLimit == null) {
            return true;
        }

        int seconds = accessLimit.seconds();
        int maxCount = accessLimit.maxCount();
        String key = resolveClientAddress(request) + ":" + request.getServletPath();
        String banKey = "ban:" + key;

        try {
            Long decision = stringRedisTemplate.execute(
                    ACCESS_LIMIT_SCRIPT,
                    List.of(key, banKey),
                    String.valueOf(seconds),
                    String.valueOf(maxCount),
                    String.valueOf(BAN_SECONDS)
            );
            if (decision == null) {
                throw new IllegalStateException("Redis rate-limit script returned null");
            }
            if (decision < 0) {
                rejectTooManyRequests(response);
                return false;
            }
            return true;
        } catch (Exception ex) {
            System.err.println("AccessLimit Redis unavailable, allow request: " + ex.getClass().getSimpleName());
            return true;
        }
    }

    private void rejectTooManyRequests(HttpServletResponse response) throws Exception {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write("{\"error\": \"请求过于频繁，请稍后再试\"}");
    }

    /**
     * Resolve the client address without trusting spoofable forwarding headers.
     * Production binds Spring Boot to loopback and Nginx overwrites X-Real-IP,
     * so the header is accepted only when the direct peer is loopback. Direct
     * development requests and any non-loopback peer keep their socket address.
     */
    String resolveClientAddress(HttpServletRequest request) {
        String remoteAddress = normalizeAddress(request.getRemoteAddr());
        if (!isLoopback(remoteAddress)) {
            return remoteAddress;
        }

        String nginxClientAddress = normalizeAddress(request.getHeader("X-Real-IP"));
        return isValidIpLiteral(nginxClientAddress) ? nginxClientAddress : remoteAddress;
    }

    private String normalizeAddress(String value) {
        return value == null ? "unknown" : value.trim();
    }

    private boolean isLoopback(String value) {
        if (!isValidIpLiteral(value)) {
            return false;
        }
        try {
            return InetAddress.getByName(value).isLoopbackAddress();
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isValidIpLiteral(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        // Reject host names before InetAddress parsing so resolution is never
        // performed on untrusted request input.
        if (!value.matches("^[0-9a-fA-F:.]+$")) {
            return false;
        }
        try {
            InetAddress.getByName(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
