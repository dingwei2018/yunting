package com.yunting.constant;

/**
 * 阅读规范应用类型常量
 * 用于统一管理阅读规范应用的类型值
 */
public final class ReadingRuleApplicationType {

    private ReadingRuleApplicationType() {
        // 工具类，禁止实例化
    }

    /**
     * 阅读规范应用类型值（数字）
     */
    public static final class Type {
        private Type() {}

        /**
         * 任务
         */
        public static final int TASK = 1;

        /**
         * 断句
         */
        public static final int BREAKING_SENTENCE = 2;
    }
}

