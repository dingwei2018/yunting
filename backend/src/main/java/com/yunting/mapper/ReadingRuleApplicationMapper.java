package com.yunting.mapper;

import com.yunting.model.ReadingRuleApplication;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReadingRuleApplicationMapper {

    int deleteByBreakingSentenceId(@Param("breakingSentenceId") Long breakingSentenceId);

    int insertBatch(@Param("list") List<ReadingRuleApplication> list);

    int deleteByRuleId(@Param("ruleId") Long ruleId);

    /**
     * 删除指定任务下指定规则的应用记录
     *
     * @param ruleId 规则ID
     * @param taskId 任务ID
     * @return 删除的记录数
     */
    int deleteByRuleIdAndTaskId(@Param("ruleId") Long ruleId, @Param("taskId") Long taskId);

    List<Long> selectRuleIdsByTaskId(@Param("taskId") Long taskId);
}


