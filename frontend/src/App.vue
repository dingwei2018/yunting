<template>
  <router-view v-slot="{ Component, route }">
    <keep-alive :include="['Sentences']">
      <component :is="Component" :key="route.fullPath" />
    </keep-alive>
  </router-view>
  
  <!-- 全局音频播放器 -->
  <AudioPlayer
    :visible="audioPlayerVisible"
    :audio-url="audioPlayerUrl"
    @close="handleAudioPlayerClose"
  />
</template>

<script setup>
import { ref, provide, onMounted } from 'vue'
import { getVoiceList } from '@/api/voice'
import { ElMessage } from 'element-plus'
import AudioPlayer from '@/components/AudioPlayer.vue'

// 全局音色列表
const globalVoiceList = ref([])

// 全局音频播放器状态
const audioPlayerVisible = ref(false)
const audioPlayerUrl = ref('')

// 加载音色列表
const loadVoiceList = async () => {
  try {
    const response = await getVoiceList()
    console.log('音色列表响应数据:', response)
    // request.js 的响应拦截器已经返回了 res.data，所以 response 就是 data 对象
    // 如果 response 本身就是数组，直接使用
    if (Array.isArray(response)) {
      globalVoiceList.value = response
      console.log('音色列表加载成功，共', globalVoiceList.value.length, '个音色')
    } else if (response && response.list && Array.isArray(response.list)) {
      globalVoiceList.value = response.list
      console.log('音色列表加载成功，共', globalVoiceList.value.length, '个音色')
    } else {
      console.warn('音色列表数据格式异常:', response)
      // 如果数据格式异常，使用空数组，避免后续错误
      globalVoiceList.value = []
    }
  } catch (error) {
    console.error('获取音色列表失败:', error)
    ElMessage.error('获取音色列表失败: ' + (error.message || '未知错误'))
    // 发生错误时使用空数组，避免后续错误
    globalVoiceList.value = []
  }
}

// 应用启动时加载音色列表
onMounted(() => {
  loadVoiceList()
})

// 提供全局音色列表给子组件
provide('globalVoiceList', globalVoiceList)

// 提供音频播放器控制方法给子组件
const showAudioPlayer = (audioUrl) => {
  audioPlayerUrl.value = audioUrl
  audioPlayerVisible.value = true
}

const hideAudioPlayer = () => {
  audioPlayerVisible.value = false
  audioPlayerUrl.value = ''
}

provide('audioPlayer', {
  show: showAudioPlayer,
  hide: hideAudioPlayer
})

// 关闭音频播放器
const handleAudioPlayerClose = () => {
  hideAudioPlayer()
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

#app {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

#app > * {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 为音频播放器预留底部空间 */
body {
  padding-bottom: 120px;
}
</style>

