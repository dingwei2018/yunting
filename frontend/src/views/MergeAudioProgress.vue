<template>
  <div class="merge-audio-progress-page">
    <div class="page-header">
      <el-button type="text" @click="handleBack" class="back-button">
        <el-icon><ArrowLeft /></el-icon>
        返回音频精修
      </el-button>
    </div>

    <div class="progress-container">
      <div class="progress-content">
        <div class="progress-title">合并音频中</div>
        
        <div class="progress-bar-wrapper">
          <el-progress
            :percentage="progress"
            :status="progressStatus"
            :stroke-width="8"
          />
        </div>

        <div class="progress-text">
          <p v-if="progress < 100">{{ progressMessage }}</p>
          <p v-else class="success-message">合并完成！</p>
        </div>

        <div v-if="progress === 100 && mergedAudioUrl" class="download-section">
          <el-button type="primary" size="large" @click="handleDownload" class="download-button">
            <el-icon><Download /></el-icon>
            下载合并后的音频
          </el-button>
          <div class="download-link-wrapper">
            <span class="download-link-label">或直接访问：</span>
            <a :href="mergedAudioUrl" target="_blank" class="download-link">{{ mergedAudioUrl }}</a>
          </div>
        </div>

        <div class="description">
          <p v-if="progress < 100">正在将所有拆句的音频合并为一个完整的音频文件</p>
          <p v-else-if="!mergedAudioUrl">正在获取音频下载链接...</p>
          <p v-else>合并完成！您可以下载完整的音频文件</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElProgress, ElButton, ElIcon } from 'element-plus'
import { ArrowLeft, Download } from '@element-plus/icons-vue'
import { mergeAudio, getTask } from '@/api/task'

defineOptions({
  name: 'MergeAudioProgress'
})

const route = useRoute()
const router = useRouter()

const progress = ref(0)
const progressStatus = ref(null)
const progressMessage = ref('正在准备合并...')
const mergedAudioUrl = ref('')
let progressTimer = null

const handleBack = () => {
  router.push({
    name: 'Sentences',
    query: route.query
  })
}

const startMerge = async () => {
  const taskId = route.query.taskId || route.params.taskId
  
  if (!taskId) {
    progressMessage.value = '缺少任务ID，无法合并音频'
    progressStatus.value = 'exception'
    return
  }

  try {
    // 模拟进度更新
    progress.value = 0
    progressStatus.value = null
    progressMessage.value = '正在准备合并...'

    // 启动进度模拟
    progressTimer = setInterval(() => {
      if (progress.value < 90) {
        progress.value += Math.random() * 10
        if (progress.value > 90) {
          progress.value = 90
        }
        
        if (progress.value < 30) {
          progressMessage.value = '正在分析音频片段...'
        } else if (progress.value < 60) {
          progressMessage.value = '正在合并音频文件...'
        } else {
          progressMessage.value = '正在处理音频效果...'
        }
      }
    }, 500)

    // 调用合并音频API
    const mergeResult = await mergeAudio(taskId)
    
    // 合并完成
    clearInterval(progressTimer)
    progress.value = 100
    progressStatus.value = 'success'
    progressMessage.value = '合并完成！'

    // 获取合并后的音频 URL
    // API 返回的数据结构：{ merged_audio_url: '...' } 或通过 request 拦截器返回的 data
    if (mergeResult?.merged_audio_url) {
      mergedAudioUrl.value = mergeResult.merged_audio_url
    } else if (mergeResult?.data?.merged_audio_url) {
      mergedAudioUrl.value = mergeResult.data.merged_audio_url
    } else {
      // 如果 API 没有直接返回 URL，尝试从任务信息中获取
      try {
        const taskResult = await getTask(taskId)
        if (taskResult?.merged_audio_url) {
          mergedAudioUrl.value = taskResult.merged_audio_url
        } else if (taskResult?.data?.merged_audio_url) {
          mergedAudioUrl.value = taskResult.data.merged_audio_url
        }
      } catch (err) {
        console.warn('获取任务信息失败:', err)
      }
    }
  } catch (error) {
    console.error('合并音频失败:', error)
    clearInterval(progressTimer)
    progress.value = 0
    progressStatus.value = 'exception'
    progressMessage.value = '合并失败，请重试'
  }
}

onMounted(() => {
  startMerge()
})

const handleDownload = () => {
  if (mergedAudioUrl.value) {
    // 创建一个临时的 a 标签来触发下载
    const link = document.createElement('a')
    link.href = mergedAudioUrl.value
    link.download = 'merged_audio.mp3'
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }
}

onUnmounted(() => {
  if (progressTimer) {
    clearInterval(progressTimer)
  }
})
</script>

<style scoped>
.merge-audio-progress-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.page-header {
  padding: 20px 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
  padding: 0;
}

.back-button:hover {
  color: #409eff;
}

.progress-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

.progress-content {
  width: 100%;
  max-width: 600px;
  background: #fff;
  border-radius: 8px;
  padding: 60px 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.progress-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  text-align: center;
  margin-bottom: 40px;
}

.progress-bar-wrapper {
  margin-bottom: 30px;
}

.progress-text {
  text-align: center;
  margin-bottom: 30px;
  min-height: 24px;
}

.progress-text p {
  font-size: 16px;
  color: #606266;
  margin: 0;
}

.success-message {
  color: #67c23a;
  font-weight: 500;
}

.description {
  text-align: center;
  color: #909399;
  font-size: 14px;
  line-height: 1.8;
}

.description p {
  margin: 8px 0;
}

.download-section {
  margin-top: 40px;
  padding-top: 30px;
  border-top: 1px solid #e4e7ed;
  text-align: center;
}

.download-button {
  margin-bottom: 20px;
  padding: 12px 32px;
  font-size: 16px;
}

.download-button .el-icon {
  margin-right: 8px;
}

.download-link-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.download-link-label {
  font-size: 14px;
  color: #909399;
}

.download-link {
  font-size: 14px;
  color: #409eff;
  text-decoration: none;
  word-break: break-all;
  max-width: 100%;
  padding: 0 12px;
  text-align: center;
}

.download-link:hover {
  text-decoration: underline;
}
</style>

