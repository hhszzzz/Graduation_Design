package org.com.controller;

import org.com.common.Result;
import org.com.entity.Comment;
import org.com.service.CommentService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 评论控制器
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Resource
    private CommentService commentService;

    /**
     * 添加评论
     * @param comment 评论信息
     * @return 结果
     */
    @PostMapping("/add")
    public Result<?> addComment(@RequestBody Comment comment) {
        boolean success = commentService.addComment(comment);
        if (success) {
            return Result.success("评论成功");
        } else {
            return Result.failed("评论失败");
        }
    }

    /**
     * 获取评论列表
     * @param newsId 新闻ID
     * @param newsType 新闻类型
     * @param page 页码，默认1
     * @param size 每页大小，默认10
     * @return 评论列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getComments(
            @RequestParam Long newsId,
            @RequestParam String newsType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> data = commentService.getComments(newsId, newsType, page, size);
        return Result.success(data);
    }

    /**
     * 获取评论回复列表
     * @param parentId 父评论ID
     * @return 回复列表
     */
    @GetMapping("/replies/{parentId}")
    public Result<List<Comment>> getReplies(@PathVariable Long parentId) {
        List<Comment> replies = commentService.getReplies(parentId);
        return Result.success(replies);
    }

    /**
     * 点赞评论
     * @param commentId 评论ID
     * @return 结果
     */
    @PostMapping("/like/{commentId}")
    public Result<?> likeComment(@PathVariable Long commentId) {
        boolean success = commentService.likeComment(commentId);
        if (success) {
            return Result.success("点赞成功");
        } else {
            return Result.failed("点赞失败");
        }
    }
} 