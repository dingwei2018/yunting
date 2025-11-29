package com.yunting.dto.breaking.request;

/**
 * insert-action 标签设置请求
 * 用于在文本的指定位置插入动作
 * 参考：https://support.huaweicloud.com/api-metastudio/metastudio_02_0038.html
 */
public class InsertActionSettingRequest {
    private Long id;
    private Integer position;  // 位置（字符索引）
    private String name;       // 动作名称
    private String tag;        // 动作标识

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}

