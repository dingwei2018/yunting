import request from './request'

// 创建任务并自动拆句
export function createTask(data) {
  return request({
    url: '/tasks',
    method: 'post',
    data
  })
}

// 获取任务详情
export function getTask(taskId) {
  return request({
    url: `/tasks/${taskId}`,
    method: 'get'
  })
}

// 获取任务句子列表
export function getTaskSentences(taskId) {
  return request({
    url: `/tasks/${taskId}/sentences`,
    method: 'get'
  })
}

// 合并音频
export function mergeAudio(taskId) {
  return request({
    url: `/tasks/${taskId}/audio/merge`,
    method: 'post'
  })
}

