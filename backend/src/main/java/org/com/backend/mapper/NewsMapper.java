package org.com.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.com.backend.entity.News;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NewsMapper extends BaseMapper<News> {

    @Insert("<script>" +
            "INSERT INTO ${tableName} (title, link, publish_time) VALUES (#{title}, #{link}, #{publishTime})" +
            "</script>")
    void insertNewsDynamicTable(@Param("tableName") String tableName,
                                @Param("title") String title,
                                @Param("link") String link,
                                @Param("publishTime") LocalDateTime publishTime);

    @Select("<script>" +
            "SELECT * FROM ${tableName}" +
            "</script>")
    List<News> selectListByType(@Param("tableName") String tableName);
}
