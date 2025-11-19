/**
 * 用户不存在异常类
 *
 * 功能概述：
 * • 表示用户查询操作中目标用户不存在的业务异常
 * • 继承自RuntimeException，属于非受检异常
 * • 用于用户认证、查询、管理等场景的异常处理
 *
 * 异常场景：
 * • 用户登录时用户名不存在
 * • 根据ID查询用户信息时用户不存在
 * • 用户权限验证时用户记录缺失
 *
 * 设计特点：
 * • 提供带消息参数的构造方法，支持自定义错误信息
 * • 保持异常链完整性，便于问题追踪
 * • 符合Spring异常处理规范，便于统一异常处理
 */
package com.example.jpaspringboot.exception;
/**
 * 构造用户不存在异常实例
 * @param message 异常描述信息，通常包含用户标识信息
 */
public class UserNotExistsException extends RuntimeException{
    public UserNotExistsException(String message) {
        super(message);
    }
}
