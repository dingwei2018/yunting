package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.task.TaskDetailDTO;
import com.yunting.dto.task.TaskListResponseDTO;
import com.yunting.dto.task.TaskCreateRequest;
import com.yunting.service.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/createTask")
    public ApiResponse<TaskDetailDTO> createTask(@RequestParam("content") String content,
                                                 @RequestParam(value = "delimiters", required = false) String delimiters) {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setContent(content);
        request.setDelimiters(delimiters);
        TaskDetailDTO task = taskService.createTask(request);
        return ResponseUtil.success(task);
    }

    @GetMapping("/getTaskDetail")
    public ApiResponse<TaskDetailDTO> getTaskDetail(@RequestParam("taskid") Long taskId) {
        TaskDetailDTO detail = taskService.getTaskDetail(taskId);
        return ResponseUtil.success(detail);
    }

    @GetMapping("/listTasks")
    public ApiResponse<TaskListResponseDTO> listTasks(@RequestParam(name = "page", required = false) Integer page,
                                                      @RequestParam(name = "page_size", required = false) Integer pageSize,
                                                      @RequestParam(name = "status", required = false) Integer status) {
        TaskListResponseDTO data = taskService.listTasks(page, pageSize, status);
        return ResponseUtil.success(data);
    }
}

