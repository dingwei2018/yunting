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
  task_id: 'mock-task-001',
  content: DEFAULT_TEXT,
  char_count: DEFAULT_TEXT.length,
  status: 2,
  merged_audio_url: '',
  merged_audio_duration: null,
  sentences: []
}

let sentenceSeq = 1

const buildSentence = (content, parentId = 0) => ({
  sentence_id: `mock-s-${sentenceSeq++}`,
  content,
  duration: Math.max(3, Math.floor(content.length / 4)),
  audio_url: '',
  voice: 'female1',
  volume: 70,
  speed: 0,
  pitch: 50,
  parent_id: parentId,
  content_type: parentId === 0 ? 'original' : 'insert',
  display_order: sentenceSeq
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
  mockTask.sentences = splitContent(content).map((sentence) =>
    buildSentence(sentence, 0)
  )
}

// 初始化一次
rebuildSentences(DEFAULT_TEXT)

const mockResponse = async (data) => {
  await delay()
  return data
}

export const mockCreateTask = async ({ content }) => {
  mockTask = {
    ...mockTask,
    task_id: `mock-task-${Date.now()}`,
    content,
    char_count: content.length
  }
  rebuildSentences(content)
  return mockResponse({
    task_id: mockTask.task_id,
    char_count: mockTask.char_count
  })
}

export const mockGetTask = async () => {
  return mockResponse({ ...mockTask })
}

export const mockGetTaskSentences = async () => {
  return mockResponse({
    task_id: mockTask.task_id,
    sentences: mockTask.sentences.map((item) => ({ ...item }))
  })
}

export const mockMergeAudio = async () => {
  mockTask.merged_audio_url =
    mockTask.merged_audio_url ||
    'https://cdn.example.com/mock/merged_audio_mock.mp3'
  return mockResponse({
    merged_audio_url: mockTask.merged_audio_url
  })
}

export const mockGetSentence = async (sentenceId) => {
  const sentence = mockTask.sentences.find(
    (item) => item.sentence_id === sentenceId
  )
  if (!sentence) throw new Error('未找到句子')
  return mockResponse({ ...sentence })
}

export const mockUpdateSentence = async (sentenceId, data) => {
  const index = mockTask.sentences.findIndex(
    (item) => item.sentence_id === sentenceId
  )
  if (index === -1) throw new Error('未找到句子')
  mockTask.sentences[index] = {
    ...mockTask.sentences[index],
    ...data
  }
  return mockResponse({ success: true })
}

export const mockDeleteSentence = async (sentenceId) => {
  mockTask.sentences = mockTask.sentences.filter(
    (item) => item.sentence_id !== sentenceId
  )
  return mockResponse({ success: true })
}

export const mockInsertSentenceAfter = async (sentenceId, { content }) => {
  const index = mockTask.sentences.findIndex(
    (item) => item.sentence_id === sentenceId
  )
  if (index === -1) throw new Error('未找到句子')
  const parentId =
    mockTask.sentences[index].parent_id === 0
      ? mockTask.sentences[index].sentence_id
      : mockTask.sentences[index].parent_id
  const newSentence = buildSentence(content, parentId)
  mockTask.sentences.splice(index + 1, 0, newSentence)
  return mockResponse({ ...newSentence })
}

export const mockSynthesizeSentence = async (sentenceId) => {
  const index = mockTask.sentences.findIndex(
    (item) => item.sentence_id === sentenceId
  )
  if (index === -1) throw new Error('未找到句子')
  mockTask.sentences[index].audio_url = `https://cdn.example.com/mock/${sentenceId}.mp3`
  return mockResponse({
    audio_url: mockTask.sentences[index].audio_url
  })
}

