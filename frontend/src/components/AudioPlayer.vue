<template>
  <div v-if="visible" class="audio-player">
    <div class="audio-player-content">
      <!-- 播放按钮 -->
      <el-button
        circle
        @click="togglePlay"
        class="play-button"
      >
        <el-icon :size="48">
          <component :is="isPlaying ? VideoPause : VideoPlay" />
        </el-icon>
      </el-button>
      
      <!-- 进度条 -->
      <div class="progress-section">
        <el-slider
          v-model="currentTime"
          :max="duration"
          :show-tooltip="false"
          @change="handleSeek"
          class="progress-slider"
        />
        <div class="time-info">
          <span class="current-time">{{ formatTime(currentTime) }}</span>
          <span class="duration">{{ formatTime(duration) }}</span>
        </div>
      </div>
      
      <!-- 关闭按钮 -->
      <el-button
        circle
        @click="handleClose"
        class="close-button"
      >
        <el-icon :size="18">
          <Close />
        </el-icon>
      </el-button>
    </div>
  </div>
  
  <!-- audio 元素始终存在，但只在 visible 时加载 src -->
  <audio
    v-show="false"
    ref="audioElement"
    :src="visible && audioUrl ? audioUrl : ''"
    @loadedmetadata="handleLoadedMetadata"
    @timeupdate="handleTimeUpdate"
    @ended="handleEnded"
    @play="handlePlay"
    @pause="handlePause"
    preload="auto"
  />
</template>

<script setup>
import { ref, computed, watch, onUnmounted, nextTick } from 'vue'
import { VideoPlay, VideoPause, Close } from '@element-plus/icons-vue'
import { ElIcon } from 'element-plus'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  audioUrl: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['close', 'play', 'pause', 'ended'])

const audioElement = ref(null)
const currentTime = ref(0)
const duration = ref(0)
const isPlaying = ref(false)
// 自动播放标志，避免重复自动播放
const hasAutoPlayed = ref(false)

// 自动播放函数
const attemptAutoPlay = async () => {
  console.log('🎵 [AudioPlayer] attemptAutoPlay 调用', {
    visible: props.visible,
    audioUrl: props.audioUrl,
    hasAudioElement: !!audioElement.value,
    hasAutoPlayed: hasAutoPlayed.value
  })
  
  if (!props.visible || !props.audioUrl || hasAutoPlayed.value) {
    return
  }
  
  // 等待 DOM 更新
  await nextTick()
  
  if (!audioElement.value) {
    console.warn('⚠️ [AudioPlayer] audioElement 未准备好，等待...')
    // 如果 audioElement 还没准备好，等待一下再试
    setTimeout(() => {
      if (audioElement.value && props.visible && props.audioUrl && !hasAutoPlayed.value) {
        attemptAutoPlay()
      }
    }, 100)
    return
  }
  
  const tryAutoPlay = () => {
    if (audioElement.value && props.audioUrl && !hasAutoPlayed.value) {
      console.log('🎵 [AudioPlayer] 执行播放', {
        readyState: audioElement.value.readyState,
        src: audioElement.value.src
      })
      audioElement.value.play().then(() => {
        console.log('✅ [AudioPlayer] 自动播放成功')
        hasAutoPlayed.value = true
      }).catch(error => {
        console.error('❌ [AudioPlayer] 自动播放失败:', error)
        // 如果自动播放失败（可能是浏览器策略限制），不显示错误，让用户手动点击
      })
    }
  }
  
  // 如果已经加载了元数据，直接播放
  if (audioElement.value.readyState >= 2) {
    console.log('🎵 [AudioPlayer] 元数据已加载，直接播放')
    tryAutoPlay()
  } else {
    console.log('🎵 [AudioPlayer] 等待元数据加载，readyState:', audioElement.value.readyState)
    // 否则等待加载完成
    const onLoadedMetadata = () => {
      console.log('🎵 [AudioPlayer] 元数据加载完成')
      tryAutoPlay()
      if (audioElement.value) {
        audioElement.value.removeEventListener('loadedmetadata', onLoadedMetadata)
      }
    }
    audioElement.value.addEventListener('loadedmetadata', onLoadedMetadata)
    
    // 如果音频已经可以播放，也尝试播放
    const onCanPlay = () => {
      console.log('🎵 [AudioPlayer] 音频可以播放')
      if (!hasAutoPlayed.value) {
        tryAutoPlay()
      }
      if (audioElement.value) {
        audioElement.value.removeEventListener('canplay', onCanPlay)
      }
    }
    audioElement.value.addEventListener('canplay', onCanPlay)
  }
}

// 监听 audioUrl 变化，加载新音频
watch(() => props.audioUrl, (newUrl, oldUrl) => {
  console.log('🎵 [AudioPlayer] audioUrl 变化', {
    newUrl,
    oldUrl,
    visible: props.visible
  })
  
  if (newUrl && audioElement.value) {
    // URL 变化时重置自动播放标志
    if (newUrl !== oldUrl) {
      hasAutoPlayed.value = false
    }
    audioElement.value.load()
    currentTime.value = 0
    duration.value = 0
    isPlaying.value = false
    
    // 如果播放器可见，尝试自动播放
    if (props.visible) {
      attemptAutoPlay()
    }
  }
}, { immediate: true })

// 监听 visible 变化
watch(() => props.visible, (newVisible) => {
  console.log('🎵 [AudioPlayer] visible 变化', {
    newVisible,
    audioUrl: props.audioUrl,
    hasAudioElement: !!audioElement.value,
    hasAutoPlayed: hasAutoPlayed.value
  })
  
  if (!newVisible && audioElement.value) {
    // 关闭时停止播放
    audioElement.value.pause()
    audioElement.value.currentTime = 0
    currentTime.value = 0
    isPlaying.value = false
    hasAutoPlayed.value = false // 重置自动播放标志
  } else if (newVisible && props.audioUrl) {
    // 显示时尝试自动播放
    attemptAutoPlay()
  }
}, { immediate: true })

// 切换播放/暂停
const togglePlay = () => {
  if (!audioElement.value || !props.audioUrl) return
  
  if (isPlaying.value) {
    audioElement.value.pause()
  } else {
    audioElement.value.play().catch(error => {
      console.error('播放失败:', error)
    })
  }
}

// 拖拽进度条
const handleSeek = (value) => {
  if (audioElement.value) {
    audioElement.value.currentTime = value
    currentTime.value = value
  }
}

// 关闭播放器
const handleClose = () => {
  if (audioElement.value) {
    audioElement.value.pause()
    audioElement.value.currentTime = 0
  }
  currentTime.value = 0
  isPlaying.value = false
  emit('close')
}

// 格式化时间
const formatTime = (seconds) => {
  if (!seconds || isNaN(seconds)) return '00:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
}

// 音频元数据加载完成
const handleLoadedMetadata = () => {
  if (audioElement.value) {
    duration.value = audioElement.value.duration || 0
  }
}

// 时间更新
const handleTimeUpdate = () => {
  if (audioElement.value) {
    currentTime.value = audioElement.value.currentTime || 0
  }
}

// 播放结束
const handleEnded = () => {
  isPlaying.value = false
  currentTime.value = 0
  if (audioElement.value) {
    audioElement.value.currentTime = 0
  }
  emit('ended')
}

// 开始播放
const handlePlay = () => {
  isPlaying.value = true
  emit('play')
}

// 暂停播放
const handlePause = () => {
  isPlaying.value = false
  emit('pause')
}

// 组件卸载时清理
onUnmounted(() => {
  if (audioElement.value) {
    audioElement.value.pause()
    audioElement.value = null
  }
})

// 暴露方法供外部调用
defineExpose({
  play: () => {
    if (audioElement.value && props.audioUrl) {
      audioElement.value.play().catch(error => {
        console.error('播放失败:', error)
      })
    }
  },
  pause: () => {
    if (audioElement.value) {
      audioElement.value.pause()
    }
  },
  stop: () => {
    if (audioElement.value) {
      audioElement.value.pause()
      audioElement.value.currentTime = 0
      currentTime.value = 0
      isPlaying.value = false
    }
  }
})
</script>

<style scoped>
.audio-player {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: 120px;
  background: #000;
  border-top: 1px solid #333;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.3);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.audio-player-content {
  width: 100%;
  max-width: 1200px;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
}

.play-button {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  background: transparent;
  border: 0px solid rgba(255, 255, 255, 0.3);
  color: #fff;
}

.play-button:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.5);
}

.progress-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.progress-slider {
  margin-top: 20px;
  width: 100%;
}

/* 自定义进度条样式（黑色背景下的白色主题） */
.progress-slider :deep(.el-slider__runway) {
  background-color: rgba(255, 255, 255, 0.2);
}

.progress-slider :deep(.el-slider__bar) {
  background-color: #fff;
}

.progress-slider :deep(.el-slider__button) {
  background-color: #fff;
  border: 2px solid #000;
}

.progress-slider :deep(.el-slider__button:hover) {
  background-color: #fff;
}

.time-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.current-time,
.duration {
  font-variant-numeric: tabular-nums;
}

.close-button {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  background: transparent;
  border: 0px solid rgba(255, 255, 255, 0.3);
  color: #fff;
}

.close-button:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.5);
  color: #fff;
}
</style>

