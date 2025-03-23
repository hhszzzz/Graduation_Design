package org.com.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.com.entity.UserCollection;
import java.util.List;

@Mapper
public interface UserCollectionMapper extends BaseMapper<UserCollection> {
    /**
     * 查找用户的所有收藏，按收藏时间降序排列
     */
    @Select("SELECT * FROM user_collections WHERE user_id = #{userId} ORDER BY collect_time DESC")
    List<UserCollection> findByUserIdOrderByCollectTimeDesc(@Param("userId") Long userId);
    
    /**
     * 查找用户对特定新闻和类型的收藏
     */
    @Select("SELECT * FROM user_collections WHERE user_id = #{userId} AND news_id = #{newsId} AND news_type = #{newsType} LIMIT 1")
    UserCollection findByUserIdAndNewsIdAndNewsType(
            @Param("userId") Long userId, 
            @Param("newsId") Long newsId, 
            @Param("newsType") String newsType);
    
    /**
     * 检查用户是否已收藏某条特定类型的新闻
     */
    @Select("SELECT COUNT(*) FROM user_collections WHERE user_id = #{userId} AND news_id = #{newsId} AND news_type = #{newsType}")
    int existsByUserIdAndNewsIdAndNewsType(
            @Param("userId") Long userId, 
            @Param("newsId") Long newsId, 
            @Param("newsType") String newsType);
    
    /**
     * 根据用户和新闻ID及类型删除收藏
     */
    @Delete("DELETE FROM user_collections WHERE user_id = #{userId} AND news_id = #{newsId} AND news_type = #{newsType}")
    int deleteByUserIdAndNewsIdAndNewsType(
            @Param("userId") Long userId, 
            @Param("newsId") Long newsId, 
            @Param("newsType") String newsType);
    
    /**
     * 查找用户对特定新闻的收藏（不考虑类型）
     */
    @Select("SELECT * FROM user_collections WHERE user_id = #{userId} AND news_id = #{newsId} LIMIT 1")
    UserCollection findByUserIdAndNewsId(
            @Param("userId") Long userId, 
            @Param("newsId") Long newsId);
    
    /**
     * 检查用户是否已收藏某条新闻（不考虑类型）
     */
    @Select("SELECT COUNT(*) FROM user_collections WHERE user_id = #{userId} AND news_id = #{newsId}")
    int existsByUserIdAndNewsId(
            @Param("userId") Long userId, 
            @Param("newsId") Long newsId);
    
    /**
     * 根据用户和新闻ID删除收藏（不考虑类型）
     */
    @Delete("DELETE FROM user_collections WHERE user_id = #{userId} AND news_id = #{newsId}")
    int deleteByUserIdAndNewsId(
            @Param("userId") Long userId, 
            @Param("newsId") Long newsId);
} 