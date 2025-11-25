package com.yunting.config;

import com.obs.services.ObsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * OBS 客户端配置。
 * 当配置了 huaweicloud.obs.endpoint 时才会初始化，以避免在本地缺少凭证时报错。
 */
@Configuration
public class HuaweiObsConfig {

    @Value("${huaweicloud.ak:}")
    private String ak;

    @Value("${huaweicloud.sk:}")
    private String sk;

    @Value("${huaweicloud.obs.endpoint:}")
    private String obsEndpoint;

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "huaweicloud.obs", name = "endpoint")
    public ObsClient obsClient() {
        if (!StringUtils.hasText(ak) || !StringUtils.hasText(sk)) {
            throw new IllegalStateException("华为云 OBS 需要配置 huaweicloud.ak / huaweicloud.sk");
        }
        return new ObsClient(ak, sk, obsEndpoint);
    }
}

