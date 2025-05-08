package org.com.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.com.entity.News;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface NewsMapper extends BaseMapper<News> {

    @Select("<script>" +
            "SELECT COUNT(*) FROM ${tableName} where title = #{title}" +
            "</script>")
    long selectCountTable(@Param("tableName") String tableName,
                          @Param("title") String title);

    @Insert("<script>" +
            "INSERT INTO ${tableName} (title, link, publish_time) VALUES (#{title}, #{link}, #{publishTime})" +
            "</script>")
    long insertNewsDynamicTable(@Param("tableName") String tableName,
                                @Param("title") String title,
                                @Param("link") String link,
                                @Param("publishTime") LocalDateTime publishTime);

    @Select("<script>" +
            "SELECT * FROM ${tableName} order by publish_time DESC " +
            "</script>")
    List<News> selectListByType(@Param("tableName") String tableName);
    
    /**
     * 分页查询新闻列表
     * @param tableName 表名
     * @param offset 偏移量
     * @param limit 每页记录数
     * @return 新闻列表
     */
    @Select("<script>" +
            "SELECT * FROM ${tableName} ORDER BY publish_time DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<News> selectPageByType(@Param("tableName") String tableName, 
                              @Param("offset") int offset, 
                              @Param("limit") int limit);
    
    /**
     * 统计指定表中的记录总数
     * @param tableName 表名
     * @return 记录总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM ${tableName}" +
            "</script>")
    long countByType(@Param("tableName") String tableName);
    
    /**
     * 根据类型查询对应表的ID和标题，专门用于CSV导出
     * @param tableName 表名
     * @return 新闻ID和标题列表
     */
    @Select("<script>" +
            "SELECT id, title FROM ${tableName}" +
            "</script>")
    List<Map<String, Object>> selectNewsByTypeForCSV(@Param("tableName") String tableName);
    
    /**
     * 根据ID和类型查询单条新闻
     * @param tableName 表名
     * @param id 新闻ID
     * @return 新闻详情
     */
    @Select("<script>" +
            "SELECT * FROM ${tableName} WHERE id = #{id}" +
            "</script>")
    News selectOneByType(@Param("tableName") String tableName, @Param("id") Long id);
    
    /**
     * 在指定表中搜索标题包含指定关键词的新闻
     * @param tableName 表名
     * @param query 搜索关键词
     * @return 匹配的新闻列表
     */
    @Select("<script>" +
            "SELECT * FROM ${tableName} WHERE title LIKE CONCAT('%', #{query}, '%') ORDER BY publish_time DESC" +
            "</script>")
    List<News> searchNewsByTitle(@Param("tableName") String tableName, @Param("query") String query);
    
    /**
     * 分页搜索新闻
     * @param tableName 表名
     * @param query 搜索关键词
     * @param offset 偏移量
     * @param limit 每页记录数
     * @return 匹配的新闻列表
     */
    @Select("<script>" +
            "SELECT * FROM ${tableName} WHERE title LIKE CONCAT('%', #{query}, '%') " +
            "ORDER BY publish_time DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<News> searchNewsPageByTitle(@Param("tableName") String tableName, 
                                  @Param("query") String query, 
                                  @Param("offset") int offset, 
                                  @Param("limit") int limit);
    
    /**
     * 统计搜索结果总数
     * @param tableName 表名
     * @param query 搜索关键词
     * @return 记录总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM ${tableName} WHERE title LIKE CONCAT('%', #{query}, '%')" +
            "</script>")
    long countSearchResults(@Param("tableName") String tableName, @Param("query") String query);
    
    /**
     * 获取最后插入的新闻ID
     * @param tableName 表名
     * @return 最后插入的ID
     */
    @Select("<script>" +
            "SELECT LAST_INSERT_ID() FROM ${tableName} LIMIT 1" +
            "</script>")
    Long selectLastInsertIdByType(@Param("tableName") String tableName);
    
    /**
     * 获取还未提取关键词的新闻列表
     * @param tableName 新闻表名
     * @param limit 限制条数
     * @return 未处理的新闻列表
     */
    @Select("<script>" +
            "SELECT n.* FROM ${tableName} n " +
            "LEFT JOIN (" +
            "  SELECT DISTINCT news_id, news_type FROM keyword " +
            "  WHERE news_type = #{tableName}" +
            ") k ON n.id = k.news_id " +
            "WHERE k.news_id IS NULL " +
            "LIMIT #{limit}" +
            "</script>")
    List<News> selectNewsWithoutKeywords(@Param("tableName") String tableName, @Param("limit") int limit);

    /**
     * 获取浏览次数最多的新闻
     * @param tableName 表名
     * @param limit 数量限制
     * @return 热门新闻列表
     */
    @Select("<script>" +
            "SELECT n.* FROM ${tableName} n " +
            "LEFT JOIN (SELECT news_id, COUNT(*) as view_count FROM user_history GROUP BY news_id) uh " +
            "ON n.id = uh.news_id " +
            "ORDER BY IFNULL(uh.view_count, 0) DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<News> findMostViewedNews(@Param("tableName") String tableName, @Param("limit") int limit);

    /**
     * 根据ID列表从多个新闻表中查询新闻
     * @param newsIds 新闻ID列表
     * @return 新闻列表
     */
    @Select("<script>" +
            "SELECT id, title, link, publish_time, source_type FROM (" +
            "  SELECT id, title, link, publish_time, 'daily' as source_type FROM daily_news WHERE id IN " +
            "  <foreach collection='newsIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "  UNION ALL " +
            "  SELECT id, title, link, publish_time, 'comprehensive' as source_type FROM comprehensive_news WHERE id IN " +
            "  <foreach collection='newsIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "  UNION ALL " +
            "  SELECT id, title, link, publish_time, 'fashion' as source_type FROM fashion_news WHERE id IN " +
            "  <foreach collection='newsIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "  UNION ALL " +
            "  SELECT id, title, link, publish_time, 'material' as source_type FROM material_news WHERE id IN " +
            "  <foreach collection='newsIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "  UNION ALL " +
            "  SELECT id, title, link, publish_time, 'exhibition' as source_type FROM exhibition_news WHERE id IN " +
            "  <foreach collection='newsIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "  UNION ALL " +
            "  SELECT id, title, link, publish_time, 'product' as source_type FROM product_news WHERE id IN " +
            "  <foreach collection='newsIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            ") AS combined_news" +
            "</script>")
    List<News> selectNewsByIds(@Param("newsIds") List<Long> newsIds);
}
