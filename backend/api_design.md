北京云听 - API接口设计文档

# 1. API概述

本文档描述了"北京云听"文本转语音（TTS）系统的RESTful API接口设计。

**基础信息：**

•·Base URL: **https://api.**xxxx**.com/v1**

•数据格式: JSON

•字符编码: UTF-8

·

# 2. 通用响应格式

## 2.1 成功响应

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps1.png)

**{**

**"code": 200,**

**"message": "success",**

**"data": {},**

**"timestamp": 1640000000000**

**}**

## 2.2 错误响应

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps2.png)

**{**

**"code": 400,**

**"message": "**错误信息",

**"data": null,**

**"timestamp": 1640000000000**

**}**

## 2.3 状态码说明

·

•200: 成功·

•400: 请求参数错误·

•404: 资源不存在

•500: 服务器错误

# 3. 任务管理接口

## 3.1 创建任务并自动拆句

**POST** **/tasks**

**请求参数：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps3.png)

**{**

**"content": "**这是一段需要转换为语音的文本内容。支持最多2000字。"

**}**

**响应：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps4.png)

**{**

**"code": 200,**

**"data": {**

**"task_id": 1,**

**"content": "**这是一段需要转换为语音的文本内容。支持最多2000字。",

**"char_count": 28,**

**"status": 2,**

**"merged_audio_url": null,**

**"merged_audio_duration": null,**

**"ssml": null,**

**"sentences": [**

**      {**

**"sentence_id": 1,**

**"content": "**这是一段需要转换为语音的文本内容。",

**"sequence": 1,**

**"char_count": 18,**

**"audio_url": null,**

**"audio_duration": null,**

**"ssml": null**

**},**

**      {**

**"sentence_id": 2,**

**"content": "**支持最多2000字。",

**"sequence": 2,**

**"char_count": 10,**

**"audio_url": null,**

**"audio_duration": null,**

**"ssml": null**

**      }**

**],**

**"total_sentences": 2,**

**"created_at": "2024-01-01T00:00:00Z",**

**"updated_at": "2024-01-01T00:00:00Z"**

**  }**

**}**

 **说明：** ·

•创建任务时会自动进行拆句处理

•返回的任务状态为2（已拆句）

•响应中包含拆句后的所有句子列表

·

**错误响应（超过**2000**字）：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps5.png)

**{**

**"code": 400,**

**"message": "**文本内容不能超过2000字"

**}**

## 3.2 获取任务详情

**GET** **/tasks**?**task**id={task_id}

**响应：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps6.png)

**{**

**"code": 200,**

**"data": {**

**"task_id": 1,**

**"content": "**这是一段需要转换为语音的文本内容。支持最多2000字。",

**"char_count": 28,**

**"status": 2,**

**"merged_audio_url": "https://example.com/audio/task_1_merged.mp3",**

**"merged_audio_duration": 5000,**

**"ssml": "`<speak>`<voice name=\"xiaoyan\"><prosody rate=\"0%\" volume=\"0dB\" pitch=\"0%\">**这是一段需要转换为语音的文本内容。<break time=\"500ms\"/>支持最多2000字。`</prosody></voice>``</speak>`",

**"sentences": [**

**      {**

**"sentence_id": 1,**

**"content": "**这是一段需要转换为语音的文本内容。",

**"sequence": 1,**

**"synthesis_status": 2,**

**"audio_url": "https://example.com/audio/sentence_1.mp3",**

**"audio_duration": 3000,**

**"ssml": "`<speak>`<voice name=\"xiaoyan\"><prosody rate=\"0%\" volume=\"0dB\" pitch=\"0%\">**这是一段需要转换为语音的文本内容。`</prosody></voice>``</speak>`"

**},**

**      {**

**"sentence_id": 2,**

**"content": "**支持最多2000字。",

**"sequence": 2,**

**"synthesis_status": 2,**

**"audio_url": "https://example.com/audio/sentence_2.mp3",**

**"audio_duration": 2000,**

**"ssml": "`<speak>`<voice name=\"xiaoyan\"><prosody rate=\"0%\" volume=\"0dB\" pitch=\"0%\">**支持最多2000字。`</prosody></voice>``</speak>`"

**      }**

**],**

**"created_at": "2024-01-01T00:00:00Z",**

**"updated_at": "2024-01-01T00:00:00Z"**

**  }**

**}**

**说明：**

·

•**merged_audio_url**: 整个任务的合并音频URL（所有句子合并后的完整音频）·

•**merged_audio_duration**: 整个任务的合并音频时长（毫秒）·

•**ssml**: 整个任务的SSML配置（XML格式，包含所有编辑配置参数）

•**sentences**: 句子列表，每个句子包含：

•**audio_url**: 该句子的音频URL

•**audio_duration**: 该句子的音频时长（毫秒）

•**ssml**: 该句子的SSML配置（XML格式，包含该句子的所有编辑配置参数）

o

# 4. 句子管理接口

## 4.1 获取句子列表

**GET** **/tasks/sentences**?taskid = {task_id}

**查询参数：**

•**page**: 页码

•**page_size**: 每页数量

·

**响应：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps7.png)

**{**

**"code": 200,**

**"data": {**

**"list": [**

**      {**

**"sentence_id": 1,**

**"content": "**这是一段需要转换为语音的文本内容。",

**"sequence": 1,**

**"synthesis_status": 2,**

**"audio_url": "https://example.com/audio/sentence_1.mp3",**

**"audio_duration": 3000,**

**"ssml": "`<speak>`<voice name=\"xiaoyan\"><prosody rate=\"0%\" volume=\"0dB\" pitch=\"0%\">**这是一段需要转换为语音的文本内容。`</prosody></voice>``</speak>`",

**"settings": {**

**"voice_id": "xiaoyan",**

**"voice_name": "**小燕",

**"speech_rate": 0,**

**"volume": 0**

**        }**

**      }**

**],**

**"total": 10**

**  }**

**}**

## 4.2 获取句子详情

**GET** **/sentences**?sendtenceId=**{sentence_id}**

**响应：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps8.png)

**{**

**"code": 200,**

**"data": {**

**"sentence_id": 1,**

**"content": "**这是一段需要转换为语音的文本内容。",

**"sequence": 1,**

**"synthesis_status": 2,**

**"audio_url": "https://example.com/audio/sentence_1.mp3",**

**"audio_duration": 3000,**

**"ssml": "`<speak>`<voice name=\"xiaoyan\"><prosody rate=\"10%\" volume=\"5dB\" pitch=\"5%\">**这是一段<break time=\"500ms\"/>需要转换为语音的文本内容。`</prosody></voice>``</speak>`",

**"settings": {**

**"voice_id": "xiaoyan",**

**"voice_name": "**小燕",

**"speech_rate": 10,**

**"volume": 5,**

**"pitch": 5**

**},**

**"pauses": [**

**      {**

**"id": 1,**

**"position": 10,**

**"duration": 500,**

**"type": 1**

**      }**

**],**

**"polyphonic_settings": []**

**  }**

**}**

# 5. 语音合成接口

## 5.1 合成单个句子

**POST** **/sentences/{sentence_id}/synthesize**

**请求参数：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps9.png)

**{**

**"voice_id": "xiaoyan",**

**"speech_rate": 0,**

**"volume": 0,**

**"pitch": 0**

**}**

**响应：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps10.png)

**{**

**"code": 200,**

**"data": {**

**"sentence_id": 1,**

**"synthesis_status": 1,**

**"task_id": "task_123456"**

**  }**

**}**

## 5.2 重新合成句子

**POST** **/sentences/{sentence_id}/resynthesize**

 **请求参数：** （同5.1）

## 5.3 批量合成

**POST** **/tasks/{task_id}/synthesize**

**请求参数：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps11.png)

**{**

**"voice_id": "xiaoyan",**

**"speech_rate": 0,**

**"volume": 0,**

**"sentence_ids": [1, 2, 3]**

**}**

**响应：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps12.png)

**{**

**"code": 200,**

**"data": {**

**"task_id": "batch_task_123456",**

**"total": 3,**

**"pending": 3**

**  }**

**}**

## 5.4 查询合成状态

**GET** **/synthesis/tasks**?taskId =**{task_id}**

**响应：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps13.png)

**{**

**"code": 200,**

**"data": {**

**"task_id": "task_123456",**

**"status": "completed",**

**"progress": 100,**

**"result": {**

**"sentence_id": 1,**

**"audio_url": "https://...",**

**"audio_duration": 3000**

**    }**

**  }**

**}**

# 6. 参数调整与停顿接口

## 6.1 调整参数

## POST /sentences/settings

{

    "sentence_id": 123

  "speech_rate": 10,

  "volume": 5,

  "pitch": -2,

  "voice_id": "xiaoyan"

}

·

# 7. 停顿和静音操作（参数调整子项）

## 7.1 添加停顿

**POST** **/sentences/{sentence_id}/pauses**

**请求参数：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps14.png)

**{**

**"position": 10,**

**"duration": 500,**

**"type": 1**

**}**

**说明：**

•position: 停顿位置（字符位置）

•duration: 停顿时长（毫秒）·

•type: 1-停顿，2-静音

·

## 7.2 插入静音

**POST** **/sentences/{sentence_id}/silence**

**请求参数：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps15.png)

**{**

**"position": 10,**

**"duration": 1000**

**}**

## 7.3 获取停顿列表

**GET** **/sentences/pauses**?sentenceid=**{sentence_id}**

**响应：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps16.png)

**{**

**"code": 200,**

**"data": {**

**"list": [**

**      {**

**"id": 1,**

**"position": 10,**

**"duration": 500,**

**"type": 1**

**      }**

**    ]**

**  }**

**}**

## 7.4 删除停顿

**DELETE** **/sentences/{sentence_id}/pauses/{pause_id}**

# 8. 多音字接口

## 8.1 设置多音字

**POST** **/sentences/{sentence_id}/polyphonic**

**请求参数：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps17.png)

**{**

**"character": "**中",

**"position": 5,**

**"pronunciation": "zhong4"**

**}**

## 8.2 获取多音字设置

**GET** **/sentences/polyphonic**?sentenceid=**{sentence_id}**

**响应：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps18.png)

**{**

**"code": 200,**

**"data": {**

**"list": [**

**      {**

**"id": 1,**

**"character": "**中",

**"position": 5,**

**"pronunciation": "zhong4"**

**      }**

**    ]**

**  }**

**}**

## 8.3 删除多音字设置

**DELETE** **/sentences/{sentence_id}/polyphonic/{polyphonic_id}**

# 9. 阅读规范接口（主要针对数字、日期、符号、术语等）

## 9.1 创建阅读规范

**POST** **/reading-rules**

**请求参数：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps19.png)

**{**

**"task_id": 1,**

**"pattern": "2025",**

**"rule_type": "**数字读法",

**"rule_value": "**二零二五",

**"scope": 2**

**}**

**说明：**

•scope: 1-全局，2-任务

•task_id: scope为2时必填

·

## 9.2 获取阅读规范列表

**GET** **/reading-rules**

**查询参数：**

·

•**task_id**: 任务ID

•**scope**: 作用域

·

**响应：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps20.png)

**{**

**"code": 200,**

**"data": {**

**"list": [**

**      {**

**"id": 1,**

**"pattern": "2025",**

**"rule_type": "**数字读法",

**"rule_value": "**二零二五",

**"scope": 2,**

**"task_id": 1**

**      }**

**    ]**

**  }**

**}**

## 9.3 应用阅读规范到句子

**POST** **/tasks/{task_id}/apply-reading-rules**

**请求参数：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps21.png)

**{**

**"rule_ids": [1, 2, 3]**

**}**

## 9.4 应用阅读规范到指定句子

**POST** **/sentences/{sentence_id}/apply-reading-rules**

**请求参数：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps22.png)

**{**

**"rule_ids": [1, 2]**

**}**

# 10. 音频编辑接口

## 10.1 向下插入句子

**POST** **/sentences/{sentence_id}/insert-below**

**请求参数：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps23.png)

**{**

**"content": "**插入的文本内容",

**"voice_id": "xiaoyan"**

**}**

**说明：**

•插入的句子音色默认与上一句相同

•可以指定voice_id来更改音色

**响应：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps24.png)

**{**

**"code": 200,**

**"data": {**

**"sentence_id": 5,**

**"content": "**插入的文本内容",

**"sequence": 3,**

**"voice_id": "xiaoyan"**

**  }**

**}**

## 10.2 合并音频

**POST** **/tasks/{task_id}/audio/merge**

**请求参数：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps25.png)

**{**

**"sentence_ids": [1, 2, 3, 4]**

**}**

**响应：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps26.png)

**{**

**"code": 200,**

**"data": {**

**"merge_id": 1,**

**"task_id": 1,**

**"merged_audio_url": "https://example.com/audio/task_1_merged.mp3",**

**"audio_duration": 12000,**

**"status": "completed"**

**  }**

**}**

**说明：**

·•合并音频完成后，任务的**merged_audio_url**和**merged_audio_duration**字段会自动更新

·

## 10.3 查询合并状态

**GET** **/audio/merges**?mergeid=**{merge_id}**

**响应：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps27.png)

**{**

**"code": 200,**

**"data": {**

**"merge_id": 1,**

**"task_id": 1,**

**"status": "completed",**

**"merged_audio_url": "https://example.com/audio/task_1_merged.mp3",**

**"audio_duration": 12000**

**  }**

**}**

**说明：**

•合并完成后，可以通过任务详情接口获取任务的merged_audio_url和merged_audio_duration

·

# 11. 音色管理接口

## 10.1 获取音色列表

**GET** **/voices**

 **查询参数** ·

•**is_recommended**: 是否只获取推荐音色（1-是，0-否）

·

·

•**language**: 语言（默认zh-CN）

·

**响应：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps28.png)

**{**

**"code": 200,**

**"data": {**

**"list": [**

**      {**

**"voice_id": "xiaoyan",**

**"voice_name": "**小燕",

**"voice_type": "**女声",

**"language": "zh-CN",**

**"is_recommended": 1,**

**"sort_order": 1**

**      }**

**    ]**

**  }**

**}**

# 12. 断句标准接口

## 12.1 获取断句标准

**GET** **/sentence-breaking/standards**

**响应：**

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps29.png)

**{**

**"code": 200,**

**"data": {**

**"standards": [**

**      {**

**"id": 1,**

**"name": "**标点符号断句",

**"description": "**根据标点符号进行断句",

**"rules": ["**。", "！", "？", "；"]

**},**

**      {**

**"id": 2,**

**"name": "**自定义断句",

**"description": "**根据自定义规则断句"

**      }**

**    ]**

**  }**

**}**

# 13. 接口使用说明

## 13.1 任务模型

• 每次请求一个文本拆句视为一个任务

• 创建任务时输入文本内容（最多2000字），系统会自动进行拆句处理

•任务创建完成后即生成多个句子（状态为已拆句）

•所有音频精修操作（参数调整、重新合成、合并等）都在任务下执行

·

## 13.2 分页

列表接口支持分页，使用page和page_size参数：

**GET /tasks?page=1&page_size=20**

## 13.3 错误处理·

•所有错误都会返回统一的错误格式

•客户端应根据code字段判断错误类型

·

## 13.4 异步操作

•语音合成、音频合并等操作是异步的·

•返回task_id用于查询状态·

•使用轮询获取结果

·

## 13.5 字符限制·

•文本输入限制10000字

•超过限制会返回400错误

·

## 13.6 SSML配置·

•SSML（Speech Synthesis Markup Language）是一种基于XML的标记语言，用于控制TTS系统的输出特性·

•每个句子和整个任务都有对应的SSML字段，包含所有编辑配置参数：

• **音色（**voice**）** : **`<voice name="xiaoyan">`**

• **语速（**rate**）** : **`<prosody rate="10%">`**（-500到500对应-50%到50%）

• **音量（**volume**）** : **`<prosody volume="5dB">`**（-60到60对应-60dB到60dB）

• **音调（**pitch**）** : **`<prosody pitch="5%">`**（-500到500对应-50%到50%）

• **停顿（**break**）** : **`<break time="500ms"/>`**

• **多音字（**phoneme**）** : **`<phoneme alphabet="py" ph="zhong4">`**中`</phoneme>`

•SSML字段会根据用户的编辑操作（调整语速、音量、音调、添加停顿、设置多音字等）自动生成和更新

•合并音频时，会将所有句子的SSML合并生成整个任务的SSML

•SSML示例：

·

![](file:////Users/dingwei/Library/Containers/com.kingsoft.wpsoffice.mac/Data/tmp/wps-dingwei/ksohtml//wps30.png)

**`<speak>`**

**`<voice name="xiaoyan">`**

**`<prosody rate="10%" volume="5dB" pitch="5%">`**

这是一段**`<break time="500ms"/>`**需要转换为语音的文本内容。

**`<phoneme alphabet="py" ph="**yue**">`**乐**`</phoneme>`**器

**`</prosody>`**

**`</voice>`**

**`</speak>`**
