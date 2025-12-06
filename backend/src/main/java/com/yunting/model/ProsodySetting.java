package com.yunting.model;

import java.time.LocalDateTime;

public class ProsodySetting {
    private Long prosodyId;
    private Long breakingSentenceId;
    private Integer beginPosition;
    private Integer endPosition;
    private Integer rate;
    private LocalDateTime createdAt;

    public Long getProsodyId() {
        return prosodyId;
    }

    public void setProsodyId(Long prosodyId) {
        this.prosodyId = prosodyId;
    }

    public Long getBreakingSentenceId() {
        return breakingSentenceId;
    }

    public void setBreakingSentenceId(Long breakingSentenceId) {
        this.breakingSentenceId = breakingSentenceId;
    }

    public Integer getBeginPosition() {
        return beginPosition;
    }

    public void setBeginPosition(Integer beginPosition) {
        this.beginPosition = beginPosition;
    }

    public Integer getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(Integer endPosition) {
        this.endPosition = endPosition;
    }

    public Integer getRate() {
        return rate;
    }

    public void setRate(Integer rate) {
        this.rate = rate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

