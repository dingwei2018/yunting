package com.yunting.dto.synthesis;

/**
 * 合成断句请求DTO
 */
public class SynthesisBreakingSentenceRequest {
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

