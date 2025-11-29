package com.yunting.service;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.PutObjectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * OBS 存储服务，封装上传/下载等能力。
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

    @Value("${huaweicloud.obs.endpoint:}")
    private String obsEndpoint;

    @Value("${huaweicloud.region:cn-north-4}")
    private String region;

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

    /**
     * 从URL下载文件并上传到OBS
     * 
     * @param downloadUrl 文件下载URL
     * @param objectKey OBS对象键（文件路径）
     * @return OBS访问URL
     */
    public String uploadFromUrl(String downloadUrl, String objectKey) {
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalStateException("未配置 huaweicloud.obs.bucket，无法上传文件");
        }
        
        try {
            // 1. 从URL下载文件
            logger.info("开始从URL下载文件: {}", downloadUrl);
            URL url = new URL(downloadUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(30000); // 30秒连接超时
            connection.setReadTimeout(300000); // 5分钟读取超时
            
            try (InputStream inputStream = connection.getInputStream()) {
                // 2. 上传到OBS
                logger.info("开始上传文件到OBS，bucket: {}, objectKey: {}", bucketName, objectKey);
                PutObjectRequest putRequest = new PutObjectRequest(bucketName, objectKey, inputStream);
                PutObjectResult result = obsClient.putObject(putRequest);
                
                // 3. 生成访问URL
                String obsUrl = generateObsUrl(objectKey);
                logger.info("文件上传成功，OBS URL: {}", obsUrl);
                return obsUrl;
            }
        } catch (Exception e) {
            logger.error("上传文件到OBS失败，downloadUrl: {}, objectKey: {}", downloadUrl, objectKey, e);
            throw new RuntimeException("上传文件到OBS失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从本地文件上传到OBS
     * 
     * @param localFile 本地文件
     * @param objectKey OBS对象键（文件路径）
     * @return OBS访问URL
     */
    public String uploadFromFile(File localFile, String objectKey) {
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalStateException("未配置 huaweicloud.obs.bucket，无法上传文件");
        }
        
        if (localFile == null || !localFile.exists() || !localFile.isFile()) {
            throw new IllegalArgumentException("本地文件不存在或不是有效文件: " + 
                    (localFile != null ? localFile.getAbsolutePath() : "null"));
        }
        
        try {
            logger.info("开始从本地文件上传到OBS，localFile: {}, bucket: {}, objectKey: {}", 
                    localFile.getAbsolutePath(), bucketName, objectKey);
            
            try (FileInputStream fileInputStream = new FileInputStream(localFile)) {
                PutObjectRequest putRequest = new PutObjectRequest(bucketName, objectKey, fileInputStream);
                PutObjectResult result = obsClient.putObject(putRequest);
                
                String obsUrl = generateObsUrl(objectKey);
                logger.info("文件上传成功，OBS URL: {}", obsUrl);
                return obsUrl;
            }
        } catch (Exception e) {
            logger.error("上传文件到OBS失败，localFile: {}, objectKey: {}", 
                    localFile.getAbsolutePath(), objectKey, e);
            throw new RuntimeException("上传文件到OBS失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成OBS访问URL
     * 
     * @param objectKey OBS对象键
     * @return 访问URL
     */
    private String generateObsUrl(String objectKey) {
        // 根据OBS配置生成访问URL
        // 格式：https://{bucket}.obs.{region}.myhuaweicloud.com/{objectKey}
        // 如果配置了endpoint，也可以使用endpoint
        if (StringUtils.hasText(obsEndpoint) && !obsEndpoint.equals("test")) {
            // 如果配置了endpoint，使用endpoint
            String endpoint = obsEndpoint.startsWith("http") ? obsEndpoint : "https://" + obsEndpoint;
            return String.format("%s/%s", endpoint, objectKey);
        } else {
            // 使用region构建URL
            return String.format("https://%s.obs.%s.myhuaweicloud.com/%s", 
                    bucketName, 
                    region, 
                    objectKey);
        }
    }
}

