package com.yunting.service;

import com.yunting.model.ReadingRule;

import java.util.List;

/**
 * 阅读规则汇总服务
 * 负责汇总断句需要的阅读规则
 */
public interface ReadingRuleAggregationService {

    /**
     * 汇总断句需要的阅读规则
     * 规则：
     * 1. 全局规则（reading_rules表中is_open=1的规则），如果打开，且在reading_rule_applications表中没有关于此次任务级关闭的记录（type=1且is_open=0），则汇入
     * 2. 断句规则：如果在reading_rule_applications中关于此断句有打开的记录（type=2且is_open=1），也汇入
     * 3. 冲突处理：如果全局规则与断句规则有冲突，以断句规则优先
     * 
     * @param breakingSentenceId 断句ID
     * @return 阅读规则列表（已去重，断句规则优先）
     */
    List<ReadingRule> aggregateReadingRules(Long breakingSentenceId);
}
