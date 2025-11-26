package com.yunting.service.impl;

import com.yunting.dto.audio.AudioMergeRequest;
import com.yunting.dto.audio.AudioMergeResponseDTO;
import com.yunting.exception.BusinessException;
import com.yunting.mapper.AudioMergeMapper;
import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.mapper.TaskMapper;
import com.yunting.model.AudioMerge;
import com.yunting.model.BreakingSentence;
import com.yunting.model.Task;
import com.yunting.service.AudioMergeService;
import com.yunting.util.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AudioMergeServiceImpl implements AudioMergeService {

    private final AudioMergeMapper audioMergeMapper;
    private final TaskMapper taskMapper;
    private final BreakingSentenceMapper breakingSentenceMapper;

    public AudioMergeServiceImpl(AudioMergeMapper audioMergeMapper,
                                 TaskMapper taskMapper,
                                 BreakingSentenceMapper breakingSentenceMapper) {
        this.audioMergeMapper = audioMergeMapper;
        this.taskMapper = taskMapper;
        this.breakingSentenceMapper = breakingSentenceMapper;
    }

    @Override
    public AudioMergeResponseDTO mergeAudio(Long taskId, AudioMergeRequest request) {
        ValidationUtil.notNull(taskId, "taskid不能为空");
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(10404, "任务不存在");
        }
        List<BreakingSentence> candidates = breakingSentenceMapper.selectByTaskId(taskId);
        if (CollectionUtils.isEmpty(candidates)) {
            throw new BusinessException(10404, "任务暂无断句");
        }

        List<Long> sentenceIds = request != null ? request.getSentenceIds() : null;
        List<BreakingSentence> toMerge;
        if (!CollectionUtils.isEmpty(sentenceIds)) {
            toMerge = candidates.stream()
                    .filter(bs -> sentenceIds.contains(bs.getBreakingSentenceId()))
                    .sorted(Comparator.comparing(BreakingSentence::getSequence))
                    .collect(Collectors.toList());
            if (toMerge.size() != sentenceIds.size()) {
                throw new BusinessException(10404, "部分断句不存在");
            }
        } else {
            toMerge = candidates.stream()
                    .filter(bs -> bs.getAudioUrl() != null)
                    .sorted(Comparator.comparing(BreakingSentence::getSequence))
                    .collect(Collectors.toList());
        }

        if (toMerge.isEmpty()) {
            throw new BusinessException(10404, "没有可合并的断句");
        }

        int duration = toMerge.stream()
                .map(BreakingSentence::getAudioDuration)
                .filter(val -> val != null && val > 0)
                .reduce(0, Integer::sum);
        String mergedUrl = buildMergedAudioUrl(taskId);
        AudioMerge audioMerge = new AudioMerge();
        audioMerge.setTaskId(taskId);
        audioMerge.setBreakingSentenceIds(
                toMerge.stream()
                        .map(BreakingSentence::getBreakingSentenceId)
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")));
        audioMerge.setMergedAudioUrl(mergedUrl);
        audioMerge.setAudioDuration(duration);
        audioMerge.setStatus(3); // completed
        audioMergeMapper.insert(audioMerge);

        task.setMergedAudioUrl(mergedUrl);
        task.setMergedAudioDuration(duration);

        return toResponse(audioMerge);
    }

    @Override
    public AudioMergeResponseDTO getMergeStatus(Long mergeId) {
        ValidationUtil.notNull(mergeId, "mergeid不能为空");
        AudioMerge audioMerge = audioMergeMapper.selectById(mergeId);
        if (audioMerge == null) {
            throw new BusinessException(10404, "合并任务不存在");
        }
        return toResponse(audioMerge);
    }

    private AudioMergeResponseDTO toResponse(AudioMerge audioMerge) {
        AudioMergeResponseDTO dto = new AudioMergeResponseDTO();
        dto.setMergeId(audioMerge.getMergeId());
        dto.setTaskId(audioMerge.getTaskId());
        dto.setMergedAudioUrl(audioMerge.getMergedAudioUrl());
        dto.setAudioDuration(audioMerge.getAudioDuration());
        dto.setStatus(mapStatus(audioMerge.getStatus()));
        return dto;
    }

    private String mapStatus(Integer status) {
        if (status == null) {
            return "pending";
        }
        return switch (status) {
            case 1 -> "pending";
            case 2 -> "processing";
            case 3 -> "completed";
            case 4 -> "failed";
            default -> "unknown";
        };
    }

    private String buildMergedAudioUrl(Long taskId) {
        return "https://example.com/audio/task_" + taskId + "_merged.mp3";
    }
}


