package org.com.utils;

import org.com.entity.News;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Component
public class CSVUtil {

    /**
     * 将新闻标题导出到CSV文件中
     * @param newsList 新闻列表
     * @param filePath 导出文件路径
     * @return 是否导出成功
     */
    public boolean exportNewsTitlesToCSV(List<News> newsList, String filePath) {
        Path path = Paths.get(filePath);
        
        // 确保目录存在
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            System.err.println("创建目录失败: " + e.getMessage());
            return false;
        }
        
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            
            // 写入CSV头部
            writer.write("news_id,title");
            writer.newLine();
            
            // 写入数据行
            for (News news : newsList) {
                writer.write(news.getId() + "," + escapeCSV(news.getTitle()));
                writer.newLine();
            }
            
            return true;
        } catch (IOException e) {
            System.err.println("写入CSV文件失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 将Map格式的新闻ID和标题导出到CSV文件中，专门用于直接数据库查询结果
     * @param newsDataList 新闻数据列表，包含id和title字段
     * @param filePath 导出文件路径
     * @return 是否导出成功
     */
    public boolean exportNewsMapToCSV(List<Map<String, Object>> newsDataList, String filePath) {
        Path path = Paths.get(filePath);
        
        // 确保目录存在
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            System.err.println("创建目录失败: " + e.getMessage());
            return false;
        }
        
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            
            // 写入CSV头部
            writer.write("news_id,title");
            writer.newLine();
            
            // 写入数据行
            for (Map<String, Object> newsData : newsDataList) {
                Object id = newsData.get("id");
                Object title = newsData.get("title");
                
                if (id != null && title != null) {
                    writer.write(id.toString() + "," + escapeCSV(title.toString()));
                    writer.newLine();
                }
            }
            
            return true;
        } catch (IOException e) {
            System.err.println("写入CSV文件失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 转义CSV字段中的特殊字符
     * @param field CSV字段值
     * @return 转义后的字段值
     */
    private String escapeCSV(String field) {
        if (field == null) {
            return "";
        }
        
        // 如果字段包含逗号、引号或换行符，则需要用引号包围并转义内部引号
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        
        return field;
    }
} 