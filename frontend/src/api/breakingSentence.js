import request from './request'
import { mockGetOriginalSentenceList } from '@/mock'

const useMock = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'

/**
 * 获取拆句列表
 * @param {number} taskId - 任务ID
 * @param {number} page - 页码
 * @param {number} pageSize - 每页数量
 * @returns {Promise}
 */
export function getOriginalSentenceList(taskId, page, pageSize) {
  if (useMock) {
    return mockGetOriginalSentenceList(taskId, page, pageSize)
  }
  let url = `/api/originalSentence/getOriginalSentenceList?taskid=${taskId}`
  if (page !== undefined) {
    url += `&page=${page}`
  }
  if (pageSize !== undefined) {
    url += `&page_size=${pageSize}`
  }
  return request({
    url,
    method: 'get'
  })
}

/**
 * 删除断句
 * 删除断句后级联删除所有关于断句的合成参数
 * @param {number} breakingSentenceId - 断句ID
 * @returns {Promise}
 */
export function deleteBreakingSentence(breakingSentenceId) {
  return request({
    url: `/api/breakingSentence/delete?breakingSentenceId=${breakingSentenceId}`,
    method: 'post'
  })
}

/**
 * 删除拆句
 * 删除拆句下所有断句以及断句所属的所有配置
 * @param {number} originalSentenceId - 拆句ID
 * @returns {Promise}
 */
export function deleteOriginalSentence(originalSentenceId) {
  if (useMock) {
    // TODO: 实现 mock 删除拆句
    return Promise.resolve({ code: 200, message: 'success', data: '' })
  }
  return request({
    url: `/api/originalSentence/delete?originalSentenceId=${originalSentenceId}`,
    method: 'post'
  })
}

