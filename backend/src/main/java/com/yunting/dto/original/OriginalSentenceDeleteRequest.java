package com.yunting.dto.original;

/**
 * 删除拆句请求DTO
 */
public class OriginalSentenceDeleteRequest {
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

