package com.yunting.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReadingRuleMapper {

    List<Long> selectExistingIds(@Param("ids") List<Long> ids);
}


