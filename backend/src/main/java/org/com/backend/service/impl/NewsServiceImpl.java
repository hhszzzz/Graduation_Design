package org.com.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.com.backend.entity.News;
import org.com.backend.mapper.NewsMapper;
import org.com.backend.service.NewsService;
import org.com.backend.utils.UrlMatchSqlUtil;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NewsServiceImpl implements NewsService {

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private NewsMapper newsMapper;

    @Resource
    private UrlMatchSqlUtil urlMatchSqlUtil;

    // 定时任务：每30分钟请求一次Python爬虫服务
    @Override
    // 每小时执行一次
    @Scheduled(cron = "0 0 * * * ?")
    public void autoCrawlTask() {
        System.out.println("自动爬虫任务启动...");

        HashMap<String,String> map = urlMatchSqlUtil.getUrls();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String url = entry.getValue();
            String sql = entry.getKey();
            System.out.println("当前爬取URL: " + url);

            try {
                ResponseEntity<List<News>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<News>>() {}
                );

                List<News> newsList = response.getBody();

                if (newsList != null) {
                    for (News news : newsList) {
                        QueryWrapper<News> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("title", news.getTitle());
                        if (newsMapper.selectCount(queryWrapper) == 0) {
                            // 手动安全解析时间后再入库
                            LocalDateTime publishTime = parseTime(news.getPublishTime());
                            // 这里直接调用mapper插入
                            newsMapper.insertNewsDynamicTable(sql, news.getTitle(), news.getLink(), publishTime);
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println("爬取 " + url + " 时发生异常: " + e.getMessage());
            }
        }

        System.out.println("自动爬虫任务完成！");
    }

    // 兼容多种时间格式（包含有秒和无秒的情况）
    private LocalDateTime parseTime(String timeStr) {
        DateTimeFormatter formatterWithSeconds = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatterWithoutTime = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if (timeStr == null || timeStr.isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            // 先尝试有秒的格式
            return LocalDateTime.parse(timeStr, formatterWithSeconds);
        } catch (DateTimeParseException e) {
            try {
                // 再尝试只有日期的格式
                LocalDate date = LocalDate.parse(timeStr, formatterWithoutTime);
                return date.atStartOfDay();
            } catch (DateTimeParseException ex) {
                // 所有解析失败时，使用当前时间
                System.out.println("时间格式错误，使用当前时间替代：" + timeStr);
                return LocalDateTime.now();
            }
        }
    }

    // 根据类型获取新闻列表
    @Override
    public List<News> listNewsByType(String type) {
        HashMap<String, String> map = urlMatchSqlUtil.getUrls();
        if (type == null || type.isEmpty()) {
            return newsMapper.selectList(null);
        }
        return newsMapper.selectListByType(type);
    }
}
