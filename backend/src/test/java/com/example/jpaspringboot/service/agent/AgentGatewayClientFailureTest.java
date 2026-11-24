package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.agent.AgentCapability;
import com.example.jpaspringboot.agent.AgentGatewayContract;
import com.example.jpaspringboot.config.AgentExecutionConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentGatewayClientFailureTest {
    private ServerSocket server;
    private ExecutorService serverExecutor;
    private String baseUrl;
    private volatile String lastRequestHead;

    @BeforeEach
    void setUp() throws IOException {
        server = new ServerSocket();
        server.bind(new InetSocketAddress("127.0.0.1", 0));
        serverExecutor = Executors.newSingleThreadExecutor();
        baseUrl = "http://127.0.0.1:" + server.getLocalPort();
    }

    @AfterEach
    void tearDown() {
        try { if (server != null) server.close(); } catch (IOException ignored) {}
        if (serverExecutor != null) serverExecutor.shutdownNow();
    }

    @Test
    void classifiesHttp429() {
        respondOnce(429, 0);
        AgentGatewayClient client = client(Duration.ofSeconds(2));

        IOException failure = assertThrows(IOException.class, () -> client.execute(request()));

        assertEquals("gateway_rate_limited", client.fallback(request(), failure).errorCode());
    }

    @Test
    void classifiesHttp5xx() {
        respondOnce(503, 0);
        AgentGatewayClient client = client(Duration.ofSeconds(2));

        IOException failure = assertThrows(IOException.class, () -> client.execute(request()));

        assertEquals("gateway_5xx", client.fallback(request(), failure).errorCode());
    }

    @Test
    void classifiesRealSocketTimeout() {
        respondOnce(200, 500);
        AgentGatewayClient client = client(Duration.ofMillis(100));

        IOException failure = assertThrows(IOException.class, () -> client.execute(request()));

        assertEquals("gateway_timeout", client.fallback(request(), failure).errorCode());
    }

    @Test
    void sendsNarrowServiceScopeAndInternalCredential() throws IOException {
        respondOnce(200, 0);

        client(Duration.ofSeconds(2)).execute(request());

        assertTrue(lastRequestHead.contains("X-HNBLUE-Service-Scope: agent.command"));
        assertTrue(lastRequestHead.contains("X-HNBLUE-Internal-Token: test-internal-token"));
    }

    private AgentGatewayClient client(Duration timeout) {
        return new AgentGatewayClient(new AgentExecutionConfig().agentOkHttpClient(timeout),
                new ObjectMapper(), baseUrl, true, "test-internal-token");
    }

    private AgentGatewayContract.Request request() {
        return new AgentGatewayContract.Request("gateway-test-task", "trace-test", "PROFESSIONAL:7",
                "PROFESSIONAL", AgentCapability.BASIC_QA, List.of("bluecarbon.knowledge.search"),
                100, 2, "test");
    }

    private void respondOnce(int status, long delayMillis) {
        serverExecutor.submit(() -> {
            try (Socket socket = server.accept()) {
                socket.setSoTimeout(2_000);
                var in = socket.getInputStream();
                int matched = 0;
                ByteArrayOutputStream requestHead = new ByteArrayOutputStream();
                int[] end = {'\r', '\n', '\r', '\n'};
                while (matched < end.length) {
                    int value = in.read();
                    if (value < 0) break;
                    requestHead.write(value);
                    matched = value == end[matched] ? matched + 1 : (value == end[0] ? 1 : 0);
                }
                lastRequestHead = requestHead.toString(java.nio.charset.StandardCharsets.US_ASCII);
                if (delayMillis > 0) Thread.sleep(delayMillis);
                byte[] payload = (status == 200
                        ? "{\"success\":true,\"partial\":false,\"output\":\"ok\",\"sources\":[],\"tokenUsage\":1,\"errorCode\":null,\"errorMessage\":null}"
                        : "{}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                String reason = status == 429 ? "Too Many Requests" : status >= 500 ? "Service Unavailable" : "OK";
                String header = "HTTP/1.1 " + status + " " + reason + "\r\nContent-Type: application/json\r\n"
                        + "Content-Length: " + payload.length + "\r\nConnection: close\r\n\r\n";
                var out = socket.getOutputStream();
                out.write(header.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                out.write(payload);
                out.flush();
            } catch (Exception ignored) {
                // Client timeout is expected to close the socket before the delayed response is written.
            }
        });
    }
}
