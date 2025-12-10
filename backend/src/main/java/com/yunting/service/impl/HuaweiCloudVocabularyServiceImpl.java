package com.yunting.service.impl;

import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.exception.ConnectionException;
import com.huaweicloud.sdk.core.exception.RequestTimeoutException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.metastudio.v1.MetaStudioClient;
import com.huaweicloud.sdk.metastudio.v1.region.MetaStudioRegion;
import java.lang.reflect.Method;
import com.yunting.model.ReadingRule;
import com.yunting.service.HuaweiCloudVocabularyService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 华为云自定义读法服务实现
 * 实现限流：10次/秒
 */
@Service
public class HuaweiCloudVocabularyServiceImpl implements HuaweiCloudVocabularyService {

    private static final Logger logger = LoggerFactory.getLogger(HuaweiCloudVocabularyServiceImpl.class);

    // 限流配置：10次/秒 = 每100毫秒处理一次
    private static final long RATE_LIMIT_INTERVAL_MS = 100;

    private final BlockingQueue<VocabularyRequest> requestQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService rateLimiterExecutor = Executors.newScheduledThreadPool(1);
    private volatile boolean running = false;

    @Value("${huaweicloud.ak:}")
    private String huaweiCloudAk;

    @Value("${huaweicloud.sk:}")
    private String huaweiCloudSk;

    @Value("${huaweicloud.region:cn-north-4}")
    private String huaweiCloudRegion;

    @Value("${huaweicloud.project-id:}")
    private String huaweiCloudProjectId;

    /**
     * 请求类型
     */
    private enum RequestType {
        LIST, CREATE, DELETE
    }

    /**
     * 限流请求封装
     */
    private static class VocabularyRequest {
        RequestType type;
        String pattern;
        String ruleValue;
        String vocabularyId;
        java.util.concurrent.CompletableFuture<Object> future;

        VocabularyRequest(RequestType type) {
            this.type = type;
            this.future = new java.util.concurrent.CompletableFuture<>();
        }
    }

    @PostConstruct
    public void init() {
        running = true;
        rateLimiterExecutor.scheduleAtFixedRate(this::processNextRequest,
                0, RATE_LIMIT_INTERVAL_MS, TimeUnit.MILLISECONDS);
        logger.info("华为云自定义读法服务限流器启动，限流速率：10次/秒");
    }

    @PreDestroy
    public void destroy() {
        running = false;
        rateLimiterExecutor.shutdown();
        logger.info("华为云自定义读法服务限流器停止");
    }

    /**
     * 限流处理：每100ms处理一条请求（10次/秒）
     */
    private void processNextRequest() {
        if (!running) {
            return;
        }

        VocabularyRequest request = requestQueue.poll();
        if (request != null) {
            try {
                processRequest(request);
            } catch (Exception e) {
                logger.error("处理华为云自定义读法请求失败", e);
                request.future.completeExceptionally(e);
            }
        }
    }

    /**
     * 处理单条请求
     */
    private void processRequest(VocabularyRequest request) {
        try {
            MetaStudioClient client = createClient();

            switch (request.type) {
                case LIST:
                    List<VocabularyConfig> configs = doListVocabularyConfigs(client);
                    request.future.complete(configs);
                    break;
                case CREATE:
                    String vocabularyId = doCreateVocabularyConfig(client, request.pattern, request.ruleValue);
                    request.future.complete(vocabularyId);
                    break;
                case DELETE:
                    doDeleteVocabularyConfig(client, request.vocabularyId);
                    request.future.complete(null);
                    break;
            }
        } catch (Exception e) {
            logger.error("处理华为云自定义读法请求异常，type: {}", request.type, e);
            request.future.completeExceptionally(e);
        }
    }

    /**
     * 创建华为云客户端
     */
    private MetaStudioClient createClient() {
        ICredential auth = new BasicCredentials()
                .withProjectId(huaweiCloudProjectId)
                .withAk(huaweiCloudAk)
                .withSk(huaweiCloudSk);

        return MetaStudioClient.newBuilder()
                .withCredential(auth)
                .withRegion(MetaStudioRegion.valueOf(huaweiCloudRegion))
                .build();
    }

    @Override
    public List<VocabularyConfig> listVocabularyConfigs() {
        VocabularyRequest request = new VocabularyRequest(RequestType.LIST);
        requestQueue.offer(request);

        try {
            @SuppressWarnings("unchecked")
            List<VocabularyConfig> result = (List<VocabularyConfig>) request.future.get(30, TimeUnit.SECONDS);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            logger.error("查询华为云自定义读法规则列表失败", e);
            throw new RuntimeException("查询华为云自定义读法规则列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String createVocabularyConfig(String pattern, String ruleValue) {
        VocabularyRequest request = new VocabularyRequest(RequestType.CREATE);
        request.pattern = pattern;
        request.ruleValue = ruleValue;
        requestQueue.offer(request);

        try {
            return (String) request.future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("创建华为云自定义读法规则失败，pattern: {}, ruleValue: {}", pattern, ruleValue, e);
            throw new RuntimeException("创建华为云自定义读法规则失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteVocabularyConfig(String vocabularyId) {
        VocabularyRequest request = new VocabularyRequest(RequestType.DELETE);
        request.vocabularyId = vocabularyId;
        requestQueue.offer(request);

        try {
            request.future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("删除华为云自定义读法规则失败，vocabularyId: {}", vocabularyId, e);
            throw new RuntimeException("删除华为云自定义读法规则失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateVocabularyConfigs(List<ReadingRule> rules) {
        try {
            // 1. 查询现有的规则
            List<VocabularyConfig> existingConfigs = listVocabularyConfigs();

            // 2. 删除所有现有规则
            for (VocabularyConfig config : existingConfigs) {
                try {
                    deleteVocabularyConfig(config.getVocabularyId());
                    logger.info("删除华为云自定义读法规则，vocabularyId: {}", config.getVocabularyId());
                } catch (Exception e) {
                    logger.warn("删除华为云自定义读法规则失败，vocabularyId: {}，继续处理", config.getVocabularyId(), e);
                }
            }

            // 3. 创建新规则
            for (ReadingRule rule : rules) {
                try {
                    String vocabularyId = createVocabularyConfig(rule.getPattern(), rule.getRuleValue());
                    logger.info("创建华为云自定义读法规则成功，vocabularyId: {}, pattern: {}, ruleValue: {}", 
                            vocabularyId, rule.getPattern(), rule.getRuleValue());
                } catch (Exception e) {
                    logger.error("创建华为云自定义读法规则失败，pattern: {}, ruleValue: {}，继续处理", 
                            rule.getPattern(), rule.getRuleValue(), e);
                }
            }

            logger.info("批量更新华为云自定义读法规则完成，共处理 {} 条规则", rules.size());
        } catch (Exception e) {
            logger.error("批量更新华为云自定义读法规则失败", e);
            throw new RuntimeException("批量更新华为云自定义读法规则失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行查询自定义读法规则列表
     */
    private List<VocabularyConfig> doListVocabularyConfigs(MetaStudioClient client) {
        try {
            // 使用反射调用API（因为SDK版本可能不同）
            Class<?> requestClass = Class.forName("com.huaweicloud.sdk.metastudio.v1.model.ListTtscVocabularyConfigsRequest");
            Object request = requestClass.getDeclaredConstructor().newInstance();
            
            // 尝试设置projectId（如果方法存在）
            try {
                requestClass.getMethod("withProjectId", String.class).invoke(request, huaweiCloudProjectId);
            } catch (NoSuchMethodException e) {
                // 如果方法不存在，跳过（projectId可能在credential中已设置）
                logger.debug("ListTtscVocabularyConfigsRequest没有withProjectId方法，跳过");
            }

            Method listMethod = client.getClass().getMethod("listTtscVocabularyConfigs", requestClass);
            Object response = listMethod.invoke(client, request);
            
            List<VocabularyConfig> configs = new ArrayList<>();
            
            // 获取响应体
            Object responseBody = null;
            try {
                Method getBody = response.getClass().getMethod("getBody");
                responseBody = getBody.invoke(response);
            } catch (NoSuchMethodException e) {
                // 如果getBody方法不存在，尝试直接获取
                logger.debug("响应对象没有getBody方法，尝试直接访问");
            }
            
            if (responseBody != null) {
                // 获取vocabularyConfigs列表
                try {
                    Method getVocabularyConfigs = responseBody.getClass().getMethod("getVocabularyConfigs");
                    @SuppressWarnings("unchecked")
                    List<Object> vocabularyConfigs = (List<Object>) getVocabularyConfigs.invoke(responseBody);
                    
                    if (vocabularyConfigs != null) {
                        for (Object info : vocabularyConfigs) {
                            VocabularyConfig config = new VocabularyConfig();
                            Method getVocabularyId = info.getClass().getMethod("getVocabularyId");
                            Method getPattern = info.getClass().getMethod("getPattern");
                            Method getRuleValue = info.getClass().getMethod("getRuleValue");
                            
                            config.setVocabularyId((String) getVocabularyId.invoke(info));
                            config.setPattern((String) getPattern.invoke(info));
                            config.setRuleValue((String) getRuleValue.invoke(info));
                            configs.add(config);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("解析响应体失败，可能API结构不同", e);
                }
            }

            logger.info("查询华为云自定义读法规则列表成功，共 {} 条", configs.size());
            return configs;
        } catch (ConnectionException e) {
            logger.error("查询华为云自定义读法规则列表连接异常", e);
            throw new RuntimeException("查询华为云自定义读法规则列表连接异常: " + e.getMessage(), e);
        } catch (RequestTimeoutException e) {
            logger.error("查询华为云自定义读法规则列表请求超时", e);
            throw new RuntimeException("查询华为云自定义读法规则列表请求超时: " + e.getMessage(), e);
        } catch (ServiceResponseException e) {
            logger.error("查询华为云自定义读法规则列表服务响应异常，HTTP状态码={}, 错误码={}, 错误信息={}", 
                    e.getHttpStatusCode(), e.getErrorCode(), e.getErrorMsg());
            throw new RuntimeException("查询华为云自定义读法规则列表服务响应异常: " + e.getErrorMsg(), e);
        } catch (Exception e) {
            logger.error("查询华为云自定义读法规则列表异常", e);
            throw new RuntimeException("查询华为云自定义读法规则列表异常: " + e.getMessage(), e);
        }
    }

    /**
     * 执行创建自定义读法规则
     */
    private String doCreateVocabularyConfig(MetaStudioClient client, String pattern, String ruleValue) {
        try {
            // 使用反射调用API
            Class<?> requestClass = Class.forName("com.huaweicloud.sdk.metastudio.v1.model.CreateTtscVocabularyConfigsRequest");
            Class<?> bodyClass = Class.forName("com.huaweicloud.sdk.metastudio.v1.model.CreateTtscVocabularyConfigsRequestBody");
            
            Object request = requestClass.getDeclaredConstructor().newInstance();
            Object body = bodyClass.getDeclaredConstructor().newInstance();
            
            // 尝试设置projectId（如果方法存在）
            try {
                requestClass.getMethod("withProjectId", String.class).invoke(request, huaweiCloudProjectId);
            } catch (NoSuchMethodException e) {
                logger.debug("CreateTtscVocabularyConfigsRequest没有withProjectId方法，跳过");
            }
            
            // 设置body参数
            bodyClass.getMethod("withPattern", String.class).invoke(body, pattern);
            bodyClass.getMethod("withRuleValue", String.class).invoke(body, ruleValue);
            requestClass.getMethod("withBody", bodyClass).invoke(request, body);

            Method createMethod = client.getClass().getMethod("createTtscVocabularyConfigs", requestClass);
            Object response = createMethod.invoke(client, request);
            
            // 获取vocabularyId
            String vocabularyId = null;
            try {
                Method getVocabularyId = response.getClass().getMethod("getVocabularyId");
                vocabularyId = (String) getVocabularyId.invoke(response);
            } catch (NoSuchMethodException e) {
                // 尝试从body中获取
                try {
                    Method getBody = response.getClass().getMethod("getBody");
                    Object responseBody = getBody.invoke(response);
                    if (responseBody != null) {
                        Method getVocabularyIdFromBody = responseBody.getClass().getMethod("getVocabularyId");
                        vocabularyId = (String) getVocabularyIdFromBody.invoke(responseBody);
                    }
                } catch (Exception ex) {
                    logger.warn("无法从响应中获取vocabularyId", ex);
                }
            }
            
            if (!StringUtils.hasText(vocabularyId)) {
                throw new RuntimeException("创建华为云自定义读法规则失败：未返回vocabularyId");
            }

            logger.info("创建华为云自定义读法规则成功，vocabularyId: {}, pattern: {}, ruleValue: {}", 
                    vocabularyId, pattern, ruleValue);
            return vocabularyId;
        } catch (ConnectionException e) {
            logger.error("创建华为云自定义读法规则连接异常，pattern: {}, ruleValue: {}", pattern, ruleValue, e);
            throw new RuntimeException("创建华为云自定义读法规则连接异常: " + e.getMessage(), e);
        } catch (RequestTimeoutException e) {
            logger.error("创建华为云自定义读法规则请求超时，pattern: {}, ruleValue: {}", pattern, ruleValue, e);
            throw new RuntimeException("创建华为云自定义读法规则请求超时: " + e.getMessage(), e);
        } catch (ServiceResponseException e) {
            logger.error("创建华为云自定义读法规则服务响应异常，pattern: {}, ruleValue: {}, HTTP状态码={}, 错误码={}, 错误信息={}", 
                    pattern, ruleValue, e.getHttpStatusCode(), e.getErrorCode(), e.getErrorMsg());
            throw new RuntimeException("创建华为云自定义读法规则服务响应异常: " + e.getErrorMsg(), e);
        } catch (Exception e) {
            logger.error("创建华为云自定义读法规则异常，pattern: {}, ruleValue: {}", pattern, ruleValue, e);
            throw new RuntimeException("创建华为云自定义读法规则异常: " + e.getMessage(), e);
        }
    }

    /**
     * 执行删除自定义读法规则
     */
    private void doDeleteVocabularyConfig(MetaStudioClient client, String vocabularyId) {
        try {
            // 使用反射调用API
            Class<?> requestClass = Class.forName("com.huaweicloud.sdk.metastudio.v1.model.DeleteTtscVocabularyConfigsRequest");
            Object request = requestClass.getDeclaredConstructor().newInstance();
            
            // 尝试设置projectId（如果方法存在）
            try {
                requestClass.getMethod("withProjectId", String.class).invoke(request, huaweiCloudProjectId);
            } catch (NoSuchMethodException e) {
                logger.debug("DeleteTtscVocabularyConfigsRequest没有withProjectId方法，跳过");
            }
            
            // 设置vocabularyId
            requestClass.getMethod("withVocabularyId", String.class).invoke(request, vocabularyId);

            Method deleteMethod = client.getClass().getMethod("deleteTtscVocabularyConfigs", requestClass);
            deleteMethod.invoke(client, request);

            logger.info("删除华为云自定义读法规则成功，vocabularyId: {}", vocabularyId);
        } catch (ConnectionException e) {
            logger.error("删除华为云自定义读法规则连接异常，vocabularyId: {}", vocabularyId, e);
            throw new RuntimeException("删除华为云自定义读法规则连接异常: " + e.getMessage(), e);
        } catch (RequestTimeoutException e) {
            logger.error("删除华为云自定义读法规则请求超时，vocabularyId: {}", vocabularyId, e);
            throw new RuntimeException("删除华为云自定义读法规则请求超时: " + e.getMessage(), e);
        } catch (ServiceResponseException e) {
            logger.error("删除华为云自定义读法规则服务响应异常，vocabularyId: {}, HTTP状态码={}, 错误码={}, 错误信息={}", 
                    vocabularyId, e.getHttpStatusCode(), e.getErrorCode(), e.getErrorMsg());
            throw new RuntimeException("删除华为云自定义读法规则服务响应异常: " + e.getErrorMsg(), e);
        } catch (Exception e) {
            logger.error("删除华为云自定义读法规则异常，vocabularyId: {}", vocabularyId, e);
            throw new RuntimeException("删除华为云自定义读法规则异常: " + e.getMessage(), e);
        }
    }
}
