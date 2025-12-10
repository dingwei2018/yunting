import { ref, reactive, computed } from 'vue'
import { clampVolume, clampSpeed } from '@/models/sentenceModels'

/**
 * 编辑区域基础状态管理 composable
 */
export function useSentenceEditorState() {
  // 编辑表单状态
  const editingForm = reactive({
    sentenceId: '',
    content: '',
    voice: 'default',
    volume: 33, // 默认值33对应接口的140（接口默认值）
    speed: 0,
    pitch: 50
  })

  // 当前编辑的断句ID
  const editingSubSentenceId = ref(null)

  // 编辑器引用
  const editorRefs = reactive({})

  // 选择状态映射
  const selectionStateMap = reactive({})

  // 停顿可用性映射
  const pauseEligibilityMap = reactive({})

  // 当前选中的断句
  const currentSubSentence = computed(() => {
    if (!editingSubSentenceId.value) return null
    // 从内部断句列表中查找
    return null // 将在主 composable 中设置
  })

  /**
   * 设置编辑器引用
   */
  const setEditorRef = (id, instance) => {
    if (instance) {
      editorRefs[id] = instance
    } else {
      delete editorRefs[id]
    }
  }

  /**
   * 更新编辑表单
   */
  const updateEditingForm = (data) => {
    Object.assign(editingForm, {
      sentenceId: data.sentenceId || '',
      content: data.content || '',
      voice: data.voice || 'default',
      volume: clampVolume(data.volume),
      speed: clampSpeed(data.speed),
      pitch: data.pitch || 50
    })
  }

  /**
   * 重置编辑表单
   */
  const resetEditingForm = () => {
    editingForm.sentenceId = ''
    editingForm.content = ''
    editingForm.voice = 'default'
    editingForm.volume = 33 // 默认值33对应接口的140（接口默认值）
    editingForm.speed = 0
    editingForm.pitch = 50
  }

  return {
    // 状态
    editingForm,
    editingSubSentenceId,
    editorRefs,
    selectionStateMap,
    pauseEligibilityMap,
    currentSubSentence,
    // 方法
    setEditorRef,
    updateEditingForm,
    resetEditingForm
  }
}

