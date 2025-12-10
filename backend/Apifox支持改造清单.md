# Apifox 文档生成支持改造清单

## 一、Apifox Helper 插件要求概述

根据 Apifox 官方文档，Apifox Helper 插件通过解析 Java 代码中的 Javadoc 注释来自动生成 API 文档。要完整支持 Apifox 的文档生成标准，需要遵循以下规范：

### 1. 类级别注释要求
- **必须**：在控制器类的 Javadoc 注释中，第一行可作为文件名称（用于生成文件夹结构）
- **推荐**：使用 `@module` 标签指定 API 所属的项目模块
- **格式**：`父目录/文件名称` 格式可自动生成对应的文件夹结构

### 2. 方法级别注释要求
- **必须**：提供详细的 Javadoc 注释，包括方法描述
- **必须**：使用 `@param` 标签描述所有方法参数
- **必须**：使用 `@return` 标签描述返回值
- **推荐**：对于可选参数，在注释中注明其可选性和默认值

### 3. DTO 类注释要求
- **推荐**：为 DTO 类的字段添加 Javadoc 注释，便于生成详细的字段说明

---

## 二、当前项目状态分析

### ✅ 已完成的注释

1. **TaskController** - 部分完成
   - ✅ 类级别有注释
   - ✅ 方法有 Javadoc 注释
   - ✅ 使用了 `@param` 和 `@return` 标签

2. **ReadingRuleController** - 部分完成
   - ✅ 类级别有注释
   - ✅ 方法有 Javadoc 注释
   - ✅ 使用了 `@param` 和 `@return` 标签

3. **SynthesisController** - 部分完成
   - ✅ 部分方法有 Javadoc 注释
   - ✅ 部分方法使用了 `@param` 和 `@return` 标签

### ❌ 缺失的注释

1. **HealthController** - 完全缺失
   - ❌ 无类级别注释
   - ❌ 无方法注释
   - ❌ 无 `@param` 和 `@return` 标签

2. **OriginalSentenceController** - 完全缺失
   - ❌ 无类级别注释
   - ❌ 无方法注释
   - ❌ 无 `@param` 和 `@return` 标签

3. **VoiceConfigController** - 完全缺失
   - ❌ 无类级别注释
   - ❌ 无方法注释
   - ❌ 无 `@param` 和 `@return` 标签

4. **AudioMergeController** - 完全缺失
   - ❌ 无类级别注释
   - ❌ 无方法注释
   - ❌ 无 `@param` 和 `@return` 标签

5. **SynthesisController** - 部分缺失
   - ❌ 部分方法缺少完整的 Javadoc 注释
   - ❌ 部分方法缺少 `@param` 和 `@return` 标签

### ⚠️ 需要改进的地方

1. **所有 Controller** - 缺少 `@module` 标签
   - 需要在类级别 Javadoc 中添加 `@module` 标签，用于模块分类

2. **DTO 类** - 缺少字段注释
   - 建议为 DTO 类的字段添加 Javadoc 注释，特别是：
     - Request DTO 的字段
     - Response DTO 的字段
     - 嵌套在 List 中的 DTO 字段

---

## 三、需要改造的具体内容

### 1. 类级别注释改造（所有 Controller）

**改造前：**
```java
/**
 * 任务管理控制器
 */
@RestController
@RequestMapping("/api/task")
public class TaskController {
```

**改造后：**
```java
/**
 * 任务管理/任务管理控制器
 * @module 任务管理
 */
@RestController
@RequestMapping("/api/task")
public class TaskController {
```

**需要改造的 Controller：**
- [ ] TaskController
- [ ] OriginalSentenceController
- [ ] SynthesisController
- [ ] ReadingRuleController
- [ ] VoiceConfigController
- [ ] AudioMergeController
- [ ] HealthController

### 2. 方法级别注释改造

#### 2.1 HealthController

**需要添加：**
```java
/**
 * 健康检查
 * 检查服务运行状态
 *
 * @return 服务状态信息，包含 status 和 message 字段
 */
@GetMapping("/health")
public ApiResponse<Map<String, String>> health() {
```

#### 2.2 OriginalSentenceController

**需要添加：**
```java
/**
 * 获取拆句列表
 * 获取指定任务下的拆句列表，支持分页
 *
 * @param taskId 任务ID（必填）
 * @param page 页码（可选，默认为1）
 * @param pageSize 每页大小（可选，默认为10）
 * @return 拆句列表响应，包含列表数据、总数、页码和每页大小
 */
@GetMapping("/getOriginalSentenceList")
public ApiResponse<OriginalSentenceListResponseDTO> getOriginalSentenceList(...) {

/**
 * 删除拆句
 * 删除指定的拆句及其关联的断句
 *
 * @param request 删除请求，包含 originalSentenceId
 * @return 删除结果，成功返回"删除成功"
 */
@PostMapping("/delete")
public ApiResponse<String> deleteOriginalSentence(...) {
```

#### 2.3 VoiceConfigController

**需要添加：**
```java
/**
 * 获取语音列表
 * 获取所有可用的语音配置列表
 *
 * @return 语音配置列表响应，包含所有可用的语音配置
 */
@GetMapping("/getList")
public ApiResponse<VoiceConfigListResponseDTO> getVoices() {
```

#### 2.4 AudioMergeController

**需要添加：**
```java
/**
 * 合并音频
 * 合并任务下的所有音频文件
 *
 * @param request 合并请求，包含 taskId
 * @return 合并响应，包含 mergeId
 */
@PostMapping("/audio")
public ApiResponse<AudioMergeResponseDTO> mergeAudio(...) {

/**
 * 获取合并状态
 * 查询音频合并任务的状态
 *
 * @param mergeId 合并任务ID（必填）
 * @return 合并状态响应，包含任务ID、合并后的音频URL、时长和状态
 */
@GetMapping("/getStatus")
public ApiResponse<AudioMergeStatusDTO> getStatus(...) {
```

#### 2.5 SynthesisController - 补充缺失的注释

**需要补充的方法：**
- [ ] `synthesizeBreakingSentence` - 需要补充 `@param` 和 `@return`
- [ ] `synthesizeOriginalSentence` - 需要补充 `@param` 和 `@return`
- [ ] `synthesizeTask` - 需要补充 `@param` 和 `@return`
- [ ] `setConfig` - 需要补充 `@param` 和 `@return`

**示例：**
```java
/**
 * 合成断句
 * 合成或重新合成单个断句
 *
 * @param request 合成请求，包含 breakingSentenceId
 * @return 合成状态，成功返回"合成请求已提交"
 */
@PostMapping("/breakingSentence")
public ApiResponse<String> synthesizeBreakingSentence(
        @RequestBody SynthesisBreakingSentenceRequest request) {
```

### 3. DTO 类字段注释（推荐，非必须）

**建议为以下 DTO 添加字段注释：**

#### 3.1 Request DTO
- [ ] TaskCreateRequest
- [ ] OriginalSentenceDeleteRequest
- [ ] SynthesisBreakingSentenceRequest
- [ ] SynthesisOriginalSentenceRequest
- [ ] SynthesisTaskRequest
- [ ] SynthesisSetConfigRequest
- [ ] ReadingRuleCreateRequest
- [ ] ReadingRuleSetGlobalSettingRequest
- [ ] AudioMergeRequest

**示例：**
```java
public class TaskCreateRequest {
    /**
     * 文本内容，不能为空，最多10000字
     */
    @NotBlank(message = "文本内容不能为空")
    @Size(max = 10000, message = "文本内容不能超过10000字")
    private String content;

    /**
     * 自定义拆句符号类型数组，可选；为空时使用默认符号集
     * 1：中文句号（。）
     * 2：中文叹号（！）
     * 3：中文问号（？）
     * 4：中文省略号（…）
     */
    private List<Integer> delimiterList;
}
```

#### 3.2 Response DTO（重点：List 中的 DTO）
- [ ] TaskCreateResponseDTO
- [ ] TaskDetailDTO
- [ ] OriginalSentenceDTO
- [ ] OriginalSentenceListResponseDTO
- [ ] OriginalSentenceListItemDTO
- [ ] BreakingSentenceWithSettingDTO
- [ ] BreakingSentenceSettingDTO
- [ ] BreakConfigDTO
- [ ] PhonemeConfigDTO
- [ ] ProsodyConfigDTO
- [ ] SilenceConfigDTO
- [ ] SynthesisResultDTO
- [ ] OriginalSentenceSynthesisStatusDTO
- [ ] TaskSynthesisStatusDTO
- [ ] AudioUrlItem
- [ ] ReadingRuleCreateResponseDTO
- [ ] ReadingRuleListPageResponseDTO
- [ ] ReadingRuleListItemDTO
- [ ] MatchingFieldListResponseDTO
- [ ] MatchingFieldDTO
- [ ] VoiceConfigListResponseDTO
- [ ] VoiceConfigDTO
- [ ] AudioMergeResponseDTO
- [ ] AudioMergeStatusDTO

**示例（重点：List 中的 DTO）：**
```java
public class OriginalSentenceListResponseDTO {
    /**
     * 拆句列表
     */
    private List<OriginalSentenceListItemDTO> list;

    /**
     * 总记录数
     */
    private Long total;
    
    // ...
}

public class OriginalSentenceListItemDTO {
    /**
     * 拆句ID
     */
    private Long originalSentenceId;

    /**
     * 序号
     */
    private Integer sequence;

    /**
     * 拆句内容
     */
    private String content;

    /**
     * 合成状态（0-未合成，1-合成中，2-已合成，3-合成失败）
     */
    private Integer synthesisStatus;

    /**
     * 断句列表
     */
    private List<BreakingSentenceWithSettingDTO> breakingSentenceList;
}
```

---

## 四、改造优先级

### 🔴 高优先级（必须完成）

1. **所有 Controller 类级别添加 `@module` 标签**
   - 影响：文档分类和模块组织
   - 工作量：小（7 个 Controller）

2. **补充缺失的方法注释**
   - HealthController
   - OriginalSentenceController
   - VoiceConfigController
   - AudioMergeController
   - SynthesisController（部分方法）

### 🟡 中优先级（强烈推荐）

3. **完善现有方法的 `@param` 和 `@return` 注释**
   - 确保所有参数都有详细说明
   - 确保返回值描述清晰

4. **为 Request DTO 添加字段注释**
   - 帮助前端理解请求参数
   - 工作量：中等（约 9 个 DTO）

### 🟢 低优先级（可选）

5. **为 Response DTO 添加字段注释**
   - 特别是 List 中的 DTO 字段
   - 工作量：大（约 20+ 个 DTO）
   - 收益：文档更详细，但可以通过 OpenAPI 文档补充

---

## 五、改造步骤建议

### 第一步：安装和配置 Apifox Helper 插件

1. 在 IntelliJ IDEA 中安装 "Apifox Helper" 插件
2. 配置 Apifox 访问令牌和项目 ID
3. 测试插件是否正常工作

### 第二步：完成高优先级改造

1. 为所有 Controller 添加 `@module` 标签
2. 补充缺失的方法注释（HealthController、OriginalSentenceController 等）
3. 使用插件测试文档生成效果

### 第三步：完善中优先级改造

1. 完善现有方法的注释
2. 为 Request DTO 添加字段注释
3. 再次测试文档生成效果

### 第四步：可选的低优先级改造

1. 为 Response DTO 添加字段注释
2. 优化注释描述，使其更加详细和准确

---

## 六、改造示例模板

### Controller 类模板

```java
/**
 * 模块名称/控制器名称
 * 控制器功能描述
 * @module 模块名称
 */
@RestController
@RequestMapping("/api/xxx")
public class XxxController {
    
    /**
     * 接口功能描述
     * 详细说明（可选）
     *
     * @param param1 参数1描述（必填/可选，默认值）
     * @param param2 参数2描述（必填/可选，默认值）
     * @return 返回值描述，包含哪些字段
     */
    @PostMapping("/xxx")
    public ApiResponse<XxxResponseDTO> xxxMethod(
            @RequestBody XxxRequest request,
            @RequestParam(value = "param", required = false) String param) {
        // ...
    }
}
```

### DTO 类模板

```java
/**
 * DTO 功能描述
 */
public class XxxDTO {
    /**
     * 字段描述
     * 详细说明（可选）
     */
    private String fieldName;
    
    /**
     * 列表字段描述
     */
    private List<ItemDTO> itemList;
}
```

---

## 七、验收标准

### 必须满足的标准

1. ✅ 所有 Controller 都有类级别注释和 `@module` 标签
2. ✅ 所有接口方法都有 Javadoc 注释
3. ✅ 所有接口方法都有 `@param` 标签（对于有参数的方法）
4. ✅ 所有接口方法都有 `@return` 标签
5. ✅ 使用 Apifox Helper 插件可以成功生成文档
6. ✅ 生成的文档包含所有接口
7. ✅ 生成的文档中 List 字段下的 DTO 字段可以正常显示

### 推荐满足的标准

8. ⭐ Request DTO 的字段都有注释
9. ⭐ Response DTO 的字段都有注释（特别是 List 中的 DTO）
10. ⭐ 注释描述详细、准确、易懂

---

## 八、注意事项

1. **注释格式**：使用标准的 Javadoc 格式，确保插件能正确解析
2. **模块命名**：`@module` 标签的值应该统一、规范，便于文档分类
3. **参数描述**：对于可选参数，明确标注"可选"和默认值
4. **返回值描述**：详细说明返回的数据结构，特别是 List 中的内容
5. **保持同步**：代码修改后，及时更新注释，保持文档与代码同步

---

## 九、参考资源

- [Apifox Helper 插件文档](https://docs.apifox.com/generate-api-docs-with-idea)
- [Javadoc 规范](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html)
- 项目现有的 OpenAPI 文档（`openapi.yaml`）可作为字段描述的参考

---

## 十、总结

要完整支持 Apifox 的文档生成标准，当前项目需要：

1. **必须完成**：
   - 为所有 7 个 Controller 添加 `@module` 标签
   - 补充 4 个 Controller 的完整方法注释
   - 完善 SynthesisController 的部分方法注释

2. **强烈推荐**：
   - 为所有 Request DTO 添加字段注释
   - 完善现有方法的参数和返回值描述

3. **可选**：
   - 为 Response DTO 添加字段注释（特别是 List 中的 DTO）

预计工作量：
- 高优先级：2-3 小时
- 中优先级：3-4 小时
- 低优先级：6-8 小时

总计：约 11-15 小时（如果只完成高优先级和中优先级，约 5-7 小时）
