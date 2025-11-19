/**
 * AI对话结果选项数据传输对象
 *
 * 功能概述：
 * • 封装AI对话API返回的单个响应选项数据
 * • 用于解析流式对话接口的增量响应内容
 * • 维护对话选项的索引位置和内容增量
 *
 * 数据结构：
 * • delta - 对话内容增量，包含角色和消息体
 * • index - 选项在响应中的排序索引位置
 *
 * 使用场景：
 * • 大语言模型流式对话响应解析
 * • 多轮对话选项结果封装
 * • AI助手消息增量更新处理
 *
 * 关联类：
 * • AiResultDelta - 对话内容增量数据结构
 * • AiResult - 完整AI响应包装类
 */
package com.example.jpaspringboot.dto;

public class AiResultChoices {
    private AiResultDelta delta;
    private Integer index;

    public AiResultDelta getDelta() {
        return delta;
    }

    public void setDelta(AiResultDelta delta) {
        this.delta = delta;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }
}
