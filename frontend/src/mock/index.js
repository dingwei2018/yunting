const delay = (ms = 400) => new Promise((resolve) => setTimeout(resolve, ms))

const DEFAULT_TEXT =
  `11月17日至18日，中央全面依法治国工作会议在北京召开。习近平总书记作出重要指示，充分肯定党的十八大以来法治中国建设取得的重大成就，对新征程上推进全面依法治国作出重要部署，强调“坚持党的领导、人民当家作主、依法治国有机统一”，要求“聚焦建设更加完善的中国特色社会主义法治体系、建设更高水平的社会主义法治国家”“全面推进科学立法、严格执法、公正司法、全民守法，全面推进国家各方面工作法治化”。
思想照亮征程，指引道路越走越宽广。2020年11月，党的历史上首次召开中央全面依法治国工作会议，明确了习近平法治思想在全面依法治国工作中的指导地位，这在中国特色社会主义法治建设进程上具有里程碑意义。5年来，从与时俱进不断完善以宪法为核心的中国特色社会主义法律体系，到制定实施法治中国建设规划、法治政府建设实施纲要、法治社会建设实施纲要，再到深入推进法治化营商环境建设、扎实开展执法司法专项检查、依法惩治违法犯罪……在习近平法治思想引领下，全面依法治国取得历史性成就，为书写“两大奇迹”新篇章提供了有力保障。
近日，《习近平法治文选》第一卷出版发行，为全党全国各族人民深入学习贯彻习近平新时代中国特色社会主义思想特别是习近平法治思想提供了权威教材。紧密联系这些年法治中国建设取得的突破性进展，联系发生在我们身边的变革性实践，更能感受到这一重要思想蕴含的真理力量和实践伟力，更加坚定走中国特色社会主义法治道路的决心和信心。新时代新征程，我们要坚持以习近平法治思想为指引，牢牢把握全面依法治国这场国家治理的深刻革命，更加自觉地在法治轨道上全面建设社会主义现代化国家，合力开创法治中国建设新局面。
“更加注重法治与改革、发展、稳定相协同，更加注重保障和促进社会公平正义”，习近平总书记以“两个更加”指明了全面依法治国的重点。这里，重点围绕如何促进法治与发展相协同进行阐释，以深化理解。
今年5月30日，民营经济促进法施行不久，最高人民法院行政庭首次适用该法，在一起案件中判决政府部门向企业支付800余万元补偿。法治固根本、稳预期、利长远。正是以法治力量为民营企业“撑腰”，让广大企业真正感受到“法治是最好的营商环境”，吃下定心丸、安心谋发展。
习近平总书记强调：“贯彻新发展理念，实现经济从高速增长转向高质量发展，必须坚持以法治为引领。”守好绿水青山，长江保护法等一系列法律法规的严格实施，让一度成为“稀客”的“微笑天使”江豚回来了。打造开放门户，海南自由贸易港法等制定出台，护航海南自贸港成形起势、即将启动全岛封关运作。以高水平法治促进高质量发展，我们前进的步伐更加稳健、动能更加强劲。
党的二十届四中全会《建议》将“社会主义法治国家建设达到更高水平”列入“十五五”时期经济社会发展的主要目标，提出建设法治经济、信用经济，打造市场化法治化国际化一流营商环境。面向未来，牢牢把握更加注重法治与发展相协同的要求，就要善于运用法治思维和法治方式推动经济社会发展、应对风险挑战，加快构建高水平社会主义市场经济体制，不断完善稳定的、公开的、规范的制度和规则，为经济社会高质量发展提供牢固的基础、持久的动力和广阔的空间。
我们干的是前无古人的开创性事业，进行人类史上非常宏大而独特的实践创新，关键要有自信、有定力、有行动。在以习近平同志为核心的党中央坚强领导下，全面贯彻习近平法治思想，沿着中国特色社会主义法治道路砥砺前行，筑牢法治之基，汇聚法治力量，中国式现代化就一定能行稳致远。`

let mockTask = {
  taskId: 1,
  mergeId: null,
  content: DEFAULT_TEXT,
  charCount: DEFAULT_TEXT.length,
  status: 0, // 0-拆句完成
  audioUrl: '',
  audioDuration: null,
  mergedAudioUrl: '',
  mergedAudioDuration: null,
  ssml: '',
  totalSentences: 0,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  sentences: [],
  breakingSentences: []
}

let sentenceSeq = 1
let breakingSentenceSeq = 1

// 构建拆句（sentences）
const buildSentence = (content, parentId = 0, sequence) => ({
  sentenceId: sentenceSeq++,
  parentId: parentId,
  sequence: sequence || sentenceSeq - 1,
  charCount: content.length,
  content,
  audioUrl: '',
  audioDuration: 0,
  ssml: ''
})

// 构建断句（breakingSentences）
const buildBreakingSentence = (content, originalSentenceId, sequence) => ({
  breakingSentenceId: breakingSentenceSeq++,
  originalSentenceId: originalSentenceId,
  sequence: sequence || breakingSentenceSeq - 1,
  content,
  synthesisStatus: 0, // 0-未合成，1-合成中，2-已合成，3-合成失败
  audioUrl: '',
  audioDuration: 0,
  ssml: ''
})

const splitContent = (content) => {
  const parts = content
    .split(/(?<=[。！？])/)
    .map((item) => item.trim())
    .filter(Boolean)
  if (parts.length === 0) {
    return [content]
  }
  return parts
}

const rebuildSentences = (content) => {
  const parts = splitContent(content)
  mockTask.sentences = parts.map((sentence, index) =>
    buildSentence(sentence, 0, index + 1)
  )
  // 每个拆句默认生成一条断句
  mockTask.breakingSentences = []
  parts.forEach((sentence, index) => {
    const originalSentence = mockTask.sentences[index]
    mockTask.breakingSentences.push(
      buildBreakingSentence(sentence, originalSentence.sentenceId, index + 1)
    )
  })
  mockTask.totalSentences = mockTask.sentences.length
}

// 初始化一次
rebuildSentences(DEFAULT_TEXT)

const mockResponse = async (data) => {
  await delay()
  return data
}

export const mockCreateTask = async ({ content, delimiters = [1, 2, 3] }) => {
  const taskId = Date.now()
  mockTask = {
    ...mockTask,
    taskId,
    content,
    charCount: content.length,
    status: 0, // 拆句完成
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  }
  rebuildSentences(content)
  return mockResponse({
    taskId: mockTask.taskId,
    mergeId: mockTask.mergeId,
    content: mockTask.content,
    charCount: mockTask.charCount,
    status: mockTask.status,
    audioUrl: mockTask.audioUrl,
    audioDuration: mockTask.audioDuration,
    createdAt: mockTask.createdAt,
    updatedAt: mockTask.updatedAt
  })
}

export const mockGetTask = async (taskId) => {
  return mockResponse({
    taskId: mockTask.taskId,
    mergeId: mockTask.mergeId,
    content: mockTask.content,
    charCount: mockTask.charCount,
    status: mockTask.status,
    mergedAudioUrl: mockTask.mergedAudioUrl,
    mergedAudioDuration: mockTask.mergedAudioDuration,
    ssml: mockTask.ssml,
    totalSentences: mockTask.totalSentences,
    createdAt: mockTask.createdAt,
    updatedAt: mockTask.updatedAt,
    sentences: mockTask.sentences.map(s => ({ ...s })),
    breakingSentences: mockTask.breakingSentences.map(bs => ({ ...bs }))
  })
}

/**
 * Mock 获取拆句列表（新接口格式）
 * api.md: page 从 0 开始
 */
export const mockGetOriginalSentenceList = async (taskId, page = 0, pageSize = 20) => {
  // 将现有的 sentences 和 breakingSentences 转换为新接口格式
  const originalSentences = mockTask.sentences.map((originalSentence) => {
    // 找到该拆句下的所有断句
    const breakingSentences = mockTask.breakingSentences
      .filter(bs => bs.originalSentenceId === originalSentence.sentenceId)
      .map(bs => ({
        breakingSentenceId: bs.breakingSentenceId,
        taskId: mockTask.taskId,
        originalSentenceId: bs.originalSentenceId,
        content: bs.content,
        charCount: bs.content.length,
        sequence: bs.sequence,
        synthesisStatus: bs.synthesisStatus,
        audioUrl: bs.audioUrl,
        audioDuration: bs.audioDuration,
        ssml: bs.ssml,
        jobId: '',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        setting: {
          content: bs.content,
          volume: 140,
          voiceId: 'default',
          breakList: [],
          phonemeList: [],
          prosodyList: [], // api.md: 使用 prosodyList
          silentList: [] // api.md: 新增静音列表
        }
      }))

    return {
      originalSentenceId: originalSentence.sentenceId,
      sequence: originalSentence.sequence,
      content: originalSentence.content,
      synthesisStatus: 0,
      audioUrl: originalSentence.audioUrl || '',
      audioDuration: originalSentence.audioDuration || 0,
      breakingSentenceList: breakingSentences
    }
  })

  return mockResponse({
    list: originalSentences,
    total: originalSentences.length,
    page: page,
    pageSize: pageSize
  })
}

export const mockGetTaskSentences = async () => {
  return mockResponse({
    taskId: mockTask.taskId,
    sentences: mockTask.sentences.map((item) => ({ ...item }))
  })
}

export const mockMergeAudio = async (taskId) => {
  const mergeId = Date.now()
  mockTask.mergeId = mergeId
  mockTask.mergedAudioUrl =
    mockTask.mergedAudioUrl ||
    'https://cdn.example.com/mock/merged_audio_mock.mp3'
  mockTask.mergedAudioDuration = 120 // 模拟音频时长
  mockTask.status = 5 // 语音合并成功
  return mockResponse({
    mergeId: mockTask.mergeId,
    taskId: mockTask.taskId,
    mergedAudioUrl: mockTask.mergedAudioUrl,
    audioDuration: mockTask.mergedAudioDuration,
    status: 'success'
  })
}

// 兼容旧的 sentence.js 使用的 mock 函数（保持向后兼容）
export const mockGetSentence = async (sentenceId) => {
  // 统一ID比较函数，支持数字和字符串
  const matchId = (id1, id2) => {
    if (id1 == null || id2 == null) return false
    // 尝试多种比较方式
    if (id1 === id2) return true
    if (String(id1) === String(id2)) return true
    if (Number(id1) === Number(id2) && !isNaN(Number(id1)) && !isNaN(Number(id2))) return true
    return false
  }

  // 调试信息
  console.log('[mockGetSentence] 查找句子ID:', sentenceId, typeof sentenceId)
  console.log('[mockGetSentence] sentences数量:', mockTask.sentences.length)
  console.log('[mockGetSentence] breakingSentences数量:', mockTask.breakingSentences.length)
  
  // 输出所有可用的ID用于调试
  const allSentenceIds = mockTask.sentences.map(s => ({ 
    sentenceId: s.sentenceId, 
    sentence_id: s.sentence_id,
    content: s.content?.substring(0, 20) 
  }))
  const allBreakingIds = mockTask.breakingSentences.map(bs => ({ 
    breakingSentenceId: bs.breakingSentenceId,
    originalSentenceId: bs.originalSentenceId,
    content: bs.content?.substring(0, 20)
  }))
  console.log('[mockGetSentence] 所有sentences ID:', allSentenceIds)
  console.log('[mockGetSentence] 所有breakingSentences ID:', allBreakingIds)

  // 先在 sentences 中查找（拆句/根句子）
  let sentence = mockTask.sentences.find(
    (item) => matchId(item.sentenceId, sentenceId) || matchId(item.sentence_id, sentenceId)
  )

  if (sentence) {
    console.log('[mockGetSentence] 在sentences中找到:', sentence.sentenceId || sentence.sentence_id)
  } else {
    console.log('[mockGetSentence] 在sentences中未找到，继续查找breakingSentences')
  }

  // 如果没找到，在 breakingSentences 中查找（断句/子句子）
  if (!sentence) {
    const breakingSentence = mockTask.breakingSentences.find(
      (item) => matchId(item.breakingSentenceId, sentenceId)
    )
    
    if (breakingSentence && import.meta.env.DEV) {
      console.log('[mockGetSentence] 在breakingSentences中找到:', breakingSentence.breakingSentenceId)
    }
    
    if (breakingSentence) {
      // 找到对应的拆句，用于获取父级信息
      const originalSentence = mockTask.sentences.find(
        (item) => item.sentenceId === breakingSentence.originalSentenceId
      )
      
      // 构造兼容格式的句子对象
      sentence = {
        sentenceId: breakingSentence.breakingSentenceId,
        sentence_id: breakingSentence.breakingSentenceId,
        parentId: breakingSentence.originalSentenceId,
        parent_id: breakingSentence.originalSentenceId,
        content: breakingSentence.content,
        audioUrl: breakingSentence.audioUrl,
        audio_url: breakingSentence.audioUrl,
        audioDuration: breakingSentence.audioDuration,
        display_order: breakingSentence.sequence,
        // 从 setting 中获取参数（如果有）
        voice: 'default',
        volume: 140,
        speed: 0,
        pitch: 50,
        speedSegments: [],
        pauseMarkers: [],
        polyphonicOverrides: [],
        silenceMarkers: [],
        readingRules: []
      }
    }
  }

  if (!sentence) {
    // 输出更详细的错误信息
    const availableIds = [
      ...mockTask.sentences.map(s => s.sentenceId || s.sentence_id),
      ...mockTask.breakingSentences.map(bs => bs.breakingSentenceId)
    ]
    console.error('[mockGetSentence] 未找到句子，查找ID:', sentenceId)
    console.error('[mockGetSentence] 可用的ID列表:', availableIds)
    throw new Error(`未找到句子，ID: ${sentenceId}`)
  }

  // 返回兼容格式
  return mockResponse({
    ...sentence,
    sentence_id: sentence.sentenceId || sentence.sentence_id || sentenceId,
    audio_url: sentence.audioUrl || sentence.audio_url || '',
    parent_id: sentence.parentId || sentence.parent_id || 0
  })
}

export const mockUpdateSentence = async (sentenceId, data) => {
  // 统一ID比较函数
  const matchId = (id1, id2) => {
    if (id1 == null || id2 == null) return false
    return id1 === id2 || String(id1) === String(id2) || Number(id1) === Number(id2)
  }

  // 先在 sentences 中查找（拆句）
  let index = mockTask.sentences.findIndex(
    (item) => matchId(item.sentenceId, sentenceId) || matchId(item.sentence_id, sentenceId)
  )

  if (index !== -1) {
    mockTask.sentences[index] = {
      ...mockTask.sentences[index],
      ...data
    }
    return mockResponse({ success: true })
  }

  // 如果没找到，在 breakingSentences 中查找（断句）
  index = mockTask.breakingSentences.findIndex(
    (item) => matchId(item.breakingSentenceId, sentenceId)
  )

  if (index !== -1) {
    mockTask.breakingSentences[index] = {
      ...mockTask.breakingSentences[index],
      ...data
    }
    return mockResponse({ success: true })
  }

  throw new Error('未找到句子')
}

export const mockDeleteSentence = async (sentenceId) => {
  // 统一ID比较函数
  const matchId = (id1, id2) => {
    if (id1 == null || id2 == null) return false
    return id1 === id2 || String(id1) === String(id2) || Number(id1) === Number(id2)
  }

  // 先在 sentences 中查找并删除（拆句）
  const sentenceIndex = mockTask.sentences.findIndex(
    (item) => matchId(item.sentenceId, sentenceId) || matchId(item.sentence_id, sentenceId)
  )

  if (sentenceIndex !== -1) {
    const deletedSentence = mockTask.sentences[sentenceIndex]
    // 删除拆句
    mockTask.sentences.splice(sentenceIndex, 1)
    // 同时删除该拆句下的所有断句
    mockTask.breakingSentences = mockTask.breakingSentences.filter(
      (item) => item.originalSentenceId !== deletedSentence.sentenceId
    )
    return mockResponse({ success: true })
  }

  // 如果没找到，在 breakingSentences 中查找并删除（断句）
  const breakingIndex = mockTask.breakingSentences.findIndex(
    (item) => matchId(item.breakingSentenceId, sentenceId)
  )

  if (breakingIndex !== -1) {
    mockTask.breakingSentences.splice(breakingIndex, 1)
    return mockResponse({ success: true })
  }

  throw new Error('未找到句子')
}

export const mockInsertSentenceAfter = async (sentenceId, { content }) => {
  const index = mockTask.sentences.findIndex(
    (item) => item.sentenceId === sentenceId || item.sentence_id === sentenceId
  )
  if (index === -1) throw new Error('未找到句子')
  const parentId =
    (mockTask.sentences[index].parentId || mockTask.sentences[index].parent_id) === 0
      ? (mockTask.sentences[index].sentenceId || mockTask.sentences[index].sentence_id)
      : (mockTask.sentences[index].parentId || mockTask.sentences[index].parent_id)
  const newSentence = buildSentence(content, parentId, mockTask.sentences.length + 1)
  mockTask.sentences.splice(index + 1, 0, newSentence)
  // 返回兼容格式
  return mockResponse({
    ...newSentence,
    sentence_id: newSentence.sentenceId,
    audio_url: newSentence.audioUrl,
    parent_id: newSentence.parentId
  })
}

export const mockSynthesizeSentence = async (sentenceId) => {
  // 统一ID比较函数
  const matchId = (id1, id2) => {
    if (id1 == null || id2 == null) return false
    return id1 === id2 || String(id1) === String(id2) || Number(id1) === Number(id2)
  }

  // 先在 sentences 中查找（拆句）
  let index = mockTask.sentences.findIndex(
    (item) => matchId(item.sentenceId, sentenceId) || matchId(item.sentence_id, sentenceId)
  )

  if (index !== -1) {
    const audioUrl = `https://cdn.example.com/mock/${sentenceId}.mp3`
    mockTask.sentences[index].audioUrl = audioUrl
    mockTask.sentences[index].audio_url = audioUrl // 兼容旧字段
    mockTask.sentences[index].audioDuration = Math.max(3, Math.floor(mockTask.sentences[index].content.length / 4))
    return mockResponse({
      audioUrl,
      audio_url: audioUrl // 兼容旧字段
    })
  }

  // 如果没找到，在 breakingSentences 中查找（断句）
  index = mockTask.breakingSentences.findIndex(
    (item) => matchId(item.breakingSentenceId, sentenceId)
  )

  if (index !== -1) {
    const audioUrl = `https://cdn.example.com/mock/${sentenceId}.mp3`
    mockTask.breakingSentences[index].audioUrl = audioUrl
    mockTask.breakingSentences[index].synthesisStatus = 2 // 已合成
    mockTask.breakingSentences[index].audioDuration = Math.max(3, Math.floor(mockTask.breakingSentences[index].content.length / 4))
    return mockResponse({
      audioUrl,
      audio_url: audioUrl // 兼容旧字段
    })
  }

  throw new Error('未找到句子')
}

// 获取音色列表
export const mockGetVoiceList = async () => {
  await delay(300)
  return mockResponse({
    list: [
      {
        voiceId: 'default',
        voiceName: '唐瑶',
        voiceType: 'news',
        sortOrder: 1,
        avatar_url: '', // api.md: 使用 avatar_url
        language: 'zh-CN',
        isRecommended: 1
      },
      {
        voiceId: 'female1',
        voiceName: '果子',
        voiceType: 'children',
        sortOrder: 2,
        avatar_url: '', // api.md: 使用 avatar_url
        language: 'zh-CN',
        isRecommended: 0
      },
      {
        voiceId: 'male1',
        voiceName: '杨笙',
        voiceType: 'image',
        sortOrder: 3,
        avatar_url: '', // api.md: 使用 avatar_url
        language: 'zh-CN',
        isRecommended: 0
      }
    ]
  })
}

