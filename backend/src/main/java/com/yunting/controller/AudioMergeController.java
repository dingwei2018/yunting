package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.audio.AudioMergeRequest;
import com.yunting.dto.audio.AudioMergeResponseDTO;
import com.yunting.service.AudioMergeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AudioMergeController {

    private final AudioMergeService audioMergeService;

    public AudioMergeController(AudioMergeService audioMergeService) {
        this.audioMergeService = audioMergeService;
    }

    @PostMapping("/tasks/audio/merge")
    public ApiResponse<AudioMergeResponseDTO> mergeAudio(@RequestParam("taskid") Long taskId,
                                                         @RequestBody(required = false) AudioMergeRequest request) {
        AudioMergeResponseDTO dto = audioMergeService.mergeAudio(taskId, request);
        return ResponseUtil.success(dto);
    }

    @GetMapping("/audio/merges/merge")
    public ApiResponse<AudioMergeResponseDTO> getMergeStatus(@RequestParam("mergeid") Long mergeId) {
        AudioMergeResponseDTO dto = audioMergeService.getMergeStatus(mergeId);
        return ResponseUtil.success(dto);
    }
}


