import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSentenceEditorApi } from './useSentenceEditorApi'

/**
 * 断句操作功能 composable
 */
export function useSentenceEditorSplit() {
  const { deleteBreakingSentenceApi } = useSentenceEditorApi()

  // 断句标准对话框状态
  const splitStandardDialogVisible = ref(false)
  const splitStandardType = ref('punctuation')
  const splitStandardCharCount = ref(50)
  const splitStandardContext = ref(null)

  /**
   * 处理断句标准
   */
  const handleSplitStandard = (rootSentence) => {
    if (!rootSentence) {
      ElMessage.warning('请先选择一个句子进行编辑')
      return
    }

    splitStandardContext.value = {
      rootSentence,
      originalText: rootSentence.content || ''
    }

    splitStandardType.value = 'punctuation'
    splitStandardDialogVisible.value = true
  }

  /**
   * 关闭断句标准对话框
   */
  const handleSplitStandardDialogClose = () => {
    splitStandardDialogVisible.value = false
    splitStandardContext.value = null
    splitStandardType.value = 'punctuation'
    splitStandardCharCount.value = 50
  }

  /**
   * 确认断句标准
   */
  const handleSplitStandardConfirm = (onSplitByPunctuation, onSplitByCharCount) => {
    if (!splitStandardContext.value) {
      splitStandardDialogVisible.value = false
      return
    }

    const { rootSentence, originalText } = splitStandardContext.value
    const selectedType = splitStandardType.value

    if (selectedType === 'punctuation') {
      if (onSplitByPunctuation) {
        onSplitByPunctuation(rootSentence, originalText)
      }
      splitStandardDialogVisible.value = false
      splitStandardContext.value = null
    } else if (selectedType === 'charCount') {
      const charCount = parseInt(splitStandardCharCount.value, 10)
      if (Number.isNaN(charCount) || charCount <= 0) {
        ElMessage.error('请输入大于 0 的字符数')
        return
      }
      if (onSplitByCharCount) {
        onSplitByCharCount(rootSentence, originalText, charCount)
      }
      splitStandardDialogVisible.value = false
      splitStandardContext.value = null
    } else {
      ElMessage.warning('请选择断句方式')
    }
  }

  /**
   * 插入停顿标记
   */
  const insertPauseMarker = (editorRefs, editingSubSentenceId) => {
    if (!editingSubSentenceId) return
    const editor = editorRefs[editingSubSentenceId]
    if (editor?.insertPause) {
      editor.insertPause()
    }
  }

  /**
   * 插入静音标记
   */
  const insertSilenceMarker = (editorRefs, editingSubSentenceId, duration) => {
    if (!editingSubSentenceId) return
    const editor = editorRefs[editingSubSentenceId]
    if (editor?.insertSilence) {
      editor.insertSilence(duration)
    }
  }

  /**
   * 提示输入静音时长
   */
  const promptSilenceDuration = async (editorRefs, editingSubSentenceId) => {
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
      const formatted = normalized % 1 === 0 ? normalized.toString() : normalized.toFixed(1)
      insertSilenceMarker(editorRefs, editingSubSentenceId, formatted)
    } catch (error) {
      if (error !== 'cancel') {
        console.error('插入静音失败:', error)
      }
    }
  }

  /**
   * 向下插入断句
   */
  const handleInsertAfter = async (sentenceId, onInsertAfter) => {
    try {
      const { value } = await ElMessageBox.prompt('请输入要插入的文本', '向下插入', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入文本'
      })

      if (onInsertAfter) {
        const newSentence = onInsertAfter(sentenceId, value)
        if (newSentence && newSentence.sentence_id) {
          ElMessage.success('插入成功')
          return newSentence
        }
      }
    } catch (error) {
      if (error !== 'cancel') {
        console.error('插入失败:', error)
        ElMessage.error(error.message || '插入失败')
      }
    }
  }

  /**
   * 删除断句
   */
  const handleDelete = async (sentenceId, isBreakingSentence, onDeleteLocal, onDeleteApi) => {
    try {
      await ElMessageBox.confirm('确定要删除这个句子吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })

      if (isBreakingSentence) {
        // 断句：只在前端本地删除
        if (onDeleteLocal) {
          onDeleteLocal(sentenceId)
        }
        ElMessage.success('已删除（本地）')
      } else {
        // 拆句：调用接口删除
        if (onDeleteApi) {
          await onDeleteApi(sentenceId)
        }
        ElMessage.success('删除成功')
      }
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除失败:', error)
      }
    }
  }

  /**
   * 清空文本
   */
  const handleClearText = async (rootSentence, onClearText) => {
    if (!rootSentence) return

    if (onClearText) {
      await onClearText(rootSentence)
    }
  }

  return {
    // 状态
    splitStandardDialogVisible,
    splitStandardType,
    splitStandardCharCount,
    splitStandardContext,
    // 方法
    handleSplitStandard,
    handleSplitStandardDialogClose,
    handleSplitStandardConfirm,
    insertPauseMarker,
    insertSilenceMarker,
    promptSilenceDuration,
    handleInsertAfter,
    handleDelete,
    handleClearText
  }
}

