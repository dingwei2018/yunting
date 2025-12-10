# OpenAPI/Swagger 文档使用说明

## 文档位置

OpenAPI 文档文件：`openapi.yaml`

## 文档特点

1. **完整的接口定义**：包含所有 7 个 Controller 的所有接口
2. **详细的字段定义**：所有 DTO 的字段都有完整定义，包括类型、描述和示例
3. **List 字段完整展开**：所有 List 中存放的 DTO 都有完整的字段定义，可以在【设计】标签下看到所有字段

## 查看方式

### 方式一：使用 Swagger Editor（推荐）

1. 访问 [Swagger Editor](https://editor.swagger.io/)
2. 点击 File -> Import File
3. 选择 `openapi.yaml` 文件
4. 在右侧可以看到完整的 API 文档
5. 点击【设计】标签可以看到所有 Schema 的完整定义

### 方式二：使用 Swagger UI

1. 安装 Swagger UI：
   ```bash
   npm install -g swagger-ui-serve
   ```
2. 启动服务：
   ```bash
   swagger-ui-serve openapi.yaml
   ```
3. 在浏览器中访问显示的地址（通常是 http://localhost:3000）

### 方式三：集成到 Spring Boot 项目

如果项目已经集成了 SpringDoc OpenAPI，可以将 `openapi.yaml` 作为基础文档：

1. 在 `application.properties` 中添加：
   ```properties
   springdoc.api-docs.path=/api-docs
   springdoc.swagger-ui.path=/swagger-ui.html
   ```

2. 访问 `http://localhost:8080/swagger-ui.html` 查看文档

## 文档结构

### 接口分组（Tags）

- **健康检查**：服务健康状态检查
- **任务管理**：任务创建和查询
- **拆句管理**：拆句列表查询和删除
- **合成管理**：音频合成相关接口
- **阅读规范**：阅读规范管理
- **语音配置**：语音配置查询
- **音频合并**：音频合并相关接口

### 重点关注的 List 字段

以下 List 字段中的 DTO 都有完整的字段定义：

1. **TaskCreateResponseDTO.originalSentenceList**
   - 类型：`List<OriginalSentenceDTO>`
   - 完整字段：originalSentenceId, taskId, content, charCount, sequence, createdAt

2. **OriginalSentenceListResponseDTO.list**
   - 类型：`List<OriginalSentenceListItemDTO>`
   - 完整字段：originalSentenceId, sequence, content, synthesisStatus, breakingSentenceList

3. **OriginalSentenceListItemDTO.breakingSentenceList**
   - 类型：`List<BreakingSentenceWithSettingDTO>`
   - 完整字段：包括所有断句信息和 setting 对象

4. **BreakingSentenceWithSettingDTO.setting**
   - 类型：`BreakingSentenceSettingDTO`
   - 包含多个 List：
     - breakList: `List<BreakConfigDTO>`
     - phonemeList: `List<PhonemeConfigDTO>`
     - prosodyList: `List<ProsodyConfigDTO>`
     - silentList: `List<SilenceConfigDTO>`

5. **SynthesisSetConfigRequest.breakingSentenceList**
   - 类型：`List<BreakingSentenceConfig>`
   - 包含多个嵌套的 List 配置

6. **OriginalSentenceSynthesisStatusDTO.audioUrlList**
   - 类型：`List<AudioUrlItem>`
   - 完整字段：sequence, audioUrl

7. **TaskSynthesisStatusDTO.audioUrlList**
   - 类型：`List<AudioUrlItem>`
   - 完整字段：sequence, audioUrl

8. **ReadingRuleListPageResponseDTO.readingRuleList**
   - 类型：`List<ReadingRuleListItemDTO>`
   - 完整字段：ruleId, pattern, ruleType, ruleValue, isOpen

9. **MatchingFieldListResponseDTO.fieldList**
   - 类型：`List<MatchingFieldDTO>`
   - 完整字段：ruleId, location, pattern, isOpen

10. **VoiceConfigListResponseDTO.list**
    - 类型：`List<VoiceConfigDTO>`
    - 完整字段：voiceId, voiceName, voiceType, sortOrder, avatar_url

## 在 Swagger UI 中查看完整字段

1. 打开 Swagger UI
2. 点击任意接口，查看请求/响应示例
3. 点击【设计】或【Schema】标签
4. 展开任意 Schema，可以看到所有字段的完整定义
5. 对于 List 类型的字段，点击展开可以看到其中 DTO 的所有字段

## 验证文档

可以使用以下工具验证 OpenAPI 文档的有效性：

```bash
# 使用 swagger-cli
npm install -g @apidevtools/swagger-cli
swagger-cli validate openapi.yaml

# 或使用 openapi-validator
npm install -g openapi-validator
openapi-validator openapi.yaml
```

## 导出其他格式

如果需要导出为其他格式：

```bash
# 导出为 HTML
npx @redocly/cli build-docs openapi.yaml -o api-docs.html

# 导出为 Postman Collection
npx openapi-to-postman -s openapi.yaml -o postman-collection.json
```

## 注意事项

1. 所有接口都使用统一的 `ApiResponse<T>` 包装返回数据
2. 时间字段使用 `date-time` 格式（ISO 8601）
3. 所有 List 字段中的 DTO 都有完整的字段定义，可以在 Schema 中查看
4. 文档中包含了详细的字段描述和示例值
