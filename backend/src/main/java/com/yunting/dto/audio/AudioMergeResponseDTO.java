package com.yunting.dto.audio;

public class AudioMergeResponseDTO {
    private Long mergeId;

    public AudioMergeResponseDTO() {
    }

    public AudioMergeResponseDTO(Long mergeId) {
        this.mergeId = mergeId;
    }

    public Long getMergeId() {
        return mergeId;
    }

    public void setMergeId(Long mergeId) {
        this.mergeId = mergeId;
    }
}


