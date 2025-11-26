-- 云听后端服务 - 基础数据初始化
USE `yunting`;

-- 初始化音色配置
INSERT INTO `voice_configs` (`voice_id`, `voice_name`, `voice_type`, `language`, `is_recommended`, `sort_order`)
VALUES
    ('xiaoyan', '小燕', '女声', 'zh-CN', 1, 1),
    ('xiaoyun', '小云', '女声', 'zh-CN', 1, 2),
    ('xiaofeng', '小峰', '男声', 'zh-CN', 1, 3)
ON DUPLICATE KEY UPDATE
    `voice_name` = VALUES(`voice_name`),
    `voice_type` = VALUES(`voice_type`),
    `language` = VALUES(`language`),
    `is_recommended` = VALUES(`is_recommended`),
    `sort_order` = VALUES(`sort_order`);

