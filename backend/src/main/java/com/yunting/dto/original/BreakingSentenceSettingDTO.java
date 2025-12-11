package com.yunting.dto.original;

import java.util.List;

/**
 * 断句配置DTO
 */
public class BreakingSentenceSettingDTO {
    /**
     * 断句内容
     */
    private String content;
    /**
     * 音量（0-100）
     */
    private Integer volume;
    /**
     * 语速，50-200，100为正常
     */
    private Integer speed;
    /**
     * 语音ID
     */
    private String voiceId;
    /**
     * 停顿配置列表
     */
    private List<BreakConfigDTO> breakList;
    /**
     * 多音字配置列表
     */
    private List<PhonemeConfigDTO> phonemeList;
    /**
     * 局部语速配置列表
     */
    private List<ProsodyConfigDTO> prosodyList;
    /**
     * 静音配置列表
     */
    private List<SilenceConfigDTO> silentList;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    public Integer getSpeed() {
        return speed;
    }

    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }

    public List<BreakConfigDTO> getBreakList() {
        return breakList;
    }

    public void setBreakList(List<BreakConfigDTO> breakList) {
        this.breakList = breakList;
    }

    public List<PhonemeConfigDTO> getPhonemeList() {
        return phonemeList;
    }

    public void setPhonemeList(List<PhonemeConfigDTO> phonemeList) {
        this.phonemeList = phonemeList;
    }

    public List<ProsodyConfigDTO> getProsodyList() {
        return prosodyList;
    }

    public void setProsodyList(List<ProsodyConfigDTO> prosodyList) {
        this.prosodyList = prosodyList;
    }

    public List<SilenceConfigDTO> getSilentList() {
        return silentList;
    }

    public void setSilentList(List<SilenceConfigDTO> silentList) {
        this.silentList = silentList;
    }
}

