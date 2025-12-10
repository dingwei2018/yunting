import {
  setConfig,
  synthesizeOriginalSentence,
  synthesizeBreakingSentence,
  getOriginalSentenceStatus,
  getBreakingSentenceStatus
} from '@/api/synthesis'
import { deleteBreakingSentence } from '@/api/breakingSentence'

/**
 * 编辑区域 API 调用 composable
 */
export function useSentenceEditorApi() {
  /**
   * 保存配置
   * @param {number} taskId - 任务ID
   * @param {number} originalSentenceId - 拆句ID
   * @param {Array} breakingSentenceList - 断句列表
   * @returns {Promise}
   */
  const saveConfig = async (taskId, originalSentenceId, breakingSentenceList) => {
    return await setConfig({
      taskId: parseInt(taskId),
      originalSentenceId: parseInt(originalSentenceId),
      breakingSentenceList
    })
  }

  /**
   * 合成音频
   * @param {string} type - 'original' | 'breaking'
   * @param {number|string} id - 拆句ID或断句ID
   * @returns {Promise}
   */
  const synthesize = async (type, id) => {
    if (type === 'original') {
      return await synthesizeOriginalSentence({ originalSentenceId: String(id) })
    } else if (type === 'breaking') {
      return await synthesizeBreakingSentence({ breakingSentenceId: id })
    }
    throw new Error(`Unknown synthesis type: ${type}`)
  }

  /**
   * 获取合成状态
   * @param {string} type - 'original' | 'breaking'
   * @param {number|string} id - 拆句ID或断句ID
   * @returns {Promise}
   */
  const getSynthesisStatus = async (type, id) => {
    if (type === 'original') {
      return await getOriginalSentenceStatus(id)
    } else if (type === 'breaking') {
      return await getBreakingSentenceStatus(id)
    }
    throw new Error(`Unknown synthesis type: ${type}`)
  }

  /**
   * 删除断句
   * @param {number|string} breakingSentenceId - 断句ID
   * @returns {Promise}
   */
  const deleteBreakingSentenceApi = async (breakingSentenceId) => {
    return await deleteBreakingSentence(breakingSentenceId)
  }

  return {
    saveConfig,
    synthesize,
    getSynthesisStatus,
    deleteBreakingSentenceApi
  }
}

