/**
 * 内容数据传输对象
 *
 * 功能概述：
 * • 封装文本内容数据的简单DTO
 * • 用于API层与业务层之间的数据传输
 * • 提供内容字段的标准化访问接口
 *
 * 设计用途：
 * • REST API请求/响应的内容载体
 * • 消息内容、文本数据等字符串信息的传输
 * • 简化复杂对象结构，聚焦核心内容字段
 *
 * 使用场景：
 * • 用户输入内容接收
 * • 文本消息传递
 * • 简单配置信息传输
 * • 通用内容包装容器
 */
package com.example.jpaspringboot.dto;

public class ContentDto {
    private String content;

    public ContentDto(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
