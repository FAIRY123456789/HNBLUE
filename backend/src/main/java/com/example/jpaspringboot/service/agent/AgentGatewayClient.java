package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.agent.AgentGatewayContract;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InterruptedIOException;

@Service
public class AgentGatewayClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient http;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final boolean enabled;
    private final String internalToken;

    public AgentGatewayClient(OkHttpClient agentOkHttpClient, ObjectMapper mapper,
                              @Value("${hnblue.agent.gateway.url:http://127.0.0.1:8890}") String baseUrl,
                              @Value("${hnblue.agent.gateway.enabled:false}") boolean enabled,
                              @Value("${hnblue.agent.gateway.internal-token:}") String internalToken) {
        this.http = agentOkHttpClient;
        this.mapper = mapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.enabled = enabled;
        this.internalToken = internalToken;
    }

    @CircuitBreaker(name = "agentService", fallbackMethod = "fallback")
    @Bulkhead(name = "agentService", type = Bulkhead.Type.SEMAPHORE)
    @Retry(name = "agentService")
    public AgentGatewayContract.Result execute(AgentGatewayContract.Request payload) throws IOException {
        return send(AgentGatewayContract.Command.execute(payload));
    }

    @CircuitBreaker(name = "agentService", fallbackMethod = "reviewFallback")
    @Bulkhead(name = "agentService", type = Bulkhead.Type.SEMAPHORE)
    @Retry(name = "agentService")
    public AgentGatewayContract.Result review(AgentGatewayContract.ReviewCommand payload) throws IOException {
        return send(AgentGatewayContract.Command.review(payload));
    }

    private AgentGatewayContract.Result send(AgentGatewayContract.Command command) throws IOException {
        if (!enabled) return AgentGatewayContract.Result.failed("gateway_disabled", "Python Agent gateway is disabled");
        RequestBody body = RequestBody.create(mapper.writeValueAsBytes(command), JSON);
        Request.Builder request = new Request.Builder()
                .url(baseUrl + "/v1/agent/tasks/command")
                .post(body)
                .header("X-Trace-Id", command.traceId())
                .header("X-HNBLUE-Service-Scope", "agent.command");
        if (!internalToken.isBlank()) request.header("X-HNBLUE-Internal-Token", internalToken);
        try (Response response = http.newCall(request.build()).execute()) {
            if (!response.isSuccessful()) {
                String code = response.code() == 429 ? "gateway_rate_limited"
                        : response.code() >= 500 ? "gateway_5xx" : "gateway_http_error";
                throw new AgentGatewayHttpException(code, response.code());
            }
            if (response.body() == null) throw new IOException("Agent gateway returned an empty body");
            return mapper.readValue(response.body().bytes(), AgentGatewayContract.Result.class);
        }
    }

    public AgentGatewayContract.Result fallback(AgentGatewayContract.Request payload, Throwable error) {
        return failure(error);
    }

    public AgentGatewayContract.Result reviewFallback(AgentGatewayContract.ReviewCommand payload, Throwable error) {
        return failure(error);
    }

    private AgentGatewayContract.Result failure(Throwable error) {
        Throwable cause = error;
        while (cause != null) {
            if (cause instanceof AgentGatewayHttpException httpError) {
                return AgentGatewayContract.Result.failed(httpError.errorCode(), httpError.getMessage());
            }
            if (cause instanceof InterruptedIOException) {
                return AgentGatewayContract.Result.failed("gateway_timeout", cause.getClass().getSimpleName());
            }
            if (cause.getCause() == cause) break;
            cause = cause.getCause();
        }
        return AgentGatewayContract.Result.failed("gateway_unavailable", error.getClass().getSimpleName());
    }

    static final class AgentGatewayHttpException extends IOException {
        private final String errorCode;

        AgentGatewayHttpException(String errorCode, int status) {
            super("Agent gateway HTTP " + status);
            this.errorCode = errorCode;
        }

        String errorCode() { return errorCode; }
    }
}
