<template>
  <div class="edit-page">
    <div class="page-header">
      <h2>精修页面</h2>
    </div>

    <div class="edit-layout" v-loading="loading">
      <!-- 左侧：音色和参数调整 -->
      <div class="left-panel">
        <el-card>
          <template #header>
            <span>音色选择</span>
          </template>
          <el-select v-model="currentSentence.voice" placeholder="选择音色" style="width: 100%">
            <el-option
              v-for="voice in voices"
              :key="voice.value"
              :label="voice.label"
              :value="voice.value"
            />
          </el-select>
        </el-card>

        <el-card style="margin-top: 20px">
          <template #header>
            <span>参数调整</span>
          </template>
          <el-form label-width="80px">
            <el-form-item label="音量">
              <el-slider
                v-model="currentSentence.volume"
                :min="0"
                :max="5000"
                :step="100"
                show-input
              />
            </el-form-item>
            <el-form-item label="语速">
              <el-slider
                v-model="currentSentence.speed"
                :min="0"
                :max="200"
                :step="1"
                show-input
              />
            </el-form-item>
            <el-form-item label="音调">
              <el-slider
                v-model="currentSentence.pitch"
                :min="0"
                :max="100"
                :step="1"
                show-input
              />
            </el-form-item>
          </el-form>
        </el-card>
      </div>

      <!-- 中间：句子列表和编辑区域 -->
      <div class="center-panel">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>句子列表</span>
              <el-button size="small" type="primary" @click="handleSaveAll">
                保存所有
              </el-button>
            </div>
          </template>

          <div class="sentence-list">
            <div
              v-for="(sentence, index) in sentences"
              :key="sentence.sentence_id"
              class="sentence-item"
              :class="{ active: sentence.sentence_id === currentSentenceId }"
              @click="selectSentence(sentence)"
            >
              <div class="sentence-header">
                <span class="sentence-number">句子{{ index + 1 }}</span>
                <div class="sentence-actions">
                  <el-button size="small" link @click.stop="handlePlay(sentence)">
                    播放
                  </el-button>
                  <el-button size="small" link @click.stop="handleSynthesize(sentence.sentence_id)">
                    合成
                  </el-button>
                </div>
              </div>
              <div class="sentence-content">
                {{ sentence.content }}
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右侧：功能按钮 -->
      <div class="right-panel">
        <el-card>
          <template #header>
            <span>编辑功能</span>
          </template>
          <div class="function-buttons">
            <el-button @click="handleBreakStandard">断句标准</el-button>
            <el-button @click="handlePause">停顿</el-button>
            <el-button @click="handlePolyphone">多音字</el-button>
            <el-button @click="handleInsertSilence">插入静音</el-button>
            <el-button @click="handleReadingRule">阅读规范</el-button>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 音频播放器 -->
    <audio
      v-if="currentAudioUrl"
      ref="audioPlayer"
      :src="currentAudioUrl"
      preload="none"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTaskSentences } from '@/api/task'
import { getSentence, updateSentence, synthesizeSentence } from '@/api/sentence'

const route = useRoute()
const loading = ref(false)
const sentences = ref([])
const currentSentenceId = ref(null)
const currentSentence = ref({
  voice: 'default',
  volume: 1000,
  speed: 100,
  pitch: 50
})
const audioPlayer = ref(null)
const currentAudioUrl = ref('')

const taskId = ref('')
const targetSentenceId = ref(null)

// 音色选项（示例）
const voices = [
  { label: '默认音色', value: 'default' },
  { label: '女声1', value: 'female1' },
  { label: '女声2', value: 'female2' },
  { label: '男声1', value: 'male1' },
  { label: '男声2', value: 'male2' }
]

onMounted(() => {
  taskId.value = route.query.task_id
  targetSentenceId.value = route.query.sentence_id

  if (taskId.value) {
    loadSentences()
  } else {
    ElMessage.error('缺少任务ID参数')
  }
})

const loadSentences = async () => {
  loading.value = true
  try {
    const data = await getTaskSentences(taskId.value)
    sentences.value = data.sentences || []
    
    // 如果有目标句子ID，自动选中
    if (targetSentenceId.value) {
      const targetSentence = sentences.value.find(s => s.sentence_id == targetSentenceId.value)
      if (targetSentence) {
        selectSentence(targetSentence)
        // 滚动到目标句子
        setTimeout(() => {
          const element = document.querySelector(`.sentence-item.active`)
          if (element) {
            element.scrollIntoView({ behavior: 'smooth', block: 'center' })
          }
        }, 100)
      }
    } else if (sentences.value.length > 0) {
      // 默认选中第一个
      selectSentence(sentences.value[0])
    }
  } catch (error) {
    console.error('加载句子列表失败:', error)
  } finally {
    loading.value = false
  }
}

const selectSentence = async (sentence) => {
  currentSentenceId.value = sentence.sentence_id
  currentAudioUrl.value = sentence.audio_url || ''
  
  // 加载句子详情
  try {
    const detail = await getSentence(sentence.sentence_id)
    currentSentence.value = {
      voice: detail.voice || 'default',
      volume: detail.volume || 1000,
      speed: detail.speed || 100,
      pitch: detail.pitch || 50
    }
  } catch (error) {
    console.error('加载句子详情失败:', error)
  }
}

const handleSaveAll = async () => {
  if (!currentSentenceId.value) {
    ElMessage.warning('请先选择一个句子')
    return
  }

  try {
    await updateSentence(currentSentenceId.value, {
      voice: currentSentence.value.voice,
      volume: currentSentence.value.volume,
      speed: currentSentence.value.speed,
      pitch: currentSentence.value.pitch
    })
    ElMessage.success('保存成功')
  } catch (error) {
    console.error('保存失败:', error)
  }
}

const handlePlay = (sentence) => {
  if (!sentence.audio_url) {
    ElMessage.warning('该句子还没有合成音频')
    return
  }
  currentAudioUrl.value = sentence.audio_url
  if (audioPlayer.value) {
    if (audioPlayer.value.paused) {
      audioPlayer.value.play()
    } else {
      audioPlayer.value.pause()
    }
  }
}

const handleSynthesize = async (sentenceId) => {
  try {
    await synthesizeSentence(sentenceId)
    ElMessage.success('合成中，请稍候...')
    setTimeout(() => {
      loadSentences()
    }, 2000)
  } catch (error) {
    console.error('合成失败:', error)
  }
}

const handleBreakStandard = () => {
  ElMessage.info('断句标准功能开发中...')
}

const handlePause = () => {
  ElMessage.info('停顿功能开发中...')
}

const handlePolyphone = () => {
  ElMessage.info('多音字功能开发中...')
}

const handleInsertSilence = () => {
  ElMessage.info('插入静音功能开发中...')
}

const handleReadingRule = () => {
  ElMessage.info('阅读规范功能开发中...')
}
</script>

<style scoped>
.edit-page {
  padding: 20px;
  min-height: 600px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 500;
}

.edit-layout {
  display: grid;
  grid-template-columns: 250px 1fr 200px;
  gap: 20px;
  min-height: 600px;
}

.left-panel,
.right-panel {
  display: flex;
  flex-direction: column;
}

.center-panel {
  overflow-y: auto;
  max-height: 800px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sentence-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.sentence-item {
  padding: 15px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.sentence-item:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.sentence-item.active {
  border-color: #409eff;
  background: #e1f3ff;
}

.sentence-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.sentence-number {
  font-weight: 500;
  color: #409eff;
}

.sentence-content {
  line-height: 1.6;
  color: #333;
}

.function-buttons {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.function-buttons .el-button {
  width: 100%;
}

@media (max-width: 1400px) {
  .edit-layout {
    grid-template-columns: 200px 1fr 180px;
  }
}
</style>

