package org.com.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 趋势数据DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendDataDTO {
    /**
     * 日期列表
     */
    private List<String> dates;
    
    /**
     * 每日统计数量
     */
    private List<Integer> counts;
} 