import request from './request'
import { mockGetVoiceList } from '@/mock'

const useMock = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'

/**
 * 获取音色列表
 * @returns {Promise}
 */
export function getVoiceList() {
  if (useMock) {
    return mockGetVoiceList()
  }
  return request({
    url: '/api/voice/getList',
    method: 'get'
  })
}

