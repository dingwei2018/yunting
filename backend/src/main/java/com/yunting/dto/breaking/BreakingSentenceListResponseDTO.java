package com.yunting.dto.breaking;

import java.util.List;

public class BreakingSentenceListResponseDTO {
    private List<BreakingSentenceListItemDTO> list;
    private long total;
    private int page;
    private int pageSize;

    public List<BreakingSentenceListItemDTO> getList() {
        return list;
    }

    public void setList(List<BreakingSentenceListItemDTO> list) {
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}


