package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.task.TaskCreateRequest;
import com.yunting.dto.task.TaskCreateResponseDTO;
import com.yunting.dto.task.TaskDetailDTO;
import com.yunting.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务管理/任务管理控制器
 * @module 任务管理
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
     * @param request 创建任务请求，包含文本内容和拆句符号类型数组
     * @return 创建的任务信息，包含任务ID、内容、字符数、状态、音频URL、音频时长、拆句列表和创建时间
     */
    @PostMapping("/create")
    public ApiResponse<TaskCreateResponseDTO> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskCreateResponseDTO data = taskService.createTask(request);
        return ResponseUtil.success(data);
    }

    /**
     * 获取任务详情
     * 返回任务详情以及任务下的拆句内容
     *
     * @param taskid 任务ID（必填）
     * @return 任务详情，包含任务ID、合并ID、内容、字符数、状态、音频URL、音频时长和创建更新时间
     */
    @GetMapping("/getDetail")
    public ApiResponse<TaskDetailDTO> getTaskDetail(@RequestParam("taskid") Long taskid) {
        TaskDetailDTO data = taskService.getTaskDetail(taskid);
        return ResponseUtil.success(data);
    }
}

