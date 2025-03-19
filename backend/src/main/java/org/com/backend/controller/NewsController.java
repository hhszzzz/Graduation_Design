package org.com.backend.controller;

import org.com.backend.entity.News;
import org.com.backend.service.NewsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Resource
    private NewsService newsService;

    // 返回全部新闻数据给前端
    @GetMapping("/list")
    public ResponseEntity<List<News>> listNews() {
        return ResponseEntity.ok(newsService.listAllNews());
    }
}
