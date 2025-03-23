package org.com.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.com.entity.Comment;

import java.util.List;

/**
 * 评论Mapper接口
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    
    /**
     * 查询评论列表（根评论），按照创建时间倒序排序
     * @param newsId 新闻ID
     * @param newsType 新闻类型
     * @return 评论列表
     */
    @Select({
        "SELECT c.*, u.username, u.avatar as user_avatar",
        "FROM comment c",
        "LEFT JOIN user u ON c.user_id = u.id",
        "WHERE c.news_id = #{newsId}",
        "AND c.news_type = #{newsType}",
        "AND c.parent_id IS NULL",
        "ORDER BY c.create_time DESC",
        "LIMIT #{offset}, #{limit}"
    })
    List<Comment> selectRootComments(@Param("newsId") Long newsId, 
                                    @Param("newsType") String newsType,
                                    @Param("offset") Integer offset, 
                                    @Param("limit") Integer limit);
    
    /**
     * 查询回复列表（子评论），按照创建时间升序排序
     * @param parentId 父评论ID
     * @return 回复列表
     */
    @Select({
        "SELECT c.*, u.username, u.avatar as user_avatar, u2.username as reply_to_username",
        "FROM comment c",
        "LEFT JOIN user u ON c.user_id = u.id",
        "LEFT JOIN comment c2 ON c.reply_to_id = c2.id",
        "LEFT JOIN user u2 ON c2.user_id = u2.id",
        "WHERE c.parent_id = #{parentId}",
        "ORDER BY c.create_time ASC"
    })
    List<Comment> selectReplies(@Param("parentId") Long parentId);
    
    /**
     * 统计根评论数量
     * @param newsId 新闻ID
     * @param newsType 新闻类型
     * @return 评论数量
     */
    @Select({
        "SELECT COUNT(*) FROM comment",
        "WHERE news_id = #{newsId}",
        "AND news_type = #{newsType}",
        "AND parent_id IS NULL"
    })
    Integer countRootComments(@Param("newsId") Long newsId, @Param("newsType") String newsType);
} 