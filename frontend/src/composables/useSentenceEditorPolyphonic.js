import { ref, reactive } from 'vue'
import { polyphonic } from 'pinyin-pro'
import { createPolyphonicState } from '@/models/sentenceModels'

/**
 * 多音字功能 composable
 */
export function useSentenceEditorPolyphonic() {
  // 多音字状态映射
  const polyphonicStateMap = reactive({})

  // 多音字模式映射
  const polyphonicModeMap = reactive({})

  // 多音字提示框状态
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

  /**
   * 确保多音字状态存在
   */
  const ensurePolyphonicState = (sentenceId) => {
    if (!sentenceId) return null
    if (!polyphonicStateMap[sentenceId]) {
      polyphonicStateMap[sentenceId] = createPolyphonicState()
    }
    if (typeof polyphonicModeMap[sentenceId] === 'undefined') {
      polyphonicModeMap[sentenceId] = false
    }
    return polyphonicStateMap[sentenceId]
  }

  /**
   * 构建多音字标记
   */
  const buildPolyphonicMarkers = (sub) => {
    if (!sub) return []
    const content = sub.content || ''
    const sentenceId = sub.sentence_id
    const state = ensurePolyphonicState(sentenceId)
    if (!content) {
      state.markers = []
      return []
    }

    // 先从 polyphonicOverrides 中恢复已保存的选择
    // polyphonicOverrides 格式：{ begin, end, ph, alphabet }
    // begin 和 end 是基于纯文本的位置（因为保存时 marker.offset 是基于纯文本计算的）
    const polyphonicOverrides = sub.polyphonicOverrides || []
    
    // 清空之前的选择，避免累积（每次构建都重新从 polyphonicOverrides 恢复）
    state.selections = {}
    
    // 先建立纯文本位置到内容位置的映射表（一次性建立，避免重复遍历）
    const plainToContentMap = new Map()
    let plainCount = 0
    
    for (let i = 0; i < content.length; i++) {
      const char = content[i]
      
      // 跳过标记
      if (char === '<') {
        const markerEnd = content.indexOf('>', i)
        if (markerEnd !== -1) {
          i = markerEnd
          continue
        }
      }
      
      // 记录纯文本位置到内容位置的映射
      plainToContentMap.set(plainCount, i)
      plainCount++
    }
    
    // 将 polyphonicOverrides 中的选择应用到 state.selections
    polyphonicOverrides.forEach((override) => {
      const begin = override.begin || 0
      const end = override.end || begin + 1
      const ph = override.ph || ''
      
      if (!ph) return
      
      // 遍历 begin 到 end 之间的位置（纯文本位置）
      for (let plainIndex = begin; plainIndex < end; plainIndex++) {
        // 使用映射表查找对应的内容位置
        const contentIndex = plainToContentMap.get(plainIndex)
        if (contentIndex !== undefined) {
          const markerId = `${sentenceId}-${contentIndex}`
          state.selections[markerId] = ph
        }
      }
    })

    let results = []
    try {
      results = polyphonic(content, { type: 'array' })
    } catch (error) {
      console.warn('polyphonic parse failed:', error)
      state.markers = []
      return []
    }

    const markers = []
    let plainTextOffset = 0 // 纯文本位置计数器
    
    for (let i = 0; i < content.length; i += 1) {
      const char = content[i]
      
      // 跳过标记字符
      if (char === '<') {
        const markerEnd = content.indexOf('>', i)
        if (markerEnd !== -1) {
          i = markerEnd
          continue
        }
      }
      
      const optionsRaw = results[i] || []
      const normalizedOptions = (optionsRaw || [])
        .map((item) => (item || '').trim())
        .filter((item) => item && item !== char)
      const uniqueOptions = [...new Set(normalizedOptions)]

      // markerId 使用内容索引（因为 state.selections 的 key 是基于内容索引的）
      const markerId = `${sentenceId}-${i}`
      const selected = state.selections?.[markerId] || null
      
      // 如果字符是多音字，或者有已保存的选择（即使当前不是多音字，也要显示已保存的选择）
      if (uniqueOptions.length > 1 || selected) {
      markers.push({
        id: markerId,
        sentenceId,
          offset: plainTextOffset, // 使用纯文本位置，与保存时的逻辑一致
        length: 1,
        char,
          options: uniqueOptions.length > 1 ? uniqueOptions : (selected ? [selected] : []),
        selected
      })
      }
      
      plainTextOffset++
    }

    state.markers = markers
    return markers
  }

  /**
   * 刷新多音字标记
   */
  const refreshPolyphonicForSub = (sub) => {
    if (!sub) return
    ensurePolyphonicState(sub.sentence_id)
    buildPolyphonicMarkers(sub)
  }

  /**
   * 获取多音字标记
   */
  const getPolyphonicMarkers = (sub) => {
    if (!sub) return []
    return polyphonicStateMap[sub.sentence_id]?.markers || []
  }

  /**
   * 检查多音字模式是否激活
   */
  const isPolyphonicModeActive = (sub) => {
    if (!sub) return false
    return !!polyphonicModeMap[sub.sentence_id]
  }

  /**
   * 切换多音字模式
   */
  const togglePolyphonicMode = (sentenceId) => {
    if (!sentenceId) return
    const current = !!polyphonicModeMap[sentenceId]
    polyphonicModeMap[sentenceId] = !current
  }

  /**
   * 取消提示框隐藏
   */
  const cancelTooltipHide = () => {
    if (polyphonicTooltipTimer) {
      clearTimeout(polyphonicTooltipTimer)
      polyphonicTooltipTimer = null
    }
  }

  /**
   * 隐藏多音字提示框
   */
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

  /**
   * 安排提示框隐藏
   */
  const scheduleTooltipHide = () => {
    if (isTooltipHovering.value) return
    cancelTooltipHide()
    polyphonicTooltipTimer = setTimeout(() => {
      if (!isTooltipHovering.value) {
        hidePolyphonicTooltip()
      }
    }, 250)
  }

  /**
   * 显示多音字提示框
   */
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

  /**
   * 处理多音字悬停
   */
  const handlePolyphonicHover = (sub, payload, getPolyphonicMarkersFn) => {
    if (!sub) return
    if (!payload) {
      scheduleTooltipHide()
      return
    }
    if (!payload.rect) {
      scheduleTooltipHide()
      return
    }
    const markers = getPolyphonicMarkersFn ? getPolyphonicMarkersFn(sub) : getPolyphonicMarkers(sub)
    const marker = markers.find((item) => item.id === payload.markerId)
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

  /**
   * 处理多音字选项选择
   */
  const handlePolyphonicOptionSelect = (option, sentenceId, markerId) => {
    if (!sentenceId || !markerId) return
    const state = ensurePolyphonicState(sentenceId)
    if (!state) return
    if (option) {
      state.selections[markerId] = option
    } else {
      delete state.selections[markerId]
    }
    hidePolyphonicTooltip()
  }

  /**
   * 处理提示框鼠标进入
   */
  const handleTooltipMouseEnter = () => {
    isTooltipHovering.value = true
    cancelTooltipHide()
  }

  /**
   * 处理提示框鼠标离开
   */
  const handleTooltipMouseLeave = () => {
    isTooltipHovering.value = false
    scheduleTooltipHide()
  }

  return {
    // 状态
    polyphonicStateMap,
    polyphonicModeMap,
    polyphonicTooltip,
    // 方法
    ensurePolyphonicState,
    buildPolyphonicMarkers,
    refreshPolyphonicForSub,
    getPolyphonicMarkers,
    isPolyphonicModeActive,
    togglePolyphonicMode,
    handlePolyphonicHover,
    handlePolyphonicOptionSelect,
    hidePolyphonicTooltip,
    handleTooltipMouseEnter,
    handleTooltipMouseLeave
  }
}

