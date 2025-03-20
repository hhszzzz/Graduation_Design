package org.com.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * AI服务配置
 */
@Configuration
public class AiConfig {

    // DeepSeek API相关配置
    @Value("${ai.deepseek.api-key:sk-5b5f00bcaf924b4f965cb8d0486ba187}")
    private String deepseekApiKey;
    
    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;
    
    @Value("${ai.model:deepseek-reasoner}")
    private String aiModel;
    
    // 本地服务基础URL
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    // 爬虫服务URL
    @Value("${crawler.base-url:http://127.0.0.1:9999}")
    private String crawlerBaseUrl;
    
    /**
     * 获取API密钥
     */
    public String getDeepseekApiKey() {
        return deepseekApiKey;
    }
    
    /**
     * 获取API基础URL
     */
    public String getDeepseekBaseUrl() {
        return deepseekBaseUrl;
    }
    
    /**
     * 获取AI模型名称
     */
    public String getAiModel() {
        return aiModel;
    }
    
    /**
     * 获取应用基础URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * 获取爬虫服务URL
     */
    public String getCrawlerBaseUrl() {
        return crawlerBaseUrl;
    }
    
    /**
     * AI请求专用RestTemplate
     */
    @Bean(name = "aiRestTemplate")
    public RestTemplate aiRestTemplate() {
        return new RestTemplate(clientHttpRequestFactory());
    }
    
    /**
     * 创建HTTP请求工厂
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 设置较长的超时时间，因为AI请求可能需要较长时间
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(60000);
        return factory;
    }
} 