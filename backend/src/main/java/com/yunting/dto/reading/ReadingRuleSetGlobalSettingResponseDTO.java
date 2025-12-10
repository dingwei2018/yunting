package com.yunting.dto.reading;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 开关全局阅读规范响应DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReadingRuleSetGlobalSettingResponseDTO {
    /**
     * 过滤的断句列表（匹配到该规则的断句）
     */
    private List<FilteredBreakingSentenceDTO> filteredSentences;
    
    /**
     * 过滤的断句总数
     */
    private Integer total;

    public List<FilteredBreakingSentenceDTO> getFilteredSentences() {
        return filteredSentences;
    }

    public void setFilteredSentences(List<FilteredBreakingSentenceDTO> filteredSentences) {
        this.filteredSentences = filteredSentences;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    /**
     * 过滤的断句信息
     */
    public static class FilteredBreakingSentenceDTO {
        private Long breakingSentenceId;
        private Long taskId;
        private Integer sequence;
        private String content;

        public Long getBreakingSentenceId() {
            return breakingSentenceId;
        }

        public void setBreakingSentenceId(Long breakingSentenceId) {
            this.breakingSentenceId = breakingSentenceId;
        }

        public Long getTaskId() {
            return taskId;
        }

        public void setTaskId(Long taskId) {
            this.taskId = taskId;
        }

        public Integer getSequence() {
            return sequence;
        }

        public void setSequence(Integer sequence) {
            this.sequence = sequence;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
