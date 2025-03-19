package org.com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("news")
public class News {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String title;
    private String link;
}
