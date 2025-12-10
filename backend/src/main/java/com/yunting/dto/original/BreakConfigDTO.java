package com.yunting.dto.original;

/**
 * 停顿配置DTO
 */
public class BreakConfigDTO {
    /**
     * 停顿位置
     */
    private String location;
    /**
     * 停顿时长（毫秒）
     */
    private String duration;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}

