/**
 * AI对话流式响应控制器
 *
 * 功能概述：
 * • 提供与DeepSeek AI API的流式对话接口
 * • 支持Server-Sent Events (SSE) 实时数据推送
 * • 实现AI对话内容的逐字输出效果
 *
 * 技术特性：
 * • 基于OkHttp的SSE长连接管理
 * • 异步事件处理与线程同步控制
 * • JSON数据序列化与反序列化
 * • 响应超时与异常处理机制
 *
 * 通信流程：
 * 1. 接收用户消息并构建AI请求参数
 * 2. 建立SSE连接与DeepSeek API服务通信
 * 3. 实时处理AI返回的流式数据
 * 4. 通过HTTP流式响应返回对话内容
 *
 * 安全配置：
 * • API密钥通过配置属性注入
 * • 请求超时时间可配置化管理
 * • 统一的错误处理与日志记录
 */
package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.dto.AiResult;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("${chat.api.path}")
@ConditionalOnProperty(name = "deepseek.legacy-direct.enabled", havingValue = "true")
public class SeekController {
    private static final Logger logger = LoggerFactory.getLogger(SeekController.class);

    // SSE流结束标识符
    private static final String DONE = "[DONE]";
    // HTTP请求超时时间（秒）
    private static final Integer timeout = 60;
    // DeepSeek AI API端点地址
    private static final String AI_URL = "https://api.deepseek.com/chat/completions";

    // 从配置文件中注入API访问密钥
    @Value("${api.password:}")
    private String apiPassword;

    /**
     * 处理AI对话流式请求
     *
     * @param message 用户输入的对话消息
     * @param response HTTP响应对象，用于流式数据输出
     */
    @GetMapping(value = "/stream")
    public void handleSse(String message, HttpServletResponse response) {
        System.out.println(message);
        // 设置SSE响应头
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("utf-8");

        try (PrintWriter pw = response.getWriter()) {
            // 调用AI服务并获取流式结果
            getAiResult(pw, message);
            // 写入流结束标识
            pw.write("data:end\n\n");
            pw.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 调用DeepSeek AI API并处理流式响应
     *
     * @param pw 响应输出流写入器
     * @param content 用户对话内容
     * @throws InterruptedException 线程中断异常
     */
    private void getAiResult(PrintWriter pw, String content) throws InterruptedException {
        // 构建AI请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("model", "4.0Ultra");

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);

        List<Map> messages = new ArrayList<>();
        messages.add(message);
        params.put("messages", messages);
        params.put("stream", true);
        params.put("model", "deepseek-chat");
        String jsonParams = JsonUtils.convertObj2Json(params);

        // 构建HTTP请求
        Request.Builder builder = new Request.Builder().url(AI_URL);
        builder.addHeader("Authorization", " Bearer " + apiPassword);
        builder.addHeader("Accept", "text/event-stream");
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonParams);
        Request request = builder.post(body).build();

        // 配置HTTP客户端超时参数
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS).build();

        // 创建事件同步锁
        CountDownLatch eventLatch = new CountDownLatch(1);

        // 创建SSE事件监听器
        RealEventSource realEventSource = new RealEventSource(request, new EventSourceListener() {
            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                // 跳过流结束标识
                if (DONE.equals(data)) {
                    return;
                }
                // 提取AI返回内容并写入响应流
                String content = getContent(data);
                pw.write("data:" + JsonUtils.convertObj2Json(new ContentDto(content)) + "\n\n");
                pw.flush();
            }

            @Override
            public void onClosed(EventSource eventSource) {
                super.onClosed(eventSource);
                // 释放同步锁
                eventLatch.countDown();
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                logger.info("调用接口失败{}", t);
                if (eventLatch != null) {
                    eventLatch.countDown();
                }
            }
        });

        // 建立SSE连接
        realEventSource.connect(client);
        // 等待事件流结束
        eventLatch.await();
    }

    /**
     * 从AI响应数据中提取对话内容
     *
     * @param data AI返回的JSON格式数据
     * @return 解析出的对话内容文本
     */
    private static String getContent(String data) {
        AiResult aiResult = JsonUtils.convertJson2Obj(data, AiResult.class);
        return aiResult.getChoices().get(0).getDelta().getContent();
    }
}
