package com.yunting.service;

import com.yunting.model.ReadingRule;

import java.util.List;

/**
 * 华为云自定义读法服务
 * 负责管理华为云租户级自定义读法规则
 */
public interface HuaweiCloudVocabularyService {

    /**
     * 查询华为云上的自定义读法规则列表
     * 
     * @return 规则列表，每个规则包含vocabularyId、pattern、ruleValue
     */
    List<VocabularyConfig> listVocabularyConfigs();

    /**
     * 创建自定义读法规则
     * 
     * @param pattern 匹配模式
     * @param ruleValue 规则值
     * @return vocabularyId
     */
    String createVocabularyConfig(String pattern, String ruleValue);

    /**
     * 删除自定义读法规则
     * 
     * @param vocabularyId 规则ID
     */
    void deleteVocabularyConfig(String vocabularyId);

    /**
     * 批量更新自定义读法规则（先删除所有，再创建新的）
     * 
     * @param rules 规则列表
     */
    void updateVocabularyConfigs(List<ReadingRule> rules);

    /**
     * 华为云自定义读法配置DTO
     */
    class VocabularyConfig {
        private String vocabularyId;
        private String pattern;
        private String ruleValue;

        public String getVocabularyId() {
            return vocabularyId;
        }

        public void setVocabularyId(String vocabularyId) {
            this.vocabularyId = vocabularyId;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getRuleValue() {
            return ruleValue;
        }

        public void setRuleValue(String ruleValue) {
            this.ruleValue = ruleValue;
        }
    }
}
