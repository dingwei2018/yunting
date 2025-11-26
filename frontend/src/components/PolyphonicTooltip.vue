<template>
  <div
    v-if="visible"
    class="polyphonic-tooltip"
    :style="{ top: `${position.y}px`, left: `${position.x}px` }"
    @mouseenter="$emit('mouseenter')"
    @mouseleave="$emit('mouseleave')"
  >
    <div class="tooltip-char">
      {{ char }}
    </div>
    <div class="tooltip-options">
      <div
        v-for="option in options"
        :key="option"
        class="tooltip-option"
        :class="{ active: option === selected }"
        @click.stop="$emit('select', option)"
      >
        {{ option }}
      </div>
    </div>
    <div
      v-if="selected"
      class="tooltip-reset"
      @click.stop="$emit('select', null)"
    >
      恢复默认
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  position: {
    type: Object,
    default: () => ({ x: 0, y: 0 })
  },
  char: {
    type: String,
    default: ''
  },
  options: {
    type: Array,
    default: () => []
  },
  selected: {
    type: String,
    default: ''
  }
})

defineEmits(['select', 'mouseenter', 'mouseleave'])
</script>

<style scoped>
.polyphonic-tooltip {
  position: fixed;
  transform: translate(-50%, 0);
  background: #fff;
  border: 1px solid #e4e7ed;
  box-shadow: 0 16px 30px rgba(0, 0, 0, 0.12);
  border-radius: 10px;
  padding: 12px 16px;
  z-index: 1000;
  min-width: 200px;
}

.tooltip-char {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #1d1f23;
}

.tooltip-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tooltip-option {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #1d1f23;
}

.tooltip-option:hover {
  border-color: #2f7bff;
  color: #2f7bff;
}

.tooltip-option.active {
  background: #ffe6aa;
  border-color: #f4c762;
  color: #5f4300;
}

.tooltip-reset {
  margin-top: 10px;
  font-size: 13px;
  color: #5a7efc;
  cursor: pointer;
  text-align: right;
}
</style>

