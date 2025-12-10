-- 将 reading_rules 表的 rule_type 字段从 VARCHAR 改为 INT
-- 迁移步骤：
-- 1. 添加临时列
-- 2. 数据迁移：将字符串值转换为整数
-- 3. 删除原列
-- 4. 重命名新列

-- 步骤1: 添加临时列
ALTER TABLE `reading_rules` 
ADD COLUMN `rule_type_new` INT NULL COMMENT '规范类型：1-数字英文，2-音标调整，3-专有词汇' AFTER `rule_type`;

-- 步骤2: 数据迁移
-- 将现有字符串值转换为整数
UPDATE `reading_rules` 
SET `rule_type_new` = CASE 
    WHEN `rule_type` IN ('数字英文', '数字读法', 'CHINESE_G2P', 'SAY_AS') THEN 1
    WHEN `rule_type` IN ('音标调整', 'PHONETIC_SYMBOL') THEN 2
    WHEN `rule_type` IN ('专有词汇', 'ALIAS', 'CONTINUUM') THEN 3
    ELSE NULL  -- 无法识别的值设为 NULL，需要手动处理
END;

-- 检查是否有未迁移的数据
-- SELECT * FROM reading_rules WHERE rule_type_new IS NULL;

-- 步骤3: 删除原列
ALTER TABLE `reading_rules` 
DROP COLUMN `rule_type`;

-- 步骤4: 重命名新列
ALTER TABLE `reading_rules` 
CHANGE COLUMN `rule_type_new` `rule_type` INT NOT NULL COMMENT '规范类型：1-数字英文，2-音标调整，3-专有词汇';
