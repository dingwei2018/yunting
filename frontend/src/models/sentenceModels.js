// 统一维护与句子编辑相关的数据模型，便于在不同模块/组件之间复用

// 根句子：拆句列表里显示的“拆句1、拆句2……”
export const createRootSentence = (data = {}) => ({
  sentence_id: data.sentence_id || '',
  parent_id: 0,
  content: data.content || '',
  duration: data.duration || 0,
  audio_url: data.audio_url || '',
  status: data.status || 'pending',
  display_order: data.display_order ?? 0,
  children: Array.isArray(data.children)
    ? data.children.map((child) => createSubSentence(child))
    : []
})

// 子句：在精修面板里可单独编辑、插入、删除的输入框
export const createSubSentence = (data = {}) => ({
  sentence_id: data.sentence_id || '',
  parent_id: data.parent_id || 0,
  display_order: data.display_order ?? 0,
  content: data.content || '',
  audio_url: data.audio_url || '',
  ...createSentenceParams(data)
})

// 子句的可调参数（音色/音量/语速/音调等）
export const createSentenceParams = (data = {}) => ({
  voice: data.voice || 'default',
  volume: clampVolume(data.volume),
  speed: clampSpeed(data.speed),
  pitch: typeof data.pitch === 'number' ? data.pitch : 50,
  polyphonicOverrides: Array.isArray(data.polyphonicOverrides)
    ? data.polyphonicOverrides
    : [],
  pauseMarkers: Array.isArray(data.pauseMarkers) ? data.pauseMarkers : [],
  silenceMarkers: Array.isArray(data.silenceMarkers)
    ? data.silenceMarkers
    : [],
  readingRules: Array.isArray(data.readingRules) ? data.readingRules : []
})

// 多音字状态：记录每个子句的多音字标记与选择结果
export const createPolyphonicState = () => ({
  markers: [],
  selections: Object.create(null)
})

// 多音字提示框状态
export const createPolyphonicTooltipState = () => ({
  visible: false,
  sentenceId: null,
  markerId: '',
  char: '',
  options: [],
  selected: '',
  position: { x: 0, y: 0 }
})

// 断句标准对话框使用的上下文
export const createSplitStandardState = () => ({
  type: 'punctuation',
  charCount: 50,
  visible: false,
  context: null // { rootSentence, originalText }
})

// ------------ 工具函数 ------------

export const clampVolume = (value) => {
  if (typeof value !== 'number' || Number.isNaN(value)) return 70
  return Math.min(100, Math.max(0, Math.round(value)))
}

export const clampSpeed = (value) => {
  if (typeof value !== 'number' || Number.isNaN(value)) return 0
  return Math.min(10, Math.max(-10, Math.round(value)))
}

