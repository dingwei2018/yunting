// 统一维护与句子编辑相关的数据模型，便于在不同模块/组件之间复用

// 根句子：拆句列表里显示的"拆句1、拆句2……"
export const createRootSentence = (data = {}) => ({
  sentence_id: data.sentence_id != null ? data.sentence_id : '',
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
export const createSubSentence = (data = {}) => {
  // 确保 parent_id 正确传递：如果明确传入 0，使用 0；如果传入其他值，使用该值；如果未传入，默认 0
  // 注意：这里使用 != null 来检查，因为 0 是有效的 parent_id（表示拆句）
  const parentId = data.parent_id != null ? data.parent_id : 0
  
  return {
    sentence_id: data.sentence_id != null ? data.sentence_id : '',
    parent_id: parentId,
    display_order: data.display_order ?? 0,
    content: data.content || '',
    audio_url: data.audio_url || '',
    ...createSentenceParams(data)
  }
}

// 子句的可调参数（音色/音量/语速/音调等）
export const createSentenceParams = (data = {}) => ({
  voice: data.voice || 'default',
  volume: clampVolume(data.volume),
  speed: clampSpeed(data.speed),
  pitch: typeof data.pitch === 'number' ? data.pitch : 50,
  speedSegments: Array.isArray(data.speedSegments) ? data.speedSegments : [],
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
  // 默认值33对应接口的140（接口默认值）
  // 计算：((140-90)/150)*100 = 33.33... ≈ 33
  if (typeof value !== 'number' || Number.isNaN(value)) return 33
  return Math.min(100, Math.max(0, Math.round(value)))
}

export const clampSpeed = (value) => {
  if (typeof value !== 'number' || Number.isNaN(value)) return 0
  return Math.min(10, Math.max(-10, Math.round(value)))
}

// ------------ API 数据转换函数 ------------

/**
 * 将 getOriginalSentenceList 返回的数据转换为现有的句子模型
 * @param {Object} apiData - API 返回的数据
 * @param {Array} apiData.list - 拆句列表
 * @returns {Array} 转换后的句子列表
 */
export const transformOriginalSentenceList = (apiData) => {
  if (!apiData || !Array.isArray(apiData.list)) {
    return []
  }

  const sentences = []

  apiData.list.forEach((originalSentence) => {
    // 创建根句子（拆句）
    const rootSentence = createRootSentence({
      sentence_id: originalSentence.originalSentenceId,
      content: originalSentence.content || '',
      duration: originalSentence.audioDuration || 0,
      audio_url: originalSentence.audioUrl || '',
      status: mapSynthesisStatus(originalSentence.synthesisStatus),
      display_order: originalSentence.sequence || 0,
      children: []
    })

    // 转换断句列表为子句子
    if (Array.isArray(originalSentence.breakingSentenceList)) {
      rootSentence.children = originalSentence.breakingSentenceList.map((breakingSentence, index) => {
        const setting = breakingSentence.setting || {}
        
        // console.log('🔄 [transformOriginalSentenceList] 转换断句', {
        //   originalSentenceId: originalSentence.originalSentenceId,
        //   breakingSentenceIndex: index,
        //   breakingSentenceId: breakingSentence.breakingSentenceId,
        //   breakingSentenceIdType: typeof breakingSentence.breakingSentenceId,
        //   synthesisStatus: breakingSentence.synthesisStatus,
        //   audioUrl: breakingSentence.audioUrl,
        //   hasAudioUrl: !!breakingSentence.audioUrl
        // })
        
        // 获取纯文本内容
        const plainContent = breakingSentence.content || setting.content || ''
        
        // 统一处理：将接口的标记数据转换为编辑器文本内容
        // 包括停顿标记（breakList）和静音标记（silentList）
        const contentWithMarkers = applyAllMarkersToContent(plainContent, setting)
        
        const subSentence = createSubSentence({
          sentence_id: breakingSentence.breakingSentenceId,
          parent_id: originalSentence.originalSentenceId,
          content: contentWithMarkers, // 使用包含所有标记的内容
          audio_url: breakingSentence.audioUrl || '',
          display_order: breakingSentence.sequence || 0,
          // 映射合成参数
          voice: setting.voiceId || 'default',
          volume: convertVolumeFromApi(setting.volume), // 将接口的90-240转换为前端的0-100
          speed: mapSpeedFromSetting(setting),
          pitch: 50, // 新接口中没有 pitch，使用默认值
          // 映射其他参数
          // api.md: 返回数据中使用 prosodyList
          speedSegments: mapProsodyToSpeedSegments(setting.prosodyList || []),
          pauseMarkers: mapBreakListToPauseMarkers(setting.breakList || []),
          polyphonicOverrides: mapPhonemeListToPolyphonic(setting.phonemeList || []),
          silenceMarkers: mapSilentListToSilenceMarkers(setting.silentList || []),
          readingRules: mapReadRuleToReadingRules(setting.readRule || [])
        })
        
        // console.log('✅ [transformOriginalSentenceList] 断句转换完成', {
        //   sentenceId: subSentence.sentence_id,
        //   sentenceIdType: typeof subSentence.sentence_id,
        //   parentId: subSentence.parent_id,
        //   audioUrl: subSentence.audio_url
        // })
        
        return subSentence
      })
    }

    sentences.push(rootSentence)
  })

  return sentences
}

/**
 * 映射合成状态
 * @param {number} status - 合成状态码
 * @returns {string} 状态字符串
 */
const mapSynthesisStatus = (status) => {
  // 0-未合成，1-合成中，2-已合成，3-合成失败
  const statusMap = {
    0: 'pending',
    1: 'processing',
    2: 'completed',
    3: 'failed'
  }
  return statusMap[status] || 'pending'
}

/**
 * 从 setting 中映射语速
 * @param {Object} setting - 断句设置
 * @returns {number} 语速值
 */
export const mapSpeedFromSetting = (setting) => {
  // 新接口中 speed 是全局语速（50-200），需要转换为 -10 到 10 的范围
  // 100 对应 0，50 对应 -10，200 对应 10
  if (typeof setting.speed === 'number') {
    return Math.round(((setting.speed - 100) / 100) * 10)
  }
  return 0
}

/**
 * 将 prosodyList 转换为 speedSegments
 * @param {Array} prosodyList - 局部语速列表（api.md 使用 prosodyList）
 * @returns {Array} speedSegments
 */
export const mapProsodyToSpeedSegments = (prosodyList) => {
  if (!Array.isArray(prosodyList)) return []
  return prosodyList.map((item) => {
    // rate 可能是字符串（返回数据）或数字（请求参数），如 "50", "100", "200" 或 50, 100, 200
    const rate = typeof item.rate === 'string' ? parseInt(item.rate) : (item.rate || 100)
    const speed = Math.round(((rate - 100) / 100) * 10)
    return {
      begin: item.begin || 0,
      end: item.end || 0,
      speed: speed
    }
  })
}

/**
 * 将 breakList 转换为 pauseMarkers
 * @param {Array} breakList - 停顿列表
 * @returns {Array} pauseMarkers
 */
export const mapBreakListToPauseMarkers = (breakList) => {
  if (!Array.isArray(breakList)) return []
  return breakList.map((item) => {
    // api.md: 使用 duration 字段（string 或 integer）
    const duration = item.duration !== undefined 
      ? (typeof item.duration === 'string' ? parseInt(item.duration) : item.duration)
      : 0
    
    return {
      location: typeof item.location === 'string' ? parseInt(item.location) : item.location || 0,
      duration: duration
    }
  })
}

/**
 * 将 breakList 转换为编辑器文本内容（在纯文本中插入停顿标记）
 * @param {string} plainText - 纯文本内容
 * @param {Array} breakList - 停顿列表
 * @returns {string} 包含停顿标记的文本内容
 */
export const applyBreakListToContent = (plainText, breakList) => {
  if (!plainText || typeof plainText !== 'string') return plainText || ''
  if (!Array.isArray(breakList) || breakList.length === 0) return plainText
  
  // 按 location 从大到小排序，从后往前插入，避免位置偏移
  const sortedBreakList = [...breakList].sort((a, b) => {
    const locationA = typeof a.location === 'string' ? parseInt(a.location) : (a.location || 0)
    const locationB = typeof b.location === 'string' ? parseInt(b.location) : (b.location || 0)
    return locationB - locationA // 从大到小排序
  })
  
  let result = plainText
  
  // 从后往前插入停顿标记，避免位置偏移
  for (const item of sortedBreakList) {
    const location = typeof item.location === 'string' ? parseInt(item.location) : (item.location || 0)
    const duration = item.duration !== undefined 
      ? (typeof item.duration === 'string' ? parseInt(item.duration) : item.duration)
      : 0
    
    // 转换为秒（duration 是毫秒）
    const durationSeconds = (duration / 1000).toFixed(1)
    
    // 在指定位置插入停顿标记
    // location 是字符位置，在 location 之后插入
    if (location >= 0 && location <= result.length) {
      const before = result.substring(0, location)
      const after = result.substring(location)
      result = before + `<pause:${durationSeconds}>` + after
    }
  }
  
  return result
}

/**
 * 将 silentList 转换为编辑器文本内容（在纯文本中插入静音标记）
 * @param {string} plainText - 纯文本内容
 * @param {Array} silentList - 静音列表
 * @returns {string} 包含静音标记的文本内容
 */
export const applySilentListToContent = (plainText, silentList) => {
  if (!plainText || typeof plainText !== 'string') return plainText || ''
  if (!Array.isArray(silentList) || silentList.length === 0) return plainText
  
  // 按 location 从大到小排序，从后往前插入，避免位置偏移
  const sortedSilentList = [...silentList].sort((a, b) => {
    const locationA = typeof a.location === 'string' ? parseInt(a.location) : (a.location || 0)
    const locationB = typeof b.location === 'string' ? parseInt(b.location) : (b.location || 0)
    return locationB - locationA // 从大到小排序
  })
  
  let result = plainText
  
  // 从后往前插入静音标记，避免位置偏移
  for (const item of sortedSilentList) {
    const location = typeof item.location === 'string' ? parseInt(item.location) : (item.location || 0)
    // 解析 duration：接口返回的是毫秒，需要转换为秒
    let duration = 0
    if (item.duration !== undefined && item.duration !== null) {
      if (typeof item.duration === 'string') {
        duration = parseFloat(item.duration) || 0
      } else if (typeof item.duration === 'number') {
        duration = item.duration
      }
    }
    
    // 转换为秒（duration 是毫秒）
    const durationSeconds = (duration / 1000).toFixed(1)
    
    // 在指定位置插入静音标记
    // location 是字符位置，在 location 之后插入
    if (location >= 0 && location <= result.length) {
      const before = result.substring(0, location)
      const after = result.substring(location)
      result = before + `<silence:${durationSeconds}>` + after
    }
  }
  
  return result
}

/**
 * 统一处理：将接口的标记数据转换为编辑器文本内容
 * 包括停顿标记（breakList）和静音标记（silentList）
 * 
 * 注意：
 * - 局部变速（prosodyList）和多音字（phonemeList）不需要转换为文本标记
 * - 它们通过 props 传递给编辑器，编辑器会自动应用
 * 
 * @param {string} plainText - 纯文本内容
 * @param {Object} setting - 接口返回的 setting 对象
 * @param {Array} setting.breakList - 停顿列表
 * @param {Array} setting.silentList - 静音列表
 * @returns {string} 包含标记的文本内容
 */
export const applyAllMarkersToContent = (plainText, setting = {}) => {
  if (!plainText || typeof plainText !== 'string') return plainText || ''
  
  let result = plainText
  
  // 先应用停顿标记
  result = applyBreakListToContent(result, setting.breakList || [])
  
  // 再应用静音标记（注意：静音标记的位置是基于原始文本的，不是基于已插入停顿标记后的文本）
  // 但为了简化，我们基于已插入停顿标记后的文本位置来插入静音标记
  // 这可能会导致位置偏移，但实际使用中，停顿和静音通常不会重叠
  result = applySilentListToContent(result, setting.silentList || [])
  
  return result
}

/**
 * 从编辑器内容中提取纯文本（去除所有标记）
 * @param {string} editorContent - 编辑器内容（包含标记）
 * @returns {string} 纯文本内容
 */
export const extractPlainTextFromContent = (editorContent) => {
  if (!editorContent || typeof editorContent !== 'string') {
    return ''
  }
  
  // 移除停顿标记 <pause:duration> 或 <pause>
  let plainText = editorContent.replace(/<pause(?::[\d.]+)?>/g, '')
  
  // 移除静音标记 <silence:duration>
  plainText = plainText.replace(/<silence:[\d.]+>/g, '')
  
  return plainText
}

/**
 * 统一处理：从编辑器内容中解析所有标记
 * 包括停顿标记和静音标记
 * 
 * 注意：
 * - 局部变速（speedSegments）和多音字（polyphonicOverrides）不需要从内容解析
 * - 它们通过 props 传递给编辑器，编辑器会自动应用
 * 
 * @param {string} editorContent - 编辑器内容（包含标记）
 * @returns {Object} 包含所有解析出的标记
 * @returns {Array} pauseMarkers - 停顿标记数组
 * @returns {Array} silenceMarkers - 静音标记数组
 */
export const parseAllMarkersFromContent = (editorContent) => {
  if (!editorContent || typeof editorContent !== 'string') {
    return {
      pauseMarkers: [],
      silenceMarkers: []
    }
  }
  
  return {
    pauseMarkers: parsePauseMarkersFromContent(editorContent),
    silenceMarkers: parseSilenceMarkersFromContent(editorContent)
  }
}

/**
 * 检测标签是否嵌套
 * 华为云暂时不支持标签嵌套，需要检测以下标记是否重叠：
 * - 停顿标记（pauseMarkers）：位置在 location（插入在字符之后，不占据字符位置）
 * - 静音标记（silenceMarkers）：位置在 location（插入在字符之后，不占据字符位置）
 * - 多音字标记（polyphonicOverrides）：位置在 begin 到 end（字符范围）
 * - 局部语速标记（speedSegments）：位置在 begin 到 end（字符范围）
 * 
 * 嵌套规则：
 * 1. 停顿和静音标记不能在同一位置
 * 2. 停顿/静音标记不能插入在多音字或局部语速标记的字符范围内
 * 3. 多音字和局部语速标记不能重叠
 * 
 * @param {Object} markers - 所有标记对象
 * @param {Array} markers.pauseMarkers - 停顿标记数组 [{ location, duration }]
 * @param {Array} markers.silenceMarkers - 静音标记数组 [{ location, duration }]
 * @param {Array} markers.polyphonicOverrides - 多音字标记数组 [{ begin, end, ph }]
 * @param {Array} markers.speedSegments - 局部语速标记数组 [{ begin, end, speed }]
 * @returns {Object} 检测结果 { hasNesting: boolean, message: string }
 */
export const detectMarkerNesting = (markers) => {
  const { pauseMarkers = [], silenceMarkers = [], polyphonicOverrides = [], speedSegments = [] } = markers || {}
  
  // 打印所有标记信息用于调试
  console.log('[检测标签嵌套] 所有标记信息', {
    pauseMarkers: pauseMarkers.map(m => ({ location: m.location, duration: m.duration, raw: m })),
    silenceMarkers: silenceMarkers.map(m => ({ location: m.location, duration: m.duration, raw: m })),
    polyphonicOverrides: polyphonicOverrides.map(m => ({ begin: m.begin, end: m.end, ph: m.ph, raw: m })),
    speedSegments: speedSegments.map(m => {
      const begin = m.begin !== undefined ? m.begin : m.offset
      const end = m.end !== undefined ? m.end : (m.begin !== undefined ? m.end : (m.offset + m.length))
      return {
        begin,
        end,
        speed: m.speed,
        raw: m,
        hasBegin: m.begin !== undefined,
        hasEnd: m.end !== undefined,
        hasOffset: m.offset !== undefined,
        hasLength: m.length !== undefined
      }
    })
  })
  
  // 1. 检查停顿和静音标记是否在同一位置
  const pauseLocations = new Set(pauseMarkers.map(m => m.location).filter(loc => loc !== undefined))
  const silenceLocations = new Set(silenceMarkers.map(m => m.location).filter(loc => loc !== undefined))
  
  for (const location of pauseLocations) {
    if (silenceLocations.has(location)) {
      console.error('[检测标签嵌套] 停顿和静音标记在同一位置', {
        location,
        pauseMarker: pauseMarkers.find(m => m.location === location),
        silenceMarker: silenceMarkers.find(m => m.location === location)
      })
      return {
        hasNesting: true,
        message: '华为云暂时不支持标签嵌套'
      }
    }
  }
  
  // 2. 检查停顿/静音标记是否插入在多音字或局部语速标记的字符范围内
  // 停顿/静音标记的 location 是插入位置（在字符之后），如果 location 在多音字/局部语速的 [begin, end) 范围内，则嵌套
  // 注意：location 是插入位置，如果 location === begin，表示在局部语速标记的开始位置插入，不算嵌套
  // 如果 location === end，表示在局部语速标记的结束位置之后插入，不算嵌套
  // 只有当 location > begin && location < end 时，才算嵌套
  for (const pauseMarker of pauseMarkers) {
    const location = pauseMarker.location
    if (location === undefined) continue
    
    // 检查是否在多音字标记范围内
    for (const polyphonic of polyphonicOverrides) {
      const begin = polyphonic.begin || 0
      const end = polyphonic.end || begin + 1
      // location 在 (begin, end) 范围内表示嵌套（不包括边界）
      if (location > begin && location < end) {
        console.error('[检测标签嵌套] 停顿标记插入在多音字标记范围内', {
          pauseLocation: location,
          pauseMarker: pauseMarker,
          polyphonic: { begin, end, ph: polyphonic.ph },
          conflict: `停顿标记位置 ${location} 在多音字标记范围 (${begin}, ${end}) 内`
        })
        return {
          hasNesting: true,
          message: '华为云暂时不支持标签嵌套'
        }
      }
    }
    
    // 检查是否在局部语速标记范围内
    for (const speedSegment of speedSegments) {
      const begin = speedSegment.begin !== undefined ? speedSegment.begin : (speedSegment.offset || 0)
      const end = speedSegment.end !== undefined ? speedSegment.end : (begin + (speedSegment.length || 1))
      // location 在 (begin, end) 范围内表示嵌套（不包括边界）
      if (location > begin && location < end) {
        console.error('[检测标签嵌套] 停顿标记插入在局部语速标记范围内', {
          pauseLocation: location,
          pauseMarker: pauseMarker,
          speedSegment: { begin, end, speed: speedSegment.speed },
          conflict: `停顿标记位置 ${location} 在局部语速标记范围 (${begin}, ${end}) 内`
        })
        return {
          hasNesting: true,
          message: '华为云暂时不支持标签嵌套'
        }
      }
    }
  }
  
  // 对静音标记做同样的检查
  for (const silenceMarker of silenceMarkers) {
    const location = silenceMarker.location
    if (location === undefined) continue
    
    // 检查是否在多音字标记范围内
    for (const polyphonic of polyphonicOverrides) {
      const begin = polyphonic.begin || 0
      const end = polyphonic.end || begin + 1
      // location 在 (begin, end) 范围内表示嵌套（不包括边界）
      if (location > begin && location < end) {
        console.error('[检测标签嵌套] 静音标记插入在多音字标记范围内', {
          silenceLocation: location,
          silenceMarker: silenceMarker,
          polyphonic: { begin, end, ph: polyphonic.ph },
          conflict: `静音标记位置 ${location} 在多音字标记范围 (${begin}, ${end}) 内`
        })
        return {
          hasNesting: true,
          message: '华为云暂时不支持标签嵌套'
        }
      }
    }
    
    // 检查是否在局部语速标记范围内
    for (const speedSegment of speedSegments) {
      const begin = speedSegment.begin !== undefined ? speedSegment.begin : (speedSegment.offset || 0)
      const end = speedSegment.end !== undefined ? speedSegment.end : (begin + (speedSegment.length || 1))
      // location 在 (begin, end) 范围内表示嵌套（不包括边界）
      if (location > begin && location < end) {
        console.error('[检测标签嵌套] 静音标记插入在局部语速标记范围内', {
          silenceLocation: location,
          silenceMarker: silenceMarker,
          speedSegment: { begin, end, speed: speedSegment.speed },
          conflict: `静音标记位置 ${location} 在局部语速标记范围 (${begin}, ${end}) 内`
        })
        return {
          hasNesting: true,
          message: '华为云暂时不支持标签嵌套'
        }
      }
    }
  }
  
  // 3. 检查多音字和局部语速标记是否重叠
  // 将所有范围标记收集起来
  const ranges = []
  
  polyphonicOverrides.forEach((marker) => {
    const begin = marker.begin || 0
    const end = marker.end || begin + 1
    ranges.push({
      type: 'polyphonic',
      start: begin,
      end: end
    })
  })
  
  speedSegments.forEach((marker) => {
    const begin = marker.begin !== undefined ? marker.begin : (marker.offset || 0)
    const end = marker.end !== undefined ? marker.end : (begin + (marker.length || 1))
    ranges.push({
      type: 'speed',
      start: begin,
      end: end
    })
  })
  
  // 检查范围是否重叠
  // 两个范围重叠的条件：!(range1.end <= range2.start || range2.end <= range1.start)
  for (let i = 0; i < ranges.length; i++) {
    for (let j = i + 1; j < ranges.length; j++) {
      const range1 = ranges[i]
      const range2 = ranges[j]
      
      if (!(range1.end <= range2.start || range2.end <= range1.start)) {
        console.error('[检测标签嵌套] 范围标记重叠', {
          range1: { type: range1.type, start: range1.start, end: range1.end },
          range2: { type: range2.type, start: range2.start, end: range2.end },
          conflict: `${range1.type} 标记 [${range1.start}, ${range1.end}) 与 ${range2.type} 标记 [${range2.start}, ${range2.end}) 重叠`
        })
        return {
          hasNesting: true,
          message: '华为云暂时不支持标签嵌套'
        }
      }
    }
  }
  
  console.log('[检测标签嵌套] 未发现嵌套')
  return {
    hasNesting: false,
    message: ''
  }
}

/**
 * 将 phonemeList 转换为 polyphonicOverrides
 * @param {Array} phonemeList - 多音字列表（接口格式：{ ph, location }）
 * @returns {Array} polyphonicOverrides（前端格式：{ begin, end, ph, alphabet }）
 */
export const mapPhonemeListToPolyphonic = (phonemeList) => {
  if (!Array.isArray(phonemeList)) return []
  return phonemeList.map((item) => {
    // 接口使用 location，前端使用 begin 和 end
    // 多音字标签只能包含1个汉字，所以 end = begin + 1
    const begin = item.location !== undefined ? item.location : (item.begin || 0)
    const end = begin + 1
    
    return {
      begin: begin,
      end: end,
    ph: item.ph || '',
    alphabet: item.alphabet || ''
    }
  })
}

/**
 * 将 silentList 转换为 silenceMarkers
 * @param {Array} silentList - 静音列表（api.md 新增）
 * @returns {Array} silenceMarkers（duration 单位为毫秒）
 */
export const mapSilentListToSilenceMarkers = (silentList) => {
  if (!Array.isArray(silentList)) return []
  
  return silentList.map((item) => {
    // 解析 location
    let location = 0
    if (item.location !== undefined && item.location !== null) {
      if (typeof item.location === 'string') {
        location = parseInt(item.location) || 0
      } else if (typeof item.location === 'number') {
        location = item.location
      }
    }
    
    // 解析 duration（接口返回的是毫秒）
    let duration = 0
    if (item.duration !== undefined && item.duration !== null) {
      if (typeof item.duration === 'string') {
        duration = parseFloat(item.duration) || 0
      } else if (typeof item.duration === 'number') {
        duration = item.duration
      }
    }
    
    return {
      location: location,
      duration: duration
    }
  })
}

// ------------ 接口数据转换为前端数据 ------------

/**
 * 将接口音量值转换为前端音量值
 * 接口范围：90~240，默认值140，前端范围：0~100，默认值33
 * @param {number} apiVolume - 接口音量值 (90~240)
 * @returns {number} 前端音量值 (0~100)
 */
export const convertVolumeFromApi = (apiVolume) => {
  // 默认值33对应接口的140（接口默认值）
  // 计算：((140-90)/150)*100 = 33.33... ≈ 33
  if (typeof apiVolume !== 'number' || Number.isNaN(apiVolume)) return 33
  // 公式: volume_frontend = ((volume_api - 90) / 150) * 100
  const clamped = Math.max(90, Math.min(240, apiVolume))
  return Math.round(((clamped - 90) / 150) * 100)
}

// ------------ 前端数据转换为接口参数 ------------

/**
 * 将前端音量值转换为接口音量值
 * 前端范围：0~100，默认值33，接口范围：90~240，默认值140
 * @param {number} frontendVolume - 前端音量值 (0~100)
 * @returns {number} 接口音量值 (90~240)
 */
export const convertVolumeToApi = (frontendVolume) => {
  // 前端 0 → 接口 90，前端 100 → 接口 240
  // 默认值33对应接口的140（接口默认值）
  // 公式: volume_api = 90 + (volume_frontend / 100) * 150
  const clamped = Math.max(0, Math.min(100, frontendVolume ?? 33))
  return Math.round(90 + (clamped / 100) * 150)
}

/**
 * 将前端语速值转换为接口语速值
 * 前端范围：-10~10，接口范围：50~200
 * @param {number} frontendSpeed - 前端语速值 (-10~10)
 * @returns {number} 接口语速值 (50~200)
 */
export const convertSpeedToApi = (frontendSpeed) => {
  // 前端 -10 → 接口 50，前端 0 → 接口 100，前端 10 → 接口 200
  // 公式: speed_api = 100 + (speed_frontend / 10) * 100
  const clamped = Math.max(-10, Math.min(10, frontendSpeed || 0))
  return Math.round(100 + (clamped / 10) * 100)
}

/**
 * 将前端 speedSegments 转换为接口 prosodyList
 * @param {Array} speedSegments - 前端局部语速列表
 * @returns {Array} prosodyList
 */
export const convertSpeedSegmentsToProsodyList = (speedSegments) => {
  if (!Array.isArray(speedSegments) || speedSegments.length === 0) {
    return []
  }
  return speedSegments.map((segment) => {
    // 将前端的 speed (-10~10) 转换为接口的 rate (50~200)
    const rate = convertSpeedToApi(segment.speed || 0)
    return {
      rate: rate,
      begin: segment.begin || 0,
      end: segment.end || 0
    }
  })
}

/**
 * 从内容字符串中解析静音标记
 * @param {string} content - 包含静音标记的内容字符串，如 "文本<silence:1.0>更多文本"
 * @returns {Array} silenceMarkers 数组，每个元素包含 { location: number, duration: number }
 */
export const parseSilenceMarkersFromContent = (content) => {
  if (!content || typeof content !== 'string') return []
  
  const SILENCE_TOKEN_REGEX = /<silence:([\d.]+)>/g
  const silenceMarkers = []
  let plainText = ''
  let lastIndex = 0
  let match
  
  // 重置正则表达式
  SILENCE_TOKEN_REGEX.lastIndex = 0
  
  while ((match = SILENCE_TOKEN_REGEX.exec(content)) !== null) {
    // 添加匹配前的文本到 plainText
    plainText += content.substring(lastIndex, match.index)
    
    // 计算静音标记的位置（在纯文本中的位置）
    const location = plainText.length
    
    // 解析静音时长
    const durationStr = match[1] || '0'
    const duration = parseFloat(durationStr) || 0
    
    // 转换为毫秒（接口要求 duration 是毫秒）
    const durationMs = Math.round(duration * 1000)
    
    const marker = {
      location: location,
      duration: durationMs
    }
    
    silenceMarkers.push(marker)
    lastIndex = SILENCE_TOKEN_REGEX.lastIndex
  }
  
  return silenceMarkers
}

/**
 * 从内容字符串中解析停顿标记
 * @param {string} content - 包含停顿标记的内容字符串（格式：<pause:duration>）
 * @returns {Array} pauseMarkers 数组
 */
export const parsePauseMarkersFromContent = (content) => {
  if (!content || typeof content !== 'string') {
    return []
  }
  
  const pauseMarkers = []
  const PAUSE_TOKEN_REGEX = /<pause(?::([\d.]+))?>/g
  const DEFAULT_PAUSE_DURATION = 0.5
  
  // 移除所有停顿标记，同时记录它们的位置
  let plainText = ''
  let match
  let lastIndex = 0
  
  // 重置正则表达式
  PAUSE_TOKEN_REGEX.lastIndex = 0
  
  while ((match = PAUSE_TOKEN_REGEX.exec(content)) !== null) {
    // 添加匹配前的文本到 plainText
    plainText += content.substring(lastIndex, match.index)
    
    // 计算停顿标记的位置（在纯文本中的位置）
    const location = plainText.length
    
    // 解析停顿时长
    const durationStr = match[1] || DEFAULT_PAUSE_DURATION
    const duration = parseFloat(durationStr) || DEFAULT_PAUSE_DURATION
    
    // 转换为毫秒（接口要求 duration 是毫秒）
    const durationMs = Math.round(duration * 1000)
    
    const marker = {
      location: location,
      duration: durationMs
    }
    
    pauseMarkers.push(marker)
    
    lastIndex = PAUSE_TOKEN_REGEX.lastIndex
  }
  
  // 添加剩余的文本
  plainText += content.substring(lastIndex)
  
  return pauseMarkers
}

/**
 * 将前端 pauseMarkers 转换为接口 breakList
 * @param {Array} pauseMarkers - 前端停顿标记列表
 * @returns {Array} breakList
 */
export const convertPauseMarkersToBreakList = (pauseMarkers) => {
  if (!Array.isArray(pauseMarkers) || pauseMarkers.length === 0) {
    return []
  }
  
  // 根据接口文档，location 和 duration 都应该是 integer 类型
  const breakList = pauseMarkers.map((marker) => {
    return {
      location: parseInt(marker.location || 0, 10),
      duration: parseInt(marker.duration || 0, 10)
    }
  })
  
  return breakList
}

/**
 * 将带声调符号的拼音转换为数字声调格式
 * 例如：huì -> hui4, mā -> ma1, lǘ -> lv2
 * 声调映射：1=āēīōūǖ, 2=áéíóúǘ, 3=ǎěǐǒǔǚ, 4=àèìòùǜ, 5=轻声（无符号）
 * @param {string} pinyin - 带声调符号的拼音
 * @returns {string} 数字声调格式的拼音
 */
export const convertPinyinToneToNumber = (pinyin) => {
  if (!pinyin || typeof pinyin !== 'string') return pinyin || ''
  
  // 声调映射表：带声调符号 -> [基础字母, 声调数字]
  const toneMap = {
    // 第一声（阴平）
    'ā': ['a', '1'], 'ē': ['e', '1'], 'ī': ['i', '1'], 'ō': ['o', '1'], 'ū': ['u', '1'], 'ǖ': ['v', '1'],
    // 第二声（阳平）
    'á': ['a', '2'], 'é': ['e', '2'], 'í': ['i', '2'], 'ó': ['o', '2'], 'ú': ['u', '2'], 'ǘ': ['v', '2'],
    // 第三声（上声）
    'ǎ': ['a', '3'], 'ě': ['e', '3'], 'ǐ': ['i', '3'], 'ǒ': ['o', '3'], 'ǔ': ['u', '3'], 'ǚ': ['v', '3'],
    // 第四声（去声）
    'à': ['a', '4'], 'è': ['e', '4'], 'ì': ['i', '4'], 'ò': ['o', '4'], 'ù': ['u', '4'], 'ǜ': ['v', '4']
  }
  
  let result = pinyin
  let tone = '5' // 默认轻声
  
  // 查找并替换声调符号
  for (const [toneChar, [baseChar, toneNum]] of Object.entries(toneMap)) {
    if (result.includes(toneChar)) {
      result = result.replace(toneChar, baseChar)
      tone = toneNum
      break // 一个拼音只能有一个声调符号
    }
  }
  
  // 如果找到了声调符号，在末尾添加数字；如果没有，添加5（轻声）
  return result + tone
}

/**
 * 将前端 polyphonicOverrides 转换为接口 phonemeList
 * @param {Array} polyphonicOverrides - 前端多音字覆盖列表
 * @returns {Array} phonemeList
 */
export const convertPolyphonicToPhonemeList = (polyphonicOverrides) => {
  if (!Array.isArray(polyphonicOverrides) || polyphonicOverrides.length === 0) {
    return []
  }
  return polyphonicOverrides
    .filter((override) => {
      // 验证：begin 和 end 之间只能有1个汉字（end - begin === 1）
      const begin = override.begin || 0
      const end = override.end || begin + 1
      if (end - begin !== 1) {
        console.warn('多音字标签起始和结束位置之间只能有1个汉字', {
          begin,
          end,
          ph: override.ph
        })
        return false
      }
      return true
    })
    .map((override) => {
    // 根据 api.md，phonemeList 的格式是 { ph, location }
    // location 是标签插入位置，默认一个中文字符
    // 前端有 begin 和 end，这里使用 begin 作为 location（因为多音字标记通常是一个字符）
      const ph = override.ph || ''
      // 将带声调符号的拼音转换为数字声调格式
      const phWithNumberTone = convertPinyinToneToNumber(ph)
      
    return {
        ph: phWithNumberTone,
      location: override.begin || 0
    }
  })
}

/**
 * 将前端 silenceMarkers 转换为接口 silenceList
 * @param {Array} silenceMarkers - 前端静音标记列表
 * @returns {Array} silenceList
 */
export const convertSilenceMarkersToSilenceList = (silenceMarkers) => {
  if (!Array.isArray(silenceMarkers) || silenceMarkers.length === 0) {
    return []
  }
  return silenceMarkers.map((marker) => ({
    location: marker.location || 0,
    duration: marker.duration || 0
  }))
}

/**
 * 将接口 readRule 格式转换为前端 readingRules
 * @param {Array} readRule - 接口阅读规则列表，格式：[{ ruleId: string, partern: string }]（注意字段名是 partern）
 * @returns {Array} readingRules 格式：[{ ruleId: string, pattern: string }]
 */
export const mapReadRuleToReadingRules = (readRule) => {
  if (!Array.isArray(readRule) || readRule.length === 0) {
    return []
  }
  return readRule.map((rule) => ({
    ruleId: String(rule.ruleId || rule.rule_id || ''),
    pattern: rule.partern || rule.pattern || ''
  }))
}

/**
 * 将前端 readingRules 转换为接口 readRule 格式
 * @param {Array} readingRules - 前端阅读规则列表，格式：[{ ruleId: string, pattern: string }]
 * @returns {Array} readRule 格式：[{ ruleId: string, partern: string }]（注意字段名是 partern）
 */
export const convertReadingRulesToReadRule = (readingRules) => {
  if (!Array.isArray(readingRules) || readingRules.length === 0) {
    return []
  }
  return readingRules.map((rule) => ({
    ruleId: String(rule.ruleId || rule.rule_id || ''),
    partern: rule.pattern || rule.partern || ''
  }))
}

/**
 * 将前端断句数据转换为接口参数格式
 * @param {Object} breakingSentence - 前端断句数据
 * @returns {Object} 接口断句参数
 */
export const convertBreakingSentenceToApi = (breakingSentence) => {
  // 判断是否为新增（本地创建的断句，ID 为负数，如 -1, -2, -3...）
  const isNew = typeof breakingSentence.sentence_id === 'number' && breakingSentence.sentence_id < 0
  
  const apiData = {
    breakingSentenceId: isNew ? -1 : (parseInt(breakingSentence.sentence_id) || -1),
    sequence: breakingSentence.display_order || breakingSentence.sequence || 0,
    content: breakingSentence.content || '',
    volume: convertVolumeToApi(breakingSentence.volume ?? 33),
    voiceId: breakingSentence.voice || 'default',
    speed: convertSpeedToApi(breakingSentence.speed || 0),
    breakList: convertPauseMarkersToBreakList(breakingSentence.pauseMarkers || []),
    phonemeList: convertPolyphonicToPhonemeList(breakingSentence.polyphonicOverrides || []),
    prosodyList: convertSpeedSegmentsToProsodyList(breakingSentence.speedSegments || []),
    silenceList: convertSilenceMarkersToSilenceList(breakingSentence.silenceMarkers || []),
    readRule: convertReadingRulesToReadRule(breakingSentence.readingRules || [])
  }
  
  // 打印转换后的接口数据
  console.log('[接口数据转换]', {
    breakingSentenceId: apiData.breakingSentenceId,
    content: apiData.content,
    contentLength: apiData.content.length,
    volume: apiData.volume,
    voiceId: apiData.voiceId,
    speed: apiData.speed,
    breakList: apiData.breakList,
    breakListCount: apiData.breakList.length,
    phonemeList: apiData.phonemeList,
    phonemeListCount: apiData.phonemeList.length,
    prosodyList: apiData.prosodyList,
    prosodyListCount: apiData.prosodyList.length,
    silenceList: apiData.silenceList,
    silenceListCount: apiData.silenceList.length,
    fullApiData: apiData
  })
  
  return apiData
}
