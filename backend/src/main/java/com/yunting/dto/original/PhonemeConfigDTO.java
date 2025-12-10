package com.yunting.dto.original;

/**
 * 多音字配置DTO
 */
public class PhonemeConfigDTO {
    /**
     * 音标
     */
    private String ph;
    /**
     * 字母表
     */
    private String alphabet;
    /**
     * 开始位置
     */
    private Integer begin;
    /**
     * 结束位置
     */
    private Integer end;

    public String getPh() {
        return ph;
    }

    public void setPh(String ph) {
        this.ph = ph;
    }

    public String getAlphabet() {
        return alphabet;
    }

    public void setAlphabet(String alphabet) {
        this.alphabet = alphabet;
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

