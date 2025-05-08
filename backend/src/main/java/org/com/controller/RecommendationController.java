package org.com.controller;

import org.com.common.Result;
import org.com.entity.News;
import org.com.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendation")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;
    
    private static final int DEFAULT_LIMIT = 6; // 默认推荐数量

    /**
     * 获取当前用户的个性化推荐
     * @param limit 推荐数量限制，默认为6
     * @return 推荐新闻列表
     */
    @GetMapping("/personalized")
    public Result<List<News>> getPersonalizedRecommendations(
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        
        // 获取当前登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // 如果用户未登录或是匿名用户，返回热门推荐
        if (username == null || "anonymousUser".equals(username)) {
            List<News> popularNews = recommendationService.getPopularNews(limit);
            return Result.success(popularNews);
        }
        
        // 否则返回个性化推荐
        List<News> recommendedNews = recommendationService.getRecommendedNewsForUser(username, limit);
        return Result.success(recommendedNews);
    }
    
    /**
     * 获取热门新闻推荐
     * @param limit 推荐数量限制，默认为6
     * @return 热门新闻列表
     */
    @GetMapping("/popular")
    public Result<List<News>> getPopularRecommendations(
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        List<News> popularNews = recommendationService.getPopularNews(limit);
        return Result.success(popularNews);
    }
} 