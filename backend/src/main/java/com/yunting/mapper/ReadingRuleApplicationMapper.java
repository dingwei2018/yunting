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

    /**
     * 根据规则ID、来源ID和类型精确删除应用记录
     *
     * @param ruleId 规则ID
     * @param fromId 来源ID（taskId或breakingSentenceId）
     * @param type 类型（1-任务级，2-断句级）
     * @return 删除的记录数
     */
    int deleteByRuleIdAndFromIdAndType(@Param("ruleId") Long ruleId, @Param("fromId") Long fromId, @Param("type") Integer type);

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

    /**
     * 查询全局规则及任务级关闭记录
     * 返回全局规则列表，同时标记哪些规则在该任务中被关闭
     *
     * @param taskId 任务ID
     * @return List<Map>，每个Map包含规则信息和task_closed标记
     */
    List<Map<String, Object>> selectGlobalRulesWithTaskClosedRules(@Param("taskId") Long taskId);

    /**
     * 查询断句级规则（已开启的）
     *
     * @param breakingSentenceId 断句ID
     * @return 规则ID列表
     */
    List<Long> selectBreakingSentenceRules(@Param("breakingSentenceId") Long breakingSentenceId);

    /**
     * 查询所有进行中的断句数量（synthesis_status=1）
     *
     * @return 进行中的断句数量
     */
    int selectProcessingBreakingSentencesCount();
}


