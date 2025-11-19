/**
 * AI对话结果增量数据传输对象
 *
 * 功能概述：
 * • 封装AI对话流式响应中的增量数据片段
 * • 用于OpenAI等流式API的delta字段映射
 * • 支持角色和内容的分段传输
 *
 * 字段说明：
 * • role - 对话角色标识（user/assistant/system）
 * • content - 对话内容文本片段
 *
 * 使用场景：
 * • AI流式对话接口响应解析
 * • 实时聊天消息分段处理
 * • 大语言模型流式输出处理
 *
 * 设计说明：
 * • 纯数据传输对象，不包含业务逻辑
 * • 字段命名与OpenAI API保持一致
 * • 支持JSON序列化/反序列化
 */
package com.example.jpaspringboot.dto;

public class AiResultDelta {

    private String role;
    private String content;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
