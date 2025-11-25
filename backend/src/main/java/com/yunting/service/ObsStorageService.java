package com.yunting.service;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * OBS 存储服务占位实现，后续可以在此封装上传/下载等能力。
 */
@Service
@ConditionalOnBean(ObsClient.class)
public class ObsStorageService {

    private static final Logger logger = LoggerFactory.getLogger(ObsStorageService.class);

    private final ObsClient obsClient;

    @Value("${huaweicloud.obs.bucket:}")
    private String bucketName;

    @Value("${huaweicloud.obs.prefix:audio/}")
    private String objectPrefix;

    public ObsStorageService(ObsClient obsClient) {
        this.obsClient = obsClient;
    }

    /**
     * 简单检查 Bucket 是否存在，便于运行期自检。
     */
    public boolean bucketExists() {
        if (!StringUtils.hasText(bucketName)) {
            logger.warn("未配置 huaweicloud.obs.bucket，无法检测 OBS Bucket");
            return false;
        }
        try {
            obsClient.headBucket(bucketName);
            return true;
        } catch (ObsException ex) {
            if (ex.getResponseCode() == 404) {
                logger.warn("OBS Bucket {} 不存在", bucketName);
                return false;
            }
            throw ex;
        }
    }

    /**
     * 统一拼装对象 Key，后续上传音频文件时复用。
     */
    public String buildObjectKey(String fileName) {
        String prefix = StringUtils.hasText(objectPrefix) ? objectPrefix : "";
        return prefix + fileName;
    }
}

