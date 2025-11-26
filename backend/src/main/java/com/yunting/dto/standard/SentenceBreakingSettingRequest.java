package com.yunting.dto.standard;

public class SentenceBreakingSettingRequest {
    private Integer breakingStandardId;
    private Integer charCount;

    public Integer getBreakingStandardId() {
        return breakingStandardId;
    }

    public void setBreakingStandardId(Integer breakingStandardId) {
        this.breakingStandardId = breakingStandardId;
    }

    public Integer getCharCount() {
        return charCount;
    }

    public void setCharCount(Integer charCount) {
        this.charCount = charCount;
    }
}


