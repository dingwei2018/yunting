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
     * 位置
     */
    private Integer location;

    public String getPh() {
        return ph;
    }

    public void setPh(String ph) {
        this.ph = ph;
    }

    public Integer getLocation() {
        return location;
    }

    public void setLocation(Integer location) {
        this.location = location;
    }
}

