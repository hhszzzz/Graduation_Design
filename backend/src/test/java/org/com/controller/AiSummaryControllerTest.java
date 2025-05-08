package org.com.controller;

import org.com.service.AiService;
import org.com.utils.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AiSummaryControllerTest {

    @Mock
    private AiService aiService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AiSummaryController aiSummaryController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(aiSummaryController).build();
    }

    /**
     * 测试总结接口 - 不带认证信息
     */
    @Test
    void testSummarizeNewsWithoutAuth() throws Exception {
        // 准备测试数据
        String testUrl = "https://example.com/news/article";
        SseEmitter mockEmitter = new SseEmitter();
        
        // 模拟AiService行为
        when(aiService.summarizeNews(eq(testUrl), isNull())).thenReturn(mockEmitter);
        
        // 执行API请求并验证响应
        mockMvc.perform(get("/api/ai/summarize")
                .param("url", testUrl)
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
        
        // 验证服务调用
        verify(aiService).summarizeNews(eq(testUrl), isNull());
    }

    /**
     * 测试总结接口 - 带认证信息
     */
    @Test
    void testSummarizeNewsWithAuth() throws Exception {
        // 准备测试数据
        String testUrl = "https://example.com/news/article";
        String authToken = "Bearer test-jwt-token";
        String extractedToken = "test-jwt-token";
        SseEmitter mockEmitter = new SseEmitter();
        
        // 模拟AiService行为
        when(aiService.summarizeNews(anyString(), anyString())).thenReturn(mockEmitter);
        
        // 执行API请求并验证响应
        mockMvc.perform(get("/api/ai/summarize")
                .param("url", testUrl)
                .header("Authorization", authToken)
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
        
        // 验证服务调用（注意：由于控制器方法内部逻辑，这里可能需要调整对应于实际实现）
        verify(aiService).summarizeNews(eq(testUrl), anyString());
    }

    /**
     * 测试参数验证 - URL缺失
     */
    @Test
    void testSummarizeNewsWithMissingUrl() throws Exception {
        // 执行API请求并验证响应
        mockMvc.perform(get("/api/ai/summarize")
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isBadRequest());
        
        // 验证服务不应被调用
        verify(aiService, never()).summarizeNews(anyString(), anyString());
    }

    /**
     * 测试带认证头的总结接口
     */
    @Test
    void testSummarizeNewsWithAuthHeader() throws Exception {
        // 准备测试数据
        String testUrl = "https://example.com/news/article";
        String authHeader = "Bearer test-jwt-token";
        
        // 模拟服务返回
        SseEmitter mockEmitter = new SseEmitter();
        when(aiService.summarizeNews(anyString(), anyString())).thenReturn(mockEmitter);
        
        // 直接调用控制器方法
        SseEmitter result = aiSummaryController.summarizeNews(testUrl, null, authHeader);
        
        // 验证结果
        assertNotNull(result, "控制器应返回非空SseEmitter");
        
        // 注意：由于控制器在新线程中调用service，这里无法直接验证
        // 可以考虑重构控制器代码使其更易于测试，或者使用其他方式验证
    }
} 