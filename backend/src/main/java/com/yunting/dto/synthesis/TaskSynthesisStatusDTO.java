package com.yunting.dto.synthesis;

public class TaskSynthesisStatusDTO {
    private Long taskId;
    private String status;
    private int progress;
    private int total;
    private int completed;
    private int pending;
    private SynthesisResultDTO result;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public int getPending() {
        return pending;
    }

    public void setPending(int pending) {
        this.pending = pending;
    }

    public SynthesisResultDTO getResult() {
        return result;
    }

    public void setResult(SynthesisResultDTO result) {
        this.result = result;
    }
}


