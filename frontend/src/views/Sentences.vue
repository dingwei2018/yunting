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
                      {{ getCombinedSentenceContentReactive(sentence) }}
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
                @clear-text="() => handleClearText(sentence)"
              />

              <SubSentenceEditorList
                :subs="getSubSentences(sentence.sentence_id).filter(sub => sub.parent_id !== 0)"
                :editing-sub-sentence-id="editingSubSentenceId"
                :get-polyphonic-markers="getPolyphonicMarkers"
                :is-polyphonic-mode-active="isPolyphonicModeActive"
                :set-editor-ref="setEditorRef"
                @select-sub="selectSubSentence"
                @editor-selection-change="({ sub, payload }) => handleEditorSelectionChange(sub, payload)"
                @editor-content-change="(sub) => handleEditorContentChange(sub)"
                @polyphonic-hover="({ sub, payload }) => handlePolyphonicHover(sub, payload)"
                @editor-focus="(sub) => handleEditorFocus(sub)"
                @play="handlePlay"
                @synthesize="handleResynthesize"
                @insert-after="handleInsertAfter"
                @delete="handleDelete"
              />
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
      <PolyphonicTooltip
        v-if="polyphonicTooltip.visible"
        :visible="polyphonicTooltip.visible"
        :position="polyphonicTooltip.position"
        :char="polyphonicTooltip.char"
        :options="polyphonicTooltip.options"
        :selected="polyphonicTooltip.selected"
        @mouseenter="handleTooltipMouseEnter"
        @mouseleave="handleTooltipMouseLeave"
        @select="handlePolyphonicOptionSelect"
      />
    </transition>

    <div class="merge-footer">
      <el-button type="primary" size="large" @click="handleMergeAudio" :loading="merging">
        合并音频
      </el-button>
    </div>

    <SplitStandardDialog
      :visible="splitStandardDialogVisible"
      :type="splitStandardType"
      :char-count="splitStandardCharCount"
      @update:visible="(val) => (splitStandardDialogVisible = val)"
      @update:type="(val) => (splitStandardType = val)"
      @update:char-count="(val) => (splitStandardCharCount = val)"
      @confirm="handleSplitStandardConfirm"
      @close="handleSplitStandardDialogClose"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed, watch, h, defineComponent, defineOptions } from 'vue'

defineOptions({
  name: 'Sentences'
})
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElRadioGroup, ElRadio, ElDialog, ElInput } from 'element-plus'
import { polyphonic } from 'pinyin-pro'
import SentenceTuningPanel from '@/components/SentenceTuningPanel.vue'
import SentenceActionLinks from '@/components/SentenceActionLinks.vue'
import SubSentenceEditorList from '@/components/SubSentenceEditorList.vue'
import SplitStandardDialog from '@/components/SplitStandardDialog.vue'
import PolyphonicTooltip from '@/components/PolyphonicTooltip.vue'
import { useSentencesRepository } from '@/composables/useSentencesRepository'

const route = useRoute()
const router = useRouter()
const sentencesRepository = useSentencesRepository()
const {
  loading,
  merging,
  sentences,
  taskId,
  loadSentences,
  handleMergeAudio: mergeAudioTask,
  insertAfter: insertSentenceAfterLocal,
  deleteSentence: deleteSentenceApi,
  synthesizeSentence: synthesizeSentenceApi,
  getSentence: getSentenceApi,
  updateSentence: updateSentenceApi
} = sentencesRepository

// 用于存储断句标准选择的值
const splitStandardType = ref('punctuation')
const splitStandardCharCount = ref(50) // 默认字符数
const splitStandardDialogVisible = ref(false)
const splitStandardContext = ref(null) // 存储当前操作的上下文
const audioRefs = ref({})
const editingSentenceId = ref(null)
const editingSubSentenceId = ref(null)
const pendingSelectSubSentenceId = ref(null)

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
const isTooltipHovering = ref(false)

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

// 获取拆句内容：将所有子句子拼接起来（响应式）
const getCombinedSentenceContent = (sentence) => {
  // 使用 computed 来确保响应式更新
  // 但由于这是在模板中调用的函数，我们需要确保它能够追踪到 sentences.value 的变化
  const children = sentences.value
    .filter((item) => item.parent_id === sentence.sentence_id)
    .sort((a, b) => (a.display_order || 0) - (b.display_order || 0))
  
  // 如果有子句子，将所有子句子的内容拼接起来
  if (children.length > 0) {
    // 访问每个子句子的 content，确保 Vue 能够追踪到变化
    return children.map(sub => {
      // 确保访问 content 属性，触发响应式追踪
      const content = sub.content || ''
      return content
    }).join('')
  }
  
  // 如果没有子句子，返回父句子的原始内容
  return sentence.content || ''
}

// 为每个根句子创建响应式的拼接内容计算属性
const sentenceCombinedContentMap = computed(() => {
  const map = {}
  rootSentences.value.forEach(sentence => {
    const children = sentences.value
      .filter((item) => item.parent_id === sentence.sentence_id)
      .sort((a, b) => (a.display_order || 0) - (b.display_order || 0))
    
    if (children.length > 0) {
      map[sentence.sentence_id] = children.map(sub => sub.content || '').join('')
    } else {
      map[sentence.sentence_id] = sentence.content || ''
    }
  })
  return map
})

// 获取拆句内容（使用计算属性）
const getCombinedSentenceContentReactive = (sentence) => {
  return sentenceCombinedContentMap.value[sentence.sentence_id] || sentence.content || ''
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

const isClearingText = ref(false)

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

const handleEditorFocus = (sub) => {
  if (!sub) return
  if (editingSubSentenceId.value !== sub.sentence_id) {
    selectSubSentence(sub)
  }
}

const handleClearText = async (rootSentence) => {
  if (!rootSentence) return
  isClearingText.value = true

  // 先清空原始拆句内容，防止后续同步又把旧文本写回
  rootSentence.content = ''
  const children = sentences.value
    .filter((item) => item.parent_id === rootSentence.sentence_id)
    .sort((a, b) => (a.display_order || 0) - (b.display_order || 0))

  const [keeper, ...toDelete] = children

  // 删除多余的子句子
  for (const child of toDelete) {
    try {
        await deleteSentenceApi(child.sentence_id)
    } catch (error) {
      console.error('删除子句子失败:', error)
    }
    const index = sentences.value.findIndex(
      (item) => item.sentence_id === child.sentence_id
    )
    if (index !== -1) {
      sentences.value.splice(index, 1)
    }
  }

  let target = keeper

  if (target) {
    target.content = ''
    editingForm.content = ''
    refreshPolyphonicForSub(target)
  } else {
    // 没有子句子时创建一个新的空输入框
    const newSentence = insertSentenceAfterLocal(rootSentence.sentence_id, {
      content: '',
      parent_id: rootSentence.sentence_id
    })
    if (newSentence && newSentence.sentence_id) {
      ensurePolyphonicState(newSentence.sentence_id)
      target = newSentence
    }
  }

  if (target) {
    editingForm.content = ''
    editingSubSentenceId.value = target.sentence_id
  }
  isClearingText.value = false
  if (target) {
    selectSubSentence(target)
  }
}
watch(
  () => editingSubSentenceId.value,
  () => {
    if (isClearingText.value) return
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
    if (isClearingText.value) return
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
  { label: '断句标准', icon: 'Tickets', actionKey: 'split-standard' },
  { label: '停顿', icon: 'Timer', actionKey: 'pause' },
  { label: '多音字', icon: 'ChatLineSquare', actionKey: 'polyphonic' },
  { label: '插入静音', icon: 'Bell', actionKey: 'silence' },
  { label: '阅读规范', icon: 'CollectionTag', actionKey: 'reading-rules' }
]

onMounted(() => {
  taskId.value = route.query.task_id
  if (taskId.value) {
    refreshSentences()
  } else {
    ElMessage.error('缺少任务ID参数')
  }
})

const refreshSentences = async () => {
  try {
    await loadSentences(taskId.value)
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
  }
}

const toggleEdit = async (sentence) => {
  if (editingSentenceId.value === sentence.sentence_id) {
    closeEditing()
    return
  }

  editingSentenceId.value = sentence.sentence_id
  
  // 检查是否有子句子（编辑区域只显示子句子）
  const children = sentences.value.filter(
    (item) => item.parent_id === sentence.sentence_id
  )
  
  // 如果没有子句子，默认进入"大符号模式"：创建一个子句子，内容是父句子的内容
  if (children.length === 0) {
    const newSentence = insertSentenceAfterLocal(sentence.sentence_id, {
      content: sentence.content,
      parent_id: sentence.sentence_id
    })

    if (newSentence && newSentence.sentence_id) {
      ensurePolyphonicState(newSentence.sentence_id)
      editingSubSentenceId.value = newSentence.sentence_id
      await loadSentenceDetail(newSentence.sentence_id, newSentence.content)
      selectSubSentence(newSentence)
      return
    }
  }
  
  // 如果有子句子，选中第一个子句子
  if (children.length > 0) {
    const firstChild = children.sort((a, b) => (a.display_order || 0) - (b.display_order || 0))[0]
    editingSubSentenceId.value = firstChild.sentence_id
    await loadSentenceDetail(firstChild.sentence_id, firstChild.content)
    selectSubSentence(firstChild)
  } else {
    // 如果没有子句子且创建失败，使用父句子（虽然不会显示在编辑区域）
    editingSubSentenceId.value = sentence.sentence_id
    await loadSentenceDetail(sentence.sentence_id, sentence.content)
    selectSubSentence(sentence)
  }
}

const closeEditing = () => {
  editingSentenceId.value = null
  editingSubSentenceId.value = null
  hidePolyphonicTooltip()
}

const loadSentenceDetail = async (sentenceId, fallbackContent = '') => {
  try {
    const detail = await getSentenceApi(sentenceId)
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
    await updateSentenceApi(editingForm.sentenceId, {
      content: target?.content || editingForm.content,
      voice: editingForm.voice,
      volume: editingForm.volume,
      speed: editingForm.speed,
      pitch: editingForm.pitch
    })
    ElMessage.success('保存成功')
    await refreshSentences()
    if (editingForm.sentenceId) {
      await loadSentenceDetail(editingForm.sentenceId)
    }
  } catch (error) {
    console.error('保存失败:', error)
  }
}

const removeLocalSentence = (sentenceId) => {
  const index = sentences.value.findIndex((item) => item.sentence_id === sentenceId)
  if (index === -1) return null
  const [removed] = sentences.value.splice(index, 1)

  if (!removed.parent_id || removed.parent_id === 0) {
    for (let i = sentences.value.length - 1; i >= 0; i -= 1) {
      if (sentences.value[i].parent_id === removed.sentence_id) {
        sentences.value.splice(i, 1)
      }
    }
  }

  delete polyphonicStateMap[sentenceId]
  delete polyphonicModeMap[sentenceId]
  delete pauseEligibilityMap[sentenceId]
  delete editorRefs[sentenceId]
  return removed
}

const handleDelete = async (sentenceId) => {
  try {
    await ElMessageBox.confirm('确定要删除这个句子吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteSentenceApi(sentenceId)
    ElMessage.success('删除成功')

    const removed = removeLocalSentence(sentenceId)

    if (removed && editingSubSentenceId.value === sentenceId) {
      const siblings = sentences.value
        .filter((item) => item.parent_id === removed.parent_id && item.parent_id !== 0)
        .sort((a, b) => (a.display_order || 0) - (b.display_order || 0))

      if (siblings.length > 0) {
        const next = siblings[0]
        editingSubSentenceId.value = next.sentence_id
        selectSubSentence(next)
      } else {
        editingSubSentenceId.value = null
        editingForm.sentenceId = ''
        editingForm.content = ''
      }
    }
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

    const clickedSentence = findSentenceById(sentenceId)
    const parentId =
      clickedSentence && clickedSentence.parent_id !== 0
        ? clickedSentence.parent_id
        : sentenceId

    const newSentence = insertSentenceAfterLocal(sentenceId, {
      content: value,
      parent_id: parentId
    })

    if (newSentence && newSentence.sentence_id) {
      ensurePolyphonicState(newSentence.sentence_id)

      if (clickedSentence) {
        const rootId =
          clickedSentence.parent_id === 0
            ? clickedSentence.sentence_id
            : clickedSentence.parent_id

        if (editingSentenceId.value === rootId && newSentence.parent_id === rootId) {
          selectSubSentence(newSentence)
        }
      }

      ElMessage.success('插入成功')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('插入失败:', error)
      ElMessage.error('插入失败')
    }
  }
}

const handleResynthesize = async (sentenceId) => {
  try {
    await synthesizeSentenceApi(sentenceId)
    ElMessage.success('重新合成中，请稍候...')
    // 等待一段时间后刷新列表
    setTimeout(() => {
      refreshSentences()
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
  try {
    await mergeAudioTask()
    ElMessage.success('合并音频成功')
  } catch (error) {
    console.error('合并音频失败:', error)
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
  isTooltipHovering.value = false
}

const scheduleTooltipHide = () => {
  if (isTooltipHovering.value) return
  cancelTooltipHide()
  polyphonicTooltipTimer = setTimeout(() => {
    if (!isTooltipHovering.value) {
      hidePolyphonicTooltip()
    }
  }, 250)
}

const showPolyphonicTooltip = (marker, rect) => {
  cancelTooltipHide()
  isTooltipHovering.value = false
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

const handleTooltipMouseEnter = () => {
  isTooltipHovering.value = true
  cancelTooltipHide()
}

const handleTooltipMouseLeave = () => {
  isTooltipHovering.value = false
  scheduleTooltipHide()
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

const handleSplitStandard = () => {
  if (!editingSentenceId.value) {
    ElMessage.warning('请先选择一个句子进行编辑')
    return
  }

  const rootSentence = findSentenceById(editingSentenceId.value)
  if (!rootSentence) {
    ElMessage.error('未找到当前句子')
    return
  }

  // 存储上下文
  splitStandardContext.value = {
    rootSentence,
    originalText: rootSentence.content || ''
  }

  // 重置选择值
  splitStandardType.value = 'punctuation'

  // 显示对话框
  splitStandardDialogVisible.value = true
}

const handleSplitStandardDialogClose = () => {
  splitStandardDialogVisible.value = false
  splitStandardContext.value = null
  // 重置选择值
  splitStandardType.value = 'punctuation'
  splitStandardCharCount.value = 50
}

const handleSplitStandardConfirm = () => {
  if (!splitStandardContext.value) {
    splitStandardDialogVisible.value = false
    return
  }

  const { rootSentence, originalText } = splitStandardContext.value
  const selectedType = splitStandardType.value

  if (selectedType === 'punctuation') {
    // 大符号：清空所有输入文本，将原始拆句作为输入文本
    handleSplitByPunctuation(rootSentence, originalText)
    splitStandardDialogVisible.value = false
    splitStandardContext.value = null
  } else if (selectedType === 'charCount') {
    // 字符数：使用对话框中输入的字符数
    const charCount = parseInt(splitStandardCharCount.value, 10)
    if (Number.isNaN(charCount) || charCount <= 0) {
      ElMessage.error('请输入大于 0 的字符数')
      return
    }
    handleSplitByCharCount(rootSentence, originalText, charCount)
    splitStandardDialogVisible.value = false
    splitStandardContext.value = null
  } else {
    ElMessage.warning('请选择断句方式')
  }
}


const handleSplitByPunctuation = async (rootSentence, originalText) => {
  // 第一步：一次性清空所有输入文本（所有子句子）
  // 找到所有需要删除的子句子：
  // 1. 直接子句子：parent_id === rootSentence.sentence_id
  // 2. 嵌套子句子：parent_id 指向其他子句子的句子
  // 需要递归找到所有相关的子句子
  const getAllChildrenIds = (parentId) => {
    const directChildren = sentences.value.filter(
      (item) => item.parent_id === parentId
    )
    const allIds = directChildren.map(sub => sub.sentence_id)
    // 递归获取子句子的子句子
    directChildren.forEach(sub => {
      const nestedIds = getAllChildrenIds(sub.sentence_id)
      allIds.push(...nestedIds)
    })
    return allIds
  }
  
  // 获取所有需要删除的子句子ID（包括嵌套的）
  const deleteIds = getAllChildrenIds(rootSentence.sentence_id)
  
  // 直接从 sentences.value 中移除所有子句子（保留父句子）
  sentences.value = sentences.value.filter(
    (item) => item.sentence_id === rootSentence.sentence_id || !deleteIds.includes(item.sentence_id)
  )
  
  // 异步删除（不等待，避免阻塞）
  Promise.all(deleteIds.map(id => deleteSentenceApi(id).catch(err => {
    console.error('删除子句子失败:', id, err)
  })))

  // 第二步：创建一个新的子句子，内容是父句子的内容，作为"输入文本1"
  // 注意：不修改父句子的内容，父句子保持原样，但编辑区域只显示子句子
  const newSentence = insertSentenceAfterLocal(rootSentence.sentence_id, {
    content: originalText,
    parent_id: rootSentence.sentence_id
  })

  if (newSentence && newSentence.sentence_id) {
    ensurePolyphonicState(newSentence.sentence_id)
    selectSubSentence(newSentence)
  } else {
    ElMessage.error('创建子句子失败')
    return
  }

  ElMessage.success('已按大符号重置：清空所有输入文本，并将父句复制为输入文本1')
}

const handleSplitByCharCount = async (rootSentence, originalText, charCount) => {
  if (!originalText) {
    ElMessage.warning('原始拆句文本为空')
    return
  }

  // 按字符数拆分文本
  const chunks = []
  for (let i = 0; i < originalText.length; i += charCount) {
    chunks.push(originalText.slice(i, i + charCount))
  }

  if (chunks.length === 0) {
    ElMessage.warning('拆分结果为空')
    return
  }

  // 获取所有子句子（包括父句子）
  const subSentences = getSubSentences(rootSentence.sentence_id)
  const existingCount = subSentences.length
  const chunksCount = chunks.length

  // 第一步：先清空所有现有的子句子（和"大符号"逻辑一样，清空所有输入文本）
  // 递归查找所有子句子并删除
  const getAllChildrenIds = (parentId) => {
    const directChildren = sentences.value.filter(
      (item) => item.parent_id === parentId
    )
    const allIds = directChildren.map(sub => sub.sentence_id)
    // 递归获取子句子的子句子
    directChildren.forEach(sub => {
      const nestedIds = getAllChildrenIds(sub.sentence_id)
      allIds.push(...nestedIds)
    })
    return allIds
  }
  
  // 获取所有需要删除的子句子ID（包括嵌套的）
  const deleteIds = getAllChildrenIds(rootSentence.sentence_id)
  
  // 直接从 sentences.value 中移除所有子句子（保留父句子，父句子内容不变）
  sentences.value = sentences.value.filter(
    (item) => item.sentence_id === rootSentence.sentence_id || !deleteIds.includes(item.sentence_id)
  )
  
  // 异步删除（不等待，避免阻塞）
  Promise.all(deleteIds.map(id => deleteSentenceApi(id).catch(err => {
    console.error('删除子句子失败:', id, err)
  })))

  // 第二步：创建新的子句子（从第一段开始，因为父句子内容不变，不显示在编辑区域）
  // 注意：父句子内容保持不变，编辑区域只显示子句子
  let lastSub = rootSentence
  for (let i = 0; i < chunksCount; i++) {
    const newSentence = insertSentenceAfterLocal(lastSub.sentence_id, {
      content: chunks[i],
      parent_id: rootSentence.sentence_id
    })

    if (newSentence && newSentence.sentence_id) {
      ensurePolyphonicState(newSentence.sentence_id)
      lastSub = newSentence
    }
  }

  // 如果当前选中的是父句子，更新编辑表单为拆分后的第一段文本
  if (editingSubSentenceId.value === rootSentence.sentence_id) {
    editingForm.content = chunks[0]
  }

  ElMessage.success(`已按 ${charCount} 个字符拆分为 ${chunksCount} 句`)
}

const handleCustomAction = (actionKey) => {
  if (actionKey === 'pause') {
    if (!isPauseEnabled.value) return
    insertPauseMarker()
  } else if (actionKey === 'polyphonic') {
    togglePolyphonicMode()
  } else if (actionKey === 'silence') {
    promptSilenceDuration()
  } else if (actionKey === 'split-standard') {
    handleSplitStandard()
  } else if (actionKey === 'reading-rules') {
    router.push('/reading-rules')
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
  width: 80px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
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

