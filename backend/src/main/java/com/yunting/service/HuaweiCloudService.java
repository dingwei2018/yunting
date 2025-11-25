package com.yunting.service;

import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.exception.ConnectionException;
import com.huaweicloud.sdk.core.exception.RequestTimeoutException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.yunting.config.HuaweiCloudConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 华为云服务封装类
 * 提供统一的华为云服务调用入口
 */
@Service
public class HuaweiCloudService {

    private static final Logger logger = LoggerFactory.getLogger(HuaweiCloudService.class);

    @Autowired
    private ICredential credential;

    @Autowired
    private HuaweiCloudConfig config;

    /**
     * 获取认证凭证
     */
    public ICredential getCredential() {
        return credential;
    }

    /**
     * 获取配置的区域
     */
    public String getRegion() {
        return config.getRegion();
    }

    /**
     * 处理华为云 SDK 异常
     */
    public void handleException(Exception e) {
        if (e instanceof ConnectionException) {
            logger.error("华为云连接异常", e);
        } else if (e instanceof RequestTimeoutException) {
            logger.error("华为云请求超时", e);
        } else if (e instanceof ServiceResponseException) {
            ServiceResponseException sre = (ServiceResponseException) e;
            logger.error("华为云服务响应异常: HTTP状态码={}, 错误码={}, 错误信息={}, 请求ID={}",
                    sre.getHttpStatusCode(), sre.getErrorCode(), sre.getErrorMsg(), sre.getRequestId());
        } else {
            logger.error("华为云调用异常", e);
        }
    }
}

