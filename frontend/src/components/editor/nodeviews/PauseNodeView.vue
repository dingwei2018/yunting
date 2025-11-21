<template>
  <NodeViewWrapper class="pause-node-view">
    <PauseMarker inline />
  </NodeViewWrapper>
</template>

<script setup>
import { NodeViewWrapper } from '@tiptap/vue-3'
import PauseMarker from '@/components/icons/PauseMarker.vue'
</script>

<style scoped>
.pause-node-view {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0 4px;
}
</style>

