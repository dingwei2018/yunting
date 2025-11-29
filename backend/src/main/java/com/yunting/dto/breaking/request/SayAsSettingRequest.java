package com.yunting.dto.breaking.request;

/**
 * say-as 标签设置请求
 * 用于指定数字或英文的读法
 * 参考：https://support.huaweicloud.com/api-metastudio/metastudio_02_0038.html
 */
public class SayAsSettingRequest {
    private Long id;
    private Integer position;  // 位置（字符索引）
    private Integer length;   // 长度（字符数）
    private String interpretAs;  // 读法类型，如：number, date, time, currency, telephone, etc.
    private String format;    // 格式（可选）

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

    public String getInterpretAs() {
        return interpretAs;
    }

    public void setInterpretAs(String interpretAs) {
        this.interpretAs = interpretAs;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}

