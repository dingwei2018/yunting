package com.yunting.controller;

import com.yunting.common.ApiResponse;
import com.yunting.common.ResponseUtil;
import com.yunting.dto.audio.AudioMergeRequest;
import com.yunting.dto.audio.AudioMergeResponseDTO;
import com.yunting.dto.audio.AudioMergeStatusDTO;
import com.yunting.service.AudioMergeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merge")
public class AudioMergeController {

    private final AudioMergeService audioMergeService;

    public AudioMergeController(AudioMergeService audioMergeService) {
        this.audioMergeService = audioMergeService;
    }

    @PostMapping("/audio")
    public ApiResponse<AudioMergeResponseDTO> mergeAudio(@RequestBody AudioMergeRequest request) {
        AudioMergeResponseDTO dto = audioMergeService.mergeAudio(request.getTaskId(), request);
        return ResponseUtil.success(dto);
    }

    @GetMapping("/getStatus")
    public ApiResponse<AudioMergeStatusDTO> getStatus(@RequestParam("mergeId") Long mergeId) {
        AudioMergeStatusDTO dto = audioMergeService.getMergeStatus(mergeId);
        return ResponseUtil.success(dto);
    }
}


