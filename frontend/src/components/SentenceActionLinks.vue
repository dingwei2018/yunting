<template>
  <div class="sentence-action-links">
    <template v-if="hasAudio">
      <span class="action-link" @click="$emit('play')">播放</span>
    </template>
    <template v-else>
      <span class="status-tag pending">未合成</span>
    </template>
    <span class="divider">|</span>
    <span class="action-link" @click="$emit('synthesize')">
      {{ hasAudio ? '重新合成' : '合成' }}
    </span>
    <span class="divider">|</span>
    <span class="action-link" @click="$emit('insert-after')">向下插入</span>
    <span class="divider">|</span>
    <span class="action-link danger" @click="$emit('delete')">删除</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  audioUrl: {
    type: String,
    default: ''
  }
})

const hasAudio = computed(() => !!props.audioUrl)
</script>

<style scoped>
.sentence-action-links {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #5a7efc;
  white-space: nowrap;
}

.action-link {
  cursor: pointer;
  color: #5a7efc;
}

.action-link.danger {
  color: #f56c6c;
}

.divider {
  color: #c0c4cc;
}

.status-tag.pending {
  color: #f59a23;
  font-weight: 600;
}
</style>


