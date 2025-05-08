package org.com.service.impl;

import org.com.entity.News;
import org.com.entity.User;
import org.com.entity.UserHistory;
import org.com.mapper.NewsMapper;
import org.com.mapper.UserHistoryMapper;
import org.com.mapper.UserMapper;
import org.com.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserHistoryMapper historyMapper;

    @Autowired
    private NewsMapper newsMapper;

    private static final int MIN_HISTORY_COUNT = 2; // 最小历史记录数量
    private static final int DEFAULT_SIMILAR_USER_COUNT = 10; // 默认相似用户数量

    @Override
    public List<News> getRecommendedNewsForUser(String username, int limit) {
        // 获取用户信息
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return getPopularNews(limit); // 用户不存在，返回热门推荐
        }

        // 获取用户浏览历史
        List<Long> viewedNewsIds = getUserViewedNewsIds(user.getId());
        if (viewedNewsIds.size() < MIN_HISTORY_COUNT) {
            return getPopularNews(limit); // 历史记录不足，返回热门推荐
        }

        // 获取相似用户
        List<Long> similarUserIds = getSimilarUsers(user.getId(), DEFAULT_SIMILAR_USER_COUNT);
        if (similarUserIds.isEmpty()) {
            return getPopularNews(limit); // 没有相似用户，返回热门推荐
        }

        // 获取相似用户浏览过但当前用户未浏览的新闻
        Set<Long> recommendedNewsIds = new HashSet<>();
        for (Long similarUserId : similarUserIds) {
            List<Long> similarUserViewedNewsIds = getUserViewedNewsIds(similarUserId);
            // 过滤出当前用户未浏览过的新闻
            similarUserViewedNewsIds.removeAll(viewedNewsIds);
            recommendedNewsIds.addAll(similarUserViewedNewsIds);
            
            // 如果已经找到足够多的推荐，就停止
            if (recommendedNewsIds.size() >= limit * 2) { // 获取2倍于需要的数量，后面再随机筛选
                break;
            }
        }

        // 如果推荐数量不足，补充热门新闻
        if (recommendedNewsIds.size() < limit) {
            List<News> popularNews = getPopularNews(limit * 2);
            for (News news : popularNews) {
                if (!viewedNewsIds.contains(news.getId())) {
                    recommendedNewsIds.add(news.getId());
                    if (recommendedNewsIds.size() >= limit) {
                        break;
                    }
                }
            }
        }

        // 随机选择指定数量的新闻（避免每次推荐都是相同的）
        List<Long> finalRecommendedIds = new ArrayList<>(recommendedNewsIds);
        Collections.shuffle(finalRecommendedIds);
        finalRecommendedIds = finalRecommendedIds.stream().limit(limit).collect(Collectors.toList());

        // 获取新闻详情
        return newsMapper.selectNewsByIds(finalRecommendedIds);
    }

    @Override
    public List<Long> getSimilarUsers(Long userId, int limit) {
        // 获取当前用户的浏览历史
        List<Long> currentUserNewsIds = getUserViewedNewsIds(userId);
        if (currentUserNewsIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取所有用户ID（排除当前用户）
        List<User> allUsers = userMapper.selectList(null);
        List<Long> otherUserIds = allUsers.stream()
                .map(User::getId)
                .filter(id -> !id.equals(userId))
                .collect(Collectors.toList());

        // 计算相似度并排序
        Map<Long, Double> similarityScores = new HashMap<>();
        for (Long otherUserId : otherUserIds) {
            List<Long> otherUserNewsIds = getUserViewedNewsIds(otherUserId);
            if (!otherUserNewsIds.isEmpty()) {
                double similarity = calculateJaccardSimilarity(currentUserNewsIds, otherUserNewsIds);
                similarityScores.put(otherUserId, similarity);
            }
        }

        // 按相似度降序排序并返回前limit个用户ID
        return similarityScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getUserViewedNewsIds(Long userId) {
        List<UserHistory> historyList = historyMapper.findByUserIdOrderByViewTimeDesc(userId);
        return historyList.stream()
                .map(UserHistory::getNewsId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<News> getPopularNews(int limit) {
        // 获取浏览次数最多的新闻
        // 从多个新闻表中获取并合并结果
        List<News> popularNews = new ArrayList<>();
        
        // 定义所有新闻表名
        List<String> newsTables = Arrays.asList(
            "daily_news", "comprehensive_news", "fashion_news", 
            "material_news", "exhibition_news", "product_news"
        );
        
        // 从每个表中获取一部分热门新闻
        int perTableLimit = (limit / newsTables.size()) + 1; // 每个表取的数量，稍微多取一点以确保总数足够
        
        for (String tableName : newsTables) {
            try {
                List<News> tableNews = newsMapper.findMostViewedNews(tableName, perTableLimit);
                if (tableNews != null) {
                    popularNews.addAll(tableNews);
                }
            } catch (Exception e) {
                // 如果某个表查询出错，继续查询其他表
                System.err.println("Error querying popular news from table " + tableName + ": " + e.getMessage());
            }
        }
        
        // 根据浏览量排序（如果需要的话）并限制总数
        if (popularNews.size() > limit) {
            // 随机打乱，增加多样性
            Collections.shuffle(popularNews);
            return popularNews.subList(0, limit);
        }
        
        return popularNews;
    }

    /**
     * 计算Jaccard相似度（两个集合的交集大小除以并集大小）
     */
    private double calculateJaccardSimilarity(List<Long> list1, List<Long> list2) {
        Set<Long> set1 = new HashSet<>(list1);
        Set<Long> set2 = new HashSet<>(list2);
        
        // 计算交集大小
        Set<Long> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        // 计算并集大小
        Set<Long> union = new HashSet<>(set1);
        union.addAll(set2);
        
        // Jaccard相似度 = 交集大小 / 并集大小
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }
} 