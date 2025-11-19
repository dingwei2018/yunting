<template>
  <div class="sentences-page">
    <div class="page-header">
      <h2>自动拆句后结果</h2>
      <el-button type="primary" @click="handleMergeAudio" :loading="merging">
        合并音频
      </el-button>
    </div>

    <el-card v-loading="loading">
      <div v-if="sentences.length === 0" class="empty-state">
        <el-empty description="暂无句子数据" />
      </div>

      <div v-else class="sentence-list">
        <div
          v-for="(sentence, index) in sentences"
          :key="sentence.sentence_id"
          class="sentence-item"
        >
          <div class="sentence-header">
            <span class="sentence-number">拆句{{ index + 1 }}</span>
            <span class="sentence-duration" v-if="sentence.duration">
              {{ formatDuration(sentence.duration) }}
            </span>
          </div>

          <div class="sentence-content">
            {{ sentence.content }}
          </div>

          <div class="sentence-actions">
            <el-button size="small" @click="handleEdit(sentence.sentence_id)">
              编辑
            </el-button>
            <el-button size="small" @click="handleDelete(sentence.sentence_id)">
              删除
            </el-button>
            <el-button size="small" @click="handleInsertAfter(sentence.sentence_id)">
              向下插入
            </el-button>
            <el-button size="small" @click="handleResynthesize(sentence.sentence_id)">
              重新合成
            </el-button>
            <el-button
              size="small"
              type="primary"
              @click="handlePlay(sentence)"
              :disabled="!sentence.audio_url"
            >
              播放
            </el-button>
          </div>

          <!-- 音频播放器 -->
          <audio
            v-if="sentence.audio_url"
            :ref="el => setAudioRef(sentence.sentence_id, el)"
            :src="sentence.audio_url"
            preload="none"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskSentences, mergeAudio } from '@/api/task'
import { deleteSentence, synthesizeSentence, insertSentenceAfter } from '@/api/sentence'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const merging = ref(false)
const sentences = ref([])
const audioRefs = ref({})

const taskId = ref('')

onMounted(() => {
  taskId.value = route.query.task_id
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
  } catch (error) {
    console.error('加载句子列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleEdit = (sentenceId) => {
  router.push(`/edit?task_id=${taskId.value}&sentence_id=${sentenceId}`)
}

const handleDelete = async (sentenceId) => {
  try {
    await ElMessageBox.confirm('确定要删除这个句子吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteSentence(sentenceId)
    ElMessage.success('删除成功')
    loadSentences()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleInsertAfter = async (sentenceId) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入要插入的文本', '向下插入', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入文本'
    })

    await insertSentenceAfter(sentenceId, { content: value })
    ElMessage.success('插入成功')
    loadSentences()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('插入失败:', error)
    }
  }
}

const handleResynthesize = async (sentenceId) => {
  try {
    await synthesizeSentence(sentenceId)
    ElMessage.success('重新合成中，请稍候...')
    // 等待一段时间后刷新列表
    setTimeout(() => {
      loadSentences()
    }, 2000)
  } catch (error) {
    console.error('重新合成失败:', error)
  }
}

const handlePlay = (sentence) => {
  const audio = audioRefs.value[sentence.sentence_id]
  if (audio) {
    if (audio.paused) {
      audio.play()
    } else {
      audio.pause()
    }
  }
}

const handleMergeAudio = async () => {
  merging.value = true
  try {
    await mergeAudio(taskId.value)
    ElMessage.success('合并音频成功')
    loadSentences()
  } catch (error) {
    console.error('合并音频失败:', error)
  } finally {
    merging.value = false
  }
}

const setAudioRef = (sentenceId, el) => {
  if (el) {
    audioRefs.value[sentenceId] = el
  }
}

const formatDuration = (seconds) => {
  if (!seconds) return ''
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}
</script>

<style scoped>
.sentences-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 500;
}

.empty-state {
  padding: 40px;
  text-align: center;
}

.sentence-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sentence-item {
  padding: 15px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fafafa;
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

.sentence-duration {
  font-size: 12px;
  color: #999;
}

.sentence-content {
  margin-bottom: 15px;
  padding: 10px;
  background: #fff;
  border-radius: 4px;
  line-height: 1.6;
}

.sentence-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
</style>

