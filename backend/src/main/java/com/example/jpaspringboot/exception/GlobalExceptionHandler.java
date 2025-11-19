/**
 * 全局异常处理器
 *
 * 功能概述：
 * • 统一处理控制器层抛出的业务异常
 * • 标准化REST API异常响应格式
 * • 提供跨控制器的异常处理机制
 *
 * 异常处理策略：
 * • UserAlreadyExistsException - 用户已存在异常（HTTP 409 Conflict）
 * • 可扩展其他业务异常处理方法
 *
 * 设计特性：
 * • 使用@RestControllerAdvice实现全局异常拦截
 * • 返回标准HTTP状态码和错误信息
 * • 避免异常信息直接暴露给客户端
 *
 * 使用场景：
 * • 用户注册时用户名重复
 * • 数据唯一性约束冲突
 * • 业务规则校验失败
 */
package com.example.jpaspringboot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // 声明为全局REST控制器异常处理器
public class GlobalExceptionHandler {

    /**
     * 处理用户已存在异常
     * @param e 用户已存在异常实例
     * @return HTTP 409 Conflict响应，包含错误信息
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<?> handleUserAlreadyExists(UserAlreadyExistsException e) {
        // 用户名已存在
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    // 其他异常处理方法可在此扩展...
}

