import { createSubSentence } from '@/models/sentenceModels'

/**
 * 句子编辑操作 composable
 * 包含插入、拆分等操作逻辑
 */
export function useSentenceEditorOperations({
  sentences,
  findSentenceById,
  generateNewSentenceId,
  originalSentenceListData
}) {
  /**
   * 清空 originalSentenceListData 中对应拆句的 breakingSentenceList
   * @private
   * @param {number} rootSentenceId - 拆句的 ID
   */
  const clearBreakingSentenceList = (rootSentenceId) => {
    if (!originalSentenceListData) return
    
    const listData = originalSentenceListData.value
    const sentenceList = listData?.list || listData?.data?.list
    if (sentenceList && Array.isArray(sentenceList)) {
      const originalSentence = sentenceList.find(
        os => os.originalSentenceId == rootSentenceId || String(os.originalSentenceId) === String(rootSentenceId)
      )
      if (originalSentence && Array.isArray(originalSentence.breakingSentenceList)) {
        // 清空 breakingSentenceList
        originalSentence.breakingSentenceList = []
      }
    }
  }

  /**
   * 计算插入位置
   * @private
   * @param {number} clickedSentenceIndex - 点击的断句在 sentences.value 中的位置
   * @param {number} parentId - 拆句的 ID
   * @param {Object} clickedSentence - 点击的断句对象
   * @param {Object|null} nextBreakingSentence - 下一个断句对象（如果存在）
   * @returns {number} - 插入位置
   */
  const calculateInsertIndex = (clickedSentenceIndex, parentId, clickedSentence, nextBreakingSentence) => {
    let insertIndex
    
    if (nextBreakingSentence) {
      // 有下一个断句：新断句应该插入在点击的断句之后，下一个断句之前
      const nextSentenceIndex = sentences.value.findIndex(
        item => item.sentence_id === nextBreakingSentence.sentence_id
      )
      
      if (nextSentenceIndex === -1) {
        throw new Error('无法找到下一个断句在 sentences.value 中的位置')
      }
      
      insertIndex = nextSentenceIndex
    } else {
      // 没有下一个断句：找到该拆句的最后一个断句，在该位置之后插入
      let lastIndex = clickedSentenceIndex
      for (let i = clickedSentenceIndex + 1; i < sentences.value.length; i++) {
        const item = sentences.value[i]
        // 如果遇到其他拆句，停止查找
        if (item.parent_id === 0 || (item.parent_id !== parentId && item.parent_id !== 0)) {
          break
        }
        // 如果遇到相同 parent_id 的断句，更新 lastIndex
        if (item.parent_id === parentId) {
          lastIndex = i
        }
      }
      insertIndex = lastIndex + 1
    }

    return insertIndex
  }

  /**
   * 重新整理所有相同 parent_id 的断句的 display_order
   * @private
   * @param {number} parentId - 拆句的 ID
   */
  const reorderDisplayOrder = (parentId) => {
    // 找到拆句在 sentences.value 中的位置
    const rootSentenceIndex = sentences.value.findIndex(
      item => item.sentence_id === parentId && (item.parent_id === 0 || !item.parent_id)
    )
    
    // 找到所有相同 parent_id 的断句，按照在 sentences.value 中的实际位置排序
    const breakingSentencesAfterInsert = []
    
    // 从拆句位置之后开始查找，收集所有相同 parent_id 的断句
    for (let i = rootSentenceIndex + 1; i < sentences.value.length; i++) {
      const item = sentences.value[i]
      // 如果遇到其他拆句，停止
      if (item.parent_id === 0 || (item.parent_id !== parentId && item.parent_id !== 0)) {
        break
      }
      // 如果遇到相同 parent_id 的断句，添加到数组中
      if (item.parent_id === parentId) {
        breakingSentencesAfterInsert.push({
          sentence: item,
          index: i
        })
      }
    }
    
    // 按照在 sentences.value 中的位置重新分配 display_order（从 0 开始，连续递增）
    breakingSentencesAfterInsert.forEach(({ sentence, index }, orderIndex) => {
      sentences.value[index].display_order = orderIndex
    })
  }

  /**
   * 向下插入断句的数据处理
   * 
   * 业务逻辑：
   * 1. 只能从断句点击（拆句没有这个按钮）
   * 2. 新断句的 parent_id 永远使用当前拆句的 ID（即点击的断句的 parent_id）
   * 3. 新断句插入在点击的断句下方（按 display_order 排序后的下一个位置）
   * 4. 插入后重新整理所有断句的 display_order（从 0 开始连续递增）
   * 5. 新断句的 ID 使用统一的 ID 生成器（-1, -2, -3...）
   * 
   * @param {number|string} sentenceId - 点击的断句 ID
   * @param {string} content - 新断句的内容
   * @returns {Object|null} - 返回新创建的断句对象，失败返回 null
   */
  const insertAfter = (sentenceId, content) => {
    // 步骤1：验证点击的句子是否存在且是断句
    const clickedSentence = findSentenceById(sentenceId)
    if (!clickedSentence) {
      throw new Error('无法找到目标句子')
    }
    
    // 只能从断句插入，不能从拆句插入（拆句的 parent_id 为 0 或不存在）
    if (!clickedSentence.parent_id || clickedSentence.parent_id === 0) {
      throw new Error('只能从断句插入，不能从拆句插入')
    }
    
    // 步骤2：确定新断句的 parent_id（永远使用拆句的 ID）
    const parentId = clickedSentence.parent_id
    
    // 验证 parentId 是否有效
    if (!parentId || parentId === 0) {
      throw new Error('无法确定新断句的 parent_id')
    }

    // 步骤3：找到所有相同 parent_id 的断句，按 display_order 排序
    const allBreakingSentences = sentences.value
      .filter(item => item.parent_id === parentId && item.parent_id !== 0)
      .sort((a, b) => (a.display_order || 0) - (b.display_order || 0))
    
    // 步骤4：找到点击的断句在排序后的数组中的位置
    const clickedIndex = allBreakingSentences.findIndex(
      item => item.sentence_id === sentenceId
    )
    
    if (clickedIndex === -1) {
      throw new Error('无法找到点击的断句在排序数组中的位置')
    }

    // 步骤5：计算新断句的 display_order（在点击的断句后面）
    const newDisplayOrder = clickedIndex + 1

    // 步骤6：找到下一个断句（在排序后的数组中，基于 display_order）
    const nextBreakingSentence = allBreakingSentences[clickedIndex + 1]
    
    // 步骤7：计算在 sentences.value 中的插入位置
    const clickedSentenceIndex = sentences.value.findIndex(
      item => item.sentence_id === sentenceId
    )
    
    if (clickedSentenceIndex === -1) {
      throw new Error('无法找到点击的断句在 sentences.value 中的位置')
    }
    
    const insertIndex = calculateInsertIndex(
      clickedSentenceIndex,
      parentId,
      clickedSentence,
      nextBreakingSentence
    )

    // 步骤8：使用统一的 ID 生成器生成新断句的 ID
    const tempId = generateNewSentenceId()
    
    // 步骤9：创建新断句
    const newSentence = createSubSentence({
      sentence_id: tempId,
      parent_id: parentId,
      content: content,
      display_order: newDisplayOrder, // 临时设置，后面会重新整理
      voice: 'default',
      volume: 33, // 默认值33对应接口的140（接口默认值）
      speed: 0,
      pitch: 50,
      pauseMarkers: [],
      polyphonicOverrides: [],
      speedSegments: [],
      silenceMarkers: [],
      readingRules: []
    })

    // 步骤10：插入到正确位置
    sentences.value.splice(insertIndex, 0, newSentence)

    // 步骤11：重新整理所有相同 parent_id 的断句的 display_order
    reorderDisplayOrder(parentId)

    return newSentence
  }

  /**
   * 按标点符号拆分断句
   * 
   * 业务逻辑：
   * 1. 清空当前拆句的所有断句（包括嵌套的子句子）
   * 2. 清空 originalSentenceListData 中对应拆句的 breakingSentenceList
   * 3. 创建一个新的子句子，内容是父句子的内容，作为"输入文本1"
   * 
   * @param {Object} rootSentence - 拆句对象
   * @param {string} originalText - 原始文本
   * @param {Function} insertAfterLocal - 插入句子的函数
   * @returns {Object|null} - 返回新创建的断句对象，失败返回 null
   */
  const splitByPunctuation = (rootSentence, originalText, insertAfterLocal) => {
    // 第一步：一次性清空所有输入文本（所有子句子）
    const getAllChildrenIds = (parentId) => {
      const directChildren = sentences.value.filter(
        (item) => item.parent_id === parentId
      )
      const allIds = directChildren.map(sub => sub.sentence_id)
      // 递归获取子句子的子句子
      directChildren.forEach(sub => {
        const nestedIds = getAllChildrenIds(sub.sentence_id)
        allIds.push(...nestedIds)
      })
      return allIds
    }
    
    // 获取所有需要删除的子句子ID（包括嵌套的）
    const deleteIds = getAllChildrenIds(rootSentence.sentence_id)
    
    // 只在前端清理数据：从 sentences.value 中移除所有子句子（保留父句子）
    sentences.value = sentences.value.filter(
      (item) => item.sentence_id === rootSentence.sentence_id || !deleteIds.includes(item.sentence_id)
    )

    // 清空 originalSentenceListData 中对应拆句的 breakingSentenceList
    clearBreakingSentenceList(rootSentence.sentence_id)

    // 第二步：创建一个新的子句子，内容是父句子的内容，作为"输入文本1"
    if (!insertAfterLocal) {
      throw new Error('insertAfterLocal 函数未提供，无法创建子句子')
    }
    
    const newSentence = insertAfterLocal(rootSentence.sentence_id, {
      content: originalText,
      parent_id: rootSentence.sentence_id,
      display_order: 0
    })

    return newSentence
  }

  /**
   * 按字符数拆分断句
   * 
   * 业务逻辑：
   * 1. 按字符数拆分文本，保持原始拆句字符的顺序
   * 2. 清空当前拆句的所有断句（只清空直接子句子）
   * 3. 清空 originalSentenceListData 中对应拆句的 breakingSentenceList
   * 4. 按顺序创建所有新断句，按照 display_order 插入到正确位置
   * 
   * @param {Object} rootSentence - 拆句对象
   * @param {string} originalText - 原始文本
   * @param {number} charCount - 字符数
   * @returns {Array} - 返回新创建的断句对象数组
   */
  const splitByCharCount = (rootSentence, originalText, charCount) => {
    if (!originalText) {
      throw new Error('原始拆句文本为空')
    }

    // 按字符数拆分文本，保持原始拆句字符的顺序
    const chunks = []
    for (let i = 0; i < originalText.length; i += charCount) {
      chunks.push(originalText.slice(i, i + charCount))
    }

    if (chunks.length === 0) {
      throw new Error('拆分结果为空')
    }

    const chunksCount = chunks.length

    // 第一步：清空当前拆句的所有断句（只清空直接子句子，不包括嵌套的子句子）
    const currentBreakingSentenceIds = sentences.value
      .filter(item => item.parent_id === rootSentence.sentence_id)
      .map(item => item.sentence_id)
    
    // 只在前端清理数据：从 sentences.value 中移除当前拆句的所有断句
    sentences.value = sentences.value.filter(
      (item) => !currentBreakingSentenceIds.includes(item.sentence_id)
    )

    // 清空 originalSentenceListData 中对应拆句的 breakingSentenceList
    clearBreakingSentenceList(rootSentence.sentence_id)

    // 第二步：按原始拆句字符的顺序创建新的断句
    const newSentences = []
    
    // 由于 internalBreakingSentences 只包含断句，新断句直接追加到数组末尾即可
    const insertIndex = sentences.value.length
    
    // 按顺序创建所有新断句，直接插入到正确位置
    for (let i = 0; i < chunksCount; i++) {
      // 使用统一的 ID 生成器，确保新增 ID 的唯一性（-1, -2, -3...）
      const tempId = generateNewSentenceId()
      
      const newSentence = createSubSentence({
        sentence_id: tempId,
        parent_id: rootSentence.sentence_id,
        content: chunks[i],
        display_order: i, // 明确设置顺序，确保与原始拆句字符顺序一致
        voice: 'default',
        volume: 33, // 默认值33对应接口的140（接口默认值）
        speed: 0,
        pitch: 50,
        pauseMarkers: [],
        polyphonicOverrides: [],
        speedSegments: [],
        silenceMarkers: [],
        readingRules: []
      })

      // 插入到正确位置
      sentences.value.splice(insertIndex + i, 0, newSentence)
      newSentences.push(newSentence)
    }

    return newSentences
  }

  return {
    insertAfter,
    splitByPunctuation,
    splitByCharCount
  }
}

