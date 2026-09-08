package com.example.jpaspringboot.service.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ThinkTagFilterTest {
    @Test
    void removesCompleteThinkTagInOneChunk() {
        ThinkTagFilter filter = new ThinkTagFilter();
        assertEquals("蓝碳是指海洋生态系统固定的碳。", filter.filter("<think>分析过程</think>蓝碳是指海洋生态系统固定的碳。"));
        assertEquals("", filter.finish());
    }

    @Test
    void handlesSplitStartTagAtEveryCharacter() {
        ThinkTagFilter filter = new ThinkTagFilter();
        assertEquals("前", filter.filter("前<"));
        assertEquals("", filter.filter("th"));
        assertEquals("", filter.filter("ink>隐藏"));
        assertEquals("后", filter.filter("</think>后"));
    }

    @Test
    void handlesSplitEndTagAtEveryCharacter() {
        ThinkTagFilter filter = new ThinkTagFilter();
        assertEquals("", filter.filter("<think>隐藏</"));
        assertEquals("", filter.filter("thi"));
        assertEquals("可见", filter.filter("nk>可见"));
    }

    @Test
    void keepsTextBeforeAndAfterThinkBlock() {
        ThinkTagFilter filter = new ThinkTagFilter();
        assertEquals("A", filter.filter("A<think>B"));
        assertEquals("C", filter.filter("</think>C"));
    }

    @Test
    void keepsTextWithoutThinkTag() {
        ThinkTagFilter filter = new ThinkTagFilter();
        assertEquals("没有推理标签的回答", filter.filter("没有推理标签的回答"));
        assertEquals("", filter.finish());
    }

    @Test
    void doesNotRemoveNormalAngleBracketsInMarkdownCode() {
        ThinkTagFilter filter = new ThinkTagFilter();
        String markdown = "```json\n{\"html\": \"<div>ok</div>\"}\n```";
        assertEquals(markdown, filter.filter(markdown));
    }

    @Test
    void handlesEmptyChunk() {
        ThinkTagFilter filter = new ThinkTagFilter();
        assertEquals("", filter.filter(""));
        assertEquals("", filter.filter(null));
    }

    @Test
    void handlesChineseAndMultilineText() {
        ThinkTagFilter filter = new ThinkTagFilter();
        assertEquals("第一行\n", filter.filter("第一行\n<think>内部\n推理"));
        assertEquals("第二行", filter.filter("</think>第二行"));
    }

    @Test
    void emitsTextImmediatelyAfterThinkEnd() {
        ThinkTagFilter filter = new ThinkTagFilter();
        assertEquals("蓝碳是指", filter.filter("<think>分析</think>蓝碳是指"));
    }

    @Test
    void unfinishedVisibleAngleBracketIsFlushedButUnfinishedThinkIsDiscarded() {
        ThinkTagFilter filter = new ThinkTagFilter();
        assertEquals("比较 ", filter.filter("比较 <"));
        assertEquals("<", filter.finish());

        ThinkTagFilter thinking = new ThinkTagFilter();
        assertEquals("", thinking.filter("<think>未结束推理"));
        assertFalse(thinking.finish().contains("未结束推理"));
    }

    @Test
    void finalizeFullAnswerShouldNotBePassedToFilterAgain() {
        ThinkTagFilter filter = new ThinkTagFilter();
        assertEquals("蓝碳", filter.filter("蓝碳"));
        // finalizeResponseStream is handled by CarbonAssistantController as meta/done,
        // so the complete answer is intentionally not sent through this filter again.
        assertEquals("", filter.finish());
    }
}
