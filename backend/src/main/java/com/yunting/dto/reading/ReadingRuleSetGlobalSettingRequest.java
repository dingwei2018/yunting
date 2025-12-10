package com.yunting.dto.reading;

/**
 * 开关全局阅读规范请求DTO
 */
public class ReadingRuleSetGlobalSettingRequest {
    /**
     * 任务ID
     */
    private Long taskId;
    /**
     * 规则ID
     */
    private Long ruleId;
    /**
     * 是否开启（true-打开，false-关闭）
     */
    private Boolean isOpen;
    /**
     * 断句ID（可选），如果有值则type=1（任务级），如果为空则type=2（断句级）
     */
    private Long breakingSentenceId;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Boolean getIsOpen() {
        return isOpen;
    }

    public void setIsOpen(Boolean isOpen) {
        this.isOpen = isOpen;
    }

    public Long getBreakingSentenceId() {
        return breakingSentenceId;
    }

    public void setBreakingSentenceId(Long breakingSentenceId) {
        this.breakingSentenceId = breakingSentenceId;
    }
}

