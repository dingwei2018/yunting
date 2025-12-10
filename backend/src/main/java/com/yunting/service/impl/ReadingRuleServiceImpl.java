package com.yunting.service.impl;

import com.yunting.constant.ReadingRuleApplicationType;
import com.yunting.constant.ReadingRuleType;
import com.yunting.dto.reading.MatchingFieldDTO;
import com.yunting.dto.reading.MatchingFieldListResponseDTO;
import com.yunting.dto.reading.ReadingRuleApplyResponseDTO;
import com.yunting.dto.reading.ReadingRuleCreateRequest;
import com.yunting.dto.reading.ReadingRuleCreateResponseDTO;
import com.yunting.dto.reading.ReadingRuleDTO;
import com.yunting.dto.reading.ReadingRuleListItemDTO;
import com.yunting.dto.reading.ReadingRuleListPageResponseDTO;
import com.yunting.dto.reading.ReadingRuleListResponseDTO;
import com.yunting.dto.reading.ReadingRuleSetGlobalSettingRequest;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReadingRuleServiceImpl implements ReadingRuleService {

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
    public ReadingRuleCreateResponseDTO createReadingRule(ReadingRuleCreateRequest request) {
        ValidationUtil.notNull(request, "请求参数不能为空");
        validateCreateRequest(request);
        
        // 检查是否存在相同的 ruleType 和 pattern 的记录
        ReadingRule existingRule = readingRuleMapper.selectByRuleTypeAndPattern(
                request.getRuleType(), request.getPattern());
        if (existingRule != null) {
            throw new BusinessException(10400, "阅读规则已存在");
        }
        
        ReadingRule rule = new ReadingRule();
        rule.setPattern(request.getPattern());
        rule.setRuleType(request.getRuleType());
        rule.setRuleValue(request.getRuleValue());
        
        // 新增阅读规范
        readingRuleMapper.insert(rule);
        
        ReadingRuleCreateResponseDTO responseDTO = new ReadingRuleCreateResponseDTO();
        responseDTO.setRuleId(rule.getRuleId());
        return responseDTO;
    }

    @Override
    public ReadingRuleListResponseDTO getReadingRules() {
        List<ReadingRule> rules = readingRuleMapper.selectList();
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
        List<BreakingSentence> sentences = breakingSentenceMapper.selectByTaskId(taskId);
        if (CollectionUtils.isEmpty(sentences)) {
            throw new BusinessException(10404, "任务暂无断句");
        }
        readingRuleApplicationMapper.deleteByRuleId(ruleId);
        List<ReadingRuleApplication> applications = sentences.stream()
                .map(sentence -> {
                    ReadingRuleApplication application = new ReadingRuleApplication();
                    application.setRuleId(ruleId);
                    application.setFromId(sentence.getBreakingSentenceId());
                    application.setType(ReadingRuleApplicationType.Type.BREAKING_SENTENCE);
                    return application;
                }).collect(Collectors.toList());
        readingRuleApplicationMapper.insertBatch(applications);
        ReadingRuleApplyResponseDTO responseDTO = new ReadingRuleApplyResponseDTO();
        responseDTO.setRuleId(ruleId);
        responseDTO.setTaskId(taskId);
        responseDTO.setAppliedCount(applications.size());
        return responseDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String setGlobalSetting(ReadingRuleSetGlobalSettingRequest request) {
        ValidationUtil.notNull(request, "请求参数不能为空");
        ValidationUtil.notNull(request.getTaskId(), "taskId不能为空");
        ValidationUtil.notNull(request.getRuleId(), "ruleId不能为空");
        ValidationUtil.notNull(request.getIsOpen(), "isOpen不能为空");

        // 验证阅读规范是否存在
        ReadingRule rule = readingRuleMapper.selectById(request.getRuleId());
        if (rule == null) {
            throw new BusinessException(10404, "阅读规范不存在");
        }

        // 验证任务是否存在
        Task task = taskMapper.selectById(request.getTaskId());
        if (task == null) {
            throw new BusinessException(10404, "任务不存在");
        }

        if (request.getIsOpen()) {
            // 如果isOpen为true，为任务的所有断句创建阅读规范应用记录
            List<BreakingSentence> sentences = breakingSentenceMapper.selectByTaskId(request.getTaskId());
            if (CollectionUtils.isEmpty(sentences)) {
                throw new BusinessException(10404, "任务暂无断句");
            }

            // 先删除该规则在该任务下的所有应用记录（只删除当前任务下的，不影响其他任务）
            readingRuleApplicationMapper.deleteByRuleIdAndTaskId(request.getRuleId(), request.getTaskId());

            // 为所有断句创建应用记录
            List<ReadingRuleApplication> applications = sentences.stream()
                    .map(sentence -> {
                        ReadingRuleApplication application = new ReadingRuleApplication();
                        application.setRuleId(request.getRuleId());
                        application.setFromId(sentence.getBreakingSentenceId());
                        application.setType(ReadingRuleApplicationType.Type.BREAKING_SENTENCE);
                        return application;
                    })
                    .collect(Collectors.toList());

            if (!applications.isEmpty()) {
                readingRuleApplicationMapper.insertBatch(applications);
            }
        } else {
            // 如果isOpen为false，删除该规则在该任务下的所有应用记录（只删除当前任务下的）
            readingRuleApplicationMapper.deleteByRuleIdAndTaskId(request.getRuleId(), request.getTaskId());
        }

        return "设置成功";
    }

    @Override
    public ReadingRuleListPageResponseDTO getReadingRuleList(Long taskId, Integer ruleType, Integer page, Integer pageSize) {
        // 参数验证和默认值处理
        int currentPage = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? 20 : pageSize;
        int offset = (currentPage - 1) * size;

        // 查询总数
        long total = readingRuleMapper.count(taskId, ruleType);

        // 查询分页数据
        List<ReadingRule> rules;
        if (total == 0) {
            rules = Collections.emptyList();
        } else {
            rules = readingRuleMapper.selectPage(taskId, ruleType, offset, size);
        }

        // 查询isOpen状态（如果提供了taskId）
        List<Long> appliedRuleIds = Collections.emptyList();
        if (taskId != null) {
            appliedRuleIds = readingRuleApplicationMapper.selectRuleIdsByTaskId(taskId);
        }

        // 转换为DTO
        final List<Long> finalAppliedRuleIds = appliedRuleIds;
        List<ReadingRuleListItemDTO> list = rules.stream()
                .map(rule -> {
                    ReadingRuleListItemDTO dto = new ReadingRuleListItemDTO();
                    dto.setRuleId(rule.getRuleId());
                    dto.setPattern(rule.getPattern());
                    dto.setRuleType(rule.getRuleType());
                    dto.setRuleValue(rule.getRuleValue());
                    // 判断isOpen：如果taskId不为null，检查该规则是否在该任务下有应用记录
                    if (taskId != null) {
                        dto.setIsOpen(finalAppliedRuleIds.contains(rule.getRuleId()));
                    } else {
                        // 如果taskId为null，isOpen默认为false（因为无法判断）
                        dto.setIsOpen(false);
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        ReadingRuleListPageResponseDTO response = new ReadingRuleListPageResponseDTO();
        response.setReadingRuleList(list);
        response.setTotal((int) total);
        response.setPage(currentPage);
        response.setPageSize(size);
        return response;
    }

    @Override
    public MatchingFieldListResponseDTO getMatchingFieldListFromText(String text) {
        MatchingFieldListResponseDTO response = new MatchingFieldListResponseDTO();
        
        if (!StringUtils.hasText(text)) {
            response.setTotal(0);
            response.setFieldList(Collections.emptyList());
            return response;
        }

        // 获取所有阅读规范
        List<ReadingRule> rules = readingRuleMapper.selectList();
        
        // 获取所有已应用的规则ID（用于判断isOpen，查询所有应用的规则）
        List<Long> appliedRuleIds = readingRuleApplicationMapper.selectRuleIdsByTaskId(null);
        
        // 遍历所有规则，匹配文本
        List<MatchingFieldDTO> fieldList = new ArrayList<>();
        for (ReadingRule rule : rules) {
            String pattern = rule.getPattern();
            if (!StringUtils.hasText(pattern)) {
                continue;
            }

            // 查找所有匹配位置（支持多个匹配）
            int startIndex = 0;
            while (true) {
                int index = text.indexOf(pattern, startIndex);
                if (index < 0) {
                    break;
                }
                
                MatchingFieldDTO field = new MatchingFieldDTO();
                field.setRuleId(rule.getRuleId());
                field.setLocation(index);
                field.setPattern(pattern);
                // 判断isOpen：检查该规则是否有应用记录（在任何任务下）
                field.setIsOpen(appliedRuleIds.contains(rule.getRuleId()));
                fieldList.add(field);
                
                // 移动到下一个可能的位置
                startIndex = index + 1;
            }
        }

        response.setTotal(fieldList.size());
        response.setFieldList(fieldList);
        return response;
    }

    private void validateCreateRequest(ReadingRuleCreateRequest request) {
        if (!StringUtils.hasText(request.getPattern())) {
            throw new BusinessException(10400, "pattern不能为空");
        }
        if (request.getRuleType() == null) {
            throw new BusinessException(10400, "ruleType不能为空");
        }
        // 使用枚举验证 ruleType 是否有效
        if (!ReadingRuleType.isValid(request.getRuleType())) {
            throw new BusinessException(10400, "ruleType必须是1、2或3");
        }
        if (!StringUtils.hasText(request.getRuleValue())) {
            throw new BusinessException(10400, "rule_value不能为空");
        }
    }

    private ReadingRuleDTO toDTO(ReadingRule rule) {
        ReadingRuleDTO dto = new ReadingRuleDTO();
        dto.setRuleId(rule.getRuleId());
        dto.setPattern(rule.getPattern());
        dto.setRuleType(rule.getRuleType());
        dto.setRuleValue(rule.getRuleValue());
        dto.setCreatedAt(rule.getCreatedAt());
        return dto;
    }
}


