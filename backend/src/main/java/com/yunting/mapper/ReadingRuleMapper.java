package com.yunting.mapper;

import com.yunting.model.ReadingRule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReadingRuleMapper {

    int insert(ReadingRule rule);

    ReadingRule selectById(@Param("ruleId") Long ruleId);

    List<ReadingRule> selectList(@Param("taskId") Long taskId, @Param("scope") Integer scope);

    List<Long> selectExistingIds(@Param("ids") List<Long> ids);
}


