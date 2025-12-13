package com.yunting.dto.synthesis;

/**
 * 取消合成请求DTO
 */
public class CancelSynthesisRequest {
    /**
     * 断句ID（必填）
     */
    private Long breakingSentenceId;

    public Long getBreakingSentenceId() {
        return breakingSentenceId;
    }

    public void setBreakingSentenceId(Long breakingSentenceId) {
        this.breakingSentenceId = breakingSentenceId;
    }
}

