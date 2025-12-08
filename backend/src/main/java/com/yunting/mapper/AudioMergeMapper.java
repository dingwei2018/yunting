package com.yunting.mapper;

import com.yunting.model.AudioMerge;
import org.apache.ibatis.annotations.Param;

public interface AudioMergeMapper {

    int insert(AudioMerge audioMerge);

    AudioMerge selectById(@Param("mergeId") Long mergeId);

    int updateById(AudioMerge audioMerge);

    /**
     * 根据任务ID查询最新的合并记录
     */
    AudioMerge selectLatestByTaskId(@Param("taskId") Long taskId);
}


