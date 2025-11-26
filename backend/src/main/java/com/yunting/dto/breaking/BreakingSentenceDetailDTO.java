package com.yunting.dto.breaking;

import java.util.List;

public class BreakingSentenceDetailDTO extends BreakingSentenceListItemDTO {
    private List<PauseDTO> pauses;
    private List<PolyphonicSettingDTO> polyphonicSettings;

    public List<PauseDTO> getPauses() {
        return pauses;
    }

    public void setPauses(List<PauseDTO> pauses) {
        this.pauses = pauses;
    }

    public List<PolyphonicSettingDTO> getPolyphonicSettings() {
        return polyphonicSettings;
    }

    public void setPolyphonicSettings(List<PolyphonicSettingDTO> polyphonicSettings) {
        this.polyphonicSettings = polyphonicSettings;
    }
}


