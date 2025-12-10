import { reactive } from 'vue'
import { clampSpeed } from '@/models/sentenceModels'

/**
 * 局部语速功能 composable
 */
export function useSentenceEditorSpeed() {
  // 局部语速对话框状态
  const localSpeedDialog = reactive({
    visible: false,
    sentenceId: '',
    docFrom: 0,
    docTo: 0,
    rangeLength: 0,
    value: 0
  })

  /**
   * 请求局部语速调整
   */
  const handleRequestLocalSpeed = (context = {}, editingSubSentenceId, editingForm, editorRefs) => {
    if (!editingSubSentenceId) return
    const range = context.selectionRange
    if (!range || range.length <= 0) return
    localSpeedDialog.visible = true
    localSpeedDialog.sentenceId = editingSubSentenceId
    localSpeedDialog.docFrom = range.docFrom
    localSpeedDialog.docTo = range.docTo
    localSpeedDialog.rangeLength = range.length
    localSpeedDialog.value = clampSpeed(editingForm.speed)
  }

  /**
   * 取消局部语速调整
   */
  const handleCancelLocalSpeed = () => {
    localSpeedDialog.visible = false
    localSpeedDialog.sentenceId = ''
    localSpeedDialog.docFrom = 0
    localSpeedDialog.docTo = 0
    localSpeedDialog.rangeLength = 0
  }

  /**
   * 确认局部语速调整
   */
  const handleConfirmLocalSpeed = (editorRefs) => {
    if (!localSpeedDialog.visible || !localSpeedDialog.sentenceId) {
      return
    }
    const editor = editorRefs[localSpeedDialog.sentenceId]
    if (editor?.applyLocalSpeedRange) {
      editor.applyLocalSpeedRange(
        localSpeedDialog.docFrom,
        localSpeedDialog.docTo,
        localSpeedDialog.value
      )
    }
    handleCancelLocalSpeed()
  }

  /**
   * 处理语速段落变化
   * 将编辑器收集的 segments（格式：{ offset, length, speed }）转换为保存格式（格式：{ begin, end, speed }）
   */
  const handleSpeedSegmentsChange = ({ sub, segments }) => {
    console.log('[处理语速段落变化] 开始处理')
    console.log('  subId:', sub?.sentence_id)
    console.log('  inputSegments:', segments)
    console.log('  inputSegmentsCount:', segments?.length || 0)
    console.log('  currentSpeedSegments:', sub?.speedSegments)
    
    if (!sub) {
      console.warn('[处理语速段落变化] sub 为空')
      return
    }
    if (!Array.isArray(segments)) {
      console.log('[处理语速段落变化] segments 不是数组，清空', {
        segments: segments
      })
      sub.speedSegments = []
      return
    }
    
    // 将 offset 和 length 转换为 begin 和 end
    sub.speedSegments = segments.map((segment, index) => {
      console.log(`[处理语速段落变化] 处理第 ${index + 1} 个段落`)
      console.log('  segment:', segment)
      console.log('  hasBegin:', typeof segment.begin === 'number')
      console.log('  hasEnd:', typeof segment.end === 'number')
      console.log('  hasOffset:', typeof segment.offset === 'number')
      console.log('  hasLength:', typeof segment.length === 'number')
      
      // 如果已经是 { begin, end, speed } 格式，直接使用
      if (typeof segment.begin === 'number' && typeof segment.end === 'number') {
        const result = {
          begin: segment.begin,
          end: segment.end,
          speed: segment.speed || 0
        }
        console.log(`[处理语速段落变化] 使用 begin/end 格式`)
        console.log('  original:', segment)
        console.log('  result:', result)
        return result
      }
      
      // 如果是 { offset, length, speed } 格式，转换为 { begin, end, speed }
      const offset = segment.offset || 0
      const length = segment.length || 0
      const result = {
        begin: offset,
        end: offset + length,
        speed: segment.speed || 0
      }
      
      console.log(`[处理语速段落变化] 转换 offset/length 为 begin/end`)
      console.log('  original:', segment)
      console.log('  offset:', offset, 'length:', length)
      console.log('  result:', result)
      
      return result
    })
    
    console.log('[处理语速段落变化] 处理完成')
    console.log('  subId:', sub.sentence_id)
    console.log('  resultSpeedSegments:', sub.speedSegments)
    console.log('  resultCount:', sub.speedSegments.length)
  }

  return {
    // 状态
    localSpeedDialog,
    // 方法
    handleRequestLocalSpeed,
    handleCancelLocalSpeed,
    handleConfirmLocalSpeed,
    handleSpeedSegmentsChange
  }
}

