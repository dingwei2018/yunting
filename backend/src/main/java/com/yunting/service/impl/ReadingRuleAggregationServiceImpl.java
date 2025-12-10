package com.yunting.service.impl;

import com.yunting.mapper.BreakingSentenceMapper;
import com.yunting.mapper.ReadingRuleApplicationMapper;
import com.yunting.mapper.ReadingRuleMapper;
import com.yunting.model.BreakingSentence;
import com.yunting.model.ReadingRule;
import com.yunting.service.ReadingRuleAggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 阅读规则汇总服务实现
 */
@Service
public class ReadingRuleAggregationServiceImpl implements ReadingRuleAggregationService {

    private static final Logger logger = LoggerFactory.getLogger(ReadingRuleAggregationServiceImpl.class);

    private final BreakingSentenceMapper breakingSentenceMapper;
    private final ReadingRuleMapper readingRuleMapper;
    private final ReadingRuleApplicationMapper readingRuleApplicationMapper;

    public ReadingRuleAggregationServiceImpl(BreakingSentenceMapper breakingSentenceMapper,
                                            ReadingRuleMapper readingRuleMapper,
                                            ReadingRuleApplicationMapper readingRuleApplicationMapper) {
        this.breakingSentenceMapper = breakingSentenceMapper;
        this.readingRuleMapper = readingRuleMapper;
        this.readingRuleApplicationMapper = readingRuleApplicationMapper;
    }

    @Override
    public List<ReadingRule> aggregateReadingRules(Long breakingSentenceId) {
        // 1. 查询断句信息，获取taskId
        BreakingSentence sentence = breakingSentenceMapper.selectById(breakingSentenceId);
        if (sentence == null) {
            logger.warn("断句不存在，breakingSentenceId: {}", breakingSentenceId);
            return Collections.emptyList();
        }

        Long taskId = sentence.getTaskId();

        // 2. 查询全局规则及任务级关闭记录
        List<Map<String, Object>> globalRulesWithClosed = 
                readingRuleApplicationMapper.selectGlobalRulesWithTaskClosedRules(taskId);

        // 3. 查询断句级规则（已开启的）
        List<Long> breakingSentenceRuleIds = 
                readingRuleApplicationMapper.selectBreakingSentenceRules(breakingSentenceId);

        // 4. 构建规则Map，使用pattern作为key，断句规则优先
        Map<String, ReadingRule> ruleMap = new LinkedHashMap<>();

        // 4.1 先添加全局规则（排除被任务级关闭的）
        for (Map<String, Object> ruleData : globalRulesWithClosed) {
            Long ruleId = ((Number) ruleData.get("rule_id")).longValue();
            Integer taskClosed = (Integer) ruleData.get("task_closed");
            
            // 如果被任务级关闭，跳过
            if (taskClosed != null && taskClosed == 1) {
                continue;
            }

            // 查询规则详情
            ReadingRule rule = readingRuleMapper.selectById(ruleId);
            if (rule != null && rule.getPattern() != null) {
                // 使用pattern作为key，如果已存在（断句规则），则跳过（断句规则优先）
                if (!ruleMap.containsKey(rule.getPattern())) {
                    ruleMap.put(rule.getPattern(), rule);
                }
            }
        }

        // 4.2 再添加断句级规则（会覆盖全局规则）
        for (Long ruleId : breakingSentenceRuleIds) {
            ReadingRule rule = readingRuleMapper.selectById(ruleId);
            if (rule != null && rule.getPattern() != null) {
                // 断句规则覆盖全局规则
                ruleMap.put(rule.getPattern(), rule);
            }
        }

        // 5. 返回规则列表
        return new ArrayList<>(ruleMap.values());
    }
}
