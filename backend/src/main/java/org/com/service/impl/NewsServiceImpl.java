package org.com.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.com.entity.News;
import org.com.mapper.NewsMapper;
import org.com.service.NewsService;
import org.com.utils.CSVUtil;
import org.com.utils.UrlMatchSqlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.com.dto.PageResult;
import org.com.service.KeywordService;

@Service
public class NewsServiceImpl implements NewsService {

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private NewsMapper newsMapper;

    @Resource
    private UrlMatchSqlUtil urlMatchSqlUtil;
    
    @Resource
    private CSVUtil csvUtil;
    
    @Resource
    private KeywordService keywordService;
    
    @Value("${news.csv.export.path:./news_export}")
    private String csvExportBasePath;

    @Value("${crawler.base-url:http://localhost:9999}")
    private String crawlerBaseUrl;

    // 定时任务：每小时请求一次Python爬虫服务
    @Override
    @Scheduled(cron = "0 0 0 * * ?")
    public void autoCrawlTask() {
        System.out.println("自动爬虫任务启动...");

        HashMap<String,String> map = urlMatchSqlUtil.getUrls();
        int urlCount = 0;
        
        // 创建常见的User-Agent列表，模拟不同浏览器
        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36"
        };
        
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String url = entry.getValue();
            String sql = entry.getKey();
            System.out.println("当前爬取URL: " + url);

            try {
                // 添加随机延迟，避免请求过于频繁 (3-7秒)
                long delayTime = 3000 + (long)(Math.random() * 4000);
                System.out.println("等待 " + (delayTime/1000) + " 秒后继续...");
                Thread.sleep(delayTime);
                
                // 随机选择一个User-Agent
                String randomUserAgent = userAgents[(int)(Math.random() * userAgents.length)];
                
                // 创建请求头并设置User-Agent
                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", randomUserAgent);
                headers.set("Accept", "application/json,text/html,application/xhtml+xml,application/xml;q=0.9");
                headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
                headers.set("Connection", "keep-alive");
                headers.set("Referer", "https://www.google.com/"); // 伪装Referer
                
                // 创建带有头信息的HTTP实体
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                // 使用带头信息的实体发送请求
                ResponseEntity<List<News>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<News>>() {}
                );

                List<News> newsList = response.getBody();
                if (newsList != null) {
                    for (News news : newsList) {
                        if (newsMapper.selectCountTable(sql,news.getTitle()) == 0) {
                            // 手动安全解析时间后再入库
                            LocalDateTime publishTime = parseTime(news.getPublishTime());
                            // 这里直接调用mapper插入
                            if (newsMapper.insertNewsDynamicTable(sql, news.getTitle(), news.getLink(), publishTime) > 0) {
                                // 获取新闻ID，用于提取关键词
                                Long newsId = newsMapper.selectLastInsertIdByType(sql);
                                // 获取新闻内容
                                String content = crawlNewsContent(news.getLink());
                                // 使用内容提取关键词
                                try {
                                    keywordService.extractAndSaveKeywords(newsId, sql, news.getTitle(), content);
                                } catch (Exception e) {
                                    System.out.println("提取关键词失败: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    // 已成功爬取一个URL
                    urlCount++;
                    System.out.println("成功爬取并处理URL: " + url + "，共处理 " + newsList.size() + " 条新闻");
                }

            } catch (Exception e) {
                System.out.println("爬取 " + url + " 时发生异常: " + e.getMessage());
                e.printStackTrace();
            }
            
            // 每爬完两个URL，增加一个较长的休息时间(10-15秒)
            if (urlCount % 2 == 0 && urlCount > 0) {
                try {
                    long restTime = 10000 + (long)(Math.random() * 5000);
                    System.out.println("已完成 " + urlCount + " 个URL的爬取，休息 " + (restTime/1000) + " 秒...");
                    Thread.sleep(restTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        System.out.println("自动爬虫任务完成！共成功爬取 " + urlCount + " 个URL");
    }

    // 兼容多种时间格式（包含有分秒和无分秒的情况）
    private LocalDateTime parseTime(String timeStr) {
        DateTimeFormatter formatterWithSeconds = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatterWithoutTime = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if (timeStr == null || timeStr.isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            // 先尝试有秒的格式
            return LocalDateTime.parse(timeStr, formatterWithSeconds);
        } catch (DateTimeParseException e) {
            try {
                // 再尝试只有日期的格式
                LocalDate date = LocalDate.parse(timeStr, formatterWithoutTime);
                return date.atStartOfDay();
            } catch (DateTimeParseException ex) {
                // 所有解析失败时，使用当前时间
                System.out.println("时间格式错误，使用当前时间替代：" + timeStr);
                return LocalDateTime.now();
            }
        }
    }

    // 根据类型获取新闻列表
    @Override
    public List<News> listNewsByType(String type) {
        HashMap<String, String> map = urlMatchSqlUtil.getUrls();
        if (type == null || type.isEmpty()) {
            return newsMapper.selectList(null);
        }
        return newsMapper.selectListByType(type);
    }
    
    /**
     * 分页查询新闻列表
     * @param type 新闻类型
     * @param pageNum 页码
     * @param pageSize 每页记录数
     * @return 分页结果
     */
    @Override
    public PageResult<News> pageNewsByType(String type, Integer pageNum, Integer pageSize) {
        // 验证分页参数
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        
        // 计算offset
        int offset = (pageNum - 1) * pageSize;
        
        // 不同类型的新闻在不同的表中，根据type获取对应的表名
        if (type == null || type.isEmpty()) {
            // 如果没有指定类型，返回空列表
            return new PageResult<>(Collections.emptyList(), 0, pageNum, pageSize);
        }
        
        // 查询总记录数
        long total = newsMapper.countByType(type);
        
        // 查询当前页数据
        List<News> records = newsMapper.selectPageByType(type, offset, pageSize);
        
        // 返回分页结果
        return new PageResult<>(records, total, pageNum, pageSize);
    }
    
    /**
     * 导出新闻标题到CSV文件
     * @param type 新闻类型，为空则导出所有新闻
     * @param filePath CSV文件保存路径
     * @return 是否导出成功
     */
    @Override
    public boolean exportNewsTitlesToCSV(String type, String filePath) {
        // 根据类型获取新闻列表
        List<News> newsList = listNewsByType(type);
        
        // 如果列表为空，返回失败
        if (newsList == null || newsList.isEmpty()) {
            return false;
        }
        
        // 调用CSV工具类导出到文件
        return csvUtil.exportNewsTitlesToCSV(newsList, filePath);
    }

    /**
     * 定时任务：每天凌晨2点自动导出新闻标题到CSV文件
     */
    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoExportNewsToCSV() {
        System.out.println("开始执行新闻标题CSV导出任务...");
        
        // 获取当前日期作为文件名的一部分
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        // 导出各类型新闻
        HashMap<String, String> typeMap = urlMatchSqlUtil.getUrls();
        for (Map.Entry<String, String> entry : typeMap.entrySet()) {
            String tableName = entry.getKey();
            
            // 为每种类型创建单独的CSV文件
            String typeFilePath = csvExportBasePath + "/" + tableName + "_titles_" + dateStr + ".csv";
            
            try {
                List<Map<String, Object>> newsData = newsMapper.selectNewsByTypeForCSV(tableName);
                
                if (newsData != null && !newsData.isEmpty()) {
                    boolean result = csvUtil.exportNewsMapToCSV(newsData, typeFilePath);
                    if (result) {
                        System.out.println("成功导出 " + newsData.size() + " 条" + tableName + "新闻标题到: " + typeFilePath);
                    } else {
                        System.err.println("导出" + tableName + "新闻标题到CSV文件失败: " + typeFilePath);
                    }
                } else {
                    System.out.println("没有可导出的" + tableName + "新闻数据");
                }
            } catch (Exception e) {
                System.err.println("查询" + tableName + "新闻数据失败: " + e.getMessage());
            }
        }
        
        System.out.println("新闻标题CSV导出任务完成");
    }

    /**
     * 根据URL爬取新闻内容
     * @param url 新闻URL
     * @return 爬取的新闻内容
     */
    @Override
    public String crawlNewsContent(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 构建爬虫API URL
            String crawlApiUrl = crawlerBaseUrl + "/api/crawl_context?url=" + url;
            System.out.println("调用爬虫API: " + crawlApiUrl);
            
            // 随机选择一个User-Agent
            String[] userAgents = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36"
            };
            String randomUserAgent = userAgents[(int)(Math.random() * userAgents.length)];
            
            // 创建HTTP请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));
            headers.set("User-Agent", randomUserAgent);
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Connection", "keep-alive");
            headers.set("Referer", "https://www.google.com/"); // 伪装Referer
            
            // 创建HTTP实体
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 发送GET请求
            ResponseEntity<String> response = restTemplate.exchange(
                crawlApiUrl, 
                HttpMethod.GET, 
                entity,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                String content = response.getBody();
                System.out.println("成功获取新闻内容，长度: " + (content != null ? content.length() : 0) + " 字符");
                
                // 如果内容是JSON字符串，尝试将其解析并提取出内容
                if (content != null && content.trim().startsWith("{")) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode rootNode = mapper.readTree(content);
                        if (rootNode.has("content")) {
                            content = rootNode.get("content").asText();
                        }
                    } catch (Exception e) {
                        System.out.println("解析JSON内容失败: " + e.getMessage());
                        // 解析失败时保持原始内容
                    }
                }
                
                return content;
            } else {
                System.out.println("爬虫API返回错误状态: " + response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            System.out.println("调用爬虫API出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 获取新闻详情
     * @param id 新闻ID
     * @param type 新闻类型
     * @return 新闻详情
     */
    @Override
    public News getNewsDetail(Long id, String type) {
        if (id == null) {
            return null;
        }
        
        try {
            QueryWrapper<News> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", id);
            
            if (type != null && !type.isEmpty()) {
                // 如果提供了类型，从指定表查询
                return newsMapper.selectOneByType(type, id);
            } else {
                // 否则从默认表查询
                return newsMapper.selectById(id);
            }
        } catch (Exception e) {
            System.out.println("获取新闻详情出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 搜索新闻（标题匹配）
     * @param query 搜索关键词
     * @return 匹配的新闻列表
     */
    @Override
    public List<News> searchNews(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // 从所有新闻表中搜索
        List<News> results = new java.util.ArrayList<>();
        HashMap<String, String> tableMap = urlMatchSqlUtil.getUrls();
        
        // 用于跟踪已处理的标题，避免重复
        java.util.Set<String> processedTitles = new java.util.HashSet<>();
        
        // 遍历所有新闻表
        for (String tableName : tableMap.keySet()) {
            try {
                // 在当前表中搜索
                List<News> tableResults = newsMapper.searchNewsByTitle(tableName, query);
                
                // 过滤掉标题重复的结果
                for (News news : tableResults) {
                    if (news.getTitle() != null && !processedTitles.contains(news.getTitle())) {
                        results.add(news);
                        processedTitles.add(news.getTitle());
                    }
                }
            } catch (Exception e) {
                System.err.println("搜索表 " + tableName + " 时出错: " + e.getMessage());
            }
        }
        
        // 按发布时间排序（如果有结果）
        if (!results.isEmpty()) {
            results.sort((a, b) -> {
                LocalDateTime timeA = a.getPublishTime() != null ? 
                    parseTime(a.getPublishTime().toString()) : LocalDateTime.now();
                LocalDateTime timeB = b.getPublishTime() != null ? 
                    parseTime(b.getPublishTime().toString()) : LocalDateTime.now();
                return timeB.compareTo(timeA); // 降序排列（最新的排在前面）
            });
        }
        
        return results;
    }
    
    /**
     * 分页搜索新闻
     * @param query 搜索关键词
     * @param pageNum 页码
     * @param pageSize 每页记录数
     * @return 分页搜索结果
     */
    @Override
    public PageResult<News> pageSearchNews(String query, Integer pageNum, Integer pageSize) {
        if (query == null || query.trim().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0, pageNum, pageSize);
        }
        
        // 验证分页参数
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        
        // 计算offset
        int offset = (pageNum - 1) * pageSize;
        
        // 从所有新闻表中搜索
        List<News> records = new java.util.ArrayList<>();
        long totalCount = 0;
        HashMap<String, String> tableMap = urlMatchSqlUtil.getUrls();
        
        // 用于跟踪已处理的标题，避免重复
        java.util.Set<String> processedTitles = new java.util.HashSet<>();
        
        // 遍历所有新闻表
        for (String tableName : tableMap.keySet()) {
            try {
                // 统计当前表中的匹配记录数
                totalCount += newsMapper.countSearchResults(tableName, query);
                
                // 在当前表中分页搜索
                List<News> tableResults = newsMapper.searchNewsPageByTitle(tableName, query, offset, pageSize);
                
                // 过滤掉标题重复的结果
                for (News news : tableResults) {
                    if (news.getTitle() != null && !processedTitles.contains(news.getTitle())) {
                        records.add(news);
                        processedTitles.add(news.getTitle());
                    }
                }
            } catch (Exception e) {
                System.err.println("搜索表 " + tableName + " 时出错: " + e.getMessage());
            }
        }
        
        // 按发布时间排序（如果有结果）
        if (!records.isEmpty()) {
            records.sort((a, b) -> {
                LocalDateTime timeA = a.getPublishTime() != null ? 
                    parseTime(a.getPublishTime().toString()) : LocalDateTime.now();
                LocalDateTime timeB = b.getPublishTime() != null ? 
                    parseTime(b.getPublishTime().toString()) : LocalDateTime.now();
                return timeB.compareTo(timeA); // 降序排列（最新的排在前面）
            });
        }
        
        // 由于是跨表分页，可能返回的结果数超过了pageSize，需要进行修剪
        if (records.size() > pageSize) {
            records = records.subList(0, pageSize);
        }
        
        return new PageResult<>(records, totalCount, pageNum, pageSize);
    }

    /**
     * 定时任务：每天凌晨3点处理已有新闻并提取关键词
     * 针对已存在的没有提取关键词的新闻进行处理
     */
    @Override
    @Scheduled(cron = "0 0 3 * * ?")
    public void processExistingNewsKeywords() {
        System.out.println("开始处理已有新闻的关键词提取任务...");
        
        HashMap<String, String> typeMap = urlMatchSqlUtil.getUrls();
        int totalProcessed = 0;
        int processLimit = 100; // 每次处理的新闻数量限制
        
        for (Map.Entry<String, String> entry : typeMap.entrySet()) {
            String tableName = entry.getKey();
            int processed = 0;
            
            try {
                // 获取该类型下未处理的新闻
                List<News> unprocessedNews = newsMapper.selectNewsWithoutKeywords(tableName, processLimit);
                
                if (unprocessedNews == null || unprocessedNews.isEmpty()) {
                    System.out.println("没有找到未处理的 " + tableName + " 类型新闻");
                    continue;
                }
                
                System.out.println("找到 " + unprocessedNews.size() + " 条未处理的 " + tableName + " 新闻");
                
                for (News news : unprocessedNews) {
                    try {
                        // 获取新闻内容
                        String content = crawlNewsContent(news.getLink());
                        
                        // 提取关键词
                        int keywordCount = keywordService.extractAndSaveKeywords(
                            news.getId(), tableName, news.getTitle(), content);
                        
                        if (keywordCount > 0) {
                            processed++;
                            System.out.println("成功从新闻ID: " + news.getId() + " 提取 " + keywordCount + " 个关键词");
                        }
                        
                        // 每处理5篇新闻添加一个随机延迟，避免请求过于频繁
                        if (processed % 5 == 0 && processed > 0) {
                            long delayTime = 2000 + (long)(Math.random() * 3000);
                            System.out.println("已处理 " + processed + " 篇新闻，休息 " + (delayTime/1000) + " 秒...");
                            Thread.sleep(delayTime);
                        }
                    } catch (Exception e) {
                        System.err.println("处理新闻ID: " + news.getId() + " 时出错: " + e.getMessage());
                    }
                }
                
                totalProcessed += processed;
                System.out.println("成功处理 " + processed + " 条 " + tableName + " 类型新闻");
                
                // 每处理完一个类型，增加一个较长的休息时间
                if (processed > 0) {
                    long restTime = 5000 + (long)(Math.random() * 5000);
                    System.out.println("完成 " + tableName + " 类型处理，休息 " + (restTime/1000) + " 秒后继续下一类型...");
                    Thread.sleep(restTime);
                }
            } catch (Exception e) {
                System.err.println("处理 " + tableName + " 类型新闻时发生异常: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("关键词提取任务完成！共处理 " + totalProcessed + " 条新闻");
    }
}
