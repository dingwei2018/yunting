package com.yunting.service.impl;

import com.yunting.constant.SynthesisStatus;
import com.yunting.dto.synthesis.OriginalSentenceSynthesisStatusDTO;
import com.yunting.dto.synthesis.SynthesisResultDTO;
import com.yunting.dto.synthesis.TaskSynthesisStatusDTO;
import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.model.BreakingSentence;
import com.yunting.service.SynthesisStatusService;
import com.yunting.util.SynthesisStatusUtil;
import com.yunting.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 合成状态查询服务实现
 */
@Service
public class SynthesisStatusServiceImpl implements SynthesisStatusService {

    private static final Logger logger = LoggerFactory.getLogger(SynthesisStatusServiceImpl.class);

    private final BreakingSentenceMapper breakingSentenceMapper;

    public SynthesisStatusServiceImpl(BreakingSentenceMapper breakingSentenceMapper) {
        this.breakingSentenceMapper = breakingSentenceMapper;
    }

    @Override
    public SynthesisResultDTO getBreakingSentenceStatus(Long breakingSentenceId) {
        // 参数验证
        ValidationUtil.notNull(breakingSentenceId, "breakingSentenceId不能为空");
        
        // 查询断句信息
        BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
        if (sentence == null) {
            logger.warn("断句不存在，breakingSentenceId: {}", breakingSentenceId);
            throw new IllegalArgumentException("断句不存在");
        }
        
        // 构建返回结果
        SynthesisResultDTO result = new SynthesisResultDTO();
        result.setAudioUrl(sentence.getAudioUrl());
        result.setAudioDuration(sentence.getAudioDuration());
        // 如果 synthesisStatus 为 null，默认为 0（未合成）
        result.setSynthesisStatus(sentence.getSynthesisStatus() != null ? sentence.getSynthesisStatus() : SynthesisStatus.Status.PENDING);
        
        return result;
    }

    @Override
    public OriginalSentenceSynthesisStatusDTO getOriginalSentenceStatus(Long originalSentenceId) {
        // 参数验证
        ValidationUtil.notNull(originalSentenceId, "originalSentenceId不能为空");
        
        // 查询该拆句下的所有断句
        List<BreakingSentence> sentences = breakingSentenceMapper.selectByOriginalSentenceId(originalSentenceId);
        
        // 构建返回结果
        OriginalSentenceSynthesisStatusDTO result = new OriginalSentenceSynthesisStatusDTO();
        
        int total = sentences.size();
        long completed = sentences.stream()
                .filter(s -> Objects.equals(s.getSynthesisStatus(), SynthesisStatus.Status.COMPLETED))
                .count();
        long processing = sentences.stream()
                .filter(s -> Objects.equals(s.getSynthesisStatus(), SynthesisStatus.Status.PROCESSING))
                .count();
        long pending = sentences.stream()
                .filter(s -> s.getSynthesisStatus() == null || 
                           Objects.equals(s.getSynthesisStatus(), SynthesisStatus.Status.PENDING))
                .count();
        
        // 计算进度（1-100）
        int progress = total > 0 ? (int) Math.round((double) completed / total * 100) : 0;
        
        // 确定整体状态（使用统一工具类方法）
        Integer status = SynthesisStatusUtil.aggregateSynthesisStatus(sentences);
        
        result.setStatus(status);
        result.setProgress(progress);
        result.setTotal(total);
        result.setCompleted((int) completed);
        result.setPending((int) (processing + pending));
        
        // 构建音频URL列表（只包含已完成的断句）
        List<OriginalSentenceSynthesisStatusDTO.AudioUrlItem> audioUrlList = sentences.stream()
                .filter(s -> Objects.equals(s.getSynthesisStatus(), SynthesisStatus.Status.COMPLETED) 
                        && StringUtils.hasText(s.getAudioUrl()))
                .sorted((s1, s2) -> {
                    int seq1 = s1.getSequence() != null ? s1.getSequence() : 0;
                    int seq2 = s2.getSequence() != null ? s2.getSequence() : 0;
                    return Integer.compare(seq1, seq2);
                })
                .map(s -> {
                    OriginalSentenceSynthesisStatusDTO.AudioUrlItem item = 
                            new OriginalSentenceSynthesisStatusDTO.AudioUrlItem();
                    item.setSequence(s.getSequence());
                    item.setAudioUrl(s.getAudioUrl());
                    return item;
                })
                .collect(Collectors.toList());
        
        result.setAudioUrlList(audioUrlList);
        
        return result;
    }

    @Override
    public TaskSynthesisStatusDTO getTaskStatus(Long taskId) {
        // 参数验证
        ValidationUtil.notNull(taskId, "taskId不能为空");
        
        // 查询该任务下的所有断句
        List<BreakingSentence> sentences = breakingSentenceMapper.selectByTaskId(taskId);
        
        // 构建返回结果
        TaskSynthesisStatusDTO result = new TaskSynthesisStatusDTO();
        
        int total = sentences.size();
        long completed = sentences.stream()
                .filter(s -> Objects.equals(s.getSynthesisStatus(), SynthesisStatus.Status.COMPLETED))
                .count();
        long processing = sentences.stream()
                .filter(s -> Objects.equals(s.getSynthesisStatus(), SynthesisStatus.Status.PROCESSING))
                .count();
        long pending = sentences.stream()
                .filter(s -> s.getSynthesisStatus() == null || 
                           Objects.equals(s.getSynthesisStatus(), SynthesisStatus.Status.PENDING))
                .count();
        
        // 计算进度（1-100）
        int progress = total > 0 ? (int) Math.round((double) completed / total * 100) : 0;
        
        // 确定整体状态（使用统一工具类方法）
        Integer status = SynthesisStatusUtil.aggregateSynthesisStatus(sentences);
        
        result.setStatus(status);
        result.setProgress(progress);
        result.setTotal(total);
        result.setCompleted((int) completed);
        result.setPending((int) (processing + pending));
        
        // 构建音频URL列表（只包含已完成的断句）
        List<TaskSynthesisStatusDTO.AudioUrlItem> audioUrlList = sentences.stream()
                .filter(s -> Objects.equals(s.getSynthesisStatus(), SynthesisStatus.Status.COMPLETED) 
                        && StringUtils.hasText(s.getAudioUrl()))
                .sorted((s1, s2) -> {
                    int seq1 = s1.getSequence() != null ? s1.getSequence() : 0;
                    int seq2 = s2.getSequence() != null ? s2.getSequence() : 0;
                    return Integer.compare(seq1, seq2);
                })
                .map(s -> {
                    TaskSynthesisStatusDTO.AudioUrlItem item = 
                            new TaskSynthesisStatusDTO.AudioUrlItem();
                    item.setSequence(s.getSequence());
                    item.setAudioUrl(s.getAudioUrl());
                    return item;
                })
                .collect(Collectors.toList());
        
        result.setAudioUrlList(audioUrlList);
        
        return result;
    }
}
