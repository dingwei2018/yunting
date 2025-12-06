package com.yunting.mapper;

import com.yunting.model.ProsodySetting;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProsodySettingMapper {

    List<ProsodySetting> selectByBreakingSentenceId(@Param("breakingSentenceId") Long breakingSentenceId);

    List<ProsodySetting> selectByBreakingSentenceIds(@Param("ids") List<Long> breakingSentenceIds);

    int deleteByBreakingSentenceId(@Param("breakingSentenceId") Long breakingSentenceId);

    int insertBatch(@Param("list") List<ProsodySetting> list);
}

