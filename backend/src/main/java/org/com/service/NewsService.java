package org.com.service;

import org.com.entity.News;
import org.com.dto.PageResult;

import java.util.List;

public interface NewsService {

    // 调用Python API并存储到数据库
    public void autoCrawlTask();

    // 根据类型获取新闻列表
    List<News> listNewsByType(String type);
    
    // 分页查询新闻列表
    PageResult<News> pageNewsByType(String type, Integer pageNum, Integer pageSize);
    
    // 导出新闻标题到CSV文件
    boolean exportNewsTitlesToCSV(String type, String filePath);
    
    // 定时任务自动导出新闻标题到CSV文件
    void autoExportNewsToCSV();
    
    // 根据URL爬取新闻内容
    String crawlNewsContent(String url);
    
    // 获取新闻详情
    News getNewsDetail(Long id, String type);
    
    // 搜索新闻（标题匹配）
    List<News> searchNews(String query);
    
    // 分页搜索新闻
    PageResult<News> pageSearchNews(String query, Integer pageNum, Integer pageSize);
    
    // 定时任务处理已有新闻并提取关键词
    void processExistingNewsKeywords();
}
