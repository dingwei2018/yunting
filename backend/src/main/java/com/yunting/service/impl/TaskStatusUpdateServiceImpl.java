package com.yunting.service.impl;

import com.yunting.constant.SynthesisStatus;
import com.yunting.constant.TaskStatus;
import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.mapper.TaskMapper;
import com.yunting.model.BreakingSentence;
import com.yunting.service.TaskStatusUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 任务状态更新服务实现
 */
@Service
public class TaskStatusUpdateServiceImpl implements TaskStatusUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(TaskStatusUpdateServiceImpl.class);

    private final BreakingSentenceMapper breakingSentenceMapper;
    private final TaskMapper taskMapper;

    public TaskStatusUpdateServiceImpl(BreakingSentenceMapper breakingSentenceMapper,
                                       TaskMapper taskMapper) {
        this.breakingSentenceMapper = breakingSentenceMapper;
        this.taskMapper = taskMapper;
    }

    @Override
    public void updateTaskStatusIfNeeded(Long taskId) {
        try {
            // 1. 查询该 task 下所有 breaking_sentence
            List<BreakingSentence> sentences = breakingSentenceMapper.selectByTaskId(taskId);
            if (sentences.isEmpty()) {
                logger.warn("任务下没有断句，taskId: {}", taskId);
                return;
            }

            // 2. 统计各状态的数量
            int total = sentences.size();
            long completed = sentences.stream()
                    .filter(s -> Objects.equals(s.getSynthesisStatus(), SynthesisStatus.Status.COMPLETED))
                    .count();
            long failed = sentences.stream()
                    .filter(s -> Objects.equals(s.getSynthesisStatus(), SynthesisStatus.Status.FAILED))
                    .count();
            long processing = sentences.stream()
                    .filter(s -> Objects.equals(s.getSynthesisStatus(), SynthesisStatus.Status.PROCESSING))
                    .count();
            long pending = sentences.stream()
                    .filter(s -> Objects.equals(s.getSynthesisStatus(), SynthesisStatus.Status.PENDING))
                    .count();

            // 3. 根据规则判断并更新 task 状态（优先级：失败 > 进行中 > 已完成）
            Integer newStatus = null;
            if (failed > 0) {
                // 如果有失败的断句，task 状态 = SYNTHESIS_FAILED（语音合成失败）
                newStatus = TaskStatus.Status.SYNTHESIS_FAILED;
            } else if (processing > 0 || pending > 0) {
                // 如果有进行中或待处理的断句，task 状态 = SYNTHESIS_PROCESSING（语音合成中）
                newStatus = TaskStatus.Status.SYNTHESIS_PROCESSING;
            } else if (completed == total) {
                // 如果全部完成，task 状态 = SYNTHESIS_SUCCESS（语音合成成功）
                newStatus = TaskStatus.Status.SYNTHESIS_SUCCESS;
            }

            // 4. 更新 task 状态
            if (newStatus != null) {
                taskMapper.updateStatus(taskId, newStatus);
                logger.info("更新任务状态，taskId: {}, newStatus: {}, total: {}, completed: {}, failed: {}, processing: {}, pending: {}", 
                        taskId, newStatus, total, completed, failed, processing, pending);
            }
        } catch (Exception e) {
            logger.error("更新任务状态失败，taskId: {}", taskId, e);
            // 不抛出异常，避免影响主流程
        }
    }
}
