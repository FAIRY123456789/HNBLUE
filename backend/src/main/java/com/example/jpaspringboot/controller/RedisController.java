/**
 * Redis数据操作控制器
 *
 * 功能概述：
 * • 提供Redis缓存数据的读写操作接口
 * • 演示Spring Data Redis的基本使用方法
 * • 提供简单的健康检查端点
 *
 * 接口说明：
 * • /setRedisData - 设置Redis键值对数据
 * • /getRedisData - 获取Redis键对应的值
 * • /hello - 应用健康检查端点
 *
 * 技术特性：
 * • 使用RedisTemplate进行Redis操作
 * • 支持任意Java对象的序列化存储
 * • 自动异常处理和连接管理
 *
 * 使用场景：
 * • 缓存数据管理调试
 * • Redis连接功能验证
 * • 应用健康状态监控
 */
package com.example.jpaspringboot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedisController {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 设置Redis键值对数据
     * @param key 存储键
     * @param value 存储值（支持任意可序列化对象）
     */
    @GetMapping("/setRedisData")
    public void setRedisData(@RequestParam String key, @RequestParam Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 获取Redis键对应的值
     * @param key 查询键
     * @return 键值对格式的字符串结果
     */
    @GetMapping("/getRedisData")
    public String getRedisData(@RequestParam String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return key + " --->>> " + value;
    }

    /**
     * 应用健康检查端点
     * @return 固定的健康响应字符串
     */
    @GetMapping("/hello")
    public String hello(){
        return "nihao";
    }
}