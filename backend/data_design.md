北京云听 - 数据库设计文档

# 1. 数据库概述

本文档描述了"北京云听"文本转语音（TTS）系统的数据库结构设计。系统支持文本输入、自动拆句、语音合成、音频编辑等功能。

# 2. 数据库表设计

## 2.1 任务表 (tasks)

存储文本拆句任务信息。每次请求一个文本拆句视为一个任务。

| **字段名**      | **类型** | **约束**                                        | **说明**                             |
| --------------------- | -------------- | ----------------------------------------------------- | ------------------------------------------ |
| id                    | BIGINT         | PRIMARY KEY, AUTO_INCREMENT                           | 任务ID                                     |
| content               | TEXT           | NOT NULL                                              | 原始文本内容（支持2000字）                 |
| char_count            | INT            | DEFAULT 0                                             | 字符数                                     |
| merged_audio_url      | VARCHAR(500)   |                                                       | 整个任务的合并音频URL                      |
| merged_audio_duration | INT            |                                                       | 整个任务的合并音频时长（毫秒）             |
| ssml                  | TEXT           |                                                       | 整个任务的SSML配置（包含所有编辑配置参数） |
| status                | TINYINT        | DEFAULT 1                                             | 状态：1-待拆句，2-已拆句，3-已完成         |
| created_at            | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP                             | 创建时间                                   |
| updated_at            | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间                                   |

**索引：**

·

•INDEX idx_status (status)

•INDEX idx_created_at (created_at)

·

## 2.2 句子表 (sentences)

存储自动拆句后的句子信息。每个任务拆句完成后会生成一串句子。

| 字段名           | 类型         | 约束                                                  | 说明                                                                             |
| ---------------- | ------------ | ----------------------------------------------------- | -------------------------------------------------------------------------------- |
| id               | BIGINT       | PRIMARY KEY, AUTO_INCREMENT                           | 句子ID                                                                           |
| task_id          | BIGINT       | NOT NULL, FOREIGN KEY                                 | 任务ID                                                                           |
| content          | TEXT         | NOT NULL                                              | 句子内容                                                                         |
| sequence         | INT          | NOT NULL                                              | 句子序号（从1开始）                                                              |
| char_count       | INT          | DEFAULT 0                                             | 字符数                                                                           |
| audio_url        | VARCHAR(500) |                                                       | 合成后的音频URL                                                                  |
| audio_duration   | INT          |                                                       | 音频时长（毫秒）                                                                 |
| ssml             | TEXT         |                                                       | 句子的SSML配置（包含该句子的所有编辑配置参数：语速、音量、音调、停顿、多音字等） |
| synthesis_status | TINYINT      | DEFAULT 0                                             | 合成状态：0-未合成，1-合成中，2-已合成，3-合成失败                               |
| created_at       | TIMESTAMP    | DEFAULT CURRENT_TIMESTAMP                             | 创建时间                                                                         |
| updated_at       | TIMESTAMP    | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间                                                                         |

 **索引：** ·

•INDEX idx_task_id (task_id)·

•INDEX idx_sequence (sequence)··

•INDEX idx_synthesis_status (synthesis_status)

·

## 2.3 合成设置表 (synthesis_settings)

存储每个句子的语音合成参数设置。

| 字段名      | 类型         | 约束                                                  | 说明                       |
| ----------- | ------------ | ----------------------------------------------------- | -------------------------- |
| id          | BIGINT       | PRIMARY KEY, AUTO_INCREMENT                           | 设置ID                     |
| sentence_id | BIGINT       | NOT NULL, FOREIGN KEY, UNIQUE                         | 句子ID                     |
| voice_id    | VARCHAR(50)  | NOT NULL                                              | 音色ID（华为云音色ID）     |
| voice_name  | VARCHAR(100) |                                                       | 音色名称                   |
| speech_rate | INT          | DEFAULT 0                                             | 语速（-500到500，0为正常） |
| volume      | INT          | DEFAULT 0                                             | 音量（-60到60，0为正常）   |
| pitch       | INT          | DEFAULT 0                                             | 音调（-500到500，0为正常） |
| created_at  | TIMESTAMP    | DEFAULT CURRENT_TIMESTAMP                             | 创建时间                   |
| updated_at  | TIMESTAMP    | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间                   |

**索引：**

•INDEX idx_sentence_id (sentence_id)

•INDEX idx_voice_id (voice_id)

·

## 2.4 停顿设置表 (pause_settings)

存储句子中的停顿设置。

| 字段名      | 类型      | 约束                        | 说明                      |
| ----------- | --------- | --------------------------- | ------------------------- |
| id          | BIGINT    | PRIMARY KEY, AUTO_INCREMENT | 停顿ID                    |
| sentence_id | BIGINT    | NOT NULL, FOREIGN KEY       | 句子ID                    |
| position    | INT       | NOT NULL                    | 停顿位置（字符位置）      |
| duration    | INT       | NOT NULL                    | 停顿时长（毫秒，如500ms） |
| type        | TINYINT   | DEFAULT 1                   | 类型：1-停顿，2-静音      |
| created_at  | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP   | 创建时间                  |

**索引：**

•INDEX idx_sentence_id (sentence_id)

•INDEX idx_position (position)

·

## 2.5 多音字设置表 (polyphonic_settings)

存储多音字的读音设置。

| 字段名        | 类型        | 约束                        | 说明               |
| ------------- | ----------- | --------------------------- | ------------------ |
| id            | BIGINT      | PRIMARY KEY, AUTO_INCREMENT | 设置ID             |
| sentence_id   | BIGINT      | NOT NULL, FOREIGN KEY       | 句子ID             |
| character     | VARCHAR(10) | NOT NULL                    | 字符               |
| position      | INT         | NOT NULL                    | 字符在句子中的位置 |
| pronunciation | VARCHAR(50) | NOT NULL                    | 读音（拼音）       |
| created_at    | TIMESTAMP   | DEFAULT CURRENT_TIMESTAMP   | 创建时间           |

**索引：**

•INDEX idx_sentence_id (sentence_id)

•INDEX idx_character (character)

·

## 2.6 阅读规范表 (reading_rules)

存储阅读规范设置（全局或任务级别）。

| 字段名     | 类型         | 约束                                                  | 说明                       |
| ---------- | ------------ | ----------------------------------------------------- | -------------------------- |
| id         | BIGINT       | PRIMARY KEY, AUTO_INCREMENT                           | 规范ID                     |
| task_id    | BIGINT       | FOREIGN KEY                                           | 任务ID（NULL表示全局设置） |
| pattern    | VARCHAR(200) | NOT NULL                                              | 匹配模式（如"2025"）       |
| rule_type  | VARCHAR(50)  | NOT NULL                                              | 规则类型（如"数字读法"）   |
| rule_value | VARCHAR(200) | NOT NULL                                              | 规则值（如"二零二五"）     |
| scope      | TINYINT      | DEFAULT 1                                             | 作用域：1-全局，2-任务     |
| created_at | TIMESTAMP    | DEFAULT CURRENT_TIMESTAMP                             | 创建时间                   |
| updated_at | TIMESTAMP    | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间                   |

**索引：**

•INDEX idx_task_id (task_id)·

•INDEX idx_pattern (pattern)

·

## 2.7 阅读规范应用表 (reading_rule_applications)

存储阅读规范在具体句子中的应用。

| 字段名       | 类型         | 约束                        | 说明         |
| ------------ | ------------ | --------------------------- | ------------ |
| id           | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | 应用ID       |
| rule_id      | BIGINT       | NOT NULL, FOREIGN KEY       | 阅读规范ID   |
| sentence_id  | BIGINT       | NOT NULL, FOREIGN KEY       | 句子ID       |
| matched_text | VARCHAR(200) | NOT NULL                    | 匹配到的文本 |
| position     | INT          |                             | 匹配位置     |
| created_at   | TIMESTAMP    | DEFAULT CURRENT_TIMESTAMP   | 创建时间     |

 **索引：** ·

•INDEX idx_rule_id (rule_id)

•INDEX idx_sentence_id (sentence_id)

·

## 2.8 音频合并记录表 (audio_merges)

记录音频合并操作。所有音频精修操作都在任务下执行。

| 字段名           | 类型         | 约束                                                  | 说明                             |
| ---------------- | ------------ | ----------------------------------------------------- | -------------------------------- |
| id               | BIGINT       | PRIMARY KEY, AUTO_INCREMENT                           | 合并ID                           |
| task_id          | BIGINT       | NOT NULL, FOREIGN KEY                                 | 任务ID                           |
| merged_audio_url | VARCHAR(500) |                                                       | 合并后的音频URL                  |
| audio_duration   | INT          |                                                       | 合并后音频时长（毫秒）           |
| sentence_ids     | JSON         |                                                       | 合并的句子ID列表                 |
| status           | TINYINT      | DEFAULT 1                                             | 状态：1-处理中，2-已完成，3-失败 |
| created_at       | TIMESTAMP    | DEFAULT CURRENT_TIMESTAMP                             | 创建时间                         |
| updated_at       | TIMESTAMP    | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间                         |

 **索引：** ·

•INDEX idx_task_id (task_id)

•INDEX idx_status (status)

·

## 2.9 音色配置表 (voice_configs)

存储可用的音色配置（常用音色）。

| 字段名         | 类型         | 约束                                                  | 说明                       |
| -------------- | ------------ | ----------------------------------------------------- | -------------------------- |
| id             | BIGINT       | PRIMARY KEY, AUTO_INCREMENT                           | 配置ID                     |
| voice_id       | VARCHAR(50)  | UNIQUE, NOT NULL                                      | 音色ID（华为云）           |
| voice_name     | VARCHAR(100) | NOT NULL                                              | 音色名称                   |
| voice_type     | VARCHAR(20)  |                                                       | 音色类型（如：女声、男声） |
| language       | VARCHAR(20)  | DEFAULT 'zh-CN'                                       | 语言                       |
| is_recommended | TINYINT      | DEFAULT 0                                             | 是否推荐：1-是，0-否       |
| sort_order     | INT          | DEFAULT 0                                             | 排序顺序                   |
| created_at     | TIMESTAMP    | DEFAULT CURRENT_TIMESTAMP                             | 创建时间                   |
| updated_at     | TIMESTAMP    | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间                   |

 **索引：** ·

•INDEX idx_voice_id (voice_id)

•INDEX idx_is_recommended (is_recommended)·

•INDEX idx_sort_order (sort_order)

·

# 3. 表关系图

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps31.png)

**tasks (1) ──< (N) sentences**

**sentences (1) ──< (1) synthesis_settings**

**sentences (1) ──< (N) pause_settings**

**sentences (1) ──< (N) polyphonic_settings**

**tasks (1) ──< (N) reading_rules**

**reading_rules (1) ──< (N) reading_rule_applications**

**tasks (1) ──< (N) audio_merges**

# 4. 数据库设计说明

## 4.1 任务模型

·

•每次请求一个文本拆句视为一个任务

•任务表存储原始文本内容

•创建任务时会自动进行拆句处理，任务创建完成后即生成多个句子（状态为已拆句）

•所有音频精修操作（参数调整、重新合成、合并等）都在任务下执行

·

## 4.2 字符限制

•文本输入支持10000字（在应用层进行校验）

•文本内容使用TEXT类型，可存储更大内容

·

## 4.3 音色管理

•音色配置表存储常用音色，便于排序和管理

•支持推荐音色标记，优先展示

·

## 4.4 阅读规范

•支持全局、任务两个级别的阅读规范

•通过阅读规范应用表记录具体应用情况

·

## 4.5 停顿与静音

•停顿和静音统一存储在pause_settings表·

•通过type字段区分：1-停顿，2-静音

·

## 4.6 音频管理·

•句子级别的音频存储在sentences表的audio_url和audio_duration字段

•整个任务的合并音频存储在tasks表的merged_audio_url和merged_audio_duration字段

•音频合并记录存储在audio_merges表（用于记录合并操作历史）·

•合并音频完成后，任务的merged_audio_url和merged_audio_duration会自动更新

·

## 4.7 SSML配置

•SSML（Speech Synthesis Markup Language）是一种基于XML的标记语言，用于控制TTS系统的输出特性

·

·

•句子级别的SSML存储在sentences表的ssml字段，包含该句子的所有编辑配置参数：

•音色（voice）

•语速（rate）

•音量（volume）

•音调（pitch）

•停顿（break）

•多音字（phoneme）

•其他SSML标签

•任务级别的SSML存储在tasks表的ssml字段，包含整个任务的合并音频的SSML配置

·

·

•SSML字段会根据用户的编辑操作（调整语速、音量、音调、添加停顿、设置多音字等）自动生成和更新·

•合并音频时，会将所有句子的SSML合并生成整个任务的SSML

·
