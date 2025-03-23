package org.com.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    /**
     * 数据列表
     */
    private List<T> records;
    
    /**
     * 总记录数
     */
    private long total;
    
    /**
     * 当前页码
     */
    private int pageNum;
    
    /**
     * 每页记录数
     */
    private int pageSize;
    
    /**
     * 总页数
     */
    private int pages;
    
    /**
     * 是否有下一页
     */
    private boolean hasNext;
    
    /**
     * 是否有上一页
     */
    private boolean hasPrevious;
    
    public PageResult(List<T> records, long total, int pageNum, int pageSize) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        
        // 计算总页数
        this.pages = (int) Math.ceil((double) total / pageSize);
        
        // 计算是否有下一页和上一页
        this.hasNext = pageNum < pages;
        this.hasPrevious = pageNum > 1;
    }
} 