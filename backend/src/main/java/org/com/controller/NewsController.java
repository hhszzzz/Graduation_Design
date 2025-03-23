package org.com.controller;

import org.com.entity.News;
import org.com.service.NewsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.com.dto.PageDTO;
import org.com.dto.PageResult;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Resource
    private NewsService newsService;

    // 返回指定类型的新闻数据给前端（不分页）
    @GetMapping("/list")
    public ResponseEntity<List<News>> listNews(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(newsService.listNewsByType(type));
    }
    
    /**
     * 分页获取新闻列表
     * @param type 新闻类型
     * @param pageDTO 分页参数
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResponseEntity<PageResult<News>> pageNews(
            @RequestParam(required = false) String type,
            PageDTO pageDTO) {
        // 验证并纠正分页参数
        if (pageDTO == null) {
            pageDTO = new PageDTO();
        }
        pageDTO.validate();
        
        return ResponseEntity.ok(newsService.pageNewsByType(type, pageDTO.getPageNum(), pageDTO.getPageSize()));
    }
    
    /**
     * 获取新闻详情
     * @param id 新闻ID
     * @param type 新闻类型（对应表名）
     * @return 新闻详情
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getNewsDetail(@PathVariable Long id, @RequestParam(required = false) String type) {
        News news = newsService.getNewsDetail(id, type);
        if (news != null) {
            return ResponseEntity.ok(news);
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("error", "未找到新闻");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    /**
     * 爬取新闻内容
     * @param request 包含URL的请求体
     * @return 爬取的内容
     */
    @PostMapping("/crawl_content")
    public ResponseEntity<?> crawlNewsContent(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        if (url == null || url.trim().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "URL不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        String content = newsService.crawlNewsContent(url);
        if (content != null) {
            // 进行内容处理
            content = processContent(content);
            
            Map<String, String> response = new HashMap<>();
            response.put("content", content);
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("error", "获取内容失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 处理爬取的内容，使其更适合前端显示
     * @param content 原始内容
     * @return 处理后的内容
     */
    private String processContent(String content) {
        if (content == null) {
            return "";
        }
        
        // 1. 将内容包装在段落标签中以正确显示
        if (!content.trim().startsWith("<")) {
            // 按换行符分割，把每段文本包装在<p>标签中
            String[] paragraphs = content.split("\\n");
            StringBuilder processed = new StringBuilder();
            for (String paragraph : paragraphs) {
                if (!paragraph.trim().isEmpty()) {
                    processed.append("<p>").append(paragraph.trim()).append("</p>");
                }
            }
            content = processed.toString();
        }
        
        // 2. 检查是否包含未解码的Unicode编码(\\uXXXX)
        if (content.contains("\\u")) {
            try {
                // 使用Java内置的Unicode解码功能
                content = decodeUnicode(content);
            } catch (Exception e) {
                System.err.println("Unicode解码失败: " + e.getMessage());
            }
        }
        
        return content;
    }
    
    /**
     * 解码包含Unicode转义序列的字符串
     * @param unicodeStr 包含Unicode转义的字符串
     * @return 解码后的字符串
     */
    private String decodeUnicode(String unicodeStr) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < unicodeStr.length()) {
            char c = unicodeStr.charAt(i++);
            if (c == '\\' && i < unicodeStr.length() && unicodeStr.charAt(i) == 'u') {
                // 跳过'u'
                i++;
                // 读取4位16进制数
                if (i + 4 <= unicodeStr.length()) {
                    String hex = unicodeStr.substring(i, i + 4);
                    try {
                        int codePoint = Integer.parseInt(hex, 16);
                        sb.append((char) codePoint);
                        i += 4;
                        continue;
                    } catch (NumberFormatException e) {
                        // 解析失败，当作普通字符处理
                    }
                }
                // 如果解析失败，回退并当作普通字符处理
                sb.append('\\');
                i--; // 回退，重新处理'u'
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 手动触发爬虫任务
     * @return 触发结果
     */
    @PostMapping("/trigger-crawl")
    public ResponseEntity<?> triggerCrawlTask() {
        try {
            // 调用已有的爬虫任务方法
            newsService.autoCrawlTask();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "爬虫任务已成功触发");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "触发爬虫任务失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 搜索新闻接口（不分页）
     * @param query 搜索关键词
     * @return 匹配的新闻列表
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchNews(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "搜索关键词不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            List<News> results = newsService.searchNews(query);
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "搜索失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 分页搜索新闻
     * @param query 搜索关键词
     * @param pageDTO 分页参数
     * @return 分页搜索结果
     */
    @GetMapping("/search/page")
    public ResponseEntity<?> pageSearchNews(
            @RequestParam String query,
            PageDTO pageDTO) {
        if (query == null || query.trim().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "搜索关键词不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 验证并纠正分页参数
        if (pageDTO == null) {
            pageDTO = new PageDTO();
        }
        pageDTO.validate();
        
        try {
            PageResult<News> results = newsService.pageSearchNews(query, pageDTO.getPageNum(), pageDTO.getPageSize());
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "搜索失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
