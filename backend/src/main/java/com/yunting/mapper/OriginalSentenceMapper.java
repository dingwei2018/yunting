package com.yunting.mapper;

import com.yunting.model.OriginalSentence;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OriginalSentenceMapper {

    int insertBatch(@Param("list") List<OriginalSentence> sentences);

    List<OriginalSentence> selectByTaskId(@Param("taskId") Long taskId);

    List<OriginalSentence> selectPageByTaskId(@Param("taskId") Long taskId,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    long countByTaskId(@Param("taskId") Long taskId);
}

