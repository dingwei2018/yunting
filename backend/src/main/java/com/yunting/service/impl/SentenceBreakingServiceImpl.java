package com.yunting.service.impl;

import com.yunting.dto.standard.SentenceBreakingSettingRequest;
import com.yunting.dto.standard.SentenceBreakingSettingResponseDTO;
import com.yunting.dto.standard.SentenceBreakingStandardDTO;
import com.yunting.dto.standard.SentenceBreakingStandardListDTO;
import com.yunting.exception.BusinessException;
import com.yunting.mapper.TaskMapper;
import com.yunting.model.Task;
import com.yunting.service.SentenceBreakingService;
import com.yunting.util.ValidationUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SentenceBreakingServiceImpl implements SentenceBreakingService {

    private final TaskMapper taskMapper;

    public SentenceBreakingServiceImpl(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Override
    public SentenceBreakingStandardListDTO getSentenceBreakingStandards() {
        SentenceBreakingStandardListDTO dto = new SentenceBreakingStandardListDTO();
        dto.setStandards(List.of(
                createStandard(1, "大符号断句", "根据标点符号进行断句（。！？；等）", "punctuation"),
                createStandard(2, "N个字符断句", "根据指定字符数进行断句", "char_count")
        ));
        return dto;
    }

    @Override
    public SentenceBreakingSettingResponseDTO saveSentenceBreakingSetting(Long taskId, SentenceBreakingSettingRequest request) {
        ValidationUtil.notNull(taskId, "taskid不能为空");
        ValidationUtil.notNull(request, "请求体不能为空");
        ValidationUtil.notNull(request.getBreakingStandardId(), "breaking_standard_id不能为空");
        if (request.getBreakingStandardId() != 1 && request.getBreakingStandardId() != 2) {
            throw new BusinessException(10400, "断句标准不存在");
        }
        if (request.getBreakingStandardId() == 2) {
            ValidationUtil.notNull(request.getCharCount(), "char_count为必填");
            if (request.getCharCount() <= 0) {
                throw new BusinessException(10400, "char_count需为正数");
            }
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(10404, "任务不存在");
        }
        taskMapper.updateBreakingStandard(taskId, request.getBreakingStandardId(), request.getCharCount());
        SentenceBreakingSettingResponseDTO responseDTO = new SentenceBreakingSettingResponseDTO();
        responseDTO.setTaskId(taskId);
        responseDTO.setBreakingStandardId(request.getBreakingStandardId());
        responseDTO.setCharCount(request.getCharCount());
        return responseDTO;
    }

    private SentenceBreakingStandardDTO createStandard(int id, String name, String description, String type) {
        SentenceBreakingStandardDTO dto = new SentenceBreakingStandardDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setDescription(description);
        dto.setType(type);
        return dto;
    }
}


