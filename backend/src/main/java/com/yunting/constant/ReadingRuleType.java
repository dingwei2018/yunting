package com.yunting.constant;

/**
 * 阅读规范类型枚举
 * 用于统一管理阅读规范的类型值
 */
public enum ReadingRuleType {
    /**
     * 数字英文
     */
    NUMBER_ENGLISH(1, "数字英文", "SAY_AS"),
    
    /**
     * 音标调整
     */
    PHONETIC_ADJUSTMENT(2, "音标调整", "PHONETIC_SYMBOL"),
    
    /**
     * 专有词汇
     */
    PROPER_NOUN(3, "专有词汇", "ALIAS");

    private final Integer code;
    private final String description;
    private final String huaweiCloudType;

    ReadingRuleType(Integer code, String description, String huaweiCloudType) {
        this.code = code;
        this.description = description;
        this.huaweiCloudType = huaweiCloudType;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 获取对应的华为云类型值
     * @return 华为云类型字符串
     */
    public String getHuaweiCloudType() {
        return huaweiCloudType;
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
     * 根据整数值获取对应的华为云类型
     *
     * @param code 规则类型代码
     * @return 华为云类型字符串，如果不存在则返回 null
     */
    public static String getHuaweiCloudType(Integer code) {
        ReadingRuleType type = fromCode(code);
        return type != null ? type.getHuaweiCloudType() : null;
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

    /**
     * 根据华为云类型字符串获取对应的本地类型代码
     *
     * @param huaweiCloudType 华为云类型字符串（如 "SAY_AS", "PHONETIC_SYMBOL", "ALIAS"）
     * @return 对应的本地类型代码，如果不存在则返回 null
     */
    public static Integer getCodeFromHuaweiCloudType(String huaweiCloudType) {
        if (huaweiCloudType == null) {
            return null;
        }
        for (ReadingRuleType type : values()) {
            if (type.huaweiCloudType.equals(huaweiCloudType)) {
                return type.code;
            }
        }
        return null;
    }
}
