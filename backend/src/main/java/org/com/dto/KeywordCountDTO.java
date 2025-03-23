package org.com.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关键词统计DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeywordCountDTO {
    /**
     * 关键词
     */
    private String keyword;
    
    /**
     * 出现次数
     */
    private Integer count;
} 