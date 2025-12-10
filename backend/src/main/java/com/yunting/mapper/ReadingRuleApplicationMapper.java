package com.yunting.mapper;

import com.yunting.model.ReadingRuleApplication;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ReadingRuleApplicationMapper {

    int deleteByBreakingSentenceId(@Param("breakingSentenceId") Long breakingSentenceId);

    int insertBatch(@Param("list") List<ReadingRuleApplication> list);

    /**
     * 批量插入或更新应用记录（如果已存在则更新isOpen，不存在则插入）
     *
     * @param list 应用记录列表
     * @return 插入/更新的记录数
     */
    int insertOrUpdateBatch(@Param("list") List<ReadingRuleApplication> list);

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

    /**
     * 批量更新指定任务下指定规则的isOpen状态
     *
     * @param ruleId 规则ID
     * @param taskId 任务ID
     * @param isOpen 是否开启
     * @return 更新的记录数
     */
    int updateIsOpenByRuleIdAndTaskId(@Param("ruleId") Long ruleId, @Param("taskId") Long taskId, @Param("isOpen") Boolean isOpen);

    /**
     * 查询指定任务下指定规则的应用记录（包含isOpen状态）
     *
     * @param ruleId 规则ID
     * @param taskId 任务ID
     * @return 应用记录列表
     */
    List<ReadingRuleApplication> selectByRuleIdAndTaskId(@Param("ruleId") Long ruleId, @Param("taskId") Long taskId);

    /**
     * 查询指定任务下所有规则的isOpen状态
     * 返回List<Map>，每个Map包含rule_id和is_open字段
     *
     * @param taskId 任务ID
     * @return List<Map>，每个Map包含rule_id和is_open
     */
    List<Map<String, Object>> selectRuleIdsWithIsOpenByTaskId(@Param("taskId") Long taskId);

    /**
     * 查询所有已应用规则的isOpen状态（用于全局查询）
     * 返回List<Map>，每个Map包含rule_id和is_open字段
     *
     * @return List<Map>，每个Map包含rule_id和is_open
     */
    List<Map<String, Object>> selectAllRuleIdsWithIsOpen();
}


