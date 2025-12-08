package com.yunting.constant;

/**
 * 音频合并状态常量
 * 用于统一管理音频合并相关的状态值
 */
public final class AudioMergeStatus {

    private AudioMergeStatus() {
        // 工具类，禁止实例化
    }

    /**
     * 音频合并状态值（数字）
     */
    public static final class Status {
        private Status() {}

        /**
         * 合并中
         */
        public static final int PROCESSING = 1;

        /**
         * 合并完成
         */
        public static final int COMPLETED = 2;

        /**
         * 合并失败
         */
        public static final int FAILED = 3;
    }
}

