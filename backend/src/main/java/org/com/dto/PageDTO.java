package org.com.dto;

import lombok.Data;

@Data
public class PageDTO {
    /**
     * 当前页码，默认第1页
     */
    private Integer pageNum = 1;
    
    /**
     * 每页记录数，默认10条
     */
    private Integer pageSize = 10;
    
    /**
     * 验证并纠正分页参数
     */
    public void validate() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        // 限制每页最大记录数为100，避免请求过大数据
        if (pageSize > 100) {
            pageSize = 100;
        }
    }
} 