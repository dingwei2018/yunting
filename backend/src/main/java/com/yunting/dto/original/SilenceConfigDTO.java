package com.yunting.dto.original;

/**
 * 静音配置DTO
 */
public class SilenceConfigDTO {
    /**
     * 静音位置
     */
    private Integer location;
    /**
     * 静音时长（毫秒）
     */
    private Integer duration;

    public Integer getLocation() {
        return location;
    }

    public void setLocation(Integer location) {
        this.location = location;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }
}

