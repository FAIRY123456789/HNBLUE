package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.service.ai.ThinkTagFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.sse.RealEventSource;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
    private static final String DONE = "[DONE]";

    private final ObjectMapper objectMapper;
    private final OkHttpClient client;

    @Value("${anythingllm.api.key}")
    private String apiKey;

    @Value("${anythingllm.api.url}")
    private String anythingllmUrl;

    @Value("${anythingllm.workspace}")
    private String workspace;

    public CarbonAssistantController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
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

        logger.info("AI请求开始 sessionId={} messageLength={}", safeSessionId, message == null ? 0 : message.length());
        sendQuietly(emitter, sse(Map.of("type", "status", "status", "connecting", "message", "正在连接 AI 碳助手…")));

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("mode", "chat");
        payload.put("sessionId", safeSessionId);

        Request request;
        try {
            request = new Request.Builder()
                    .url(normalizedAnythingLlmUrl() + "/v1/workspace/" + workspace + "/stream-chat")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "text/event-stream")
                    .addHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .addHeader("X-Accel-Buffering", "no")
                    .post(RequestBody.create(objectMapper.writeValueAsString(payload), MediaType.parse("application/json")))
                    .build();
        } catch (Exception e) {
            sendErrorAndComplete(state, "AI 请求创建失败");
            logger.error("AI请求创建失败 sessionId={}", safeSessionId, e);
            return emitter;
        }

        RealEventSource upstream = new RealEventSource(request, new EventSourceListener() {
            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                handleAnythingLlmEvent(state, data);
            }

            @Override
            public void onClosed(EventSource eventSource) {
                finishStream(state);
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response upstreamResponse) {
                String status = upstreamResponse == null ? "no-http-response" : String.valueOf(upstreamResponse.code());
                logger.error("AnythingLLM连接失败 sessionId={} httpStatus={} message={}",
                        safeSessionId, status, t == null ? "" : t.getMessage());
                sendErrorAndComplete(state, "AI 服务连接失败，请检查 Spring Boot 与 AnythingLLM。");
            }
        });

        state.setUpstream(upstream);
        emitter.onCompletion(() -> state.cancelUpstream("client-complete"));
        emitter.onTimeout(() -> {
            state.cancelUpstream("client-timeout");
            sendErrorAndComplete(state, "AI 服务响应超时，请稍后重试。");
        });
        emitter.onError(error -> state.cancelUpstream("client-error"));

        upstream.connect(client);
        return emitter;
    }

    private void handleAnythingLlmEvent(StreamState state, String data) {
        if (data == null || data.isBlank() || DONE.equals(data)) return;
        int current = state.chunkCount.incrementAndGet();
        int before = data.length();

        try {
            JsonNode node = objectMapper.readTree(data);
            if (node.path("error").asBoolean(false)) {
                logger.warn("AI上游错误事件 sessionId={} chunk={} type={}", state.sessionId, current, node.path("type").asText(""));
                sendErrorAndComplete(state, "AI 服务返回错误，请稍后重试。");
                return;
            }

            String eventType = node.path("type").asText("");
            switch (eventType) {
                case "textResponseChunk" -> handleDeltaEvent(state, node.path("textResponse").asText(""));
                case "textResponse" -> handleTextResponseEvent(state, node.path("textResponse").asText(""));
                case "finalizeResponseStream" -> handleFinalizeEvent(state, node);
                case "agentThought", "thought" -> handleThoughtEvent(state);
                case "abort" -> sendErrorAndComplete(state, "AI 回答已中断。");
                default -> handleCompatibleEvent(state, node);
            }
            logger.info("AI收到chunk数量={} 类型={} 过滤前长度={} 可见字符累计={}",
                    current, eventType, before, state.visibleChars.get());
        } catch (Exception e) {
            logger.warn("AI事件解析失败 sessionId={} chunk={} length={} message={}",
                    state.sessionId, current, before, e.getMessage());
            sendErrorAndComplete(state, "AI 服务返回格式异常。");
        }
    }

    private void handleDeltaEvent(StreamState state, String delta) {
        state.receivedDelta.set(true);
        String visible = state.thinkFilter.filter(delta);
        if (state.thinkFilter.isInsideThink() && visible.isEmpty()) {
            sendStatusOnce(state, "thinking", "正在分析问题并检索知识库…");
            return;
        }
        sendDelta(state, visible);
    }

    private void handleTextResponseEvent(StreamState state, String text) {
        if (state.receivedDelta.get()) return;
        sendDelta(state, state.thinkFilter.filter(text));
    }

    private void handleFinalizeEvent(StreamState state, JsonNode node) {
        if (state.metaSent.compareAndSet(false, true)) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("type", "meta");
            meta.put("sources", summarizeSources(node.path("sources")));
            Map<String, Object> metrics = compactMetrics(node.path("metrics"));
            if (!metrics.isEmpty()) meta.put("metrics", metrics);
            if (metrics.containsKey("model")) meta.put("model", metrics.get("model"));
            if (metrics.containsKey("duration")) meta.put("duration", metrics.get("duration"));
            copyIfPresent(meta, node, "model");
            copyIfPresent(meta, node, "duration");
            copyIfPresent(meta, node, "chatId");
            sendQuietly(state.emitter, sse(meta));
        }
        finishStream(state);
    }

    private void handleThoughtEvent(StreamState state) {
        sendStatusOnce(state, "thinking", "正在分析问题并检索知识库…");
    }

    private void handleCompatibleEvent(StreamState state, JsonNode node) {
        String delta = node.path("textResponse").asText("");
        if (!delta.isBlank() && !node.path("close").asBoolean(false)) {
            handleDeltaEvent(state, delta);
        }
    }

    private void sendDelta(StreamState state, String content) {
        if (content == null || content.isEmpty()) return;
        if (state.firstVisibleAt.compareAndSet(0L, System.currentTimeMillis())) {
            sendQuietly(state.emitter, sse(Map.of("type", "status", "status", "generating", "message", "正在生成回答…")));
        }
        state.visibleChars.addAndGet(content.length());
        sendQuietly(state.emitter, sse(Map.of("type", "delta", "content", content)));
    }

    private void finishStream(StreamState state) {
        if (!state.doneSent.compareAndSet(false, true)) return;
        String tail = state.thinkFilter.finish();
        sendDelta(state, tail);
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
        state.cancelUpstream("error");
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


    private List<Map<String, Object>> summarizeSources(JsonNode sources) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (sources == null || !sources.isArray()) return result;
        int count = 0;
        for (JsonNode source : sources) {
            if (count++ >= 8) break;
            Map<String, Object> item = new LinkedHashMap<>();
            putTextIfPresent(item, source, "title");
            putTextIfPresent(item, source, "url");
            putTextIfPresent(item, source, "docSource");
            putTextIfPresent(item, source, "chunkSource");
            putTextIfPresent(item, source, "published");
            if (!item.isEmpty()) result.add(item);
        }
        return result;
    }

    private Map<String, Object> compactMetrics(JsonNode metrics) {
        Map<String, Object> compact = new LinkedHashMap<>();
        if (metrics == null || !metrics.isObject()) return compact;
        for (String key : List.of("model", "provider", "duration", "prompt_tokens", "completion_tokens", "total_tokens", "outputTps")) {
            copyIfPresent(compact, metrics, key);
        }
        return compact;
    }

    private void putTextIfPresent(Map<String, Object> target, JsonNode node, String key) {
        if (node.has(key) && !node.get(key).isNull()) {
            String value = node.get(key).asText("");
            if (!value.isBlank()) target.put(key, value);
        }
    }
    private void copyIfPresent(Map<String, Object> meta, JsonNode node, String key) {
        if (node.has(key) && !node.get(key).isNull()) {
            meta.put(key, objectMapper.convertValue(node.get(key), Object.class));
        }
    }

    private String normalizedAnythingLlmUrl() {
        String base = anythingllmUrl == null ? "" : anythingllmUrl.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (base.endsWith("/api")) return base;
        return base + "/api";
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
        private final ThinkTagFilter thinkFilter = new ThinkTagFilter();
        private final AtomicInteger chunkCount = new AtomicInteger();
        private final AtomicLong visibleChars = new AtomicLong();
        private final AtomicLong firstVisibleAt = new AtomicLong();
        private final long startedAt = System.currentTimeMillis();
        private final AtomicBoolean receivedDelta = new AtomicBoolean(false);
        private final AtomicBoolean doneSent = new AtomicBoolean(false);
        private final AtomicBoolean metaSent = new AtomicBoolean(false);
        private volatile EventSource upstream;
        private volatile String lastStatus = "";

        private StreamState(SseEmitter emitter, String sessionId) {
            this.emitter = emitter;
            this.sessionId = sessionId;
        }

        private void setUpstream(EventSource upstream) {
            this.upstream = upstream;
        }

        private void cancelUpstream(String reason) {
            EventSource source = upstream;
            if (source != null) {
                logger.debug("关闭上游AI连接 sessionId={} reason={}", sessionId, reason);
                source.cancel();
            }
        }
    }
}
