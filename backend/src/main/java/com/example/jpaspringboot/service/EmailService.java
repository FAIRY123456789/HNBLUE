/**
 * 邮件服务接口定义
 *
 * 功能概述：
 * • 提供邮件发送能力的抽象接口
 * • 定义统一的邮件服务契约
 * • 支持简单文本邮件的发送功能
 *
 * 核心能力：
 * • 单收件人邮件发送
 * • 自定义邮件主题和内容
 * • 文本格式邮件支持
 *
 * 设计目的：
 * • 抽象邮件发送实现细节
 * • 支持多种邮件服务提供商
 * • 提供一致的邮件服务API
 *
 * 扩展方向：
 * • 支持HTML格式邮件
 * • 增加附件发送能力
 * • 支持批量邮件发送
 * • 添加邮件模板支持
 */
package com.example.jpaspringboot.service;

public interface EmailService {
    /**
     * 发送简单的文本邮件
     *
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param content 邮件正文内容
     */
    void send(String to, String subject, String content);
}