package com.yunting.dto.audio;

import java.util.List;

public class AudioMergeRequest {
    private List<Long> sentenceIds;

    public List<Long> getSentenceIds() {
        return sentenceIds;
    }

    public void setSentenceIds(List<Long> sentenceIds) {
        this.sentenceIds = sentenceIds;
    }
}


