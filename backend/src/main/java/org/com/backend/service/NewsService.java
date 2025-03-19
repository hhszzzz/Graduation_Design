package org.com.backend.service;

import org.com.backend.entity.News;

import java.util.List;

public interface NewsService {

    // 调用Python API并存储到数据库
    public void scheduledCrawlTask();

    public List<News> listAllNews();
}
