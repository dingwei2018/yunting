package com.yunting.service;

import com.yunting.dto.synthesis.OriginalSentenceSynthesisStatusDTO;
import com.yunting.dto.synthesis.SynthesisResultDTO;
import com.yunting.dto.synthesis.TaskSynthesisStatusDTO;

/**
 * 合成状态查询服务
 * 负责查询合成状态信息
 */
public interface SynthesisStatusService {

    /**
     * 获取断句合成状态
     * 给出单句断句的合成状态和已完成合成的音频文件下载地址和时长
     * 
     * @param breakingSentenceId 断句ID
     * @return 合成结果DTO，包含音频URL、时长和合成状态
     */
    SynthesisResultDTO getBreakingSentenceStatus(Long breakingSentenceId);

    /**
     * 获取拆句合成状态
     * 给出拆句下所有断句的合成进度和已完成合成的音频文件下载地址和时长
     * 
     * @param originalSentenceId 拆句ID
     * @return 拆句合成状态DTO，包含进度、统计信息和音频URL列表
     */
    OriginalSentenceSynthesisStatusDTO getOriginalSentenceStatus(Long originalSentenceId);

    /**
     * 获取任务合成状态
     * 给出任务下所有断句的合成进度和已完成合成的音频文件下载地址和时长
     * 
     * @param taskId 任务ID
     * @return 任务合成状态DTO，包含进度、统计信息和音频URL列表
     */
    TaskSynthesisStatusDTO getTaskStatus(Long taskId);
}
