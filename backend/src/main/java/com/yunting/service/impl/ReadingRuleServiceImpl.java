package com.yunting.service.impl;

import com.yunting.dto.reading.ReadingRuleApplyResponseDTO;
import com.yunting.dto.reading.ReadingRuleCreateRequest;
import com.yunting.dto.reading.ReadingRuleDTO;
import com.yunting.dto.reading.ReadingRuleListResponseDTO;
import com.yunting.exception.BusinessException;
import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.mapper.ReadingRuleApplicationMapper;
import com.yunting.mapper.ReadingRuleMapper;
import com.yunting.mapper.TaskMapper;
import com.yunting.model.BreakingSentence;
import com.yunting.model.ReadingRule;
import com.yunting.model.ReadingRuleApplication;
import com.yunting.model.Task;
import com.yunting.service.ReadingRuleService;
import com.yunting.util.ValidationUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReadingRuleServiceImpl implements ReadingRuleService {

    private static final int SCOPE_GLOBAL = 1;
    private static final int SCOPE_TASK = 2;

    private final ReadingRuleMapper readingRuleMapper;
    private final TaskMapper taskMapper;
    private final BreakingSentenceMapper breakingSentenceMapper;
    private final ReadingRuleApplicationMapper readingRuleApplicationMapper;

    public ReadingRuleServiceImpl(ReadingRuleMapper readingRuleMapper,
                                  TaskMapper taskMapper,
                                  BreakingSentenceMapper breakingSentenceMapper,
                                  ReadingRuleApplicationMapper readingRuleApplicationMapper) {
        this.readingRuleMapper = readingRuleMapper;
        this.taskMapper = taskMapper;
        this.breakingSentenceMapper = breakingSentenceMapper;
        this.readingRuleApplicationMapper = readingRuleApplicationMapper;
    }

    @Override
    public ReadingRuleDTO createReadingRule(ReadingRuleCreateRequest request) {
        ValidationUtil.notNull(request, "请求参数不能为空");
        validateCreateRequest(request);
        Task task = null;
        if (SCOPE_TASK == request.getScope()) {
            task = taskMapper.selectById(request.getTaskId());
            if (task == null) {
                throw new BusinessException(10404, "任务不存在");
            }
        }
        ReadingRule rule = new ReadingRule();
        rule.setTaskId(SCOPE_TASK == request.getScope() ? request.getTaskId() : null);
        rule.setPattern(request.getPattern());
        rule.setRuleType(request.getRuleType());
        rule.setRuleValue(request.getRuleValue());
        rule.setScope(request.getScope());
        readingRuleMapper.insert(rule);
        return toDTO(rule);
    }

    @Override
    public ReadingRuleListResponseDTO getReadingRules(Long taskId, Integer scope) {
        List<ReadingRule> rules = readingRuleMapper.selectList(taskId, scope);
        ReadingRuleListResponseDTO responseDTO = new ReadingRuleListResponseDTO();
        responseDTO.setList(rules.stream().map(this::toDTO).collect(Collectors.toList()));
        return responseDTO;
    }

    @Override
    public ReadingRuleApplyResponseDTO applyReadingRule(Long ruleId, Long taskId) {
        ValidationUtil.notNull(ruleId, "ruleid不能为空");
        ValidationUtil.notNull(taskId, "taskid不能为空");
        ReadingRule rule = readingRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BusinessException(10404, "阅读规范不存在");
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(10404, "任务不存在");
        }
        if (SCOPE_TASK == rule.getScope() && (rule.getTaskId() == null || !rule.getTaskId().equals(taskId))) {
            throw new BusinessException(10400, "阅读规范不属于当前任务");
        }
        List<BreakingSentence> sentences = breakingSentenceMapper.selectByTaskId(taskId);
        if (CollectionUtils.isEmpty(sentences)) {
            throw new BusinessException(10404, "任务暂无断句");
        }
        readingRuleApplicationMapper.deleteByRuleId(ruleId);
        List<ReadingRuleApplication> applications = sentences.stream()
                .map(sentence -> {
                    ReadingRuleApplication application = new ReadingRuleApplication();
                    application.setRuleId(ruleId);
                    application.setBreakingSentenceId(sentence.getBreakingSentenceId());
                    return application;
                }).collect(Collectors.toList());
        readingRuleApplicationMapper.insertBatch(applications);
        ReadingRuleApplyResponseDTO responseDTO = new ReadingRuleApplyResponseDTO();
        responseDTO.setRuleId(ruleId);
        responseDTO.setTaskId(taskId);
        responseDTO.setAppliedCount(applications.size());
        return responseDTO;
    }

    private void validateCreateRequest(ReadingRuleCreateRequest request) {
        if (!StringUtils.hasText(request.getPattern())) {
            throw new BusinessException(10400, "pattern不能为空");
        }
        if (!StringUtils.hasText(request.getRuleType())) {
            throw new BusinessException(10400, "rule_type不能为空");
        }
        if (!StringUtils.hasText(request.getRuleValue())) {
            throw new BusinessException(10400, "rule_value不能为空");
        }
        if (request.getScope() == null || (request.getScope() != SCOPE_GLOBAL && request.getScope() != SCOPE_TASK)) {
            throw new BusinessException(10400, "scope参数无效");
        }
        if (request.getScope() == SCOPE_TASK && request.getTaskId() == null) {
            throw new BusinessException(10400, "scope为2时task_id必填");
        }
    }

    private ReadingRuleDTO toDTO(ReadingRule rule) {
        ReadingRuleDTO dto = new ReadingRuleDTO();
        dto.setRuleId(rule.getRuleId());
        dto.setTaskId(rule.getTaskId());
        dto.setPattern(rule.getPattern());
        dto.setRuleType(rule.getRuleType());
        dto.setRuleValue(rule.getRuleValue());
        dto.setScope(rule.getScope());
        dto.setCreatedAt(rule.getCreatedAt());
        return dto;
    }
}


