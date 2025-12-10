import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useSentenceEditorApi } from './useSentenceEditorApi'

/**
 * 合成功能 composable
 */
export function useSentenceEditorSynthesis(originalSentence, getBreakingSentences) {
  const { synthesize, getSynthesisStatus } = useSentenceEditorApi()

  // 断句合成状态
  const breakingSentenceStatus = ref({})

  /**
   * 通用的合成状态轮询函数
   */
  const startPollingSynthesisStatus = ({
    getStatusApi,
    progressState,
    onCompleted,
    onFailed,
    onProcessing,
    parseStatusData
  }) => {
    const getState = () => {
      return progressState.value !== undefined ? progressState.value : progressState
    }

    const setState = (updates) => {
      const state = getState()
      Object.assign(state, updates)
    }

    const currentState = getState()
    if (currentState.timer) {
      clearInterval(currentState.timer)
    }

    const poll = async () => {
      try {
        const statusData = await getStatusApi()
        const data = parseStatusData ? parseStatusData(statusData) : (statusData || {})
        const state = getState()

        if (data.total !== undefined) {
          state.total = data.total || 0
        }
        if (data.completed !== undefined) {
          state.completed = data.completed || 0
        }
        if (data.pending !== undefined) {
          state.pending = data.pending || 0
        }

        if (state.total > 0) {
          state.progress = Math.round((state.completed / state.total) * 100)
        } else if (data.progress !== undefined) {
          state.progress = data.progress || 0
        }

        const status = data.status
        const statusNum = Number(status)

        const isCompleted = status === 'completed' || 
                           statusNum === 2 || 
                           state.progress >= 100 ||
                           (state.total > 0 && state.completed >= state.total)

        const isFailed = status === 'failed' || statusNum === 3

        if (isCompleted) {
          setState({ status: 'completed', progress: 100 })
          const currentState = getState()
          if (currentState.timer) {
            clearInterval(currentState.timer)
            currentState.timer = null
          }
          if (onCompleted) {
            onCompleted(data)
          }
        } else if (isFailed) {
          setState({ status: 'failed' })
          const currentState = getState()
          if (currentState.timer) {
            clearInterval(currentState.timer)
            currentState.timer = null
          }
          if (onFailed) {
            onFailed(data)
          }
        } else {
          setState({ status: 'processing' })
          if (onProcessing) {
            onProcessing(data)
          }
        }
      } catch (error) {
        console.error('获取合成状态失败:', error)
      }
    }

    const timer = setInterval(() => {
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
    poll()
  }

  /**
   * 启动轮询断句合成状态
   */
  const startPollingBreakingSentenceStatus = (breakingSentenceId) => {
    if (!breakingSentenceStatus.value[breakingSentenceId]) {
      breakingSentenceStatus.value[breakingSentenceId] = {
        status: 'processing',
        audioUrl: '',
        timer: null
      }
    }

    startPollingSynthesisStatus({
      getStatusApi: () => getSynthesisStatus('breaking', breakingSentenceId),
      progressState: breakingSentenceStatus.value[breakingSentenceId],
      onCompleted: (data) => {
        const statusInfo = breakingSentenceStatus.value[breakingSentenceId]
        statusInfo.status = 'completed'
        statusInfo.audioUrl = data.audioUrl || ''
        ElMessage.success('合成完成')
      },
      onFailed: (data) => {
        const statusInfo = breakingSentenceStatus.value[breakingSentenceId]
        statusInfo.status = 'failed'
        ElMessage.error('合成失败')
      },
      onProcessing: (data) => {
        const statusInfo = breakingSentenceStatus.value[breakingSentenceId]
        statusInfo.status = 'processing'
        if (data.audioUrl) {
          statusInfo.audioUrl = data.audioUrl
        }
      },
      parseStatusData: (statusData) => {
        const synthesisStatus = statusData.synthesisStatus
        let status = 'processing'
        if (synthesisStatus === 2) {
          status = 'completed'
        } else if (synthesisStatus === 3) {
          status = 'failed'
        } else if (synthesisStatus === 0) {
          status = 'pending'
        }
        return {
          ...statusData,
          status
        }
      }
    })
  }

  /**
   * 合成断句
   */
  const handleResynthesizeBreakingSentence = async (breakingSentenceId) => {
    try {
      await synthesize('breaking', breakingSentenceId)
      ElMessage.success('合成中，请稍候...')

      if (!breakingSentenceStatus.value[breakingSentenceId]) {
        breakingSentenceStatus.value[breakingSentenceId] = {
          status: 'processing',
          audioUrl: '',
          timer: null
        }
      } else {
        breakingSentenceStatus.value[breakingSentenceId].status = 'processing'
      }

      startPollingBreakingSentenceStatus(breakingSentenceId)
    } catch (error) {
      console.error('合成断句失败:', error)
      ElMessage.error('合成失败，请重试')
      if (breakingSentenceStatus.value[breakingSentenceId]) {
        breakingSentenceStatus.value[breakingSentenceId].status = 'failed'
      }
    }
  }

  /**
   * 全部合成：合成当前拆句下的所有断句
   */
  const handleSynthesizeAllBreakingSentences = async (originalSentenceId) => {
    if (!originalSentenceId) {
      ElMessage.warning('请先选择一个拆句进行编辑')
      return
    }

    try {
      await synthesize('original', originalSentenceId)
      ElMessage.success('合成中，请稍候...')

      const breakingSentences = getBreakingSentences ? getBreakingSentences(originalSentenceId) : []

      if (breakingSentences.length === 0) {
        ElMessage.warning('该拆句下没有断句')
        return
      }

      breakingSentences.forEach(breakingSentence => {
        const breakingSentenceId = breakingSentence.sentence_id

        if (!breakingSentenceStatus.value[breakingSentenceId]) {
          breakingSentenceStatus.value[breakingSentenceId] = {
            status: 'processing',
            audioUrl: '',
            timer: null
          }
        } else {
          breakingSentenceStatus.value[breakingSentenceId].status = 'processing'
        }

        startPollingBreakingSentenceStatus(breakingSentenceId)
      })
    } catch (error) {
      console.error('全部合成失败:', error)
      ElMessage.error('合成失败，请重试')

      const breakingSentences = getBreakingSentences ? getBreakingSentences(originalSentenceId) : []
      breakingSentences.forEach(breakingSentence => {
        const breakingSentenceId = breakingSentence.sentence_id
        if (breakingSentenceStatus.value[breakingSentenceId]) {
          breakingSentenceStatus.value[breakingSentenceId].status = 'failed'
        }
      })
    }
  }

  /**
   * 获取断句合成状态
   */
  const getBreakingSentenceSynthesisStatus = (breakingSentenceId, originalSentence) => {
    // 新插入的断句（ID 为负数），直接返回 pending 状态
    if (typeof breakingSentenceId === 'number' && breakingSentenceId < 0) {
      return { status: 'pending', audioUrl: '' }
    }

    // 从状态中获取
    const statusInfo = breakingSentenceStatus.value[breakingSentenceId]
    if (statusInfo) {
      return { status: statusInfo.status, audioUrl: statusInfo.audioUrl || '' }
    }

    // 从原始数据中查找
    if (originalSentence && Array.isArray(originalSentence.breakingSentenceList)) {
      const breakingSentence = originalSentence.breakingSentenceList.find(
        bs => bs.breakingSentenceId == breakingSentenceId || 
             String(bs.breakingSentenceId) === String(breakingSentenceId)
      )

      if (breakingSentence) {
        const statusMap = {
          0: 'pending',
          1: 'processing',
          2: 'completed',
          3: 'failed'
        }
        const status = statusMap[breakingSentence.synthesisStatus] || 'pending'
        const audioUrl = breakingSentence.audioUrl || ''
        return { status, audioUrl }
      }
    }

    return { status: 'pending', audioUrl: '' }
  }

  return {
    // 状态
    breakingSentenceStatus,
    // 方法
    handleResynthesizeBreakingSentence,
    handleSynthesizeAllBreakingSentences,
    getBreakingSentenceSynthesisStatus,
    startPollingBreakingSentenceStatus
  }
}

