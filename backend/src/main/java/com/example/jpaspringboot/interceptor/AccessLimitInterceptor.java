/**
 * 接口访问频率限制拦截器
 *
 * 功能概述：
 * • 基于注解的接口访问频率控制
 * • 支持IP级别的请求限流和惩罚机制
 * • 防止恶意请求和API滥用
 *
 * 限流策略：
 * • 滑动时间窗口计数算法
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

import com.example.jpaspringboot.util.RedisUtil;
import com.example.jpaspringboot.service.AccessLimit;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AccessLimitInterceptor implements HandlerInterceptor {

    @Resource
    private RedisUtil redisUtil;

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
        String key = request.getRemoteAddr() + ":" + request.getServletPath();
        String banKey = "ban:" + key;

        try {
            Object banVal = redisUtil.get(banKey);
            if (banVal != null) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"error\": \"请求过于频繁，请稍后再试\"}");
                return false;
            }

            Integer count = null;
            Object countVal = redisUtil.get(key);
            if (countVal != null) {
                count = Integer.parseInt(countVal.toString());
            }

            if (count == null || count == -1) {
                redisUtil.set(key, 1);
                redisUtil.expire(key, seconds);
                return true;
            }

            if (count < maxCount) {
                redisUtil.incr(key, 1);
                return true;
            }

            redisUtil.set(banKey, 1);
            redisUtil.expire(banKey, 60);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"error\": \"请求过于频繁，请稍后再试\"}");
            return false;
        } catch (Exception ex) {
            System.err.println("AccessLimit Redis unavailable, allow request: " + ex.getClass().getSimpleName());
            return true;
        }
    }
}
