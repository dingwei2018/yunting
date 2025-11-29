package com.yunting.dto.breaking.request;

/**
 * word 标签设置请求
 * 用于设置选中文字为连读模式
 * 参考：https://support.huaweicloud.com/api-metastudio/metastudio_02_0038.html
 */
public class WordSettingRequest {
    private Long id;
    private Integer position;  // 位置（字符索引）
    private Integer length;   // 长度（字符数）

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
}

