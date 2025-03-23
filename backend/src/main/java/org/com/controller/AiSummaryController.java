package org.com.controller;

import org.com.service.AiService;
import org.com.utils.JwtTokenUtil;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*", allowedHeaders = "*", exposedHeaders = "Authorization")
public class AiSummaryController {

    @Resource
    private AiService aiService;
    
    @Resource
    private JwtTokenUtil jwtTokenUtil;
    
    @Resource
    private UserDetailsService userDetailsService;

    @GetMapping(value = "/summarize", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter summarizeNews(
            @RequestParam String url, 
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // 打印调试信息
        System.out.println("收到AI总结请求，URL: " + url);
        System.out.println("Token参数: " + (token != null ? "存在" : "不存在"));
        System.out.println("Authorization头: " + (authHeader != null ? "存在" : "不存在"));
        
        // 创建SSE发射器，增加超时时间
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        
        // 添加完成回调，确保资源正确释放
        emitter.onCompletion(() -> System.out.println("SSE连接已完成"));
        emitter.onTimeout(() -> System.out.println("SSE连接超时"));
        emitter.onError((ex) -> System.out.println("SSE连接错误: " + ex.getMessage()));
        
        // 实际使用的token优先使用URL参数中的token，如果不存在则使用Authorization头
        String actualToken = token != null ? token : authHeader;
        
        // 移除Bearer前缀（如果存在）
        if (actualToken != null && actualToken.startsWith("Bearer ")) {
            actualToken = actualToken.substring(7);
        }
        
        final String finalToken = actualToken; // 创建final副本用于lambda表达式
        
        // 在单独的线程中处理请求，避免阻塞主线程
        new Thread(() -> {
            try {
                // 立即发送连接确认事件
                try {
                    emitter.send(SseEmitter.event()
                        .name("connection")
                        .data("连接已建立", MediaType.TEXT_EVENT_STREAM));
                } catch (Exception e) {
                    System.out.println("发送连接确认失败: " + e.getMessage());
                }
                
                // 移除token验证，直接调用AI总结服务
                aiService.summarizeNews(url, finalToken, emitter);
                
            } catch (Exception e) {
                System.out.println("处理SSE请求时发生异常: " + e.getMessage());
                try {
                    sendErrorEvent(emitter, 500, "服务器内部错误");
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        }).start();
        
        return emitter;
    }
    
    // 辅助方法：发送错误事件
    private void sendErrorEvent(SseEmitter emitter, int code, String message) throws IOException {
        // 直接发送错误对象的字符串形式
        String jsonError = "{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}";
        
        emitter.send(SseEmitter.event()
                .name("error")
                .data(jsonError, MediaType.TEXT_EVENT_STREAM));
        emitter.complete();
    }
} 