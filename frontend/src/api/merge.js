import request from './request'

/**
 * 合并音频
 * @param {number} taskId - 任务ID（query参数，必选）
 * @param {Object} data - 请求参数（body，可选）
 * @param {number} data.taskId - 任务ID
 * @returns {Promise}
 */
export function mergeAudio(taskId, data = {}) {
  // 确保 taskId 有效
  if (!taskId && taskId !== 0) {
    console.error('mergeAudio: taskId 无效', { taskId, type: typeof taskId })
    throw new Error('taskId 不能为空')
  }
  
  // 如果 data 中没有 taskId，添加它
  const requestData = {
    ...data,
    taskId: data.taskId !== undefined ? data.taskId : taskId
  }
  
  console.log('🔍 [mergeAudio] 调用参数', {
    taskId,
    urlTaskId: taskId,
    bodyTaskId: requestData.taskId,
    requestData
  })
  
  return request({
    url: `/api/merge/audio?taskid=${taskId}`,
    method: 'post',
    data: requestData
  })
}

/**
 * 获取合并状态
 * @param {number} mergeId - 合并ID
 * @returns {Promise}
 */
export function getMergeStatus(mergeId) {
  return request({
    url: `/api/merge/getStatus?mergeId=${mergeId}`,
    method: 'get'
  })
}

