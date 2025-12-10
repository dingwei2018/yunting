package com.yunting.dto.reading;

/**
 * 匹配字段DTO
 */
public class MatchingFieldDTO {
    /**
     * 规则ID
     */
    private Long ruleId;
    /**
     * 匹配字段第一个字符的位置
     */
    private Integer location;
    /**
     * 字段
     */
    private String pattern;

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
}

