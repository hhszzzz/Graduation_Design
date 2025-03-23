package org.com.service.impl;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import com.huaban.analysis.jieba.WordDictionary;
import org.com.dto.KeywordCountDTO;
import org.com.dto.TrendDataDTO;
import org.com.entity.Keyword;
import org.com.mapper.KeywordMapper;
import org.com.service.KeywordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 关键词服务实现类
 */
@Service
public class KeywordServiceImpl implements KeywordService {
    
    @Autowired
    private KeywordMapper keywordMapper;
    
    // 结巴分词器
    private final JiebaSegmenter segmenter = new JiebaSegmenter();
    
    // 停用词表
    private Set<String> stopWords = new HashSet<>();
    
    /**
     * 初始化停用词和自定义词典
     */
    @PostConstruct
    public void init() {
        try {
            // 加载停用词
            loadStopWords();
            
            // 加载自定义词典
            loadUserDictionary();
            
        } catch (IOException e) {
            // 如果加载失败，使用默认的基础停用词
            stopWords = new HashSet<>(Arrays.asList(
                "的", "了", "和", "与", "或", "是", "在", "我", "你", "他", "她", "它", "们", "这", "那", "有",
                "将", "会", "就", "都", "而", "及", "但", "并", "等", "只", "被", "为", "所", "以", "之", "向",
                "上", "下", "左", "右", "中", "前", "后", "因", "为", "由", "对", "于", "与", "同", "时", "再",
                "能", "要", "把", "给", "来", "去", "让", "该", "还", "得", "说", "啊", "呢", "吗", "吧", "呀", 
                "个", "地", "得", "着", "使", "可", "便", "应", "又", "如", "何", "处", "从", "既"
            ));
        }
    }
    
    /**
     * 加载停用词文件
     */
    private void loadStopWords() throws IOException {
        ClassPathResource resource = new ClassPathResource("stopwords.txt");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim();
                if (StringUtils.hasText(word)) {
                    stopWords.add(word);
                }
            }
        }
    }
    
    /**
     * 加载自定义词典
     */
    private void loadUserDictionary() throws IOException {
        ClassPathResource resource = new ClassPathResource("userdictionary.txt");
        // WordDictionary.getInstance().loadUserDict需要Path参数，这里创建临时文件
        Path tempDictFile = Files.createTempFile("userdict", ".txt");
        try (InputStream in = resource.getInputStream();
             OutputStream out = Files.newOutputStream(tempDictFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        
        // 使用临时文件路径加载词典
        WordDictionary.getInstance().loadUserDict(tempDictFile);
        
        // 操作完成后删除临时文件
        Files.deleteIfExists(tempDictFile);
    }
    
    @Override
    public TrendDataDTO getKeywordTrend(LocalDate startDate, LocalDate endDate, String category) {
        // 转换为LocalDateTime
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        // 查询数据
        List<Map<String, Object>> rawData = keywordMapper.countKeywordsByDay(startDateTime, endDateTime, category);
        
        // 格式化日期
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // 按日期填充数据
        Map<String, Integer> dateMap = new HashMap<>();
        
        // 初始化日期范围内的所有日期
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dateMap.put(formatter.format(date), 0);
        }
        
        // 填充查询结果
        for (Map<String, Object> row : rawData) {
            if (row != null && row.get("date") != null && row.get("count") != null) {
                String date = row.get("date").toString();
                Integer count = ((Number) row.get("count")).intValue();
                dateMap.put(date, count);
            }
        }
        
        // 排序并分离日期和数量
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(dateMap.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());
        
        List<String> dates = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            dates.add(entry.getKey());
            counts.add(entry.getValue());
        }
        
        return new TrendDataDTO(dates, counts);
    }
    
    @Override
    public List<KeywordCountDTO> getTopKeywords(LocalDate startDate, LocalDate endDate, String category, int limit) {
        // 转换为LocalDateTime
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        // 查询数据
        return keywordMapper.getTopKeywords(startDateTime, endDateTime, category, limit);
    }
    
    @Override
    public boolean addKeyword(Keyword keyword) {
        if (keyword.getCreateTime() == null) {
            keyword.setCreateTime(LocalDateTime.now());
        }
        
        if (keyword.getRecordTime() == null) {
            keyword.setRecordTime(LocalDateTime.now());
        }
        
        return keywordMapper.insert(keyword) > 0;
    }
    
    @Override
    public int extractAndSaveKeywords(Long newsId, String newsType, String title, String content) {
        if (title == null && content == null) {
            return 0;
        }
        
        // 合并标题和内容进行分词，将标题权重提高
        String text = (title != null ? title + " " + title + " " : "") + (content != null ? content : "");
        
        // 使用jieba分词提取关键词
        List<SegToken> tokens = segmenter.process(text, JiebaSegmenter.SegMode.SEARCH);
        
        // 过滤停用词和过短的词，统计词频
        Map<String, Integer> wordFrequency = new HashMap<>();
        for (SegToken token : tokens) {
            String word = token.word.trim();
            if (word.length() >= 2 && !stopWords.contains(word)) {
                wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
            }
        }
        
        // 按词频排序，提取关键词
        List<Map.Entry<String, Integer>> sortedWords = wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
        
        // 保存关键词
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        
        // 最多保存前100个关键词
        int maxKeywords = Math.min(100, sortedWords.size());
        for (int i = 0; i < maxKeywords; i++) {
            String word = sortedWords.get(i).getKey();
            Keyword keyword = new Keyword();
            keyword.setKeyword(word);
            keyword.setNewsId(newsId);
            keyword.setNewsType(newsType);
            keyword.setNewsTitle(title);
            keyword.setRecordTime(now);
            keyword.setCreateTime(now);
            
            if (addKeyword(keyword)) {
                count++;
            }
        }
        
        return count;
    }
} 