package com.example.jpaspringboot.service.ai;

import com.example.jpaspringboot.repository.AiChatMessageRepository;
import com.example.jpaspringboot.repository.AiUserMemoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class AiConversationServiceTest {
    private final AiConversationService service = new AiConversationService(
            mock(AiChatMessageRepository.class), mock(AiUserMemoryRepository.class), new ObjectMapper());

    @Test
    void expandsShortReferenceWithPreviousUserQuestionLocally() {
        List<Map<String, String>> history = List.of(
                Map.of("role", "user", "content", "VM0033 2.1 是什么版本？"),
                Map.of("role", "assistant", "content", "回答"));
        assertEquals("为消解当前追问中的指代，请结合前一相关问题检索。\n前一相关问题：VM0033 2.1 是什么版本？\n当前问题：这个已经被 3.0 替代了吗？",
                service.contextualizeLocally("这个已经被 3.0 替代了吗？", history));
    }

    @Test
    void expandsOrdinalAndPartitiveReferences() {
        List<Map<String, String>> history = List.of(
                Map.of("role", "user", "content", "追溯时要看来源、时间、区域、质量和文件哈希。"),
                Map.of("role", "assistant", "content", "回答"));
        assertEquals("为消解当前追问中的指代，请结合前一相关问题检索。\n前一相关问题：追溯时要看来源、时间、区域、质量和文件哈希。\n当前问题：第二项为什么重要？",
                service.contextualizeLocally("第二项为什么重要？", history));
        assertEquals("为消解当前追问中的指代，请结合前一相关问题检索。\n前一相关问题：追溯时要看来源、时间、区域、质量和文件哈希。\n当前问题：其中一个没有入库怎么办？",
                service.contextualizeLocally("其中一个没有入库怎么办？", history));
        assertEquals("为消解当前追问中的指代，请结合前一相关问题检索。\n前一相关问题：追溯时要看来源、时间、区域、质量和文件哈希。\n当前问题：最后一项主要解决什么问题？",
                service.contextualizeLocally("最后一项主要解决什么问题？", history));
    }

    @Test
    void leavesStandaloneQuestionUnchanged() {
        assertEquals("什么是蓝碳？", service.contextualizeLocally("什么是蓝碳？",
                List.of(Map.of("role", "user", "content", "别的问题"))));
    }
}
