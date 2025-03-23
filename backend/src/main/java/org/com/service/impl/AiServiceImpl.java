package org.com.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.com.config.AiConfig;
import org.com.service.AiService;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class AiServiceImpl implements AiService {

    @Resource
    private AiConfig aiConfig;

    @Resource(name = "aiRestTemplate")
    private RestTemplate restTemplate;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public SseEmitter summarizeNews(String url, String token) {
        // 创建一个SSE发射器，超时时间设置为10分钟
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(10));
        
        // 设置超时或出错处理
        emitter.onCompletion(() -> {
            System.out.println("摘要生成完成");
        });
        emitter.onTimeout(() -> {
            System.out.println("摘要生成超时");
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("生成摘要超时，请重试", MediaType.TEXT_EVENT_STREAM));
                emitter.complete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        emitter.onError(e -> {
            System.out.println("摘要生成出错: " + e.getMessage());
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("生成摘要出错: " + e.getMessage(), MediaType.TEXT_EVENT_STREAM));
                emitter.complete();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        
        // 在线程池中异步处理请求
        executorService.execute(() -> {
            try {
                processSummarizeRequest(url, token, emitter);
            } catch (Exception e) {
                // 异常处理
                try {
                    System.out.println("处理请求时发生错误: " + e.getMessage());
                    e.printStackTrace();
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("处理请求时发生错误: " + e.getMessage(), MediaType.TEXT_EVENT_STREAM));
                    emitter.complete();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        return emitter;
    }
    
    @Override
    public void summarizeNews(String url, String token, SseEmitter emitter) {
        // 在线程池中异步处理请求
        executorService.execute(() -> {
            try {
                processSummarizeRequest(url, token, emitter);
            } catch (Exception e) {
                // 异常处理
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("处理请求时发生错误: " + e.getMessage()));
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        });
    }
    
    // 获取网页内容
    private String fetchWebContent(String url) {
        try {
            // 构建爬虫API URL (使用专门的爬虫服务地址)
            String crawlApiUrl = aiConfig.getCrawlerBaseUrl() + "/api/crawl_context?url=" + url;
            System.out.println("调用爬虫API: " + crawlApiUrl);
            
            // 创建HTTP请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.Collections.singletonList(MediaType.TEXT_PLAIN));
            
            // 创建HTTP实体
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 发送GET请求
            ResponseEntity<String> response = restTemplate.exchange(
                crawlApiUrl, 
                HttpMethod.GET, 
                entity,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                String content = response.getBody();
                System.out.println("成功获取网页内容，长度: " + (content != null ? content.length() : 0) + " 字符");
                return content;
            } else {
                System.out.println("爬虫API返回错误状态: " + response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            System.out.println("调用爬虫API出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    // 处理摘要请求
    private void processSummarizeRequest(String url, String token, SseEmitter emitter) throws IOException, InterruptedException {
        // 检查token是否有效
        if (token == null || token.isEmpty()) {
            // 如果token无效，发送401错误
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("认证失败：未提供有效的访问令牌", MediaType.TEXT_EVENT_STREAM));
            emitter.complete();
            return;
        }
        
        // 打印token用于调试
        System.out.println("处理请求，token: " + (token.length() > 10 ? token.substring(0, 10) + "..." : token));
        
        try {
            // 发送思考过程开始事件
            emitter.send(SseEmitter.event()
                    .name("reasoning-start")
                    .data("开始分析新闻内容", MediaType.TEXT_EVENT_STREAM));
            
            // 第一步：调用爬虫API获取网页内容
            emitter.send(SseEmitter.event()
                    .name("reasoning")
                    .data("1. 正在获取网页内容: " + url, MediaType.TEXT_EVENT_STREAM));
            
            // 获取网页内容
            String pageContent = fetchWebContent(url);
            if (pageContent == null || pageContent.trim().isEmpty()) {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("无法获取网页内容，请检查URL是否有效", MediaType.TEXT_EVENT_STREAM));
                emitter.complete();
                return;
            }
            
            // 限制内容长度，避免过长
            String processedContent = truncateContent(pageContent, 6000);
            
            // 发送已获取内容的通知
            emitter.send(SseEmitter.event()
                    .name("reasoning")
                    .data("2. 已成功获取网页内容，共 " + processedContent.length() + " 字符", MediaType.TEXT_EVENT_STREAM));
            
            Thread.sleep(800);
            
            // 发送内容分析中的通知
            emitter.send(SseEmitter.event()
                    .name("reasoning")
                    .data("3. 分析文章结构和关键信息...", MediaType.TEXT_EVENT_STREAM));
            
            Thread.sleep(800);
            
            emitter.send(SseEmitter.event()
                    .name("reasoning")
                    .data("4. 提取主要观点和事实...", MediaType.TEXT_EVENT_STREAM));
            
            Thread.sleep(800);
            
            // 发送思考过程结束
            emitter.send(SseEmitter.event()
                    .name("reasoning-end")
                    .data("5. 分析完成，正在生成摘要", MediaType.TEXT_EVENT_STREAM));
            
            // 检查是否配置了API密钥
            String apiKey = aiConfig.getDeepseekApiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                // 如果未配置API密钥，则使用本地处理生成摘要
                generateLocalSummary(processedContent, url, emitter);
            } else {
                // 使用AI API生成摘要
                generateAiSummary(processedContent, url, apiKey, emitter);
            }
        } catch (Exception e) {
            System.out.println("处理摘要请求时出错: " + e.getMessage());
            e.printStackTrace();
            
            // 发送错误事件
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("处理请求时发生错误: " + e.getMessage(), MediaType.TEXT_EVENT_STREAM));
            emitter.complete();
        }
    }
    
    // 模拟DeepSeek API流式响应
    private void mockDeepseekStreamResponse(String url, SseEmitter emitter) throws IOException, InterruptedException {
        // 模拟接收思维链内容
        String[] reasoningSteps = {
            "1. 首先，我需要分析新闻链接: " + url,
            "2. 从链接中提取并解析新闻页面内容",
            "3. 识别新闻的主题和核心观点",
            "4. 分析文章的主要事实和数据",
            "5. 确定文章的时间背景和重要性",
            "6. 综合以上信息，生成一个简洁的摘要"
        };
        
        for (String step : reasoningSteps) {
            Thread.sleep(800);
            emitter.send(SseEmitter.event()
                    .name("reasoning")
                    .data(step, MediaType.TEXT_EVENT_STREAM));
        }
        
        // 模拟思维链结束
        emitter.send(SseEmitter.event()
                .name("reasoning-end")
                .data("分析完成，正在生成摘要", MediaType.TEXT_EVENT_STREAM));
        
        Thread.sleep(500);
        
        // 模拟分段发送摘要内容
        String[] summaryParts = {
            "这篇新闻主要报道了服装行业的最新发展趋势，",
            "重点关注了可持续发展和环保材料在服装制造中的应用。",
            "文章提到，越来越多的品牌正在采用再生纤维和有机棉花等环保材料，",
            "以减少生产过程中的环境影响。",
            "此外，文章还分析了消费者对可持续时尚的态度转变，",
            "认为环保意识正在成为影响购买决策的重要因素。",
            "总体而言，这篇新闻反映了服装行业正在向更可持续的方向发展，",
            "并预测这一趋势将在未来几年继续加强。"
        };
        
        for (String part : summaryParts) {
            Thread.sleep(200);
            emitter.send(SseEmitter.event()
                    .name("summary")
                    .data(part, MediaType.TEXT_EVENT_STREAM));
        }
        
        // 完成
        emitter.send(SseEmitter.event()
                .name("complete")
                .data("摘要生成完成", MediaType.TEXT_EVENT_STREAM));
        
        emitter.complete();
    }
    
    // 模拟摘要生成（当API密钥未配置时使用）
    private void simulateSummarize(String url, SseEmitter emitter) throws IOException, InterruptedException {
        // 发送思考过程（模拟）
        String[] reasoningSteps = {
            "1. 访问新闻链接: " + url,
            "2. 提取页面主要内容...",
            "3. 分析文章结构和关键信息...",
            "4. 识别主要观点和事实...",
            "5. 整合信息生成摘要..."
        };
        
        // 发送思考过程开始
        emitter.send(SseEmitter.event()
                .name("reasoning-start")
                .data("开始分析新闻内容", MediaType.TEXT_EVENT_STREAM));
        
        for (String step : reasoningSteps) {
            // 模拟思考过程
            Thread.sleep(800);
            emitter.send(SseEmitter.event()
                    .name("reasoning")
                    .data(step, MediaType.TEXT_EVENT_STREAM));
        }
        
        emitter.send(SseEmitter.event()
                .name("reasoning-end")
                .data("分析完成，正在生成摘要", MediaType.TEXT_EVENT_STREAM));
        
        // 模拟生成摘要
        Thread.sleep(500);
        
        // 摘要内容
        String summary = "这篇新闻主要报道了一项重要的行业发展动态。文章分析了当前市场趋势和未来发展方向，强调了技术创新对行业的推动作用。";
        String summary2 = "同时，文章还提到了相关政策变化可能带来的影响，以及企业应对策略。整体来看，这是一篇信息量丰富、视角全面的行业分析报道。";
        
        // 模拟流式输出摘要
        for (int i = 0; i < summary.length(); i += 10) {
            int end = Math.min(i + 10, summary.length());
            emitter.send(SseEmitter.event()
                    .name("summary")
                    .data(summary.substring(i, end), MediaType.TEXT_EVENT_STREAM));
            Thread.sleep(100); // 模拟网络延迟
        }
        
        for (int i = 0; i < summary2.length(); i += 10) {
            int end = Math.min(i + 10, summary2.length());
            emitter.send(SseEmitter.event()
                    .name("summary")
                    .data(summary2.substring(i, end), MediaType.TEXT_EVENT_STREAM));
            Thread.sleep(100); // 模拟网络延迟
        }
        
        // 完成
        emitter.send(SseEmitter.event()
                .name("complete")
                .data("摘要生成完成", MediaType.TEXT_EVENT_STREAM));
        
        emitter.complete();
    }
    
    // 截断内容以避免过长
    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        return content.length() <= maxLength ? content : content.substring(0, maxLength) + "...";
    }
    
    // 安全地完成SSE发射器
    private void safeEmitterComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception e) {
            System.out.println("关闭SSE连接时出错: " + e.getMessage());
        }
    }
    
    // 安全地发送SSE事件
    private void sendSseEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data, MediaType.TEXT_EVENT_STREAM));
        } catch (Exception e) {
            System.out.println("发送SSE事件时出错: " + e.getMessage());
            // 如果发送失败，尝试完成emitter
            safeEmitterComplete(emitter);
        }
    }

    private void generateAiSummary(String pageContent, String url, String apiKey, SseEmitter emitter) throws IOException, InterruptedException {
        try {
            // 构建DeepSeek API请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // 正确格式化Authorization头
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 构建提示词 - 使用配置的模板
            String prompt = aiConfig.getSummaryTemplate() + "\n\n内容:\n" + pageContent;
            
            // 构建符合DeepSeek API要求的请求体
            Map<String, Object> requestBody = new HashMap<>();
            
            // 设置模型
            requestBody.put("model", aiConfig.getAiModel());
            
            // 构建消息数组
            ArrayList<Map<String, Object>> messages = new ArrayList<>();
            
            // 系统角色消息
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "assistant");
            systemMessage.put("content", "你是一个专业的新闻摘要助手。你的任务是生成简短的摘要（不超过150字），提取文章的核心观点和关键信息，禁止直接复制原文。请务必用你自己的语言重新组织表达，确保摘要简明扼要，重点突出。回答必须是中文。");
            messages.add(systemMessage);
            
            // 用户消息
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            
            // 设置其他参数
            requestBody.put("temperature", 0.3);  // 降低温度，使输出更确定性
            requestBody.put("max_tokens", 2048);  // 限制回复长度
            requestBody.put("stream", false);  // 使用流式输出，改为普通请求
            
            // 如果是reasoner模型，设置推理努力度
            if (aiConfig.getAiModel().contains("reasoner")) {
                requestBody.put("reasoning_effort", 0.7); // 设置较高的推理度，更彻底地分析
            }
            
            // 设置响应格式
            Map<String, String> responseFormat = new HashMap<>();
            responseFormat.put("type", "text");
            requestBody.put("response_format", responseFormat);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // 构建完整的API URL
            String apiUrl = aiConfig.getDeepseekBaseUrl() + "/v1/chat/completions";
            System.out.println("调用DeepSeek API: " + apiUrl);
            
            // 发送请求通知
            sendSseEvent(emitter, "reasoning", "正在调用AI进行摘要生成...");
            
            // 尝试进行真实API调用
            try {
                // 配置RestTemplate，设置为更长的超时时间
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                // 设置连接超时为1分钟
                factory.setConnectTimeout(60000);
                // 设置读取超时为8分钟
                factory.setReadTimeout(480000);
                
                RestTemplate streamingTemplate = new RestTemplate(factory);
                
                // 使用postForEntity直接发送请求
                ResponseEntity<String> response = streamingTemplate.postForEntity(
                    apiUrl,
                    requestEntity,
                    String.class
                );
                
                // 检查响应状态
                if (response.getStatusCode() == HttpStatus.OK) {
                    // 由于不是流式响应，直接处理全部内容
                    String responseContent = response.getBody();
                    if (responseContent != null) {
                        // 解析JSON响应
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode rootNode = objectMapper.readTree(responseContent);
                        System.out.println("API响应: " + responseContent.substring(0, Math.min(responseContent.length(), 500)) + "...");
                        
                        // 提取AI回复的content内容
                        String aiContent = "";
                        JsonNode choicesNode = rootNode.path("choices");
                        if (choicesNode.isArray() && choicesNode.size() > 0) {
                            JsonNode firstChoice = choicesNode.get(0);
                            JsonNode messageNode = firstChoice.path("message");
                            if (!messageNode.isMissingNode()) {
                                // 尝试提取content
                                JsonNode contentNode = messageNode.path("content");
                                if (!contentNode.isMissingNode()) {
                                    aiContent = contentNode.asText("");
                                    System.out.println("提取到content: " + aiContent.substring(0, Math.min(aiContent.length(), 200)) + "...");
                                }
                                
                                // 检查是否有reasoning_content字段（DeepSeek-reasoner模型特有）
                                JsonNode reasoningNode = messageNode.path("reasoning_content");
                                if (!reasoningNode.isMissingNode()) {
                                    String reasoningContent = reasoningNode.asText("");
                                    System.out.println("发现reasoning_content字段，长度: " + reasoningContent.length());
                                    
                                    // 如果正文内容过长或与原文相似，优先使用推理内容作为摘要
                                    if (aiContent.length() > 1000 || aiContent.startsWith(pageContent.substring(0, Math.min(pageContent.length(), 50)))) {
                                        System.out.println("使用reasoning_content作为摘要");
                                        // 从reasoning中提取最后一部分作为摘要（假设最后部分是总结）
                                        String[] paragraphs = reasoningContent.split("\n\n");
                                        if (paragraphs.length > 0) {
                                            aiContent = paragraphs[paragraphs.length - 1];
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 如果提取的内容过长，则进行截断
                        if (aiContent.length() > 800) {
                            System.out.println("摘要过长，进行截断");
                            aiContent = aiContent.substring(0, 800) + "...";
                        }
                        
                        // 如果提取的内容与原文非常相似，使用备选方案
                        if (aiContent.isEmpty() || 
                            pageContent.contains(aiContent) || 
                            aiContent.length() > pageContent.length() * 0.8 ||
                            containsSubstring(pageContent, aiContent, 50)) {
                            System.out.println("提取的内容与原文相似度过高或为空，使用备选方案生成摘要");
                            sendSseEvent(emitter, "warning", "API返回内容可能是复制原文，使用备选方案生成摘要");
                            
                            // 使用模拟数据作为回退
                            mockAiSummaryFromContent(pageContent, url, emitter);
                            return;
                        }
                        
                        // 分段发送，模拟流式输出
                        for (int i = 0; i < aiContent.length(); i += 10) {
                            int end = Math.min(i + 10, aiContent.length());
                            sendSseEvent(emitter, "summary", aiContent.substring(i, end));
                            Thread.sleep(50); // 模拟延迟
                        }
                        
                        // 发送完成事件
                        sendSseEvent(emitter, "complete", "摘要生成完成");
                        safeEmitterComplete(emitter);
                    }
                } else {
                    // 处理错误
                    System.out.println("API调用失败，状态码: " + response.getStatusCode());
                    sendSseEvent(emitter, "error", "API调用失败，正在使用本地模拟");
                    
                    // 使用模拟数据作为回退
                    mockAiSummaryFromContent(pageContent, url, emitter);
                }
            } catch (Exception e) {
                System.out.println("调用DeepSeek API出错: " + e.getMessage());
                e.printStackTrace();
                
                // 检查是否为管道破裂错误
                if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
                    System.out.println("检测到管道破裂错误，客户端可能已断开连接");
                    safeEmitterComplete(emitter);
                    return;
                }
                
                // 发送错误通知并使用备选方案
                sendSseEvent(emitter, "error", "API调用失败，正在使用本地模拟: " + e.getMessage());
                
                // 使用模拟数据作为备选
                mockAiSummaryFromContent(pageContent, url, emitter);
            }
        } catch (Exception e) {
            System.out.println("构建API请求出错: " + e.getMessage());
            e.printStackTrace();
            
            // 检查是否为管道破裂错误
            if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
                System.out.println("检测到管道破裂错误，客户端可能已断开连接");
                safeEmitterComplete(emitter);
                return;
            }
            
            sendSseEvent(emitter, "error", "构建API请求出错，正在使用本地模拟");
            
            // 使用模拟数据作为备选
            mockAiSummaryFromContent(pageContent, url, emitter);
        }
    }
    
    // 本地生成摘要（无API密钥时使用）
    private void generateLocalSummary(String content, String url, SseEmitter emitter) throws IOException, InterruptedException {
        // 简单的提取式摘要生成
        // 实际项目中可以使用更复杂的算法
        
        // 为模拟效果，这里发送流式摘要
        String[] sentences = content.split("\\. ");
        
        // 选择前几个句子作为摘要（简单示例）
        int maxSentences = Math.min(sentences.length, 5);
        
        StringBuilder summary = new StringBuilder();
        summary.append("根据网页内容，以下是主要信息：\n\n");
        
        for (int i = 0; i < maxSentences; i++) {
            if (sentences[i].trim().length() > 15) {  // 忽略过短的句子
                summary.append(sentences[i].trim()).append("。 ");
            }
        }
        
        summary.append("\n\n(注: 此摘要由简单算法自动生成，可能不够全面)");
        
        // 模拟流式输出摘要
        String summaryText = summary.toString();
        for (int i = 0; i < summaryText.length(); i += 10) {
            int end = Math.min(i + 10, summaryText.length());
            try {
                sendSseEvent(emitter, "summary", summaryText.substring(i, end));
                Thread.sleep(50); // 模拟网络延迟
            } catch (Exception e) {
                System.out.println("发送摘要片段时出错: " + e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
                    System.out.println("检测到管道破裂错误，客户端可能已断开连接");
                    return;
                }
            }
        }
        
        // 完成
        try {
            sendSseEvent(emitter, "complete", "摘要生成完成");
            safeEmitterComplete(emitter);
        } catch (Exception e) {
            System.out.println("发送完成事件时出错: " + e.getMessage());
        }
    }
    
    // 从实际内容生成摘要
    private void mockAiSummaryFromContent(String content, String url, SseEmitter emitter) throws IOException, InterruptedException {
        // 从内容中提取关键句子
        String[] sentences = content.split("\\. |\\.|。|！|\\!|\\?|？");
        
        // 过滤无效句子
        java.util.List<String> validSentences = new java.util.ArrayList<>();
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() > 20 && trimmed.length() < 200) {
                validSentences.add(trimmed);
            }
        }
        
        // 构建摘要
        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append("根据网页内容分析，");
        
        // 添加内容概要
        if (validSentences.size() > 3) {
            // 选择3-5个句子作为摘要
            int sentenceCount = Math.min(5, validSentences.size());
            java.util.Random random = new java.util.Random();
            
            // 避免重复选择相同的句子
            java.util.Set<Integer> selectedIndexes = new java.util.HashSet<>();
            while (selectedIndexes.size() < sentenceCount) {
                selectedIndexes.add(random.nextInt(validSentences.size()));
            }
            
            // 拼接句子
            summaryBuilder.append("该文章主要包含以下要点：\n\n");
            int count = 1;
            for (Integer index : selectedIndexes) {
                summaryBuilder.append(count++).append(". ").append(validSentences.get(index)).append("。\n");
            }
        } else if (!validSentences.isEmpty()) {
            // 如果句子太少，直接使用所有句子
            summaryBuilder.append("该文章内容简洁，主要表达：\n\n");
            for (String sentence : validSentences) {
                summaryBuilder.append("- ").append(sentence).append("。\n");
            }
        } else {
            // 如果没有有效句子，生成通用摘要
            summaryBuilder.append("该页面内容较少或格式特殊，无法提取出有意义的摘要。建议直接访问原始链接查看完整内容。");
        }
        
        // 添加总结
        summaryBuilder.append("\n总的来说，这是一篇关于");
        
        // 根据URL判断可能的主题
        if (url.contains("news") || url.contains("article")) {
            summaryBuilder.append("新闻资讯");
        } else if (url.contains("product")) {
            summaryBuilder.append("产品信息");
        } else if (url.contains("tech") || url.contains("technology")) {
            summaryBuilder.append("技术相关");
        } else if (url.contains("fashion") || url.contains("clothing")) {
            summaryBuilder.append("服装时尚");
        } else {
            summaryBuilder.append("行业信息");
        }
        
        summaryBuilder.append("的文章，提供了相关领域的重要信息。");
        
        String summary = summaryBuilder.toString();
        
        // 模拟流式输出摘要
        for (int i = 0; i < summary.length(); i += 10) {
            int end = Math.min(i + 10, summary.length());
            try {
                sendSseEvent(emitter, "summary", summary.substring(i, end));
                Thread.sleep(80); // 模拟网络延迟
            } catch (Exception e) {
                System.out.println("发送摘要片段时出错: " + e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
                    System.out.println("检测到管道破裂错误，客户端可能已断开连接");
                    return;
                }
            }
        }
        
        // 完成
        try {
            sendSseEvent(emitter, "complete", "摘要生成完成");
            safeEmitterComplete(emitter);
        } catch (Exception e) {
            System.out.println("发送完成事件时出错: " + e.getMessage());
        }
    }

    // 检查字符串是否包含相当大的子串（用于检测AI是否复制了原文的大部分内容）
    private boolean containsSubstring(String source, String content, int minLength) {
        // 如果内容很短，不检查
        if (content.length() < minLength) {
            return false;
        }
        
        // 对于较长的内容，检查是否有长段落与原文匹配
        for (int i = 0; i < content.length() - minLength; i++) {
            String chunk = content.substring(i, i + minLength);
            if (source.contains(chunk)) {
                System.out.println("在原文中发现匹配的大段内容: " + chunk);
                return true;
            }
        }
        
        return false;
    }
} 