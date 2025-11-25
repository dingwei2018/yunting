package com.yunting.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SentenceSplitter {

    private static final Set<Character> DELIMITERS = Set.of('。', '！', '？', '!', '?', ';', '；');

    private SentenceSplitter() {
    }

    public static List<String> split(String text) {
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return result;
        }
        String normalized = text.replace("\r\n", "\n");
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            buffer.append(ch);
            if (DELIMITERS.contains(ch) || ch == '\n') {
                addSentence(result, buffer.toString());
                buffer.setLength(0);
            }
        }
        if (buffer.length() > 0) {
            addSentence(result, buffer.toString());
        }
        return result;
    }

    private static void addSentence(List<String> result, String sentence) {
        String trimmed = sentence.trim();
        if (StringUtils.hasText(trimmed)) {
            result.add(trimmed);
        }
    }
}

