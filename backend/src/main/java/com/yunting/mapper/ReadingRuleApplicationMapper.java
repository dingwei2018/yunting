package com.yunting.mapper;

import com.yunting.model.ReadingRuleApplication;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReadingRuleApplicationMapper {

    int deleteByBreakingSentenceId(@Param("breakingSentenceId") Long breakingSentenceId);

    int insertBatch(@Param("list") List<ReadingRuleApplication> list);

    int deleteByRuleId(@Param("ruleId") Long ruleId);
}


