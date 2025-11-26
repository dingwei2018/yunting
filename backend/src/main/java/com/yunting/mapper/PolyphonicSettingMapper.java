package com.yunting.mapper;

import com.yunting.model.PolyphonicSetting;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PolyphonicSettingMapper {

    List<PolyphonicSetting> selectByBreakingSentenceId(@Param("breakingSentenceId") Long breakingSentenceId);

    List<PolyphonicSetting> selectByBreakingSentenceIds(@Param("ids") List<Long> breakingSentenceIds);
}


