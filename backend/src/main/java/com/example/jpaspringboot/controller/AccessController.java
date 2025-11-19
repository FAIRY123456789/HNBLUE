/**
 * 接口访问频率限制测试控制器
 *
 * 功能概述：
 * • 提供访问频率限制功能的测试接口
 * • 演示自定义注解@AccessLimit的实际应用
 * • 验证拦截器对接口访问的限流效果
 *
 * 技术实现：
 * • 使用自定义@AccessLimit注解标记需要限流的接口
 * • 通过拦截器实现注解解析和访问频率控制
 * • 支持参数化配置限流时间和最大访问次数
 *
 * 限流配置：
 * • seconds = 3 - 时间窗口为3秒
 * • maxCount = 5 - 窗口期内最大允许5次访问
 *
 * 测试用途：
 * • 验证拦截器限流逻辑的正确性
 * • 测试系统在高频访问下的稳定性
 * • 演示注解驱动编程的实际应用
 */
package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.service.AccessLimit;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("access")
public class AccessController {

    /**
     * 访问频率限制测试接口
     *
     * 方法说明：
     * • 使用@AccessLimit注解实现接口级访问控制
     * • 拦截器会拦截该方法并执行限流逻辑
     * • 在3秒时间窗口内最多允许5次访问
     *
     * 限流流程：
     * 1. 拦截器检测方法上的@AccessLimit注解
     * 2. 根据注解参数初始化限流器
     * 3. 验证当前访问是否超出频率限制
     * 4. 超出限制则抛出异常，否则正常执行
     *
     * @return 固定成功响应字符串
     */
    @ResponseBody
    @GetMapping("accessLimit")
    @AccessLimit(seconds = 3, maxCount = 5)
    public String accessLimit(){
        return "It is ok";
    }
}