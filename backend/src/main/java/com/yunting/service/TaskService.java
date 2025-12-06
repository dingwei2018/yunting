package com.yunting.service;

import com.yunting.dto.task.TaskCreateRequest;
import com.yunting.dto.task.TaskCreateResponseDTO;
import com.yunting.dto.task.TaskDetailDTO;

/**
 * 任务服务接口
 */
public interface TaskService {

    /**
     * 创建任务并自动拆句
     * 拆句完成后，每条拆句记录默认会生成一条断句记录
     *
     * @param request 创建任务请求
     * @return 创建的任务信息
     */
    TaskCreateResponseDTO createTask(TaskCreateRequest request);

    /**
     * 获取任务详情
     * 返回任务详情以及任务下的拆句内容
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    TaskDetailDTO getTaskDetail(Long taskId);
}

