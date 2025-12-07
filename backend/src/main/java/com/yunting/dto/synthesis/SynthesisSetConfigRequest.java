package com.yunting.dto.synthesis;

import java.util.List;

/**
 * 设置拆句合成参数请求DTO
 */
public class SynthesisSetConfigRequest {
    private Long taskId;
    private Long originalSentenceId;
    private List<BreakingSentenceConfig> breakingSentenceList;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getOriginalSentenceId() {
        return originalSentenceId;
    }

    public void setOriginalSentenceId(Long originalSentenceId) {
        this.originalSentenceId = originalSentenceId;
    }

    public List<BreakingSentenceConfig> getBreakingSentenceList() {
        return breakingSentenceList;
    }

    public void setBreakingSentenceList(List<BreakingSentenceConfig> breakingSentenceList) {
        this.breakingSentenceList = breakingSentenceList;
    }

    /**
     * 断句配置
     */
    public static class BreakingSentenceConfig {
        private Long breakingSentenceId;
        private String content;
        private Integer volume;
        private String voiceId;
        private Integer speed;
        private List<BreakConfig> breakList;
        private List<PhonemeConfig> phonemeList;
        private List<ProsodyConfig> prosodyList;
        private List<SilenceConfig> silenceList;

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

        public Integer getSpeed() {
            return speed;
        }

        public void setSpeed(Integer speed) {
            this.speed = speed;
        }

        public List<BreakConfig> getBreakList() {
            return breakList;
        }

        public void setBreakList(List<BreakConfig> breakList) {
            this.breakList = breakList;
        }

        public List<PhonemeConfig> getPhonemeList() {
            return phonemeList;
        }

        public void setPhonemeList(List<PhonemeConfig> phonemeList) {
            this.phonemeList = phonemeList;
        }

        public List<ProsodyConfig> getProsodyList() {
            return prosodyList;
        }

        public void setProsodyList(List<ProsodyConfig> prosodyList) {
            this.prosodyList = prosodyList;
        }

        public List<SilenceConfig> getSilenceList() {
            return silenceList;
        }

        public void setSilenceList(List<SilenceConfig> silenceList) {
            this.silenceList = silenceList;
        }
    }

    /**
     * 停顿配置
     */
    public static class BreakConfig {
        private Integer location;
        private Integer duration;

        public Integer getLocation() {
            return location;
        }

        public void setLocation(Integer location) {
            this.location = location;
        }

        public Integer getDuration() {
            return duration;
        }

        public void setDuration(Integer duration) {
            this.duration = duration;
        }
    }

    /**
     * 多音字配置
     */
    public static class PhonemeConfig {
        private String ph;
        private Integer location;

        public String getPh() {
            return ph;
        }

        public void setPh(String ph) {
            this.ph = ph;
        }

        public Integer getLocation() {
            return location;
        }

        public void setLocation(Integer location) {
            this.location = location;
        }
    }

    /**
     * 局部语速配置
     */
    public static class ProsodyConfig {
        private Integer rate;
        private Integer begin;
        private Integer end;

        public Integer getRate() {
            return rate;
        }

        public void setRate(Integer rate) {
            this.rate = rate;
        }

        public Integer getBegin() {
            return begin;
        }

        public void setBegin(Integer begin) {
            this.begin = begin;
        }

        public Integer getEnd() {
            return end;
        }

        public void setEnd(Integer end) {
            this.end = end;
        }
    }

    /**
     * 静音配置
     */
    public static class SilenceConfig {
        private Integer location;
        private Integer duration;

        public Integer getLocation() {
            return location;
        }

        public void setLocation(Integer location) {
            this.location = location;
        }

        public Integer getDuration() {
            return duration;
        }

        public void setDuration(Integer duration) {
            this.duration = duration;
        }
    }
}

