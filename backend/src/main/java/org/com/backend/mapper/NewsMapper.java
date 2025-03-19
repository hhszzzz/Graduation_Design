package org.com.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.com.backend.entity.News;

@Mapper
public interface NewsMapper extends BaseMapper<News> {
}
