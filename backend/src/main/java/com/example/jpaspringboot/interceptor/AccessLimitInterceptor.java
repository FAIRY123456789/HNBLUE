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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod){ //只用来处理方法
            HandlerMethod handlerMethod=(HandlerMethod)handler;
            //尝试拿到该方法的限流注解
            AccessLimit accessLimit = handlerMethod.getMethodAnnotation(AccessLimit.class);
            //如果没拿到，就说明这个方法不需要限流
            if (accessLimit==null){
                return true;
            }
            int seconds = accessLimit.seconds();
            int maxCount = accessLimit.maxCount();
            boolean needLogin = accessLimit.needLogin();

            if (needLogin){
                //判断是否登录，拦截器不会拦截已登录的url
            }

            //获取路径
            String ip = request.getRemoteAddr();
            //将key设置为http://ip:/url的格式，只需要看ip就行，不需要添加接口，接口一旦限定，那剩下的接口都不会被拦截了
            String key = ip + ":" + request.getServletPath();

            Integer count = null;
            System.out.println();
            if (redisUtil.get(key)!=null){
                count = Integer.parseInt(redisUtil.get(key).toString());
            }
            //首次访问
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
//当用户的请求频率超过了限制，拦截器将返回一个带有429状态码的HTTP响应，以及一个包含错误消息的JSON对象。
//这将使前端能够根据返回的状态码和消息来更新用户界面。
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 设置状态码为429
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"error\": \"请求过于频繁，请稍后再试\"}");
                return false;
            }
        }
        return true;
    }
}
