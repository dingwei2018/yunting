<template>
  <span :class="wrapperClasses">
    <span class="pause-marker__icon">
      <span class="pause-marker__bar" />
      <span class="pause-marker__bar" />
    </span>
    <span v-if="label" class="pause-marker__label">{{ label }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  inline: {
    type: Boolean,
    default: false
  },
  label: {
    type: String,
    default: ''
  },
  clickable: {
    type: Boolean,
    default: false
  }
})

const wrapperClasses = computed(() => [
  'pause-marker',
  {
    'pause-marker--inline': props.inline,
    'pause-marker--clickable': props.clickable
  }
])
</script>

<style scoped>
.pause-marker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #79c0b7;
  border: 2px solid #d9d9d9;
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
  gap: 4px;
  box-shadow: 0 2px 6px rgba(49, 64, 77, 0.25);
  font-size: 12px;
  color: #103f3e;
  font-weight: 600;
  line-height: 1;
}

.pause-marker--inline {
  margin: 0 4px;
}

.pause-marker--clickable {
  cursor: pointer;
}

.pause-marker__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 50%;
}

.pause-marker__bar {
  width: 2px;
  height: 10px;
  background: #1a4b48;
  margin: 0 1px;
  border-radius: 1px;
}

.pause-marker__label {
  min-width: 20px;
  text-align: center;
}
</style>

