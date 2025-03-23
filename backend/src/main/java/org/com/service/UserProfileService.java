package org.com.service;

import org.com.entity.News;
import org.com.entity.User;
import org.com.entity.UserCollection;
import org.com.entity.UserHistory;
import org.com.mapper.NewsMapper;
import org.com.mapper.UserCollectionMapper;
import org.com.mapper.UserHistoryMapper;
import org.com.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserProfileService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private UserCollectionMapper collectionMapper;

    @Autowired
    private UserHistoryMapper historyMapper;
    
    // 新闻类型常量
    private static final String DEFAULT_NEWS_TYPE = "daily_news";

    /**
     * 获取用户收藏的新闻列表
     */
    public List<UserCollection> getUserCollections(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return Collections.emptyList();
        }
        
        List<UserCollection> collections = collectionMapper.findByUserIdOrderByCollectTimeDesc(user.getId());
        // 处理关联对象
        for (UserCollection collection : collections) {
            try {
                // 尝试根据新闻类型和ID获取新闻
                News news = newsMapper.selectOneByType(collection.getNewsType(), collection.getNewsId());
                if (news == null) {
                    // 如果找不到，使用基本的news信息
                    news = new News();
                    news.setId(collection.getNewsId());
                    news.setTitle("未知标题");
                }
                collection.setNews(news);
            } catch (Exception e) {
                // 发生异常则创建一个基本的news对象
                News news = new News();
                news.setId(collection.getNewsId());
                news.setTitle("未知标题 - " + collection.getNewsType());
                collection.setNews(news);
            }
            collection.setUser(user);
        }
        
        return collections;
    }

    /**
     * 获取用户浏览历史
     */
    public List<UserHistory> getUserHistory(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return Collections.emptyList();
        }
        
        List<UserHistory> historyList = historyMapper.findByUserIdOrderByViewTimeDesc(user.getId());
        // 处理关联对象
        for (UserHistory history : historyList) {
            try {
                // 尝试根据新闻类型和ID获取新闻
                News news = newsMapper.selectOneByType(history.getNewsType(), history.getNewsId());
                if (news == null) {
                    // 如果找不到，使用基本的news信息
                    news = new News();
                    news.setId(history.getNewsId());
                    news.setTitle("未知标题");
                }
                history.setNews(news);
            } catch (Exception e) {
                // 发生异常则创建一个基本的news对象
                News news = new News();
                news.setId(history.getNewsId());
                news.setTitle("未知标题 - " + history.getNewsType());
                history.setNews(news);
            }
            history.setUser(user);
        }
        
        return historyList;
    }

    /**
     * 收藏新闻
     */
    @Transactional
    public boolean collectNews(String username, Long newsId, String newsType) {
        User user = userMapper.findByUsername(username);

        if (user == null) {
            return false;
        }
        
        // 确保 newsType 不为空
        if (newsType == null || newsType.trim().isEmpty()) {
            newsType = DEFAULT_NEWS_TYPE;
        }
        
        // 尝试获取新闻详情
        News news = null;
        try {
            news = newsMapper.selectOneByType(newsType, newsId);
        } catch (Exception e) {
            // 如果获取失败，使用基本的news查询
            news = newsMapper.selectById(newsId);
        }
        
        // 如果查不到任何新闻信息，则无法收藏
        if (news == null) {
            // 创建一个临时News对象以便能够继续操作
            news = new News();
            news.setId(newsId);
        }
        
        // 检查是否已收藏
        UserCollection existingCollection = collectionMapper.findByUserIdAndNewsIdAndNewsType(user.getId(), newsId, newsType);
        if (existingCollection != null) {
            // 已收藏，更新时间
            existingCollection.setCollectTime(LocalDateTime.now());
            collectionMapper.updateById(existingCollection);
        } else {
            // 新增收藏
            UserCollection collection = new UserCollection();
            collection.setUserId(user.getId());
            collection.setNewsId(newsId);
            collection.setNewsType(newsType);
            collection.setCollectTime(LocalDateTime.now());
            collectionMapper.insert(collection);
        }
        
        return true;
    }
    
    /**
     * 收藏新闻 (兼容旧方法)
     */
    @Transactional
    public boolean collectNews(String username, Long newsId) {
        return collectNews(username, newsId, DEFAULT_NEWS_TYPE);
    }

    /**
     * 取消收藏新闻
     */
    @Transactional
    public boolean removeCollection(String username, Long newsId, String newsType) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return false;
        }
        
        // 确保 newsType 不为空
        if (newsType == null || newsType.trim().isEmpty()) {
            newsType = DEFAULT_NEWS_TYPE;
        }
        
        // 检查是否已收藏
        int count = collectionMapper.existsByUserIdAndNewsIdAndNewsType(user.getId(), newsId, newsType);
        if (count > 0) {
            collectionMapper.deleteByUserIdAndNewsIdAndNewsType(user.getId(), newsId, newsType);
            return true;
        }
        
        return false;
    }
    
    /**
     * 取消收藏新闻 (兼容旧方法)
     */
    @Transactional
    public boolean removeCollection(String username, Long newsId) {
        return removeCollection(username, newsId, DEFAULT_NEWS_TYPE);
    }

    /**
     * 记录新闻浏览历史
     */
    @Transactional
    public boolean recordNewsView(String username, Long newsId, String newsType) {
        User user = userMapper.findByUsername(username);
        
        if (user == null) {
            return false;
        }
        
        // 确保 newsType 不为空
        if (newsType == null || newsType.trim().isEmpty()) {
            newsType = DEFAULT_NEWS_TYPE;
        }
        
        // 尝试获取新闻详情
        News news = null;
        try {
            news = newsMapper.selectOneByType(newsType, newsId);
        } catch (Exception e) {
            // 如果获取失败，使用基本的news查询
            news = newsMapper.selectById(newsId);
        }
        
        // 如果查不到任何新闻信息，则创建一个临时对象
        if (news == null) {
            news = new News();
            news.setId(newsId);
        }
        
        // 检查是否有浏览记录
        UserHistory existingHistory = historyMapper.findByUserIdAndNewsIdAndNewsType(user.getId(), newsId, newsType);
        if (existingHistory != null) {
            // 更新浏览时间
            existingHistory.setViewTime(LocalDateTime.now());
            historyMapper.updateById(existingHistory);
        } else {
            // 新增浏览记录
            UserHistory history = new UserHistory();
            history.setUserId(user.getId());
            history.setNewsId(newsId);
            history.setNewsType(newsType);
            history.setViewTime(LocalDateTime.now());
            historyMapper.insert(history);
        }
        
        return true;
    }
    
    /**
     * 记录新闻浏览历史 (兼容旧方法)
     */
    @Transactional
    public boolean recordNewsView(String username, Long newsId) {
        return recordNewsView(username, newsId, DEFAULT_NEWS_TYPE);
    }

    /**
     * 检查用户是否已收藏某条新闻
     */
    public boolean isNewsCollected(String username, Long newsId, String newsType) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return false;
        }
        
        // 确保 newsType 不为空
        if (newsType == null || newsType.trim().isEmpty()) {
            newsType = DEFAULT_NEWS_TYPE;
        }
        
        int count = collectionMapper.existsByUserIdAndNewsIdAndNewsType(user.getId(), newsId, newsType);
        return count > 0;
    }
    
    /**
     * 检查用户是否已收藏某条新闻 (兼容旧方法)
     */
    public boolean isNewsCollected(String username, Long newsId) {
        return isNewsCollected(username, newsId, DEFAULT_NEWS_TYPE);
    }
} 