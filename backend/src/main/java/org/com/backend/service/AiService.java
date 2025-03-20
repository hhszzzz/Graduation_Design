package org.com.backend.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI服务接口
 */
public interface AiService {
    /**
     * 生成新闻摘要
     * @param url 新闻URL
     * @param token 用户token
     * @return SseEmitter
     */
    SseEmitter summarizeNews(String url, String token);
    
    /**
     * 生成新闻摘要（直接使用传入的发射器）
     * @param url 新闻URL
     * @param token 用户token
     * @param emitter 客户端已创建的SSE发射器
     */
    void summarizeNews(String url, String token, SseEmitter emitter);
} 