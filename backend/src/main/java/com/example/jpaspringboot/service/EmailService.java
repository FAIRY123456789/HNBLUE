package com.example.jpaspringboot.service;

public interface EmailService {
    /**
     * 发送简单的文本邮件
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void send(String to, String subject, String content);


}
