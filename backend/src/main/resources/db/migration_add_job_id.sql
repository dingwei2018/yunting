-- 数据库迁移脚本：在 breaking_sentences 表中添加 job_id 字段
-- 执行时间：2025-01-XX
-- 说明：为支持华为云TTS回调功能，添加job_id字段用于关联TTS任务

USE `yunting`;

-- 添加 job_id 字段
ALTER TABLE `breaking_sentences`
ADD COLUMN `job_id` VARCHAR(100) NULL COMMENT '华为云TTS任务ID' AFTER `ssml`;

-- 添加索引以提高查询性能
ALTER TABLE `breaking_sentences`
ADD INDEX `idx_job_id` (`job_id`);

