<template>
  <el-dialog
    :model-value="visible"
    title="断句标准"
    width="400px"
    @close="handleClose"
  >
    <div class="split-dialog-body">
      <el-radio-group
        :model-value="type"
        @change="(val) => emit('update:type', val)"
      >
        <el-radio label="punctuation">大符号</el-radio>
        <el-radio label="charCount">字符数</el-radio>
      </el-radio-group>
      <div
        v-if="type === 'charCount'"
        class="char-count-input"
      >
        <span>字符数：</span>
        <el-input
          :model-value="charCount"
          type="number"
          :min="1"
          placeholder="请输入字符数"
          style="width: 150px;"
          @input="(val) => emit('update:char-count', Number(val))"
        />
      </div>
    </div>
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button
        type="primary"
        :disabled="type === 'charCount' && (!charCount || charCount <= 0)"
        @click="emit('confirm')"
      >
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  type: {
    type: String,
    default: 'punctuation'
  },
  charCount: {
    type: Number,
    default: 50
  }
})

const emit = defineEmits([
  'update:visible',
  'update:type',
  'update:char-count',
  'confirm',
  'close'
])

const handleClose = () => {
  emit('close')
  emit('update:visible', false)
}
</script>

<style scoped>
.split-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.char-count-input {
  margin-left: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.char-count-input span {
  white-space: nowrap;
}
</style>

