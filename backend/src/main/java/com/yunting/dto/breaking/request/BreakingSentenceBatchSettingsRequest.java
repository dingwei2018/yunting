package com.yunting.dto.breaking.request;

import java.util.List;

public class BreakingSentenceBatchSettingsRequest {
    private List<BreakingSentenceParamRequest> breakingSentences;

    public List<BreakingSentenceParamRequest> getBreakingSentences() {
        return breakingSentences;
    }

    public void setBreakingSentences(List<BreakingSentenceParamRequest> breakingSentences) {
        this.breakingSentences = breakingSentences;
    }
}


