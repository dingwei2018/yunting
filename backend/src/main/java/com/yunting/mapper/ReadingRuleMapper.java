package com.yunting.mapper;

import com.yunting.model.ReadingRule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReadingRuleMapper {

    int insert(ReadingRule rule);

    int update(ReadingRule rule);

    ReadingRule selectById(@Param("ruleId") Long ruleId);

    List<ReadingRule> selectList();

    List<Long> selectExistingIds(@Param("ids") List<Long> ids);

    ReadingRule selectByRuleTypeAndPattern(@Param("ruleType") String ruleType, @Param("pattern") String pattern);

    List<ReadingRule> selectPage(@Param("taskId") Long taskId, @Param("ruleType") String ruleType, @Param("offset") int offset, @Param("limit") int limit);

    long count(@Param("taskId") Long taskId, @Param("ruleType") String ruleType);
}


