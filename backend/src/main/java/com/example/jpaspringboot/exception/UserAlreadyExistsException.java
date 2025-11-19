/**
 * 用户已存在业务异常类
 *
 * 功能概述：
 * • 处理用户注册时用户名重复的业务异常
 * • 继承运行时异常，支持非受检异常处理机制
 * • 提供清晰的异常信息用于前端展示和日志记录
 *
 * 使用场景：
 * • 用户注册时检测到用户名已存在
 * • 用户信息更新时发生唯一性约束冲突
 * • 批量导入用户数据时发现重复记录
 *
 * 异常处理：
 * • 在Service层抛出，由全局异常处理器统一捕获
 * • 返回标准化的错误响应格式给客户端
 * • 记录适当的日志用于问题追踪
 */
package com.example.jpaspringboot.exception;
/**
 * 构造用户已存在异常实例
 * @param message 异常描述信息，通常包含重复的用户标识
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

