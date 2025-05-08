package org.com.service;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import org.com.entity.Keyword;
import org.com.mapper.KeywordMapper;
import org.com.service.impl.KeywordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KeywordServiceTest {

    @Mock
    private KeywordMapper keywordMapper;

    @Mock
    private JiebaSegmenter segmenter;

    @Mock
    private Set<String> stopWords;

    @InjectMocks
    private KeywordServiceImpl keywordService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 配置停用词列表
        when(stopWords.contains("的")).thenReturn(true);
        when(stopWords.contains("了")).thenReturn(true);
        when(stopWords.contains("是")).thenReturn(true);
        when(stopWords.contains("在")).thenReturn(true);
        when(stopWords.contains("和")).thenReturn(true);
        
        // 配置关键词插入成功的模拟返回
        when(keywordMapper.insert(any(Keyword.class))).thenReturn(1);
    }

    /**
     * 测试从文本提取关键词并保存
     */
    @Test
    void testExtractAndSaveKeywords() {
        // 准备测试数据
        Long newsId = 123L;
        String newsType = "technology";
        String title = "特斯拉发布全自动驾驶技术更新";
        String content = "特斯拉公司今日宣布其全自动驾驶技术取得重大突破，新版本将于下月推送给用户。" +
                         "该技术采用先进的人工智能算法，能够在复杂路况下实现更安全的自动驾驶。" +
                         "马斯克表示，这是向完全自动驾驶迈出的重要一步。";

        // 模拟分词结果
        List<SegToken> segTokens = new ArrayList<>();
        // 添加标题分词
        addSegToken(segTokens, "特斯拉", 0);
        addSegToken(segTokens, "发布", 3);
        addSegToken(segTokens, "全自动驾驶", 5);
        addSegToken(segTokens, "技术", 10);
        addSegToken(segTokens, "更新", 12);
        
        // 添加内容分词
        addSegToken(segTokens, "特斯拉", 14);
        addSegToken(segTokens, "公司", 17);
        addSegToken(segTokens, "今日", 19);
        addSegToken(segTokens, "宣布", 21);
        addSegToken(segTokens, "全自动驾驶", 23);
        addSegToken(segTokens, "技术", 28);
        addSegToken(segTokens, "重大", 30);
        addSegToken(segTokens, "突破", 32);
        addSegToken(segTokens, "新版本", 34);
        addSegToken(segTokens, "下月", 37);
        addSegToken(segTokens, "推送", 39);
        addSegToken(segTokens, "用户", 41);
        addSegToken(segTokens, "技术", 43);
        addSegToken(segTokens, "采用", 45);
        addSegToken(segTokens, "先进", 47);
        addSegToken(segTokens, "人工智能", 49);
        addSegToken(segTokens, "算法", 53);
        addSegToken(segTokens, "复杂", 55);
        addSegToken(segTokens, "路况", 57);
        addSegToken(segTokens, "实现", 59);
        addSegToken(segTokens, "安全", 61);
        addSegToken(segTokens, "自动驾驶", 63);
        addSegToken(segTokens, "马斯克", 67);
        addSegToken(segTokens, "表示", 70);
        addSegToken(segTokens, "完全", 72);
        addSegToken(segTokens, "自动驾驶", 74);
        addSegToken(segTokens, "重要", 78);
        addSegToken(segTokens, "一步", 80);
        
        // 模拟分词器返回
        when(segmenter.process(contains(title), any())).thenReturn(segTokens);
        
        // 执行测试方法
        int count = keywordService.extractAndSaveKeywords(newsId, newsType, title, content);
        
        // 验证关键词提取与保存
        // 应该至少有5个关键词被保存
        assertTrue(count >= 5, "应至少提取并保存5个关键词");
        
        // 验证插入调用
        verify(keywordMapper, atLeast(5)).insert(any(Keyword.class));
        
        // 捕获参数以验证插入的关键词
        ArgumentCaptor<Keyword> keywordCaptor = ArgumentCaptor.forClass(Keyword.class);
        verify(keywordMapper, atLeast(1)).insert(keywordCaptor.capture());
        
        // 获取所有捕获的关键词
        List<Keyword> capturedKeywords = keywordCaptor.getAllValues();
        
        // 验证关键词内容
        Set<String> extractedKeywords = new HashSet<>();
        for (Keyword keyword : capturedKeywords) {
            extractedKeywords.add(keyword.getKeyword());
            
            // 验证每个关键词的元数据
            assertEquals(newsId, keyword.getNewsId(), "新闻ID应匹配");
            assertEquals(newsType, keyword.getNewsType(), "新闻类型应匹配");
            assertEquals(title, keyword.getNewsTitle(), "新闻标题应匹配");
            assertNotNull(keyword.getCreateTime(), "创建时间不应为空");
            assertNotNull(keyword.getRecordTime(), "记录时间不应为空");
        }
        
        // 验证是否包含预期的关键词
        assertTrue(extractedKeywords.contains("特斯拉"), "应提取关键词'特斯拉'");
        assertTrue(extractedKeywords.contains("全自动驾驶") || extractedKeywords.contains("自动驾驶"), 
                  "应提取关键词'全自动驾驶'或'自动驾驶'");
        assertTrue(extractedKeywords.contains("技术"), "应提取关键词'技术'");
    }

    /**
     * 测试关键词添加功能
     */
    @Test
    void testAddKeyword() {
        // 准备测试数据
        Keyword keyword = new Keyword();
        keyword.setKeyword("人工智能");
        keyword.setNewsId(456L);
        keyword.setNewsType("science");
        keyword.setNewsTitle("人工智能研究取得突破性进展");
        
        // 执行测试
        boolean result = keywordService.addKeyword(keyword);
        
        // 验证结果
        assertTrue(result, "添加关键词应返回true");
        
        // 验证时间戳是否自动设置
        assertNotNull(keyword.getCreateTime(), "创建时间应自动设置");
        assertNotNull(keyword.getRecordTime(), "记录时间应自动设置");
        
        // 验证mapper是否被调用
        verify(keywordMapper).insert(keyword);
    }

    /**
     * 测试边界情况：空内容
     */
    @Test
    void testExtractKeywordsWithEmptyContent() {
        // 执行测试方法 - 标题和内容都为null
        int count = keywordService.extractAndSaveKeywords(789L, "general", null, null);
        
        // 验证结果
        assertEquals(0, count, "空内容应返回0个关键词");
        
        // 验证mapper未被调用
        verify(keywordMapper, never()).insert(any(Keyword.class));
    }
    
    /**
     * 测试关键词提取 - 只有标题没有内容
     */
    @Test
    void testExtractKeywordsWithTitleOnly() {
        // 准备测试数据
        Long newsId = 321L;
        String newsType = "politics";
        String title = "国务院召开重要会议讨论经济政策";
        String content = null;
        
        // 模拟分词结果
        List<SegToken> segTokens = new ArrayList<>();
        addSegToken(segTokens, "国务院", 0);
        addSegToken(segTokens, "召开", 3);
        addSegToken(segTokens, "重要", 5);
        addSegToken(segTokens, "会议", 7);
        addSegToken(segTokens, "讨论", 9);
        addSegToken(segTokens, "经济", 11);
        addSegToken(segTokens, "政策", 13);
        
        // 模拟分词器返回
        when(segmenter.process(contains(title), any())).thenReturn(segTokens);
        
        // 执行测试方法
        int count = keywordService.extractAndSaveKeywords(newsId, newsType, title, content);
        
        // 验证结果
        assertTrue(count > 0, "应提取并保存至少一个关键词");
        
        // 验证插入调用
        verify(keywordMapper, atLeast(1)).insert(any(Keyword.class));
        
        // 捕获参数以验证插入的关键词
        ArgumentCaptor<Keyword> keywordCaptor = ArgumentCaptor.forClass(Keyword.class);
        verify(keywordMapper, atLeast(1)).insert(keywordCaptor.capture());
        
        // 验证关键词内容
        List<Keyword> capturedKeywords = keywordCaptor.getAllValues();
        Set<String> extractedKeywords = new HashSet<>();
        for (Keyword keyword : capturedKeywords) {
            extractedKeywords.add(keyword.getKeyword());
        }
        
        // 验证是否包含预期的关键词
        assertTrue(extractedKeywords.contains("国务院"), "应提取关键词'国务院'");
        assertTrue(extractedKeywords.contains("经济"), "应提取关键词'经济'");
        assertTrue(extractedKeywords.contains("政策"), "应提取关键词'政策'");
    }
    
    /**
     * 辅助方法：创建分词结果
     */
    private void addSegToken(List<SegToken> tokens, String word, int startOffset) {
        SegToken token = new SegToken(word, startOffset, startOffset + word.length());
        tokens.add(token);
    }
} 