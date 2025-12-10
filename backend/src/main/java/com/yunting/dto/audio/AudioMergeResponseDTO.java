package com.yunting.dto.audio;

/**
 * 音频合并响应DTO
 */
public class AudioMergeResponseDTO {
    /**
     * 合并任务ID
     */
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


