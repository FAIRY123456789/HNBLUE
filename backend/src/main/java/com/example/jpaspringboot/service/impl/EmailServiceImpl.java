/**
 * 邮件服务实现类
 *
 * 功能概述：
 * • 基于Spring Mail提供邮件发送能力
 * • 实现简单的文本邮件发送功能
 * • 封装邮件发送的底层技术细节
 *
 * 技术实现：
 * • 使用JavaMailSender进行邮件传输
 * • 支持SMTP协议邮件发送
 * • 配置发件人邮箱地址固定
 *
 * 使用场景：
 * • 用户注册验证码发送
 * • 系统通知消息推送
 * • 业务状态变更提醒
 * • 密码重置邮件发送
 *
 * 配置依赖：
 * • Spring Boot Mail Starter
 * • SMTP服务器配置（QQ邮箱）
 * • 发件人认证信息配置
 */
package com.example.jpaspringboot.service.impl;
import com.example.jpaspringboot.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;  // Spring邮件发送器，自动注入配置的邮件服务器信息

    @Value("${spring.mail.username:}")
    private String fromAddress;

    /**
     * 发送简单的文本邮件
     * 使用SimpleMailMessage构建基础邮件内容，支持纯文本格式
     *
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param content 邮件正文内容
     */
    public void send(String to, String subject, String content) {
        // 创建简单邮件消息对象
        SimpleMailMessage message = new SimpleMailMessage();

        // 发件人由环境配置提供，并应与 SMTP 认证账号一致。
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("MAIL_USERNAME is required before sending email");
        }
        message.setFrom(fromAddress);

        // 设置收件人邮箱地址
        message.setTo(to);

        // 设置邮件主题
        message.setSubject(subject);

        // 设置邮件正文内容
        message.setText(content);

        // 执行邮件发送操作
        mailSender.send(message);
    }
}
