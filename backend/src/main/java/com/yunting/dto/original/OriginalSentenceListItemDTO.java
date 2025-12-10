package com.yunting.dto.original;

import java.util.List;

/**
 * 拆句列表项DTO
 */
public class OriginalSentenceListItemDTO {
    /**
     * 拆句ID
     */
    private Long originalSentenceId;
    /**
     * 序号
     */
    private Integer sequence;
    /**
     * 拆句内容
     */
    private String content;
    /**
     * 合成状态（0-未合成，1-合成中，2-已合成，3-合成失败）
     */
    private Integer synthesisStatus;
    /**
     * 断句列表
     */
    private List<BreakingSentenceWithSettingDTO> breakingSentenceList;

    public Long getOriginalSentenceId() {
        return originalSentenceId;
    }

    public void setOriginalSentenceId(Long originalSentenceId) {
        this.originalSentenceId = originalSentenceId;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSynthesisStatus() {
        return synthesisStatus;
    }

    public void setSynthesisStatus(Integer synthesisStatus) {
        this.synthesisStatus = synthesisStatus;
    }

    public List<BreakingSentenceWithSettingDTO> getBreakingSentenceList() {
        return breakingSentenceList;
    }

    public void setBreakingSentenceList(List<BreakingSentenceWithSettingDTO> breakingSentenceList) {
        this.breakingSentenceList = breakingSentenceList;
    }
}

