package com.yunting.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TaskCreateRequest {

    @NotBlank(message = "文本内容不能为空")
    @Size(max = 10000, message = "文本内容不能超过10000字")
    private String content;

    /**
     * 自定义拆句符号，多个字符直接拼接，例如 "。！？"
     * 可选；为空时使用默认符号集
     */
    private String delimiters;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDelimiters() {
        return delimiters;
    }

    public void setDelimiters(String delimiters) {
        this.delimiters = delimiters;
    }
}

