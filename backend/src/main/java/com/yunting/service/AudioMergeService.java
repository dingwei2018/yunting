package com.yunting.service;

import com.yunting.dto.audio.AudioMergeRequest;
import com.yunting.dto.audio.AudioMergeResponseDTO;

public interface AudioMergeService {

    AudioMergeResponseDTO mergeAudio(Long taskId, AudioMergeRequest request);

    AudioMergeResponseDTO getMergeStatus(Long mergeId);
}


