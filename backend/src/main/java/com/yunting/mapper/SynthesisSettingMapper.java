package com.yunting.mapper;

import com.yunting.model.SynthesisSetting;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SynthesisSettingMapper {

    SynthesisSetting selectByBreakingSentenceId(@Param("breakingSentenceId") Long breakingSentenceId);

    List<SynthesisSetting> selectByBreakingSentenceIds(@Param("ids") List<Long> breakingSentenceIds);

    int upsert(SynthesisSetting setting);
}


