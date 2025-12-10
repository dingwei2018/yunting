package com.yunting.dto.synthesis;

import java.util.List;

/**
 * 设置拆句合成参数请求DTO
 */
public class SynthesisSetConfigRequest {
    /**
     * 任务ID
     */
    private Long taskId;
    /**
     * 拆句ID
     */
    private Long originalSentenceId;
    /**
     * 断句配置列表
     */
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
        /**
         * 断句ID
         */
        private Long breakingSentenceId;
        /**
         * 序号
         */
        private Integer sequence;
        /**
         * 断句内容
         */
        private String content;
        /**
         * 音量（0-100）
         */
        private Integer volume;
        /**
         * 语音ID
         */
        private String voiceId;
        /**
         * 语速
         */
        private Integer speed;
        /**
         * 停顿配置列表
         */
        private List<BreakConfig> breakList;
        /**
         * 多音字配置列表
         */
        private List<PhonemeConfig> phonemeList;
        /**
         * 局部语速配置列表
         */
        private List<ProsodyConfig> prosodyList;
        /**
         * 静音配置列表
         */
        private List<SilenceConfig> silenceList;
        /**
         * 阅读规范配置列表
         */
        private List<ReadRuleConfig> readRule;

        public Long getBreakingSentenceId() {
            return breakingSentenceId;
        }

        public void setBreakingSentenceId(Long breakingSentenceId) {
            this.breakingSentenceId = breakingSentenceId;
        }

        public Integer getSequence() {
            return sequence;
        }

        public void setSequence(Integer sequence) {
            this.sequence = sequence;
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

        public List<ReadRuleConfig> getReadRule() {
            return readRule;
        }

        public void setReadRule(List<ReadRuleConfig> readRule) {
            this.readRule = readRule;
        }
    }

    /**
     * 停顿配置
     */
    public static class BreakConfig {
        /**
         * 停顿位置
         */
        private Integer location;
        /**
         * 停顿时长（毫秒）
         */
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
        /**
         * 音标
         */
        private String ph;
        /**
         * 位置
         */
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
        /**
         * 语速
         */
        private Integer rate;
        /**
         * 开始位置
         */
        private Integer begin;
        /**
         * 结束位置
         */
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
        /**
         * 静音位置
         */
        private Integer location;
        /**
         * 静音时长（毫秒）
         */
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
     * 阅读规范配置
     */
    public static class ReadRuleConfig {
        /**
         * 规则ID
         */
        private Long ruleId;
        /**
         * 匹配模式（注意：文档中是 "partern" 不是 "pattern"）
         */
        private String partern;
        /**
         * 是否开启
         */
        private Boolean isOpen;

        public Long getRuleId() {
            return ruleId;
        }

        public void setRuleId(Long ruleId) {
            this.ruleId = ruleId;
        }

        public String getPartern() {
            return partern;
        }

        public void setPartern(String partern) {
            this.partern = partern;
        }

        public Boolean getIsOpen() {
            return isOpen;
        }

        public void setIsOpen(Boolean isOpen) {
            this.isOpen = isOpen;
        }
    }
}

