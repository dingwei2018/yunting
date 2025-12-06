package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.task.TaskCreateRequest;
import com.yunting.dto.task.TaskCreateResponseDTO;
import com.yunting.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务管理控制器
 */
@RestController
@RequestMapping("/api/task")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 创建任务
     * 创建任务时会自动进行拆句处理（根据指定的拆句符号拆句）
     * 拆句完成后，每条拆句记录默认会生成一条断句记录
     *
     * @param request 创建任务请求
     * @return 创建的任务信息
     */
    @PostMapping("/create")
    public ApiResponse<TaskCreateResponseDTO> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskCreateResponseDTO data = taskService.createTask(request);
        return ResponseUtil.success(data);
    }
}

