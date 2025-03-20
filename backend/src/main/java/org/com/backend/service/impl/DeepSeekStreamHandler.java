package org.com.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

/**
 * DeepSeek API流式响应处理器
 */
public class DeepSeekStreamHandler {

    private final SseEmitter emitter;
    private final ObjectMapper objectMapper;
    
    /**
     * 构造函数
     *
     * @param emitter SSE发射器
     */
    public DeepSeekStreamHandler(SseEmitter emitter) {
        this.emitter = emitter;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 处理流式响应
     *
     * @param inputStream 输入流
     * @return 完成后的Future
     */
    public CompletableFuture<Void> handleStream(InputStream inputStream) {
        return CompletableFuture.runAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                StringBuilder contentBuffer = new StringBuilder();
                
                while ((line = reader.readLine()) != null) {
                    // DeepSeek API的流式响应格式是: data: {json}
                    if (line.startsWith("data: ")) {
                        // 跳过心跳信息
                        if (line.equals("data: [DONE]")) {
                            continue;
                        }
                        
                        // 提取JSON部分
                        String jsonStr = line.substring(6);
                        try {
                            // 解析JSON
                            JsonNode jsonNode = objectMapper.readTree(jsonStr);
                            
                            // 处理流式响应
                            JsonNode choices = jsonNode.get("choices");
                            if (choices != null && choices.isArray() && choices.size() > 0) {
                                JsonNode choice = choices.get(0);
                                JsonNode delta = choice.get("delta");
                                
                                if (delta != null) {
                                    // 获取内容增量
                                    if (delta.has("content") && !delta.get("content").isNull()) {
                                        String content = delta.get("content").asText();
                                        if (content != null && !content.isEmpty()) {
                                            // 发送摘要增量
                                            emitter.send(SseEmitter.event()
                                                    .name("summary")
                                                    .data(content, MediaType.TEXT_EVENT_STREAM));
                                            
                                            // 累积内容
                                            contentBuffer.append(content);
                                        }
                                    }
                                    
                                    // 处理reasoning内容 (deepseek-reasoner模型特有)
                                    if (delta.has("reasoning_content") && !delta.get("reasoning_content").isNull()) {
                                        String reasoning = delta.get("reasoning_content").asText();
                                        if (reasoning != null && !reasoning.isEmpty()) {
                                            emitter.send(SseEmitter.event()
                                                    .name("reasoning")
                                                    .data(reasoning, MediaType.TEXT_EVENT_STREAM));
                                        }
                                    }
                                }
                                
                                // 检查是否完成
                                if (choice.has("finish_reason") && 
                                        !choice.get("finish_reason").isNull() && 
                                        !"null".equals(choice.get("finish_reason").asText())) {
                                    // 发送完成事件
                                    emitter.send(SseEmitter.event()
                                            .name("complete")
                                            .data("摘要生成完成", MediaType.TEXT_EVENT_STREAM));
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("解析流式响应时出错: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
                
                // 确保完成事件被发送
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data("摘要生成完成", MediaType.TEXT_EVENT_STREAM));
                
                // 关闭发射器
                emitter.complete();
                
            } catch (Exception e) {
                System.out.println("处理流式响应时出错: " + e.getMessage());
                e.printStackTrace();
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("处理响应流时出错: " + e.getMessage(), MediaType.TEXT_EVENT_STREAM));
                    emitter.complete();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
} 