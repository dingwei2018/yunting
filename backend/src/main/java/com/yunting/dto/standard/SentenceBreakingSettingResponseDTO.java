package com.yunting.dto.standard;

public class SentenceBreakingSettingResponseDTO {
    private Long taskId;
    private Integer breakingStandardId;
    private Integer charCount;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

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


