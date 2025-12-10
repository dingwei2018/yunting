package com.yunting.dto.reading;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建阅读规范请求DTO
 */
public class ReadingRuleCreateRequest {
    /**
     * 原始词
     */
    @NotBlank(message = "pattern不能为空")
    private String pattern;

    /**
     * 规范类型：
     * 1：数字英文
     * 2：音标调整
     * 3：专有词汇
     */
    @NotNull(message = "ruleType不能为空")
    @Min(value = 1, message = "ruleType必须为1、2或3")
    @Max(value = 3, message = "ruleType必须为1、2或3")
    private Integer ruleType;

    /**
     * 自定义读法，类型为SAY_AS时，只允许传下面的值。
     * number：数字
     * date：日期
     * figure：数值
     * phone：电话号码
     * english：英文单词
     * spell：逐个字母读英文
     */
    @NotBlank(message = "ruleValue不能为空")
    private String ruleValue;

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
}


