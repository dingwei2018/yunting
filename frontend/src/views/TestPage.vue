<template>
  <div class="test-page">
    <div class="test-header">
      <h1>北京云听音频精修 - 测试页面</h1>
      <p>用于测试各个页面的弹框效果</p>
    </div>

    <div class="test-controls">
      <el-card>
        <template #header>
          <span>参数设置</span>
        </template>
        <el-form :inline="true">
          <el-form-item label="任务ID">
            <el-input v-model="taskId" placeholder="请输入任务ID" style="width: 200px" />
          </el-form-item>
          <el-form-item label="句子ID">
            <el-input v-model="sentenceId" placeholder="请输入句子ID（可选）" style="width: 200px" />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card style="margin-top: 20px">
        <template #header>
          <span>页面测试</span>
        </template>
        <div class="button-group">
          <el-button type="primary" @click="openTextInput">打开文本输入页面</el-button>
          <el-button type="success" @click="openSentences">打开拆句结果页面</el-button>
          <el-button type="warning" @click="openEdit">打开精修页面</el-button>
        </div>
      </el-card>
    </div>

    <!-- 弹框容器 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="90%"
      :close-on-click-modal="false"
      :before-close="handleClose"
      destroy-on-close
    >
      <div class="dialog-content">
        <router-view v-if="dialogVisible" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const taskId = ref('')
const sentenceId = ref('')

const openTextInput = () => {
  dialogTitle.value = '文本输入页面'
  dialogVisible.value = true
  router.push('/text-input')
}

const openSentences = () => {
  if (!taskId.value) {
    ElMessage.warning('请输入任务ID')
    return
  }
  dialogTitle.value = '自动拆句后结果页面'
  dialogVisible.value = true
  router.push(`/sentences?task_id=${taskId.value}`)
}

const openEdit = () => {
  if (!taskId.value) {
    ElMessage.warning('请输入任务ID')
    return
  }
  dialogTitle.value = '精修页面'
  dialogVisible.value = true
  let url = `/edit?task_id=${taskId.value}`
  if (sentenceId.value) {
    url += `&sentence_id=${sentenceId.value}`
  }
  router.push(url)
}

const handleClose = () => {
  dialogVisible.value = false
  router.push('/')
}
</script>

<style scoped>
.test-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.test-header {
  text-align: center;
  margin-bottom: 30px;
}

.test-header h1 {
  font-size: 28px;
  margin-bottom: 10px;
}

.test-header p {
  color: #666;
  font-size: 14px;
}

.test-controls {
  margin-bottom: 20px;
}

.button-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.dialog-content {
  min-height: 600px;
}
</style>

