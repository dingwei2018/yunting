<template>
  <div class="sub-textarea-list">
    <div
      v-for="(sub, subIndex) in subs"
      :key="sub.sentence_id"
      class="sub-textarea-item"
      @click="emitSelect(sub)"
    >
      <div class="textarea-toolbar">
        <span>
          输入文本{{ subIndex + 1 }}
        </span>
        <span class="textarea-count">
          {{ sub.content?.length || 0 }}/5000
        </span>
      </div>
      <div class="textarea-input">
        <RichTextEditor
          :ref="(el) => setEditorRef(sub.sentence_id, el)"
          v-model="sub.content"
          :polyphonic-markers="getPolyphonicMarkers(sub)"
          :show-polyphonic-hints="isPolyphonicModeActive(sub)"
          :is-active="editingSubSentenceId === sub.sentence_id"
          @selection-change="(payload) => emitSelectionChange(sub, payload)"
          @content-change="() => emitContentChange(sub)"
          @polyphonic-hover="(payload) => emitPolyphonicHover(sub, payload)"
          @focus="() => emitEditorFocus(sub)"
        />
        <div class="textarea-floating-links" @click.stop>
          <SentenceActionLinks
            :audio-url="sub.audio_url"
            @play="$emit('play', sub)"
            @synthesize="$emit('synthesize', sub.sentence_id)"
            @insert-after="$emit('insert-after', sub.sentence_id)"
            @delete="$emit('delete', sub.sentence_id)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'
import RichTextEditor from '@/components/editor/RichTextEditor.vue'
import SentenceActionLinks from '@/components/SentenceActionLinks.vue'

const props = defineProps({
  subs: {
    type: Array,
    default: () => []
  },
  editingSubSentenceId: {
    type: [String, Number],
    default: null
  },
  getPolyphonicMarkers: {
    type: Function,
    required: true
  },
  isPolyphonicModeActive: {
    type: Function,
    required: true
  },
  setEditorRef: {
    type: Function,
    required: true
  }
})

const emit = defineEmits([
  'select-sub',
  'editor-selection-change',
  'editor-content-change',
  'polyphonic-hover',
  'editor-focus',
  'play',
  'synthesize',
  'insert-after',
  'delete'
])

const emitSelect = (sub) => {
  emit('select-sub', sub)
}

const emitSelectionChange = (sub, payload) => {
  emit('editor-selection-change', { sub, payload })
}

const emitContentChange = (sub) => {
  emit('editor-content-change', sub)
}

const emitPolyphonicHover = (sub, payload) => {
  emit('polyphonic-hover', { sub, payload })
}

const emitEditorFocus = (sub) => {
  emit('editor-focus', sub)
}

const setEditorRef = (sentenceId, instance) => {
  props.setEditorRef(sentenceId, instance)
}

const getPolyphonicMarkers = (sub) => props.getPolyphonicMarkers(sub)
const isPolyphonicModeActive = (sub) => props.isPolyphonicModeActive(sub)
</script>

<style scoped>
.sub-textarea-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.sub-textarea-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.insert-tag {
  margin-left: 8px;
  font-size: 12px;
  color: #f59a23;
}

.textarea-toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #666;
}

.textarea-count {
  color: #999;
  font-size: 12px;
}

.textarea-input {
  position: relative;
}

.textarea-floating-links {
  position: absolute;
  top: 12px;
  right: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #5a7efc;
  background: rgba(255, 255, 255, 0.9);
  padding: 4px 6px;
  border-radius: 6px;
}
</style>

