package com.yunting.constant;

/**
 * 合成状态常量
 * 用于统一管理合成相关的状态值和消息
 */
public final class SynthesisStatus {

    private SynthesisStatus() {
        // 工具类，禁止实例化
    }

    /**
     * 合成状态值（数字）
     */
    public static final class Status {
        private Status() {}

        /**
         * 未合成
         */
        public static final int PENDING = 0;

        /**
         * 合成中
         */
        public static final int PROCESSING = 1;

        /**
         * 已合成
         */
        public static final int COMPLETED = 2;

        /**
         * 合成失败
         */
        public static final int FAILED = 3;
    }

    /**
     * 合成返回消息
     */
    public static final class Message {
        private Message() {}

        /**
         * 合成中
         */
        public static final String PROCESSING = "合成中";

        /**
         * 合成失败
         */
        public static final String FAILED = "合成失败";
    }

    /**
     * TTS回调状态
     */
    public static final class Callback {
        private Callback() {}

        /**
         * 任务完成
         */
        public static final String FINISHED = "FINISHED";

        /**
         * 任务失败
         */
        public static final String ERROR = "ERROR";

        /**
         * 任务等待中
         */
        public static final String WAITING = "WAITING";
    }
}

