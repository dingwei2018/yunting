package com.yunting.constant;

/**
 * 合成默认参数常量
 * 用于统一管理合成参数的默认值
 */
public final class SynthesisDefault {

    private SynthesisDefault() {
        // 工具类，禁止实例化
    }

    /**
     * 默认音色ID
     */
    public static final String VOICE_ID = "c41f12c125f24c834ed3ae7c1fdae456";

    /**
     * 默认音量
     * 范围：0-200，140为默认值
     */
    public static final int VOLUME = 140;

    /**
     * 默认语速
     * 范围：0-200，100为正常语速（默认值）
     */
    public static final int SPEECH_RATE = 100;

    /**
     * 默认音调
     * 范围：-100到100，100为默认值
     */
    public static final int PITCH = 100;
}

