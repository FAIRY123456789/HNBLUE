/**
 * 碳助手AI对话流式响应控制器
 *
 * 功能概述：
 * • 提供与AnythingLLM AI服务的流式对话接口
 * • 支持Server-Sent Events (SSE) 实时数据推送
 * • 处理碳储量相关的智能问答和数据查询
 *
 * 技术特性：
 * • 基于OkHttp的SSE长连接通信
 * • 异步事件驱动响应机制
 * • 可配置的超时和连接参数
 * • 完整的错误处理和日志记录
 *
 * 通信流程：
 * 1. 接收前端查询请求并验证参数
 * 2. 构建AnythingLLM API请求负载
 * 3. 建立SSE连接并实时转发AI响应
 * 4. 处理流式数据并格式化为标准事件
 * 5. 管理连接生命周期和异常情况
 *
 * 配置依赖：
 * • AnythingLLM API密钥和端点地址
 * • 工作空间标识符
 * • 连接超时时间设置
 */
package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.dto.ContentDto;
import com.example.jpaspringboot.util.JsonUtils;
import okhttp3.*;
import okhttp3.internal.sse.RealEventSource;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("${chat.api.path}")
public class CarbonAssistantController {

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(CarbonAssistantController.class);

    // SSE流结束标识符
    private static final String DONE = "[DONE]";

    // HTTP客户端超时时间（秒）
    private static final Integer timeout = 60;

    // AnythingLLM API认证密钥
    @Value("${anythingllm.api.key}")
    private String apiKey;

    // AnythingLLM服务端点地址
    @Value("${anythingllm.api.url}")
    private String anythingllmUrl;

    // 工作空间标识符
    @Value("${anythingllm.workspace}")
    private String workspace;

    /**
     * 碳助手流式对话接口
     *
     * @param message 用户查询消息
     * @param response HTTP响应对象
     */
    @GetMapping("/stream-carbon")
    public void handleCarbonStream(String message, HttpServletResponse response) {
        // 配置SSE响应头
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("utf-8");

        try (PrintWriter pw = response.getWriter()) {
            // 启动AnythingLLM流式对话
            streamAnythingLLM(message, pw);
            // 发送流结束标识
            pw.write("data:end\n\n");
            pw.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 与AnythingLLM服务建立流式对话连接
     *
     * @param msg 用户消息内容
     * @param pw 响应输出流
     * @throws InterruptedException 线程中断异常
     */
    private void streamAnythingLLM(String msg, PrintWriter pw) throws InterruptedException {
        // 构建API请求负载
        Map<String, Object> payload = new HashMap<>();
        payload.put("workspace", workspace);
        payload.put("message", msg);
        String json = JsonUtils.convertObj2Json(payload);

        // 创建HTTP请求
        Request request = new Request.Builder()
                .url(anythingllmUrl + "/v1/workspace/hnblue/stream-chat")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

        // 配置HTTP客户端
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build();

        // 同步锁，等待连接结束
        CountDownLatch latch = new CountDownLatch(1);

        // 创建SSE事件源监听器
        RealEventSource realEventSource = new RealEventSource(request, new EventSourceListener() {
            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                // 过滤流结束标识
                if (DONE.equals(data)) return;
                // 转发AI响应数据
                pw.write("data:" + JsonUtils.convertObj2Json(new ContentDto(data)) + "\n\n");
                pw.flush();
            }

            @Override
            public void onClosed(EventSource eventSource) {
                // 连接关闭时释放同步锁
                latch.countDown();
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                // 记录连接失败日志
                logger.error("AnythingLLM 连接失败", t);
                latch.countDown();
            }
        });

        // 建立连接并等待完成
        realEventSource.connect(client);
        latch.await();
    }
}