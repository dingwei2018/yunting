package com.yunting.dto.task;

import java.util.List;

public class TaskListResponseDTO {
    private List<TaskListItemDTO> list;
    private Long total;
    private Integer page;
    private Integer pageSize;

    public List<TaskListItemDTO> getList() {
        return list;
    }

    public void setList(List<TaskListItemDTO> list) {
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

