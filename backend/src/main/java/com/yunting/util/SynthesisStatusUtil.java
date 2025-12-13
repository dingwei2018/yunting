package com.yunting.util;

import com.yunting.constant.SynthesisStatus;
import com.yunting.model.BreakingSentence;

import java.util.List;

/**
 * 合成状态聚合工具类
 */
public final class SynthesisStatusUtil {

    private SynthesisStatusUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 聚合断句列表的合成状态
     * 聚合规则（优先级从高到低）：
     * 1. 如果任一断句状态为 3（失败） → 返回 3（失败）
     * 2. 如果所有断句状态都是 2（已完成） → 返回 2（已完成）
     * 3. 如果任一断句状态为 1（合成中） → 返回 1（进行中）
     * 4. 默认 → 返回 0（未合成）
     *
     * @param breakingSentences 断句列表
     * @return 聚合后的合成状态值
     */
    public static Integer aggregateSynthesisStatus(List<BreakingSentence> breakingSentences) {
        if (breakingSentences == null || breakingSentences.isEmpty()) {
            return SynthesisStatus.Status.PENDING; // 未合成
        }

        // 如果任一断句状态为 3（失败） → 返回 3（失败）
        boolean hasFailed = breakingSentences.stream()
                .anyMatch(bs -> bs.getSynthesisStatus() != null && 
                        bs.getSynthesisStatus() == SynthesisStatus.Status.FAILED);
        if (hasFailed) {
            return SynthesisStatus.Status.FAILED; // 失败
        }

        // 如果所有断句状态都是 2（已完成） → 返回 2（已完成）
        boolean allCompleted = breakingSentences.stream()
                .allMatch(bs -> bs.getSynthesisStatus() != null && 
                        bs.getSynthesisStatus() == SynthesisStatus.Status.COMPLETED);
        if (allCompleted) {
            return SynthesisStatus.Status.COMPLETED; // 已完成
        }

        // 如果任一断句状态为 1（合成中） → 返回 1（进行中）
        boolean hasInProgress = breakingSentences.stream()
                .anyMatch(bs -> bs.getSynthesisStatus() != null && 
                        bs.getSynthesisStatus() == SynthesisStatus.Status.PROCESSING);
        if (hasInProgress) {
            return SynthesisStatus.Status.PROCESSING; // 进行中
        }

        return SynthesisStatus.Status.PENDING; // 默认未合成
    }
}

