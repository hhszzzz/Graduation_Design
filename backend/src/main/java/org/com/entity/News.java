package org.com.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class News {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String title;
    private String link;
    private String publishTime;
}
