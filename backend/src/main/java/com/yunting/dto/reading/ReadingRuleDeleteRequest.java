package com.yunting.dto.reading;

/**
 * 删除阅读规则请求DTO
 */
public class ReadingRuleDeleteRequest {
    /**
     * 规则ID（必填）
     */
    private Long ruleId;

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }
}

