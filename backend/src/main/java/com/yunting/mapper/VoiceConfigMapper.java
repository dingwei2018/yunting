package com.yunting.mapper;

import com.yunting.model.VoiceConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface VoiceConfigMapper {

    List<VoiceConfig> selectList(@Param("isRecommended") Integer isRecommended,
                                 @Param("language") String language);
}


