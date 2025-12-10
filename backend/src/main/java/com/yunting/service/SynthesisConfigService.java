package com.yunting.service;

import com.yunting.dto.synthesis.SynthesisSetConfigRequest;

/**
 * 合成配置服务
 * 负责处理合成参数设置
 */
public interface SynthesisConfigService {

    /**
     * 设置拆句合成参数
     * 覆盖旧数据
     * 
     * @param request 配置请求
     */
    void setConfig(SynthesisSetConfigRequest request);
}
