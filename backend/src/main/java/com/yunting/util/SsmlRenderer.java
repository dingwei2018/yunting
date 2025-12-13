package com.yunting.util;

import com.yunting.dto.breaking.request.*;
import com.yunting.dto.synthesis.SynthesisSetConfigRequest;
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
     * 只使用 request 中实际传递的参数，未传递的参数不生成标签
     */
    public static String renderFull(String content,
                                    SynthesisSetting setting,
                                    BreakingSentenceParamRequest request) {
        if (!StringUtils.hasText(content)) {
            return null;
        }

        // 获取基础参数：只使用 request 中实际传递的参数
        // 如果 request 中没有传递，则不使用（不生成对应的标签属性）
        String voiceId = null;
        Integer speechRate = null;
        Integer volume = null;
        Integer pitch = null;
        
        if (request != null) {
            // 只使用 request 中实际传递的参数
            voiceId = request.getVoiceId();
            speechRate = request.getSpeechRate();
            volume = request.getVolume();
            pitch = request.getPitch();
        }
        
        // 不再设置默认值，如果 request 中没有传递 voiceId，就不生成 voice 标签

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
        
        // 只有当传递了 voiceId 时才生成 voice 标签
        if (voiceId != null && StringUtils.hasText(voiceId)) {
            ssml.append("<voice name=\"").append(escape(voiceId)).append("\">");
        }
        
        // 只生成传递了参数的 prosody 属性
        boolean hasProsodyAttr = false;
        StringBuilder prosodyAttrs = new StringBuilder();
        
        if (speechRate != null) {
            prosodyAttrs.append(" rate=\"").append(speechRate).append("%\"");
            hasProsodyAttr = true;
        }
        if (volume != null) {
            prosodyAttrs.append(" volume=\"").append(volume).append("dB\"");
            hasProsodyAttr = true;
        }
        if (pitch != null) {
            prosodyAttrs.append(" pitch=\"").append(pitch).append("%\"");
            hasProsodyAttr = true;
        }
        
        // 只有当至少有一个 prosody 属性时才生成 prosody 标签
        if (hasProsodyAttr) {
            ssml.append("<prosody").append(prosodyAttrs).append(">");
        }

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

        // 只有当生成了 prosody 标签时才关闭它
        if (hasProsodyAttr) {
            ssml.append("</prosody>");
        }
        
        // 只有当生成了 voice 标签时才关闭它
        if (voiceId != null && StringUtils.hasText(voiceId)) {
            ssml.append("</voice>");
        }
        
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
        if (poly.getPosition() == null || poly.getPosition() <= 0 || poly.getPosition() > content.length()) {
            return "";
        }
        String character = poly.getCharacter();
        if (!StringUtils.hasText(character)) {
            character = content.substring(poly.getPosition() - 1, poly.getPosition());
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

    /**
     * 从 SynthesisSetConfigRequest.BreakingSentenceConfig 生成 SSML
     * 支持 breakList、phonemeList 和 prosodyList
     * 注意：voiceId、volume、speed 不生成标签，只用于数据库存储
     */
    public static String renderFromConfig(SynthesisSetConfigRequest.BreakingSentenceConfig config) {
        if (config == null || !StringUtils.hasText(config.getContent())) {
            return null;
        }

        String content = config.getContent();

        // 构建标记列表（按位置排序）
        List<Mark> marks = new ArrayList<>();

        // 添加 break 标记
        if (!CollectionUtils.isEmpty(config.getBreakList())) {
            for (SynthesisSetConfigRequest.BreakConfig breakConfig : config.getBreakList()) {
                if (breakConfig.getLocation() != null) {
                    marks.add(new Mark(breakConfig.getLocation(), MarkType.BREAK, breakConfig));
                }
            }
        }

        // 添加 silence 标记（也使用 break 标签）
        if (!CollectionUtils.isEmpty(config.getSilenceList())) {
            for (SynthesisSetConfigRequest.SilenceConfig silenceConfig : config.getSilenceList()) {
                if (silenceConfig.getLocation() != null) {
                    // 将 SilenceConfig 转换为 BreakConfig 格式，以便使用相同的 buildBreakTagFromConfig 方法
                    SynthesisSetConfigRequest.BreakConfig breakConfig = new SynthesisSetConfigRequest.BreakConfig();
                    breakConfig.setLocation(silenceConfig.getLocation());
                    breakConfig.setDuration(silenceConfig.getDuration());
                    marks.add(new Mark(silenceConfig.getLocation(), MarkType.BREAK, breakConfig));
                }
            }
        }

        // 添加 phoneme 标记
        if (!CollectionUtils.isEmpty(config.getPhonemeList())) {
            for (SynthesisSetConfigRequest.PhonemeConfig phonemeConfig : config.getPhonemeList()) {
                if (phonemeConfig.getLocation() != null && StringUtils.hasText(phonemeConfig.getPh())) {
                    marks.add(new Mark(phonemeConfig.getLocation(), MarkType.PHONEME_CONFIG, phonemeConfig));
                }
            }
        }

        // prosody 标记单独处理，不添加到 marks 列表

        // 按位置排序
        marks.sort(Comparator.comparingInt(Mark::getPosition));

        // 构建 SSML
        StringBuilder ssml = new StringBuilder();
        ssml.append("<speak>");

        // 构建 prosody 范围列表（按 prosodyList 的顺序，不排序）
        List<ProsodyRange> prosodyRanges = new ArrayList<>();
        if (!CollectionUtils.isEmpty(config.getProsodyList())) {
            for (SynthesisSetConfigRequest.ProsodyConfig prosodyConfig : config.getProsodyList()) {
                if (prosodyConfig.getBegin() != null && prosodyConfig.getEnd() != null 
                        && prosodyConfig.getRate() != null
                        && prosodyConfig.getBegin() < prosodyConfig.getEnd()) {
                    prosodyRanges.add(new ProsodyRange(prosodyConfig.getBegin(), prosodyConfig.getEnd(), prosodyConfig.getRate()));
                }
            }
        }

        // 处理文本和标记
        int[] currentPos = {0};  // 使用数组包装，使其可以在 lambda 中修改
        int[] currentProsodyIndex = {0};  // 当前正在处理的 prosody 范围索引
        ProsodyRange[] currentProsodyRange = {null};  // 当前激活的 prosody 范围（同一时间只有一个），使用数组包装
        
        // 辅助方法：检查并打开下一个 prosody 标签
        Runnable checkAndOpenProsody = () -> {
            // 如果当前没有激活的 prosody 范围，检查是否需要打开下一个
            if (currentProsodyRange[0] == null && currentProsodyIndex[0] < prosodyRanges.size()) {
                ProsodyRange range = prosodyRanges.get(currentProsodyIndex[0]);
                // currentPos[0] 是从 0 开始的索引，range.getBegin() 是从 1 开始的位置
                // currentPos[0] + 1 表示当前已处理完的位置（从1开始）
                // 如果 >= range.getBegin()，说明已经到了或超过了 begin 位置，可以打开标签
                if (currentPos[0] + 1 >= range.getBegin()) {
                    ssml.append("<prosody rate=\"").append(range.getRate()).append("\">");
                    currentProsodyRange[0] = range;
                }
            }
        };
        
        // 辅助方法：检查并关闭当前的 prosody 标签
        Runnable checkAndCloseProsody = () -> {
            // 如果当前有激活的 prosody 范围，检查是否需要关闭
            if (currentProsodyRange[0] != null) {
                // currentPos[0] 是从 0 开始的索引，range.getEnd() 是从 1 开始的位置
                // currentPos[0] + 1 表示当前已处理完的位置（从1开始）
                // 如果 > range.getEnd()，说明已经处理完 end 位置的字符，可以关闭标签
                if (currentPos[0] + 1 > currentProsodyRange[0].getEnd()) {
                    ssml.append("</prosody>");
                    currentProsodyRange[0] = null;
                    currentProsodyIndex[0]++;  // 移动到下一个 prosody 范围
                }
            }
        };
        
        for (Mark mark : marks) {
            int markPos = mark.getPosition();

            // 处理标记前的文本
            if (markPos > currentPos[0] && markPos <= content.length()) {
                // 对于PHONEME_CONFIG，需要跳过它要标记的字符（location-1位置的字符）
                // 所以只输出到markPos-1，避免重复输出
                int endPos = mark.getType() == MarkType.PHONEME_CONFIG ? markPos - 1 : markPos;
                if (endPos > currentPos[0]) {
                    // 逐字符处理，以便正确管理 prosody 标签
                    for (int i = currentPos[0]; i < endPos; i++) {
                        // 先更新当前位置，以便正确判断标签状态
                        currentPos[0] = i;
                        
                        // 检查并关闭当前 prosody 标签（如果已到结束位置）
                        checkAndCloseProsody.run();
                        
                        // 检查并打开下一个 prosody 标签（如果已到开始位置）
                        checkAndOpenProsody.run();
                        
                        // 输出字符
                        ssml.append(escape(String.valueOf(content.charAt(i))));
                        
                        // 更新位置到下一个字符
                        currentPos[0] = i + 1;
                        
                        // 再次检查并关闭当前 prosody 标签（在输出字符并更新位置之后）
                        checkAndCloseProsody.run();
                    }
                }
            }

            // 更新 prosody 标签状态到标记位置
            currentPos[0] = markPos;
            checkAndCloseProsody.run();
            checkAndOpenProsody.run();

            // 添加标记
            switch (mark.getType()) {
                case BREAK:
                    ssml.append(buildBreakTagFromConfig((SynthesisSetConfigRequest.BreakConfig) mark.getData()));
                    // break 不占用文本位置，currentPos 保持不变
                    break;
                case PHONEME_CONFIG:
                    ssml.append(buildPhonemeTagFromConfig((SynthesisSetConfigRequest.PhonemeConfig) mark.getData(), content));
                    // phoneme 标签已经包含了 location-1 位置的字符，所以 currentPos 更新为 markPos
                    // 这样后续文本会从 markPos 位置继续输出
                    currentPos[0] = markPos;
                    break;
            }
        }

        // 处理剩余文本
        if (currentPos[0] < content.length()) {
            for (int i = currentPos[0]; i < content.length(); i++) {
                // 先更新当前位置，以便正确判断标签状态
                currentPos[0] = i;
                
                // 检查并关闭当前 prosody 标签（如果已到结束位置）
                checkAndCloseProsody.run();
                
                // 检查并打开下一个 prosody 标签（如果已到开始位置）
                checkAndOpenProsody.run();
                
                // 输出字符
                ssml.append(escape(String.valueOf(content.charAt(i))));
                
                // 更新位置到下一个字符
                currentPos[0] = i + 1;
                
                // 再次检查并关闭当前 prosody 标签（在输出字符并更新位置之后）
                checkAndCloseProsody.run();
            }
        }

        // 关闭所有剩余的 prosody 标签
        if (currentProsodyRange[0] != null) {
            ssml.append("</prosody>");
        }

        ssml.append("</speak>");

        return ssml.toString();
    }

    private static String buildBreakTagFromConfig(SynthesisSetConfigRequest.BreakConfig breakConfig) {
        StringBuilder tag = new StringBuilder("<break");
        if (breakConfig.getDuration() != null && breakConfig.getDuration() > 0) {
            tag.append(" time=\"").append(breakConfig.getDuration()).append("ms\"");
        }
        tag.append("/>");
        return tag.toString();
    }

    private static String buildPhonemeTagFromConfig(SynthesisSetConfigRequest.PhonemeConfig phonemeConfig, String content) {
        if (phonemeConfig.getLocation() == null || phonemeConfig.getLocation() <= 0 || phonemeConfig.getLocation() > content.length()) {
            return "";
        }
        String character = content.substring(phonemeConfig.getLocation() - 1, phonemeConfig.getLocation());
        String pronunciation = phonemeConfig.getPh();
        if (!StringUtils.hasText(pronunciation)) {
            return escape(character);
        }
        return "<phoneme ph=\"" + escape(pronunciation) + "\">" + escape(character) + "</phoneme>";
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
        PHONEME_CONFIG,  // 用于 SynthesisSetConfigRequest 的 phoneme
        SAY_AS,
        SUB,
        WORD,
        EMOTION,
        INSERT_ACTION,
        PROSODY  // 用于局部语速
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

    /**
     * Prosody 范围类
     */
    private static class ProsodyRange {
        private final int begin;
        private final int end;
        private final int rate;

        public ProsodyRange(int begin, int end, int rate) {
            this.begin = begin;
            this.end = end;
            this.rate = rate;
        }

        public int getBegin() {
            return begin;
        }

        public int getEnd() {
            return end;
        }

        public int getRate() {
            return rate;
        }
    }
}
