package com.yunting.util;

import com.yunting.dto.breaking.request.*;
import com.yunting.model.SynthesisSetting;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SSML 渲染器
 * 根据华为云 SSML 规范生成 SSML 文本
 * 参考：https://support.huaweicloud.com/api-metastudio/metastudio_02_0038.html
 */
public final class SsmlRenderer {

    private SsmlRenderer() {
    }

    /**
     * 生成 SSML（简化版本，仅使用基础参数）
     */
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

    /**
     * 生成完整的 SSML（支持所有标签）
     */
    public static String renderFull(String content,
                                    SynthesisSetting setting,
                                    BreakingSentenceParamRequest request) {
        if (!StringUtils.hasText(content)) {
            return null;
        }

        // 获取基础参数
        String voiceId = setting != null && StringUtils.hasText(setting.getVoiceId()) ? setting.getVoiceId() : "default";
        int speechRate = setting != null ? setting.getSpeechRate() : 0;
        int volume = setting != null ? setting.getVolume() : 0;
        int pitch = setting != null ? setting.getPitch() : 0;

        // 构建标记列表（按位置排序）
        List<Mark> marks = new ArrayList<>();

        // 添加 break 标记
        if (request != null && !CollectionUtils.isEmpty(request.getPauses())) {
            for (PauseSettingRequest pause : request.getPauses()) {
                if (pause.getPosition() != null) {
                    marks.add(new Mark(pause.getPosition(), MarkType.BREAK, pause));
                }
            }
        }

        // 添加 phoneme 标记
        if (request != null && !CollectionUtils.isEmpty(request.getPolyphonic())) {
            for (PolyphonicSettingRequest poly : request.getPolyphonic()) {
                if (poly.getPosition() != null && StringUtils.hasText(poly.getCharacter())) {
                    marks.add(new Mark(poly.getPosition(), MarkType.PHONEME, poly));
                }
            }
        }

        // 添加 say-as 标记
        if (request != null && !CollectionUtils.isEmpty(request.getSayAs())) {
            for (SayAsSettingRequest sayAs : request.getSayAs()) {
                if (sayAs.getPosition() != null && sayAs.getLength() != null && sayAs.getLength() > 0) {
                    marks.add(new Mark(sayAs.getPosition(), MarkType.SAY_AS, sayAs));
                }
            }
        }

        // 添加 sub 标记
        if (request != null && !CollectionUtils.isEmpty(request.getSub())) {
            for (SubSettingRequest sub : request.getSub()) {
                if (sub.getPosition() != null && sub.getLength() != null && sub.getLength() > 0) {
                    marks.add(new Mark(sub.getPosition(), MarkType.SUB, sub));
                }
            }
        }

        // 添加 word 标记
        if (request != null && !CollectionUtils.isEmpty(request.getWord())) {
            for (WordSettingRequest word : request.getWord()) {
                if (word.getPosition() != null && word.getLength() != null && word.getLength() > 0) {
                    marks.add(new Mark(word.getPosition(), MarkType.WORD, word));
                }
            }
        }

        // 添加 emotion 标记
        if (request != null && !CollectionUtils.isEmpty(request.getEmotion())) {
            for (EmotionSettingRequest emotion : request.getEmotion()) {
                if (emotion.getPosition() != null && emotion.getLength() != null && emotion.getLength() > 0) {
                    marks.add(new Mark(emotion.getPosition(), MarkType.EMOTION, emotion));
                }
            }
        }

        // 添加 insert-action 标记
        if (request != null && !CollectionUtils.isEmpty(request.getInsertAction())) {
            for (InsertActionSettingRequest action : request.getInsertAction()) {
                if (action.getPosition() != null) {
                    marks.add(new Mark(action.getPosition(), MarkType.INSERT_ACTION, action));
                }
            }
        }

        // 按位置排序
        marks.sort(Comparator.comparingInt(Mark::getPosition));

        // 构建 SSML
        StringBuilder ssml = new StringBuilder();
        ssml.append("<speak>");
        ssml.append("<voice name=\"").append(escape(voiceId)).append("\">");
        ssml.append("<prosody rate=\"").append(speechRate).append("%\" volume=\"")
                .append(volume).append("dB\" pitch=\"").append(pitch).append("%\">");

        // 处理文本和标记
        int currentPos = 0;
        for (Mark mark : marks) {
            int markPos = mark.getPosition();
            
            // 添加标记前的文本
            if (markPos > currentPos && markPos <= content.length()) {
                String textBefore = content.substring(currentPos, markPos);
                ssml.append(escape(textBefore));
            }

            // 添加标记
            switch (mark.getType()) {
                case BREAK:
                    ssml.append(buildBreakTag((PauseSettingRequest) mark.getData()));
                    break;
                case PHONEME:
                    ssml.append(buildPhonemeTag((PolyphonicSettingRequest) mark.getData(), content));
                    break;
                case SAY_AS:
                    ssml.append(buildSayAsTag((SayAsSettingRequest) mark.getData(), content));
                    break;
                case SUB:
                    ssml.append(buildSubTag((SubSettingRequest) mark.getData(), content));
                    break;
                case WORD:
                    ssml.append(buildWordTag((WordSettingRequest) mark.getData(), content));
                    break;
                case EMOTION:
                    ssml.append(buildEmotionTag((EmotionSettingRequest) mark.getData(), content));
                    break;
                case INSERT_ACTION:
                    ssml.append(buildInsertActionTag((InsertActionSettingRequest) mark.getData()));
                    break;
            }

            // 更新当前位置
            if (mark.getType() == MarkType.PHONEME) {
                // phoneme 只标记单个字符
                currentPos = markPos + 1;
            } else if (mark.getType() == MarkType.BREAK || mark.getType() == MarkType.INSERT_ACTION) {
                // break 和 insert-action 不占用文本位置
                currentPos = markPos;
            } else {
                // 其他标记占用一定长度的文本
                int length = getMarkLength(mark);
                currentPos = markPos + length;
            }
        }

        // 添加剩余的文本
        if (currentPos < content.length()) {
            String remainingText = content.substring(currentPos);
            ssml.append(escape(remainingText));
        }

        ssml.append("</prosody>");
        ssml.append("</voice>");
        ssml.append("</speak>");

        return ssml.toString();
    }

    private static String buildBreakTag(PauseSettingRequest pause) {
        StringBuilder tag = new StringBuilder("<break");
        if (pause.getDuration() != null && pause.getDuration() > 0) {
            tag.append(" time=\"").append(pause.getDuration()).append("ms\"");
        } else if (pause.getType() != null) {
            // type: 1-停顿，2-静音，可以映射到 strength
            String strength = pause.getType() == 1 ? "medium" : "strong";
            tag.append(" strength=\"").append(strength).append("\"");
        }
        tag.append("/>");
        return tag.toString();
    }

    private static String buildPhonemeTag(PolyphonicSettingRequest poly, String content) {
        if (poly.getPosition() == null || poly.getPosition() >= content.length()) {
            return "";
        }
        String character = poly.getCharacter();
        if (!StringUtils.hasText(character)) {
            character = content.substring(poly.getPosition(), Math.min(poly.getPosition() + 1, content.length()));
        }
        String pronunciation = poly.getPronunciation();
        if (!StringUtils.hasText(pronunciation)) {
            return escape(character);
        }
        return "<phoneme ph=\"" + escape(pronunciation) + "\">" + escape(character) + "</phoneme>";
    }

    private static String buildSayAsTag(SayAsSettingRequest sayAs, String content) {
        if (sayAs.getPosition() == null || sayAs.getLength() == null || sayAs.getLength() <= 0) {
            return "";
        }
        int endPos = Math.min(sayAs.getPosition() + sayAs.getLength(), content.length());
        String text = content.substring(sayAs.getPosition(), endPos);
        StringBuilder tag = new StringBuilder("<say-as");
        if (StringUtils.hasText(sayAs.getInterpretAs())) {
            tag.append(" interpret-as=\"").append(escape(sayAs.getInterpretAs())).append("\"");
        }
        if (StringUtils.hasText(sayAs.getFormat())) {
            tag.append(" format=\"").append(escape(sayAs.getFormat())).append("\"");
        }
        tag.append(">").append(escape(text)).append("</say-as>");
        return tag.toString();
    }

    private static String buildSubTag(SubSettingRequest sub, String content) {
        if (sub.getPosition() == null || sub.getLength() == null || sub.getLength() <= 0) {
            return "";
        }
        int endPos = Math.min(sub.getPosition() + sub.getLength(), content.length());
        String text = content.substring(sub.getPosition(), endPos);
        if (!StringUtils.hasText(sub.getAlias())) {
            return escape(text);
        }
        return "<sub alias=\"" + escape(sub.getAlias()) + "\">" + escape(text) + "</sub>";
    }

    private static String buildWordTag(WordSettingRequest word, String content) {
        if (word.getPosition() == null || word.getLength() == null || word.getLength() <= 0) {
            return "";
        }
        int endPos = Math.min(word.getPosition() + word.getLength(), content.length());
        String text = content.substring(word.getPosition(), endPos);
        return "<word>" + escape(text) + "</word>";
    }

    private static String buildEmotionTag(EmotionSettingRequest emotion, String content) {
        if (emotion.getPosition() == null || emotion.getLength() == null || emotion.getLength() <= 0) {
            return "";
        }
        int endPos = Math.min(emotion.getPosition() + emotion.getLength(), content.length());
        String text = content.substring(emotion.getPosition(), endPos);
        String type = StringUtils.hasText(emotion.getType()) ? emotion.getType() : "DEFAULT";
        return "<emotion type=\"" + escape(type) + "\">" + escape(text) + "</emotion>";
    }

    private static String buildInsertActionTag(InsertActionSettingRequest action) {
        StringBuilder tag = new StringBuilder("<insert-action");
        if (StringUtils.hasText(action.getName())) {
            tag.append(" name=\"").append(escape(action.getName())).append("\"");
        }
        if (StringUtils.hasText(action.getTag())) {
            tag.append(" tag=\"").append(escape(action.getTag())).append("\"");
        }
        tag.append("/>");
        return tag.toString();
    }

    private static int getMarkLength(Mark mark) {
        switch (mark.getType()) {
            case PHONEME:
                return 1;  // phoneme 只标记单个字符
            case SAY_AS:
                SayAsSettingRequest sayAs = (SayAsSettingRequest) mark.getData();
                return sayAs.getLength() != null ? sayAs.getLength() : 0;
            case SUB:
                SubSettingRequest sub = (SubSettingRequest) mark.getData();
                return sub.getLength() != null ? sub.getLength() : 0;
            case WORD:
                WordSettingRequest word = (WordSettingRequest) mark.getData();
                return word.getLength() != null ? word.getLength() : 0;
            case EMOTION:
                EmotionSettingRequest emotion = (EmotionSettingRequest) mark.getData();
                return emotion.getLength() != null ? emotion.getLength() : 0;
            default:
                return 0;
        }
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * 标记类型
     */
    private enum MarkType {
        BREAK,
        PHONEME,
        SAY_AS,
        SUB,
        WORD,
        EMOTION,
        INSERT_ACTION
    }

    /**
     * 标记类
     */
    private static class Mark {
        private final int position;
        private final MarkType type;
        private final Object data;

        public Mark(int position, MarkType type, Object data) {
            this.position = position;
            this.type = type;
            this.data = data;
        }

        public int getPosition() {
            return position;
        }

        public MarkType getType() {
            return type;
        }

        public Object getData() {
            return data;
        }
    }
}
