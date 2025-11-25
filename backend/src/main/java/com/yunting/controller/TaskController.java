package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.task.TaskCreateRequest;
import com.yunting.dto.task.TaskDetailDTO;
import com.yunting.dto.task.TaskListResponseDTO;
import com.yunting.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ApiResponse<TaskDetailDTO> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskDetailDTO task = taskService.createTask(request);
        return ResponseUtil.success(task);
    }

    @GetMapping("/taskid={taskId}")
    public ApiResponse<TaskDetailDTO> getTaskDetail(@PathVariable("taskId") Long taskId) {
        TaskDetailDTO detail = taskService.getTaskDetail(taskId);
        return ResponseUtil.success(detail);
    }

    @GetMapping(params = "taskid")
    public ApiResponse<TaskDetailDTO> getTaskDetailByQuery(@RequestParam("taskid") Long taskId) {
        TaskDetailDTO detail = taskService.getTaskDetail(taskId);
        return ResponseUtil.success(detail);
    }

    @GetMapping
    public ApiResponse<TaskListResponseDTO> listTasks(@RequestParam(name = "page", required = false) Integer page,
                                                      @RequestParam(name = "page_size", required = false) Integer pageSize,
                                                      @RequestParam(name = "status", required = false) Integer status) {
        TaskListResponseDTO data = taskService.listTasks(page, pageSize, status);
        return ResponseUtil.success(data);
    }
}

