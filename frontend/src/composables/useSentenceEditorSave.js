import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSentenceEditorApi } from './useSentenceEditorApi'
import { convertBreakingSentenceToApi, parseAllMarkersFromContent, extractPlainTextFromContent, detectMarkerNesting } from '@/models/sentenceModels'

/**
 * 保存功能 composable
 */
export function useSentenceEditorSave() {
  const { saveConfig } = useSentenceEditorApi()

  const saving = ref(false)
  const backupData = ref(null)

  /**
   * 创建数据备份
   */
  const createBackup = (internalBreakingSentences) => {
    if (internalBreakingSentences) {
      backupData.value = JSON.parse(JSON.stringify(internalBreakingSentences))
    }
  }

  /**
   * 从备份恢复数据
   */
  const restoreFromBackup = () => {
    if (!backupData.value) {
      console.warn('没有备份数据可恢复')
      return null
    }
    return JSON.parse(JSON.stringify(backupData.value))
  }

  /**
   * 检查是否有未保存的修改
   */
  const hasUnsavedChanges = (currentBreakingSentences, backupBreakingSentences) => {
    if (!backupBreakingSentences || !currentBreakingSentences) {
      return false
    }

    // 比较数量
    if (currentBreakingSentences.length !== backupBreakingSentences.length) {
      return true
    }

    // 比较每个断句的数据
    for (const current of currentBreakingSentences) {
      const backup = backupBreakingSentences.find(b => b.sentence_id === current.sentence_id)

      if (!backup) {
        return true // 新增了断句
      }

      // 比较关键字段
      if (current.content !== backup.content ||
          current.voice !== backup.voice ||
          current.volume !== backup.volume ||
          current.speed !== backup.speed) {
        return true
      }

      // 比较 pauseMarkers
      const currentPauseMarkers = JSON.stringify((current.pauseMarkers || []).sort((a, b) => a.location - b.location))
      const backupPauseMarkers = JSON.stringify((backup.pauseMarkers || []).sort((a, b) => a.location - b.location))
      if (currentPauseMarkers !== backupPauseMarkers) {
        return true
      }

      // 比较 polyphonicOverrides
      const currentPolyphonic = JSON.stringify((current.polyphonicOverrides || []).sort((a, b) => a.begin - b.begin))
      const backupPolyphonic = JSON.stringify((backup.polyphonicOverrides || []).sort((a, b) => a.begin - b.begin))
      if (currentPolyphonic !== backupPolyphonic) {
        return true
      }

      // 比较 speedSegments
      const currentSpeedSegments = JSON.stringify((current.speedSegments || []).sort((a, b) => a.begin - b.begin))
      const backupSpeedSegments = JSON.stringify((backup.speedSegments || []).sort((a, b) => a.begin - b.begin))
      if (currentSpeedSegments !== backupSpeedSegments) {
        return true
      }
    }

    // 检查是否有断句被删除
    for (const backup of backupBreakingSentences) {
      const current = currentBreakingSentences.find(c => c.sentence_id === backup.sentence_id)
      if (!current) {
        return true // 删除了断句
      }
    }

    return false
  }

  /**
   * 保存当前拆句的修改
   */
  const handleSave = async ({
    originalSentenceId,
    taskId,
    breakingSentences,
    editingForm,
    editorRefs,
    polyphonicStateMap
  }) => {
    if (!originalSentenceId) {
      ElMessage.warning('请先选择一个拆句进行编辑')
      return
    }

    if (breakingSentences.length === 0) {
      ElMessage.warning('该拆句下没有断句')
      return
    }

    saving.value = true

    try {
      // 将前端断句数据转换为接口参数格式
      const breakingSentenceList = breakingSentences.map(breakingSentence => {
        const isCurrentEditing = breakingSentence.sentence_id === editingForm.sentenceId

        // 从编辑器实例获取内容用于解析所有标记
        let editorContentForParsing = breakingSentence.content || ''
        const editorInstance = editorRefs[breakingSentence.sentence_id]
        if (editorInstance && typeof editorInstance.getContent === 'function') {
          const editorContent = editorInstance.getContent()
          if (editorContent) {
            editorContentForParsing = editorContent
          }
        }

        // 从编辑器内容中解析所有标记（停顿、静音）
        const { pauseMarkers, silenceMarkers } = parseAllMarkersFromContent(editorContentForParsing)
        
        // content 字段使用纯文本（去除所有标记），避免保存时重复
        const content = extractPlainTextFromContent(editorContentForParsing)

        // 从编辑器状态获取多音字标记
        let polyphonicOverrides = breakingSentence.polyphonicOverrides || []
        if (polyphonicStateMap && polyphonicStateMap[breakingSentence.sentence_id]) {
          const state = polyphonicStateMap[breakingSentence.sentence_id]
          const markers = state.markers || []
          const selections = state.selections || {}

          polyphonicOverrides = markers
            .filter(marker => selections[marker.id])
            .map(marker => {
              const selectedPh = selections[marker.id] || ''
              return {
                begin: marker.offset || 0,
                end: (marker.offset || 0) + (marker.length || 1),
                ph: selectedPh,
                alphabet: marker.alphabet || ''
              }
            })
        }

        // 从 breakingSentence 获取局部变速标记
        const speedSegments = breakingSentence.speedSegments || []

        // 打印调试信息
        console.log('[保存时标记信息]', {
          sentenceId: breakingSentence.sentence_id,
          editorContentLength: editorContentForParsing?.length || 0,
          plainTextContent: content,
          plainTextLength: content?.length || 0,
          pauseMarkers: pauseMarkers,
          silenceMarkers: silenceMarkers,
          polyphonicOverrides: polyphonicOverrides,
          speedSegments: speedSegments
        })

        // 检测标签嵌套
        const nestingResult = detectMarkerNesting({
          pauseMarkers,
          silenceMarkers,
          polyphonicOverrides,
          speedSegments
        })
        
        if (nestingResult.hasNesting) {
          throw new Error(nestingResult.message)
        }

        let mergedData = {
          ...breakingSentence,
          content: content,
          pauseMarkers: pauseMarkers,
          silenceMarkers: silenceMarkers,
          polyphonicOverrides: polyphonicOverrides,
          speedSegments: speedSegments
        }

        // 如果当前正在编辑这个断句，使用编辑表单的其他数据
        if (isCurrentEditing) {
          mergedData = {
            ...mergedData,
            voice: editingForm.voice,
            volume: editingForm.volume,
            speed: editingForm.speed
          }
        }

        return convertBreakingSentenceToApi(mergedData)
      })

      // 调用保存接口
      await saveConfig(taskId, originalSentenceId, breakingSentenceList)

      // 更新备份
      createBackup(breakingSentences)

      ElMessage.success('保存成功')

      return {
        success: true,
        breakingSentenceList
      }
    } catch (error) {
      console.error('保存失败:', error)
      // 如果是标签嵌套错误，显示特定提示
      if (error.message === '华为云暂时不支持标签嵌套') {
        ElMessage.error('华为云暂时不支持标签嵌套')
      } else {
      ElMessage.error('保存失败，请重试')
      }
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * 取消编辑（检查未保存修改）
   */
  const handleCancel = async (currentBreakingSentences, onCancel) => {
    if (hasUnsavedChanges(currentBreakingSentences, backupData.value)) {
      try {
        await ElMessageBox.confirm(
          '当前有未保存的设置，是否保存？',
          '提示',
          {
            confirmButtonText: '保存',
            cancelButtonText: '取消',
            type: 'warning',
            distinguishCancelAndClose: true
          }
        )
        // 用户点击了"保存"，返回 'save' 标识
        return 'save'
      } catch (error) {
        // 用户点击了"取消"，返回 'cancel' 标识
        if (error === 'cancel') {
          return 'cancel'
        }
        return 'cancel'
      }
    } else {
      // 没有未保存的修改，直接取消
      if (onCancel) {
        onCancel()
      }
      return 'cancel'
    }
  }

  return {
    // 状态
    saving,
    backupData,
    // 方法
    handleSave,
    handleCancel,
    hasUnsavedChanges,
    createBackup,
    restoreFromBackup
  }
}

