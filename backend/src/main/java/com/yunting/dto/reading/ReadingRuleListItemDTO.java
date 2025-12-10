package com.yunting.dto.reading;

/**
 * 阅读规范列表项DTO
 */
public class ReadingRuleListItemDTO {
    /**
     * 规则ID
     */
    private Long ruleId;
    /**
     * 原始词（匹配模式）
     */
    private String pattern;
    /**
     * 规范类型（1-数字英文，2-音标调整，3-专有词汇）
     */
    private Integer ruleType;
    /**
     * 自定义读法
     */
    private String ruleValue;
    /**
     * 是否开启
     */
    private Boolean isOpen;

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Integer getRuleType() {
        return ruleType;
    }

    public void setRuleType(Integer ruleType) {
        this.ruleType = ruleType;
    }

    public String getRuleValue() {
        return ruleValue;
    }

    public void setRuleValue(String ruleValue) {
        this.ruleValue = ruleValue;
    }

    public Boolean getIsOpen() {
        return isOpen;
    }

    public void setIsOpen(Boolean isOpen) {
        this.isOpen = isOpen;
    }
}
