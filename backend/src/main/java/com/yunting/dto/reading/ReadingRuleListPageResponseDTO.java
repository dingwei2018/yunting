package com.yunting.dto.reading;

import java.util.List;

/**
 * 阅读规范分页列表响应DTO
 */
public class ReadingRuleListPageResponseDTO {
    /**
     * 阅读规范列表
     */
    private List<ReadingRuleListItemDTO> readingRuleList;
    /**
     * 总记录数
     */
    private Integer total;
    /**
     * 当前页码
     */
    private Integer page;
    /**
     * 每页大小
     */
    private Integer pageSize;

    public List<ReadingRuleListItemDTO> getReadingRuleList() {
        return readingRuleList;
    }

    public void setReadingRuleList(List<ReadingRuleListItemDTO> readingRuleList) {
        this.readingRuleList = readingRuleList;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
