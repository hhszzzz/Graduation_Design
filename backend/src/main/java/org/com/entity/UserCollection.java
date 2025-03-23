package org.com.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_collections")
public class UserCollection {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;
    
    @JsonIgnore
    @TableField(exist = false)
    private User user;

    @TableField("news_id")
    private Long newsId;
    
    @TableField(exist = false)
    private News news;
    
    @TableField("news_type")
    private String newsType;

    @TableField("collect_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime = LocalDateTime.now();
} 