# 接口测试说明文档

## 一、测试脚本使用说明

### 1.1 Python测试脚本

**文件**: `api_test.py`

**依赖安装**:
```bash
pip install requests
```

**使用方法**:
```bash
# 使用默认Base URL (http://localhost:8080/v1)
python api_test.py

# 指定Base URL
python api_test.py http://your-server:8080
```

**功能说明**:
- 自动测试所有21个接口
- 自动管理测试数据（task_id、sentence_id等）
- 输出详细的测试结果和日志
- 支持成功/失败统计

**测试流程**:
1. 创建任务 → 获取task_id和sentence_id
2. 获取音色列表 → 设置默认voice_id
3. 依次测试各个接口
4. 输出测试结果汇总

---

## 二、Postman Collection使用说明

### 2.1 导入Collection

1. 打开Postman
2. 点击左上角 **Import** 按钮
3. 选择文件 `北京云听音频精修系统.postman_collection.json`
4. 导入成功后会看到"北京云听音频精修系统"集合

### 2.2 配置环境变量

在Postman中，需要设置以下环境变量（或Collection变量）：

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `base_url` | 服务器基础URL | `http://localhost:8080` |
| `task_id` | 任务ID | `1` |
| `sentence_id` | 句子ID | `1` |
| `rule_id` | 阅读规范ID | `1` |
| `merge_id` | 合并ID | `1` |

**设置方法**:
1. 点击Collection名称右侧的 **...** 菜单
2. 选择 **Edit**
3. 切换到 **Variables** 标签
4. 修改变量值

### 2.3 使用流程

#### 第一步：创建任务
1. 打开 **任务管理** → **创建任务并自动拆句**
2. 修改请求体中的 `content` 字段
3. 点击 **Send**
4. 从响应中复制 `task_id` 和 `sentence_id`，更新到Collection变量中

#### 第二步：测试其他接口
1. 按照功能模块依次测试接口
2. 注意接口之间的依赖关系：
   - 合成接口需要先有句子
   - 合并接口需要先有合成的音频
   - 应用阅读规范需要先创建阅读规范

#### 第三步：查看结果
- 检查响应状态码（应该是10200表示成功）
- 查看响应数据是否符合预期

---

## 三、接口测试顺序建议

### 3.1 基础功能测试（必须按顺序）

1. **创建任务并自动拆句** (`POST /tasks`)
   - 获取 `task_id` 和 `sentence_id`

2. **获取任务详情** (`GET /tasks/taskid={task_id}`)
   - 验证任务创建成功

3. **获取句子列表** (`GET /tasks/sentences?taskid={task_id}`)
   - 验证拆句成功

4. **获取句子详情** (`GET /sentences/info?sentenceid={sentence_id}`)
   - 验证句子数据

5. **获取音色列表** (`GET /voices`)
   - 获取可用的音色ID

### 3.2 核心功能测试

6. **合成单个句子** (`POST /sentences/synthesize`)
   - 异步操作，需要等待

7. **查询合成状态** (`GET /synthesis/tasks?taskid={task_id}`)
   - 轮询查询，直到合成完成

8. **调整参数** (`POST /sentences/settings`)
   - 测试基础参数调整

9. **调整参数（向下插入句子）** (`POST /sentences/settings`)
   - 测试插入句子功能

10. **调整参数（重新断句）** (`POST /sentences/settings`)
    - 测试重新断句功能

### 3.3 高级功能测试

11. **创建阅读规范** (`POST /reading-rules`)
    - 获取 `rule_id`

12. **获取阅读规范列表** (`GET /reading-rules`)

13. **应用阅读规范** (`POST /reading-rules/apply`)

14. **批量合成** (`POST /tasks/synthesize`)
    - 等待合成完成

15. **合并音频** (`POST /tasks/audio/merge`)
    - 获取 `merge_id`

16. **查询合并状态** (`GET /audio/merges/merge?mergeid={merge_id}`)

### 3.4 其他功能测试

17. **获取断句标准** (`GET /sentence-breaking/standards`)

18. **保存断句标准设置** (`POST /sentence-breaking/settings`)

19. **重新合成句子** (`POST /sentences/resynthesize`)

20. **获取任务列表** (`GET /tasks`)

21. **删除句子** (`DELETE /sentences`)
    - 注意：此操作会删除数据，建议最后测试

---

## 四、常见问题

### 4.1 连接错误

**问题**: `Connection refused` 或 `Timeout`

**解决方案**:
- 检查服务器是否启动
- 检查 `base_url` 配置是否正确
- 检查防火墙设置

### 4.2 参数错误

**问题**: 返回 `10400` 错误码

**解决方案**:
- 检查必填参数是否都提供了
- 检查参数格式是否正确（JSON格式）
- 检查参数值是否在允许范围内

### 4.3 异步操作

**问题**: 合成或合并接口返回成功，但查询状态时显示未完成

**解决方案**:
- 这是正常的，异步操作需要时间
- 使用轮询机制，每隔几秒查询一次状态
- 直到状态变为完成或失败

### 4.4 数据依赖

**问题**: 某些接口测试失败，提示资源不存在

**解决方案**:
- 检查前置接口是否执行成功
- 检查 `task_id`、`sentence_id` 等变量是否正确设置
- 按照建议的测试顺序执行

---

## 五、测试数据准备

### 5.1 测试文本示例

```json
{
  "content": "这是一段测试文本。用于验证系统功能。支持最多10000字。包含多个句子，可以测试拆句功能。"
}
```

### 5.2 测试参数示例

**合成参数**:
```json
{
  "voice_id": "xiaoyan",
  "speech_rate": 0,
  "volume": 0,
  "pitch": 0
}
```

**参数调整**:
```json
{
  "speech_rate": 10,
  "volume": 5,
  "pitch": -2,
  "voice_id": "xiaoyan",
  "pauses": [
    {"position": 5, "duration": 500, "type": 1}
  ]
}
```

---

## 六、测试检查清单

### 6.1 功能测试

- [ ] 任务创建和查询
- [ ] 句子列表和详情
- [ ] 语音合成（单个、批量、重新合成）
- [ ] 参数调整（基础参数、插入句子、重新断句）
- [ ] 阅读规范（创建、列表、应用）
- [ ] 音频合并
- [ ] 音色管理
- [ ] 断句标准

### 6.2 异常测试

- [ ] 超过10000字的文本（应该返回错误）
- [ ] 不存在的task_id（应该返回404）
- [ ] 不存在的sentence_id（应该返回404）
- [ ] 无效的参数值（应该返回400）

### 6.3 性能测试

- [ ] 大量句子的任务创建
- [ ] 批量合成性能
- [ ] 音频合并性能

---

## 七、联系与支持

如有问题，请参考：
- `接口完整说明书.md` - 详细的接口文档
- `开发任务拆解.md` - 开发任务说明
- `服务模块架构设计.md` - 系统架构说明

---

**文档版本**: v1.0  
**创建时间**: 2024年

