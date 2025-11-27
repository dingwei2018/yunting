<template>
  <NodeViewWrapper class="pause-node-view" ref="wrapperRef">
    <ElPopover
      placement="top"
      trigger="manual"
      :visible="popoverVisible"
      width="220"
      popper-class="pause-node-view__popover"
    >
      <div class="pause-duration-editor" @mousedown.stop @click.stop>
        <div class="pause-duration-editor__title">停顿时长（秒）</div>
        <ElInputNumber
          v-model="durationValue"
          :step="0.1"
          :precision="1"
          :min="0"
          size="small"
          controls-position="right"
        />
        <div class="pause-duration-editor__actions">
          <ElButton size="small" @click="handleCancel">取消</ElButton>
          <ElButton size="small" type="primary" @click="handleConfirm">确认</ElButton>
        </div>
      </div>
      <template #reference>
        <button class="pause-node-view__button" type="button" @click="handleMarkerClick">
          <PauseMarker inline clickable :label="displayDuration" />
        </button>
      </template>
    </ElPopover>
  </NodeViewWrapper>
</template>

<script setup>
import { NodeViewWrapper } from '@tiptap/vue-3'
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElPopover, ElInputNumber, ElButton } from 'element-plus'
import PauseMarker from '@/components/icons/PauseMarker.vue'
import { DEFAULT_PAUSE_DURATION } from '../extensions/pause'

const props = defineProps({
  node: {
    type: Object,
    required: true
  },
  updateAttributes: {
    type: Function,
    required: true
  }
})

const wrapperRef = ref(null)
const popoverVisible = ref(false)
const durationValue = ref(parseDuration(props.node.attrs.duration))

function parseDuration(value) {
  const num = Number(value)
  if (Number.isFinite(num) && num >= 0) {
    return Number(num.toFixed(1))
  }
  return Number(DEFAULT_PAUSE_DURATION)
}

function formatDuration(value) {
  const num = Number(value)
  if (!Number.isFinite(num) || num < 0) {
    return DEFAULT_PAUSE_DURATION
  }
  return num.toFixed(1)
}

const displayDuration = computed(() => formatDuration(props.node.attrs.duration))

watch(
  () => props.node.attrs.duration,
  (val) => {
    durationValue.value = parseDuration(val)
  }
)

const closePopover = () => {
  popoverVisible.value = false
}

const handleMarkerClick = (event) => {
  event.preventDefault()
  event.stopPropagation()
  durationValue.value = parseDuration(props.node.attrs.duration)
  popoverVisible.value = !popoverVisible.value
}

const handleCancel = () => {
  closePopover()
  durationValue.value = parseDuration(props.node.attrs.duration)
}

const handleConfirm = () => {
  const normalized = formatDuration(durationValue.value)
  props.updateAttributes({
    duration: normalized
  })
  closePopover()
}

const handleOutsideClick = (event) => {
  if (!popoverVisible.value) return
  const el = wrapperRef.value?.$el ?? wrapperRef.value
  if (el && el.contains(event.target)) {
    return
  }
  closePopover()
}

onMounted(() => {
  document.addEventListener('mousedown', handleOutsideClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleOutsideClick)
})
</script>

<style scoped>
.pause-node-view {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0 4px;
}

.pause-node-view__button {
  border: none;
  padding: 0;
  background: transparent;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.pause-node-view__button:focus {
  outline: none;
}

.pause-duration-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pause-duration-editor__title {
  font-size: 13px;
  color: #333;
}

.pause-duration-editor__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>

<style>
.pause-node-view__popover {
  padding: 12px 16px;
}
</style>
