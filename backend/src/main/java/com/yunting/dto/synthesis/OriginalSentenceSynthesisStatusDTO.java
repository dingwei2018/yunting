package com.yunting.dto.synthesis;

import java.util.List;

/**
 * 拆句合成状态DTO
 */
public class OriginalSentenceSynthesisStatusDTO {
    /**
     * 整体状态（0-未合成，1-合成中，2-已合成，3-合成失败）
     */
    private Integer status;
    /**
     * 进度百分比（0-100）
     */
    private int progress;
    /**
     * 总断句数
     */
    private int total;
    /**
     * 已完成数
     */
    private int completed;
    /**
     * 待处理数
     */
    private int pending;
    /**
     * 音频URL列表
     */
    private List<AudioUrlItem> audioUrlList;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public int getPending() {
        return pending;
    }

    public void setPending(int pending) {
        this.pending = pending;
    }

    public List<AudioUrlItem> getAudioUrlList() {
        return audioUrlList;
    }

    public void setAudioUrlList(List<AudioUrlItem> audioUrlList) {
        this.audioUrlList = audioUrlList;
    }

    /**
     * 音频URL项
     */
    public static class AudioUrlItem {
        /**
         * 序号
         */
        private Integer sequence;
        /**
         * 音频URL
         */
        private String audioUrl;

        public Integer getSequence() {
            return sequence;
        }

        public void setSequence(Integer sequence) {
            this.sequence = sequence;
        }

        public String getAudioUrl() {
            return audioUrl;
        }

        public void setAudioUrl(String audioUrl) {
            this.audioUrl = audioUrl;
        }
    }
}

