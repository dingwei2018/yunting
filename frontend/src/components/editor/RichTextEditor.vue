<template>
  <div class="rich-text-editor" v-if="editor">
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
  ref
} from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import PauseExtension, { PauseNodeName } from './extensions/pause'
import SilenceExtension, { SilenceNodeName } from './extensions/silence'
import PolyphonicMarkersExtension from './extensions/polyphonicMarkers'

const PAUSE_PLACEHOLDER = '<pause>'
const PAUSE_PLACEHOLDER_REGEX = new RegExp(PAUSE_PLACEHOLDER, 'g')
const SILENCE_PLACEHOLDER_REGEX = /<silence:([\d.]+)>/g

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
  }
})

const emit = defineEmits([
  'update:modelValue',
  'selectionChange',
  'contentChange',
  'polyphonicHover'
])

const markerMap = ref(new Map())

const serialize = (value = '') => {
  if (!value) {
    return '<p></p>'
  }
  const paragraphs = value.split('\n').map((line) => {
    if (!line) return '<p><br /></p>'
    const htmlLine = line
      .replace(PAUSE_PLACEHOLDER_REGEX, '<span data-pause="true"></span>')
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
  const paragraphs = Array.from(tmp.querySelectorAll('p')).map((p) =>
    p.innerHTML
      .replace(/<span[^>]*data-pause="true"[^>]*><\/span>/g, PAUSE_PLACEHOLDER)
      .replace(/<span[^>]*data-silence="([^"]*)"[^>]*><\/span>/g, (_, duration = '0') => {
        const safeDuration = duration || '0'
        return `<silence:${safeDuration}>`
      })
      .replace(/<br\s*\/?>/g, '')
      .replace(/&nbsp;/g, ' ')
  )
  return paragraphs.join('\n').trim()
}

const editor = useEditor({
  extensions: [
    StarterKit.configure({
      history: false
    }),
    PauseExtension,
    SilenceExtension,
    PolyphonicMarkersExtension
  ],
  autofocus: props.autofocus,
  content: serialize(props.modelValue),
  onUpdate({ editor }) {
    const html = editor.getHTML()
    const asString = deserialize(html)
    emit('update:modelValue', asString)
    emit('contentChange', asString)
  },
  onSelectionUpdate({ editor }) {
    const { from } = editor.state.selection
    const text = editor.state.doc.textBetween(0, from)
    emit('selectionChange', { hasTextBefore: text.length > 0 })
  }
})

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
    } else {
      detachDomEvents()
    }
  }
)

const insertPause = () => {
  if (!editor?.value) return
  editor.value
    .chain()
    .focus()
    .insertContent({
      type: PauseNodeName
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
  focus: () => editor?.value?.commands.focus()
})

onBeforeUnmount(() => {
  detachDomEvents()
  editor?.value?.destroy()
})
</script>

<style scoped>
.rich-text-editor {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
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

