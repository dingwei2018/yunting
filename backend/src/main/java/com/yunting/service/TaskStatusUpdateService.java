package com.yunting.service;

/**
 * 任务状态更新服务
 * 负责根据断句状态更新任务状态
 */
public interface TaskStatusUpdateService {

    /**
     * 根据 breaking_sentence 的合成状态更新 task 状态
     * 规则：
     * - 如果有 breaking_sentence 合成失败（状态为3），task 状态 = SYNTHESIS_FAILED（语音合成失败）
     * - 如果有 breaking_sentence 还未完成合成（状态为0或1），task 状态 = SYNTHESIS_PROCESSING（语音合成中）
     * - 如果全部 breaking_sentence 都已完成合成（状态都为2），task 状态 = SYNTHESIS_SUCCESS（语音合成成功）
     * 
     * @param taskId 任务ID
     */
    void updateTaskStatusIfNeeded(Long taskId);
}
