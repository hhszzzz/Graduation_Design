package org.com.backend.controller;

import org.com.backend.entity.News;
import org.com.backend.service.NewsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Resource
    private NewsService newsService;

    // 返回指定类型的新闻数据给前端
    @GetMapping("/list")
    public ResponseEntity<List<News>> listNews(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(newsService.listNewsByType(type));
    }
}
