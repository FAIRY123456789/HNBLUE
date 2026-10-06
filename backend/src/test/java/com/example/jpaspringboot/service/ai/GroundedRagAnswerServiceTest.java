package com.example.jpaspringboot.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroundedRagAnswerServiceTest {
    private final GroundedRagAnswerService service = new GroundedRagAnswerService(
            new ObjectMapper(), Mockito.mock(ApiSecretService.class));

    @Test
    void acceptsOnlyProvidedCitationNumbers() {
        assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(service, "validCitations", "结论 [S1]", 2)));
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(service, "validCitations", "没有引用", 2)));
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(service, "validCitations", "伪造 [S3]", 2)));
    }

    @Test
    void sendsOnlyMinimalRewrittenContextForReferenceResolution() {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) ReflectionTestUtils.invokeMethod(
                service,
                "buildMessages",
                "第二项为什么重要？",
                "前一相关问题：追溯要看来源、时间、区域、质量和文件哈希。\n当前问题：第二项为什么重要？",
                List.of(Map.of(
                        "title", "来源追溯",
                        "section", "字段",
                        "source_path", "source_traceability.md",
                        "content", "时间字段用于判断时效。")));
        assertTrue(messages.get(messages.size() - 1).get("content").contains("最小上下文"));
        assertTrue(messages.get(messages.size() - 1).get("content").contains("前一相关问题"));
        assertTrue(messages.get(0).get("content").contains("含义不同"));
    }
}
