package org.com.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.com.entity.Comment;
import org.com.mapper.CommentMapper;
import org.com.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评论服务实现类
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Resource
    private CommentMapper commentMapper;

    @Override
    @Transactional
    public boolean addComment(Comment comment) {
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        comment.setCreateTime(now);
        comment.setUpdateTime(now);
        
        // 点赞数初始化为0
        comment.setLikeCount(0);
        
        return this.save(comment);
    }

    @Override
    public Map<String, Object> getComments(Long newsId, String newsType, int page, int size) {
        Map<String, Object> result = new HashMap<>();
        
        // 计算分页参数
        int offset = (page - 1) * size;
        
        // 查询根评论列表
        List<Comment> comments = commentMapper.selectRootComments(newsId, newsType, offset, size);
        
        // 查询每个根评论的回复
        for (Comment comment : comments) {
            List<Comment> replies = commentMapper.selectReplies(comment.getId());
            comment.setChildren(replies);
        }
        
        // 统计总评论数
        Integer total = commentMapper.countRootComments(newsId, newsType);
        
        result.put("comments", comments);
        result.put("total", total);
        
        return result;
    }

    @Override
    public List<Comment> getReplies(Long parentId) {
        return commentMapper.selectReplies(parentId);
    }

    @Override
    @Transactional
    public boolean likeComment(Long commentId) {
        // 更新点赞数
        LambdaUpdateWrapper<Comment> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Comment::getId, commentId);
        wrapper.setSql("like_count = like_count + 1");
        
        return this.update(wrapper);
    }
} 