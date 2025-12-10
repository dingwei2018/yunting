package com.yunting.dto.reading;

import java.util.List;

/**
 * 匹配字段列表响应DTO
 */
public class MatchingFieldListResponseDTO {
    private Integer total; // 注意：文档中是 "toatal"，但应使用 "total"
    private List<MatchingFieldDTO> fieldList;

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<MatchingFieldDTO> getFieldList() {
        return fieldList;
    }

    public void setFieldList(List<MatchingFieldDTO> fieldList) {
        this.fieldList = fieldList;
    }
}

