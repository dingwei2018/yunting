package com.yunting.util;

import com.yunting.model.SynthesisSetting;
import org.springframework.util.StringUtils;

public final class SsmlRenderer {

    private SsmlRenderer() {
    }

    public static String render(String content, SynthesisSetting setting) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String voiceId = setting != null && StringUtils.hasText(setting.getVoiceId()) ? setting.getVoiceId() : "default";
        int speechRate = setting != null ? setting.getSpeechRate() : 0;
        int volume = setting != null ? setting.getVolume() : 0;
        int pitch = setting != null ? setting.getPitch() : 0;
        String escaped = escape(content);
        return "<speak><voice name=\"" + voiceId + "\"><prosody rate=\"" + speechRate + "%\" volume=\"" +
                volume + "dB\" pitch=\"" + pitch + "%\">" + escaped + "</prosody></voice></speak>";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}


