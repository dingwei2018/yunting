-- 云听后端服务 - 基础数据初始化
USE `yunting`;

-- 初始化音色配置
INSERT INTO `voice_configs` (`voice_id`, `voice_name`, `voice_type`, `language`, `is_recommended`, `sort_order`)
VALUES
    ('c41f12c125f24c834ed3ae7c1fdae456', '闲聊女声', '女声', 'zh-CN', 1, 1),
    ('2536eb2c89bda92505b76d4d90d8637b', '直播女声', '女声', 'zh-CN', 1, 2),
    ('305442831dba3f3861e8d50fc8d69865', '悬疑男声', '男声', 'zh-CN', 1, 3)
ON DUPLICATE KEY UPDATE
    `voice_name` = VALUES(`voice_name`),
    `voice_type` = VALUES(`voice_type`),
    `language` = VALUES(`language`),
    `is_recommended` = VALUES(`is_recommended`),
    `sort_order` = VALUES(`sort_order`);

