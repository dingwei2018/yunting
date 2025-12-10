import { ref } from 'vue'
import { mergeAudio } from '@/api/merge'
import { getOriginalSentenceList } from '@/api/breakingSentence'
import {
  deleteSentence,
  synthesizeSentence,
  getSentence,
  updateSentence
} from '@/api/sentence'
import { transformOriginalSentenceList, createSubSentence } from '@/models/sentenceModels'

export const useSentencesRepository = () => {
  const loading = ref(false)
  const merging = ref(false)
  const sentences = ref([])
  const taskId = ref('')
  const currentPage = ref(0) // api.md: page 从 0 开始
  const pageSize = ref(10)
  const hasMore = ref(true)
  const loadingMore = ref(false)

  const loadSentences = async (inputTaskId = taskId.value, page = 0, pageSizeParam = 10, append = false) => {
    if (!inputTaskId) return
    taskId.value = inputTaskId
    
    // 如果是追加模式，使用 loadingMore，否则使用 loading
    if (append) {
      loadingMore.value = true
    } else {
      loading.value = true
      currentPage.value = 0 // api.md: page 从 0 开始
      hasMore.value = true
    }
    
    try {
      const data = await getOriginalSentenceList(inputTaskId, page, pageSizeParam)
      
      // 使用转换函数将新接口数据转换为现有模型
      const newSentences = transformOriginalSentenceList(data)
      
      // 关键修复：将嵌套结构扁平化，确保断句也在 sentences.value 中
      const flattenedSentences = newSentences.flatMap(root => [root, ...(root.children || [])])
      
      if (append) {
        // 追加模式：将新数据追加到现有数据
        sentences.value = [...sentences.value, ...flattenedSentences]
      } else {
        // 替换模式：替换所有数据
        sentences.value = flattenedSentences
      }
      
      // 更新分页信息
      // 响应拦截器返回的是 res.data，所以 data 直接就是 { list, total, page, pageSize }
      currentPage.value = data?.page ?? page
      pageSize.value = data?.pageSize ?? pageSizeParam
      
      // 判断是否还有更多数据
      const total = data?.total ?? 0
      const currentCount = sentences.value.length
      hasMore.value = currentCount < total
      
      // 返回原始数据，用于获取 synthesisStatus
      return data
    } finally {
      if (append) {
        loadingMore.value = false
      } else {
        loading.value = false
      }
    }
  }

  const loadMoreSentences = async () => {
    if (!hasMore.value || loadingMore.value || loading.value) {
      return null
    }
    const nextPage = currentPage.value + 1
    const data = await loadSentences(taskId.value, nextPage, pageSize.value, true)
    return data
  }

  const handleMergeAudio = async () => {
    if (!taskId.value) {
      console.error('handleMergeAudio: taskId 为空', { taskId: taskId.value })
      throw new Error('taskId 不能为空')
    }
    
    merging.value = true
    try {
      // 调用合并音频接口，返回的数据中包含 mergeId
      // 确保 taskId 是数字类型
      const taskIdNum = parseInt(taskId.value)
      if (isNaN(taskIdNum)) {
        console.error('handleMergeAudio: taskId 不是有效数字', { taskId: taskId.value })
        throw new Error('taskId 必须是有效数字')
      }
      
      const result = await mergeAudio(taskIdNum)
      await loadSentences()
      // 返回结果，以便调用方可以获取 mergeId
      return result
    } finally {
      merging.value = false
    }
  }

  // 统一的 ID 生成器：为新增的断句生成唯一的负数 ID（-1, -2, -3...）
  // 确保新增 ID 的唯一性，排序只依赖于 display_order，不依赖于 ID
  const generateNewSentenceId = () => {
    // 找到所有负数 ID（新增的断句）
    const existingNegativeIds = sentences.value
      .filter(item => typeof item.sentence_id === 'number' && item.sentence_id < 0)
      .map(item => item.sentence_id)
    
    // 如果没有任何负数 ID，从 -1 开始
    if (existingNegativeIds.length === 0) {
      return -1
    }
    
    // 找到最小的负数 ID（最接近 0 的负数）
    const minNegativeId = Math.min(...existingNegativeIds)
    
    // 返回比最小负数 ID 小 1 的 ID（更小的负数）
    return minNegativeId - 1
  }

  const insertAfter = (sentenceId, payload = {}) => {
    if (!sentenceId) return null
    // 使用统一的 ID 生成器，确保新增 ID 的唯一性（-1, -2, -3...）
    // 排序只依赖于 display_order，不依赖于 ID
    const tempId = generateNewSentenceId()
    
    // parent_id 一定会在 payload 中指定，直接使用
    // 如果没有提供 parent_id，抛出错误
    if (payload.parent_id === undefined || payload.parent_id === null) {
      console.error('insertAfter: parent_id 未提供', { sentenceId, payload })
      throw new Error('parent_id 必须提供')
    }
    const parentId = payload.parent_id
    
    // 找到当前断句在 sentences.value 中的位置
    const index = sentences.value.findIndex((item) => item.sentence_id === sentenceId)
    if (index === -1) {
      // 如果找不到，直接添加到末尾
      // 注意：先展开 payload，然后用明确的值覆盖，确保 parent_id 不会被 payload 中的 undefined 覆盖
      const normalized = createSubSentence({
        ...payload,
        sentence_id: tempId,
        parent_id: parentId, // 明确设置，覆盖 payload 中可能存在的 parent_id
        content: payload.content || '',
        display_order: 0
      })
      sentences.value.push(normalized)
      return normalized
    }
    
    // 在当前断句的下方插入新断句
    // 如果传入了 display_order，使用传入的值；否则使用临时值 0，后面会重新整理
    // 注意：先展开 payload，然后用明确的值覆盖，确保 parent_id 不会被 payload 中可能存在的 undefined 覆盖
    const normalized = createSubSentence({
      ...payload,
      sentence_id: tempId,
      parent_id: parentId, // 明确设置，覆盖 payload 中可能存在的 parent_id
      content: payload.content || '',
      display_order: payload.display_order !== undefined ? payload.display_order : 0
    })
    sentences.value.splice(index + 1, 0, normalized)
    
    // 如果传入了 display_order，不重新整理（由调用方负责整理）
    // 如果没有传入 display_order，重新整理所有相同 parent_id 的断句的 display_order
    if (payload.display_order === undefined) {
      // 找到所有相同 parent_id 的断句
      const breakingSentences = sentences.value.filter(
        item => item.parent_id === parentId && item.parent_id !== 0
      )
      
      // 按照在 sentences.value 中的顺序重新分配 display_order
      // 从 0 开始，连续递增
      breakingSentences.forEach((sentence, orderIndex) => {
        const sentenceIndex = sentences.value.findIndex(
          item => item.sentence_id === sentence.sentence_id
        )
        if (sentenceIndex !== -1) {
          sentences.value[sentenceIndex].display_order = orderIndex
        }
      })
    }
    
    return normalized
  }

  return {
    loading,
    merging,
    sentences,
    taskId,
    currentPage,
    pageSize,
    hasMore,
    loadingMore,
    loadSentences,
    loadMoreSentences,
    handleMergeAudio,
    insertAfter,
    deleteSentence,
    synthesizeSentence,
    getSentence,
    updateSentence
  }
}

