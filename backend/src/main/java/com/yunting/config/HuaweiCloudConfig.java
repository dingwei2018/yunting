package com.yunting.config;

import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 华为云 SDK 配置类
 */
@Configuration
public class HuaweiCloudConfig {

    @Value("${huaweicloud.ak:}")
    private String ak;

    @Value("${huaweicloud.sk:}")
    private String sk;

    @Value("${huaweicloud.region:cn-north-4}")
    private String region;

    @Value("${huaweicloud.project-id:}")
    private String projectId;

    @Value("${app.callback.url:}")
    private String callbackUrl;

    /**
     * 创建华为云认证凭证
     * 可通过环境变量 HUAWEICLOUD_SDK_AK 和 HUAWEICLOUD_SDK_SK 配置
     */
    @Bean
    public ICredential huaweiCloudCredential() {
        BasicCredentials credentials = new BasicCredentials()
                .withAk(ak)
                .withSk(sk);
        
        if (projectId != null && !projectId.isEmpty()) {
            credentials.withProjectId(projectId);
        }
        
        return credentials;
    }

    /**
     * 获取配置的区域
     */
    public String getRegion() {
        return region;
    }

    /**
     * 获取回调URL配置
     * 用于华为云TTS异步任务的回调通知
     * 
     * @return 回调URL，如果未配置则返回null
     */
    public String getCallbackUrl() {
        return callbackUrl != null && !callbackUrl.isEmpty() ? callbackUrl : null;
    }
}

