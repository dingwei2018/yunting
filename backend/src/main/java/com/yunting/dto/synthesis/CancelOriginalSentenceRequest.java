package com.yunting.dto.synthesis;

/**
 * 取消拆句合成请求DTO
 */
public class CancelOriginalSentenceRequest {
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

