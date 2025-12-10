package com.yunting.constant;

/**
 * 阅读规范类型枚举
 * 用于统一管理阅读规范的类型值
 */
public enum ReadingRuleType {
    /**
     * 数字英文
     */
    NUMBER_ENGLISH(1, "数字英文"),
    
    /**
     * 音标调整
     */
    PHONETIC_ADJUSTMENT(2, "音标调整"),
    
    /**
     * 专有词汇
     */
    PROPER_NOUN(3, "专有词汇");

    private final Integer code;
    private final String description;

    ReadingRuleType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据整数值获取枚举
     *
     * @param code 整数值
     * @return 对应的枚举，如果不存在则返回 null
     */
    public static ReadingRuleType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReadingRuleType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 检查整数值是否有效
     *
     * @param code 整数值
     * @return 如果值有效返回 true，否则返回 false
     */
    public static boolean isValid(Integer code) {
        return fromCode(code) != null;
    }
}
