<template>
  <NodeViewWrapper class="silence-node-view">
    <SilenceMarker :duration="node.attrs.duration" inline />
  </NodeViewWrapper>
</template>

<script setup>
import { NodeViewWrapper } from '@tiptap/vue-3'
import SilenceMarker from '@/components/icons/SilenceMarker.vue'

defineProps({
  node: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.silence-node-view {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0 4px;
}
</style>

