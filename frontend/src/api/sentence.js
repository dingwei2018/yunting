import request from './request'
import {
  mockGetSentence,
  mockUpdateSentence,
  mockDeleteSentence,
  mockSynthesizeSentence,
  mockInsertSentenceAfter
} from '@/mock'

const useMock = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'

// 获取句子详情
export function getSentence(sentenceId) {
  if (useMock) {
    return mockGetSentence(sentenceId)
  }
  return request({
    url: `/sentences/${sentenceId}`,
    method: 'get'
  })
}

// 更新句子
export function updateSentence(sentenceId, data) {
  if (useMock) {
    return mockUpdateSentence(sentenceId, data)
  }
  return request({
    url: `/sentences/${sentenceId}`,
    method: 'put',
    data
  })
}

// 删除句子
export function deleteSentence(sentenceId) {
  if (useMock) {
    return mockDeleteSentence(sentenceId)
  }
  return request({
    url: `/sentences/${sentenceId}`,
    method: 'delete'
  })
}

// 合成音频
export function synthesizeSentence(sentenceId) {
  if (useMock) {
    return mockSynthesizeSentence(sentenceId)
  }
  return request({
    url: `/sentences/${sentenceId}/synthesize`,
    method: 'post'
  })
}

// 向下插入句子
export function insertSentenceAfter(sentenceId, data) {
  if (useMock) {
    return mockInsertSentenceAfter(sentenceId, data)
  }
  return request({
    url: `/sentences/${sentenceId}/insert`,
    method: 'post',
    data
  })
}

