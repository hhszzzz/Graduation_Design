package org.com.controller;

import org.com.dto.KeywordCountDTO;
import org.com.dto.TrendDataDTO;
import org.com.service.KeywordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 关键词分析控制器
 */
@RestController
@RequestMapping("/api/keywords")
public class KeywordController {
    
    @Autowired
    private KeywordService keywordService;
    
    /**
     * 获取指定时间范围内的关键词趋势数据
     * @param days 天数范围，默认7天
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param category 新闻分类（可选）
     * @return 趋势数据
     */
    @GetMapping("/trend")
    public ResponseEntity<?> getKeywordTrend(
            @RequestParam(required = false, defaultValue = "7") String days,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "all") String category) {
        
        try {
            int daysNum = Integer.parseInt(days);
            LocalDate start = null;
            LocalDate end = LocalDate.now();
            
            // 如果提供了自定义日期范围，优先使用
            if (startDate != null && endDate != null) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    start = LocalDate.parse(startDate, formatter);
                    end = LocalDate.parse(endDate, formatter);
                } catch (DateTimeParseException e) {
                    return ResponseEntity.badRequest().body("日期格式错误，请使用yyyy-MM-dd格式");
                }
            } else {
                // 否则使用天数参数
                start = end.minusDays(daysNum);
            }
            
            TrendDataDTO trendData = keywordService.getKeywordTrend(start, end, category);
            return ResponseEntity.ok(trendData);
            
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("天数参数格式错误");
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "获取趋势数据失败: " + e.getMessage());
            e.printStackTrace(); // 打印详细错误信息
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取热门关键词列表
     * @param days 天数范围，默认7天
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param category 新闻分类（可选）
     * @param limit 返回关键词数量限制，默认50个
     * @return 关键词列表及出现次数
     */
    @GetMapping("/top")
    public ResponseEntity<?> getTopKeywords(
            @RequestParam(required = false, defaultValue = "7") String days,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "all") String category,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        
        try {
            int daysNum = Integer.parseInt(days);
            LocalDate start = null;
            LocalDate end = LocalDate.now();
            
            // 如果提供了自定义日期范围，优先使用
            if (startDate != null && endDate != null) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    start = LocalDate.parse(startDate, formatter);
                    end = LocalDate.parse(endDate, formatter);
                } catch (DateTimeParseException e) {
                    return ResponseEntity.badRequest().body("日期格式错误，请使用yyyy-MM-dd格式");
                }
            } else {
                // 否则使用天数参数
                start = end.minusDays(daysNum);
            }
            
            List<KeywordCountDTO> topKeywords = keywordService.getTopKeywords(start, end, category, limit);
            
            // 对关键词进行Unicode解码
            for (KeywordCountDTO keyword : topKeywords) {
                String decodedKeyword = decodeUnicodeString(keyword.getKeyword());
                keyword.setKeyword(decodedKeyword);
            }
            
            return ResponseEntity.ok(topKeywords);
            
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("天数参数格式错误");
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "获取热门关键词失败: " + e.getMessage());
            e.printStackTrace(); // 打印详细错误信息
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 解码包含Unicode字符的字符串
     * 识别形如u54c1或\\u54c1的编码并转换为实际字符
     *
     * @param input 输入字符串
     * @return 解码后的字符串
     */
    private String decodeUnicodeString(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // 修复缺少反斜杠的Unicode编码，u54c1 -> \u54c1
        Pattern pattern = Pattern.compile("(?<![\\\\])u([0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(input);
        String fixedInput = matcher.replaceAll("\\\\u$1");
        
        // 解码标准Unicode转义序列
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < fixedInput.length()) {
            char c = fixedInput.charAt(i++);
            if (c == '\\' && i < fixedInput.length() && fixedInput.charAt(i) == 'u') {
                i++; // 跳过 'u'
                if (i + 4 <= fixedInput.length()) {
                    String hex = fixedInput.substring(i, i + 4);
                    try {
                        int codePoint = Integer.parseInt(hex, 16);
                        result.append((char) codePoint);
                        i += 4;
                        continue;
                    } catch (NumberFormatException e) {
                        // 解析失败时，当作普通字符
                    }
                }
                // 处理失败回退
                result.append('\\');
                i--; // 重新处理 'u'
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
} 