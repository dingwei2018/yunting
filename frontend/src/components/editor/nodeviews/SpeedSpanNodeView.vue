<template>
  <NodeViewWrapper
    class="speed-span-node"
    :data-speed-span="speedLabel"
    :data-speed-id="node.attrs.uid"
  >
    <span class="speed-span-node__content">
      <NodeViewContent class="speed-span-node__inner" />
    </span>
    <span class="speed-span-node__badge">
      【语速{{ speedLabel }}】
      <button type="button" class="speed-span-node__remove" @mousedown.stop.prevent @click="handleRemove">
        ×
      </button>
    </span>
  </NodeViewWrapper>
</template>

<script setup>
import { computed } from 'vue'
import { NodeViewWrapper, NodeViewContent } from '@tiptap/vue-3'

const props = defineProps({
  node: {
    type: Object,
    required: true
  },
  getPos: {
    type: Function,
    required: true
  },
  editor: {
    type: Object,
    required: true
  }
})

const speedLabel = computed(() => {
  const value = Number(props.node?.attrs?.speed ?? 0)
  if (!Number.isFinite(value)) {
    return '+0'
  }
  if (value > 0) return `+${value}`
  return value.toString()
})

const handleRemove = () => {
  const pos = typeof props.getPos === 'function' ? props.getPos() : null
  if (pos === null || !props.editor) return
  props.editor
    .chain()
    .focus()
    .removeSpeedSpanAt(pos)
    .run()
}
</script>

<style scoped>
.speed-span-node {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 6px;
  border-radius: 6px;
  background: rgba(79, 123, 255, 0.12);
  border: 1px solid rgba(79, 123, 255, 0.4);
  margin: 0 2px;
  white-space: nowrap;
}

.speed-span-node__content {
  display: inline-flex;
  align-items: center;
  padding: 2px 0;
}

.speed-span-node__inner :deep(.ProseMirror) {
  display: contents;
}

.speed-span-node__badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #2f3d74;
  white-space: nowrap;
}

.speed-span-node__remove {
  border: none;
  background: transparent;
  color: #2f3d74;
  cursor: pointer;
  font-size: 12px;
  padding: 0 2px;
  line-height: 1;
}

.speed-span-node__remove:hover {
  color: #f56c6c;
}
</style>


