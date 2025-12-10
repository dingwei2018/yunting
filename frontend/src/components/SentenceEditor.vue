<template>
  <div class="sentence-editor">
    <transition name="fade">
      <div v-if="internalExpanded" class="editing-panel">
        <SentenceTuningPanel
          :voice-options="voiceOptions"
          :custom-options="customOptions"
          :editing-form="editingForm"
          :current-content="currentContent"
          :custom-disabled="customDisabledState"
          :active-actions="activeCustomActions"
          :selection-context="currentSelectionContext"
          @select-voice="selectVoice"
          @custom-action="handleCustomAction"
          @request-local-speed="handleRequestLocalSpeed"
          @clear-text="() => handleClearText(rootSentence)"
          @synthesize-all="handleSynthesizeAllBreakingSentences"
        />

        <SubSentenceEditorList
          :subs="internalBreakingSentences"
          :editing-sub-sentence-id="editingSubSentenceId"
          :get-polyphonic-markers="getPolyphonicMarkers"
          :is-polyphonic-mode-active="isPolyphonicModeActive"
          :set-editor-ref="setEditorRef"
          :get-breaking-sentence-status="getBreakingSentenceSynthesisStatus"
          :get-reading-rule-markers="getReadingRuleMarkers"
          :toggle-reading-rule="toggleReadingRule"
          @select-sub="selectSubSentence"
          @editor-selection-change="handleEditorSelectionChange"
          @editor-content-change="handleEditorContentChange"
          @polyphonic-hover="handlePolyphonicHover"
          @editor-focus="handleEditorFocus"
          @speed-segments-change="handleSpeedSegmentsChange"
          @reading-rule-hover="handleReadingRuleHover"
          @reading-rule-toggle="(event) => toggleReadingRule(event.sub.sentence_id, event.payload.ruleId, event.payload.pattern, event.payload.applied)"
          @play="handlePlay"
          @synthesize="handleResynthesizeBreakingSentence"
          @insert-after="handleInsertAfter"
          @delete="handleDelete"
        />
        
        <div class="textarea-actions">
          <div class="textarea-buttons">
            <el-button @click="handleCancel">取消</el-button>
            <el-button type="primary" @click="handleSave" :loading="saving">
              保存当前修改
            </el-button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="fade">
      <PolyphonicTooltip
        v-if="polyphonicTooltip.visible"
        :visible="polyphonicTooltip.visible"
        :position="polyphonicTooltip.position"
        :char="polyphonicTooltip.char"
        :options="polyphonicTooltip.options"
        :selected="polyphonicTooltip.selected"
        @mouseenter="handleTooltipMouseEnter"
        @mouseleave="handleTooltipMouseLeave"
        @select="handlePolyphonicOptionSelect"
      />
    </transition>

    <transition name="fade">
      <ReadingRuleTooltip
        v-if="readingRuleTooltip.visible"
        :visible="readingRuleTooltip.visible"
        :position="readingRuleTooltip.position"
        :pattern="readingRuleTooltip.pattern"
        :applied="readingRuleTooltip.applied"
        :rule-id="readingRuleTooltip.ruleId"
        @mouseenter="handleReadingRuleTooltipMouseEnter"
        @mouseleave="handleReadingRuleTooltipMouseLeave"
        @select="handleReadingRuleSelect"
      />
    </transition>

    <SplitStandardDialog
      :visible="splitStandardDialogVisible"
      :type="splitStandardType"
      :char-count="splitStandardCharCount"
      @update:visible="(val) => (splitStandardDialogVisible = val)"
      @update:type="(val) => (splitStandardType = val)"
      @update:char-count="(val) => (splitStandardCharCount = val)"
      @confirm="handleSplitStandardConfirm"
      @close="handleSplitStandardDialogClose"
    />

    <el-dialog
      v-model="localSpeedDialog.visible"
      title="局部语速调整"
      width="420px"
      :close-on-click-modal="false"
    >
      <div class="local-speed-dialog__body">
        <div class="local-speed-dialog__info">
          已选字符：{{ localSpeedDialog.rangeLength }} 个
        </div>
        <el-slider
          v-model="localSpeedDialog.value"
          :min="-10"
          :max="10"
          :step="1"
          show-input
        />
      </div>
      <template #footer>
        <el-button @click="handleCancelLocalSpeed">取消</el-button>
        <el-button type="primary" @click="handleConfirmLocalSpeed">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import SentenceTuningPanel from '@/components/SentenceTuningPanel.vue'
import SubSentenceEditorList from '@/components/SubSentenceEditorList.vue'
import PolyphonicTooltip from '@/components/PolyphonicTooltip.vue'
import ReadingRuleTooltip from '@/components/ReadingRuleTooltip.vue'
import SplitStandardDialog from '@/components/SplitStandardDialog.vue'
import { useSentenceEditor } from '@/composables/useSentenceEditor'

const props = defineProps({
  // 拆句数据（来自 getOriginalSentenceList 的 data.list 中的一项）
  originalSentence: {
    type: Object,
    required: false,
    default: null
  },
  // 任务ID（用于保存和合成）
  taskId: {
    type: [Number, String],
    required: true
  },
  // 音色列表
  voiceOptions: {
    type: Array,
    default: () => []
  },
  // 是否展开编辑区域
  expanded: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'update:expanded',
  'saved',
  'refresh'
])

// 使用主 composable
const {
  // 状态
  internalExpanded,
  editingForm,
  editingSubSentenceId,
  internalBreakingSentences,
  selectionStateMap,
  polyphonicModeMap,
  saving,
  currentContent,
  isSplitting,
  // 方法
  selectSubSentence,
  setEditorRef,
  getPolyphonicMarkers,
  isPolyphonicModeActive,
  getReadingRuleMarkers,
  toggleReadingRule,
  handleEditorSelectionChange,
  handleEditorContentChange,
  handleEditorFocus,
  handleSpeedSegmentsChange,
  handlePolyphonicHover,
  handleReadingRuleHover,
  handlePolyphonicOptionSelect,
  handleRequestLocalSpeed,
  handleConfirmLocalSpeed,
  handleCancelLocalSpeed,
  handleCustomAction,
  handleSplitStandardConfirm,
  handleSplitStandardDialogClose,
  handleInsertAfter,
  handleDelete,
  handleSynthesizeAllBreakingSentences,
  handleResynthesizeBreakingSentence,
  handlePlay,
  handleClearText,
  selectVoice,
  handleSave,
  handleCancel,
  hasUnsavedChanges,
  getBreakingSentenceSynthesisStatus,
  // 对话框状态
  localSpeedDialog,
  splitStandardDialogVisible,
  splitStandardType,
  splitStandardCharCount,
  polyphonicTooltip,
  handleTooltipMouseEnter,
  handleTooltipMouseLeave,
  readingRuleTooltip,
  handleReadingRuleTooltipMouseEnter,
  handleReadingRuleTooltipMouseLeave,
  handleReadingRuleSelect,
  currentSelectionContext
} = useSentenceEditor(props, emit)

// 暴露方法给父组件
defineExpose({
  hasUnsavedChanges
})

// 根句子对象（用于清空文本）
const rootSentence = computed(() => ({
  sentence_id: props.originalSentence.originalSentenceId,
  content: props.originalSentence.content
}))

// 自定义选项配置
const customOptions = [
  {
    label: '音量',
    icon: 'Headset',
    controlKey: 'volume',
    min: 0,
    max: 100,
    step: 1
  },
  {
    label: '语速',
    icon: 'MagicStick',
    controlKey: 'speed',
    min: -10,
    max: 10,
    step: 1
  },
  { label: '断句标准', icon: 'Tickets', actionKey: 'split-standard' },
  { label: '停顿', icon: 'Timer', actionKey: 'pause' },
  { label: '多音字', icon: 'ChatLineSquare', actionKey: 'polyphonic' },
  { label: '插入静音', icon: 'Bell', actionKey: 'silence' },
  { label: '阅读规范', icon: 'CollectionTag', actionKey: 'reading-rules' }
]

// 计算属性
const customDisabledState = computed(() => ({
  pause: !selectionStateMap[editingSubSentenceId.value]?.hasTextBefore
}))

const activeCustomActions = computed(() => ({
  polyphonic: !!polyphonicModeMap[editingSubSentenceId.value]
}))

// 监听 expanded prop 变化
watch(() => props.expanded, (val) => {
  if (val !== internalExpanded.value) {
    internalExpanded.value = val
  }
})

// 监听内部展开状态变化
watch(internalExpanded, (val) => {
  emit('update:expanded', val)
})

// 注意：不再实时同步 internalBreakingSentences 到父组件
// 数据同步只在保存时进行（通过 API 调用和刷新）
// 这样可以避免循环更新问题：
// - 实时同步会导致 props.originalSentence 变化
// - props.originalSentence 变化会触发 useSentenceEditor 的 watch
// - watch 会重新初始化 internalBreakingSentences
// - 这又会导致 watch 再次触发，形成无限循环

</script>

<style scoped>
.sentence-editor {
  width: 100%;
}

.editing-panel {
  margin-top: 16px;
  margin-right: 124px;
  padding: 18px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fdfdfd;
}

.textarea-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 10px;
  flex-wrap: wrap;
  gap: 10px;
}

.textarea-buttons {
  display: flex;
  gap: 10px;
}

.local-speed-dialog__body {
  padding: 20px 0;
}

.local-speed-dialog__info {
  margin-bottom: 20px;
  color: #666;
  font-size: 14px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

