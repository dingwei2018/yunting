package com.yunting.service.impl;

import com.yunting.service.FFmpegService;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class FFmpegServiceImpl implements FFmpegService {

    private static final Logger logger = LoggerFactory.getLogger(FFmpegServiceImpl.class);
    
    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;
    
    @Value("${ffprobe.path:ffprobe}")
    private String ffprobePath;
    
    @Value("${ffmpeg.timeout:300000}")
    private long ffmpegTimeout; // 默认5分钟超时
    
    private FFmpeg ffmpeg;
    private FFprobe ffprobe;
    private FFmpegExecutor executor;

    /**
     * 初始化 FFmpeg 和 FFprobe 实例
     */
    private void initFFmpeg() {
        if (ffmpeg == null || ffprobe == null) {
            try {
                ffmpeg = new FFmpeg(ffmpegPath);
                ffprobe = new FFprobe(ffprobePath);
                executor = new FFmpegExecutor(ffmpeg, ffprobe);
                logger.info("FFmpeg 初始化成功，路径: {}, FFprobe 路径: {}", ffmpegPath, ffprobePath);
            } catch (IOException e) {
                logger.error("FFmpeg 初始化失败", e);
                throw new RuntimeException("FFmpeg 初始化失败: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public int mergeAudioFiles(List<File> inputFiles, File outputFile) {
        if (inputFiles == null || inputFiles.isEmpty()) {
            throw new IllegalArgumentException("输入文件列表不能为空");
        }
        
        // 验证所有输入文件存在
        for (File file : inputFiles) {
            if (file == null || !file.exists() || !file.isFile()) {
                throw new IllegalArgumentException("输入文件不存在: " + 
                    (file != null ? file.getAbsolutePath() : "null"));
            }
        }
        
        // 确保输出目录存在
        File outputDir = outputFile.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        initFFmpeg();
        
        try {
            // 构建 FFmpeg 命令
            FFmpegBuilder builder = buildMergeCommand(inputFiles, outputFile);
            
            logger.info("执行 FFmpeg 合并命令，输入文件数: {}, 输出文件: {}", 
                inputFiles.size(), outputFile.getAbsolutePath());
            
            // 执行命令
            executor.createJob(builder).run();
            
            // 验证输出文件是否存在
            if (!outputFile.exists() || outputFile.length() == 0) {
                throw new RuntimeException("合并后的音频文件不存在或为空: " + 
                    outputFile.getAbsolutePath());
            }
            
            logger.info("音频合并成功，输出文件: {}, 大小: {} bytes", 
                outputFile.getAbsolutePath(), outputFile.length());
            
            // 获取合并后的音频时长
            return getAudioDuration(outputFile);
            
        } catch (Exception e) {
            logger.error("执行 FFmpeg 合并失败", e);
            throw new RuntimeException("FFmpeg 合并失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int getAudioDuration(File audioFile) {
        if (audioFile == null || !audioFile.exists() || !audioFile.isFile()) {
            throw new IllegalArgumentException("音频文件不存在: " + 
                (audioFile != null ? audioFile.getAbsolutePath() : "null"));
        }
        
        initFFmpeg();
        
        try {
            logger.debug("获取音频时长，文件: {}", audioFile.getAbsolutePath());
            
            // 使用 FFprobe 探测音频文件信息
            FFmpegProbeResult probeResult = ffprobe.probe(audioFile.getAbsolutePath());
            
            // 查找音频流
            FFmpegStream audioStream = null;
            for (FFmpegStream stream : probeResult.getStreams()) {
                if (stream.codec_type == FFmpegStream.CodecType.AUDIO) {
                    audioStream = stream;
                    break;
                }
            }
            
            if (audioStream == null) {
                throw new RuntimeException("无法找到音频流: " + audioFile.getAbsolutePath());
            }
            
            // 获取时长（秒）
            double durationSeconds = probeResult.getFormat().duration;
            
            // 转换为毫秒
            int durationMs = (int) (durationSeconds * 1000);
            
            logger.debug("音频时长: {}ms ({}秒)", durationMs, durationSeconds);
            
            return durationMs;
            
        } catch (Exception e) {
            logger.error("获取音频时长失败", e);
            throw new RuntimeException("获取音频时长失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isFFmpegAvailable() {
        try {
            initFFmpeg();
            // 尝试获取版本信息来验证 FFmpeg 是否可用
            ffmpeg.version();
            return true;
        } catch (Exception e) {
            logger.warn("FFmpeg 不可用: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 构建 FFmpeg 合并命令
     * 使用 filter_complex 进行音频合并，支持不同格式的音频文件
     */
    private FFmpegBuilder buildMergeCommand(List<File> inputFiles, File outputFile) {
        FFmpegBuilder builder = new FFmpegBuilder();
        
        // 添加所有输入文件
        for (File file : inputFiles) {
            builder.addInput(file.getAbsolutePath());
        }
        
        // 构建 filter_complex 参数
        // 格式: [0:0][1:0]concat=n=2:v=0:a=1[out]
        StringBuilder filterComplex = new StringBuilder();
        for (int i = 0; i < inputFiles.size(); i++) {
            filterComplex.append("[").append(i).append(":0]");
        }
        filterComplex.append("concat=n=").append(inputFiles.size())
            .append(":v=0:a=1[out]");
        
        // 设置 filter_complex
        builder.setComplexFilter(filterComplex.toString());
        
        // 覆盖输出文件（在 builder 上设置）
        builder.overrideOutputFiles(true);
        
        // 输出文件及格式和质量设置
        // 在 0.8.0 版本中，音频相关设置需要在 addOutput 返回的对象上调用
        // 统一使用 WAV 格式，使用 PCM 编码
        // -map [out] 应该在输出部分添加，使用 addExtraArgs
        builder.addOutput(outputFile.getAbsolutePath())
               .setFormat("wav")
               .setAudioCodec("pcm_s16le")  // WAV 使用 PCM 编码（16位小端）
               .setAudioSampleRate(44100)    // 采样率 44100Hz
               .setAudioChannels(2)          // 立体声
               .addExtraArgs("-map", "[out]"); // 映射 filter_complex 的输出
        
        return builder;
    }
}

