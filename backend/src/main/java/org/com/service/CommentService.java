package org.com.service;

import org.com.dto.CommentDTO;
import org.com.entity.Comment;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 评论服务接口
 */
public interface CommentService extends IService<Comment> {
    
    /**
     * 添加评论
     * @param comment 评论信息
     * @return 是否成功
     */
    boolean addComment(Comment comment);
    
    /**
     * 获取评论列表
     * @param newsId 新闻ID
     * @param newsType 新闻类型
     * @param page 页码
     * @param size 每页大小
     * @return 评论列表及相关信息
     */
    Map<String, Object> getComments(Long newsId, String newsType, int page, int size);
    
    /**
     * 获取回复列表
     * @param parentId 父评论ID
     * @return 回复列表
     */
    List<Comment> getReplies(Long parentId);
    
    /**
     * 点赞评论
     * @param commentId 评论ID
     * @return 是否成功
     */
    boolean likeComment(Long commentId);
} 