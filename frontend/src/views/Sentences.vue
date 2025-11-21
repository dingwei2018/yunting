.sub-textarea-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.sub-textarea-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.insert-tag {
  margin-left: 8px;
  font-size: 12px;
  color: #f59a23;
}

.sub-textarea-item.active {
  border-color: #2f7bff;
  box-shadow: 0 6px 16px rgba(47, 123, 255, 0.2);
}

.sub-textarea-item :deep(.el-textarea__inner) {
  background: #f7f9fc;
  border-radius: 6px;
}

.sentence-row {
  margin-bottom: 12px;
}

<template>
  <div class="sentences-page">
    <div class="page-header">
      <h2>音频精修</h2>
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
          v-for="(sentence, index) in rootSentences"
          :key="sentence.sentence_id"
          class="sentence-item"
        >
          <div class="sentence-layout">
            <div class="sentence-main">
              <div class="sentence-header">
                <span class="sentence-number">拆句{{ index + 1 }}</span>
                <span class="sentence-duration" v-if="sentence.duration">
                  {{ formatDuration(sentence.duration) }}
                </span>
              </div>

              <div class="sentence-row">
                <div class="sentence-content">
                  <div class="sentence-text-row">
                    <div class="sentence-text">
                      {{ sentence.content }}
                    </div>
                    <div class="sentence-links">
                      <SentenceActionLinks
                        :audio-url="sentence.audio_url"
                        @play="handlePlay(sentence)"
                        @synthesize="handleResynthesize(sentence.sentence_id)"
                        @insert-after="handleInsertAfter(sentence.sentence_id)"
                        @delete="handleDelete(sentence.sentence_id)"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="sentence-quick-actions">
              <span class="sentence-status" :class="{ success: !!sentence.audio_url }">
                {{ sentence.audio_url ? '已合成' : '未合成' }}
              </span>
              <span class="sentence-id">ID {{ sentence.sentence_id }}</span>
              <el-button link type="primary" @click="toggleEdit(sentence)">
                {{ editingSentenceId === sentence.sentence_id ? '收起精修' : '编辑' }}
              </el-button>
            </div>
          </div>

          <transition name="fade">
            <div
              v-if="editingSentenceId === sentence.sentence_id"
              class="editing-panel"
            >
              <SentenceTuningPanel
                :voice-categories="voiceCategories"
                :voice-options="voiceOptions"
                :active-voice-category="activeVoiceCategory"
                :custom-options="customOptions"
                :editing-form="editingForm"
                :custom-disabled="customDisabledState"
                :active-actions="activeCustomActions"
                @update:activeVoiceCategory="handleUpdateCategory"
                @select-voice="selectVoice"
                @custom-action="handleCustomAction"
              />

              <div class="sub-textarea-list">
                <div
                  v-for="(sub, subIndex) in getSubSentences(sentence.sentence_id)"
                  :key="sub.sentence_id"
                  class="sub-textarea-item"
                  :class="{ active: editingSubSentenceId === sub.sentence_id }"
                  @click="selectSubSentence(sub)"
                >
                  <div class="textarea-toolbar">
                    <span>
                      输入文本{{ subIndex + 1 }}
                      <!-- <span v-if="sub.parent_id !== 0" class="insert-tag">向下插入</span> -->
                    </span>
                    <span class="textarea-count">
                      {{ sub.content?.length || 0 }}/5000
                    </span>
                  </div>
                  <div class="textarea-input">
                    <RichTextEditor
                      :ref="(el) => setEditorRef(sub.sentence_id, el)"
                      v-model="sub.content"
                      :polyphonic-markers="getPolyphonicMarkers(sub)"
                      :show-polyphonic-hints="isPolyphonicModeActive(sub)"
                      @selection-change="(payload) => handleEditorSelectionChange(sub, payload)"
                      @content-change="() => handleEditorContentChange(sub)"
                      @polyphonic-hover="(payload) => handlePolyphonicHover(sub, payload)"
                    />
                    <div class="textarea-floating-links" @click.stop>
                      <SentenceActionLinks
                        :audio-url="sub.audio_url"
                        @play="handlePlay(sub)"
                        @synthesize="handleResynthesize(sub.sentence_id)"
                        @insert-after="handleInsertAfter(sub.sentence_id)"
                        @delete="handleDelete(sub.sentence_id)"
                      />
                    </div>
                  </div>
                </div>
              </div>
              <div class="textarea-actions">
                <div class="textarea-buttons">
                  <el-button @click="closeEditing">取消</el-button>
                  <el-button type="primary" @click="handleSaveCurrent">
                    保存当前修改
                  </el-button>
                </div>
              </div>
            </div>
          </transition>

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

    <transition name="fade">
      <div
        v-if="polyphonicTooltip.visible"
        class="polyphonic-tooltip"
        :style="{ top: `${polyphonicTooltip.position.y}px`, left: `${polyphonicTooltip.position.x}px` }"
        @mouseenter="cancelTooltipHide"
        @mouseleave="scheduleTooltipHide"
      >
        <div class="tooltip-char">
          {{ polyphonicTooltip.char }}
        </div>
        <div class="tooltip-options">
          <div
            v-for="option in polyphonicTooltip.options"
            :key="option"
            class="tooltip-option"
            :class="{ active: option === polyphonicTooltip.selected }"
            @click.stop="handlePolyphonicOptionSelect(option)"
          >
            {{ option }}
          </div>
        </div>
        <div
          v-if="polyphonicTooltip.selected"
          class="tooltip-reset"
          @click.stop="handlePolyphonicOptionSelect(null)"
        >
          恢复默认
        </div>
      </div>
    </transition>

    <div class="merge-footer">
      <el-button type="primary" size="large" @click="handleMergeAudio" :loading="merging">
        合并音频
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskSentences, mergeAudio } from '@/api/task'
import {
  deleteSentence,
  synthesizeSentence,
  insertSentenceAfter,
  getSentence,
  updateSentence
} from '@/api/sentence'
import { polyphonic } from 'pinyin-pro'
import SentenceTuningPanel from '@/components/SentenceTuningPanel.vue'
import SentenceActionLinks from '@/components/SentenceActionLinks.vue'
import RichTextEditor from '@/components/editor/RichTextEditor.vue'

const route = useRoute()
const loading = ref(false)
const merging = ref(false)
const sentences = ref([])
const audioRefs = ref({})
const editingSentenceId = ref(null)
const editingSubSentenceId = ref(null)
const pendingSelectSubSentenceId = ref(null)

const PAUSE_MARK = '<pause>'

const editingForm = reactive({
  sentenceId: '',
  content: '',
  voice: 'default',
  volume: 70,
  speed: 0,
  pitch: 50
})

const clampVolume = (value) => {
  if (typeof value !== 'number' || Number.isNaN(value)) return 70
  return Math.min(100, Math.max(0, Math.round(value)))
}

const clampSpeed = (value) => {
  if (typeof value !== 'number' || Number.isNaN(value)) return 0
  return Math.min(10, Math.max(-10, Math.round(value)))
}

const sanitizeTextareaValue = (value) => {
  if (typeof value !== 'string') return ''
  return value.replace(/⏸/g, PAUSE_MARK)
}

const normalizeSentenceParams = (sentence) => {
  if (!sentence) return sentence
  sentence.volume = clampVolume(sentence.volume)
  sentence.speed = clampSpeed(sentence.speed)
  return sentence
}

const editorRefs = reactive({})
const pauseEligibilityMap = reactive({})
const polyphonicModeMap = reactive({})
const polyphonicStateMap = reactive({})
const polyphonicTooltip = reactive({
  visible: false,
  sentenceId: null,
  markerId: '',
  char: '',
  options: [],
  selected: '',
  position: { x: 0, y: 0 }
})
let polyphonicTooltipTimer = null

const findSentenceById = (id) =>
  sentences.value.find((item) => item.sentence_id === id)

const currentSubSentence = computed(() =>
  findSentenceById(editingSubSentenceId.value)
)

const isPauseEnabled = computed(() =>
  editingSubSentenceId.value
    ? !!pauseEligibilityMap[editingSubSentenceId.value]
    : false
)

const customDisabledState = computed(() => ({
  pause: !isPauseEnabled.value
}))

const activeCustomActions = computed(() => ({
  polyphonic: editingSubSentenceId.value
    ? !!polyphonicModeMap[editingSubSentenceId.value]
    : false
}))

const rootSentences = computed(() =>
  sentences.value.filter((item) => !item.parent_id || item.parent_id === 0)
)

const getSubSentences = (parentId) => {
  const parent = findSentenceById(parentId)
  if (!parent) return []
  const children = sentences.value
    .filter((item) => item.parent_id === parentId)
    .sort((a, b) => (a.display_order || 0) - (b.display_order || 0))
  return [parent, ...children]
}

const ensurePolyphonicState = (sentenceId) => {
  if (!sentenceId) return null
  if (!polyphonicStateMap[sentenceId]) {
    polyphonicStateMap[sentenceId] = {
      selections: {},
      markers: []
    }
  }
  if (typeof polyphonicModeMap[sentenceId] === 'undefined') {
    polyphonicModeMap[sentenceId] = false
  }
  return polyphonicStateMap[sentenceId]
}

const getPolyphonicMarkers = (sub) => {
  if (!sub) return []
  return polyphonicStateMap[sub.sentence_id]?.markers || []
}

const isPolyphonicModeActive = (sub) => {
  if (!sub) return false
  return !!polyphonicModeMap[sub.sentence_id]
}

const buildPolyphonicMarkers = (sub) => {
  if (!sub) return []
  const content = sub.content || ''
  const sentenceId = sub.sentence_id
  const state = ensurePolyphonicState(sentenceId)
  if (!content) {
    state.markers = []
    return []
  }

  let results = []
  try {
    results = polyphonic(content, { type: 'array' })
  } catch (error) {
    console.warn('polyphonic parse failed:', error)
    state.markers = []
    return []
  }

  const markers = []
  for (let i = 0; i < content.length; i += 1) {
    const char = content[i]
    const optionsRaw = results[i] || []
    const normalizedOptions = (optionsRaw || [])
      .map((item) => (item || '').trim())
      .filter((item) => item && item !== char)
    const uniqueOptions = [...new Set(normalizedOptions)]
    if (uniqueOptions.length <= 1) continue

    const markerId = `${sentenceId}-${i}`
    const selected = state.selections?.[markerId] || null
    markers.push({
      id: markerId,
      sentenceId,
      offset: i,
      length: 1,
      char,
      options: uniqueOptions,
      selected
    })
  }

  state.markers = markers
  return markers
}

const refreshPolyphonicForSub = (sub) => {
  if (!sub) return
  ensurePolyphonicState(sub.sentence_id)
  buildPolyphonicMarkers(sub)
}

const selectSubSentence = (sub) => {
  if (!sub) return
  editingSubSentenceId.value = sub.sentence_id
  editingForm.sentenceId = sub.sentence_id
  editingForm.content = sub.content
  editingForm.voice = sub.voice || 'default'
  editingForm.volume = clampVolume(sub.volume)
  editingForm.speed = clampSpeed(sub.speed)
  editingForm.pitch = sub.pitch || 50
  refreshPolyphonicForSub(sub)
}

const setEditorRef = (id, instance) => {
  if (instance) {
    editorRefs[id] = instance
  } else {
    delete editorRefs[id]
  }
}

const handleEditorSelectionChange = (sub, payload = {}) => {
  pauseEligibilityMap[sub.sentence_id] = !!payload?.hasTextBefore
}

const handleEditorContentChange = (sub) => {
  if (!sub) return
  refreshPolyphonicForSub(sub)
}
watch(
  () => editingSubSentenceId.value,
  () => {
    const sub = currentSubSentence.value
    if (sub) {
      editingForm.sentenceId = sub.sentence_id
      editingForm.content = sub.content
      editingForm.voice = sub.voice || 'default'
      editingForm.volume = clampVolume(sub.volume)
      editingForm.speed = clampSpeed(sub.speed)
      editingForm.pitch = sub.pitch || 50
    }
  }
)

watch(
  () => currentSubSentence.value && currentSubSentence.value.content,
  (val) => {
    if (typeof val === 'string') {
      editingForm.content = val
    }
  }
)

watch(
  () => [
    editingForm.voice,
    editingForm.volume,
    editingForm.speed,
    editingForm.pitch
  ],
  () => {
    const sub = currentSubSentence.value
    if (sub) {
      sub.voice = editingForm.voice
      sub.volume = clampVolume(editingForm.volume)
      sub.speed = clampSpeed(editingForm.speed)
      sub.pitch = editingForm.pitch
    }
  }
)

watch(
  () => editingForm.volume,
  (val) => {
    const clamped = clampVolume(val)
    if (clamped !== val) {
      editingForm.volume = clamped
    }
  }
)

watch(
  () => editingForm.speed,
  (val) => {
    const clamped = clampSpeed(val)
    if (clamped !== val) {
      editingForm.speed = clamped
    }
  }
)

const taskId = ref('')
const voiceCategories = [
  { label: '新闻', value: 'news' },
  { label: '小说', value: 'novel' }
]
const activeVoiceCategory = ref('news')
const voiceOptions = [
  { label: '唐瑶', value: 'default', desc: '真实3.0', tag: '新闻', avatar: '唐' },
  { label: '果子', value: 'female1', desc: '亲子3.0VC', tag: '儿童', avatar: '果' },
  { label: '杨笙', value: 'male1', desc: '形象3.0', tag: '形象', avatar: '杨' }
]
const customOptions = [
  {
    label: '音量',
    icon: 'Headset',
    controlKey: 'volume',
    min: 0,
    max: 100,
    step: 1
  },
  {
    label: '语速',
    icon: 'MagicStick',
    controlKey: 'speed',
    min: -10,
    max: 10,
    step: 1
  },
  { label: '断句标准', icon: 'Tickets' },
  { label: '停顿', icon: 'Timer', actionKey: 'pause' },
  { label: '多音字', icon: 'ChatLineSquare', actionKey: 'polyphonic' },
  { label: '插入静音', icon: 'Bell', actionKey: 'silence' },
  { label: '阅读规范', icon: 'CollectionTag' }
]

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
    sentences.value = (data.sentences || []).map((item) =>
      normalizeSentenceParams(item)
    )
    sentences.value.forEach((item) => ensurePolyphonicState(item.sentence_id))
    if (editingSentenceId.value) {
      const current = sentences.value.find(
        (item) => item.sentence_id === editingSentenceId.value
      )
      if (current) {
        await loadSentenceDetail(current.sentence_id, current.content)
        const sub =
          findSentenceById(pendingSelectSubSentenceId.value) ||
          findSentenceById(editingSubSentenceId.value) ||
          current
        selectSubSentence(sub)
        pendingSelectSubSentenceId.value = null
      } else {
        closeEditing()
      }
    } else if (pendingSelectSubSentenceId.value) {
      const sub = findSentenceById(pendingSelectSubSentenceId.value)
      if (sub) {
        selectSubSentence(sub)
      }
      pendingSelectSubSentenceId.value = null
    }
  } catch (error) {
    console.error('加载句子列表失败:', error)
  } finally {
    loading.value = false
  }
}

const toggleEdit = async (sentence) => {
  if (editingSentenceId.value === sentence.sentence_id) {
    closeEditing()
    return
  }

  editingSentenceId.value = sentence.sentence_id
  editingSubSentenceId.value = sentence.sentence_id
  await loadSentenceDetail(sentence.sentence_id, sentence.content)
  selectSubSentence(sentence)
}

const closeEditing = () => {
  editingSentenceId.value = null
  editingSubSentenceId.value = null
  hidePolyphonicTooltip()
}

const loadSentenceDetail = async (sentenceId, fallbackContent = '') => {
  try {
    const detail = await getSentence(sentenceId)
    editingForm.sentenceId = sentenceId
    editingForm.content = detail.content || fallbackContent
  editingForm.voice = detail.voice || 'default'
  editingForm.volume = clampVolume(detail.volume)
  editingForm.speed = clampSpeed(detail.speed)
  editingForm.pitch = detail.pitch || 50
  } catch (error) {
    console.error('获取句子详情失败:', error)
    editingForm.sentenceId = sentenceId
    editingForm.content = fallbackContent
  }
}

const selectVoice = (voice) => {
  editingForm.voice = voice
}

const handleUpdateCategory = (value) => {
  activeVoiceCategory.value = value
}

const handleSaveCurrent = async () => {
  if (!editingForm.sentenceId) {
    return
  }
  const target = currentSubSentence.value
  try {
    await updateSentence(editingForm.sentenceId, {
      content: target?.content || editingForm.content,
      voice: editingForm.voice,
      volume: editingForm.volume,
      speed: editingForm.speed,
      pitch: editingForm.pitch
    })
    ElMessage.success('保存成功')
    await loadSentences()
    if (editingForm.sentenceId) {
      await loadSentenceDetail(editingForm.sentenceId)
    }
  } catch (error) {
    console.error('保存失败:', error)
  }
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

    const newSentence = await insertSentenceAfter(sentenceId, { content: value })
    if (newSentence && newSentence.sentence_id) {
      pendingSelectSubSentenceId.value = newSentence.sentence_id
    }
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

const cancelTooltipHide = () => {
  if (polyphonicTooltipTimer) {
    clearTimeout(polyphonicTooltipTimer)
    polyphonicTooltipTimer = null
  }
}

const hidePolyphonicTooltip = () => {
  cancelTooltipHide()
  polyphonicTooltip.visible = false
  polyphonicTooltip.markerId = ''
  polyphonicTooltip.sentenceId = null
  polyphonicTooltip.char = ''
  polyphonicTooltip.options = []
  polyphonicTooltip.selected = ''
}

const scheduleTooltipHide = () => {
  cancelTooltipHide()
  polyphonicTooltipTimer = setTimeout(() => {
    hidePolyphonicTooltip()
  }, 120)
}

const showPolyphonicTooltip = (marker, rect) => {
  cancelTooltipHide()
  const centerX = rect.left + (rect.right - rect.left) / 2
  const bottomY = rect.bottom + 8
  polyphonicTooltip.visible = true
  polyphonicTooltip.markerId = marker.id
  polyphonicTooltip.sentenceId = marker.sentenceId
  polyphonicTooltip.char = marker.char
  polyphonicTooltip.options = marker.options || []
  polyphonicTooltip.selected = marker.selected || ''
  polyphonicTooltip.position = {
    x: centerX,
    y: bottomY
  }
}

const handlePolyphonicHover = (sub, payload) => {
  if (!sub) return
  if (!payload) {
    scheduleTooltipHide()
    return
  }
  if (!payload.rect) {
    scheduleTooltipHide()
    return
  }
  const marker = getPolyphonicMarkers(sub).find(
    (item) => item.id === payload.markerId
  )
  if (!marker) {
    scheduleTooltipHide()
    return
  }
  const modeActive = isPolyphonicModeActive(sub)
  if (!modeActive && !marker.selected) {
    hidePolyphonicTooltip()
    return
  }
  showPolyphonicTooltip(marker, payload.rect)
}

const handlePolyphonicOptionSelect = (option) => {
  const { sentenceId, markerId } = polyphonicTooltip
  if (!sentenceId || !markerId) return
  const state = ensurePolyphonicState(sentenceId)
  if (!state) return
  if (option) {
    state.selections[markerId] = option
  } else {
    delete state.selections[markerId]
  }
  const target = findSentenceById(sentenceId)
  refreshPolyphonicForSub(target)
  hidePolyphonicTooltip()
}

const togglePolyphonicMode = () => {
  const sub = currentSubSentence.value
  if (!sub) return
  const current = !!polyphonicModeMap[sub.sentence_id]
  polyphonicModeMap[sub.sentence_id] = !current
  refreshPolyphonicForSub(sub)
  if (!polyphonicModeMap[sub.sentence_id]) {
    hidePolyphonicTooltip()
  }
}

const insertPauseMarker = () => {
  const currentId = editingSubSentenceId.value
  if (!currentId) return
  const editor = editorRefs[currentId]
  if (editor?.insertPause) {
    editor.insertPause()
  }
}

const insertSilenceMarker = (duration) => {
  const currentId = editingSubSentenceId.value
  if (!currentId) return
  const editor = editorRefs[currentId]
  if (editor?.insertSilence) {
    editor.insertSilence(duration)
  }
}

const promptSilenceDuration = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入静音时长（秒）', '插入静音', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '例如 3.1',
      inputValue: '1.0'
    })
    const parsed = Number(value)
    if (Number.isNaN(parsed) || parsed <= 0) {
      ElMessage.error('请输入大于 0 的秒数')
      return
    }
    const normalized = Math.min(60, Math.max(0.1, parsed))
    const formatted =
      normalized % 1 === 0 ? normalized.toString() : normalized.toFixed(1)
    insertSilenceMarker(formatted)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('插入静音失败:', error)
    }
  }
}

const handleCustomAction = (actionKey) => {
  if (actionKey === 'pause') {
    if (!isPauseEnabled.value) return
    insertPauseMarker()
  } else if (actionKey === 'polyphonic') {
    togglePolyphonicMode()
  } else if (actionKey === 'silence') {
    promptSilenceDuration()
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
  padding: 20px 0 60px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 20px;
}

.page-header h2 {
  font-size: 22px;
  font-weight: 600;
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
  padding: 0;
}

.sentence-layout {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 20px;
}

.sentence-main {
  flex: 1;
  min-width: 0;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  padding: 16px;
}

.sentence-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.sentence-number {
  font-weight: 600;
  color: #1d1f23;
  margin-right: 12px;
}

.sentence-duration {
  font-size: 12px;
  color: #999;
}

.sentence-status {
  font-size: 12px;
  color: #f56c6c;
}

.sentence-status.success {
  color: #67c23a;
}

.sentence-content {
  flex: 1;
  padding: 14px 16px;
  background: #f7f9fc;
  border-radius: 6px;
  line-height: 1.6;
  color: #333;
}

.sentence-text-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.sentence-text {
  flex: 1;
  min-width: 0;
}

.sentence-links {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 8px;
  white-space: nowrap;
  font-size: 13px;
  color: #5a7efc;
}

.sentence-link {
  cursor: pointer;
  color: #5a7efc;
}

.sentence-link.danger {
  color: #f56c6c;
}

.sentence-divider {
  color: #c0c4cc;
}

.sentence-status-tag {
  color: #f59a23;
  font-weight: 600;
}

.sentence-quick-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #666;
  font-size: 13px;
}
.editing-panel {
  margin-top: 16px;
  padding: 18px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fdfdfd;
}

.textarea-wrapper {
  margin-top: 16px;
}

.textarea-input {
  position: relative;
}

.textarea-input :deep(.el-textarea__inner) {
  padding-right: 240px;
}

.textarea-floating-links {
  position: absolute;
  top: 12px;
  right: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #5a7efc;
  background: rgba(255, 255, 255, 0.9);
  padding: 4px 6px;
  border-radius: 6px;
}

.textarea-link {
  cursor: pointer;
  color: #5a7efc;
}

.textarea-link.danger {
  color: #f56c6c;
}

.textarea-status-tag.pending {
  color: #f59a23;
  font-weight: 600;
}

.textarea-divider {
  color: #c0c4cc;
}

.textarea-toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #666;
}

.toolbar-links {
  display: flex;
  align-items: center;
  gap: 8px;
}

.textarea-count {
  color: #999;
  font-size: 12px;
}

.textarea-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 10px;
  flex-wrap: wrap;
  gap: 10px;
}

.link-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.textarea-buttons {
  display: flex;
  gap: 10px;
}

.polyphonic-tooltip {
  position: fixed;
  transform: translate(-50%, 0);
  background: #fff;
  border: 1px solid #e4e7ed;
  box-shadow: 0 16px 30px rgba(0, 0, 0, 0.12);
  border-radius: 10px;
  padding: 12px 16px;
  z-index: 1000;
  min-width: 200px;
}

.polyphonic-tooltip .tooltip-char {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #1d1f23;
}

.polyphonic-tooltip .tooltip-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.polyphonic-tooltip .tooltip-option {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #1d1f23;
}

.polyphonic-tooltip .tooltip-option:hover {
  border-color: #2f7bff;
  color: #2f7bff;
}

.polyphonic-tooltip .tooltip-option.active {
  background: #ffe6aa;
  border-color: #f4c762;
  color: #5f4300;
}

.polyphonic-tooltip .tooltip-reset {
  margin-top: 10px;
  font-size: 13px;
  color: #5a7efc;
  cursor: pointer;
  text-align: right;
}

.merge-footer {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

