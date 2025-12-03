package com.yunting.mapper;

import com.yunting.model.AudioMerge;
import org.apache.ibatis.annotations.Param;

public interface AudioMergeMapper {

    int insert(AudioMerge audioMerge);

    AudioMerge selectById(@Param("mergeId") Long mergeId);

    int updateById(AudioMerge audioMerge);
}


