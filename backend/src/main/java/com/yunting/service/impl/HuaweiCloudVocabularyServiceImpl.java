package com.yunting.service.impl;

import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.exception.ConnectionException;
import com.huaweicloud.sdk.core.exception.RequestTimeoutException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.metastudio.v1.MetaStudioClient;
import com.huaweicloud.sdk.metastudio.v1.model.*;
import com.huaweicloud.sdk.metastudio.v1.region.MetaStudioRegion;
import com.yunting.constant.ReadingRuleType;
import com.yunting.model.ReadingRule;
import com.yunting.service.HuaweiCloudVocabularyService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Value("${huaweicloud.vocabulary.group-id:2c9084d59ac09773019b0d2662021381}")
    private String huaweiCloudVocabularyGroupId;

    /**
     * 请求类型
     */
    private enum RequestType {
        LIST, CREATE, DELETE, BATCH_DELETE
    }

    /**
     * 限流请求封装
     */
    private static class VocabularyRequest {
        RequestType type;
        String pattern;
        String ruleValue;
        Integer ruleType;
        String vocabularyId;
        List<String> vocabularyIds;
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
                    String vocabularyId = doCreateVocabularyConfig(client, request.pattern, request.ruleValue, request.ruleType);
                    request.future.complete(vocabularyId);
                    break;
                case DELETE:
                    doDeleteVocabularyConfig(client, request.vocabularyId);
                    request.future.complete(null);
                    break;
                case BATCH_DELETE:
                    doBatchDeleteVocabularyConfigs(client, request.vocabularyIds);
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
    public ListTtscVocabularyConfigsResponse listVocabularyConfigsResponse() {
        try {
            // 创建华为云客户端
            ICredential auth = new BasicCredentials()
                    .withProjectId(huaweiCloudProjectId)
                    .withAk(huaweiCloudAk)
                    .withSk(huaweiCloudSk);
            MetaStudioClient client = MetaStudioClient.newBuilder()
                    .withCredential(auth)
                    .withRegion(MetaStudioRegion.valueOf(huaweiCloudRegion))
                    .build();

            // 构建请求
            ListTtscVocabularyConfigsRequest request = new ListTtscVocabularyConfigsRequest();
            ListTtscVocabularyConfigsResponse response = client.listTtscVocabularyConfigs(request);
            logger.info("查询华为云自定义读法规则列表成功");
            return response;
        } catch (ConnectionException e) {
            logger.error("查询华为云自定义读法规则列表连接异常", e);
            throw new RuntimeException("查询华为云自定义读法规则列表连接异常: " + e.getMessage(), e);
        } catch (RequestTimeoutException e) {
            logger.error("查询华为云自定义读法规则列表请求超时", e);
            throw new RuntimeException("查询华为云自定义读法规则列表请求超时: " + e.getMessage(), e);
        } catch (ServiceResponseException e) {
            logger.error("查询华为云自定义读法规则列表服务响应异常，HTTP状态码={}, 请求ID={}, 错误码={}, 错误信息={}", 
                    e.getHttpStatusCode(), e.getRequestId(), e.getErrorCode(), e.getErrorMsg());
            throw new RuntimeException("查询华为云自定义读法规则列表服务响应异常: " + e.getErrorMsg(), e);
        } catch (Exception e) {
            logger.error("查询华为云自定义读法规则列表异常", e);
            throw new RuntimeException("查询华为云自定义读法规则列表异常: " + e.getMessage(), e);
        }
    }

    @Override
    public String createVocabularyConfig(String pattern, String ruleValue, Integer ruleType) {
        VocabularyRequest request = new VocabularyRequest(RequestType.CREATE);
        request.pattern = pattern;
        request.ruleValue = ruleValue;
        request.ruleType = ruleType;
        requestQueue.offer(request);

        try {
            return (String) request.future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("创建华为云自定义读法规则失败，pattern: {}, ruleValue: {}, ruleType: {}", pattern, ruleValue, ruleType, e);
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
    public void deleteVocabularyConfigs(List<String> vocabularyIds) {
        if (vocabularyIds == null || vocabularyIds.isEmpty()) {
            logger.warn("批量删除华为云自定义读法规则：vocabularyIds为空，跳过删除");
            return;
        }

        VocabularyRequest request = new VocabularyRequest(RequestType.BATCH_DELETE);
        request.vocabularyIds = vocabularyIds;
        requestQueue.offer(request);

        try {
            request.future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("批量删除华为云自定义读法规则失败，vocabularyIds: {}", vocabularyIds, e);
            throw new RuntimeException("批量删除华为云自定义读法规则失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateVocabularyConfigs(List<ReadingRule> rules) {
        try {
            // 1. 查询现有的规则
            List<VocabularyConfig> existingConfigs = listVocabularyConfigs();

            // 2. 构建需要的规则Map（pattern -> ReadingRule），用于快速查找
            Map<String, ReadingRule> requiredRuleMap = rules.stream()
                    .collect(Collectors.toMap(
                            ReadingRule::getPattern,
                            rule -> rule,
                            (v1, v2) -> v1  // 如果有重复，保留第一个
                    ));

            // 3. 构建云端规则Map（pattern -> VocabularyConfig），用于快速查找
            Map<String, VocabularyConfig> cloudRuleMap = existingConfigs.stream()
                    .collect(Collectors.toMap(
                            VocabularyConfig::getPattern,
                            config -> config,
                            (v1, v2) -> v1  // 如果有重复，保留第一个
                    ));

            // 4. 找出需要删除的规则ID列表
            List<String> vocabularyIdsToDelete = new ArrayList<>();
            for (VocabularyConfig cloudConfig : existingConfigs) {
                String pattern = cloudConfig.getPattern();
                ReadingRule requiredRule = requiredRuleMap.get(pattern);

                // 需要删除的情况：
                // a) 云端存在但需要的规则中不存在（多余的规则）
                // b) 云端存在且需要的规则中也存在，但内容不一致（需要更新的规则）
                if (requiredRule == null) {
                    // 情况a：多余的规则
                    vocabularyIdsToDelete.add(cloudConfig.getVocabularyId());
                    logger.debug("标记删除多余的规则，pattern: {}, vocabularyId: {}", pattern, cloudConfig.getVocabularyId());
                } else {
                    // 情况b：检查内容是否一致
                    boolean isConsistent = Objects.equals(requiredRule.getRuleValue(), cloudConfig.getRuleValue())
                            && Objects.equals(requiredRule.getRuleType(), cloudConfig.getRuleType());
                    if (!isConsistent) {
                        // 内容不一致，需要删除后重建
                        vocabularyIdsToDelete.add(cloudConfig.getVocabularyId());
                        logger.debug("标记删除需要更新的规则，pattern: {}, vocabularyId: {}", pattern, cloudConfig.getVocabularyId());
                    } else {
                        logger.debug("保留相同的规则，pattern: {}, vocabularyId: {}", pattern, cloudConfig.getVocabularyId());
                    }
                }
            }

            // 5. 找出需要创建的规则列表
            List<ReadingRule> rulesToCreate = new ArrayList<>();
            for (ReadingRule requiredRule : rules) {
                String pattern = requiredRule.getPattern();
                VocabularyConfig cloudConfig = cloudRuleMap.get(pattern);

                // 需要创建的情况：
                // a) 需要的规则中存在但云端不存在（新增的规则）
                // b) 需要的规则中存在且云端也存在但内容不一致（需要更新的规则，先删后建）
                if (cloudConfig == null) {
                    // 情况a：新增的规则
                    rulesToCreate.add(requiredRule);
                    logger.debug("标记创建新增的规则，pattern: {}", pattern);
                } else {
                    // 情况b：检查内容是否一致
                    boolean isConsistent = Objects.equals(requiredRule.getRuleValue(), cloudConfig.getRuleValue())
                            && Objects.equals(requiredRule.getRuleType(), cloudConfig.getRuleType());
                    if (!isConsistent) {
                        // 内容不一致，需要创建（删除已在步骤4中处理）
                        rulesToCreate.add(requiredRule);
                        logger.debug("标记创建需要更新的规则，pattern: {}", pattern);
                    }
                    // 如果内容一致，则不需要任何操作，已在步骤4中保留
                }
            }

            // 6. 批量删除需要删除的规则
            if (!vocabularyIdsToDelete.isEmpty()) {
                deleteVocabularyConfigs(vocabularyIdsToDelete);
                logger.info("批量删除华为云自定义读法规则成功，共删除 {} 条规则", vocabularyIdsToDelete.size());
            } else {
                logger.info("无需删除的规则");
            }

            // 7. 创建需要创建的规则
            int createdCount = 0;
            for (ReadingRule rule : rulesToCreate) {
                try {
                    String vocabularyId = createVocabularyConfig(rule.getPattern(), rule.getRuleValue(), rule.getRuleType());
                    createdCount++;
                    logger.info("创建华为云自定义读法规则成功，vocabularyId: {}, pattern: {}, ruleValue: {}, ruleType: {}", 
                            vocabularyId, rule.getPattern(), rule.getRuleValue(), rule.getRuleType());
                } catch (Exception e) {
                    logger.error("创建华为云自定义读法规则失败，pattern: {}, ruleValue: {}, ruleType: {}，继续处理", 
                            rule.getPattern(), rule.getRuleValue(), rule.getRuleType(), e);
                }
            }

            int preservedCount = existingConfigs.size() - vocabularyIdsToDelete.size();
            logger.info("批量更新华为云自定义读法规则完成，共处理 {} 条规则：删除 {} 条，创建 {} 条，保留 {} 条", 
                    rules.size(), vocabularyIdsToDelete.size(), createdCount, preservedCount);
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

            // 构建请求
            ListTtscVocabularyConfigsRequest request = new ListTtscVocabularyConfigsRequest();
            //设置规则组ID，不设置的话，无法查到读法配置
            request.setGroupId(huaweiCloudVocabularyGroupId);
            ListTtscVocabularyConfigsResponse response = client.listTtscVocabularyConfigs(request);

            // 获取响应体，然后获取vocabularyConfigs列表
            List<VocabularyConfig> configs = new ArrayList<>();
            if (response.getData() != null) {
                // 遍历vocabularyConfigs列表
                for (com.huaweicloud.sdk.metastudio.v1.model.VocabularyConfig info : response.getData()) {
                    VocabularyConfig config = new VocabularyConfig();
                    config.setVocabularyId(info.getId());
                    config.setPattern(info.getKey());
                    config.setRuleValue(info.getValue());
                    
                    // 提取并转换华为云类型为本地ruleType
                    if (info.getType() != null) {
                        String huaweiCloudType = info.getType();
                        Integer ruleType = ReadingRuleType.getCodeFromHuaweiCloudType(huaweiCloudType);
                        config.setRuleType(ruleType);
                    }
                    
                    configs.add(config);
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
    private String doCreateVocabularyConfig(MetaStudioClient client, String pattern, String ruleValue, Integer ruleType) {
        try {
            // 构建请求
            CreateTtscVocabularyConfigsRequest request = new CreateTtscVocabularyConfigsRequest();
            SaveTtscVocabularyConfigsRequestBody body = new SaveTtscVocabularyConfigsRequestBody();
            
            // 使用枚举获取华为云类型
            String huaweiCloudType = ReadingRuleType.getHuaweiCloudType(ruleType);
            if (huaweiCloudType == null) {
                throw new IllegalArgumentException("不支持的ruleType: " + ruleType + "，支持的值为：1-数字英文，2-音标调整，3-专有词汇");
            }
            
            // 设置body参数
            body.withKey(pattern)
                .withValue(ruleValue)
                .withType(SaveTtscVocabularyConfigsRequestBody.TypeEnum.fromValue(huaweiCloudType))
                .withGroupId(huaweiCloudVocabularyGroupId);
            request.withBody(body);
            
            // 调用SDK API
            CreateTtscVocabularyConfigsResponse response = client.createTtscVocabularyConfigs(request);
            
            // 获取vocabularyId
            String vocabularyId = response.getId();
            
            if (!StringUtils.hasText(vocabularyId)) {
                throw new RuntimeException("创建华为云自定义读法规则失败：未返回vocabularyId");
            }

            logger.info("创建华为云自定义读法规则成功，vocabularyId: {}, pattern: {}, ruleValue: {}, ruleType: {}", 
                    vocabularyId, pattern, ruleValue, ruleType);
            return vocabularyId;
        } catch (ConnectionException e) {
            logger.error("创建华为云自定义读法规则连接异常，pattern: {}, ruleValue: {}, ruleType: {}", pattern, ruleValue, ruleType, e);
            throw new RuntimeException("创建华为云自定义读法规则连接异常: " + e.getMessage(), e);
        } catch (RequestTimeoutException e) {
            logger.error("创建华为云自定义读法规则请求超时，pattern: {}, ruleValue: {}, ruleType: {}", pattern, ruleValue, ruleType, e);
            throw new RuntimeException("创建华为云自定义读法规则请求超时: " + e.getMessage(), e);
        } catch (ServiceResponseException e) {
            logger.error("创建华为云自定义读法规则服务响应异常，pattern: {}, ruleValue: {}, ruleType: {}, HTTP状态码={}, 错误码={}, 错误信息={}", 
                    pattern, ruleValue, ruleType, e.getHttpStatusCode(), e.getErrorCode(), e.getErrorMsg());
            throw new RuntimeException("创建华为云自定义读法规则服务响应异常: " + e.getErrorMsg(), e);
        } catch (Exception e) {
            logger.error("创建华为云自定义读法规则异常，pattern: {}, ruleValue: {}, ruleType: {}", pattern, ruleValue, ruleType, e);
            throw new RuntimeException("创建华为云自定义读法规则异常: " + e.getMessage(), e);
        }
    }

    /**
     * 执行删除自定义读法规则
     */
    private void doDeleteVocabularyConfig(MetaStudioClient client, String vocabularyId) {
        try {
            // 构建请求
            DeleteTtscVocabularyConfigsRequest request = new DeleteTtscVocabularyConfigsRequest();
            DeleteTtscVocabularyConfigsRequestBody body = new DeleteTtscVocabularyConfigsRequestBody();
            
            // 创建ID列表
            List<String> idList = new ArrayList<>();
            idList.add(vocabularyId);
            
            // 设置请求体
            body.withId(idList);
            request.withBody(body);
            
            // 调用SDK API
            client.deleteTtscVocabularyConfigs(request);

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

    /**
     * 执行批量删除自定义读法规则
     */
    private void doBatchDeleteVocabularyConfigs(MetaStudioClient client, List<String> vocabularyIds) {
        if (vocabularyIds == null || vocabularyIds.isEmpty()) {
            logger.warn("批量删除华为云自定义读法规则：vocabularyIds为空，跳过删除");
            return;
        }

        try {
            // 构建请求
            DeleteTtscVocabularyConfigsRequest request = new DeleteTtscVocabularyConfigsRequest();
            DeleteTtscVocabularyConfigsRequestBody body = new DeleteTtscVocabularyConfigsRequestBody();
            
            // 设置请求体，直接使用传入的ID列表
            body.withId(vocabularyIds);
            request.withBody(body);
            
            // 调用SDK API
            client.deleteTtscVocabularyConfigs(request);

            logger.info("批量删除华为云自定义读法规则成功，共删除 {} 条规则", vocabularyIds.size());
        } catch (ConnectionException e) {
            logger.error("批量删除华为云自定义读法规则连接异常，vocabularyIds: {}", vocabularyIds, e);
            throw new RuntimeException("批量删除华为云自定义读法规则连接异常: " + e.getMessage(), e);
        } catch (RequestTimeoutException e) {
            logger.error("批量删除华为云自定义读法规则请求超时，vocabularyIds: {}", vocabularyIds, e);
            throw new RuntimeException("批量删除华为云自定义读法规则请求超时: " + e.getMessage(), e);
        } catch (ServiceResponseException e) {
            logger.error("批量删除华为云自定义读法规则服务响应异常，vocabularyIds: {}, HTTP状态码={}, 错误码={}, 错误信息={}", 
                    vocabularyIds, e.getHttpStatusCode(), e.getErrorCode(), e.getErrorMsg());
            throw new RuntimeException("批量删除华为云自定义读法规则服务响应异常: " + e.getErrorMsg(), e);
        } catch (Exception e) {
            logger.error("批量删除华为云自定义读法规则异常，vocabularyIds: {}", vocabularyIds, e);
            throw new RuntimeException("批量删除华为云自定义读法规则异常: " + e.getMessage(), e);
        }
    }
}
