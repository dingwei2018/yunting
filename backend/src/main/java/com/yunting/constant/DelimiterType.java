package com.yunting.constant;

/**
 * 拆句符号类型常量
 */
public final class DelimiterType {

    private DelimiterType() {
    }

    /**
     * 中文句号
     */
    public static final int CHINESE_PERIOD = 1;
    public static final char CHINESE_PERIOD_CHAR = '。';

    /**
     * 中文叹号
     */
    public static final int CHINESE_EXCLAMATION = 2;
    public static final char CHINESE_EXCLAMATION_CHAR = '！';

    /**
     * 中文问号
     */
    public static final int CHINESE_QUESTION = 3;
    public static final char CHINESE_QUESTION_CHAR = '？';

    /**
     * 中文省略号
     */
    public static final int CHINESE_ELLIPSIS = 4;
    public static final char CHINESE_ELLIPSIS_CHAR = '…';

    /**
     * 根据类型数字获取对应的字符
     *
     * @param type 拆句符号类型（1-4）
     * @return 对应的字符，如果类型无效返回 null
     */
    public static Character getCharByType(int type) {
        return switch (type) {
            case CHINESE_PERIOD -> CHINESE_PERIOD_CHAR;
            case CHINESE_EXCLAMATION -> CHINESE_EXCLAMATION_CHAR;
            case CHINESE_QUESTION -> CHINESE_QUESTION_CHAR;
            case CHINESE_ELLIPSIS -> CHINESE_ELLIPSIS_CHAR;
            default -> null;
        };
    }
}

