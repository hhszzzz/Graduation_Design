package org.com.service;

import org.com.entity.News;
import java.util.List;

/**
 * 新闻推荐服务接口
 */
public interface RecommendationService {
    
    /**
     * 基于用户浏览历史的协同过滤推荐
     * @param username 用户名
     * @param limit 推荐数量限制
     * @return 推荐的新闻列表
     */
    List<News> getRecommendedNewsForUser(String username, int limit);
    
    /**
     * 获取相似用户
     * @param userId 当前用户ID
     * @param limit 相似用户数量限制
     * @return 相似用户ID列表
     */
    List<Long> getSimilarUsers(Long userId, int limit);
    
    /**
     * 获取用户浏览过的新闻ID集合
     * @param userId 用户ID
     * @return 浏览过的新闻ID列表
     */
    List<Long> getUserViewedNewsIds(Long userId);
    
    /**
     * 当用户没有足够的浏览历史或无法找到相似用户时，获取热门新闻作为推荐
     * @param limit 数量限制
     * @return 热门新闻列表
     */
    List<News> getPopularNews(int limit);
} 