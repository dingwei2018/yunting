package com.yunting.dto.reading;

/**
 * 匹配字段DTO
 */
public class MatchingFieldDTO {
    private Long ruleId;
    private Integer location; // 匹配字段第一个字符的位置
    private String pattern; // 字段
    private Boolean isOpen;

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Integer getLocation() {
        return location;
    }

    public void setLocation(Integer location) {
        this.location = location;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Boolean getIsOpen() {
        return isOpen;
    }

    public void setIsOpen(Boolean isOpen) {
        this.isOpen = isOpen;
    }
}

