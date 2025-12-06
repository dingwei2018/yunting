-- 云听后端服务 - 数据库初始化脚本
-- MySQL 8.0+, 字符集 utf8mb4
CREATE DATABASE IF NOT EXISTS `yunting`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `yunting`;

-- 1. 任务表
CREATE TABLE IF NOT EXISTS `tasks` (
    `task_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `content` TEXT NOT NULL,
    `char_count` INT NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `merged_audio_url` VARCHAR(500) NULL,
    `merged_audio_duration` INT NULL,
    `breaking_standard_id` INT NULL,
    `char_count_limit` INT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 原始拆句表
CREATE TABLE IF NOT EXISTS `original_sentences` (
    `original_sentence_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `char_count` INT NOT NULL,
    `sequence` INT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_sequence` (`task_id`, `sequence`),
    CONSTRAINT `fk_original_sentences_task`
        FOREIGN KEY (`task_id`) REFERENCES `tasks` (`task_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 断句表
CREATE TABLE IF NOT EXISTS `breaking_sentences` (
    `breaking_sentence_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL,
    `original_sentence_id` BIGINT NULL,
    `content` TEXT NOT NULL,
    `char_count` INT NOT NULL,
    `sequence` INT NOT NULL,
    `synthesis_status` TINYINT NOT NULL DEFAULT 0,
    `audio_url` VARCHAR(500) NULL,
    `audio_duration` INT NULL,
    `ssml` TEXT NULL,
    `job_id` VARCHAR(100) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_breaking_task_id` (`task_id`),
    INDEX `idx_original_sentence_id` (`original_sentence_id`),
    INDEX `idx_breaking_sequence` (`task_id`, `sequence`),
    INDEX `idx_synthesis_status` (`synthesis_status`),
    CONSTRAINT `fk_breaking_sentences_task`
        FOREIGN KEY (`task_id`) REFERENCES `tasks` (`task_id`)
        ON DELETE CASCADE,
    CONSTRAINT `fk_breaking_sentences_original`
        FOREIGN KEY (`original_sentence_id`) REFERENCES `original_sentences` (`original_sentence_id`)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 合成设置表
CREATE TABLE IF NOT EXISTS `synthesis_settings` (
    `setting_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `breaking_sentence_id` BIGINT NOT NULL,
    `voice_id` VARCHAR(50) NOT NULL,
    `voice_name` VARCHAR(100) NULL,
    `speech_rate` INT NOT NULL DEFAULT 0,
    `volume` INT NOT NULL DEFAULT 0,
    `pitch` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_breaking_sentence_id` (`breaking_sentence_id`),
    INDEX `idx_voice_id` (`voice_id`),
    CONSTRAINT `fk_synthesis_breaking`
        FOREIGN KEY (`breaking_sentence_id`) REFERENCES `breaking_sentences` (`breaking_sentence_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 停顿设置表
CREATE TABLE IF NOT EXISTS `pause_settings` (
    `pause_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `breaking_sentence_id` BIGINT NOT NULL,
    `position` INT NOT NULL,
    `duration` INT NOT NULL,
    `type` TINYINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_pause_breaking_sentence_id` (`breaking_sentence_id`),
    CONSTRAINT `fk_pause_breaking`
        FOREIGN KEY (`breaking_sentence_id`) REFERENCES `breaking_sentences` (`breaking_sentence_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 多音字设置表
CREATE TABLE IF NOT EXISTS `polyphonic_settings` (
    `polyphonic_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `breaking_sentence_id` BIGINT NOT NULL,
    `character` VARCHAR(10) NOT NULL,
    `position` INT NOT NULL,
    `pronunciation` VARCHAR(50) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_polyphonic_breaking_sentence_id` (`breaking_sentence_id`),
    CONSTRAINT `fk_polyphonic_breaking`
        FOREIGN KEY (`breaking_sentence_id`) REFERENCES `breaking_sentences` (`breaking_sentence_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 局部语速设置表
CREATE TABLE IF NOT EXISTS `prosody_settings` (
    `prosody_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `breaking_sentence_id` BIGINT NOT NULL,
    `begin_position` INT NOT NULL COMMENT '标签开始位置',
    `end_position` INT NOT NULL COMMENT '标签结束位置',
    `rate` INT NOT NULL COMMENT '语速：50表示0.5倍语速，100表示正常语速，200表示2倍语速。取值范围：50~200',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_prosody_breaking_sentence_id` (`breaking_sentence_id`),
    CONSTRAINT `fk_prosody_breaking`
        FOREIGN KEY (`breaking_sentence_id`) REFERENCES `breaking_sentences` (`breaking_sentence_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. 阅读规范表
CREATE TABLE IF NOT EXISTS `reading_rules` (
    `rule_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NULL,
    `pattern` VARCHAR(200) NOT NULL,
    `rule_type` VARCHAR(50) NOT NULL,
    `rule_value` VARCHAR(200) NOT NULL,
    `scope` TINYINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_reading_task_id` (`task_id`),
    INDEX `idx_scope` (`scope`),
    CONSTRAINT `fk_reading_rules_task`
        FOREIGN KEY (`task_id`) REFERENCES `tasks` (`task_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. 阅读规范应用表
CREATE TABLE IF NOT EXISTS `reading_rule_applications` (
    `application_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `rule_id` BIGINT NOT NULL,
    `breaking_sentence_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_rule_breaking` (`rule_id`, `breaking_sentence_id`),
    INDEX `idx_application_rule_id` (`rule_id`),
    INDEX `idx_application_breaking_sentence_id` (`breaking_sentence_id`),
    CONSTRAINT `fk_application_rule`
        FOREIGN KEY (`rule_id`) REFERENCES `reading_rules` (`rule_id`)
        ON DELETE CASCADE,
    CONSTRAINT `fk_application_breaking`
        FOREIGN KEY (`breaking_sentence_id`) REFERENCES `breaking_sentences` (`breaking_sentence_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. 音频合并记录表
CREATE TABLE IF NOT EXISTS `audio_merges` (
    `merge_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL,
    `breaking_sentence_ids` TEXT NOT NULL,
    `merged_audio_url` VARCHAR(500) NULL,
    `audio_duration` INT NULL,
    `status` TINYINT NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_audio_task_id` (`task_id`),
    INDEX `idx_audio_status` (`status`),
    CONSTRAINT `fk_audio_merges_task`
        FOREIGN KEY (`task_id`) REFERENCES `tasks` (`task_id`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. 音色配置表
CREATE TABLE IF NOT EXISTS `voice_configs` (
    `voice_id` VARCHAR(50) PRIMARY KEY,
    `voice_name` VARCHAR(100) NOT NULL,
    `voice_type` VARCHAR(20) NOT NULL,
    `language` VARCHAR(10) NOT NULL,
    `is_recommended` TINYINT NOT NULL DEFAULT 0,
    `sort_order` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_voice_is_recommended` (`is_recommended`),
    INDEX `idx_voice_language` (`language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


