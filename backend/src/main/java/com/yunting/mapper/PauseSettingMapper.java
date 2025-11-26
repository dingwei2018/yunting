package com.yunting.mapper;

import com.yunting.model.PauseSetting;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PauseSettingMapper {

    List<PauseSetting> selectByBreakingSentenceId(@Param("breakingSentenceId") Long breakingSentenceId);

    List<PauseSetting> selectByBreakingSentenceIds(@Param("ids") List<Long> breakingSentenceIds);
}


