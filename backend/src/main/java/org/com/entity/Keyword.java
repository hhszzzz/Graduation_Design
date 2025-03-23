package org.com.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关键词实体类
 */
@Data
@TableName("keyword")
public class Keyword {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关键词
     */
    private String keyword;
    
    /**
     * 新闻类型
     */
    private String newsType;
    
    /**
     * 新闻ID
     */
    private Long newsId;
    
    /**
     * 新闻标题
     */
    private String newsTitle;
    
    /**
     * 记录时间
     */
    private LocalDateTime recordTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}