package org.com.controller;

import org.com.entity.UserCollection;
import org.com.entity.UserHistory;
import org.com.security.SecurityUtils;
import org.com.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;

    /**
     * 获取用户收藏的新闻列表
     */
    @GetMapping("/collections")
    public ResponseEntity<?> getUserCollections() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body("用户未登录");
        }
        
        List<UserCollection> collections = userProfileService.getUserCollections(username);
        return ResponseEntity.ok(collections);
    }

    /**
     * 收藏新闻
     */
    @PostMapping("/collections/{newsId}")
    public ResponseEntity<?> collectNews(
            @PathVariable Long newsId,
            @RequestParam(required = false) String newsType) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body("用户未登录");
        }
        
        boolean success = userProfileService.collectNews(username, newsId, newsType);
        if (success) {
            return ResponseEntity.ok().body("收藏成功");
        } else {
            return ResponseEntity.badRequest().body("收藏失败");
        }
    }

    /**
     * 取消收藏新闻
     */
    @DeleteMapping("/collections/{newsId}")
    public ResponseEntity<?> removeCollection(
            @PathVariable Long newsId,
            @RequestParam(required = false) String newsType) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body("用户未登录");
        }
        
        boolean success = userProfileService.removeCollection(username, newsId, newsType);
        if (success) {
            return ResponseEntity.ok().body("取消收藏成功");
        } else {
            return ResponseEntity.badRequest().body("取消收藏失败");
        }
    }

    /**
     * 获取用户浏览历史
     */
    @GetMapping("/history")
    public ResponseEntity<?> getUserHistory() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body("用户未登录");
        }
        
        List<UserHistory> history = userProfileService.getUserHistory(username);
        return ResponseEntity.ok(history);
    }

    /**
     * 记录新闻浏览历史
     */
    @PostMapping("/history/{newsId}")
    public ResponseEntity<?> recordNewsView(
            @PathVariable Long newsId,
            @RequestParam(required = false) String newsType) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body("用户未登录");
        }
        
        boolean success = userProfileService.recordNewsView(username, newsId, newsType);
        if (success) {
            return ResponseEntity.ok().body("记录浏览历史成功");
        } else {
            return ResponseEntity.badRequest().body("记录浏览历史失败");
        }
    }

    /**
     * 检查用户是否已收藏某条新闻
     */
    @GetMapping("/is-collected/{newsId}")
    public ResponseEntity<?> isNewsCollected(
            @PathVariable Long newsId,
            @RequestParam(required = false) String newsType) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body("用户未登录");
        }
        
        boolean isCollected = userProfileService.isNewsCollected(username, newsId, newsType);
        Map<String, Boolean> result = new HashMap<>();
        result.put("isCollected", isCollected);
        return ResponseEntity.ok(result);
    }
} 