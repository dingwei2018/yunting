-- 云听后端服务 - 数据库初始化脚本
-- MySQL 8.0+, 字符集 utf8mb4
CREATE DATABASE IF NOT EXISTS `yunting`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `yunting`;

-- 临时禁用外键检查，避免创建表时的顺序问题
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for audio_merges
-- ----------------------------
DROP TABLE IF EXISTS `audio_merges`;
CREATE TABLE `audio_merges`  (
                                 `merge_id` bigint(0) NOT NULL AUTO_INCREMENT,
                                 `task_id` bigint(0) NOT NULL,
                                 `breaking_sentence_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                 `merged_audio_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
                                 `audio_duration` int(0) NULL DEFAULT NULL,
                                 `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '1-合并中，2-合并完成，3-合并失败',
                                 `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
                                 PRIMARY KEY (`merge_id`) USING BTREE,
                                 INDEX `idx_audio_task_id`(`task_id`) USING BTREE,
                                 INDEX `idx_audio_status`(`status`) USING BTREE,
                                 CONSTRAINT `fk_audio_merges_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`task_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '语音合并记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for breaking_sentences
-- ----------------------------
DROP TABLE IF EXISTS `breaking_sentences`;
CREATE TABLE `breaking_sentences`  (
                                       `breaking_sentence_id` bigint(0) NOT NULL AUTO_INCREMENT,
                                       `task_id` bigint(0) NOT NULL,
                                       `original_sentence_id` bigint(0) NULL DEFAULT NULL,
                                       `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                       `char_count` int(0) NOT NULL,
                                       `sequence` int(0) NOT NULL,
                                       `synthesis_status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '0-未合成，1-合成中，2-已合成，3-合成失败',
                                       `audio_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
                                       `audio_duration` int(0) NULL DEFAULT NULL,
                                       `ssml` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
                                       `job_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
                                       `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
                                       PRIMARY KEY (`breaking_sentence_id`) USING BTREE,
                                       INDEX `idx_breaking_task_id`(`task_id`) USING BTREE,
                                       INDEX `idx_original_sentence_id`(`original_sentence_id`) USING BTREE,
                                       INDEX `idx_breaking_sequence`(`task_id`, `sequence`) USING BTREE,
                                       INDEX `idx_synthesis_status`(`synthesis_status`) USING BTREE,
                                       CONSTRAINT `fk_breaking_sentences_original` FOREIGN KEY (`original_sentence_id`) REFERENCES `original_sentences` (`original_sentence_id`) ON DELETE SET NULL ON UPDATE RESTRICT,
                                       CONSTRAINT `fk_breaking_sentences_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`task_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 43 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '断句表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for original_sentences
-- ----------------------------
DROP TABLE IF EXISTS `original_sentences`;
CREATE TABLE `original_sentences`  (
                                       `original_sentence_id` bigint(0) NOT NULL AUTO_INCREMENT,
                                       `task_id` bigint(0) NOT NULL,
                                       `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                       `char_count` int(0) NOT NULL,
                                       `sequence` int(0) NOT NULL,
                                       `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       PRIMARY KEY (`original_sentence_id`) USING BTREE,
                                       INDEX `idx_task_id`(`task_id`) USING BTREE,
                                       INDEX `idx_sequence`(`task_id`, `sequence`) USING BTREE,
                                       CONSTRAINT `fk_original_sentences_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`task_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 43 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '拆句表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for pause_settings
-- ----------------------------
DROP TABLE IF EXISTS `pause_settings`;
CREATE TABLE `pause_settings`  (
                                   `pause_id` bigint(0) NOT NULL AUTO_INCREMENT,
                                   `breaking_sentence_id` bigint(0) NOT NULL,
                                   `position` int(0) NOT NULL,
                                   `duration` int(0) NOT NULL,
                                   `type` tinyint(0) NOT NULL,
                                   `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   PRIMARY KEY (`pause_id`) USING BTREE,
                                   INDEX `idx_pause_breaking_sentence_id`(`breaking_sentence_id`) USING BTREE,
                                   CONSTRAINT `fk_pause_breaking` FOREIGN KEY (`breaking_sentence_id`) REFERENCES `breaking_sentences` (`breaking_sentence_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '停顿静音合成设置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for polyphonic_settings
-- ----------------------------
DROP TABLE IF EXISTS `polyphonic_settings`;
CREATE TABLE `polyphonic_settings`  (
                                        `polyphonic_id` bigint(0) NOT NULL AUTO_INCREMENT,
                                        `breaking_sentence_id` bigint(0) NOT NULL,
                                        `word` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                        `position` int(0) NOT NULL,
                                        `pronunciation` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                        `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        PRIMARY KEY (`polyphonic_id`) USING BTREE,
                                        INDEX `idx_polyphonic_breaking_sentence_id`(`breaking_sentence_id`) USING BTREE,
                                        CONSTRAINT `fk_polyphonic_breaking` FOREIGN KEY (`breaking_sentence_id`) REFERENCES `breaking_sentences` (`breaking_sentence_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '多音字合成设置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for prosody_settings
-- ----------------------------
DROP TABLE IF EXISTS `prosody_settings`;
CREATE TABLE `prosody_settings`  (
                                     `prosody_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '局部语速ID',
                                     `breaking_sentence_id` bigint(0) NOT NULL COMMENT '断句ID',
                                     `begin_position` int(0) NOT NULL COMMENT '标签开始位置',
                                     `end_position` int(0) NOT NULL COMMENT '标签结束位置',
                                     `rate` int(0) NOT NULL COMMENT '当取值为\"100\"时，表示一个成年人正常的语速，约为250字/分钟。50表示0.5倍语速，100表示正常语速，200表示2倍语速。取值范围：50~200。',
                                     `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     PRIMARY KEY (`prosody_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '局部语速合成设置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for reading_rule_applications
-- ----------------------------
DROP TABLE IF EXISTS `reading_rule_applications`;
CREATE TABLE `reading_rule_applications`  (
                                              `application_id` bigint(0) NOT NULL AUTO_INCREMENT,
                                              `rule_id` bigint(0) NOT NULL,
                                              `from_id` bigint(0) NOT NULL,
                                              `type` tinyint(0) NOT NULL COMMENT '1，任务，2，断句',
                                              `is_open` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否开启：0-关闭，1-打开',
                                              `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              PRIMARY KEY (`application_id`) USING BTREE,
                                              UNIQUE INDEX `uk_rule_from`(`rule_id`, `from_id`) USING BTREE,
                                              INDEX `idx_application_rule_id`(`rule_id`) USING BTREE,
                                              INDEX `idx_application_from_id`(`from_id`) USING BTREE,
                                              CONSTRAINT `fk_application_from` FOREIGN KEY (`from_id`) REFERENCES `breaking_sentences` (`breaking_sentence_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
                                              CONSTRAINT `fk_application_rule` FOREIGN KEY (`rule_id`) REFERENCES `reading_rules` (`rule_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '阅读规范应用表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for reading_rules
-- ----------------------------
DROP TABLE IF EXISTS `reading_rules`;
CREATE TABLE `reading_rules`  (
                                  `rule_id` bigint(0) NOT NULL AUTO_INCREMENT,
                                  `pattern` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                  `rule_type` int(0) NOT NULL COMMENT '规范类型：1-数字英文，2-音标调整，3-专有词汇',
                                  `rule_value` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
                                  PRIMARY KEY (`rule_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '阅读规范表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for synthesis_settings
-- ----------------------------
DROP TABLE IF EXISTS `synthesis_settings`;
CREATE TABLE `synthesis_settings`  (
                                       `setting_id` bigint(0) NOT NULL AUTO_INCREMENT,
                                       `breaking_sentence_id` bigint(0) NOT NULL,
                                       `voice_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                       `voice_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
                                       `speech_rate` int(0) NOT NULL DEFAULT 0,
                                       `volume` int(0) NOT NULL DEFAULT 0,
                                       `pitch` int(0) NOT NULL DEFAULT 0,
                                       `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
                                       PRIMARY KEY (`setting_id`) USING BTREE,
                                       UNIQUE INDEX `uk_breaking_sentence_id`(`breaking_sentence_id`) USING BTREE,
                                       INDEX `idx_voice_id`(`voice_id`) USING BTREE,
                                       CONSTRAINT `fk_synthesis_breaking` FOREIGN KEY (`breaking_sentence_id`) REFERENCES `breaking_sentences` (`breaking_sentence_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 72 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '合成设置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tasks
-- ----------------------------
DROP TABLE IF EXISTS `tasks`;
CREATE TABLE `tasks`  (
                          `task_id` bigint(0) NOT NULL AUTO_INCREMENT,
                          `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                          `char_count` int(0) NOT NULL,
                          `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '0，拆句完成，1，语音合成中，2，语音合成成功，3，语音合成失败，4，语音合并中，5，语音合并成功，6，语音合并失败',
                          `merged_audio_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
                          `merged_audio_duration` int(0) NULL DEFAULT NULL,
                          `breaking_standard_id` int(0) NULL DEFAULT NULL,
                          `char_count_limit` int(0) NULL DEFAULT NULL,
                          `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
                          PRIMARY KEY (`task_id`) USING BTREE,
                          INDEX `idx_status`(`status`) USING BTREE,
                          INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for voice_configs
-- ----------------------------
DROP TABLE IF EXISTS `voice_configs`;
CREATE TABLE `voice_configs`  (
                                  `voice_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                  `voice_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                  `voice_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                  `language` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                  `avatar_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
                                  `is_recommended` tinyint(0) NOT NULL DEFAULT 0,
                                  `sort_order` int(0) NOT NULL DEFAULT 0,
                                  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
                                  PRIMARY KEY (`voice_id`) USING BTREE,
                                  INDEX `idx_voice_is_recommended`(`is_recommended`) USING BTREE,
                                  INDEX `idx_voice_language`(`language`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '音色资源表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

