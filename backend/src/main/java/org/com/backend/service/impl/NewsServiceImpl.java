package org.com.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.com.backend.entity.News;
import org.com.backend.mapper.NewsMapper;
import org.com.backend.service.NewsService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NewsServiceImpl implements NewsService {

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private NewsMapper newsMapper;

    // 定时任务：每30分钟请求一次Python爬虫服务
    @Override
    @Scheduled(cron = "0 0/30 * * * ?") // 每30分钟触发一次
    public void scheduledCrawlTask() {
        // System.out.println("开始自动执行爬虫任务...");
        // 明确告诉RestTemplate数据的类型，避免泛型擦除问题
        ResponseEntity<List<Map<String, String>>> response = restTemplate.exchange(
                "http://127.0.0.1:9999/api/crawl",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, String>>>() {}
        );
        List<Map<String, String>> newsData = response.getBody();
        System.out.println(newsData);

        if (newsData != null) {
            for (Map<String, String> item : newsData) {
                // 检查数据库是否已有记录，避免重复插入
                QueryWrapper<News> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("title", item.get("title"));
                Long count = newsMapper.selectCount(queryWrapper);
                if (count == 0) {
                    News news = new News();
                    news.setTitle(item.get("title"));
                    news.setLink(item.get("link"));
                    newsMapper.insert(news);
                }
            }
            System.out.println("爬虫任务执行完成。");
        }
    }

    // 返回全部新闻数据给前端
    @Override
    public List<News> listAllNews() {
        return newsMapper.selectList(null);
    }
}
