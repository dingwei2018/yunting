package com.yunting.dto.breaking.request;

/**
 * emotion 标签设置请求
 * 用于设置选中文字使用的音色情感/风格
 * 参考：https://support.huaweicloud.com/api-metastudio/metastudio_02_0038.html
 * 支持的类型：DEFAULT, HAPPY, SAD, ANGRY, FEAR, AMAZED, COMFORT, NEWS, MARKETING, LIVE, EDUCATION, CUSTOMER, STORYTELLING
 */
public class EmotionSettingRequest {
    private Long id;
    private Integer position;  // 位置（字符索引）
    private Integer length;   // 长度（字符数）
    private String type;       // 情感/风格类型

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

