package com.yunting.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SentenceSplitter {

    private static final Set<Character> DEFAULT_DELIMITERS = Set.of('。', '！', '？', '!', '?', ';', '；');

    private SentenceSplitter() {
    }

    public static List<String> split(String text) {
        return split(text, null);
    }

    public static List<String> split(String text, String customDelimiters) {
        Set<Character> delimiters = resolveDelimiters(customDelimiters);
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return result;
        }
        String normalized = text.replace("\r\n", "\n");
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            buffer.append(ch);
            if (delimiters.contains(ch) || ch == '\n') {
                addSentence(result, buffer.toString());
                buffer.setLength(0);
            }
        }
        if (buffer.length() > 0) {
            addSentence(result, buffer.toString());
        }
        return result;
    }

    private static Set<Character> resolveDelimiters(String custom) {
        if (!StringUtils.hasText(custom)) {
            return DEFAULT_DELIMITERS;
        }
        Set<Character> set = new java.util.HashSet<>();
        for (char c : custom.toCharArray()) {
            set.add(c);
        }
        return set.isEmpty() ? DEFAULT_DELIMITERS : set;
    }

    private static void addSentence(List<String> result, String sentence) {
        String trimmed = sentence.trim();
        if (StringUtils.hasText(trimmed)) {
            result.add(trimmed);
        }
    }
}

