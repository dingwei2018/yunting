package com.yunting.service.impl;

import com.yunting.dto.synthesis.TtsSynthesisRequest;
import com.yunting.model.ReadingRule;
import com.yunting.service.HuaweiCloudVocabularyService;
import com.yunting.service.ReadingRuleAggregationService;
import com.yunting.service.TtsSynthesisCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TTS合成协调器实现
 */
@Service
public class TtsSynthesisCoordinatorImpl implements TtsSynthesisCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(TtsSynthesisCoordinatorImpl.class);

    private final ReadingRuleAggregationService readingRuleAggregationService;
    private final HuaweiCloudVocabularyService huaweiCloudVocabularyService;

    @Value("${huaweicloud.vocabulary.update.timeout:30}")
    private int vocabularyUpdateTimeout;

    public TtsSynthesisCoordinatorImpl(ReadingRuleAggregationService readingRuleAggregationService,
                                      HuaweiCloudVocabularyService huaweiCloudVocabularyService) {
        this.readingRuleAggregationService = readingRuleAggregationService;
        this.huaweiCloudVocabularyService = huaweiCloudVocabularyService;
    }

    @Override
    public void ensureVocabularyConfigsAndSynthesize(TtsSynthesisRequest request, 
                                                     Runnable createTtsJobCallback) {
        Long breakingSentenceId = request.getBreakingSentenceId();
        logger.info("开始确保阅读规则已同步，breakingSentenceId: {}", breakingSentenceId);

        try {
            // 1. 汇总断句需要的阅读规则
            List<ReadingRule> requiredRules = readingRuleAggregationService.aggregateReadingRules(breakingSentenceId);
            logger.info("汇总断句需要的阅读规则，breakingSentenceId: {}, 规则数量: {}", 
                    breakingSentenceId, requiredRules.size());

            // 2. 查询华为云上的规则
            List<HuaweiCloudVocabularyService.VocabularyConfig> cloudConfigs = 
                    huaweiCloudVocabularyService.listVocabularyConfigs();
            logger.info("查询华为云上的自定义读法规则，数量: {}", cloudConfigs.size());

            // 3. 对比规则是否一致
            boolean isConsistent = compareRules(requiredRules, cloudConfigs);

            if (!isConsistent) {
                logger.info("阅读规则不一致，需要更新，breakingSentenceId: {}", breakingSentenceId);
                
                // 4. 更新华为云上的规则
                try {
                    huaweiCloudVocabularyService.updateVocabularyConfigs(requiredRules);
                    logger.info("更新华为云自定义读法规则成功，breakingSentenceId: {}, 规则数量: {}", 
                            breakingSentenceId, requiredRules.size());
                } catch (Exception e) {
                    logger.error("更新华为云自定义读法规则失败，但继续执行合成，breakingSentenceId: {}", 
                            breakingSentenceId, e);
                    // 不抛出异常，继续执行合成
                }
            } else {
                logger.info("阅读规则一致，无需更新，breakingSentenceId: {}", breakingSentenceId);
            }

            // 6. 执行TTS合成
            createTtsJobCallback.run();
            logger.info("TTS合成请求已提交，breakingSentenceId: {}", breakingSentenceId);

        } catch (Exception e) {
            logger.error("确保阅读规则同步失败，但继续执行合成，breakingSentenceId: {}", breakingSentenceId, e);
            // 不抛出异常，继续执行合成
            createTtsJobCallback.run();
        }
    }

    /**
     * 对比规则是否一致
     * 只有当 type、key、value 完全一样时，才认为一致
     * 
     * @param requiredRules 需要的规则列表
     * @param cloudConfigs 华为云上的规则列表
     * @return 是否一致
     */
    private boolean compareRules(List<ReadingRule> requiredRules, 
                                List<HuaweiCloudVocabularyService.VocabularyConfig> cloudConfigs) {
        // 构建需要的规则Map（pattern -> ReadingRule），用于完整对比
        Map<String, ReadingRule> requiredRuleMap = requiredRules.stream()
                .collect(Collectors.toMap(
                        ReadingRule::getPattern,
                        rule -> rule,
                        (v1, v2) -> v1  // 如果有重复，保留第一个
                ));

        // 构建华为云上的规则Map（pattern -> VocabularyConfig），用于完整对比
        Map<String, HuaweiCloudVocabularyService.VocabularyConfig> cloudRuleMap = cloudConfigs.stream()
                .collect(Collectors.toMap(
                        HuaweiCloudVocabularyService.VocabularyConfig::getPattern,
                        config -> config,
                        (v1, v2) -> v1  // 如果有重复，保留第一个
                ));

        // 对比两个Map的数量是否一致
        if (requiredRuleMap.size() != cloudRuleMap.size()) {
            logger.debug("规则数量不一致，需要的: {}, 华为云上的: {}", 
                    requiredRuleMap.size(), cloudRuleMap.size());
            return false;
        }

        // 对比每个规则的 type、key、value 是否完全一致
        for (Map.Entry<String, ReadingRule> entry : requiredRuleMap.entrySet()) {
            String pattern = entry.getKey();
            ReadingRule requiredRule = entry.getValue();
            HuaweiCloudVocabularyService.VocabularyConfig cloudConfig = cloudRuleMap.get(pattern);

            // 如果云端没有这个pattern，不一致
            if (cloudConfig == null) {
                logger.debug("规则不存在于云端，pattern: {}", pattern);
                return false;
            }

            // 对比 ruleValue
            if (!Objects.equals(requiredRule.getRuleValue(), cloudConfig.getRuleValue())) {
                logger.debug("规则值不一致，pattern: {}, 需要的: {}, 华为云上的: {}", 
                        pattern, requiredRule.getRuleValue(), cloudConfig.getRuleValue());
                return false;
            }

            // 对比 ruleType
            if (!Objects.equals(requiredRule.getRuleType(), cloudConfig.getRuleType())) {
                logger.debug("规则类型不一致，pattern: {}, 需要的: {}, 华为云上的: {}", 
                        pattern, requiredRule.getRuleType(), cloudConfig.getRuleType());
                return false;
            }
        }

        return true;
    }
}
