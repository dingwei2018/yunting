<template>
  <div class="text-input-page">
    <div class="page-header">
      <h2>音频精修</h2>
    </div>

    <el-card>
      <el-form :model="form" label-width="120px">
        <el-form-item label="拆句规则：">
          <el-checkbox-group v-model="form.splitRules">
            <el-checkbox label="句号">句号</el-checkbox>
            <el-checkbox label="叹号">叹号</el-checkbox>
            <el-checkbox label="问号">问号</el-checkbox>
            <el-checkbox label="省略号">省略号</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="文本内容：">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="请输入需要精修文本"
            maxlength="10000"
            show-word-limit
          />
          <div class="hint-text">字数支持1万字</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleAutoSplit">
            自动拆句
          </el-button>
          <el-button @click="handleClear">清空</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createTask } from '@/api/task'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  content: '',
  splitRules: ['句号', '叹号', '问号', '省略号']
})

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
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 500;
}

.hint-text {
  margin-top: 5px;
  font-size: 12px;
  color: #999;
}
</style>

