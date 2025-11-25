package com.yunting.mapper;

import com.yunting.model.BreakingSentence;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BreakingSentenceMapper {

    int insertBatch(@Param("list") List<BreakingSentence> sentences);

    List<BreakingSentence> selectByTaskId(@Param("taskId") Long taskId);

    int countByTaskId(@Param("taskId") Long taskId);
}

