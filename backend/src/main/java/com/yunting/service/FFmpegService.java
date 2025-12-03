package com.yunting.service;

import java.io.File;
import java.util.List;

/**
 * FFmpeg 音频处理服务
 */
public interface FFmpegService {
    
    /**
     * 合并多个音频文件
     * 
     * @param inputFiles 输入音频文件列表（按顺序）
     * @param outputFile 输出音频文件
     * @return 合并后的音频时长（毫秒）
     * @throws RuntimeException 如果合并失败
     */
    int mergeAudioFiles(List<File> inputFiles, File outputFile);
    
    /**
     * 获取音频时长（毫秒）
     * 
     * @param audioFile 音频文件
     * @return 音频时长（毫秒）
     * @throws RuntimeException 如果获取失败
     */
    int getAudioDuration(File audioFile);
    
    /**
     * 检查 FFmpeg 是否可用
     * 
     * @return true 如果 FFmpeg 可用
     */
    boolean isFFmpegAvailable();
}

