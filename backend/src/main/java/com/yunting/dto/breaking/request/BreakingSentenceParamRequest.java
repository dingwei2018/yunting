package com.yunting.dto.breaking.request;

import java.util.List;

public class BreakingSentenceParamRequest {
    private Long breakingSentenceId;
    private String content;
    private Integer speechRate;
    private Integer volume;
    private Integer pitch;
    private String voiceId;
    private List<PauseSettingRequest> pauses;
    private List<PolyphonicSettingRequest> polyphonic;
    private List<SayAsSettingRequest> sayAs;  // say-as 标签设置
    private List<SubSettingRequest> sub;  // sub 标签设置
    private List<WordSettingRequest> word;  // word 标签设置
    private List<EmotionSettingRequest> emotion;  // emotion 标签设置
    private List<InsertActionSettingRequest> insertAction;  // insert-action 标签设置
    private List<Long> readingRuleIds;

    public Long getBreakingSentenceId() {
        return breakingSentenceId;
    }

    public void setBreakingSentenceId(Long breakingSentenceId) {
        this.breakingSentenceId = breakingSentenceId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSpeechRate() {
        return speechRate;
    }

    public void setSpeechRate(Integer speechRate) {
        this.speechRate = speechRate;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    public Integer getPitch() {
        return pitch;
    }

    public void setPitch(Integer pitch) {
        this.pitch = pitch;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(String voiceId) {
        this.voiceId = voiceId;
    }

    public List<PauseSettingRequest> getPauses() {
        return pauses;
    }

    public void setPauses(List<PauseSettingRequest> pauses) {
        this.pauses = pauses;
    }

    public List<PolyphonicSettingRequest> getPolyphonic() {
        return polyphonic;
    }

    public void setPolyphonic(List<PolyphonicSettingRequest> polyphonic) {
        this.polyphonic = polyphonic;
    }

    public List<SayAsSettingRequest> getSayAs() {
        return sayAs;
    }

    public void setSayAs(List<SayAsSettingRequest> sayAs) {
        this.sayAs = sayAs;
    }

    public List<SubSettingRequest> getSub() {
        return sub;
    }

    public void setSub(List<SubSettingRequest> sub) {
        this.sub = sub;
    }

    public List<WordSettingRequest> getWord() {
        return word;
    }

    public void setWord(List<WordSettingRequest> word) {
        this.word = word;
    }

    public List<EmotionSettingRequest> getEmotion() {
        return emotion;
    }

    public void setEmotion(List<EmotionSettingRequest> emotion) {
        this.emotion = emotion;
    }

    public List<InsertActionSettingRequest> getInsertAction() {
        return insertAction;
    }

    public void setInsertAction(List<InsertActionSettingRequest> insertAction) {
        this.insertAction = insertAction;
    }

    public List<Long> getReadingRuleIds() {
        return readingRuleIds;
    }

    public void setReadingRuleIds(List<Long> readingRuleIds) {
        this.readingRuleIds = readingRuleIds;
    }
}


