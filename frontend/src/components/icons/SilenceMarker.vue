<template>
  <div class="silence-marker" :class="{ inline }">
    <div class="silence-icon">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="12" r="9" fill="none" stroke="#8b8b8b" stroke-width="2" />
        <line x1="12" y1="7" x2="12" y2="12" stroke="#8b8b8b" stroke-width="2" stroke-linecap="round" />
        <line x1="12" y1="12" x2="16" y2="14" stroke="#8b8b8b" stroke-width="2" stroke-linecap="round" />
      </svg>
    </div>
    <div class="silence-text">
      <span>静音</span>
      <span class="silence-duration">{{ formattedDuration }}</span>
      <span>秒</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  duration: {
    type: [String, Number],
    default: 0
  },
  inline: {
    type: Boolean,
    default: false
  }
})

const formattedDuration = computed(() => {
  const value = Number(props.duration)
  if (Number.isNaN(value) || value < 0) return '0'
  if (value % 1 === 0) return value.toString()
  return value.toFixed(1).replace(/\.0+$/, '')
})
</script>

<style scoped>
.silence-marker {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: 2px solid #c5c5c5;
  border-radius: 18px;
  padding: 3px 8px;
  background: #bae6ac;
  font-size: 12px;
  color: #333;
}

.silence-marker.inline {
  padding: 2px 12px;
}

.silence-icon {
  width: 20px;
  height: 20px;
  border-radius: 20px;
  background: #f2f2f2;
  display: flex;
  align-items: center;
  justify-content: center;
}

.silence-icon svg {
  width: 18px;
  height: 18px;
}

.silence-text {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  line-height: 1;
}

.silence-duration {
  font-size: 20px;
  color: #1d1f23;
}
</style>

