package org.com.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long id;
    private Long newsId;
    private String newsType;
    private Long userId;
    private String content;
    private Long parentId;
    private Long replyToId;
    private Integer likeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 关联信息
    private String username;
    private String userAvatar;
    private String replyToUsername;
    
    // 子评论列表
    private List<CommentDTO> children;
} 