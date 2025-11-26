package com.yunting.service.impl;

import com.yunting.dto.task.BreakingSentenceDTO;
import com.yunting.dto.task.TaskCreateRequest;
import com.yunting.dto.task.TaskDetailDTO;
import com.yunting.dto.task.TaskListItemDTO;
import com.yunting.dto.task.TaskListResponseDTO;
import com.yunting.dto.task.TaskSentenceDTO;
import com.yunting.exception.BusinessException;
import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.mapper.OriginalSentenceMapper;
import com.yunting.mapper.TaskMapper;
import com.yunting.model.BreakingSentence;
import com.yunting.model.OriginalSentence;
import com.yunting.model.Task;
import com.yunting.service.TaskService;
import com.yunting.util.SentenceSplitter;
import com.yunting.util.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private static final int STATUS_SPLIT = 2;

    private final TaskMapper taskMapper;
    private final OriginalSentenceMapper originalSentenceMapper;
    private final BreakingSentenceMapper breakingSentenceMapper;

    public TaskServiceImpl(TaskMapper taskMapper,
                           OriginalSentenceMapper originalSentenceMapper,
                           BreakingSentenceMapper breakingSentenceMapper) {
        this.taskMapper = taskMapper;
        this.originalSentenceMapper = originalSentenceMapper;
        this.breakingSentenceMapper = breakingSentenceMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskDetailDTO createTask(TaskCreateRequest request) {
        ValidationUtil.notNull(request, "请求参数不能为空");
        String content = request.getContent();
        ValidationUtil.notBlank(content, "文本内容不能为空");
        if (content.codePointCount(0, content.length()) > 10000) {
            throw new BusinessException(10400, "文本内容不能超过10000字");
        }
        List<String> sentences = SentenceSplitter.split(content, request.getDelimiters());
        if (CollectionUtils.isEmpty(sentences)) {
            throw new BusinessException("未识别到有效内容，请检查文本");
        }
        Task task = new Task();
        task.setContent(content);
        task.setCharCount(content.length());
        task.setStatus(STATUS_SPLIT);
        task.setBreakingStandardId(1);
        task.setCharCountLimit(null);
        taskMapper.insert(task);

        List<OriginalSentence> originalSentences = new ArrayList<>();
        List<BreakingSentence> breakingSentences = new ArrayList<>();
        int sequence = 1;
        for (String sentence : sentences) {
            OriginalSentence original = new OriginalSentence();
            original.setTaskId(task.getTaskId());
            original.setContent(sentence);
            original.setCharCount(sentence.length());
            original.setSequence(sequence);
            originalSentences.add(original);

            BreakingSentence breaking = new BreakingSentence();
            breaking.setTaskId(task.getTaskId());
            breaking.setContent(sentence);
            breaking.setCharCount(sentence.length());
            breaking.setSequence(sequence);
            breaking.setSynthesisStatus(0);
            breaking.setAudioUrl(null);
            breaking.setAudioDuration(null);
            breaking.setSsml(null);
            breakingSentences.add(breaking);
            sequence++;
        }
        originalSentenceMapper.insertBatch(originalSentences);
        List<OriginalSentence> persistedOriginals = originalSentenceMapper.selectByTaskId(task.getTaskId());
        if (persistedOriginals.size() != originalSentences.size()) {
            throw new BusinessException("保存原始拆句失败");
        }
        for (int i = 0; i < persistedOriginals.size(); i++) {
            OriginalSentence original = persistedOriginals.get(i);
            BreakingSentence breaking = breakingSentences.get(i);
            breaking.setOriginalSentenceId(original.getOriginalSentenceId());
        }
        breakingSentenceMapper.insertBatch(breakingSentences);
        List<BreakingSentence> persistedBreaking = breakingSentenceMapper.selectByTaskId(task.getTaskId());

        List<TaskSentenceDTO> sentenceDTOList = persistedBreaking.stream()
                .map(this::toTaskSentenceDTO)
                .collect(Collectors.toList());

        TaskDetailDTO detail = toTaskDetailDTO(taskMapper.selectById(task.getTaskId()));
        detail.setSentences(sentenceDTOList);
        detail.setTotalSentences(sentenceDTOList.size());
        return detail;
    }

    @Override
    public TaskDetailDTO getTaskDetail(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(10404, "任务不存在");
        }
        List<BreakingSentence> breakingSentences = breakingSentenceMapper.selectByTaskId(taskId);
        TaskDetailDTO detail = toTaskDetailDTO(task);
        detail.setBreakingSentences(
                breakingSentences.stream().map(this::toBreakingSentenceDTO).collect(Collectors.toList()));
        detail.setTotalSentences(detail.getBreakingSentences().size());
        return detail;
    }

    @Override
    public TaskListResponseDTO listTasks(Integer page, Integer pageSize, Integer status) {
        int currentPage = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? 20 : pageSize;

        int offset = (currentPage - 1) * size;
        List<Task> tasks = taskMapper.selectList(status, offset, size);
        long total = taskMapper.countByStatus(status);

        List<TaskListItemDTO> list = tasks.stream().map(task -> {
            TaskListItemDTO item = new TaskListItemDTO();
            item.setTaskId(task.getTaskId());
            item.setContent(preview(task.getContent()));
            item.setCharCount(task.getCharCount());
            item.setStatus(task.getStatus());
            item.setTotalSentences(breakingSentenceMapper.countByTaskId(task.getTaskId()));
            item.setCreatedAt(task.getCreatedAt());
            item.setUpdatedAt(task.getUpdatedAt());
            return item;
        }).collect(Collectors.toList());

        TaskListResponseDTO responseDTO = new TaskListResponseDTO();
        responseDTO.setList(list);
        responseDTO.setTotal(total);
        responseDTO.setPage(currentPage);
        responseDTO.setPageSize(size);
        return responseDTO;
    }

    private TaskDetailDTO toTaskDetailDTO(Task task) {
        TaskDetailDTO detail = new TaskDetailDTO();
        detail.setTaskId(task.getTaskId());
        detail.setContent(task.getContent());
        detail.setCharCount(task.getCharCount());
        detail.setStatus(task.getStatus());
        detail.setMergedAudioUrl(task.getMergedAudioUrl());
        detail.setMergedAudioDuration(task.getMergedAudioDuration());
        detail.setSsml(null);
        detail.setCreatedAt(task.getCreatedAt());
        detail.setUpdatedAt(task.getUpdatedAt());
        return detail;
    }

    private TaskSentenceDTO toTaskSentenceDTO(BreakingSentence breakingSentence) {
        TaskSentenceDTO dto = new TaskSentenceDTO();
        dto.setSentenceId(breakingSentence.getBreakingSentenceId());
        dto.setParentId(breakingSentence.getOriginalSentenceId());
        dto.setSequence(breakingSentence.getSequence());
        dto.setCharCount(breakingSentence.getCharCount());
        dto.setContent(breakingSentence.getContent());
        dto.setAudioUrl(breakingSentence.getAudioUrl());
        dto.setAudioDuration(breakingSentence.getAudioDuration());
        dto.setSsml(breakingSentence.getSsml());
        return dto;
    }

    private BreakingSentenceDTO toBreakingSentenceDTO(BreakingSentence breakingSentence) {
        BreakingSentenceDTO dto = new BreakingSentenceDTO();
        dto.setBreakingSentenceId(breakingSentence.getBreakingSentenceId());
        dto.setOriginalSentenceId(breakingSentence.getOriginalSentenceId());
        dto.setSequence(breakingSentence.getSequence());
        dto.setContent(breakingSentence.getContent());
        dto.setSynthesisStatus(breakingSentence.getSynthesisStatus());
        dto.setAudioUrl(breakingSentence.getAudioUrl());
        dto.setAudioDuration(breakingSentence.getAudioDuration());
        dto.setSsml(breakingSentence.getSsml());
        return dto;
    }

    private String preview(String content) {
        if (content == null) {
            return null;
        }
        return content.length() <= 50 ? content : content.substring(0, 50) + "...";
    }

}

