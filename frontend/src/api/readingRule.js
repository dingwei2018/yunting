import request from './request'

/**
 * 获取文本中符合规则字段
 * @param {string} text - 文本内容
 * @returns {Promise}
 */
export function getMatchingFieldListFromText(text) {
  return request({
    url: '/api/readingRule/getMatchingFieldListFromText',
    method: 'get',
    params: {
      text: text || ''
    }
  })
}

/**
 * 创建阅读规范
 * @param {Object} data - 请求参数
 * @param {string} data.pattern - 原始词
 * @param {string} data.ruleType - 规范类型
 * @param {string} data.ruleValue - 自定义读法
 * @returns {Promise}
 */
export function createReadingRule(data) {
  return request({
    url: '/api/readingRule/create',
    method: 'post',
    data
  })
}

/**
 * 获取阅读规范列表
 * @param {Object} params - 请求参数
 * @param {number} params.task_id - 任务ID（可选）
 * @param {number} params.scope - 范围（可选）
 * @param {number} params.ruleType - 规则类型（可选）
 * @param {number} params.page - 页码（必填）
 * @param {number} params.pageSize - 每页数量（必填）
 * @returns {Promise}
 */
export function getReadingRuleList(params) {
  return request({
    url: '/api/readingRule/getList',
    method: 'get',
    params
  })
}

/**
 * 开关全局阅读规范
 * @param {Object} data - 请求参数
 * @param {number} data.taskId - 任务ID
 * @param {number} data.ruleId - 规则ID
 * @param {number} data.status - 状态（0-关闭，1-打开）
 * @returns {Promise}
 */
export function setGlobalSetting(data) {
  return request({
    url: '/api/readingRule/setGlobalSetting',
    method: 'post',
    data
  })
}

