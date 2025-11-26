import { ref } from 'vue'
import { getTaskSentences, mergeAudio } from '@/api/task'
import {
  deleteSentence,
  synthesizeSentence,
  getSentence,
  updateSentence
} from '@/api/sentence'
import { createRootSentence, createSubSentence } from '@/models/sentenceModels'

export const useSentencesRepository = () => {
  const loading = ref(false)
  const merging = ref(false)
  const sentences = ref([])
  const taskId = ref('')

  const normalizeList = (list = []) => {
    return list.map((item) => {
      if (item.parent_id && item.parent_id !== 0) {
        return createSubSentence(item)
      }
      const children = list
        .filter((sub) => sub.parent_id === item.sentence_id)
        .map((sub) => createSubSentence(sub))
      return createRootSentence({ ...item, children })
    })
  }

  const loadSentences = async (inputTaskId = taskId.value) => {
    if (!inputTaskId) return
    taskId.value = inputTaskId
    loading.value = true
    try {
      const data = await getTaskSentences(taskId.value)
      sentences.value = normalizeList(data.sentences || [])
    } finally {
      loading.value = false
    }
  }

  const handleMergeAudio = async () => {
    if (!taskId.value) return
    merging.value = true
    try {
      await mergeAudio(taskId.value)
      await loadSentences()
    } finally {
      merging.value = false
    }
  }

  const insertAfter = (sentenceId, payload = {}) => {
    if (!sentenceId) return null
    const tempId = `local-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
    const normalized = createSubSentence({
      sentence_id: tempId,
      parent_id: payload.parent_id ?? sentenceId,
      content: payload.content || '',
      display_order: payload.display_order ?? 0,
      ...payload
    })
    const index = sentences.value.findIndex((item) => item.sentence_id === sentenceId)
    if (index !== -1) {
      sentences.value.splice(index + 1, 0, normalized)
    } else {
      sentences.value.push(normalized)
    }
    return normalized
  }

  return {
    loading,
    merging,
    sentences,
    taskId,
    loadSentences,
    handleMergeAudio,
    insertAfter,
    deleteSentence,
    synthesizeSentence,
    getSentence,
    updateSentence
  }
}

