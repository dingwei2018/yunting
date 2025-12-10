<template>
  <div class="sentences-page">
    <div class="page-header">
      <h2>音频精修</h2>
      <div class="header-actions">
        <el-button 
          @click="handleReadingRules"
        >
          阅读规范
        </el-button>
        <el-button 
          type="primary" 
          @click="handleSynthesizeAll" 
          :loading="synthesizingAll"
          :disabled="!canSynthesizeAll"
        >
          合成全部音频
        </el-button>
        <el-button 
          type="primary" 
          @click="handleMergeAudio" 
          :loading="merging"
          :disabled="!canMergeAudio"
        >
          合并音频
        </el-button>
      </div>
    </div>

    <el-card v-loading="loading" class="sentences-card">
      <div v-if="sentences.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无句子数据" />
      </div>

      <div v-else class="sentence-list" @scroll="handleScroll">
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
                        :synthesis-status="getOriginalSentenceSynthesisStatus(sentence.sentence_id)"
                        :show-insert-after="false"
                        @play="handlePlayOriginalSentence(sentence)"
                        @synthesize="handleResynthesizeOriginalSentence(sentence.sentence_id)"
                        @delete="() => {}"
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

          <SentenceEditor
            v-if="editingSentenceId === sentence.sentence_id && getOriginalSentenceData(sentence.sentence_id)"
            ref="sentenceEditorRef"
            :original-sentence="getOriginalSentenceData(sentence.sentence_id)"
            :task-id="taskId"
            :voice-options="voiceOptions"
            :expanded="editingSentenceId === sentence.sentence_id"
            @update:expanded="handleEditorExpandedChange"
            @saved="handleEditorSaved"
            @refresh="handleEditorRefresh"
          />

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
    
    <!-- 分页状态提示 - 置于页面最下端 -->
    <div v-if="loadingMore" class="loading-more">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
    <div v-else-if="!hasMore && sentences.length > 0" class="no-more">
      没有更多数据了
    </div>


    <!-- 合成进度对话框 -->
    <el-dialog
      v-model="taskSynthesisProgress.visible"
      title="合成进度"
      width="500px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      class="synthesis-progress-dialog"
    >
      <div class="progress-content">
        <div class="progress-header">
          <h3>正在合成全部音频</h3>
        </div>
        <div class="progress-info">
          <el-progress
            :percentage="taskSynthesisProgress.progress"
            :status="taskSynthesisProgress.status === 'completed' ? 'success' : taskSynthesisProgress.status === 'failed' ? 'exception' : undefined"
            :stroke-width="8"
          />
          <div class="progress-text">
            <span>已完成：{{ taskSynthesisProgress.completed }} / {{ taskSynthesisProgress.total }}</span>
            <span v-if="taskSynthesisProgress.pending > 0">待处理：{{ taskSynthesisProgress.pending }}</span>
          </div>
          <div class="progress-status">
            <span v-if="taskSynthesisProgress.status === 'processing'">合成中，请稍候...</span>
            <span v-else-if="taskSynthesisProgress.status === 'completed'" class="success">合成完成！</span>
            <span v-else-if="taskSynthesisProgress.status === 'failed'" class="error">合成失败</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, watch, defineOptions, inject } from 'vue'

defineOptions({
  name: 'Sentences'
})
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElDialog, ElProgress } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import SentenceActionLinks from '@/components/SentenceActionLinks.vue'
import SentenceEditor from '@/components/SentenceEditor.vue'
import { useSentencesRepository } from '@/composables/useSentencesRepository'
import { synthesizeOriginalSentence, getOriginalSentenceStatus, synthesizeTask, getTaskStatus } from '@/api/synthesis'
import { getTaskDetail } from '@/api/task'

const route = useRoute()
const router = useRouter()
const sentencesRepository = useSentencesRepository()

// 注入全局音频播放器
const audioPlayer = inject('audioPlayer', null)
const {
  loading,
  merging,
  sentences,
  taskId,
  hasMore,
  loadingMore,
  loadSentences,
  loadMoreSentences,
  handleMergeAudio: mergeAudioTask
} = sentencesRepository

const audioRefs = ref({})
const editingSentenceId = ref(null)
const sentenceEditorRef = ref(null)

// 拆句合成状态管理
const originalSentenceStatus = ref({}) // { [originalSentenceId]: { status, audioUrlList, timer } }
// 断句合成状态管理
const breakingSentenceStatus = ref({}) // { [breakingSentenceId]: { status, audioUrl, timer } }
// 当前正在播放的音频信息
const currentPlayingAudio = ref(null) // { sentenceId, audioList, currentIndex, audioElements }
// 合成全部音频的加载状态
const synthesizingAll = ref(false)
// 任务状态
const taskStatus = ref(null) // 0-拆句完成，1-语音合成中，2-语音合成成功，3-语音合成失败，4-语音合并中，5-语音合并成功，6-语音合并失败
const taskMergeId = ref(null) // 合并ID，用于跳转
// 任务合成进度遮罩状态
const taskSynthesisProgress = ref({
  visible: false,
  status: '', // 'processing', 'completed', 'failed'
  progress: 0, // 0-100
  total: 0,
  completed: 0,
  pending: 0,
  timer: null
})
// 保存原始拆句列表数据，用于获取 synthesisStatus
const originalSentenceListData = ref(null) // 保存 getOriginalSentenceList 返回的原始数据
// 保存数据备份（仅用于 createBackup，restoreFromBackup 已不再使用）
const backupSentences = ref(null) // 保存 sentences 的深拷贝备份
const backupOriginalSentenceListData = ref(null) // 保存 originalSentenceListData 的深拷贝备份


const rootSentences = computed(() =>
  sentences.value.filter((item) => !item.parent_id || item.parent_id === 0)
)

// 从接口数据中获取拆句的原始数据（用于 SentenceEditor 组件）
const getOriginalSentenceData = (originalSentenceId) => {
  const listData = originalSentenceListData.value
  const sentenceList = listData?.list || listData?.data?.list

  if (!sentenceList || !Array.isArray(sentenceList)) {
    return null
  }

  const originalSentence = sentenceList.find(
    os => os.originalSentenceId == originalSentenceId || String(os.originalSentenceId) === String(originalSentenceId)
  )

  return originalSentence || null
}

// 处理 SentenceEditor 组件的展开/收起状态变化
const handleEditorExpandedChange = (expanded) => {
  if (!expanded) {
    editingSentenceId.value = null
  }
}

// 处理 SentenceEditor 组件的保存成功事件
const handleEditorSaved = async () => {
  // 刷新数据
  await refreshSentences(true)
}

// 处理 SentenceEditor 组件的刷新事件
const handleEditorRefresh = async () => {
  // 刷新数据
  await refreshSentences(true)
}

// 从content中提取纯文本，移除所有标签（停顿、静音、多音字等）
const extractPlainText = (content, sentenceId = null) => {
  if (!content || typeof content !== 'string') return ''
  // 移除停顿标签: <pause:1.0> 或 <pause>
  let plainText = content.replace(/<pause(?::[\d.]+)?>/g, '')
  // 移除静音标签: <silence:1.0>
  plainText = plainText.replace(/<silence:[\d.]+>/g, '')
  // 移除可能的其他HTML标签（如果有）
  plainText = plainText.replace(/<[^>]+>/g, '')
  return plainText
}

// 为每个根句子创建响应式的拼接内容计算属性
const sentenceCombinedContentMap = computed(() => {
  const map = {}
  rootSentences.value.forEach((sentence) => {
    const children = sentences.value
      .filter((item) => item.parent_id === sentence.sentence_id)
      .sort((a, b) => (a.display_order || 0) - (b.display_order || 0))
    
    if (children.length > 0) {
      // 提取纯文本，移除所有标签
      const parts = children.map(sub => {
        return extractPlainText(sub.content || '', sentence.sentence_id)
      })
      map[sentence.sentence_id] = parts.join('')
    } else {
      // 提取纯文本，移除所有标签
      map[sentence.sentence_id] = extractPlainText(sentence.content || '', sentence.sentence_id)
    }
  })
  return map
})

// 获取拆句内容（使用计算属性）
const getCombinedSentenceContentReactive = (sentence) => {
  // sentenceCombinedContentMap 已经处理过纯文本提取，直接返回即可
  const content = sentenceCombinedContentMap.value[sentence.sentence_id]
  if (content !== undefined) {
    return content
  }
  // 如果没有在 map 中，说明可能是新数据，需要提取纯文本
  return extractPlainText(sentence.content || '')
}


// 从全局注入获取音色列表
const globalVoiceList = inject('globalVoiceList', ref([]))

// 将 API 返回的音色数据转换为 voiceOptions 格式
const voiceOptions = computed(() => {
  if (!globalVoiceList.value || globalVoiceList.value.length === 0) {
    // 如果没有音色数据，返回默认值
    return [
      { label: '默认', value: 'default', desc: '真实3.0', avatar: '音' },
    ]
  }
  
  // 按接口返回的 sortOrder 字段进行排序（从小到大，sortOrder 越小越靠前）
  // 如果 sortOrder 为 null 或 undefined，则视为 0
  const sortedVoices = [...globalVoiceList.value].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  
  return sortedVoices.map(voice => {
    // 获取音色名称的第一个字符作为头像（当 header_url 为空时使用）
    const avatar = voice.voiceName ? voice.voiceName.charAt(0) : '音'
    
    // 生成描述（可以根据实际需求调整）
    const desc = voice.voiceType
    
    return {
      label: voice.voiceName,
      value: voice.voiceId,
      desc: desc,
      avatar: avatar,
      voiceType: voice.voiceType,
      avatar_url: voice.header_url || '' // 使用接口返回的 header_url
    }
  })
})

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

// 滚动处理函数
const handleScroll = async (event) => {
  const target = event.target
  if (!target) return
  
  // 计算是否滚动到底部（距离底部100px时触发）
  const scrollTop = target.scrollTop
  const scrollHeight = target.scrollHeight
  const clientHeight = target.clientHeight
  
  // 距离底部100px时加载更多
  if (scrollHeight - scrollTop - clientHeight < 100) {
    if (hasMore.value && !loadingMore.value && !loading.value) {
      const listData = await loadMoreSentences()
      if (listData) {
        // 合并新的原始数据
        if (originalSentenceListData.value && listData.data) {
          originalSentenceListData.value.data.list = [
            ...(originalSentenceListData.value.data.list || []),
            ...(listData.data.list || [])
          ]
        } else if (listData.data) {
          originalSentenceListData.value = listData
        }
        
        // 初始化新加载的拆句的合成状态
        if (listData.data && listData.data.list) {
          initializeOriginalSentenceStatus(listData)
        }
      }
    }
  }
}

// 检查任务合成状态
const checkTaskSynthesisStatus = async () => {
  if (!taskId.value) return
  
  try {
    const taskDetail = await getTaskDetail(parseInt(taskId.value))
    
    // 响应拦截器返回的是 res.data，所以 taskDetail 就是 data 对象
    // 如果 taskDetail 有 data 属性，说明是嵌套结构，否则 taskDetail 本身就是 data
    const data = taskDetail.data !== undefined ? taskDetail.data : taskDetail
    const status = data?.status
    const mergeId = data?.mergeId
    
    // 更新任务状态（确保是数字类型）
    taskStatus.value = status !== undefined ? Number(status) : null
    taskMergeId.value = mergeId
    
    // status: 0-拆句完成，1-语音合成中，2-语音合成成功，3-语音合成失败，4-语音合并中，5-语音合并成功，6-语音合并失败
    if (status === 1) {
      // 如果状态是"语音合成中"（status === 1），启动轮询
      ElMessage.info('检测到任务正在合成中，将显示合成进度。')
      taskSynthesisProgress.value.visible = true
      taskSynthesisProgress.value.status = 'processing'
      taskSynthesisProgress.value.statusText = '合成中'
      startPollingTaskSynthesis()
    } else if (status === 4) {
      // status = 4（语音合并中），直接跳转到音频合成页面，并传递 mergeId 参数
      if (mergeId) {
        router.push({
          name: 'MergeAudioProgress',
          query: {
            taskId: taskId.value,
            mergeId: mergeId
          }
        })
      } else {
        router.push({
          name: 'MergeAudioProgress',
          query: {
            taskId: taskId.value
          }
        })
      }
    } else if (status === 5) {
      // status = 5（语音合并成功），弹出提示框
      try {
        await ElMessageBox.confirm(
          '当前任务已完成，是否查看音频合并结果？',
          '提示',
          {
            confirmButtonText: '查看合成音频',
            cancelButtonText: '取消',
            type: 'success',
            distinguishCancelAndClose: true
          }
        )
        
        // 用户点击了"查看合成音频"
        if (mergeId) {
          router.push({
            name: 'MergeAudioProgress',
            query: {
              taskId: taskId.value,
              mergeId: mergeId
            }
          })
        } else {
          router.push({
            name: 'MergeAudioProgress',
            query: {
              taskId: taskId.value
            }
          })
        }
      } catch (error) {
        // 用户点击了"取消"，停留在当前精修页面
        if (error === 'cancel') {
          // 什么都不做，停留在当前页面
        }
      }
    }
  } catch (error) {
    console.error('获取任务详情失败:', error)
  }
}

// 获取 taskId 的辅助函数，同时支持 taskId 和 task_id 两种参数名（兼容旧版本）
const getTaskIdFromRoute = () => {
  return route.query.taskId || route.query.task_id
}

onMounted(() => {
  taskId.value = getTaskIdFromRoute()
  if (taskId.value) {
    refreshSentences()
  } else {
    ElMessage.error('缺少任务ID参数')
  }
})

// 监听路由变化，当从其他页面返回时重新加载数据
watch(
  () => route.query.taskId || route.query.task_id,
  (newTaskId) => {
    if (newTaskId && newTaskId !== taskId.value) {
      taskId.value = newTaskId
      refreshSentences()
    }
  },
  { immediate: false }
)

// 组件卸载时清理定时器和停止音频
onBeforeUnmount(() => {
  // 清除所有拆句轮询定时器
  Object.values(originalSentenceStatus.value).forEach(statusInfo => {
    if (statusInfo.timer) {
      clearInterval(statusInfo.timer)
    }
  })
  originalSentenceStatus.value = {}
  
  // 清除所有断句轮询定时器
  Object.values(breakingSentenceStatus.value).forEach(statusInfo => {
    if (statusInfo.timer) {
      clearInterval(statusInfo.timer)
    }
  })
  breakingSentenceStatus.value = {}
  
  // 停止所有音频播放
  stopAllPlayingAudio()
})

// 根据列表数据初始化拆句合成状态
const initializeOriginalSentenceStatus = (listData) => {
  if (!listData || !Array.isArray(listData.list)) return
  
  listData.list.forEach((originalSentence) => {
    const originalSentenceId = originalSentence.originalSentenceId
    if (!originalSentenceId) return
    
    // 映射 synthesisStatus: 0-未合成，1-合成中，2-已合成，3-合成失败
    const statusMap = {
      0: 'pending',
      1: 'processing',
      2: 'completed',
      3: 'failed'
    }
    const status = statusMap[originalSentence.synthesisStatus] || 'pending'
    
    // 初始化状态，如果已存在且正在轮询中，则保留定时器，只更新状态（如果列表状态更准确）
    const existingStatus = originalSentenceStatus.value[originalSentenceId]
    
    if (!existingStatus) {
      // 新建状态
      originalSentenceStatus.value[originalSentenceId] = {
        status,
        audioUrlList: [],
        timer: null
      }
    } else {
      // 如果已有状态
      // 如果正在轮询中（有定时器），且列表状态是"已合成"或"失败"，说明轮询可能已经完成，更新状态
      if (existingStatus.timer) {
        // 如果列表显示已完成或失败，但轮询还在进行，可能是数据不同步，以列表为准
        if (status === 'completed' || status === 'failed') {
          existingStatus.status = status
          // 如果已完成，清除定时器（可能列表数据已更新）
          if (status === 'completed' || status === 'failed') {
            clearInterval(existingStatus.timer)
            existingStatus.timer = null
          }
        }
      } else {
        // 没有定时器，直接更新状态
        existingStatus.status = status
      }
    }
    
    // 如果已合成，尝试从断句列表中构建 audioUrlList
    if (status === 'completed' && Array.isArray(originalSentence.breakingSentenceList)) {
      const audioUrlList = originalSentence.breakingSentenceList
        .filter(bs => bs.audioUrl) // 只包含有音频的断句
        .map(bs => ({
          sequence: bs.sequence || 0,
          audioUrl: bs.audioUrl
        }))
        .sort((a, b) => a.sequence - b.sequence)
      
      if (audioUrlList.length > 0) {
        originalSentenceStatus.value[originalSentenceId].audioUrlList = audioUrlList
      }
    }
  })
}

// 根据列表数据初始化断句合成状态
const initializeBreakingSentenceStatus = (listData) => {
  if (!listData || !Array.isArray(listData.list)) {
    return
  }
  
  let totalBreakingSentences = 0
  let initializedCount = 0
  
  listData.list.forEach((originalSentence, origIndex) => {
    if (!Array.isArray(originalSentence.breakingSentenceList)) return
    
    originalSentence.breakingSentenceList.forEach((breakingSentence, breakIndex) => {
      totalBreakingSentences++
      const breakingSentenceId = breakingSentence.breakingSentenceId
      if (!breakingSentenceId) {
        return
      }
      
      // 统一使用字符串 ID 作为键，避免类型不匹配问题
      const id = String(breakingSentenceId)
      
      // 映射 synthesisStatus: 0-未合成，1-合成中，2-已合成，3-合成失败
      const statusMap = {
        0: 'pending',
        1: 'processing',
        2: 'completed',
        3: 'failed'
      }
      const status = statusMap[breakingSentence.synthesisStatus] || 'pending'
      const audioUrl = breakingSentence.audioUrl || ''
      
      // console.log(`📝 [initializeBreakingSentenceStatus] 处理断句`, {
      //   originalSentenceIndex: origIndex,
      //   breakingSentenceIndex: breakIndex,
      //   breakingSentenceId: breakingSentenceId,
      //   idType: typeof breakingSentenceId,
      //   idString: id,
      //   synthesisStatus: breakingSentence.synthesisStatus,
      //   mappedStatus: status,
      //   audioUrl: audioUrl,
      //   hasAudioUrl: !!audioUrl
      // })
      
      // 初始化状态，如果已存在且正在轮询中，则保留定时器，只更新状态（如果列表状态更准确）
      const existingStatus = breakingSentenceStatus.value[id] || breakingSentenceStatus.value[breakingSentenceId]
      
      if (!existingStatus) {
        // 新建状态，统一使用字符串 ID 作为键
        breakingSentenceStatus.value[id] = {
          status,
          audioUrl,
          timer: null
        }
        initializedCount++
        // console.log(`✅ [initializeBreakingSentenceStatus] 新建状态`, {
        //   id,
        //   status,
        //   audioUrl,
        //   storedKeys: Object.keys(breakingSentenceStatus.value)
        // })
        
        // 如果原 ID 是数字，也存储一份数字版本，确保兼容性
        if (!isNaN(breakingSentenceId) && String(breakingSentenceId) !== id) {
          breakingSentenceStatus.value[breakingSentenceId] = breakingSentenceStatus.value[id]
        }
      } else {
        // 如果已有状态，更新到字符串 ID 键
        const statusObj = breakingSentenceStatus.value[id] || existingStatus
        
        // console.log(`🔄 [initializeBreakingSentenceStatus] 更新已有状态`, {
        //   id,
        //   oldStatus: statusObj.status,
        //   newStatus: status,
        //   oldAudioUrl: statusObj.audioUrl,
        //   newAudioUrl: audioUrl,
        //   hasTimer: !!statusObj.timer
        // })
        
        // 如果正在轮询中（有定时器），且列表状态是"已合成"或"失败"，说明轮询可能已经完成，更新状态
        if (statusObj.timer) {
          // 如果列表显示已完成或失败，但轮询还在进行，可能是数据不同步，以列表为准
          if (status === 'completed' || status === 'failed') {
            statusObj.status = status
            statusObj.audioUrl = audioUrl
            // 如果已完成，清除定时器（可能列表数据已更新）
            if (status === 'completed' || status === 'failed') {
              clearInterval(statusObj.timer)
              statusObj.timer = null
            }
          }
        } else {
          // 没有定时器，直接更新状态
          statusObj.status = status
          statusObj.audioUrl = audioUrl
        }
        
        // 确保字符串 ID 键存在
        breakingSentenceStatus.value[id] = statusObj
        // 如果原 ID 是数字，也更新数字版本
        if (!isNaN(breakingSentenceId) && String(breakingSentenceId) !== id) {
          breakingSentenceStatus.value[breakingSentenceId] = statusObj
        }
      }
    })
  })
  
}

const refreshSentences = async (preserveEditingState = false) => {
  try {
    // 如果要求保持编辑状态，先保存当前状态
    let savedEditingSentenceId = null
    if (preserveEditingState) {
      savedEditingSentenceId = editingSentenceId.value
      // 临时清空编辑状态，避免刷新时的自动恢复逻辑
      editingSentenceId.value = null
    }
    
    // 刷新时重置分页，加载第一页，每页10条（api.md: page 从 0 开始）
    const listData = await loadSentences(taskId.value, 0, 10, false)
    
    // 保存原始数据
    originalSentenceListData.value = listData
    
    // 创建/更新数据备份（每次刷新都更新，确保备份是最新的原始数据）
    createBackup()
    
    // 根据列表数据初始化拆句合成状态
    initializeOriginalSentenceStatus(listData)
    // 根据列表数据初始化断句合成状态
    initializeBreakingSentenceStatus(listData)
    
    // 检查任务状态（在刷新数据后）
    await checkTaskSynthesisStatus()
    
    // 如果要求保持编辑状态，恢复之前的状态
    if (preserveEditingState && savedEditingSentenceId) {
      editingSentenceId.value = savedEditingSentenceId
      return
    }
    
    // 默认行为：如果之前有编辑状态，尝试恢复
    if (editingSentenceId.value) {
      const current = sentences.value.find(
        (item) => item.sentence_id === editingSentenceId.value
      )
      if (!current) {
        // 如果找不到，关闭编辑
        editingSentenceId.value = null
      }
    }
  } catch (error) {
    console.error('加载句子列表失败:', error)
  }
}

// 创建数据备份
const createBackup = () => {
  // 深拷贝 sentences
  backupSentences.value = JSON.parse(JSON.stringify(sentences.value))
  // 深拷贝 originalSentenceListData
  if (originalSentenceListData.value) {
    backupOriginalSentenceListData.value = JSON.parse(JSON.stringify(originalSentenceListData.value))
  }
}

const toggleEdit = async (sentence) => {
  if (editingSentenceId.value === sentence.sentence_id) {
    // 收起编辑
    editingSentenceId.value = null
    return
  }
  
  // 开始编辑
  editingSentenceId.value = sentence.sentence_id
}


// sentenceController 已迁移到 useSentenceEditorOperations composable，不再需要

// convertVolumeFromApi 和 mapSpeedFromSetting 已迁移到 sentenceModels.js


// removeLocalSentence 已不再需要，删除操作在 SentenceEditor 组件内部处理

// handleDelete 和 handleInsertAfter 已迁移到 SentenceEditor 组件

// 获取拆句的合成状态
const getOriginalSentenceSynthesisStatus = (originalSentenceId) => {
  const status = originalSentenceStatus.value[originalSentenceId]
  if (!status) return 'pending'
  return status.status || 'pending'
}

// 合成拆句（根句子）
const handleResynthesizeOriginalSentence = async (originalSentenceId) => {
  // 检查是否有未保存的编辑数据
  if (sentenceEditorRef.value && sentenceEditorRef.value.hasUnsavedChanges && sentenceEditorRef.value.hasUnsavedChanges()) {
    ElMessage.warning('当前有未保存的编辑数据，请先【保存当前修改】再进行合成')
    return
  }
  
  try {
    // 调用合成拆句接口
    await synthesizeOriginalSentence({ originalSentenceId: String(originalSentenceId) })
    ElMessage.success('合成中，请稍候...')
    
    // 更新状态为合成中
    if (!originalSentenceStatus.value[originalSentenceId]) {
      originalSentenceStatus.value[originalSentenceId] = {
        status: 'processing',
        audioUrlList: [],
        timer: null
      }
    } else {
      originalSentenceStatus.value[originalSentenceId].status = 'processing'
    }
    
    // 启动轮询，显示遮罩（类似合成全部音频）
    startPollingOriginalSentenceStatus(originalSentenceId, true)
  } catch (error) {
    console.error('合成拆句失败:', error)
    ElMessage.error('合成失败，请重试')
    if (originalSentenceStatus.value[originalSentenceId]) {
      originalSentenceStatus.value[originalSentenceId].status = 'failed'
    }
    // 如果遮罩已显示，关闭它
    if (taskSynthesisProgress.value.visible) {
      taskSynthesisProgress.value.visible = false
    }
  }
}

// 启动轮询拆句合成状态
const startPollingOriginalSentenceStatus = (originalSentenceId, showProgressDialog = false) => {
  // 初始化状态
  if (!originalSentenceStatus.value[originalSentenceId]) {
    originalSentenceStatus.value[originalSentenceId] = {
      status: 'processing',
      audioUrlList: [],
      timer: null,
      total: 0,
      completed: 0,
      pending: 0,
      progress: 0
    }
  }
  
  // 如果需要显示遮罩，显示它
  if (showProgressDialog) {
    taskSynthesisProgress.value.visible = true
    taskSynthesisProgress.value.status = 'processing'
  }
  
  startPollingSynthesisStatus({
    getStatusApi: () => getOriginalSentenceStatus(originalSentenceId),
    progressState: originalSentenceStatus.value[originalSentenceId],
    onCompleted: (data) => {
      const statusInfo = originalSentenceStatus.value[originalSentenceId]
      statusInfo.status = 'completed'
      statusInfo.audioUrlList = (data.audioUrlList || []).sort((a, b) => (a.sequence || 0) - (b.sequence || 0))
      
      // 如果显示了遮罩，关闭它
      if (showProgressDialog) {
        setTimeout(() => {
          taskSynthesisProgress.value.visible = false
          ElMessage.success('合成完成')
        }, 1000)
      } else {
        ElMessage.success('合成完成')
      }
    },
    onFailed: (data) => {
      const statusInfo = originalSentenceStatus.value[originalSentenceId]
      statusInfo.status = 'failed'
      
      // 如果显示了遮罩，关闭它
      if (showProgressDialog) {
        setTimeout(() => {
          taskSynthesisProgress.value.visible = false
          ElMessage.error('合成失败')
        }, 2000)
      } else {
        ElMessage.error('合成失败')
      }
    },
    onProcessing: (data) => {
      const statusInfo = originalSentenceStatus.value[originalSentenceId]
      statusInfo.status = 'processing'
      // 更新已完成的音频列表
      if (data.audioUrlList && data.audioUrlList.length > 0) {
        statusInfo.audioUrlList = data.audioUrlList.sort((a, b) => (a.sequence || 0) - (b.sequence || 0))
      }
      
      // 更新遮罩进度（如果显示了遮罩）
      if (showProgressDialog && data.total !== undefined) {
        taskSynthesisProgress.value.total = data.total || 0
        taskSynthesisProgress.value.completed = data.completed || 0
        taskSynthesisProgress.value.pending = data.pending || 0
        if (taskSynthesisProgress.value.total > 0) {
          taskSynthesisProgress.value.progress = Math.round(
            (taskSynthesisProgress.value.completed / taskSynthesisProgress.value.total) * 100
          )
        }
      }
    },
    parseStatusData: (statusData) => {
      // 拆句状态数据直接返回，不需要额外解析
      return statusData || {}
    }
  })
}

// 播放拆句（按顺序播放多个音频）
const handlePlayOriginalSentence = async (sentence) => {
  const originalSentenceId = sentence.sentence_id
  
  // 获取该拆句的音频列表
  const statusInfo = originalSentenceStatus.value[originalSentenceId]
  if (!statusInfo || !statusInfo.audioUrlList || statusInfo.audioUrlList.length === 0) {
    ElMessage.warning('暂无音频可播放')
    return
  }
  
  const audioUrlList = statusInfo.audioUrlList
  
  // 如果只有一个音频，使用全局播放器
  if (audioUrlList.length === 1 && audioPlayer) {
    audioPlayer.show(audioUrlList[0].audioUrl)
    return
  }
  
  // 多个音频：使用原来的播放方式（按顺序播放）
  // 停止当前正在播放的音频
  stopAllPlayingAudio()
  
  // 创建音频元素数组
  const audioElements = audioUrlList.map((item, index) => {
    const audio = new Audio(item.audioUrl)
    audio.preload = 'auto'
    return { audio, sequence: item.sequence || index }
  })
  
  // 按 sequence 排序
  audioElements.sort((a, b) => a.sequence - b.sequence)
  
  // 设置当前播放信息
  currentPlayingAudio.value = {
    sentenceId: originalSentenceId,
    audioList: audioElements,
    currentIndex: 0,
    audioElements: audioElements.map(item => item.audio)
  }
  
  // 播放第一个音频
  playNextAudio(0)
}

// 播放下一个音频
const playNextAudio = (index) => {
  if (!currentPlayingAudio.value || index >= currentPlayingAudio.value.audioElements.length) {
    // 播放完成
    currentPlayingAudio.value = null
    return
  }
  
  const audio = currentPlayingAudio.value.audioElements[index]
  currentPlayingAudio.value.currentIndex = index
  
  // 监听播放结束事件
  const onEnded = () => {
    audio.removeEventListener('ended', onEnded)
    // 播放下一个
    playNextAudio(index + 1)
  }
  
  audio.addEventListener('ended', onEnded)
  
  // 播放当前音频
  audio.play().catch(error => {
    console.error('播放音频失败:', error)
    ElMessage.error('播放失败')
    currentPlayingAudio.value = null
  })
}

// 停止所有正在播放的音频
const stopAllPlayingAudio = () => {
  if (currentPlayingAudio.value) {
    currentPlayingAudio.value.audioElements.forEach(audio => {
      audio.pause()
      audio.currentTime = 0
    })
    currentPlayingAudio.value = null
  }
  
  // 停止所有 audioRefs 中的音频
  Object.values(audioRefs.value).forEach(audio => {
    if (audio && !audio.paused) {
      audio.pause()
      audio.currentTime = 0
    }
  })
}


/**
 * 通用的合成状态轮询函数
 * @param {Object} config - 轮询配置
 * @param {Function} config.getStatusApi - 获取状态的API函数
 * @param {Object|Ref} config.progressState - 进度状态对象（可以是 ref 或普通对象）
 * @param {Function} config.onCompleted - 完成回调
 * @param {Function} config.onFailed - 失败回调
 * @param {Function} config.onProcessing - 处理中回调（可选）
 * @param {Function} config.parseStatusData - 解析状态数据的函数（可选）
 * @param {Function} config.getProgressState - 获取进度状态的函数（可选，用于从对象中获取状态）
 */
const startPollingSynthesisStatus = ({
  getStatusApi,
  progressState,
  onCompleted,
  onFailed,
  onProcessing,
  parseStatusData,
  getProgressState
}) => {
  // 获取状态对象的辅助函数
  const getState = () => {
    if (getProgressState) {
      return getProgressState()
    }
    // 如果是 ref，返回 .value；否则直接返回
    return progressState.value !== undefined ? progressState.value : progressState
  }
  
  // 设置状态对象的辅助函数
  const setState = (updates) => {
    const state = getState()
    Object.assign(state, updates)
  }
  
  // 清除之前的定时器
  const currentState = getState()
  if (currentState.timer) {
    clearInterval(currentState.timer)
  }
  
  const poll = async () => {
    try {
      const statusData = await getStatusApi()
      
      // 如果提供了自定义解析函数，使用它；否则直接使用返回的数据
      const data = parseStatusData ? parseStatusData(statusData) : (statusData || {})
      
      const state = getState()
      
      // 更新进度信息（如果数据中有这些字段）
      if (data.total !== undefined) {
        state.total = data.total || 0
      }
      if (data.completed !== undefined) {
        state.completed = data.completed || 0
      }
      if (data.pending !== undefined) {
        state.pending = data.pending || 0
      }
      
      // 计算进度百分比
      if (state.total > 0) {
        state.progress = Math.round(
          (state.completed / state.total) * 100
        )
      } else if (data.progress !== undefined) {
        state.progress = data.progress || 0
      }
      
      // 判断状态
      const status = data.status
      const statusNum = Number(status)
      
      // 判断是否完成：status === 2（语音合成成功）或 progress >= 100 或 completed >= total
      const isCompleted = status === 'completed' || 
                         statusNum === 2 || 
                         state.progress >= 100 ||
                         (state.total > 0 && 
                          state.completed >= state.total)
      
      // 判断是否失败：status === 3（语音合成失败）
      const isFailed = status === 'failed' || statusNum === 3
      
      if (isCompleted) {
        // 合成完成
        setState({ status: 'completed', progress: 100 })
        
        // 清除定时器 - 需要重新获取 state，确保获取到最新的定时器引用
        const currentState = getState()
        if (currentState.timer) {
          clearInterval(currentState.timer)
          currentState.timer = null
        }
        
        // 调用完成回调
        if (onCompleted) {
          onCompleted(data)
        }
      } else if (isFailed) {
        // 合成失败
        setState({ status: 'failed' })
        
        // 清除定时器 - 需要重新获取 state，确保获取到最新的定时器引用
        const currentState = getState()
        if (currentState.timer) {
          clearInterval(currentState.timer)
          currentState.timer = null
        }
        
        // 调用失败回调
        if (onFailed) {
          onFailed(data)
        }
      } else {
        // 继续合成中
        setState({ status: 'processing' })
        
        // 调用处理中回调
        if (onProcessing) {
          onProcessing(data)
        }
      }
    } catch (error) {
      console.error('获取合成状态失败:', error)
      // 出错时不清除定时器，继续轮询
    }
  }
  
  // 设置定时器，每500ms轮询一次
  const timer = setInterval(() => {
    // 在每次轮询前检查状态，如果已完成或失败，停止轮询
    const currentState = getState()
    if (currentState.status === 'completed' || currentState.status === 'failed') {
      if (currentState.timer) {
        clearInterval(currentState.timer)
        currentState.timer = null
      }
      return
    }
    poll()
  }, 500)
  
  const state = getState()
  state.timer = timer
  
  // 立即执行一次
  poll()
}

// 启动任务合成进度轮询
const startPollingTaskSynthesis = () => {
  // 显示遮罩
  taskSynthesisProgress.value.visible = true
  taskSynthesisProgress.value.status = 'processing'
  
  startPollingSynthesisStatus({
    getStatusApi: () => getTaskStatus(parseInt(taskId.value)),
    progressState: taskSynthesisProgress,
    onCompleted: async (data) => {
      taskStatus.value = 2 // 更新任务状态为 2（语音合成成功）
      // 延迟关闭遮罩并刷新数据
      setTimeout(async () => {
        taskSynthesisProgress.value.visible = false
        await refreshSentences()
        ElMessage.success('合成完成')
      }, 1000)
    },
    onFailed: (data) => {
      taskStatus.value = 3 // 更新任务状态为 3（语音合成失败）
      // 延迟关闭遮罩
      setTimeout(() => {
        taskSynthesisProgress.value.visible = false
        ElMessage.error('合成失败')
      }, 2000)
    }
  })
}

// 跳转到阅读规范页面
const handleReadingRules = () => {
  if (!taskId.value) {
    ElMessage.warning('缺少任务ID')
    return
  }
  router.push({
    path: '/reading-rules',
    query: {
      taskId: taskId.value
    }
  })
}

// 合成全部音频
const handleSynthesizeAll = async () => {
  if (!taskId.value) {
    ElMessage.warning('缺少任务ID')
    return
  }
  
  // 检查是否有未保存的编辑数据
  if (sentenceEditorRef.value && sentenceEditorRef.value.hasUnsavedChanges && sentenceEditorRef.value.hasUnsavedChanges()) {
    ElMessage.warning('当前有未保存的编辑数据，请先【保存当前修改】再进行合成')
    return
  }
  
  try {
    synthesizingAll.value = true
    await synthesizeTask({ taskId: parseInt(taskId.value) })
    
    // 启动轮询
    startPollingTaskSynthesis()
  } catch (error) {
    console.error('合成全部音频失败:', error)
    ElMessage.error('合成失败，请重试')
    synthesizingAll.value = false
  } finally {
    // 注意：synthesizingAll 在轮询完成后不需要重置，因为遮罩已经显示了
  }
}

// 根据任务状态计算按钮可用性
const canSynthesizeAll = computed(() => {
  // status = 0（拆句完成）：可点击
  // status = 1（语音合成中）：不可点击
  // status = 2（语音合成成功）：不可点击
  // status = 3（语音合成失败）：可点击
  // status = 4（语音合并中）：不可点击（但会直接跳转）
  // status = 5（语音合并成功）：不可点击
  // status = 6（语音合并失败）：可点击
  if (taskStatus.value === null) return true // 初始状态，默认可点击
  return taskStatus.value === 0 || taskStatus.value === 3 || taskStatus.value === 6
})

const canMergeAudio = computed(() => {
  // status = 0（拆句完成）：不可点击
  // status = 1（语音合成中）：不可点击
  // status = 2（语音合成成功）：可点击
  // status = 3（语音合成失败）：不可点击
  // status = 4（语音合并中）：不可点击（但会直接跳转）
  // status = 5（语音合并成功）：不可点击
  // status = 6（语音合并失败）：可点击
  if (taskStatus.value === null) return false // 初始状态，默认不可点击
  return taskStatus.value === 2 || taskStatus.value === 6
})

const handleMergeAudio = async () => {
  if (!taskId.value) {
    ElMessage.warning('缺少任务ID')
    return
  }
  
  try {
    // 调用合并音频接口
    // mergeAudioTask 是 useSentencesRepository 中的方法，它调用 mergeAudio API
    // 响应拦截器返回的是 res.data，所以返回值直接就是 data 对象
    const mergeResult = await mergeAudioTask(taskId.value)
    
    // 从接口返回的数据中获取 mergeId
    // mergeResult 可能是直接的数据对象，也可能有 data 属性
    const data = mergeResult?.data !== undefined ? mergeResult.data : mergeResult
    const mergeId = data?.mergeId || data?.merge_id || taskMergeId.value
    
    // 更新 taskMergeId
    if (mergeId) {
      taskMergeId.value = mergeId
    }
    
    // 跳转到合并音频进度页面
    const query = {
      taskId: taskId.value || route.query.taskId
    }
    
    // 如果有 mergeId，传递 mergeId 参数
    if (mergeId) {
      query.mergeId = String(mergeId) // 确保是字符串类型
    }
    router.push({
      name: 'MergeAudioProgress',
      query
    })
  } catch (error) {
    console.error('合并音频失败:', error)
    ElMessage.error('合并音频失败，请重试')
  }
}

const setAudioRef = (sentenceId, el) => {
  if (el) {
    audioRefs.value[sentenceId] = el
  }
}

// 所有编辑相关的函数已迁移到 SentenceEditor 组件

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
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 20px;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.page-header h2 {
  font-size: 22px;
  font-weight: 600;
}

.sentences-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  margin: 0 20px;
}

.sentences-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 20px;
}

.empty-state {
  padding: 40px;
  text-align: center;
}

.sentence-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 8px;
}

.sentence-list::-webkit-scrollbar {
  width: 6px;
}

.sentence-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.sentence-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.sentence-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.loading-more,
.no-more {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 20px;
  color: #999;
  font-size: 14px;
  margin-top: auto;
  margin-bottom: 0px;
}

.loading-more .el-icon {
  font-size: 16px;
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

/* 编辑相关的样式已迁移到 SentenceEditor 组件 */

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.synthesis-progress-dialog :deep(.el-dialog__header) {
  padding: 20px 20px 10px;
}

.synthesis-progress-dialog :deep(.el-dialog__body) {
  padding: 20px;
}

.progress-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1d1f23;
}

.progress-content {
  padding: 10px 0;
}

.progress-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-text {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #666;
}

.progress-status {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.progress-status .success {
  color: #67c23a;
  font-weight: 600;
}

.progress-status .error {
  color: #f56c6c;
  font-weight: 600;
}
</style>

