---
title: 默认模块
language_tabs:
  - shell: Shell
  - http: HTTP
  - javascript: JavaScript
  - ruby: Ruby
  - python: Python
  - php: PHP
  - java: Java
  - go: Go
toc_footers: []
includes: []
search: true
code_clipboard: true
highlight_theme: darkula
headingLevel: 2
generator: "@tarslib/widdershins v4.0.30"

---

# 默认模块

Base URLs:

# Authentication

# 任务管理接口

## POST 创建任务

POST /api/task/create

创建任务时会自动进行拆句处理（根据指定的拆句符号拆句）。
拆句完成后，每条拆句记录默认会生成一条断句记录。

> Body 请求参数

```json
{
  "content": "",
  "delimiterList": []
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[CreateTaskRequest](#schemacreatetaskrequest)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": null,
  "message": "",
  "data": {
    "taskId": null,
    "mergeId": null,
    "content": "",
    "charCount": null,
    "status": null,
    "audioUrl": "",
    "audioDuration": null,
    "createdAt": "",
    "updatedAt": ""
  }
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|false|none||none|
|» message|string|false|none||none|
|» data|[TaskDetailDTO](#schemataskdetaildto)|false|none||none|
|»» taskId|integer(int64)|false|none||none|
|»» mergeId|integer|false|none||none|
|»» content|string|false|none||none|
|»» charCount|integer|false|none||none|
|»» status|integer|false|none||0，拆句完成，1，语音合成中，2，语音合成成功，3，语音合成失败，4，语音合并中，5，语音合并成功，6，语音合并失败|
|»» audioUrl|string|false|none||none|
|»» audioDuration|integer|false|none||none|
|»» createdAt|string|false|none||none|
|»» updatedAt|string|false|none||none|

## GET 获取任务详情

GET /api/task/getDetail

返回任务详情以及任务下的拆句内容

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|taskid|query|integer| 是 |none|

> 返回示例

```json
{
  "code": null,
  "message": "",
  "data": {
    "taskId": null,
    "mergeId": null,
    "content": "",
    "charCount": null,
    "status": null,
    "audioUrl": "",
    "audioDuration": null,
    "createdAt": "",
    "updatedAt": ""
  }
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "taskId": 0,
    "content": "",
    "charCount": 0,
    "status": 0,
    "mergedAudioUrl": "",
    "mergedAudioDuration": 0,
    "ssml": "",
    "totalSentences": 0,
    "createdAt": "",
    "updatedAt": "",
    "sentences": [
      {
        "sentenceId": 0,
        "parentId": 0,
        "sequence": 0,
        "charCount": 0,
        "content": "",
        "audioUrl": "",
        "audioDuration": 0,
        "ssml": ""
      }
    ],
    "breakingSentences": [
      {
        "breakingSentenceId": 0,
        "originalSentenceId": 0,
        "sequence": 0,
        "content": "",
        "synthesisStatus": 0,
        "audioUrl": "",
        "audioDuration": 0,
        "ssml": ""
      }
    ]
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "taskId": 0,
    "content": "",
    "charCount": 0,
    "status": 0,
    "mergedAudioUrl": "",
    "mergedAudioDuration": 0,
    "ssml": "",
    "totalSentences": 0,
    "createdAt": "",
    "updatedAt": "",
    "sentences": [
      {
        "sentenceId": 0,
        "parentId": 0,
        "sequence": 0,
        "charCount": 0,
        "content": "",
        "audioUrl": "",
        "audioDuration": 0,
        "ssml": ""
      }
    ],
    "breakingSentences": [
      {
        "breakingSentenceId": 0,
        "originalSentenceId": 0,
        "sequence": 0,
        "content": "",
        "synthesisStatus": 0,
        "audioUrl": "",
        "audioDuration": 0,
        "ssml": ""
      }
    ]
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "taskId": 0,
    "content": "",
    "charCount": 0,
    "status": 0,
    "mergedAudioUrl": "",
    "mergedAudioDuration": 0,
    "ssml": "",
    "totalSentences": 0,
    "createdAt": "",
    "updatedAt": "",
    "sentences": [
      {
        "sentenceId": 0,
        "parentId": 0,
        "sequence": 0,
        "charCount": 0,
        "content": "",
        "audioUrl": "",
        "audioDuration": 0,
        "ssml": ""
      }
    ],
    "breakingSentences": [
      {
        "breakingSentenceId": 0,
        "originalSentenceId": 0,
        "sequence": 0,
        "content": "",
        "synthesisStatus": 0,
        "audioUrl": "",
        "audioDuration": 0,
        "ssml": ""
      }
    ]
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ApiResponseTaskDetailDTO](#schemaapiresponsetaskdetaildto)|

# 语音合成接口

## POST 设置拆句合成参数

POST /api/synthesis/setConfig

覆盖旧数据

> Body 请求参数

```json
"{\n    \"taskId\": ,\n    \"originalSentenceId\": ,\n    \"breakingSentenceList\": [\n        {\n            \"breakingSentenceId\": ,\n            \"sequence\": ,\n            \"content\": \"\",\n            \"volume\": ,\n            \"voiceId\": \"\",\n            \"speed\": ,\n            \"breakList\": [\n                {\n                    \"location\": ,\n                    \"duration\": \n                }\n            ],\n            \"phonemeList\": [\n                {\n                    \"ph\": \"\",\n                    \"location\": \n                },\n                {\n                    \"ph\": \"\",\n                    \"location\": \n                },\n                {\n                    \"ph\": \"\",\n                    \"location\": \n                }\n            ],\n            \"prosodyList\": [\n                {\n                    \"rate\": ,\n                    \"begin\": ,\n                    \"end\": \n                },\n                {\n                    \"rate\": ,\n                    \"begin\": ,\n                    \"end\": \n                }\n            ],\n            \"silenceList\": [\n                {\n                    \"location\": ,\n                    \"duration\": \n                },\n                {\n                    \"location\": ,\n                    \"duration\": \n                },\n                {\n                    \"location\": ,\n                    \"duration\": \n                }\n            ],\n            \"readRule\": [\n                {\n                    \"ruleId\": ,\n                    \"partern\": \"\",\n                    \"isOpen\": \n                },\n                {\n                    \"ruleId\": ,\n                    \"partern\": \"\",\n                    \"isOpen\": \n                }\n            ]\n        },\n        {\n            \"breakingSentenceId\": ,\n            \"sequence\": ,\n            \"content\": \"\",\n            \"volume\": ,\n            \"voiceId\": \"\",\n            \"speed\": ,\n            \"breakList\": [\n                {\n                    \"location\": ,\n                    \"duration\": \n                }\n            ],\n            \"phonemeList\": [\n                {\n                    \"ph\": \"\",\n                    \"location\": \n                }\n            ],\n            \"prosodyList\": [\n                {\n                    \"rate\": ,\n                    \"begin\": ,\n                    \"end\": \n                }\n            ],\n            \"silenceList\": [\n                {\n                    \"location\": ,\n                    \"duration\": \n                },\n                {\n                    \"location\": ,\n                    \"duration\": \n                }\n            ],\n            \"readRule\": [\n                {\n                    \"ruleId\": ,\n                    \"partern\": \"\",\n                    \"isOpen\": \n                }\n            ]\n        },\n        {\n            \"breakingSentenceId\": ,\n            \"sequence\": ,\n            \"content\": \"\",\n            \"volume\": ,\n            \"voiceId\": \"\",\n            \"speed\": ,\n            \"breakList\": [\n                {\n                    \"location\": ,\n                    \"duration\": \n                },\n                {\n                    \"location\": ,\n                    \"duration\": \n                },\n                {\n                    \"location\": ,\n                    \"duration\": \n                }\n            ],\n            \"phonemeList\": [\n                {\n                    \"ph\": \"\",\n                    \"location\": \n                },\n                {\n                    \"ph\": \"\",\n                    \"location\": \n                },\n                {\n                    \"ph\": \"\",\n                    \"location\": \n                }\n            ],\n            \"prosodyList\": [\n                {\n                    \"rate\": ,\n                    \"begin\": ,\n                    \"end\": \n                }\n            ],\n            \"silenceList\": [\n                {\n                    \"location\": ,\n                    \"duration\": \n                },\n                {\n                    \"location\": ,\n                    \"duration\": \n                },\n                {\n                    \"location\": ,\n                    \"duration\": \n                }\n            ],\n            \"readRule\": [\n                {\n                    \"ruleId\": ,\n                    \"partern\": \"\",\n                    \"isOpen\": \n                },\n                {\n                    \"ruleId\": ,\n                    \"partern\": \"\",\n                    \"isOpen\": \n                },\n                {\n                    \"ruleId\": ,\n                    \"partern\": \"\",\n                    \"isOpen\": \n                }\n            ]\n        }\n    ]\n}"
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[synthesisSetConfigRequest](#schemasynthesissetconfigrequest)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": null,
  "message": "",
  "data": "",
  "timestamp": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|false|none||none|
|» message|string|false|none||none|
|» data|string|false|none||none|
|» timestamp|integer(int64)|false|none||none|

## POST 合成断句

POST /api/synthesis/breakingSentence

合成或重新合成单个断句。
使用RocketMQ进行流控，每秒请求华为云最多5次。

> Body 请求参数

```json
"{\n    \"breakingSentenceId\": \n}"
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|object| 否 |none|
|» breakingSentenceId|body|integer| 是 |none|

> 返回示例

```json
{
  "code": null,
  "message": "",
  "data": "",
  "timestamp": null
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "breakingSentenceId": 0,
    "taskId": 0,
    "synthesisStatus": 0
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "breakingSentenceId": 0,
    "taskId": 0,
    "synthesisStatus": 0
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "breakingSentenceId": 0,
    "taskId": 0,
    "synthesisStatus": 0
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "breakingSentenceId": 0,
    "taskId": 0,
    "synthesisStatus": 0
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "breakingSentenceId": 0,
    "taskId": 0,
    "synthesisStatus": 0
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "breakingSentenceId": 0,
    "taskId": 0,
    "synthesisStatus": 0
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|false|none||none|
|» message|string|false|none||none|
|» data|string|false|none||none|
|» timestamp|integer(int64)|false|none||none|

## POST 合成拆句

POST /api/synthesis/originalSentence

合成或重新合成拆句下的所有断句。

> Body 请求参数

```json
"{\n    \"originalSentenceId\": \n}"
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|object| 否 |none|
|» originalSentenceId|body|integer| 是 |none|

> 返回示例

```json
{
  "code": null,
  "message": "",
  "data": "",
  "timestamp": null
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "breakingSentenceId": 0,
    "taskId": 0,
    "synthesisStatus": 0
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "breakingSentenceId": 0,
    "taskId": 0,
    "synthesisStatus": 0
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "breakingSentenceId": 0,
    "taskId": 0,
    "synthesisStatus": 0
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "breakingSentenceId": 0,
    "taskId": 0,
    "synthesisStatus": 0
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "breakingSentenceId": 0,
    "taskId": 0,
    "synthesisStatus": 0
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|false|none||none|
|» message|string|false|none||none|
|» data|string|false|none||none|
|» timestamp|integer(int64)|false|none||none|

## POST 合成任务

POST /api/synthesis/task

合成或重新合成任务下的所有断句。

> Body 请求参数

```json
"{\n    \"taskId\": \n}"
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|object| 否 |none|
|» taskId|body|integer| 是 |none|

> 返回示例

```json
{
  "code": null,
  "message": "",
  "data": "",
  "timestamp": null
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "taskId": 0,
    "total": 0,
    "pending": 0
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "taskId": 0,
    "total": 0,
    "pending": 0
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "taskId": 0,
    "total": 0,
    "pending": 0
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "taskId": 0,
    "total": 0,
    "pending": 0
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "taskId": 0,
    "total": 0,
    "pending": 0
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|false|none||none|
|» message|string|false|none||none|
|» data|string|false|none||none|
|» timestamp|integer(int64)|false|none||none|

## GET 获取任务合成状态

GET /api/synthesis/getTaskStatus

给出任务下所有断句的合成进度和已完成合成的音频文件下载地址和时长。

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|taskid|query|integer| 是 |none|

> 返回示例

```json
{
  "code": null,
  "message": "",
  "data": {
    "status": null,
    "progress": null,
    "total": null,
    "completed": null,
    "pending": null,
    "audioUrlList": [
      {
        "sequence": null,
        "audioUrl": ""
      },
      {
        "sequence": null,
        "audioUrl": ""
      }
    ]
  },
  "timestamp": null
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "taskId": 0,
    "status": "",
    "progress": 0,
    "total": 0,
    "completed": 0,
    "pending": 0,
    "result": {
      "sentenceId": 0,
      "audioUrl": "",
      "audioDuration": 0
    }
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "taskId": 0,
    "status": "",
    "progress": 0,
    "total": 0,
    "completed": 0,
    "pending": 0,
    "result": {
      "sentenceId": 0,
      "audioUrl": "",
      "audioDuration": 0
    }
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "taskId": 0,
    "status": "",
    "progress": 0,
    "total": 0,
    "completed": 0,
    "pending": 0,
    "result": {
      "sentenceId": 0,
      "audioUrl": "",
      "audioDuration": 0
    }
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "taskId": 0,
    "status": "",
    "progress": 0,
    "total": 0,
    "completed": 0,
    "pending": 0,
    "result": {
      "sentenceId": 0,
      "audioUrl": "",
      "audioDuration": 0
    }
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "taskId": 0,
    "status": "",
    "progress": 0,
    "total": 0,
    "completed": 0,
    "pending": 0,
    "result": {
      "sentenceId": 0,
      "audioUrl": "",
      "audioDuration": 0
    }
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ApiResponseTaskSynthesisStatusDTO](#schemaapiresponsetasksynthesisstatusdto)|

## GET 获取拆句合成状态

GET /api/synthesis/getOriginalSentenceStatus

给出拆句下所有断句的合成进度和已完成合成的音频文件下载地址和时长。

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|originalSentenceId|query|integer| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": null,
  "message": "",
  "data": {
    "status": null,
    "progress": null,
    "total": null,
    "completed": null,
    "pending": null,
    "audioUrlList": [
      {
        "sequence": null,
        "audioUrl": ""
      }
    ]
  },
  "timestamp": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ApiResponseTaskSynthesisStatusDTO](#schemaapiresponsetasksynthesisstatusdto)|

## GET 获取断句合成状态

GET /api/synthesis/getBreakingSentenceStatus

给出单句断句的合成状态和已完成合成的音频文件下载地址和时长。

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|breakingSentenceId|query|integer| 否 |none|

> 返回示例

> 200 Response

```json
{
  "audioUrl": "",
  "audioDuration": null,
  "synthesisStatus": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[SynthesisResultDTO](#schemasynthesisresultdto)|

# 音色管理接口

## GET 获取音色列表

GET /api/voice/getList

> 返回示例

```json
{
  "code": null,
  "message": "",
  "data": {
    "list": [
      {
        "voiceId": "",
        "voiceName": "",
        "voiceType": "",
        "sortOrder": null,
        "avatar_url": ""
      }
    ]
  },
  "timestamp": null
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "list": [
      {
        "voiceId": "",
        "voiceName": "",
        "voiceType": "",
        "language": "",
        "isRecommended": 0,
        "sortOrder": 0
      }
    ]
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "list": [
      {
        "voiceId": "",
        "voiceName": "",
        "voiceType": "",
        "language": "",
        "isRecommended": 0,
        "sortOrder": 0
      }
    ]
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "list": [
      {
        "voiceId": "",
        "voiceName": "",
        "voiceType": "",
        "language": "",
        "isRecommended": 0,
        "sortOrder": 0
      }
    ]
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "list": [
      {
        "voiceId": "",
        "voiceName": "",
        "voiceType": "",
        "language": "",
        "isRecommended": 0,
        "sortOrder": 0
      }
    ]
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ApiResponseVoiceConfigListResponseDTO](#schemaapiresponsevoiceconfiglistresponsedto)|

# 阅读规范接口

## GET 获取文本中符合规则字段

GET /api/readingRule/getMatchingFieldListFromText

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|text|query|string| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": null,
  "message": "",
  "data": {
    "total": null,
    "fieldList": [
      {
        "ruleId": null,
        "location": null,
        "pattern": "",
        "isOpen": null
      },
      {
        "ruleId": null,
        "location": null,
        "pattern": "",
        "isOpen": null
      }
    ]
  },
  "timestamp": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ApiResponseMatchingFieldListResponseDTO](#schemaapiresponsematchingfieldlistresponsedto)|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|false|none||none|
|» message|string|false|none||none|
|» data|[MatchingFieldListResponseDTO](#schemamatchingfieldlistresponsedto)|false|none||none|
|»» total|integer|false|none||none|
|»» fieldList|[[MatchingFieldDTO](#schemamatchingfielddto)]|false|none||none|
|»»» ruleId|integer(int64)|false|none||none|
|»»» location|integer|false|none|字符位置|匹配字段第一个字符的位置|
|»»» pattern|string|false|none|字段|none|
|»»» isOpen|boolean|true|none||none|
|» timestamp|integer(int64)|false|none||none|

## POST 创建阅读规范

POST /api/readingRule/create

> Body 请求参数

```json
{
  "pattern": "",
  "ruleType": "",
  "ruleValue": ""
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[ReadingRuleCreateRequest](#schemareadingrulecreaterequest)| 否 |none|

> 返回示例

```json
{
  "code": null,
  "message": "",
  "data": {
    "ruleId": null
  },
  "timestamp": null
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "ruleId": 0,
    "taskId": 0,
    "pattern": "",
    "ruleType": "",
    "ruleValue": "",
    "scope": 0,
    "createdAt": ""
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "ruleId": 0,
    "taskId": 0,
    "pattern": "",
    "ruleType": "",
    "ruleValue": "",
    "scope": 0,
    "createdAt": ""
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "ruleId": 0,
    "taskId": 0,
    "pattern": "",
    "ruleType": "",
    "ruleValue": "",
    "scope": 0,
    "createdAt": ""
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "ruleId": 0,
    "taskId": 0,
    "pattern": "",
    "ruleType": "",
    "ruleValue": "",
    "scope": 0,
    "createdAt": ""
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|false|none||none|
|» message|string|false|none||none|
|» data|object|false|none||none|
|»» ruleId|integer|false|none||none|
|» timestamp|integer(int64)|false|none||none|

## GET 获取阅读规范列表

GET /api/readingRule/getList

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|task_id|query|integer| 否 |none|
|ruleType|query|string| 否 |none|
|page|query|integer| 是 |none|
|pageSize|query|integer| 是 |none|

> 返回示例

```json
{
  "code": null,
  "message": "",
  "data": {
    "readingRuleList": [
      {
        "ruleId": null,
        "pattern": "",
        "ruleType": null,
        "ruleValue": "",
        "isOpen": null
      },
      {
        "ruleId": null,
        "pattern": "",
        "ruleType": null,
        "ruleValue": "",
        "isOpen": null
      }
    ],
    "total": null,
    "page": null,
    "pageSize": null
  },
  "timestamp": null
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "list": [
      {
        "ruleId": 0,
        "taskId": 0,
        "pattern": "",
        "ruleType": "",
        "ruleValue": "",
        "scope": 0,
        "createdAt": ""
      }
    ]
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "list": [
      {
        "ruleId": 0,
        "taskId": 0,
        "pattern": "",
        "ruleType": "",
        "ruleValue": "",
        "scope": 0,
        "createdAt": ""
      }
    ]
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "list": [
      {
        "ruleId": 0,
        "taskId": 0,
        "pattern": "",
        "ruleType": "",
        "ruleValue": "",
        "scope": 0,
        "createdAt": ""
      }
    ]
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "list": [
      {
        "ruleId": 0,
        "taskId": 0,
        "pattern": "",
        "ruleType": "",
        "ruleValue": "",
        "scope": 0,
        "createdAt": ""
      }
    ]
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ApiResponseReadingRuleListPageResponseDTO](#schemaapiresponsereadingrulelistpageresponsedto)|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|false|none||none|
|» message|string|false|none||none|
|» data|[ReadingRuleListPageResponseDTO](#schemareadingrulelistpageresponsedto)|false|none||none|
|»» readingRuleList|[[ReadingRuleListItemDTO](#schemareadingrulelistitemdto)]|false|none||none|
|»»» ruleId|integer(int64)|false|none||none|
|»»» pattern|string|false|none|原文本|none|
|»»» ruleType|string|false|none||none|
|»»» ruleValue|string|false|none||none|
|»»» isOpen|boolean|true|none|全局开关状态|0关闭，1打开|
|»» total|integer|false|none||none|
|»» page|integer|false|none||none|
|»» pageSize|integer|false|none||none|
|» timestamp|integer(int64)|false|none||none|

## POST 开关全局阅读规范

POST /api/readingRule/setGlobalSetting

> Body 请求参数

```json
{
  "taskId": 0,
  "ruleId": 0,
  "isOpen": true
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[ReadingRuleSetGlobalSettingRequest](#schemareadingrulesetglobalsettingrequest)| 否 |none|
|» taskId|body|integer(int64)| 是 |none|
|» ruleId|body|integer(int64)| 是 |none|
|» isOpen|body|boolean| 是 |0关闭，1打开|

> 返回示例

```json
{
  "code": 0,
  "message": "",
  "data": "设置成功",
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ApiResponseReadingRuleApplyResponseDTO](#schemaapiresponsereadingruleapplyresponsedto)|

**注意**：此接口返回的`data`字段为`string`类型（如"设置成功"），不是对象。

# 音频合并接口

## POST 合并音频

POST /api/merge/audio

> Body 请求参数

```json
"{\n    \"taskId\": \n}"
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|taskid|query|integer| 是 |none|
|body|body|[AudioMergeRequest](#schemaaudiomergerequest)| 否 |none|

> 返回示例

```json
{
  "code": null,
  "message": "",
  "data": {
    "mergeId": null
  },
  "timestamp": null
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "mergeId": 0,
    "taskId": 0,
    "mergedAudioUrl": "",
    "audioDuration": 0,
    "status": ""
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "mergeId": 0,
    "taskId": 0,
    "mergedAudioUrl": "",
    "audioDuration": 0,
    "status": ""
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "mergeId": 0,
    "taskId": 0,
    "mergedAudioUrl": "",
    "audioDuration": 0,
    "status": ""
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "mergeId": 0,
    "taskId": 0,
    "mergedAudioUrl": "",
    "audioDuration": 0,
    "status": ""
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|false|none||none|
|» message|string|false|none||none|
|» data|object|false|none||none|
|»» mergeId|integer|true|none||none|
|» timestamp|integer(int64)|false|none||none|

## GET 获取合并状态

GET /api/merge/getStatus

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|mergeId|query|integer| 是 |none|

> 返回示例

```json
{
  "code": null,
  "message": "",
  "data": {
    "taskId": null,
    "mergedAudioUrl": "",
    "audioDuration": null,
    "status": null
  },
  "timestamp": null
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "mergeId": 0,
    "taskId": 0,
    "mergedAudioUrl": "",
    "audioDuration": 0,
    "status": ""
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "mergeId": 0,
    "taskId": 0,
    "mergedAudioUrl": "",
    "audioDuration": 0,
    "status": ""
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "mergeId": 0,
    "taskId": 0,
    "mergedAudioUrl": "",
    "audioDuration": 0,
    "status": ""
  },
  "timestamp": 0
}
```

```json
{
  "code": 0,
  "message": "",
  "data": {
    "mergeId": 0,
    "taskId": 0,
    "mergedAudioUrl": "",
    "audioDuration": 0,
    "status": ""
  },
  "timestamp": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|false|none||none|
|» message|string|false|none||none|
|» data|object|false|none||none|
|»» taskId|integer|true|none||none|
|»» mergedAudioUrl|string¦null|false|none||none|
|»» audioDuration|integer¦null|false|none||none|
|»» status|integer|true|none||1-合并中，2-合并完成，3-合并失败|
|» timestamp|integer(int64)|false|none||none|

# 拆句管理接口

## POST 删除拆句

POST /api/originalSentence/delete

删除拆句下所有断句以及断句所属的所有合成配置。同时删除断句对应的阅读规则。

> Body 请求参数

```json
{
  "originalSentenceId": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[OriginalSentenceDeleteRequest](#schemaoriginalsentencedeleterequest)| 否 |none|
|» originalSentenceId|body|integer(int64)| 是 |none|

> 返回示例

> 200 Response

```json
{
  "code": null,
  "message": "",
  "data": "",
  "timestamp": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|false|none||none|
|» message|string|false|none||none|
|» data|string|false|none||none|
|» timestamp|integer(int64)|false|none||none|

## GET 获取拆句列表

GET /api/originalSentence/getOriginalSentenceList

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|taskid|query|integer| 是 |none|
|page|query|integer| 否 |none|
|page_size|query|integer| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": null,
  "message": "",
  "data": {
    "list": [
      {
        "originalSentenceId": null,
        "sequence": null,
        "content": "",
        "synthesisStatus": null,
        "breakingSentenceList": [
          {
            "breakingSentenceId": null,
            "taskId": null,
            "originalSentenceId": null,
            "content": "",
            "charCount": null,
            "sequence": null,
            "synthesisStatus": null,
            "audioUrl": "",
            "audioDuration": null,
            "ssml": "",
            "jobId": "",
            "createdAt": "",
            "updatedAt": "",
            "setting": {
              "content": "",
              "readRule": [
                {
                  "ruleId": "",
                  "partern": "",
                  "isOpen": null
                }
              ],
              "volume": null,
              "voiceId": "",
              "breakList": [
                {
                  "location": "",
                  "duration": ""
                }
              ],
              "phonemeList": [
                {
                  "ph": "",
                  "alphabet": "",
                  "begin": null,
                  "end": null
                },
                {
                  "ph": "",
                  "alphabet": "",
                  "begin": null,
                  "end": null
                },
                {
                  "ph": "",
                  "alphabet": "",
                  "begin": null,
                  "end": null
                }
              ],
              "prosodyList": [
                {
                  "rate": "",
                  "begin": null,
                  "end": null
                }
              ],
              "silentList": [
                {
                  "location": null,
                  "duration": null
                }
              ]
            }
          },
          {
            "breakingSentenceId": null,
            "taskId": null,
            "originalSentenceId": null,
            "content": "",
            "charCount": null,
            "sequence": null,
            "synthesisStatus": null,
            "audioUrl": "",
            "audioDuration": null,
            "ssml": "",
            "jobId": "",
            "createdAt": "",
            "updatedAt": "",
            "setting": {
              "content": "",
              "readRule": [
                {
                  "ruleId": "",
                  "partern": "",
                  "isOpen": null
                },
                {
                  "ruleId": "",
                  "partern": "",
                  "isOpen": null
                }
              ],
              "volume": null,
              "voiceId": "",
              "breakList": [
                {
                  "location": "",
                  "duration": ""
                },
                {
                  "location": "",
                  "duration": ""
                },
                {
                  "location": "",
                  "duration": ""
                }
              ],
              "phonemeList": [
                {
                  "ph": "",
                  "alphabet": "",
                  "begin": null,
                  "end": null
                }
              ],
              "prosodyList": [
                {
                  "rate": "",
                  "begin": null,
                  "end": null
                },
                {
                  "rate": "",
                  "begin": null,
                  "end": null
                }
              ],
              "silentList": [
                {
                  "location": null,
                  "duration": null
                }
              ]
            }
          }
        ]
      }
    ],
    "total": null,
    "page": null,
    "pageSize": null
  },
  "timestamp": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|» code|integer|false|none||none|
|» message|string|false|none||none|
|» data|object|false|none||none|
|»» list|[object]|false|none||none|
|»»» originalSentenceId|integer(int64)|false|none||none|
|»»» sequence|integer|false|none||none|
|»»» content|string|false|none||none|
|»»» synthesisStatus|integer|false|none||none|
|»»» breakingSentenceList|[object]|true|none||none|
|»»»» breakingSentenceId|integer|true|none||none|
|»»»» taskId|integer|true|none||none|
|»»»» originalSentenceId|integer¦null|false|none||none|
|»»»» content|string|true|none||none|
|»»»» charCount|integer|true|none||none|
|»»»» sequence|integer|true|none||none|
|»»»» synthesisStatus|integer|true|none||none|
|»»»» audioUrl|string¦null|false|none||none|
|»»»» audioDuration|integer¦null|false|none||none|
|»»»» ssml|string¦null|false|none||none|
|»»»» jobId|string¦null|false|none||none|
|»»»» createdAt|string(date-time)|true|none||none|
|»»»» updatedAt|string(date-time)|true|none||none|
|»»»» setting|[Param](#schemaparam)|true|none||none|
|»»»»» content|string|true|none|断句内容|none|
|»»»»» readRule|[object]|true|none||none|
|»»»»»» ruleId|string|true|none||none|
|»»»»»» partern|string|true|none||none|
|»»»»»» isOpen|boolean|true|none|是否打开|none|
|»»»»» volume|integer|true|none|音量|取值范围：90~240。默认取值：140。|
|»»»»» voiceId|string|true|none|音色ID|none|
|»»»»» breakList|[object]|false|none|停顿|标签关系：不能包含其他任何标签。<br />不能包含其他任何标签|
|»»»»»» location|string|true|none|标签插入位置|下面所有插入位置默认在字符后。|
|»»»»»» duration|string|true|none|停顿时间|单位ms|
|»»»»» phonemeList|[object]|false|none|多音字|标签关系：可以包含文本，不可以包含其他标签|
|»»»»»» ph|string|true|none|多音字|输入汉语拼音时，声调用1、2、3、4来表示，5表示轻声。标签起始和结束中间只能有1个汉字。举例1：天气的ph取值为“tian1 qi4”。举例2：Token的ph取值为“ˈtəʊkən”。|
|»»»»»» alphabet|string|false|none|音标符号系统|使用音标时必选，举例：<phoneme alphabet="ipa" ph="təˈmɑːtəʊ">tomato</phoneme>，参数为ipa时，使用国际音标系统。|
|»»»»»» begin|integer|true|none|起始标签插入位置|none|
|»»»»»» end|integer|true|none|结束标签插入位置|none|
|»»»»» prosodyList|[object]|false|none|局部语速|标签关系：可以包含文本，不可以包含其他标签。|
|»»»»»» rate|string|true|none|语速百分比值|最小值50，最大值200。示例：50，表示用0.5倍速度阅读|
|»»»»»» begin|integer|true|none||none|
|»»»»»» end|integer|true|none||none|
|»»»»» silentList|[object]|true|none|静音|none|
|»»»»»» location|integer|true|none||none|
|»»»»»» duration|integer|true|none||none|
|»» total|integer(int64)|false|none||none|
|»» page|integer|false|none||none|
|»» pageSize|integer|false|none||none|
|» timestamp|integer(int64)|false|none||none|

# 数据模型

<h2 id="tocS_BreakingSentences">BreakingSentences</h2>

<a id="schemabreakingsentences"></a>
<a id="schema_BreakingSentences"></a>
<a id="tocSbreakingsentences"></a>
<a id="tocsbreakingsentences"></a>

```json
{
  "breakingSentenceId": 1,
  "taskId": -2147483648,
  "originalSentenceId": -2147483648,
  "content": "string",
  "charCount": -2147483648,
  "sequence": -2147483648,
  "synthesisStatus": "0",
  "audioUrl": "string",
  "audioDuration": -2147483648,
  "ssml": "string",
  "jobId": "string",
  "createdAt": "CURRENT_TIMESTAMP",
  "updatedAt": "CURRENT_TIMESTAMP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|breakingSentenceId|integer|true|none||none|
|taskId|integer|true|none||none|
|originalSentenceId|integer¦null|false|none||none|
|content|string|true|none||none|
|charCount|integer|true|none||none|
|sequence|integer|true|none||none|
|synthesisStatus|integer|true|none||none|
|audioUrl|string¦null|false|none||none|
|audioDuration|integer¦null|false|none||none|
|ssml|string¦null|false|none||none|
|jobId|string¦null|false|none||none|
|createdAt|string(date-time)|true|none||none|
|updatedAt|string(date-time)|true|none||none|

<h2 id="tocS_SynthesisBreakingSentenceRequest">SynthesisBreakingSentenceRequest</h2>

<a id="schemasynthesisbreakingsentencerequest"></a>
<a id="schema_SynthesisBreakingSentenceRequest"></a>
<a id="tocSsynthesisbreakingsentencerequest"></a>
<a id="tocssynthesisbreakingsentencerequest"></a>

```json
{
  "breakingSentenceId": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|breakingSentenceId|integer|true|none||none|

<h2 id="tocS_CreateTaskRequest">CreateTaskRequest</h2>

<a id="schemacreatetaskrequest"></a>
<a id="schema_CreateTaskRequest"></a>
<a id="tocScreatetaskrequest"></a>
<a id="tocscreatetaskrequest"></a>

```json
{
  "content": "string",
  "delimiterList": [
    0
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|content|string|true|none|原始文本|最多10000字|
|delimiterList|[integer]|true|none|拆句符号|1，句号，2，叹号，3，问号，4，省略号|

<h2 id="tocS_SynthesisOriginalSentenceRequest">SynthesisOriginalSentenceRequest</h2>

<a id="schemasynthesisoriginalsentencerequest"></a>
<a id="schema_SynthesisOriginalSentenceRequest"></a>
<a id="tocSsynthesisoriginalsentencerequest"></a>
<a id="tocssynthesisoriginalsentencerequest"></a>

```json
{
  "originalSentenceId": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|originalSentenceId|integer|true|none||none|

<h2 id="tocS_SynthesisTaskRequest">SynthesisTaskRequest</h2>

<a id="schemasynthesistaskrequest"></a>
<a id="schema_SynthesisTaskRequest"></a>
<a id="tocSsynthesistaskrequest"></a>
<a id="tocssynthesistaskrequest"></a>

```json
{
  "taskId": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|taskId|integer|true|none||none|

<h2 id="tocS_synthesisSetConfigRequest">synthesisSetConfigRequest</h2>

<a id="schemasynthesissetconfigrequest"></a>
<a id="schema_synthesisSetConfigRequest"></a>
<a id="tocSsynthesissetconfigrequest"></a>
<a id="tocssynthesissetconfigrequest"></a>

```json
{
  "taskId": 0,
  "originalSentenceId": 0,
  "breakingSentenceList": [
    {
      "breakingSentenceId": 0,
      "sequence": 0,
      "content": "string",
      "volume": 0,
      "voiceId": "string",
      "speed": 0,
      "breakList": [
        {
          "location": 0,
          "duration": 0
        }
      ],
      "phonemeList": [
        {
          "ph": "string",
          "location": 0
        }
      ],
      "prosodyList": [
        {
          "rate": 0,
          "begin": 0,
          "end": 0
        }
      ],
      "silenceList": [
        {
          "location": 0,
          "duration": 0
        }
      ],
      "readRule": [
        {
          "ruleId": 0,
          "partern": "string",
          "isOpen": true
        }
      ]
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|taskId|integer|true|none||none|
|originalSentenceId|integer|true|none||none|
|breakingSentenceList|[object]|true|none||none|
|» breakingSentenceId|integer|true|none|断句ID|负整为新增|
|» sequence|integer|true|none|次序|none|
|» content|string|true|none|断句内容|none|
|» volume|integer|true|none|音量|取值范围：90~240。默认取值：140。|
|» voiceId|string|true|none|音色ID|none|
|» speed|integer|true|none|全局语速|当取值为“100”时，表示一个成年人正常的语速，约为250字/分钟。50表示0.5倍语速，100表示正常语速，200表示2倍语速。取值范围：50~200。默认取值：100。|
|» breakList|[object]|false|none|停顿|标签关系：不能包含其他任何标签。|
|»» location|integer|true|none|标签插入位置|下面所有插入位置默认在字符后。|
|»» duration|integer|true|none|停顿时间|单位ms|
|» phonemeList|[object]|false|none|多音字|标签关系：可以包含文本，不可以包含其他标签|
|»» ph|string|true|none|拼音|输入汉语拼音时，声调用1、2、3、4来表示，5表示轻声。标签起始和结束中间只能有1个汉字。举例1：天气的ph取值为“tian1 qi4”。举例2：Token的ph取值为“ˈtəʊkən”。|
|»» location|integer|true|none|标签插入位置|默认一个中文字符|
|» prosodyList|[object]|false|none|局部语速|标签关系：可以包含文本，不可以包含其他标签。|
|»» rate|integer|true|none||设置与全局语速相同|
|»» begin|integer|true|none||none|
|»» end|integer|true|none||none|
|» silenceList|[object]|false|none|静音|标签关系：不能包含其他任何标签。|
|»» location|integer|true|none||none|
|»» duration|integer|true|none||none|
|» readRule|[object]|true|none||none|
|»» ruleId|integer|true|none||none|
|»» partern|string|true|none||none|
|»» isOpen|boolean|true|none||none|

<h2 id="tocS_Param">Param</h2>

<a id="schemaparam"></a>
<a id="schema_Param"></a>
<a id="tocSparam"></a>
<a id="tocsparam"></a>

```json
{
  "content": "string",
  "readRule": [
    {
      "ruleId": "string",
      "partern": "string",
      "isOpen": true
    }
  ],
  "volume": 0,
  "voiceId": "string",
  "breakList": [
    {
      "location": "string",
      "duration": "string"
    }
  ],
  "phonemeList": [
    {
      "ph": "string",
      "alphabet": "string",
      "begin": 0,
      "end": 0
    }
  ],
  "prosodyList": [
    {
      "rate": "string",
      "begin": 0,
      "end": 0
    }
  ],
  "silentList": [
    {
      "location": 0,
      "duration": 0
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|content|string|true|none|断句内容|none|
|readRule|[object]|true|none||none|
|» ruleId|string|true|none||none|
|» partern|string|true|none||none|
|» isOpen|boolean|true|none|是否打开|none|
|volume|integer|true|none|音量|取值范围：90~240。默认取值：140。|
|voiceId|string|true|none|音色ID|none|
|breakList|[object]|false|none|停顿|标签关系：不能包含其他任何标签。<br />不能包含其他任何标签|
|» location|string|true|none|标签插入位置|下面所有插入位置默认在字符后。|
|» duration|string|true|none|停顿时间|单位ms|
|phonemeList|[object]|false|none|多音字|标签关系：可以包含文本，不可以包含其他标签|
|» ph|string|true|none|多音字|输入汉语拼音时，声调用1、2、3、4来表示，5表示轻声。标签起始和结束中间只能有1个汉字。举例1：天气的ph取值为“tian1 qi4”。举例2：Token的ph取值为“ˈtəʊkən”。|
|» alphabet|string|false|none|音标符号系统|使用音标时必选，举例：<phoneme alphabet="ipa" ph="təˈmɑːtəʊ">tomato</phoneme>，参数为ipa时，使用国际音标系统。|
|» begin|integer|true|none|起始标签插入位置|none|
|» end|integer|true|none|结束标签插入位置|none|
|prosodyList|[object]|false|none|局部语速|标签关系：可以包含文本，不可以包含其他标签。|
|» rate|string|true|none|语速百分比值|最小值50，最大值200。示例：50，表示用0.5倍速度阅读|
|» begin|integer|true|none||none|
|» end|integer|true|none||none|
|silentList|[object]|true|none|静音|none|
|» location|integer|true|none||none|
|» duration|integer|true|none||none|

<h2 id="tocS_TaskDetailDTO">TaskDetailDTO</h2>

<a id="schemataskdetaildto"></a>
<a id="schema_TaskDetailDTO"></a>
<a id="tocStaskdetaildto"></a>
<a id="tocstaskdetaildto"></a>

```json
{
  "taskId": 0,
  "mergeId": 0,
  "content": "string",
  "charCount": 0,
  "status": 0,
  "audioUrl": "string",
  "audioDuration": 0,
  "createdAt": "string",
  "updatedAt": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|taskId|integer(int64)|false|none||none|
|mergeId|integer|false|none||none|
|content|string|false|none||none|
|charCount|integer|false|none||none|
|status|integer|false|none||0，拆句完成，1，语音合成中，2，语音合成成功，3，语音合成失败，4，语音合并中，5，语音合并成功，6，语音合并失败|
|audioUrl|string|false|none||none|
|audioDuration|integer|false|none||none|
|createdAt|string|false|none||none|
|updatedAt|string|false|none||none|

<h2 id="tocS_ApiResponseTaskDetailDTO">ApiResponseTaskDetailDTO</h2>

<a id="schemaapiresponsetaskdetaildto"></a>
<a id="schema_ApiResponseTaskDetailDTO"></a>
<a id="tocSapiresponsetaskdetaildto"></a>
<a id="tocsapiresponsetaskdetaildto"></a>

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "taskId": 0,
    "mergeId": 0,
    "content": "string",
    "charCount": 0,
    "status": 0,
    "audioUrl": "string",
    "audioDuration": 0,
    "createdAt": "string",
    "updatedAt": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|message|string|false|none||none|
|data|[TaskDetailDTO](#schemataskdetaildto)|false|none||none|

<h2 id="tocS_SynthesisResultDTO">SynthesisResultDTO</h2>

<a id="schemasynthesisresultdto"></a>
<a id="schema_SynthesisResultDTO"></a>
<a id="tocSsynthesisresultdto"></a>
<a id="tocssynthesisresultdto"></a>

```json
{
  "audioUrl": "string",
  "audioDuration": 0,
  "synthesisStatus": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|audioUrl|string|false|none||none|
|audioDuration|integer|false|none||none|
|synthesisStatus|integer|false|none||合成状态（0-未合成，1-合成中，2-已合成，3-合成失败）|

<h2 id="tocS_TaskSynthesisStatusDTO">TaskSynthesisStatusDTO</h2>

<a id="schematasksynthesisstatusdto"></a>
<a id="schema_TaskSynthesisStatusDTO"></a>
<a id="tocStasksynthesisstatusdto"></a>
<a id="tocstasksynthesisstatusdto"></a>

```json
{
  "status": 0,
  "progress": 0,
  "total": 0,
  "completed": 0,
  "pending": 0,
  "audioUrlList": [
    {
      "sequence": 0,
      "audioUrl": "string"
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|status|integer|false|none|合成状态|0-未合成，1-合成中，2-已合成，3-合成失败|
|progress|integer|false|none||进度，1-100|
|total|integer|false|none||需要合成的断句总数，包括未改动的断句|
|completed|integer|false|none||已完成合成的断句数|
|pending|integer|false|none||待完成合成的断句数|
|audioUrlList|[object]|false|none||none|
|» sequence|integer|false|none||none|
|» audioUrl|string|false|none||none|

<h2 id="tocS_ApiResponseTaskSynthesisStatusDTO">ApiResponseTaskSynthesisStatusDTO</h2>

<a id="schemaapiresponsetasksynthesisstatusdto"></a>
<a id="schema_ApiResponseTaskSynthesisStatusDTO"></a>
<a id="tocSapiresponsetasksynthesisstatusdto"></a>
<a id="tocsapiresponsetasksynthesisstatusdto"></a>

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "status": 0,
    "progress": 0,
    "total": 0,
    "completed": 0,
    "pending": 0,
    "audioUrlList": [
      {
        "sequence": 0,
        "audioUrl": "string"
      }
    ]
  },
  "timestamp": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|message|string|false|none||none|
|data|[TaskSynthesisStatusDTO](#schematasksynthesisstatusdto)|false|none||none|
|timestamp|integer(int64)|false|none||none|

<h2 id="tocS_VoiceConfigDTO">VoiceConfigDTO</h2>

<a id="schemavoiceconfigdto"></a>
<a id="schema_VoiceConfigDTO"></a>
<a id="tocSvoiceconfigdto"></a>
<a id="tocsvoiceconfigdto"></a>

```json
{
  "voiceId": "string",
  "voiceName": "string",
  "voiceType": "string",
  "sortOrder": 0,
  "avatar_url": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|voiceId|string|false|none||none|
|voiceName|string|false|none||none|
|voiceType|string|false|none||none|
|sortOrder|integer|false|none||none|
|avatar_url|string|false|none||头像url|

<h2 id="tocS_VoiceConfigListResponseDTO">VoiceConfigListResponseDTO</h2>

<a id="schemavoiceconfiglistresponsedto"></a>
<a id="schema_VoiceConfigListResponseDTO"></a>
<a id="tocSvoiceconfiglistresponsedto"></a>
<a id="tocsvoiceconfiglistresponsedto"></a>

```json
{
  "list": [
    {
      "voiceId": "string",
      "voiceName": "string",
      "voiceType": "string",
      "sortOrder": 0,
      "avatar_url": "string"
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|list|[[VoiceConfigDTO](#schemavoiceconfigdto)]|false|none||none|

<h2 id="tocS_ApiResponseVoiceConfigListResponseDTO">ApiResponseVoiceConfigListResponseDTO</h2>

<a id="schemaapiresponsevoiceconfiglistresponsedto"></a>
<a id="schema_ApiResponseVoiceConfigListResponseDTO"></a>
<a id="tocSapiresponsevoiceconfiglistresponsedto"></a>
<a id="tocsapiresponsevoiceconfiglistresponsedto"></a>

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "list": [
      {
        "voiceId": "string",
        "voiceName": "string",
        "voiceType": "string",
        "sortOrder": 0,
        "avatar_url": "string"
      }
    ]
  },
  "timestamp": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|message|string|false|none||none|
|data|[VoiceConfigListResponseDTO](#schemavoiceconfiglistresponsedto)|false|none||none|
|timestamp|integer(int64)|false|none||none|

<h2 id="tocS_ReadingRuleDTO">ReadingRuleDTO</h2>

<a id="schemareadingruledto"></a>
<a id="schema_ReadingRuleDTO"></a>
<a id="tocSreadingruledto"></a>
<a id="tocsreadingruledto"></a>

```json
{
  "ruleId": 0,
  "pattern": "string",
  "ruleType": "string",
  "ruleValue": "string",
  "isOpen": true
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|ruleId|integer|false|none||none|
|pattern|string|false|none|原文本|none|
|ruleType|string|false|none||none|
|ruleValue|string|false|none||none|
|isOpen|boolean|true|none|全局开关状态|0，关闭，1，打开|

<h2 id="tocS_ReadingRuleCreateRequest">ReadingRuleCreateRequest</h2>

<a id="schemareadingrulecreaterequest"></a>
<a id="schema_ReadingRuleCreateRequest"></a>
<a id="tocSreadingrulecreaterequest"></a>
<a id="tocsreadingrulecreaterequest"></a>

```json
{
  "pattern": "string",
  "ruleType": "string",
  "ruleValue": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|pattern|string|true|none|原始词|none|
|ruleType|string|true|none|规范类型|1.数字英文，2音标调整，3.专有词汇|
|ruleValue|string|true|none|自定义读法|类型为SAY_AS时，只允许传下面的值。<br />number：数字<br />date：日期<br />figure：数值<br />phone：电话号码<br />english：英文单词<br />spell：逐个字母读英文|

<h2 id="tocS_ReadingRuleListResponseDTO">ReadingRuleListResponseDTO</h2>

<a id="schemareadingrulelistresponsedto"></a>
<a id="schema_ReadingRuleListResponseDTO"></a>
<a id="tocSreadingrulelistresponsedto"></a>
<a id="tocsreadingrulelistresponsedto"></a>

```json
{
  "readingRuleList": [
    {
      "ruleId": 0,
      "pattern": "string",
      "ruleType": "string",
      "ruleValue": "string",
      "isOpen": true
    }
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|readingRuleList|[[ReadingRuleDTO](#schemareadingruledto)]|false|none||none|

<h2 id="tocS_ApiResponseReadingRuleApplyResponseDTO">ApiResponseReadingRuleApplyResponseDTO</h2>

<a id="schemaapiresponsereadingruleapplyresponsedto"></a>
<a id="schema_ApiResponseReadingRuleApplyResponseDTO"></a>
<a id="tocSapiresponsereadingruleapplyresponsedto"></a>
<a id="tocsapiresponsereadingruleapplyresponsedto"></a>

```json
{
  "code": 0,
  "message": "string",
  "data": "string",
  "timestamp": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|message|string|false|none||none|
|data|string|false|none||none|
|timestamp|integer(int64)|false|none||none|

<h2 id="tocS_AudioMergeRequest">AudioMergeRequest</h2>

<a id="schemaaudiomergerequest"></a>
<a id="schema_AudioMergeRequest"></a>
<a id="tocSaudiomergerequest"></a>
<a id="tocsaudiomergerequest"></a>

```json
{
  "taskId": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|taskId|integer|true|none||none|


<h2 id="tocS_OriginalSentenceDeleteRequest">OriginalSentenceDeleteRequest</h2>

<a id="schemaoriginalsentencedeleterequest"></a>
<a id="schema_OriginalSentenceDeleteRequest"></a>
<a id="tocSoriginalsentencedeleterequest"></a>
<a id="tocsoriginalsentencedeleterequest"></a>

```json
{
  "originalSentenceId": 0
}
```

### 属�?

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|originalSentenceId|integer(int64)|true|none||none|

<h2 id="tocS_ReadingRuleSetGlobalSettingRequest">ReadingRuleSetGlobalSettingRequest</h2>

<a id="schemareadingrulesetglobalsettingrequest"></a>
<a id="schema_ReadingRuleSetGlobalSettingRequest"></a>
<a id="tocSreadingrulesetglobalsettingrequest"></a>
<a id="tocsreadingrulesetglobalsettingrequest"></a>

```json
{
  "taskId": 0,
  "ruleId": 0,
  "isOpen": true
}
```

### 属�?

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|taskId|integer(int64)|true|none||none|
|ruleId|integer(int64)|true|none||none|
|isOpen|boolean|true|none||0关闭�?打开|

<h2 id="tocS_MatchingFieldDTO">MatchingFieldDTO</h2>

<a id="schemamatchingfielddto"></a>
<a id="schema_MatchingFieldDTO"></a>
<a id="tocSmatchingfielddto"></a>
<a id="tocsmatchingfielddto"></a>

```json
{
  "ruleId": 0,
  "location": 0,
  "pattern": "string",
  "isOpen": true
}
```

### 属�?

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|ruleId|integer(int64)|false|none||none|
|location|integer|false|none|字符位置|匹配字段第一个字符的位置|
|pattern|string|false|none|字段|none|
|isOpen|boolean|true|none||none|

<h2 id="tocS_MatchingFieldListResponseDTO">MatchingFieldListResponseDTO</h2>

<a id="schemamatchingfieldlistresponsedto"></a>
<a id="schema_MatchingFieldListResponseDTO"></a>
<a id="tocSmatchingfieldlistresponsedto"></a>
<a id="tocsmatchingfieldlistresponsedto"></a>

```json
{
  "total": 0,
  "fieldList": [
    {
      "ruleId": 0,
      "location": 0,
      "pattern": "string",
      "isOpen": true
    }
  ]
}
```

### 属�?

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|total|integer|false|none||none|
|fieldList|[[MatchingFieldDTO](#schemamatchingfielddto)]|false|none||none|

<h2 id="tocS_ReadingRuleListItemDTO">ReadingRuleListItemDTO</h2>

<a id="schemareadingrulelistitemdto"></a>
<a id="schema_ReadingRuleListItemDTO"></a>
<a id="tocSreadingrulelistitemdto"></a>
<a id="tocsreadingrulelistitemdto"></a>

```json
{
  "ruleId": 0,
  "pattern": "string",
  "ruleType": "string",
  "ruleValue": "string",
  "isOpen": true
}
```

### 属�?

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|ruleId|integer(int64)|false|none||none|
|pattern|string|false|none|原文本|none|
|ruleType|string|false|none||none|
|ruleValue|string|false|none||none|
|isOpen|boolean|true|none|全局开关状态|0关闭�?打开|

<h2 id="tocS_ReadingRuleListPageResponseDTO">ReadingRuleListPageResponseDTO</h2>

<a id="schemareadingrulelistpageresponsedto"></a>
<a id="schema_ReadingRuleListPageResponseDTO"></a>
<a id="tocSreadingrulelistpageresponsedto"></a>
<a id="tocsreadingrulelistpageresponsedto"></a>

```json
{
  "readingRuleList": [
    {
      "ruleId": 0,
      "pattern": "string",
      "ruleType": "string",
      "ruleValue": "string",
      "isOpen": true
    }
  ],
  "total": 0,
  "page": 0,
  "pageSize": 0
}
```

### 属�?

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|readingRuleList|[[ReadingRuleListItemDTO](#schemareadingrulelistitemdto)]|false|none||none|
|total|integer|false|none||none|
|page|integer|false|none||none|
|pageSize|integer|false|none||none|

<h2 id="tocS_ApiResponseMatchingFieldListResponseDTO">ApiResponseMatchingFieldListResponseDTO</h2>

<a id="schemaapiresponsematchingfieldlistresponsedto"></a>
<a id="schema_ApiResponseMatchingFieldListResponseDTO"></a>
<a id="tocSapiresponsematchingfieldlistresponsedto"></a>
<a id="tocsapiresponsematchingfieldlistresponsedto"></a>

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "total": 0,
    "fieldList": [
      {
        "ruleId": 0,
        "location": 0,
        "pattern": "string",
        "isOpen": true
      }
    ]
  },
  "timestamp": 0
}
```

### 属�?

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|message|string|false|none||none|
|data|[MatchingFieldListResponseDTO](#schemamatchingfieldlistresponsedto)|false|none||none|
|timestamp|integer(int64)|false|none||none|

<h2 id="tocS_ApiResponseReadingRuleListPageResponseDTO">ApiResponseReadingRuleListPageResponseDTO</h2>

<a id="schemaapiresponsereadingrulelistpageresponsedto"></a>
<a id="schema_ApiResponseReadingRuleListPageResponseDTO"></a>
<a id="tocSapiresponsereadingrulelistpageresponsedto"></a>
<a id="tocsapiresponsereadingrulelistpageresponsedto"></a>

```json
{
  "code": 0,
  "message": "string",
  "data": {
    "readingRuleList": [
      {
        "ruleId": 0,
        "pattern": "string",
        "ruleType": "string",
        "ruleValue": "string",
        "isOpen": true
      }
    ],
    "total": 0,
    "page": 0,
    "pageSize": 0
  },
  "timestamp": 0
}
```

### 属�?

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|message|string|false|none||none|
|data|[ReadingRuleListPageResponseDTO](#schemareadingrulelistpageresponsedto)|false|none||none|
|timestamp|integer(int64)|false|none||none|


