package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.audio.AudioMergeRequest;
import com.yunting.dto.audio.AudioMergeResponseDTO;
import com.yunting.dto.audio.AudioMergeStatusDTO;
import com.yunting.service.AudioMergeService;
import org.springframework.web.bind.annotation.*;

/**
 * 音频合并/音频合并控制器
 * @module 音频合并
 */
@RestController
@RequestMapping("/api/merge")
public class AudioMergeController {

    private final AudioMergeService audioMergeService;

    public AudioMergeController(AudioMergeService audioMergeService) {
        this.audioMergeService = audioMergeService;
    }

    /**
     * 合并音频
     * 合并任务下的所有音频文件
     *
     * @param request 合并请求，包含 taskId
     * @return 合并响应，包含 mergeId
     */
    @PostMapping("/audio")
    public ApiResponse<AudioMergeResponseDTO> mergeAudio(@RequestBody AudioMergeRequest request) {
        AudioMergeResponseDTO dto = audioMergeService.mergeAudio(request.getTaskId(), request);
        return ResponseUtil.success(dto);
    }

    /**
     * 获取合并状态
     * 查询音频合并任务的状态
     *
     * @param mergeId 合并任务ID（必填）
     * @return 合并状态响应，包含任务ID、合并后的音频URL、时长和状态
     */
    @GetMapping("/getStatus")
    public ApiResponse<AudioMergeStatusDTO> getStatus(@RequestParam("mergeId") Long mergeId) {
        AudioMergeStatusDTO dto = audioMergeService.getMergeStatus(mergeId);
        return ResponseUtil.success(dto);
    }
}


