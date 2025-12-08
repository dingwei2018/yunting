package com.yunting.dto.synthesis;

import java.util.List;

public class OriginalSentenceSynthesisStatusDTO {
    private Integer status;
    private int progress;
    private int total;
    private int completed;
    private int pending;
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

    public static class AudioUrlItem {
        private Integer sequence;
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

