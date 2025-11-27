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
  'speedSegmentsChange'
])

const markerMap = ref(new Map())
const lastAppliedSpeedSegments = ref('[]')
let isSyncingSpeedSegments = false

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
  return editor.value.state.doc.textBetween(
    from,
    to,
    '\n',
    '\n',
    (node) => mapLeafNodeToText(node)
  )
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
  
  // 添加日志（仅用于调试第一句）
  const isFirstSentence = html.includes('11月17日') || html.includes('中央')
  if (isFirstSentence) {
    console.log('[deserialize] 输入 HTML 长度:', html.length, '内容:', html)
  }
  
  const tmp = document.createElement('div')
  tmp.innerHTML = html
  
  if (isFirstSentence) {
    console.log('[deserialize] 原始 HTML:', tmp.innerHTML)
  }
  
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
  
  if (isFirstSentence) {
    console.log('[deserialize] 移除 <p> 标签后的 HTML:', tmp.innerHTML)
  }
  
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
  
  if (isFirstSentence) {
    console.log('[deserialize] 移除语速标签后的 HTML:', tmp.innerHTML)
  }
  
  // 处理停顿和静音标记，将它们转换为文本标记
  tmp.querySelectorAll('[data-pause]').forEach((node) => {
    const duration = node.getAttribute('data-pause') || DEFAULT_PAUSE_DURATION
    const marker = document.createTextNode(`<pause:${duration}>`)
    node.parentNode?.replaceChild(marker, node)
  })
  
  tmp.querySelectorAll('[data-silence]').forEach((node) => {
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
  
  if (isFirstSentence) {
    console.log('[deserialize] 最终结果:', '长度:', result.length, '内容:', result)
  }
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
    SpeedSpanExtension
  ],
  autofocus: props.autofocus,
  content: serialize(props.modelValue),
  onUpdate({ editor }) {
    const html = editor.getHTML()
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
  const content = props.modelValue || ''
  const lines = content.split('\n')
  let remaining = marker.offset ?? 0
  let targetLine = -1
  let innerOffset = 0

  for (let i = 0; i < lines.length; i += 1) {
    const lineLength = lines[i].length
    if (remaining < lineLength) {
      targetLine = i
      innerOffset = remaining
      break
    }
    remaining -= lineLength
    if (remaining < 0) break
    if (i < lines.length - 1) {
      remaining -= 1
      if (remaining < 0) break
    }
  }

  if (targetLine < 0) return null

  const doc = editor.value.state.doc
  let foundFrom = null
  let foundTo = null
  let currentLine = -1

  doc.descendants((node, pos) => {
    if (node.type.name === 'paragraph') {
      currentLine += 1
      if (currentLine === targetLine) {
        const start = pos + 1 + innerOffset
        foundFrom = start
        foundTo = start + (marker.length || 1)
        return false
      }
    }
    return true
  })

  if (foundFrom == null || foundTo == null) {
    return null
  }
  return { from: foundFrom, to: foundTo }
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

const clampLocalSpeed = (value) => {
  const num = Number(value)
  if (Number.isNaN(num)) return 0
  if (num > 10) return 10
  if (num < -10) return -10
  return Math.round(num)
}

const normalizeSpeedSegments = (segments = []) =>
  (segments || [])
    .filter(
      (item) =>
        typeof item === 'object' &&
        typeof item.offset === 'number' &&
        typeof item.length === 'number' &&
        item.length > 0
    )
    .map((item, index) => ({
      id: item.id || `speed-${index}-${Date.now()}`,
      offset: Math.max(0, Math.floor(item.offset)),
      length: Math.max(1, Math.floor(item.length)),
      speed: clampLocalSpeed(item.speed)
    }))

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
  const normalized = normalizeSpeedSegments(segments)
  const serialized = JSON.stringify(normalized)
  if (serialized === lastAppliedSpeedSegments.value) {
    return
  }
  isSyncingSpeedSegments = true
  clearSpeedSpans()
  normalized.forEach((segment) => {
    const range = resolveDocRange({
      offset: segment.offset,
      length: segment.length
    })
    if (!range) return
    editor.value
      .chain()
      .focus()
      .setTextSelection({ from: range.from, to: range.to })
      .wrapSpeedSpan({ speed: segment.speed, uid: segment.id })
      .run()
  })
  isSyncingSpeedSegments = false
  lastAppliedSpeedSegments.value = serialized
}

const collectSpeedSegments = () => {
  if (!editor?.value) return []
  const segments = []
  editor.value.state.doc.descendants((node, pos) => {
    if (node.type.name !== SpeedSpanNodeName) return true
    const start = pos + 1
    const end = pos + node.nodeSize - 1
    const offset = getPlainTextBetween(0, start).length
    const length = getPlainTextBetween(start, end).length
    segments.push({
      id: node.attrs?.uid || `speed-${offset}-${Date.now()}`,
      offset,
      length,
      speed: clampLocalSpeed(node.attrs?.speed)
    })
    return false
  })
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
}

const insertSilence = (duration = '0') => {
  if (!editor?.value) return
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

defineExpose({
  insertPause,
  insertSilence,
  focus: () => editor?.value?.commands.focus(),
  applyLocalSpeedRange
})

onBeforeUnmount(() => {
  detachDomEvents()
  editor?.value?.destroy()
})

watch(
  () => props.modelValue,
  (val) => {
    if (!editor?.value) return
    const current = deserialize(editor.value.getHTML())
    if (current !== val) {
      editor.value.commands.setContent(serialize(val || ''))
      nextTick(() => {
        applySpeedSegments(props.speedSegments || [])
      })
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
</style>

