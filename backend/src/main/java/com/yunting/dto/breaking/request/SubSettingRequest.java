package com.yunting.dto.breaking.request;

/**
 * sub 标签设置请求
 * 用于设置当前标记文字的别名，即替代读法
 * 参考：https://support.huaweicloud.com/api-metastudio/metastudio_02_0038.html
 */
public class SubSettingRequest {
    private Long id;
    private Integer position;  // 位置（字符索引）
    private Integer length;   // 长度（字符数）
    private String alias;     // 替代读法

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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}

