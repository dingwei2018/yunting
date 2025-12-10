<template>
  <div
    v-if="visible"
    class="reading-rule-tooltip"
    :style="{ top: `${position.y}px`, left: `${position.x}px` }"
    @mouseenter="$emit('mouseenter')"
    @mouseleave="$emit('mouseleave')"
  >
    <div class="tooltip-pattern">
      {{ pattern }}
    </div>
    <div class="tooltip-options">
      <el-radio-group 
        :model-value="applied ? 'applied' : 'not-applied'"
        @update:model-value="handleRadioChange"
        size="small"
      >
        <el-radio label="applied">应用</el-radio>
        <el-radio label="not-applied">不应用</el-radio>
      </el-radio-group>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  position: {
    type: Object,
    default: () => ({ x: 0, y: 0 })
  },
  pattern: {
    type: String,
    default: ''
  },
  applied: {
    type: Boolean,
    default: false
  },
  ruleId: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['select', 'mouseenter', 'mouseleave'])

const handleRadioChange = (value) => {
  emit('select', value === 'applied')
}
</script>

<style scoped>
.reading-rule-tooltip {
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

.tooltip-pattern {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #1d1f23;
  word-break: break-all;
}

.tooltip-options {
  display: flex;
  justify-content: center;
}
</style>

