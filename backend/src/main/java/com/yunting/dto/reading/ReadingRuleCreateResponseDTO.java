package com.yunting.dto.reading;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 创建阅读规范响应DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReadingRuleCreateResponseDTO {
    /**
     * 规则ID
     */
    private Long ruleId;
    /**
     * 任务ID
     */
    private Long taskId;
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
     * 作用域
     */
    private Integer scope;
    /**
     * 创建时间
     */
    private String createdAt;

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
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

    public Integer getScope() {
        return scope;
    }

    public void setScope(Integer scope) {
        this.scope = scope;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
