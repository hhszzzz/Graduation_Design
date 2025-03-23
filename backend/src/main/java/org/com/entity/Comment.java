package org.com.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("comment")
public class Comment {
    
    /**
     * 评论ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 新闻ID
     */
    private Long newsId;
    
    /**
     * 新闻类型
     */
    private String newsType;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 评论内容
     */
    private String content;
    
    /**
     * 父评论ID，用于回复评论，如果是顶层评论则为null
     */
    private Long parentId;
    
    /**
     * 被回复的评论ID，如果是回复评论则存储被回复的评论ID，否则为null
     */
    private Long replyToId;
    
    /**
     * 点赞数量
     */
    private Integer likeCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 用户名，非数据库字段
     */
    @TableField(exist = false)
    private String username;
    
    /**
     * 用户头像，非数据库字段
     */
    @TableField(exist = false)
    private String userAvatar;
    
    /**
     * 被回复用户名，非数据库字段
     */
    @TableField(exist = false)
    private String replyToUsername;
    
    /**
     * 子评论列表，非数据库字段
     */
    @TableField(exist = false)
    private List<Comment> children;
} 