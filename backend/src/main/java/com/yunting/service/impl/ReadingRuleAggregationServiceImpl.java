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

        // 2. 分别查询断句的应用规则和断句所属任务的规则
        // 2.1 查询断句级规则（type=2）
        List<Map<String, Object>> breakingSentenceRecords = 
                readingRuleApplicationMapper.selectByFromIdAndType(breakingSentenceId, 2);
        logger.debug("查询断句级规则，breakingSentenceId: {}, 记录数: {}", breakingSentenceId, breakingSentenceRecords.size());

        // 2.2 查询任务级规则（type=1）
        List<Map<String, Object>> taskRecords = 
                readingRuleApplicationMapper.selectByFromIdAndType(taskId, 1);
        logger.debug("查询任务级规则，taskId: {}, 记录数: {}", taskId, taskRecords.size());

        // 3. 合并规则，有冲突的以断句规则为准
        // 使用rule_id作为key，断句规则会覆盖任务规则
        Map<Long, Boolean> mergedRuleMap = new HashMap<>();
        
        // 3.1 先添加任务级规则
        for (Map<String, Object> record : taskRecords) {
            Long ruleId = ((Number) record.get("rule_id")).longValue();
            Boolean isOpen = convertToBoolean(record.get("is_open"));
            mergedRuleMap.put(ruleId, isOpen);
        }
        
        // 3.2 再添加断句级规则（会覆盖任务级规则）
        for (Map<String, Object> record : breakingSentenceRecords) {
            Long ruleId = ((Number) record.get("rule_id")).longValue();
            Boolean isOpen = convertToBoolean(record.get("is_open"));
            mergedRuleMap.put(ruleId, isOpen);
        }

        // 4. 提取出需要关闭的规则
        Set<Long> closedRuleIds = mergedRuleMap.entrySet().stream()
                .filter(entry -> !entry.getValue()) // is_open = false
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        logger.debug("需要关闭的规则ID: {}", closedRuleIds);

        // 5. 查询规则表中的所有规则
        List<ReadingRule> allRules = readingRuleMapper.selectList();
        logger.debug("规则表中的总规则数: {}", allRules.size());

        // 6. 返回规则表中剩余的规则（排除需要关闭的规则）
        List<ReadingRule> result = allRules.stream()
                .filter(rule -> !closedRuleIds.contains(rule.getRuleId()))
                .collect(Collectors.toList());
        
        logger.info("汇总断句需要的阅读规则完成，breakingSentenceId: {}, taskId: {}, 返回规则数: {}", 
                breakingSentenceId, taskId, result.size());
        
        return result;
    }

    /**
     * 将数据库返回的值转换为Boolean类型
     * 支持Boolean、Number、String等类型
     * 
     * @param value 数据库返回的值
     * @return Boolean值，默认为true
     */
    private Boolean convertToBoolean(Object value) {
        if (value == null) {
            return true; // 默认值为true
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            return "1".equals(str) || "true".equalsIgnoreCase(str);
        }
        return true; // 默认值为true
    }
}
