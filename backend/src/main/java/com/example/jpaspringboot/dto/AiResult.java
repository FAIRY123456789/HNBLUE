/**
 * AI服务响应数据转换对象
 *
 * 功能概述：
 * • 封装第三方AI服务API的标准化响应结构
 * • 映射JSON响应到Java对象，便于数据提取和处理
 * • 提供完整的响应状态和结果内容承载
 *
 * 字段说明：
 * • code - 业务状态码，标识请求处理结果
 * • message - 响应消息描述，包含成功或错误信息
 * • sid - 会话标识符，用于追踪对话上下文
 * • id - 请求唯一标识，用于日志追踪和调试
 * • created - 响应创建时间戳（Unix时间戳格式）
 * • choices - AI生成的结果选项列表，包含多个候选回复
 *
 * 使用场景：
 * • 大语言模型API响应解析
 * • 智能对话服务结果封装
 * • AI内容生成服务数据接收
 *
 * 数据流：
 * AI服务JSON响应 → 反序列化为AiResult对象 → 业务逻辑处理
 */
package com.example.jpaspringboot.dto;

import java.util.List;

public class AiResult {
    private Integer code;
    private String message;
    private String sid;
    private String id;
    private Long created;
    private List<AiResultChoices> choices;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public List<AiResultChoices> getChoices() {
        return choices;
    }

    public void setChoices(List<AiResultChoices> choices) {
        this.choices = choices;
    }
}
