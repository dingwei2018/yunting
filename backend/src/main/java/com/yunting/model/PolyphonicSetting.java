package com.yunting.model;

import java.time.LocalDateTime;

public class PolyphonicSetting {
    private Long polyphonicId;
    private Long breakingSentenceId;
    private String character;
    private Integer position;
    private String pronunciation;
    private LocalDateTime createdAt;

    public Long getPolyphonicId() {
        return polyphonicId;
    }

    public void setPolyphonicId(Long polyphonicId) {
        this.polyphonicId = polyphonicId;
    }

    public Long getBreakingSentenceId() {
        return breakingSentenceId;
    }

    public void setBreakingSentenceId(Long breakingSentenceId) {
        this.breakingSentenceId = breakingSentenceId;
    }

    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getPronunciation() {
        return pronunciation;
    }

    public void setPronunciation(String pronunciation) {
        this.pronunciation = pronunciation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


