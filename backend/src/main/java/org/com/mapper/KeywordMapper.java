package org.com.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.ResultType;
import org.com.dto.KeywordCountDTO;
import org.com.entity.Keyword;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 关键词Mapper接口
 */
@Mapper
public interface KeywordMapper extends BaseMapper<Keyword> {
    
    /**
     * 统计指定时间范围内每天的关键词数量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param newsType 新闻类型，如果为all则不限制类型
     * @return 每天的统计结果
     */
    @Select({"<script>",
            "SELECT DATE_FORMAT(record_time, '%Y-%m-%d') as date, COUNT(*) as count",
            "FROM keyword",
            "WHERE record_time BETWEEN #{startTime} AND #{endTime}",
            "<if test=\"newsType != null and newsType != 'all'\">",
            "AND news_type = #{newsType}",
            "</if>",
            "GROUP BY DATE_FORMAT(record_time, '%Y-%m-%d')",
            "ORDER BY date",
            "</script>"})
    @ResultType(Map.class)
    List<Map<String, Object>> countKeywordsByDay(@Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime,
                                      @Param("newsType") String newsType);
    
    /**
     * 获取指定时间范围内的热门关键词
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param newsType 新闻类型，如果为all则不限制类型
     * @param limit 限制返回数量
     * @return 关键词及出现次数
     */
    @Select({"<script>",
            "SELECT keyword, COUNT(*) as count",
            "FROM keyword",
            "WHERE record_time BETWEEN #{startTime} AND #{endTime}",
            "<if test=\"newsType != null and newsType != 'all'\">",
            "AND news_type = #{newsType}",
            "</if>",
            "GROUP BY keyword",
            "ORDER BY count DESC",
            "LIMIT #{limit}",
            "</script>"})
    List<KeywordCountDTO> getTopKeywords(@Param("startTime") LocalDateTime startTime, 
                                        @Param("endTime") LocalDateTime endTime,
                                        @Param("newsType") String newsType,
                                        @Param("limit") int limit);
} 