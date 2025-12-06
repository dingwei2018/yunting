package com.yunting.dto.task;

import com.yunting.constant.DelimiterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 创建任务请求DTO
 */
public class TaskCreateRequest {

    @NotBlank(message = "文本内容不能为空")
    @Size(max = 10000, message = "文本内容不能超过10000字")
    private String content;

    /**
     * 自定义拆句符号类型数组
     * 可选；为空时使用默认符号集
     * 1：中文句号（。）
     * 2：中文叹号（！）
     * 3：中文问号（？）
     * 4：中文省略号（…）
     */
    private List<Integer> delimiterList;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Integer> getDelimiters() {
        return delimiterList;
    }

    public void setDelimiters(List<Integer> delimiters) {
        this.delimiterList = delimiters;
    }

    /**
     * 将拆句符号类型数组转换为字符串格式（用于 SentenceSplitter）
     * 如果 delimiterList 为空或 null，返回 null
     * 
     * @return 拆句符号字符串，例如 "。！？"
     */
    public String getDelimitersAsString() {
        if (delimiterList == null || delimiterList.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Integer type : delimiterList) {
            if (type != null) {
                Character ch = DelimiterType.getCharByType(type);
                if (ch != null) {
                    sb.append(ch);
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}

