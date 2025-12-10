package com.yunting.dto.reading;

import java.util.List;

/**
 * 匹配字段列表响应DTO
 */
public class MatchingFieldListResponseDTO {
    /**
     * 匹配字段总数
     */
    private Integer total;
    /**
     * 匹配字段列表
     */
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

