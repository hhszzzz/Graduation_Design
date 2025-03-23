package org.com.service;

import org.com.dto.KeywordCountDTO;
import org.com.dto.TrendDataDTO;
import org.com.entity.Keyword;

import java.time.LocalDate;
import java.util.List;

/**
 * 关键词服务接口
 */
public interface KeywordService {
    
    /**
     * 获取关键词趋势数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param category 新闻分类
     * @return 趋势数据
     */
    TrendDataDTO getKeywordTrend(LocalDate startDate, LocalDate endDate, String category);
    
    /**
     * 获取热门关键词
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param category 新闻分类
     * @param limit 返回数量限制
     * @return 关键词统计列表
     */
    List<KeywordCountDTO> getTopKeywords(LocalDate startDate, LocalDate endDate, String category, int limit);
    
    /**
     * 添加关键词
     * @param keyword 关键词实体
     * @return 是否添加成功
     */
    boolean addKeyword(Keyword keyword);
    
    /**
     * 从新闻内容中提取关键词并保存
     * @param newsId 新闻ID
     * @param newsType 新闻类型
     * @param title 新闻标题
     * @param content 新闻内容
     * @return 提取的关键词数量
     */
    int extractAndSaveKeywords(Long newsId, String newsType, String title, String content);
} 