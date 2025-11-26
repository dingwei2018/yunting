package com.yunting.model;

import java.time.LocalDateTime;

public class PauseSetting {
    private Long pauseId;
    private Long breakingSentenceId;
    private Integer position;
    private Integer duration;
    private Integer type;
    private LocalDateTime createdAt;

    public Long getPauseId() {
        return pauseId;
    }

    public void setPauseId(Long pauseId) {
        this.pauseId = pauseId;
    }

    public Long getBreakingSentenceId() {
        return breakingSentenceId;
    }

    public void setBreakingSentenceId(Long breakingSentenceId) {
        this.breakingSentenceId = breakingSentenceId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


