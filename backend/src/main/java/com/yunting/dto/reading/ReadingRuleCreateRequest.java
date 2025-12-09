package com.yunting.dto.reading;

import jakarta.validation.constraints.NotBlank;

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
     * CHINESE_G2P：拼音
     * PHONETIC_SYMBOL：音标
     * CONTINUUM：连读
     * ALIAS：别名
     * SAY_AS：数字/英文的读法
     */
    @NotBlank(message = "ruleType不能为空")
    private String ruleType;

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

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getRuleValue() {
        return ruleValue;
    }

    public void setRuleValue(String ruleValue) {
        this.ruleValue = ruleValue;
    }
}


