import request from './request'
import {
  mockCreateTask,
  mockGetTask,
  mockGetTaskSentences,
  mockMergeAudio
} from '@/mock'

const useMock = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'

// 创建任务并自动拆句
export function createTask(data) {
  if (useMock) {
    return mockCreateTask(data)
  }
  return request({
    url: '/tasks',
    method: 'post',
    data
  })
}

// 获取任务详情
export function getTask(taskId) {
  if (useMock) {
    return mockGetTask(taskId)
  }
  return request({
    url: `/tasks/${taskId}`,
    method: 'get'
  })
}

// 获取任务句子列表
export function getTaskSentences(taskId) {
  if (useMock) {
    return mockGetTaskSentences(taskId)
  }
  return request({
    url: `/tasks/${taskId}/sentences`,
    method: 'get'
  })
}

// 合并音频
export function mergeAudio(taskId) {
  if (useMock) {
    return mockMergeAudio(taskId)
  }
  return request({
    url: `/tasks/${taskId}/audio/merge`,
    method: 'post'
  })
}

