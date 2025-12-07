package com.yunting.dto.original;

import java.util.List;

public class OriginalSentenceListItemDTO {
    private Long originalSentenceId;
    private Integer sequence;
    private String content;
    private Integer synthesisStatus;
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

