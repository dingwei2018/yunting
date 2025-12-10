package com.yunting.dto.original;

import java.util.List;

/**
 * 拆句列表响应DTO
 */
public class OriginalSentenceListResponseDTO {
    /**
     * 拆句列表
     */
    private List<OriginalSentenceListItemDTO> list;
    /**
     * 总记录数
     */
    private Long total;
    /**
     * 当前页码
     */
    private Integer page;
    /**
     * 每页大小
     */
    private Integer pageSize;

    public List<OriginalSentenceListItemDTO> getList() {
        return list;
    }

    public void setList(List<OriginalSentenceListItemDTO> list) {
        this.list = list;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
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

