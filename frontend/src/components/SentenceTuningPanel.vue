<template>
  <div class="tuning-panel">
    <div class="voice-section">
      <div class="voice-tabs">
        <div
          v-for="voice in voiceOptions"
          :key="voice.value"
          class="voice-tab"
          :class="{ active: editingForm.voice === voice.value }"
          @click="selectVoiceInternal(voice.value)"
        >
          <div class="voice-info">
            <el-avatar class="voice-avatar">{{ voice.avatar }}</el-avatar>
            <div>
              <div class="voice-name">{{ voice.label }}</div>
              <div class="voice-meta">{{ voice.desc }} · {{ voice.tag }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="custom-section">
      <div class="custom-toolbar">
        <div class="custom-title">个性定制</div>
        <div class="custom-actions">
          <div
            v-for="item in customOptions"
            :key="item.label"
            class="custom-item"
          :class="{
            active: isItemActive(item),
            disabled: isItemDisabled(item)
          }"
            @click="handleCustomClick(item)"
          >
            <el-icon><component :is="iconComponents[item.icon]" /></el-icon>
            <span>{{ item.label }}</span>
          </div>
        </div>
        <div class="custom-ops">
          <span class="text-count">{{ editingForm.content.length }}/5000</span>
          <el-button link type="primary">全部合成</el-button>
          <el-button link type="primary" @click="$emit('clear-text')">
            清空文本
          </el-button>
        </div>
      </div>

      <div v-if="activeCustomConfig" class="custom-slider-panel">
        <div class="slider-title">{{ activeCustomConfig.label }} 调整</div>
        <el-slider
          v-model="editingForm[activeCustomConfig.controlKey]"
          :min="activeCustomConfig.min ?? 0"
          :max="activeCustomConfig.max ?? 100"
          :step="activeCustomConfig.step ?? 1"
          show-input
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  Headset,
  Timer,
  MagicStick,
  Bell,
  ChatLineSquare,
  CollectionTag,
  Tickets
} from '@element-plus/icons-vue'

import { computed, ref } from 'vue'

const props = defineProps({
  voiceCategories: { type: Array, default: () => [] },
  voiceOptions: { type: Array, default: () => [] },
  activeVoiceCategory: { type: String, default: '' },
  customOptions: { type: Array, default: () => [] },
  editingForm: { type: Object, required: true },
  customDisabled: { type: Object, default: () => ({}) },
  activeActions: { type: Object, default: () => ({}) }
})

const emit = defineEmits([
  'update:activeVoiceCategory',
  'select-voice',
  'custom-action',
  'clear-text'
])

const iconComponents = {
  Headset,
  Timer,
  MagicStick,
  Bell,
  ChatLineSquare,
  CollectionTag,
  Tickets
}

const activeCustom = ref('')

const activeCustomConfig = computed(() =>
  props.customOptions.find((item) => item.controlKey === activeCustom.value)
)

const isItemActive = (item) => {
  if (item.controlKey) {
    return activeCustom.value === item.controlKey
  }
  if (item.actionKey) {
    return !!props.activeActions[item.actionKey]
  }
  return false
}

const updateCategory = (value) => {
  emit('update:activeVoiceCategory', value)
}

const selectVoiceInternal = (value) => {
  emit('select-voice', value)
}

const isItemDisabled = (item) => {
  if (!item.actionKey) return false
  return !!props.customDisabled[item.actionKey]
}

const handleCustomClick = (item) => {
  if (isItemDisabled(item)) return
  if (item.controlKey) {
    activeCustom.value =
      activeCustom.value === item.controlKey ? '' : item.controlKey
  } else if (item.actionKey) {
    emit('custom-action', item.actionKey)
  }
}
</script>

<style scoped>
.voice-section {
  margin-bottom: 16px;
}

.voice-category {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.voice-category-item {
  padding: 6px 18px;
  border-radius: 999px;
  background: #f0f2f7;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}

.voice-category-item.active {
  background: #e6f1ff;
  color: #2f7bff;
}

.voice-tabs {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.voice-info {
  display: flex;
  gap: 10px;
  align-items: center;
}

.voice-tab {
  width: 240px;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fff;
}

.voice-tab.active {
  border-color: #2f7bff;
  box-shadow: 0 6px 16px rgba(47, 123, 255, 0.25);
}

.voice-avatar {
  background: #2f7bff;
  color: #fff;
  font-weight: 600;
}

.voice-name {
  font-weight: 600;
  color: #1d1f23;
}

.voice-meta {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.voice-library {
  margin-top: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.custom-section {
  border-top: 1px dashed #e4e7ed;
  padding-top: 16px;
  margin-bottom: 16px;
}

.custom-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.custom-title {
  font-weight: 600;
  white-space: nowrap;
}

.custom-actions {
  display: flex;
  flex: 1;
  justify-content: flex-start;
  gap: 20px;
  flex-wrap: wrap;
}

.custom-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #1d1f23;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.custom-item.active {
  color: #2f7bff;
}

.custom-item.disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}

.custom-ops {
  display: flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}

.text-count {
  color: #999;
}

.custom-slider-panel {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #f9fbff;
  width: 400px;
}

.slider-title {
  font-weight: 500;
  margin-bottom: 8px;
}
</style>

