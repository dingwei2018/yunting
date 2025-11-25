package com.yunting.service;

import com.yunting.dto.task.TaskCreateRequest;
import com.yunting.dto.task.TaskDetailDTO;
import com.yunting.dto.task.TaskListResponseDTO;

public interface TaskService {

    TaskDetailDTO createTask(TaskCreateRequest request);

    TaskDetailDTO getTaskDetail(Long taskId);

    TaskListResponseDTO listTasks(Integer page, Integer pageSize, Integer status);
}

