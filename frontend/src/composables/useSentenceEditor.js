import { ref, reactive, computed, watch, nextTick, inject } from 'vue'
import { ElMessage } from 'element-plus'
import { useSentenceEditorState } from './useSentenceEditorState'
import { useSentenceEditorPolyphonic } from './useSentenceEditorPolyphonic'
import { useSentenceEditorSpeed } from './useSentenceEditorSpeed'
import { useSentenceEditorSynthesis } from './useSentenceEditorSynthesis'
import { useSentenceEditorPlayback } from './useSentenceEditorPlayback'
import { useSentenceEditorSplit } from './useSentenceEditorSplit'
import { useSentenceEditorSave } from './useSentenceEditorSave'
import {
  createSubSentence,
  applyAllMarkersToContent,
  mapBreakListToPauseMarkers,
  mapPhonemeListToPolyphonic,
  mapProsodyToSpeedSegments,
  mapSilentListToSilenceMarkers,
  mapSpeedFromSetting,
  convertVolumeFromApi,
  extractPlainTextFromContent
} from '@/models/sentenceModels'
import { useSentenceEditorOperations } from './useSentenceEditorOperations'
import { getMatchingFieldListFromText } from '@/api/readingRule'

/**
 * 主编辑区域 composable
 * 组合所有小的 composables，提供统一的接口
 */
export function useSentenceEditor(props, emit) {
  // 注入音频播放器
  const audioPlayer = inject('audioPlayer', null)

  // 展开状态
  const internalExpanded = ref(props.expanded || false)

  // 使用所有小的 composables
  const state = useSentenceEditorState()
  const polyphonic = useSentenceEditorPolyphonic()
  const speed = useSentenceEditorSpeed()
  const save = useSentenceEditorSave()
  const split = useSentenceEditorSplit()

  // 内部断句列表（从 originalSentence 转换而来）
  const internalBreakingSentences = ref([])
  
  // 阅读规则状态：记录每个子句的阅读规则匹配和应用状态
  const readingRulesStateMap = reactive({})
  
  // 阅读规则提示框状态
  const readingRuleTooltip = reactive({
    visible: false,
    sentenceId: null,
    ruleId: '',
    pattern: '',
    applied: false,
    position: { x: 0, y: 0 }
  })
  
  let readingRuleTooltipTimer = null
  const isReadingRuleTooltipHovering = ref(false)

  // 生成新句子ID的计数器
  let newSentenceIdCounter = -1
  const generateNewSentenceId = () => {
    return newSentenceIdCounter--
  }

  // 查找断句
  const findBreakingSentenceById = (id) => {
    if (id == null) return null
    return internalBreakingSentences.value.find(
      (item) => item.sentence_id == id || String(item.sentence_id) === String(id)
    )
  }

  // 初始化合成功能（需要 getBreakingSentences 函数）
  const getBreakingSentences = (originalSentenceId) => {
    return internalBreakingSentences.value.filter(
      sub => sub.parent_id === originalSentenceId && sub.parent_id !== 0
    )
  }

  const synthesis = useSentenceEditorSynthesis(props.originalSentence, getBreakingSentences)
  const playback = useSentenceEditorPlayback(audioPlayer)

  // 创建句子操作 composable（用于断句操作）
  const insertAfterLocal = (parentId, data) => {
    const newId = generateNewSentenceId()
    const newSentence = createSubSentence({
      sentence_id: newId,
      parent_id: parentId,
      content: data.content || '',
      display_order: internalBreakingSentences.value.filter(s => s.parent_id === parentId).length,
      ...data
    })
    internalBreakingSentences.value.push(newSentence)
    return newSentence
  }

  const operations = useSentenceEditorOperations({
    sentences: internalBreakingSentences,
    findSentenceById: findBreakingSentenceById,
    generateNewSentenceId,
    originalSentenceListData: { value: { list: props.originalSentence ? [props.originalSentence] : [] } }
  })

  // 初始化断句列表（从 originalSentence 转换）
  const initializeBreakingSentences = () => {
    const originalSentence = props.originalSentence
    if (!originalSentence) {
      internalBreakingSentences.value = []
      return
    }
    if (!Array.isArray(originalSentence.breakingSentenceList)) {
      internalBreakingSentences.value = []
      return
    }

    internalBreakingSentences.value = originalSentence.breakingSentenceList.map((breakingSentence) => {
      const setting = breakingSentence.setting || {}
      const plainContent = breakingSentence.content || setting.content || ''
      const contentWithMarkers = applyAllMarkersToContent(plainContent, setting)

      return createSubSentence({
        sentence_id: breakingSentence.breakingSentenceId,
        parent_id: originalSentence.originalSentenceId,
        content: contentWithMarkers,
        audio_url: breakingSentence.audioUrl || '',
        display_order: breakingSentence.sequence || 0,
        voice: setting.voiceId || 'default',
        volume: convertVolumeFromApi(setting.volume),
        speed: mapSpeedFromSetting(setting),
        pitch: 50,
        speedSegments: mapProsodyToSpeedSegments(setting.prosodyList || []),
        pauseMarkers: mapBreakListToPauseMarkers(setting.breakList || []),
        polyphonicOverrides: mapPhonemeListToPolyphonic(setting.phonemeList || []),
        silenceMarkers: mapSilentListToSilenceMarkers(setting.silentList || [])
      })
    }).sort((a, b) => (a.display_order || 0) - (b.display_order || 0))
  }

  // 选择子句（需要在初始化之前定义）
  const selectSubSentence = (sub) => {
    if (!sub) return
    state.editingSubSentenceId.value = sub.sentence_id
    state.updateEditingForm({
      sentenceId: sub.sentence_id,
      content: sub.content,
      voice: sub.voice || 'default',
      volume: sub.volume,
      speed: sub.speed,
      pitch: sub.pitch || 50
    })
    polyphonic.refreshPolyphonicForSub(sub)
  }

  // 初始化（如果 originalSentence 存在）
  if (props.originalSentence) {
    initializeBreakingSentences()
    // Select the first sub-sentence if available
    if (internalBreakingSentences.value.length > 0) {
      selectSubSentence(internalBreakingSentences.value[0])
    }
  }

  // 监听 originalSentence 变化，重新初始化
  // 注意：在拆分操作期间（isSplitting = true）不重新初始化，避免覆盖新创建的断句
  watch(() => props.originalSentence, () => {
    // 如果正在执行拆分操作，跳过重新初始化，避免覆盖新创建的断句
    if (isSplitting.value) {
      return
    }
    
    if (!props.originalSentence) {
      internalBreakingSentences.value = []
      state.editingSubSentenceId.value = null
      return
    }
    initializeBreakingSentences()
    // Select the first sub-sentence if available
    if (internalBreakingSentences.value.length > 0) {
      selectSubSentence(internalBreakingSentences.value[0])
    }
  }, { immediate: false, deep: true })

  // 编辑器选择变化（使用防抖避免频繁更新）
  let selectionChangeTimer = null
  const handleEditorSelectionChange = (event) => {
    // 事件格式: { sub, payload }
    const { sub, payload = {} } = event || {}
    if (!sub || !sub.sentence_id) return
    
    // 清除之前的定时器
    if (selectionChangeTimer) {
      clearTimeout(selectionChangeTimer)
    }
    
    // 使用防抖，延迟更新，避免在插入停顿时频繁触发
    selectionChangeTimer = setTimeout(() => {
      const currentPauseEligibility = state.pauseEligibilityMap[sub.sentence_id]
      const newPauseEligibility = !!payload?.hasTextBefore
      
      // 只在值真正变化时才更新，避免不必要的响应式触发
      if (currentPauseEligibility !== newPauseEligibility) {
        state.pauseEligibilityMap[sub.sentence_id] = newPauseEligibility
      }
      
      // 检查 selectionStateMap 是否需要更新
      const currentState = state.selectionStateMap[sub.sentence_id]
      const newState = {
        ...payload,
        sentenceId: sub.sentence_id
      }
      
      // 只在状态真正变化时才更新
      if (!currentState || JSON.stringify(currentState) !== JSON.stringify(newState)) {
        state.selectionStateMap[sub.sentence_id] = newState
      }
      
      selectionChangeTimer = null
    }, 50) // 50ms 防抖
  }

  // 当前编辑子句的内容（计算属性，用于实时显示字符计数）
  const currentContent = computed(() => {
    const currentSub = internalBreakingSentences.value.find(
      s => s.sentence_id === state.editingSubSentenceId.value
    )
    return currentSub?.content || ''
  })

  // 编辑器内容变化
  const handleEditorContentChange = (sub) => {
    if (!sub) return
    // sub.content 已经通过 v-model 自动更新了
    // 不再同步到 editingForm.content，避免循环更新
    // editingForm.content 只在 selectSubSentence 时更新一次
    // 实时字符计数使用 currentContent 计算属性
    polyphonic.refreshPolyphonicForSub(sub)
  }

  // 编辑器焦点
  const handleEditorFocus = (sub) => {
    if (!sub) return
    if (state.editingSubSentenceId.value !== sub.sentence_id) {
      selectSubSentence(sub)
    }
  }

  // 语速段落变化
  const handleSpeedSegmentsChange = ({ sub, segments }) => {
    speed.handleSpeedSegmentsChange({ sub, segments })
  }

  // 多音字悬停
  const handlePolyphonicHover = (event) => {
    // 事件格式: { sub, payload }
    const { sub, payload } = event || {}
    if (!sub) return
    polyphonic.handlePolyphonicHover(sub, payload, polyphonic.getPolyphonicMarkers)
  }

  // 多音字选项选择
  const handlePolyphonicOptionSelect = (option) => {
    const { sentenceId, markerId } = polyphonic.polyphonicTooltip
    if (!sentenceId || !markerId) return
    polyphonic.handlePolyphonicOptionSelect(option, sentenceId, markerId)
    const target = findBreakingSentenceById(sentenceId)
    if (target) {
      polyphonic.refreshPolyphonicForSub(target)
    }
  }

  // 请求局部语速
  const handleRequestLocalSpeed = (context = {}) => {
    speed.handleRequestLocalSpeed(
      context,
      state.editingSubSentenceId.value,
      state.editingForm,
      state.editorRefs
    )
  }

  // 确认局部语速
  const handleConfirmLocalSpeed = () => {
    speed.handleConfirmLocalSpeed(state.editorRefs)
  }

  // 取消局部语速
  const handleCancelLocalSpeed = () => {
    speed.handleCancelLocalSpeed()
  }

  // 自定义操作
  const handleCustomAction = (actionKey) => {
    if (actionKey === 'pause') {
      if (!state.pauseEligibilityMap[state.editingSubSentenceId.value]) return
      split.insertPauseMarker(state.editorRefs, state.editingSubSentenceId.value)
    } else if (actionKey === 'polyphonic') {
      polyphonic.togglePolyphonicMode(state.editingSubSentenceId.value)
    } else if (actionKey === 'silence') {
      split.promptSilenceDuration(state.editorRefs, state.editingSubSentenceId.value)
    } else if (actionKey === 'split-standard') {
      const rootSentence = {
        sentence_id: props.originalSentence.originalSentenceId,
        content: props.originalSentence.content
      }
      split.handleSplitStandard(rootSentence)
    } else if (actionKey === 'reading-rules') {
      handleReadingRules()
    }
  }

  // 处理阅读规范
  const handleReadingRules = async () => {
    const currentSubId = state.editingSubSentenceId.value
    if (!currentSubId) {
      ElMessage.warning('请先选择一个断句进行编辑')
      return
    }

    const currentSub = findBreakingSentenceById(currentSubId)
    if (!currentSub) {
      ElMessage.warning('未找到当前编辑的断句')
      return
    }

    // 获取编辑器实例
    const editorInstance = state.editorRefs[currentSubId]
    if (!editorInstance) {
      ElMessage.warning('编辑器未初始化')
      return
    }

    // 获取纯文本内容（去除所有标记）
    let plainText = ''
    if (typeof editorInstance.getContent === 'function') {
      const editorContent = editorInstance.getContent()
      plainText = extractPlainTextFromContent(editorContent)
    } else {
      plainText = extractPlainTextFromContent(currentSub.content || '')
    }

    if (!plainText || !plainText.trim()) {
      ElMessage.warning('当前断句内容为空，无法检测阅读规范')
      return
    }

    try {
      // 调用接口获取匹配的阅读规范
      const response = await getMatchingFieldListFromText(plainText)
      
      if (!response || !Array.isArray(response.matchingFieldList)) {
        ElMessage.warning('未找到匹配的阅读规范')
        // 如果之前有显示阅读规则标记，清除它们
        if (readingRulesStateMap[currentSubId]) {
          readingRulesStateMap[currentSubId].markers = []
          updateReadingRuleMarkers(currentSubId)
        }
        return
      }

      // 获取当前断句已应用的阅读规则
      const appliedRules = currentSub.readingRules || []
      const appliedRuleMap = new Map()
      appliedRules.forEach(rule => {
        appliedRuleMap.set(rule.pattern || rule.partern || '', rule.ruleId || rule.rule_id || '')
      })

      // 构建阅读规则标记
      const markers = response.matchingFieldList.map((field, index) => {
        const pattern = field.pattern || ''
        const ruleId = field.ruleId || field.rule_id || ''
        
        // 检查是否已应用
        const isApplied = appliedRuleMap.has(pattern) || appliedRuleMap.has(ruleId)
        
        // 在纯文本中查找匹配位置
        const patternIndex = plainText.indexOf(pattern)
        if (patternIndex === -1) {
          return null
        }

        return {
          ruleId: ruleId,
          pattern: pattern,
          offset: patternIndex,
          length: pattern.length,
          applied: isApplied
        }
      }).filter(Boolean)

      // 更新阅读规则状态
      if (!readingRulesStateMap[currentSubId]) {
        readingRulesStateMap[currentSubId] = {
          markers: [],
          appliedRules: new Set()
        }
      }

      readingRulesStateMap[currentSubId].markers = markers
      
      // 更新已应用的规则集合
      markers.forEach(marker => {
        if (marker.applied) {
          readingRulesStateMap[currentSubId].appliedRules.add(marker.ruleId || marker.pattern)
        }
      })

      // 更新编辑器中的阅读规则标记
      updateReadingRuleMarkers(currentSubId)

      if (markers.length === 0) {
        ElMessage.info('未找到匹配的阅读规范')
      } else {
        ElMessage.success(`找到 ${markers.length} 个匹配的阅读规范`)
      }
    } catch (error) {
      console.error('获取阅读规范匹配失败:', error)
      ElMessage.error('获取阅读规范匹配失败，请重试')
    }
  }

  // 更新编辑器中的阅读规则标记
  const updateReadingRuleMarkers = (subId) => {
    const editorInstance = state.editorRefs[subId]
    if (!editorInstance || typeof editorInstance.setReadingRuleMarkers !== 'function') {
      return
    }

    const readingRuleState = readingRulesStateMap[subId]
    if (!readingRuleState || !readingRuleState.markers || readingRuleState.markers.length === 0) {
      editorInstance.clearReadingRuleMarkers()
      return
    }

    // 获取当前断句的纯文本内容，用于位置转换
    const currentSub = findBreakingSentenceById(subId)
    if (!currentSub) {
      editorInstance.clearReadingRuleMarkers()
      return
    }

    // 获取编辑器内容用于计算文档位置
    let editorContent = currentSub.content || ''
    if (typeof editorInstance.getContent === 'function') {
      const content = editorInstance.getContent()
      if (content) {
        editorContent = content
      }
    }

    // 转换标记格式为编辑器需要的格式
    // 注意：这里需要将纯文本位置（offset）转换为文档位置（from/to）
    // 由于编辑器内容可能包含标记（如 <pause:0.5>），需要计算实际位置
    const plainText = extractPlainTextFromContent(editorContent)
    
    const markers = readingRuleState.markers.map(marker => {
      const pattern = marker.pattern || ''
      const patternIndex = plainText.indexOf(pattern, marker.offset)
      
      if (patternIndex === -1) {
        return null
      }

      // 使用 resolveDocRange 将纯文本位置转换为文档位置
      // 这里需要传入编辑器的 resolveDocRange 方法，但暂时使用简化方案
      // 实际位置转换会在编辑器的 updateReadingRuleMarkers 中处理
      return {
        ruleId: marker.ruleId,
        pattern: marker.pattern,
        offset: patternIndex,
        length: marker.length,
        applied: readingRuleState.appliedRules.has(marker.ruleId || marker.pattern)
      }
    }).filter(Boolean)

    // 将标记传递给编辑器，编辑器会负责位置转换
    editorInstance.setReadingRuleMarkers(markers)
  }

  // 获取阅读规则标记（用于传递给编辑器）
  const getReadingRuleMarkers = (sub) => {
    if (!sub || !sub.sentence_id) return []
    const readingRuleState = readingRulesStateMap[sub.sentence_id]
    if (!readingRuleState || !readingRuleState.markers) return []
    
    return readingRuleState.markers.map(marker => ({
      ruleId: marker.ruleId,
      pattern: marker.pattern,
      offset: marker.offset,
      length: marker.length,
      applied: readingRuleState.appliedRules.has(marker.ruleId || marker.pattern)
    }))
  }

  // 切换阅读规则的应用状态
  const toggleReadingRule = (subId, ruleId, pattern, applied) => {
    if (!readingRulesStateMap[subId]) {
      readingRulesStateMap[subId] = {
        markers: [],
        appliedRules: new Set()
      }
    }

    const readingRuleState = readingRulesStateMap[subId]
    const key = ruleId || pattern

    if (applied) {
      readingRuleState.appliedRules.add(key)
    } else {
      readingRuleState.appliedRules.delete(key)
    }

    // 更新当前断句的 readingRules
    const currentSub = findBreakingSentenceById(subId)
    if (currentSub) {
      const readingRules = []
      readingRuleState.appliedRules.forEach(key => {
        // 从 markers 中找到对应的规则信息
        const marker = readingRuleState.markers.find(m => (m.ruleId || m.pattern) === key)
        if (marker) {
          readingRules.push({
            ruleId: marker.ruleId,
            pattern: marker.pattern
          })
        }
      })
      currentSub.readingRules = readingRules
    }

    // 更新编辑器标记
    updateReadingRuleMarkers(subId)
  }

  // 取消阅读规则提示框隐藏
  const cancelReadingRuleTooltipHide = () => {
    if (readingRuleTooltipTimer) {
      clearTimeout(readingRuleTooltipTimer)
      readingRuleTooltipTimer = null
    }
  }

  // 隐藏阅读规则提示框
  const hideReadingRuleTooltip = () => {
    cancelReadingRuleTooltipHide()
    readingRuleTooltip.visible = false
    readingRuleTooltip.ruleId = ''
    readingRuleTooltip.sentenceId = null
    readingRuleTooltip.pattern = ''
    readingRuleTooltip.applied = false
    isReadingRuleTooltipHovering.value = false
  }

  // 安排提示框隐藏
  const scheduleReadingRuleTooltipHide = () => {
    if (isReadingRuleTooltipHovering.value) return
    cancelReadingRuleTooltipHide()
    readingRuleTooltipTimer = setTimeout(() => {
      if (!isReadingRuleTooltipHovering.value) {
        hideReadingRuleTooltip()
      }
    }, 250)
  }

  // 显示阅读规则提示框
  const showReadingRuleTooltip = (sub, payload) => {
    if (!sub || !payload) {
      scheduleReadingRuleTooltipHide()
      return
    }
    
    cancelReadingRuleTooltipHide()
    isReadingRuleTooltipHovering.value = false
    
    const position = payload.position || { x: 0, y: 0 }
    readingRuleTooltip.visible = true
    readingRuleTooltip.sentenceId = sub.sentence_id
    readingRuleTooltip.ruleId = payload.ruleId || ''
    readingRuleTooltip.pattern = payload.pattern || ''
    readingRuleTooltip.applied = payload.applied !== false
    readingRuleTooltip.position = {
      x: position.left || position.x || 0,
      y: position.top || position.y || 0
    }
  }

  // 处理阅读规则悬停事件
  const handleReadingRuleHover = (sub, payload) => {
    if (!payload) {
      scheduleReadingRuleTooltipHide()
      return
    }
    
    showReadingRuleTooltip(sub, payload)
  }

  // 处理阅读规则提示框鼠标进入
  const handleReadingRuleTooltipMouseEnter = () => {
    isReadingRuleTooltipHovering.value = true
    cancelReadingRuleTooltipHide()
  }

  // 处理阅读规则提示框鼠标离开
  const handleReadingRuleTooltipMouseLeave = () => {
    isReadingRuleTooltipHovering.value = false
    scheduleReadingRuleTooltipHide()
  }

  // 处理阅读规则选择
  const handleReadingRuleSelect = (applied) => {
    if (!readingRuleTooltip.sentenceId || !readingRuleTooltip.ruleId) {
      return
    }
    
    toggleReadingRule(
      readingRuleTooltip.sentenceId,
      readingRuleTooltip.ruleId,
      readingRuleTooltip.pattern,
      applied
    )
    
    hideReadingRuleTooltip()
  }

  // 标志：是否正在执行拆分操作（防止循环更新）
  const isSplitting = ref(false)

  // 断句标准确认
  const handleSplitStandardConfirm = () => {
    split.handleSplitStandardConfirm(
      (rootSentence, originalText) => {
        isSplitting.value = true
        try {
          const newSentence = operations.splitByPunctuation(rootSentence, originalText, insertAfterLocal)
          if (newSentence && newSentence.sentence_id) {
            polyphonic.ensurePolyphonicState(newSentence.sentence_id)
            selectSubSentence(newSentence)
            ElMessage.success('已按大符号重置：清空所有输入文本，并将父句复制为输入文本1')
          }
        } finally {
          // 延迟重置标志，确保响应式更新完成
          nextTick(() => {
            setTimeout(() => {
              isSplitting.value = false
            }, 100)
          })
        }
      },
      (rootSentence, originalText, charCount) => {
        isSplitting.value = true
        try {
          // 使用 nextTick 延迟执行，避免在响应式更新过程中触发循环
          nextTick(() => {
            try {
              const newSentences = operations.splitByCharCount(rootSentence, originalText, charCount)
              
              // 确保 internalBreakingSentences 已更新（splitByCharCount 已经修改了它）
              // 但我们需要等待响应式更新完成
              nextTick(() => {
                newSentences.forEach(newSentence => {
                  if (newSentence && newSentence.sentence_id) {
                    polyphonic.ensurePolyphonicState(newSentence.sentence_id)
                  }
                })
                if (newSentences.length > 0) {
                  selectSubSentence(newSentences[0])
                }
                ElMessage.success(`已按 ${charCount} 个字符拆分为 ${newSentences.length} 句`)
                
                // 先等待 watch 完成，然后再重置标志
                // 使用更长的延迟，确保所有响应式更新都完成
                setTimeout(() => {
                  isSplitting.value = false
                }, 200) // 增加延迟时间到 200ms
              })
            } catch (error) {
              console.error('[useSentenceEditor] 按字符数拆分失败:', error)
              ElMessage.error(error.message || '拆分失败')
              isSplitting.value = false
            }
          })
        } catch (error) {
          console.error('[useSentenceEditor] 按字符数拆分失败:', error)
          ElMessage.error(error.message || '拆分失败')
          isSplitting.value = false
        }
      }
    )
  }

  // 断句标准对话框关闭
  const handleSplitStandardDialogClose = () => {
    split.handleSplitStandardDialogClose()
  }

  // 插入断句
  const handleInsertAfter = async (sentenceId) => {
    const newSentence = await split.handleInsertAfter(sentenceId, (sentenceId, content) => {
      return operations.insertAfter(sentenceId, content)
    })
    if (newSentence && newSentence.sentence_id) {
      polyphonic.ensurePolyphonicState(newSentence.sentence_id)
      selectSubSentence(newSentence)
    }
  }

  // 删除断句
  const handleDelete = async (sentenceId) => {
    const sentence = findBreakingSentenceById(sentenceId)
    const isBreakingSentence = sentence && sentence.parent_id && sentence.parent_id !== 0
    await split.handleDelete(
      sentenceId,
      isBreakingSentence,
      (id) => {
        const index = internalBreakingSentences.value.findIndex(s => s.sentence_id === id)
        if (index !== -1) {
          internalBreakingSentences.value.splice(index, 1)
        }
      },
      async (id) => {
        // 调用删除接口
        const { deleteBreakingSentenceApi } = await import('./useSentenceEditorApi')
        const api = useSentenceEditorApi()
        await api.deleteBreakingSentenceApi(id)
      }
    )
  }

  // 清空文本
  const handleClearText = async (rootSentence) => {
    await split.handleClearText(rootSentence, async (rootSentence) => {
      rootSentence.content = ''
      const children = internalBreakingSentences.value
        .filter((item) => item.parent_id === rootSentence.sentence_id)
        .sort((a, b) => (a.display_order || 0) - (b.display_order || 0))

      const [keeper, ...toDelete] = children

      for (const child of toDelete) {
        const index = internalBreakingSentences.value.findIndex(
          (item) => item.sentence_id === child.sentence_id
        )
        if (index !== -1) {
          internalBreakingSentences.value.splice(index, 1)
        }
      }

      let target = keeper

      if (target) {
        target.content = ''
        target.speedSegments = []
        state.editingForm.content = ''
        polyphonic.refreshPolyphonicForSub(target)
      } else {
        // 使用 insertAfterLocal 创建新句子（因为这是清空操作，不是从断句插入）
        const newSentence = insertAfterLocal(rootSentence.sentence_id, {
          content: '',
          parent_id: rootSentence.sentence_id
        })
        if (newSentence && newSentence.sentence_id) {
          polyphonic.ensurePolyphonicState(newSentence.sentence_id)
          newSentence.speedSegments = []
          target = newSentence
        }
      }

      if (target) {
        state.editingForm.content = ''
        state.editingSubSentenceId.value = target.sentence_id
        selectSubSentence(target)
      }
    })
  }

  // 选择音色
  const selectVoice = (voice) => {
    state.editingForm.voice = voice
  }

  // 全部合成
  const handleSynthesizeAllBreakingSentences = async () => {
    if (!props.originalSentence) return
    await synthesis.handleSynthesizeAllBreakingSentences(props.originalSentence.originalSentenceId)
  }

  // 合成断句
  const handleResynthesizeBreakingSentence = async (breakingSentenceId) => {
    await synthesis.handleResynthesizeBreakingSentence(breakingSentenceId)
  }

  // 播放
  const handlePlay = (sentence) => {
    playback.handlePlay(
      sentence,
      synthesis.breakingSentenceStatus.value,
      props.originalSentence
    )
  }

  // 保存
  const handleSave = async () => {
    if (!props.originalSentence) {
      ElMessage.warning('缺少拆句数据')
      return
    }
    try {
      await save.handleSave({
        originalSentenceId: props.originalSentence.originalSentenceId,
        taskId: props.taskId,
        breakingSentences: internalBreakingSentences.value,
        editingForm: state.editingForm,
        editorRefs: state.editorRefs,
        polyphonicStateMap: polyphonic.polyphonicStateMap
      })

      // 触发保存成功事件
      emit('saved')
      emit('refresh')
    } catch (error) {
      console.error('保存失败:', error)
    }
  }

  // 取消
  const handleCancel = async () => {
    const result = await save.handleCancel(
      internalBreakingSentences.value,
      () => {
        internalExpanded.value = false
        emit('update:expanded', false)
      }
    )

    if (result === 'save') {
      await handleSave()
      internalExpanded.value = false
      emit('update:expanded', false)
    } else if (result === 'cancel') {
      // 恢复备份
      const restored = save.restoreFromBackup()
      if (restored) {
        internalBreakingSentences.value = restored
      }
      internalExpanded.value = false
      emit('update:expanded', false)
    }
  }

  // 检查未保存修改
  const hasUnsavedChanges = () => {
    return save.hasUnsavedChanges(internalBreakingSentences.value, save.backupData.value)
  }

  // 获取断句合成状态
  const getBreakingSentenceSynthesisStatus = (breakingSentenceId) => {
    return synthesis.getBreakingSentenceSynthesisStatus(breakingSentenceId, props.originalSentence)
  }

  // 获取多音字标记
  const getPolyphonicMarkers = (sub) => {
    return polyphonic.getPolyphonicMarkers(sub)
  }

  // 检查多音字模式是否激活
  const isPolyphonicModeActive = (sub) => {
    return polyphonic.isPolyphonicModeActive(sub)
  }

  // 设置编辑器引用
  const setEditorRef = (id, instance) => {
    state.setEditorRef(id, instance)
  }

  // 计算属性
  const currentSelectionContext = computed(() => {
    if (!state.editingSubSentenceId.value) return null
    return state.selectionStateMap[state.editingSubSentenceId.value] || null
  })

  return {
    // 状态
    internalExpanded,
    editingForm: state.editingForm,
    editingSubSentenceId: state.editingSubSentenceId,
    internalBreakingSentences,
    polyphonicStateMap: polyphonic.polyphonicStateMap,
    polyphonicModeMap: polyphonic.polyphonicModeMap,
    editorRefs: state.editorRefs,
    selectionStateMap: state.selectionStateMap,
    breakingSentenceStatus: synthesis.breakingSentenceStatus,
    backupData: save.backupData,
    saving: save.saving,
    localSpeedDialog: speed.localSpeedDialog,
    splitStandardDialogVisible: split.splitStandardDialogVisible,
    splitStandardType: split.splitStandardType,
    splitStandardCharCount: split.splitStandardCharCount,
    polyphonicTooltip: polyphonic.polyphonicTooltip,
    readingRuleTooltip,
    currentSelectionContext,
    currentContent,
    isSplitting,
    // 方法
    selectSubSentence,
    setEditorRef,
    getPolyphonicMarkers,
    isPolyphonicModeActive,
    handleEditorSelectionChange,
    handleEditorContentChange,
    handleEditorFocus,
    handleSpeedSegmentsChange,
    handlePolyphonicHover,
    handlePolyphonicOptionSelect,
    handleRequestLocalSpeed,
    handleConfirmLocalSpeed,
    handleCancelLocalSpeed,
    handleCustomAction,
    handleSplitStandard: () => {
      const rootSentence = {
        sentence_id: props.originalSentence.originalSentenceId,
        content: props.originalSentence.content
      }
      split.handleSplitStandard(rootSentence)
    },
    handleSplitStandardConfirm,
    handleSplitStandardDialogClose,
    handleInsertAfter,
    handleDelete,
    handleSynthesizeAllBreakingSentences,
    handleResynthesizeBreakingSentence,
    handlePlay,
    handleClearText,
    selectVoice,
    handleSave,
    handleCancel,
    hasUnsavedChanges,
    getBreakingSentenceSynthesisStatus,
    handleTooltipMouseEnter: polyphonic.handleTooltipMouseEnter,
    handleTooltipMouseLeave: polyphonic.handleTooltipMouseLeave,
    getReadingRuleMarkers,
    toggleReadingRule,
    handleReadingRuleHover,
    handleReadingRuleTooltipMouseEnter,
    handleReadingRuleTooltipMouseLeave,
    handleReadingRuleSelect
  }
}

