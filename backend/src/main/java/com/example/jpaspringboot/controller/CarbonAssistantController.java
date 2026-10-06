package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.service.ai.AiConversationService;
import com.example.jpaspringboot.service.ai.GroundedRagAnswerService;
import com.example.jpaspringboot.entity.AiChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("${chat.api.path}")
public class CarbonAssistantController {
    private static final Logger logger = LoggerFactory.getLogger(CarbonAssistantController.class);
    private final ObjectMapper objectMapper;
    private final OkHttpClient ragClient;

    @Autowired(required = false)
    private GroundedRagAnswerService groundedRagAnswerService;

    @Autowired(required = false)
    private AiConversationService aiConversationService;

    @Value("${rag.local.enabled:true}")
    private boolean localRagEnabled;

    @Value("${rag.api.url:http://127.0.0.1:8880}")
    private String ragApiUrl;

    public CarbonAssistantController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.ragClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(35, TimeUnit.SECONDS)
                .build();
    }

    @GetMapping("/knowledge-status")
    public Map<String, Object> knowledgeStatus() {
        Map<String, Object> localStatus = localRagStatus();
        if (Boolean.TRUE.equals(localStatus.get("configured"))) {
            String generation = groundedRagAnswerService == null ? "unavailable" : groundedRagAnswerService.generationMode();
            localStatus.put("generation", generation);
            localStatus.put("deepSeekConfigured", "deepseek-grounded".equals(generation));
            localStatus.put("memoryWindowMessages", 20);
            localStatus.put("historyStore", "mysql");
            localStatus.put("memoryPrivacy", "minimal-context-only; full history and secrets are not sent to DeepSeek");
            return localStatus;
        }
        localStatus.put("generation", groundedRagAnswerService == null
                ? "unavailable" : groundedRagAnswerService.generationMode());
        localStatus.put("deepSeekConfigured", groundedRagAnswerService != null
                && "deepseek-grounded".equals(groundedRagAnswerService.generationMode()));
        localStatus.put("memoryWindowMessages", 20);
        localStatus.put("historyStore", "mysql");
        localStatus.put("memoryPrivacy", "minimal-context-only; full history and secrets are not sent to DeepSeek");
        return localStatus;
    }

    @PostMapping(value = "/stream-carbon", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleGroundedCarbonStream(
            @org.springframework.web.bind.annotation.RequestBody ChatRequest requestBody,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletResponse response
    ) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        String message = requestBody == null ? "" : requestBody.message();
        String safeSessionId = normalizeSessionId(requestBody == null ? null : requestBody.sessionId());
        SseEmitter emitter = new SseEmitter(100_000L);
        StreamState state = new StreamState(emitter, safeSessionId);
        if (message == null || message.isBlank()) {
            sendErrorAndComplete(state, "请输入要查询的问题。");
            return emitter;
        }
        if (message.length() > 4000) {
            sendErrorAndComplete(state, "问题内容过长，请控制在 4000 字以内。");
            return emitter;
        }
        if (groundedRagAnswerService == null || aiConversationService == null) {
            sendErrorAndComplete(state, "HNBLUE 3.0 问答服务暂时不可用。");
            return emitter;
        }
        sendStatusOnce(state, "thinking", "正在检索证据并执行来源约束…");
        try {
            AiConversationService.Identity identity = aiConversationService.resolveIdentity(authorization, safeSessionId);
            List<Map<String, String>> recent = aiConversationService.recentMessages(identity, safeSessionId);
            String retrievalQuestion = aiConversationService.contextualizeLocally(message, recent);
            AiChatMessage userRow = aiConversationService.saveMessage(identity, safeSessionId, "user", message,
                    null, "pending", "", "none");
            GroundedRagAnswerService.Answer answer = groundedRagAnswerService.answer(message, retrievalQuestion);
            boolean blocked = "blocked_prompt_injection".equals(answer.evidenceStatus());
            aiConversationService.maybeRemember(identity, userRow, message, blocked);
            aiConversationService.saveMessage(identity, safeSessionId, "assistant", answer.text(), answer.sources(),
                    answer.evidenceStatus(), answer.model(), answer.injectionRisk());
            for (int offset = 0; offset < answer.text().length(); offset += 220) {
                sendDelta(state, answer.text().substring(offset, Math.min(answer.text().length(), offset + 220)));
            }
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("type", "meta");
            meta.put("provider", "hnblue-rag-3.0");
            meta.put("model", answer.model());
            meta.put("sources", answer.sources());
            meta.put("metrics", answer.metrics());
            meta.put("evidenceStatus", answer.evidenceStatus());
            meta.put("injectionRisk", answer.injectionRisk());
            meta.put("memoryWindowMessages", 20);
            meta.put("memoryPrivacy", "minimal-context-only");
            state.metaSent.set(true);
            sendQuietly(emitter, sse(meta));
            finishStream(state);
        } catch (Exception error) {
            logger.error("HNBLUE 3.0 问答失败 sessionId={} errorType={} message={}", safeSessionId,
                    error.getClass().getSimpleName(), error.getMessage());
            sendErrorAndComplete(state, "证据问答服务暂时不可用，请稍后重试。");
        }
        return emitter;
    }

    @GetMapping("/history")
    public Map<String, Object> history(@RequestParam String sessionId,
                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        String safe = normalizeSessionId(sessionId);
        if (aiConversationService == null) return Map.of("messages", List.of());
        AiConversationService.Identity identity = aiConversationService.resolveIdentity(authorization, safe);
        return Map.of("sessionId", safe, "messages", aiConversationService.history(identity, safe));
    }

    @DeleteMapping("/history")
    public Map<String, Object> clearHistory(@RequestParam String sessionId,
                                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String safe = normalizeSessionId(sessionId);
        if (aiConversationService == null) return Map.of("deleted", 0);
        AiConversationService.Identity identity = aiConversationService.resolveIdentity(authorization, safe);
        return Map.of("deleted", aiConversationService.clear(identity, safe));
    }

    @GetMapping(value = "/stream-carbon", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleCarbonStream(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId,
            HttpServletResponse response
    ) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        SseEmitter emitter = new SseEmitter(0L);
        String safeSessionId = normalizeSessionId(sessionId);
        StreamState state = new StreamState(emitter, safeSessionId);

        if (message == null || message.isBlank()) {
            sendErrorAndComplete(state, "请输入要查询的问题。");
            return emitter;
        }
        if (message.length() > 4000) {
            sendErrorAndComplete(state, "问题内容过长，请控制在 4000 字以内。");
            return emitter;
        }
        if (localRagEnabled) {
            if (handleLocalRagStream(state, message)) return emitter;
            logger.warn("本地RAG请求失败 sessionId={}", safeSessionId);
        }
        sendErrorAndComplete(state, "本地知识库服务暂时不可用；公开数据与来源查询仍可正常使用。");
        return emitter;
    }

    private boolean handleLocalRagStream(StreamState state, String message) {
        sendStatusOnce(state, "thinking", "正在检索 HNBLUE 本地知识库…");
        try {
            Map<String, Object> payload = Map.of("message", message, "topK", 5);
            Request request = new Request.Builder()
                    .url(normalizedRagUrl() + "/api/rag/answer")
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(objectMapper.writeValueAsString(payload), MediaType.parse("application/json")))
                    .build();
            try (Response response = ragClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return false;
                JsonNode body = objectMapper.readTree(response.body().string());
                if (!body.path("ok").asBoolean(false)) return false;
                String answer = body.path("answer").asText("");
                if (answer.isBlank()) return false;
                for (int offset = 0; offset < answer.length(); offset += 240) {
                    sendDelta(state, answer.substring(offset, Math.min(answer.length(), offset + 240)));
                }
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("type", "meta");
                meta.put("provider", body.path("provider").asText("hnblue-local-hybrid-rag"));
                meta.put("sources", objectMapper.convertValue(body.path("sources"), List.class));
                meta.put("metrics", objectMapper.convertValue(body.path("metrics"), Map.class));
                meta.put("evidenceStatus", body.path("evidenceStatus").asText("retrieved"));
                state.metaSent.set(true);
                sendQuietly(state.emitter, sse(meta));
                finishStream(state);
                return true;
            }
        } catch (Exception error) {
            logger.error("本地RAG连接失败 sessionId={} errorType={} message={}",
                    state.sessionId, error.getClass().getSimpleName(), error.getMessage());
            return false;
        }
    }

    private Map<String, Object> localRagStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "hnblue-local-hybrid-rag");
        result.put("configured", false);
        result.put("status", localRagEnabled ? "unavailable" : "disabled");
        result.put("publicDataFallback", true);
        if (!localRagEnabled) return result;
        try {
            Request request = new Request.Builder()
                    .url(normalizedRagUrl() + "/api/rag/status")
                    .addHeader("Accept", "application/json")
                    .get()
                    .build();
            try (Response response = ragClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return result;
                JsonNode body = objectMapper.readTree(response.body().string());
                if (!body.path("ok").asBoolean(false)) return result;
                result.put("configured", true);
                result.put("status", "ready");
                result.put("chunks", body.path("chunks").asInt(0));
                result.put("sourceFiles", body.path("sourceFiles").asInt(0));
                result.put("indexSha256", body.path("indexSha256").asText(""));
                result.put("retrievalModes", objectMapper.convertValue(body.path("retrievalModes"), List.class));
                return result;
            }
        } catch (Exception error) {
            logger.warn("本地RAG状态检查失败 errorType={}", error.getClass().getSimpleName());
            return result;
        }
    }

    private String normalizedRagUrl() {
        String base = ragApiUrl == null ? "" : ragApiUrl.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base;
    }

    private void sendDelta(StreamState state, String content) {
        if (content == null || content.isEmpty()) return;
        state.chunkCount.incrementAndGet();
        if (state.firstVisibleAt.compareAndSet(0L, System.currentTimeMillis())) {
            sendQuietly(state.emitter, sse(Map.of("type", "status", "status", "generating", "message", "正在生成回答…")));
        }
        state.visibleChars.addAndGet(content.length());
        sendQuietly(state.emitter, sse(Map.of("type", "delta", "content", content)));
    }

    private void finishStream(StreamState state) {
        if (!state.doneSent.compareAndSet(false, true)) return;
        long totalMs = System.currentTimeMillis() - state.startedAt;
        long firstVisibleMs = state.firstVisibleAt.get() == 0L ? -1L : state.firstVisibleAt.get() - state.startedAt;
        sendQuietly(state.emitter, sse(Map.of(
                "type", "done",
                "eventCount", state.chunkCount.get(),
                "visibleChars", state.visibleChars.get(),
                "firstVisibleMs", firstVisibleMs,
                "totalMs", totalMs
        )));
        logger.info("AI连接关闭 sessionId={} eventCount={} visibleChars={} firstVisibleMs={} totalMs={}",
                state.sessionId, state.chunkCount.get(), state.visibleChars.get(), firstVisibleMs, totalMs);
        state.emitter.complete();
    }

    private void sendErrorAndComplete(StreamState state, String message) {
        if (state.doneSent.get()) return;
        sendQuietly(state.emitter, sse(Map.of("type", "error", "message", message)));
        finishStream(state);
    }

    private void sendStatusOnce(StreamState state, String status, String label) {
        if (state.lastStatus.equals(status)) return;
        state.lastStatus = status;
        sendQuietly(state.emitter, sse(Map.of("type", "status", "status", status, "message", label)));
    }

    private void sendQuietly(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException ignored) {
            emitter.complete();
        }
    }

    private SseEmitter.SseEventBuilder sse(Object payload) {
        return SseEmitter.event().data(payload);
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return UUID.randomUUID().toString();
        String safe = sessionId.replaceAll("[^a-zA-Z0-9_-]", "");
        if (safe.isBlank()) return UUID.randomUUID().toString();
        return safe.substring(0, Math.min(64, safe.length()));
    }

    private static class StreamState {
        private final SseEmitter emitter;
        private final String sessionId;
        private final AtomicInteger chunkCount = new AtomicInteger();
        private final AtomicLong visibleChars = new AtomicLong();
        private final AtomicLong firstVisibleAt = new AtomicLong();
        private final long startedAt = System.currentTimeMillis();
        private final AtomicBoolean doneSent = new AtomicBoolean(false);
        private final AtomicBoolean metaSent = new AtomicBoolean(false);
        private volatile String lastStatus = "";

        private StreamState(SseEmitter emitter, String sessionId) {
            this.emitter = emitter;
            this.sessionId = sessionId;
        }
    }

    public record ChatRequest(String message, String sessionId) {}
}
