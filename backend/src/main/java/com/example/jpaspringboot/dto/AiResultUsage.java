/**
 * AI服务令牌使用量统计数据传输对象
 *
 * 功能概述：
 * • 封装AI API调用过程中的令牌消耗统计信息
 * • 用于记录和分析AI服务使用成本
 * • 提供令牌使用量的结构化数据承载
 *
 * 字段说明：
 * • prompt_tokens - 提示词消耗的令牌数量
 * • completion_tokens - 补全内容消耗的令牌数量
 * • total_tokens - 本次调用消耗的令牌总数
 *
 * 使用场景：
 * • AI API调用响应的使用量统计
 * • 服务计费和成本核算
 * • 使用量监控和配额管理
 * • 性能分析和优化参考
 *
 * 数据关系：
 * • total_tokens = prompt_tokens + completion_tokens
 * • 令牌数量与API调用成本直接相关
 */
package com.example.jpaspringboot.dto;

public class AiResultUsage {
    private Integer prompt_tokens;
    private Integer completion_tokens;
    private Integer total_tokens;

    public Integer getPrompt_tokens() {
        return prompt_tokens;
    }

    public void setPrompt_tokens(Integer prompt_tokens) {
        this.prompt_tokens = prompt_tokens;
    }

    public Integer getCompletion_tokens() {
        return completion_tokens;
    }

    public void setCompletion_tokens(Integer completion_tokens) {
        this.completion_tokens = completion_tokens;
    }

    public Integer getTotal_tokens() {
        return total_tokens;
    }

    public void setTotal_tokens(Integer total_tokens) {
        this.total_tokens = total_tokens;
    }
}
