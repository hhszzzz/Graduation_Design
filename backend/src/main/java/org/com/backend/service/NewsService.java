package org.com.backend.service;

import org.com.backend.entity.News;

import java.util.List;

public interface NewsService {

    // 调用Python API并存储到数据库
    public void autoCrawlTask();

    // 根据类型获取新闻列表
    List<News> listNewsByType(String type);
}
