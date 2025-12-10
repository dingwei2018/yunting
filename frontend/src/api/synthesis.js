import request from './request'

/**
 * 设置拆句合成参数
 * 覆盖旧数据
 * @param {Object} data - 请求参数
 * @param {number} data.taskId - 任务ID
 * @param {number} data.originalSentenceId - 拆句ID
 * @param {Array} data.breakingSentenceList - 断句列表
 * @returns {Promise}
 */
export function setConfig(data) {
  return request({
    url: '/api/synthesis/setConfig',
    method: 'post',
    data
  })
}

/**
 * 合成断句
 * 合成或重新合成单个断句
 * @param {Object} data - 请求参数
 * @param {number} data.breakingSentenceId - 断句ID
 * @returns {Promise}
 */
export function synthesizeBreakingSentence(data) {
  return request({
    url: '/api/synthesis/breakingSentence',
    method: 'post',
    data
  })
}

/**
 * 合成拆句
 * 合成或重新合成拆句下的所有断句
 * @param {Object} data - 请求参数
 * @param {string} data.originalSentenceId - 拆句ID
 * @returns {Promise}
 */
export function synthesizeOriginalSentence(data) {
  return request({
    url: '/api/synthesis/originalSentence',
    method: 'post',
    data
  })
}

/**
 * 合成任务
 * 合成或重新合成任务下的所有断句
 * @param {Object} data - 请求参数
 * @param {number} data.taskId - 任务ID
 * @returns {Promise}
 */
export function synthesizeTask(data) {
  return request({
    url: '/api/synthesis/task',
    method: 'post',
    data
  })
}

/**
 * 获取任务合成状态
 * 给出任务下所有断句的合成进度和已完成合成的音频文件下载地址和时长
 * @param {number} taskId - 任务ID
 * @returns {Promise}
 */
export function getTaskStatus(taskId) {
  return request({
    url: `/api/synthesis/getTaskStatus?taskid=${taskId}`,
    method: 'get'
  })
}

/**
 * 获取拆句合成状态
 * 给出拆句下所有断句的合成进度和已完成合成的音频文件下载地址和时长
 * @param {number} originalSentenceId - 拆句ID
 * @returns {Promise}
 */
export function getOriginalSentenceStatus(originalSentenceId) {
  return request({
    url: `/api/synthesis/getOriginalSentenceStatus?originalSentenceId=${originalSentenceId}`,
    method: 'get'
  })
}

/**
 * 获取断句合成状态
 * 给出单句断句的合成状态和已完成合成的音频文件下载地址和时长
 * @param {number} breakingSentenceId - 断句ID
 * @returns {Promise}
 */
export function getBreakingSentenceStatus(breakingSentenceId) {
  return request({
    url: `/api/synthesis/getBreakingSentenceStatus?breakingSentenceId=${breakingSentenceId}`,
    method: 'get'
  })
}

