package com.yunting.mapper;

import com.yunting.model.BreakingSentence;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BreakingSentenceMapper {

    int insert(BreakingSentence sentence);

    int insertBatch(@Param("list") List<BreakingSentence> sentences);

    List<BreakingSentence> selectByTaskId(@Param("taskId") Long taskId);

    int countByTaskId(@Param("taskId") Long taskId);

    List<BreakingSentence> selectPageByTaskId(@Param("taskId") Long taskId,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    BreakingSentence selectById(@Param("breakingSentenceId") Long breakingSentenceId);

    int deleteById(@Param("breakingSentenceId") Long breakingSentenceId);

    int decrementSequenceAfter(@Param("taskId") Long taskId, @Param("sequence") int sequence);

    List<BreakingSentence> selectByIds(@Param("ids") List<Long> breakingSentenceIds);

    List<BreakingSentence> selectPendingByTaskId(@Param("taskId") Long taskId);

    List<BreakingSentence> selectByOriginalSentenceId(@Param("originalSentenceId") Long originalSentenceId);

    int updateSynthesisInfo(@Param("breakingSentenceId") Long breakingSentenceId,
                            @Param("status") int status,
                            @Param("audioUrl") String audioUrl,
                            @Param("audioDuration") Integer audioDuration);

    int resetSynthesisStatus(@Param("breakingSentenceId") Long breakingSentenceId);

    int updateContent(@Param("breakingSentenceId") Long breakingSentenceId,
                      @Param("content") String content,
                      @Param("charCount") Integer charCount);

    int updateSsml(@Param("breakingSentenceId") Long breakingSentenceId,
                   @Param("ssml") String ssml);

    BreakingSentence selectByJobId(@Param("jobId") String jobId);

    int updateJobId(@Param("breakingSentenceId") Long breakingSentenceId,
                    @Param("jobId") String jobId);

    int updateSequence(@Param("breakingSentenceId") Long breakingSentenceId,
                       @Param("sequence") Integer sequence);
}

