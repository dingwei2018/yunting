<template>
  <div :class="editorClasses" v-if="editor" @focusin="handleFocusin">
    <EditorContent :editor="editor" class="rich-text-editor__content" />
  </div>
</template>

<script setup>
import {
  onBeforeUnmount,
  watch,
  defineProps,
  defineEmits,
  defineExpose,
  nextTick,
  ref,
  computed
} from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import PauseExtension, { PauseNodeName, DEFAULT_PAUSE_DURATION } from './extensions/pause'
import SilenceExtension, { SilenceNodeName } from './extensions/silence'
import PolyphonicMarkersExtension from './extensions/polyphonicMarkers'
import SpeedSpanExtension, { SpeedSpanNodeName } from './extensions/speedSpan'
import ReadingRulesExtension from './extensions/readingRules'

const PAUSE_TOKEN_REGEX = /<pause(?::([\d.]+))?>/g
const SILENCE_PLACEHOLDER_REGEX = /<silence:([\d.]+)>/g
const PAUSE_SPAN_REGEX = /<span[^>]*data-pause="([^"]*)"[^>]*><\/span>/g

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  autofocus: {
    type: Boolean,
    default: false
  },
  polyphonicMarkers: {
    type: Array,
    default: () => []
  },
  showPolyphonicHints: {
    type: Boolean,
    default: false
  },
  isActive: {
    type: Boolean,
    default: false
  },
  speedSegments: {
    type: Array,
    default: () => []
  },
  readingRules: {
    type: Array,
    default: () => []
  }
})

const editorClasses = computed(() => [
  'rich-text-editor',
  {
    'is-active': props.isActive
  }
])

const emit = defineEmits([
  'update:modelValue',
  'selectionChange',
  'contentChange',
  'polyphonicHover',
  'focus',
  'speedSegmentsChange',
  'readingRuleHover',
  'readingRuleToggle'
])

const markerMap = ref(new Map())
const lastAppliedSpeedSegments = ref('[]')
let isSyncingSpeedSegments = false
let isInternalUpdate = false // 标志：是否正在内部更新（避免循环）

const mapLeafNodeToText = (node) => {
  if (!node) return ''
  if (node.type?.name === PauseNodeName) {
    const duration = node.attrs?.duration || DEFAULT_PAUSE_DURATION
    return `<pause:${duration}>`
  }
  if (node.type?.name === SilenceNodeName) {
    const duration = node.attrs?.duration || '0'
    return `<silence:${duration}>`
  }
  return ''
}

const getPlainTextBetween = (from, to) => {
  if (!editor?.value) return ''
  
  const result = editor.value.state.doc.textBetween(
    from,
    to,
    '\n',
    '\n',
    (node) => mapLeafNodeToText(node)
  )
  
  return result
}

/**
 * 获取去除换行符后的纯文本（用于局部变速标记的位置计算）
 * 因为局部变速标记内部的 <p> 标签会产生换行符，但用户选中的文本是连续的
 */
const getPlainTextWithoutNewlinesBetween = (from, to) => {
  const text = getPlainTextBetween(from, to)
  return text.replace(/\n/g, '')
}

const serialize = (value = '') => {
  const normalizeDuration = (duration) => {
    const num = Number(duration)
    if (!Number.isFinite(num) || num < 0) {
      return DEFAULT_PAUSE_DURATION
    }
    return num.toFixed(1)
  }

  if (!value) {
    return '<p></p>'
  }
  // TipTap 编辑器需要 <p> 标签作为段落节点，这是必需的
  // 问题不在于添加 <p> 标签，而在于 deserialize 需要正确处理嵌套的 <p> 标签
  const paragraphs = value.split('\n').map((line) => {
    if (!line) return '<p><br /></p>'
    const htmlLine = line
      .replace(PAUSE_TOKEN_REGEX, (_, duration = DEFAULT_PAUSE_DURATION) => {
        const safeDuration = normalizeDuration(duration || DEFAULT_PAUSE_DURATION)
        return `<span data-pause="${safeDuration}"></span>`
      })
      .replace(SILENCE_PLACEHOLDER_REGEX, (_, duration = '0') => {
        const safeDuration = duration || '0'
        return `<span data-silence="${safeDuration}"></span>`
      })
    return `<p>${htmlLine || '<br />'}</p>`
  })
  return paragraphs.join('')
}

const deserialize = (html) => {
  if (!html) return ''
  
  const tmp = document.createElement('div')
  tmp.innerHTML = html
  
  // 先移除所有 <p> 标签，但保留其内容
  // 因为原始内容不应该有 <p> 标签，这些是 TipTap 自动添加的
  // 先移除 <p> 可以简化 HTML 结构，避免后续处理嵌套的复杂情况
  tmp.querySelectorAll('p').forEach((p) => {
    const parent = p.parentNode
    if (!parent) return
    
    // 将 <p> 标签内的所有内容移到父节点
    const fragment = document.createDocumentFragment()
    while (p.firstChild) {
      fragment.appendChild(p.firstChild)
    }
    // 在 <p> 标签位置插入内容
    parent.insertBefore(fragment, p)
    parent.removeChild(p)
  })
  
  // 再移除语速标签，但保留其内容
  // 现在 HTML 结构已经简化，不需要处理嵌套的 <p> 标签
  const removeSpeedSpans = (container) => {
    const speedSpans = container.querySelectorAll('[data-speed-span]')
    speedSpans.forEach((node) => {
      const parent = node.parentNode
      if (!parent) return
      
      // 将节点内的所有子节点移到父节点
      const fragment = document.createDocumentFragment()
      while (node.firstChild) {
        fragment.appendChild(node.firstChild)
      }
      parent.insertBefore(fragment, node)
      parent.removeChild(node)
    })
  }
  
  // 递归移除所有语速标签
  removeSpeedSpans(tmp)
  
  // 处理停顿和静音标记，将它们转换为文本标记
  const pauseNodes = tmp.querySelectorAll('[data-pause]')
  const silenceNodes = tmp.querySelectorAll('[data-silence]')
  
  pauseNodes.forEach((node) => {
    const duration = node.getAttribute('data-pause') || DEFAULT_PAUSE_DURATION
    const marker = document.createTextNode(`<pause:${duration}>`)
    node.parentNode?.replaceChild(marker, node)
  })
  
  silenceNodes.forEach((node) => {
    const duration = node.getAttribute('data-silence') || '0'
    const marker = document.createTextNode(`<silence:${duration}>`)
    node.parentNode?.replaceChild(marker, node)
  })
  
  // 直接使用 textContent 提取所有文本内容
  // 这样可以获取所有文本，包括不在 <p> 标签内的文本节点
  let textContent = tmp.textContent || tmp.innerText || ''
  
  // 将 &nbsp; 转换为空格
  textContent = textContent.replace(/&nbsp;/g, ' ')
  
  // 恢复停顿和静音标记（如果它们被移除了）
  textContent = textContent
    .replace(/<pause:([\d.]+)>/g, '<pause:$1>')
    .replace(/<silence:([\d.]+)>/g, '<silence:$1>')
  
  // 处理换行：将连续的空白字符（包括换行）规范化
  // 但保留段落之间的换行（通过 <p> 标签分隔的内容）
  const result = textContent.trim()
  
  return result
}

const editor = useEditor({
  extensions: [
    StarterKit.configure({
      history: false
    }),
    PauseExtension,
    SilenceExtension,
    PolyphonicMarkersExtension,
    SpeedSpanExtension,
    ReadingRulesExtension
  ],
  autofocus: props.autofocus,
  content: serialize(props.modelValue),
  onUpdate({ editor }) {
    // 如果是内部更新（通过 setContent），不触发 emit，避免循环
    if (isInternalUpdate) return
    
    const html = editor.getHTML()
    // 打印编辑器内的 HTML 内容
    console.log('[编辑器 HTML 内容]', html)
    
    const asString = deserialize(html)
    
    emit('update:modelValue', asString)
    emit('contentChange', asString)
    if (!isSyncingSpeedSegments) {
      const segments = collectSpeedSegments()
      emit('speedSegmentsChange', segments)
      lastAppliedSpeedSegments.value = JSON.stringify(normalizeSpeedSegments(segments))
    }
  },
  onSelectionUpdate({ editor }) {
    const { from, to } = editor.state.selection
    const before = getPlainTextBetween(0, from)
    const selected = getPlainTextBetween(from, to)
    emit('selectionChange', {
      hasTextBefore: before.length > 0,
      hasSelection: to > from && selected.length > 0,
      selectionRange: {
        docFrom: from,
        docTo: to,
        plainFrom: before.length,
        plainTo: before.length + selected.length,
        length: selected.length
      }
    })
  },
  onFocus() {
    emit('focus')
  }
})

const handleFocusin = () => {
  emit('focus')
}

const getRenderableMarkers = () => {
  const showPending = props.showPolyphonicHints
  return (props.polyphonicMarkers || []).filter(
    (marker) => marker.selected || showPending
  )
}

const resolveDocRange = (marker) => {
  if (!editor?.value) return null
  
  console.log('[解析文档范围] ========== 开始解析 ==========')
  console.log('  marker:', JSON.stringify(marker))
  console.log('  offset:', marker.offset, 'length:', marker.length)
  
  const targetOffset = marker.offset ?? 0
  const targetLength = marker.length ?? 1
  const targetEnd = targetOffset + targetLength
  
  const doc = editor.value.state.doc
  let foundFrom = null
  let foundTo = null
  const docSize = doc.content.size
  
  // 先获取完整文档的纯文本（用于对比）
  const fullPlainText = getPlainTextWithoutNewlinesBetween(0, doc.content.size)
  const fullPlainTextWithNewlines = getPlainTextBetween(0, doc.content.size)
  console.log('[解析文档范围] 文档信息')
  console.log('  docSize:', docSize)
  console.log('  fullPlainText (去除换行):', fullPlainText)
  console.log('  fullPlainTextLength (去除换行):', fullPlainText.length)
  console.log('  fullPlainText (包含换行):', fullPlainTextWithNewlines)
  console.log('  fullPlainTextLength (包含换行):', fullPlainTextWithNewlines.length)
  console.log('  targetOffset:', targetOffset, 'targetEnd:', targetEnd)
  console.log('  expectedText:', fullPlainText.substring(targetOffset, targetEnd))
  
  // 使用去除换行符后的纯文本长度来匹配位置
  let lastBeforeLength = -1
  for (let pos = 0; pos <= docSize; pos++) {
    const beforeText = getPlainTextWithoutNewlinesBetween(0, pos)
    const beforeLength = beforeText.length
    
    // 记录关键位置的变化
    if (pos % 10 === 0 || beforeLength !== lastBeforeLength) {
      console.log(`[解析文档范围] 位置 ${pos}: beforeLength=${beforeLength}, targetOffset=${targetOffset}`)
      lastBeforeLength = beforeLength
    }
    
    if (beforeLength === targetOffset && foundFrom === null) {
      foundFrom = pos
      console.log(`[解析文档范围] ✓ 找到起始位置: pos=${pos}, beforeLength=${beforeLength}`)
      
      // 继续查找结束位置
      for (let endPos = pos; endPos <= docSize; endPos++) {
        const endBeforeText = getPlainTextWithoutNewlinesBetween(0, endPos)
        const endBeforeLength = endBeforeText.length
        
        if (endBeforeLength >= targetEnd) {
          foundTo = endPos
          console.log(`[解析文档范围] ✓ 找到结束位置: pos=${endPos}, endBeforeLength=${endBeforeLength}, targetEnd=${targetEnd}`)
          break
        }
      }
      
      if (foundTo === null) {
        foundTo = docSize
        console.log(`[解析文档范围] ⚠ 未找到结束位置，使用文档末尾: ${docSize}`)
      }
      
      break
    }
    
    if (beforeLength > targetOffset) {
      console.log(`[解析文档范围] ✗ 超过目标位置: pos=${pos}, beforeLength=${beforeLength} > targetOffset=${targetOffset}`)
      break
    }
  }

  if (foundFrom == null || foundTo == null) {
    console.error('[解析文档范围] ✗ 无法找到文档位置', {
      marker: marker,
      targetOffset: targetOffset,
      targetLength: targetLength,
      docSize: docSize,
      foundFrom: foundFrom,
      foundTo: foundTo
    })
    return null
  }
  
  const resolvedRange = { from: foundFrom, to: foundTo }
  const actualText = getPlainTextWithoutNewlinesBetween(foundFrom, foundTo)
  const actualTextWithNewlines = getPlainTextBetween(foundFrom, foundTo)
  
  console.log('[解析文档范围] ========== 解析完成 ==========')
  console.log('  resolvedRange:', resolvedRange)
  console.log('  actualText (去除换行):', actualText, 'length:', actualText.length)
  console.log('  actualText (包含换行):', actualTextWithNewlines, 'length:', actualTextWithNewlines.length)
  console.log('  expectedText:', fullPlainText.substring(targetOffset, targetEnd), 'length:', targetLength)
  console.log('  匹配结果:', actualText === fullPlainText.substring(targetOffset, targetEnd) ? '✓ 匹配' : '✗ 不匹配')
  
  return resolvedRange
}

const updatePolyphonicDecorations = () => {
  if (!editor?.value) return
  const markers = getRenderableMarkers()
  if (!markers.length) {
    markerMap.value = new Map()
    editor.value.chain().setPolyphonicMarkers([]).run()
    return
  }

  const mapped = markers
    .map((marker) => {
      const range = resolveDocRange(marker)
      if (!range) return null
      return { ...marker, ...range }
    })
    .filter(Boolean)

  markerMap.value = new Map(mapped.map((item) => [item.id, item]))
  editor.value.chain().setPolyphonicMarkers(mapped).run()
}

const schedulePolyphonicUpdate = () => {
  if (!editor?.value) return
  nextTick(() => {
    updatePolyphonicDecorations()
  })
}

// 更新阅读规则标记
const updateReadingRuleMarkers = () => {
  if (!editor?.value || !props.readingRules?.length) {
    editor.value?.chain().clearReadingRuleMarkers().run()
    return
  }

  const plainText = getPlainTextBetween(0, editor.value.state.doc.content.size)
  
  const markers = props.readingRules.map((rule) => {
    const pattern = rule.pattern || ''
    if (!pattern) return null
    
    const range = resolveDocRange({
      offset: 0,
      length: plainText.length
    })
    
    if (!range) return null
    
    // 在纯文本中查找匹配的位置
    const patternIndex = plainText.indexOf(pattern)
    if (patternIndex === -1) return null
    
    // 计算文档中的位置
    let docFrom = range.from
    let docTo = range.to
    
    // 简化处理：如果匹配在文本开头，直接使用范围
    if (patternIndex === 0) {
      // 需要找到 pattern 在文档中的实际位置
      const beforeText = getPlainTextBetween(0, docFrom)
      const actualFrom = beforeText.length
      const actualTo = actualFrom + pattern.length
      
      // 重新计算文档位置
      const actualRange = resolveDocRange({
        offset: actualFrom,
        length: pattern.length
      })
      
      if (!actualRange) return null
      
      return {
        ruleId: rule.ruleId || rule.rule_id || '',
        pattern: pattern,
        from: actualRange.from,
        to: actualRange.to,
        applied: rule.applied !== false // 默认为 true，除非明确设置为 false
      }
    }
    
    return null
  }).filter(Boolean)

  editor.value.chain().setReadingRuleMarkers(markers).run()
}

const clampLocalSpeed = (value) => {
  const num = Number(value)
  if (Number.isNaN(num)) return 0
  if (num > 10) return 10
  if (num < -10) return -10
  return Math.round(num)
}

const normalizeSpeedSegments = (segments = []) => {
  console.log('[规范化语速段落] 开始规范化')
  console.log('  inputSegments:', segments)
  console.log('  inputSegmentsCount:', segments?.length || 0)
  
  const filtered = (segments || [])
    .filter((item, index) => {
      if (typeof item !== 'object') {
        console.log(`[规范化语速段落] 过滤第 ${index + 1} 项（不是对象）`, { item })
        return false
      }
      // 支持两种格式：{ offset, length, speed } 或 { begin, end, speed }
      const hasOffsetLength = typeof item.offset === 'number' && typeof item.length === 'number' && item.length > 0
      const hasBeginEnd = typeof item.begin === 'number' && typeof item.end === 'number' && item.end > item.begin
      const isValid = hasOffsetLength || hasBeginEnd
      
      if (!isValid) {
        console.log(`[规范化语速段落] 过滤第 ${index + 1} 项（格式无效）`, {
          item,
          hasOffsetLength,
          hasBeginEnd
        })
      }
      
      return isValid
    })
  
  console.log('[规范化语速段落] 过滤后')
  console.log('  filteredCount:', filtered.length)
  console.log('  filtered:', filtered)
  
  const normalized = filtered.map((item, index) => {
      // 如果已经是 { offset, length, speed } 格式，直接使用
      if (typeof item.offset === 'number' && typeof item.length === 'number') {
        const result = {
      id: item.id || `speed-${index}-${Date.now()}`,
      offset: Math.max(0, Math.floor(item.offset)),
      length: Math.max(1, Math.floor(item.length)),
      speed: clampLocalSpeed(item.speed)
        }
        console.log(`[规范化语速段落] 第 ${index + 1} 项（offset/length 格式）`)
        console.log('  original:', item)
        console.log('  result:', result)
        return result
      }
      
      // 如果是 { begin, end, speed } 格式，转换为 { offset, length, speed }
      const begin = Math.max(0, Math.floor(item.begin || 0))
      const end = Math.max(begin + 1, Math.floor(item.end || 0))
      const result = {
        id: item.id || `speed-${index}-${Date.now()}`,
        offset: begin,
        length: end - begin,
        speed: clampLocalSpeed(item.speed)
      }
      
      console.log(`[规范化语速段落] 第 ${index + 1} 项（begin/end 格式）`)
      console.log('  original:', item)
      console.log('  begin:', begin, 'end:', end)
      console.log('  result:', result)
      
      return result
    })
  
  console.log('[规范化语速段落] 规范化完成')
  console.log('  normalizedCount:', normalized.length)
  console.log('  normalized:', normalized)
  
  return normalized
}

const clearSpeedSpans = () => {
  if (!editor?.value) return
  const positions = []
  editor.value.state.doc.descendants((node, pos) => {
    if (node.type.name === SpeedSpanNodeName) {
      positions.push(pos)
    }
  })
  positions
    .sort((a, b) => b - a)
    .forEach((pos) => {
      editor.value.commands.removeSpeedSpanAt(pos)
    })
}

const applySpeedSegments = (segments = []) => {
  if (!editor?.value) return
  
  console.log('[应用局部语速标记] 开始应用')
  console.log('  inputSegments:', segments)
  console.log('  inputSegmentsCount:', segments.length)
  console.log('  lastApplied:', lastAppliedSpeedSegments.value)
  
  const normalized = normalizeSpeedSegments(segments)
  
  console.log('[应用局部语速标记] 规范化后')
  console.log('  normalized:', normalized)
  console.log('  normalizedCount:', normalized.length)
  
  const serialized = JSON.stringify(normalized)
  if (serialized === lastAppliedSpeedSegments.value) {
    console.log('[应用局部语速标记] 跳过应用（内容未变化）')
    return
  }
  
  // 先计算所有标记的位置（在清除标记之前）
  const fullPlainText = getPlainTextWithoutNewlinesBetween(0, editor.value.state.doc.content.size)
  const fullPlainTextWithNewlines = getPlainTextBetween(0, editor.value.state.doc.content.size)
  console.log('[应用局部语速标记] ========== 开始应用 ==========')
  console.log('[应用局部语速标记] 当前文档状态（清除前）')
  console.log('  docSize:', editor.value.state.doc.content.size)
  console.log('  fullPlainText (去除换行):', fullPlainText)
  console.log('  fullPlainTextLength (去除换行):', fullPlainText.length)
  console.log('  fullPlainText (包含换行):', fullPlainTextWithNewlines)
  console.log('  fullPlainTextLength (包含换行):', fullPlainTextWithNewlines.length)
  console.log('  normalizedCount:', normalized.length)
  console.log('  normalized:', JSON.stringify(normalized, null, 2))
  
  // 先解析所有标记的文档范围（在清除标记之前）
  const rangesToApply = []
  for (let i = 0; i < normalized.length; i++) {
    const segment = normalized[i]
    console.log(`[应用局部语速标记] ========== 预处理第 ${i + 1}/${normalized.length} 个标记 ==========`)
    console.log('  segment:', JSON.stringify(segment))
    console.log('  offset:', segment.offset, 'length:', segment.length)
    console.log('  expectedText:', fullPlainText.substring(segment.offset, segment.offset + segment.length))
    
    const range = resolveDocRange({
      offset: segment.offset,
      length: segment.length
    })
    
    if (!range) {
      console.error(`[应用局部语速标记] ✗ 无法解析文档范围，跳过`, {
        segment: segment
      })
      continue
    }
    
    const actualText = getPlainTextWithoutNewlinesBetween(range.from, range.to)
    const expectedText = fullPlainText.substring(segment.offset, segment.offset + segment.length)
    console.log(`[应用局部语速标记] 预处理结果`)
    console.log('  range:', range)
    console.log('  actualText:', actualText, 'length:', actualText.length)
    console.log('  expectedText:', expectedText, 'length:', segment.length)
    console.log('  匹配结果:', actualText === expectedText ? '✓ 匹配' : '✗ 不匹配')
    
    rangesToApply.push({
      segment,
      range
    })
  }
  
  console.log(`[应用局部语速标记] 预处理完成，共 ${rangesToApply.length} 个标记待应用`)
  
  // 现在清除所有标记
  console.log('[应用局部语速标记] ========== 清除所有标记 ==========')
  isSyncingSpeedSegments = true
  clearSpeedSpans()
  
  // 重新获取清除后的文档纯文本
  const fullPlainTextAfterClear = getPlainTextWithoutNewlinesBetween(0, editor.value.state.doc.content.size)
  const fullPlainTextAfterClearWithNewlines = getPlainTextBetween(0, editor.value.state.doc.content.size)
  console.log('[应用局部语速标记] 清除标记后的文档状态')
  console.log('  docSize:', editor.value.state.doc.content.size)
  console.log('  fullPlainTextAfterClear (去除换行):', fullPlainTextAfterClear)
  console.log('  fullPlainTextAfterClearLength (去除换行):', fullPlainTextAfterClear.length)
  console.log('  fullPlainTextAfterClear (包含换行):', fullPlainTextAfterClearWithNewlines)
  console.log('  fullPlainTextAfterClearLength (包含换行):', fullPlainTextAfterClearWithNewlines.length)
  console.log('  文本变化:', fullPlainText === fullPlainTextAfterClear ? '✓ 无变化' : '✗ 有变化')
  
  // 重新计算所有标记的文档范围
  const finalRangesToApply = []
  for (let i = 0; i < rangesToApply.length; i++) {
    const { segment } = rangesToApply[i]
    console.log(`[应用局部语速标记] ========== 重新计算第 ${i + 1}/${rangesToApply.length} 个标记 ==========`)
    console.log('  segment:', JSON.stringify(segment))
    
    const range = resolveDocRange({
      offset: segment.offset,
      length: segment.length
    })
    
    if (!range) {
      console.error(`[应用局部语速标记] ✗ 重新计算后仍无法解析文档范围，跳过`, {
        segment: segment
      })
      continue
    }
    
    const actualText = getPlainTextWithoutNewlinesBetween(range.from, range.to)
    const expectedText = fullPlainTextAfterClear.substring(segment.offset, segment.offset + segment.length)
    console.log(`[应用局部语速标记] 重新计算结果`)
    console.log('  range:', range)
    console.log('  actualText:', actualText, 'length:', actualText.length)
    console.log('  expectedText:', expectedText, 'length:', segment.length)
    console.log('  匹配结果:', actualText === expectedText ? '✓ 匹配' : '✗ 不匹配')
    
    finalRangesToApply.push({
      segment,
      range
    })
  }
  
  console.log(`[应用局部语速标记] 重新计算完成，共 ${finalRangesToApply.length} 个标记待应用`)
  
  // 按 offset 从大到小排序，从后往前应用标记
  // 这样可以避免应用前面的标记时影响后面标记的位置
  const sortedRanges = [...finalRangesToApply].sort((a, b) => b.segment.offset - a.segment.offset)
  console.log('[应用局部语速标记] 排序后的标记（从后往前）')
  sortedRanges.forEach((item, index) => {
    console.log(`  ${index + 1}. offset=${item.segment.offset}, length=${item.segment.length}`)
  })
  
  // 应用所有标记（从后往前）
  console.log('[应用局部语速标记] ========== 开始应用标记（从后往前） ==========')
  for (let i = 0; i < sortedRanges.length; i++) {
    const { segment, range } = sortedRanges[i]
    console.log(`[应用局部语速标记] 应用第 ${i + 1}/${sortedRanges.length} 个标记（offset=${segment.offset}）`)
    console.log('  segment:', JSON.stringify(segment))
    console.log('  range:', range)
    
    const beforeApplyText = getPlainTextWithoutNewlinesBetween(range.from, range.to)
    console.log('  应用前选中文本:', beforeApplyText, 'length:', beforeApplyText.length)
    
    editor.value
      .chain()
      .focus()
      .setTextSelection({ from: range.from, to: range.to })
      .wrapSpeedSpan({ speed: segment.speed, uid: segment.id })
      .run()
    
    // 应用后立即检查
    const afterApplyText = getPlainTextWithoutNewlinesBetween(range.from, range.to)
    console.log('  应用后选中文本:', afterApplyText, 'length:', afterApplyText.length)
    console.log('  应用结果:', beforeApplyText === afterApplyText ? '✓ 文本一致' : '✗ 文本不一致')
    
    // 如果文本不一致，说明位置计算有问题，需要重新计算后续标记
    if (beforeApplyText !== afterApplyText) {
      console.warn(`[应用局部语速标记] ⚠ 标记应用后文本不一致，可能需要重新计算后续标记`)
    }
  }
  
  isSyncingSpeedSegments = false
  lastAppliedSpeedSegments.value = serialized
  
  console.log('[应用局部语速标记] 应用完成')
  console.log('  appliedCount:', finalRangesToApply.length)
  console.log('  lastApplied:', lastAppliedSpeedSegments.value)
}

const collectSpeedSegments = () => {
  if (!editor?.value) return []
  const segments = []
  const fullPlainText = getPlainTextWithoutNewlinesBetween(0, editor.value.state.doc.content.size)
  const fullPlainTextWithNewlines = getPlainTextBetween(0, editor.value.state.doc.content.size)
  
  console.log('[收集局部语速标记] ========== 开始收集 ==========')
  console.log('  docSize:', editor.value.state.doc.content.size)
  console.log('  fullPlainText (去除换行):', fullPlainText)
  console.log('  fullPlainTextLength (去除换行):', fullPlainText.length)
  console.log('  fullPlainText (包含换行):', fullPlainTextWithNewlines)
  console.log('  fullPlainTextLength (包含换行):', fullPlainTextWithNewlines.length)
  
  editor.value.state.doc.descendants((node, pos) => {
    if (node.type.name !== SpeedSpanNodeName) return true
    
    // 计算局部变速标记的文档范围
    const start = pos + 1
    const end = pos + node.nodeSize - 1
    
    // 获取原始文本（包含换行符）和去除换行符后的文本
    const beforeTextWithNewlines = getPlainTextBetween(0, start)
    const segmentTextWithNewlines = getPlainTextBetween(start, end)
    const beforeText = getPlainTextWithoutNewlinesBetween(0, start)
    const segmentText = getPlainTextWithoutNewlinesBetween(start, end)
    
    const offset = beforeText.length
    const length = segmentText.length
    
    const segment = {
      id: node.attrs?.uid || `speed-${offset}-${Date.now()}`,
      offset,
      length,
      speed: clampLocalSpeed(node.attrs?.speed)
    }
    
    console.log('[收集局部语速标记] ========== 发现标记 ==========')
    console.log('  nodePos:', pos)
    console.log('  start:', start, 'end:', end)
    console.log('  nodeSize:', node.nodeSize)
    console.log('  beforeText (包含换行):', beforeTextWithNewlines, 'length:', beforeTextWithNewlines.length)
    console.log('  beforeText (去除换行):', beforeText, 'length:', beforeText.length)
    console.log('  segmentText (包含换行):', segmentTextWithNewlines, 'length:', segmentTextWithNewlines.length)
    console.log('  segmentText (去除换行):', segmentText, 'length:', segmentText.length)
    console.log('  offset:', offset, 'length:', length)
    console.log('  speed:', segment.speed)
    console.log('  nodeAttrs:', node.attrs)
    console.log('  segment:', JSON.stringify(segment))
    console.log('  验证: fullPlainText.substring(offset, offset+length) =', fullPlainText.substring(offset, offset + length))
    console.log('  验证结果:', fullPlainText.substring(offset, offset + length) === segmentText ? '✓ 匹配' : '✗ 不匹配')
    
    segments.push(segment)
    return false
  })
  
  console.log('[收集局部语速标记] ========== 收集完成 ==========')
  console.log('  segmentsCount:', segments.length)
  console.log('  segments:', JSON.stringify(segments, null, 2))
  
  return segments
}

watch(
  () => [props.modelValue, props.polyphonicMarkers, props.showPolyphonicHints],
  () => {
    schedulePolyphonicUpdate()
  },
  { deep: true }
)

watch(
  () => [props.modelValue, props.readingRules],
  () => {
    if (editor?.value) {
      nextTick(() => {
        updateReadingRuleMarkers()
      })
    }
  },
  { deep: true }
)

watch(
  () => editor?.value,
  (instance) => {
    if (instance) {
      schedulePolyphonicUpdate()
      attachDomEvents()
      applySpeedSegments(props.speedSegments || [])
    } else {
      detachDomEvents()
    }
  }
)

const insertPause = (duration = DEFAULT_PAUSE_DURATION) => {
  if (!editor?.value) return
  // 设置内部更新标志，避免触发响应式更新循环
  isInternalUpdate = true
  try {
    editor.value
      .chain()
      .focus()
      .insertContent({
        type: PauseNodeName,
        attrs: {
          duration: (() => {
            const num = Number(duration)
            if (!Number.isFinite(num) || num < 0) {
              return DEFAULT_PAUSE_DURATION
            }
            return num.toFixed(1)
          })()
        }
      })
      .run()
    // 延迟重置标志，确保 NodeView 渲染完成
    setTimeout(() => {
      isInternalUpdate = false
    }, 100)
  } catch (error) {
    isInternalUpdate = false
    console.error('插入停顿失败:', error)
  }
}

const insertSilence = (duration = '0') => {
  if (!editor?.value) return
  // 设置内部更新标志，避免触发响应式更新循环
  isInternalUpdate = true
  try {
    editor.value
      .chain()
      .focus()
      .insertContent({
        type: SilenceNodeName,
        attrs: {
          duration: duration.toString()
        }
      })
      .run()
    // 延迟重置标志，确保 NodeView 渲染完成
    setTimeout(() => {
      isInternalUpdate = false
    }, 100)
  } catch (error) {
    isInternalUpdate = false
    console.error('插入静音失败:', error)
  }
}

const applyLocalSpeedRange = (from, to, speed, uid) => {
  if (!editor?.value) return
  if (typeof from !== 'number' || typeof to !== 'number') return
  if (from >= to) return
  editor.value
    .chain()
    .focus()
    .setTextSelection({ from, to })
    .wrapSpeedSpan({ speed: clampLocalSpeed(speed), uid })
    .run()
}

const resolvePolyphonicTarget = (event) => {
  let el = event.target
  if (typeof Node !== 'undefined' && el?.nodeType === Node.TEXT_NODE) {
    el = el.parentElement
  }
  return el?.closest?.('[data-poly-id]') || null
}

let hoverMarkerId = ''
let domRef = null

const handleMouseMove = (event) => {
  const target = resolvePolyphonicTarget(event)
  if (!target) {
    if (hoverMarkerId) {
      hoverMarkerId = ''
      emit('polyphonicHover', null)
    }
    return
  }
  const markerId = target.getAttribute('data-poly-id')
  if (!markerId || hoverMarkerId === markerId) return
  hoverMarkerId = markerId
  const marker = markerMap.value.get(markerId)
  if (!marker) return
  const rect = target.getBoundingClientRect()
  emit('polyphonicHover', {
    markerId,
    rect: {
      top: rect.top,
      bottom: rect.bottom,
      left: rect.left,
      right: rect.right
    }
  })
}

const handleMouseLeave = () => {
  if (hoverMarkerId) {
    hoverMarkerId = ''
    emit('polyphonicHover', null)
  }
}

const attachDomEvents = () => {
  if (!editor?.value) return
  const el = editor.value.view?.dom
  if (!el || domRef === el) return
  detachDomEvents()
  domRef = el
  domRef.addEventListener('mousemove', handleMouseMove)
  domRef.addEventListener('mouseleave', handleMouseLeave)
}

const detachDomEvents = () => {
  if (!domRef) return
  domRef.removeEventListener('mousemove', handleMouseMove)
  domRef.removeEventListener('mouseleave', handleMouseLeave)
  domRef = null
}

// 获取编辑器内容（包含停顿标记）
const getContent = () => {
  if (!editor?.value) return ''
  const html = editor.value.getHTML()
  // 打印编辑器内的 HTML 内容
  console.log('[编辑器 HTML 内容 (getContent)]', html)
  return deserialize(html)
}

// 设置阅读规则标记
const setReadingRuleMarkers = (markers) => {
  if (!editor?.value) return
  editor.value.chain().setReadingRuleMarkers(markers).run()
}

// 清除阅读规则标记
const clearReadingRuleMarkers = () => {
  if (!editor?.value) return
  editor.value.chain().clearReadingRuleMarkers().run()
}

defineExpose({
  insertPause,
  insertSilence,
  focus: () => editor?.value?.commands.focus(),
  applyLocalSpeedRange,
  getContent,
  setReadingRuleMarkers,
  clearReadingRuleMarkers
})

onBeforeUnmount(() => {
  detachDomEvents()
  editor?.value?.destroy()
})

watch(
  () => props.modelValue,
  (val) => {
    if (!editor?.value || isInternalUpdate) return
    const current = deserialize(editor.value.getHTML())
    // 更精确的比较：去除空白字符后比较，避免因格式差异导致的循环
    const normalizedCurrent = current.trim()
    const normalizedVal = (val || '').trim()
    if (normalizedCurrent !== normalizedVal) {
      isInternalUpdate = true
      try {
        editor.value.commands.setContent(serialize(val || ''))
        // 使用 setTimeout 确保在下一个事件循环中重置标志
        setTimeout(() => {
          isInternalUpdate = false
          nextTick(() => {
            applySpeedSegments(props.speedSegments || [])
          })
        }, 0)
      } catch (error) {
        isInternalUpdate = false
        console.error('设置编辑器内容失败:', error)
      }
    }
  }
)

watch(
  () => props.speedSegments,
  (segments) => {
    if (!editor?.value) return
    applySpeedSegments(segments || [])
  },
  { deep: true }
)

watch(
  () => props.readingRules,
  () => {
    if (editor?.value) {
      nextTick(() => {
        updateReadingRuleMarkers()
      })
    }
  },
  { deep: true }
)
</script>

<style scoped>
.rich-text-editor {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.rich-text-editor.is-active {
  border-color: #4f7bff;
  box-shadow:
    0 0 0 2px rgba(79, 123, 255, 0.18),
    0 12px 30px rgba(79, 123, 255, 0.22);
}

.rich-text-editor__content :deep(.ProseMirror) {
  min-height: 120px;
  padding: 10px 240px 10px 10px;
  outline: none;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}

.rich-text-editor__content :deep(.ProseMirror p) {
  margin: 0;
}

.rich-text-editor__content :deep(.polyphonic-marker) {
  border-radius: 4px;
  padding: 0 3px;
  cursor: pointer;
  transition: background 0.15s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  line-height: 1.2;
}

.rich-text-editor__content :deep(.polyphonic-marker--pending) {
  background: rgba(47, 123, 255, 0.16);
  color: #1f5edb;
}

.rich-text-editor__content :deep(.polyphonic-marker--resolved) {
  background: #ffe6aa;
  color: #704b00;
}

.rich-text-editor__content :deep(.reading-rule-marker) {
  border-radius: 4px;
  padding: 0 3px;
  cursor: pointer;
  transition: background 0.15s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  line-height: 1.2;
}

.rich-text-editor__content :deep(.reading-rule-marker--pending) {
  background: rgba(64, 158, 255, 0.2);
  color: #409eff;
}

.rich-text-editor__content :deep(.reading-rule-marker--applied) {
  background: rgba(64, 158, 255, 0.4);
  color: #1d5cb8;
}
</style>

