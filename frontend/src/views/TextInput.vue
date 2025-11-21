<template>
  <div class="text-input-page">
    <div class="text-input-container">
      <h1 class="page-title">请输入需要精修文本</h1>

      <div class="input-wrapper">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="12"
          placeholder="请输入需要精修文本"
          maxlength="10000"
          show-word-limit="false"
        />
        <div class="word-count">{{ charCount }}</div>
      </div>
      <div class="hint-text">字数支持1万字</div>

      <div class="rule-panel">
        <span class="rule-label">拆句规则</span>
        <div class="rule-tags">
          <div
            v-for="rule in splitRuleOptions"
            :key="rule"
            class="rule-tag"
            :class="{ active: form.splitRules.includes(rule) }"
            @click="toggleRule(rule)"
          >
            {{ rule }}
          </div>
        </div>
      </div>

      <div class="actions">
        <el-button class="submit-btn" type="primary" :loading="loading" @click="handleAutoSplit">
          自动拆句
        </el-button>
        <el-button link type="info" @click="handleClear">清空文本</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createTask } from '@/api/task'

const router = useRouter()
const loading = ref(false)
const splitRuleOptions = ['句号', '叹号', '问号', '省略号']

const form = reactive({
  content: '',
  splitRules: [...splitRuleOptions]
})

const charCount = computed(() => form.content.length)

const toggleRule = (rule) => {
  const index = form.splitRules.indexOf(rule)
  if (index > -1) {
    form.splitRules.splice(index, 1)
  } else {
    form.splitRules.push(rule)
  }
}

const handleAutoSplit = async () => {
  if (!form.content.trim()) {
    ElMessage.warning('请输入文本内容')
    return
  }

  if (form.content.length > 10000) {
    ElMessage.warning('文本内容不能超过1万字')
    return
  }

  loading.value = true
  try {
    const result = await createTask({
      content: form.content
    })
    
    ElMessage.success('拆句成功')
    // 跳转到拆句结果页面
    router.push(`/sentences?task_id=${result.task_id}`)
  } catch (error) {
    console.error('拆句失败:', error)
  } finally {
    loading.value = false
  }
}

const handleClear = () => {
  form.content = ''
  ElMessage.info('已清空文本')
}
</script>

<style scoped>
.text-input-page {
  padding: 60px 0;
  display: flex;
  justify-content: center;
  background: #f8fafb;
  min-height: 100%;
}

.text-input-container {
  width: min(1200px, 90%);
  background: #fff;
  padding: 48px 60px 80px;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(15, 24, 44, 0.08);
}

.page-title {
  font-size: 24px;
  color: #1d1f23;
  font-weight: 600;
  margin-bottom: 24px;
}

.input-wrapper {
  position: relative;
}

.word-count {
  position: absolute;
  right: 16px;
  bottom: 12px;
  color: #8c8c8c;
  font-size: 13px;
}

.hint-text {
  margin-top: 12px;
  margin-bottom: 24px;
  font-size: 13px;
  color: #8c8c8c;
}

.rule-panel {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: #f5f8ff;
  border-radius: 8px;
  margin-bottom: 40px;
}

.rule-label {
  font-weight: 500;
  color: #1d1f23;
}

.rule-tags {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.rule-tag {
  padding: 6px 20px;
  border-radius: 999px;
  border: 1px solid #d1d6e4;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.rule-tag.active {
  background: #2f7bff;
  color: #fff;
  border-color: #2f7bff;
  box-shadow: 0 6px 12px rgba(47, 123, 255, 0.3);
}

.actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.submit-btn {
  width: 320px;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
}

@media (max-width: 768px) {
  .text-input-container {
    padding: 32px 24px;
  }

  .rule-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .submit-btn {
    width: 100%;
  }
}
</style>

