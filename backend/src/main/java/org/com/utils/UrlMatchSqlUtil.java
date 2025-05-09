package org.com.utils;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
public class UrlMatchSqlUtil {
    private HashMap<String, String> urls = new HashMap<String, String>() {{
        put("daily_news", "http://localhost:9999/api/crawl1");
        put("comprehensive_news", "http://localhost:9999/api/crawl2");
        put("fashion_news", "http://localhost:9999/api/crawl3");
        put("material_news", "http://localhost:9999/api/crawl4");
        put("exhibition_news", "http://localhost:9999/api/crawl5");
        put("product_news", "http://localhost:9999/api/crawl6");
    }};
}
