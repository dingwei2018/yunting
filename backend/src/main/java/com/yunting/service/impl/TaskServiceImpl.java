package com.yunting.service.impl;

import com.yunting.dto.task.OriginalSentenceDTO;
import com.yunting.dto.task.TaskCreateRequest;
import com.yunting.dto.task.TaskCreateResponseDTO;
import com.yunting.dto.task.TaskDetailDTO;
import com.yunting.exception.BusinessException;
import com.yunting.mapper.AudioMergeMapper;
import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.mapper.OriginalSentenceMapper;
import com.yunting.mapper.TaskMapper;
import com.yunting.model.AudioMerge;
import com.yunting.model.BreakingSentence;
import com.yunting.model.OriginalSentence;
import com.yunting.model.Task;
import com.yunting.service.TaskService;
import com.yunting.util.SentenceSplitter;
import com.yunting.util.ValidationUtil;
import com.yunting.constant.TaskStatus;
import com.yunting.constant.SynthesisStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务服务实现类
 */
@Service
public class TaskServiceImpl implements TaskService {

    /**
     * 默认断句标准ID：大符号
     */
    private static final int DEFAULT_BREAKING_STANDARD_ID = 1;

    private final TaskMapper taskMapper;
    private final OriginalSentenceMapper originalSentenceMapper;
    private final BreakingSentenceMapper breakingSentenceMapper;
    private final AudioMergeMapper audioMergeMapper;

    public TaskServiceImpl(TaskMapper taskMapper,
                           OriginalSentenceMapper originalSentenceMapper,
                           BreakingSentenceMapper breakingSentenceMapper,
                           AudioMergeMapper audioMergeMapper) {
        this.taskMapper = taskMapper;
        this.originalSentenceMapper = originalSentenceMapper;
        this.breakingSentenceMapper = breakingSentenceMapper;
        this.audioMergeMapper = audioMergeMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskCreateResponseDTO createTask(TaskCreateRequest request) {
        // 参数校验
        ValidationUtil.notNull(request, "请求参数不能为空");
        String content = request.getContent();
        ValidationUtil.notBlank(content, "文本内容不能为空");
        
        // 字符数校验（使用 codePointCount 来正确计算中文字符）
        if (content.codePointCount(0, content.length()) > 10000) {
            throw new BusinessException(10400, "文本内容不能超过10000字");
        }

        // 拆句处理
        String delimitersStr = request.getDelimitersAsString();
        List<String> sentences = SentenceSplitter.split(content, delimitersStr);
        if (CollectionUtils.isEmpty(sentences)) {
            throw new BusinessException("未识别到有效内容，请检查文本");
        }

        // 创建任务记录
        Task task = new Task();
        task.setContent(content);
        task.setCharCount(content.length());
        task.setStatus(TaskStatus.Status.BREAKING_COMPLETED);
        task.setBreakingStandardId(DEFAULT_BREAKING_STANDARD_ID);
        task.setCharCountLimit(null);
        task.setMergedAudioUrl(null);
        task.setMergedAudioDuration(null);
        taskMapper.insert(task);
        
        // 重新查询任务以获取数据库自动填充的 createdAt 和 updatedAt 字段
        Task persistedTask = taskMapper.selectById(task.getTaskId());
        if (persistedTask == null) {
            throw new BusinessException("任务创建失败");
        }

        // 创建原始拆句记录
        List<OriginalSentence> originalSentences = new ArrayList<>();
        int sequence = 1;
        for (String sentence : sentences) {
            OriginalSentence original = new OriginalSentence();
            original.setTaskId(task.getTaskId());
            original.setContent(sentence);
            original.setCharCount(sentence.length());
            original.setSequence(sequence);
            originalSentences.add(original);
            sequence++;
        }
        originalSentenceMapper.insertBatch(originalSentences);

        // 查询已保存的原始拆句（获取生成的ID）
        List<OriginalSentence> persistedOriginals = originalSentenceMapper.selectByTaskId(task.getTaskId());
        if (persistedOriginals.size() != originalSentences.size()) {
            throw new BusinessException("保存原始拆句失败");
        }

        // 为每条原始拆句创建对应的断句记录
        List<BreakingSentence> breakingSentences = new ArrayList<>();
        for (OriginalSentence original : persistedOriginals) {
            BreakingSentence breaking = new BreakingSentence();
            breaking.setTaskId(task.getTaskId());
            breaking.setOriginalSentenceId(original.getOriginalSentenceId());
            breaking.setContent(original.getContent());
            breaking.setCharCount(original.getCharCount());
            breaking.setSequence(original.getSequence());
            breaking.setSynthesisStatus(SynthesisStatus.Status.PENDING); // 未合成
            breaking.setAudioUrl(null);
            breaking.setAudioDuration(null);
            breaking.setSsml(null);
            breakingSentences.add(breaking);
        }
        breakingSentenceMapper.insertBatch(breakingSentences);

        // 构建响应DTO
        TaskCreateResponseDTO response = new TaskCreateResponseDTO();
        response.setTaskId(persistedTask.getTaskId());
        response.setContent(persistedTask.getContent());
        response.setCharCount(persistedTask.getCharCount());
        response.setStatus(persistedTask.getStatus());
        // audioUrl 和 audioDuration 如果为 null，则设置为空值
        response.setAudioUrl(persistedTask.getMergedAudioUrl() != null ? persistedTask.getMergedAudioUrl() : "");
        response.setAudioDuration(persistedTask.getMergedAudioDuration() != null ? persistedTask.getMergedAudioDuration() : 0);
        response.setCreatedAt(persistedTask.getCreatedAt());
        response.setUpdatedAt(persistedTask.getUpdatedAt());

        // 转换原始拆句列表
        List<OriginalSentenceDTO> originalSentenceDTOList = persistedOriginals.stream()
                .map(this::toOriginalSentenceDTO)
                .collect(Collectors.toList());
        response.setOriginalSentenceList(originalSentenceDTOList);

        return response;
    }

    @Override
    public TaskDetailDTO getTaskDetail(Long taskId) {
        // 参数校验
        ValidationUtil.notNull(taskId, "任务ID不能为空");
        
        // 查询任务
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        
        // 查询最新的合并记录
        AudioMerge latestMerge = audioMergeMapper.selectLatestByTaskId(taskId);
        
        // 构建TaskDetailDTO
        TaskDetailDTO detailDTO = new TaskDetailDTO();
        detailDTO.setTaskId(task.getTaskId());
        detailDTO.setContent(task.getContent());
        detailDTO.setCharCount(task.getCharCount());
        detailDTO.setStatus(task.getStatus());
        // audioUrl 和 audioDuration 使用任务的合并音频URL，如果为 null，则设置为空值
        detailDTO.setAudioUrl(task.getMergedAudioUrl() != null ? task.getMergedAudioUrl() : "");
        detailDTO.setAudioDuration(task.getMergedAudioDuration() != null ? task.getMergedAudioDuration() : 0);
        detailDTO.setCreatedAt(task.getCreatedAt());
        detailDTO.setUpdatedAt(task.getUpdatedAt());
        
        // 设置mergeId（如果有合并记录）
        if (latestMerge != null) {
            detailDTO.setMergeId(latestMerge.getMergeId());
        }
        
        return detailDTO;
    }

    /**
     * 将 OriginalSentence 转换为 OriginalSentenceDTO
     */
    private OriginalSentenceDTO toOriginalSentenceDTO(OriginalSentence original) {
        OriginalSentenceDTO dto = new OriginalSentenceDTO();
        dto.setOriginalSentenceId(original.getOriginalSentenceId());
        dto.setTaskId(original.getTaskId());
        dto.setContent(original.getContent());
        dto.setCharCount(original.getCharCount());
        dto.setSequence(original.getSequence());
        dto.setCreatedAt(original.getCreatedAt());
        return dto;
    }
}

