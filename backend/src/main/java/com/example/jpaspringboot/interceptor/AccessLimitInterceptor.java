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
        // 仅拦截方法级别的处理器
        if (handler instanceof HandlerMethod){
            HandlerMethod handlerMethod=(HandlerMethod)handler;
            // 获取方法上的限流注解配置
            AccessLimit accessLimit = handlerMethod.getMethodAnnotation(AccessLimit.class);
            // 无限流注解的方法直接放行
            if (accessLimit==null){
                return true;
            }
            // 解析限流参数
            int seconds = accessLimit.seconds();        // 时间窗口长度（秒）
            int maxCount = accessLimit.maxCount();      // 窗口内最大请求次数
            boolean needLogin = accessLimit.needLogin(); // 是否需要登录验证

            if (needLogin){
                // 判断是否登录，拦截器不会拦截已登录的url
            }

            // 获取路径
            String ip = request.getRemoteAddr();
            // 将key设置为http://ip:/url的格式，只需ip，不需要添加接口，因为接口一旦限定，剩下的接口都不会被拦截
            String key = ip + ":" + request.getServletPath();

            // 检查惩罚期状态
            String banKey = "ban:" + key; // 惩罚期键名
            Object banVal = redisUtil.get(banKey);
            if (banVal != null) {
                // 惩罚期内直接返回429状态
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"error\": \"请求过于频繁，请稍后再试\"}");
                return false;
            }

            // 获取当前请求计数
            Integer count = null;
            System.out.println();
            if (redisUtil.get(key)!=null){
                count = Integer.parseInt(redisUtil.get(key).toString());
            }

            // 首次访问初始化计数器
            if (count==null||count==-1){
                redisUtil.set(key,1);
                //设置过期时间
                redisUtil.expire(key,seconds);
                return true;
            }

            //如果访问次数<最大次数，则value+1
            if (count<maxCount){
                redisUtil.incr(key,1);
                return true;
            }

            //如果访问次数≥最大次数
            if (count >= maxCount) {
                // 触发阈值后，进入惩罚期（全部 429），不改返回体
                int banSeconds = 60; // 惩罚期时长（可按需调整，如 seconds*10）
                redisUtil.set(banKey, 1);
                redisUtil.expire(banKey, banSeconds); // 设置惩罚期过期时间

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 设置状态码为429
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"error\": \"请求过于频繁，请稍后再试\"}");
                return false;
            }
        }
        return true;
    }
}
