package com.yunting.dto.original;

import java.util.List;

public class BreakingSentenceSettingDTO {
    private String content;
    private Integer volume;
    private String voiceId;
    private List<BreakConfigDTO> breakList;
    private List<PhonemeConfigDTO> phonemeList;
    private List<ProsodyConfigDTO> prosodyList;
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

