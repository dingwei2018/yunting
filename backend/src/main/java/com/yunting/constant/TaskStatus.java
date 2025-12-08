package com.yunting.constant;

/**
 * 任务状态常量
 * 用于统一管理任务相关的状态值
 */
public final class TaskStatus {

    private TaskStatus() {
        // 工具类，禁止实例化
    }

    /**
     * 任务状态值（数字）
     */
    public static final class Status {
        private Status() {}

        /**
         * 拆句完成
         */
        public static final int BREAKING_COMPLETED = 0;

        /**
         * 语音合成中
         */
        public static final int SYNTHESIS_PROCESSING = 1;

        /**
         * 语音合成成功
         */
        public static final int SYNTHESIS_SUCCESS = 2;

        /**
         * 语音合成失败
         */
        public static final int SYNTHESIS_FAILED = 3;

        /**
         * 语音合并中
         */
        public static final int MERGE_PROCESSING = 4;

        /**
         * 语音合并成功
         */
        public static final int MERGE_SUCCESS = 5;

        /**
         * 语音合并失败
         */
        public static final int MERGE_FAILED = 6;
    }
}

