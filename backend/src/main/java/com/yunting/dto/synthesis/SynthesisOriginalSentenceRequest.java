package com.yunting.dto.synthesis;

/**
 * 合成拆句请求DTO
 */
public class SynthesisOriginalSentenceRequest {
    /**
     * 拆句ID（必填）
     */
    private Long originalSentenceId;

    public Long getOriginalSentenceId() {
        return originalSentenceId;
    }

    public void setOriginalSentenceId(Long originalSentenceId) {
        this.originalSentenceId = originalSentenceId;
    }
}

