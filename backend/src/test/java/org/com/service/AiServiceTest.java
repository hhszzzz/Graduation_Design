package org.com.service;

import org.com.config.AiConfig;
import org.com.service.impl.AiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiServiceTest {

    @Mock
    private AiConfig aiConfig;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 配置 AiConfig 的返回值
        when(aiConfig.getDeepseekApiKey()).thenReturn("test-api-key");
        when(aiConfig.getDeepseekBaseUrl()).thenReturn("https://api.deepseek.com");
        when(aiConfig.getAiModel()).thenReturn("deepseek-chat");
        when(aiConfig.getSummaryTemplate()).thenReturn("请用150字左右简要总结以下新闻内容的要点，不要复制原文:");
        when(aiConfig.getCrawlerBaseUrl()).thenReturn("http://localhost:9999");
    }

    /**
     * 测试本地摘要生成方法
     */
    @Test
    void testGenerateLocalSummary() throws Exception {
        // 准备测试数据
        String testContent = "北京时间2023年6月8日，中国科学院宣布成功发射探月卫星'嫦娥六号'。"
                + "这次任务将采集月球背面的土壤样本，是人类首次从月球背面带回样本。"
                + "科学家期待通过分析这些样本，进一步了解月球的形成和演化历史。"
                + "此次任务将持续约23天，预计于6月底返回地球。"
                + "中国航天科技集团表示，这标志着中国探月工程取得了重大突破。";
        
        String testUrl = "https://example.com/news/article";

        // 创建用于捕获摘要内容的容器
        final ArrayList<String> capturedSummary = new ArrayList<>();
        
        // 创建测试完成信号
        final CountDownLatch latch = new CountDownLatch(1);
        
        // 创建自定义的SseEmitter实现，用于捕获sendEvent方法
        SseEmitter testEmitter = new SseEmitter(300000L) { // 5分钟超时，确保足够长
            @Override
            public void send(SseEmitter.SseEventBuilder event) throws IOException {
                // 获取事件类型和数据
                String eventString = event.toString();
                if (eventString.contains("summary")) {
                    // 提取数据部分
                    String data = eventString.replaceAll(".*data=([^,]+).*", "$1");
                    capturedSummary.add(data);
                } else if (eventString.contains("complete")) {
                    // 当收到完成事件时释放锁
                    latch.countDown();
                }
                
                // 不实际调用父类方法，避免实际发送
            }
            
            @Override
            public void complete() {
                // 确保无论如何都会释放锁
                latch.countDown();
            }
        };
        
        // 使用反射调用私有方法
        Method generateLocalSummaryMethod = AiServiceImpl.class.getDeclaredMethod(
                "generateLocalSummary", String.class, String.class, SseEmitter.class);
        generateLocalSummaryMethod.setAccessible(true);
        
        // 执行测试
        generateLocalSummaryMethod.invoke(aiService, testContent, testUrl, testEmitter);
        
        // 等待完成或超时（最多等待10秒）
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        
        // 验证结果
        assertTrue(completed, "摘要生成应在超时前完成");
        assertFalse(capturedSummary.isEmpty(), "应生成非空摘要");
        
        // 合并所有捕获的摘要片段
        StringBuilder fullSummaryBuilder = new StringBuilder();
        for (String part : capturedSummary) {
            fullSummaryBuilder.append(part);
        }
        String fullSummary = fullSummaryBuilder.toString();
        
        // 验证摘要是否包含关键信息 - 设置宽松一些的验证条件
        assertTrue(
            fullSummary.contains("嫦娥") || 
            fullSummary.contains("月球") || 
            fullSummary.contains("样本") ||
            fullSummary.contains("探月") ||
            fullSummary.contains("卫星"),
            "摘要应包含原文的关键信息"
        );
    }

    /**
     * 测试模拟摘要生成方法
     */
    @Test
    void testMockAiSummaryFromContent() throws Exception {
        // 准备测试数据
        String testContent = "特斯拉首席执行官埃隆·马斯克宣布推出全新电动汽车Model Y。"
                + "这款中型SUV将搭载特斯拉最新的自动驾驶技术，续航里程可达500公里。"
                + "据悉，Model Y将于明年第一季度开始交付，起售价为39,000美元。"
                + "分析师预测，Model Y将成为特斯拉最畅销的车型，进一步推动电动汽车市场的发展。";
        
        String testUrl = "https://example.com/news/tech";

        // 创建用于捕获摘要内容的容器
        final ArrayList<String> capturedSummary = new ArrayList<>();
        
        // 创建测试完成信号
        final CountDownLatch latch = new CountDownLatch(1);
        
        // 创建自定义的SseEmitter实现，用于捕获sendEvent方法
        SseEmitter testEmitter = new SseEmitter(300000L) { // 5分钟超时
            @Override
            public void send(SseEmitter.SseEventBuilder event) throws IOException {
                // 获取事件类型和数据
                String eventString = event.toString();
                if (eventString.contains("summary")) {
                    // 提取数据部分
                    String data = eventString.replaceAll(".*data=([^,]+).*", "$1");
                    capturedSummary.add(data);
                } else if (eventString.contains("complete")) {
                    // 当收到完成事件时释放锁
                    latch.countDown();
                }
                
                // 不实际调用父类方法，避免实际发送
            }
            
            @Override
            public void complete() {
                // 确保无论如何都会释放锁
                latch.countDown();
            }
        };
        
        // 使用反射调用私有方法
        Method mockAiSummaryMethod = AiServiceImpl.class.getDeclaredMethod(
                "mockAiSummaryFromContent", String.class, String.class, SseEmitter.class);
        mockAiSummaryMethod.setAccessible(true);
        
        // 执行测试
        mockAiSummaryMethod.invoke(aiService, testContent, testUrl, testEmitter);
        
        // 等待完成或超时
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        
        // 验证结果
        assertTrue(completed, "摘要生成应在超时前完成");
        assertFalse(capturedSummary.isEmpty(), "应生成非空摘要");
        
        // 合并所有捕获的摘要片段
        StringBuilder fullSummaryBuilder = new StringBuilder();
        for (String part : capturedSummary) {
            fullSummaryBuilder.append(part);
        }
        String fullSummary = fullSummaryBuilder.toString();
        
        // 验证摘要长度
        assertTrue(fullSummary.length() <= 1000, "摘要长度应小于1000字符");
        
        // 验证摘要是否包含关键信息
        assertTrue(
            fullSummary.contains("特斯拉") || 
            fullSummary.contains("Model Y") || 
            fullSummary.contains("电动汽车") ||
            fullSummary.contains("马斯克"),
            "摘要应包含关键信息'特斯拉'、'Model Y'、'电动汽车'或'马斯克'"
        );
        
        // 验证是否包含根据URL判断的主题
        assertTrue(fullSummary.contains("技术相关"), "根据URL，摘要应标识为'技术相关'");
    }

    /**
     * 测试检测原文复制的方法
     */
    @Test
    void testContainsSubstring() throws Exception {
        // 使用反射获取私有方法
        Method containsSubstringMethod = AiServiceImpl.class.getDeclaredMethod(
                "containsSubstring", String.class, String.class, int.class);
        containsSubstringMethod.setAccessible(true);
        
        // 测试用例1：完全匹配
        String original = "这是一段测试文本，用于测试子字符串检测功能。";
        String substring = "这是一段测试文本";
        boolean result1 = (boolean) containsSubstringMethod.invoke(aiService, original, substring, 10);
        assertTrue(result1, "应检测到完全匹配的子字符串");
        
        // 测试用例2：部分匹配但小于指定长度
        String shortSubstring = "这是";
        boolean result2 = (boolean) containsSubstringMethod.invoke(aiService, original, shortSubstring, 10);
        assertFalse(result2, "对于短于指定长度的子字符串应返回false");
        
        // 测试用例3：不匹配
        String nonMatchingSubstring = "其他完全不同的内容";
        boolean result3 = (boolean) containsSubstringMethod.invoke(aiService, original, nonMatchingSubstring, 10);
        assertFalse(result3, "对于不匹配的子字符串应返回false");
        
        // 测试用例4：空字符串
        String emptyString = "";
        boolean result4 = (boolean) containsSubstringMethod.invoke(aiService, original, emptyString, 10);
        assertFalse(result4, "对于空子字符串应返回false");
    }

    /**
     * 测试网页内容获取
     */
    @Test
    void testFetchWebContent() throws Exception {
        // 模拟爬虫API响应
        String mockContent = "这是一篇测试新闻文章的内容";
        ResponseEntity<String> mockResponse = new ResponseEntity<>(mockContent, HttpStatus.OK);
        
        // 配置mock
        when(restTemplate.exchange(
            anyString(), 
            eq(HttpMethod.GET), 
            any(HttpEntity.class), 
            eq(String.class)
        )).thenReturn(mockResponse);
        
        // 使用反射调用私有方法
        Method fetchWebContentMethod = AiServiceImpl.class.getDeclaredMethod("fetchWebContent", String.class);
        fetchWebContentMethod.setAccessible(true);
        
        // 执行测试
        String result = (String) fetchWebContentMethod.invoke(aiService, "https://example.com/test");
        
        // 验证结果
        assertEquals(mockContent, result, "应返回模拟的网页内容");
        
        // 验证restTemplate被正确调用
        verify(restTemplate).exchange(
            contains("/api/crawl_context?url="), 
            eq(HttpMethod.GET), 
            any(HttpEntity.class), 
            eq(String.class)
        );
    }
}