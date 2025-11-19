import request from './request'

// 获取句子详情
export function getSentence(sentenceId) {
  return request({
    url: `/sentences/${sentenceId}`,
    method: 'get'
  })
}

// 更新句子
export function updateSentence(sentenceId, data) {
  return request({
    url: `/sentences/${sentenceId}`,
    method: 'put',
    data
  })
}

// 删除句子
export function deleteSentence(sentenceId) {
  return request({
    url: `/sentences/${sentenceId}`,
    method: 'delete'
  })
}

// 合成音频
export function synthesizeSentence(sentenceId) {
  return request({
    url: `/sentences/${sentenceId}/synthesize`,
    method: 'post'
  })
}

// 向下插入句子
export function insertSentenceAfter(sentenceId, data) {
  return request({
    url: `/sentences/${sentenceId}/insert`,
    method: 'post',
    data
  })
}

