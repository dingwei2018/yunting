package com.yunting.dto.original;

/**
 * 局部语速配置DTO
 */
public class ProsodyConfigDTO {
    /**
     * 语速（slow, medium, fast）
     */
    private String rate;
    /**
     * 开始位置
     */
    private Integer begin;
    /**
     * 结束位置
     */
    private Integer end;

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    public Integer getBegin() {
        return begin;
    }

    public void setBegin(Integer begin) {
        this.begin = begin;
    }

    public Integer getEnd() {
        return end;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }
}

