import { ref } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 播放功能 composable
 */
export function useSentenceEditorPlayback(audioPlayer) {
  // 当前正在播放的音频信息
  const currentPlayingAudio = ref(null)

  /**
   * 播放下一个音频
   */
  const playNextAudio = (index) => {
    if (!currentPlayingAudio.value || index >= currentPlayingAudio.value.audioElements.length) {
      currentPlayingAudio.value = null
      return
    }

    const audio = currentPlayingAudio.value.audioElements[index]
    currentPlayingAudio.value.currentIndex = index

    const onEnded = () => {
      audio.removeEventListener('ended', onEnded)
      playNextAudio(index + 1)
    }

    audio.addEventListener('ended', onEnded)
    audio.play().catch(error => {
      console.error('播放音频失败:', error)
      ElMessage.error('播放失败')
      currentPlayingAudio.value = null
    })
  }

  /**
   * 停止所有正在播放的音频
   */
  const stopAllPlayingAudio = () => {
    if (currentPlayingAudio.value) {
      currentPlayingAudio.value.audioElements.forEach(audio => {
        audio.pause()
        audio.currentTime = 0
      })
      currentPlayingAudio.value = null
    }
  }

  /**
   * 播放拆句（按顺序播放多个音频）
   */
  const handlePlayOriginalSentence = async (sentence, originalSentenceStatus) => {
    const originalSentenceId = sentence.sentence_id || sentence.originalSentenceId

    const statusInfo = originalSentenceStatus?.[originalSentenceId]
    if (!statusInfo || !statusInfo.audioUrlList || statusInfo.audioUrlList.length === 0) {
      ElMessage.warning('暂无音频可播放')
      return
    }

    const audioUrlList = statusInfo.audioUrlList

    if (audioUrlList.length === 1 && audioPlayer) {
      audioPlayer.show(audioUrlList[0].audioUrl)
      return
    }

    stopAllPlayingAudio()

    const audioElements = audioUrlList.map((item, index) => {
      const audio = new Audio(item.audioUrl)
      audio.preload = 'auto'
      return { audio, sequence: item.sequence || index }
    })

    audioElements.sort((a, b) => a.sequence - b.sequence)

    currentPlayingAudio.value = {
      sentenceId: originalSentenceId,
      audioList: audioElements,
      currentIndex: 0,
      audioElements: audioElements.map(item => item.audio)
    }

    playNextAudio(0)
  }

  /**
   * 播放断句（单个音频）
   */
  const handlePlay = (sentence, breakingSentenceStatus, originalSentence) => {
    const breakingSentenceId = sentence.sentence_id
    const statusInfo = breakingSentenceStatus?.[breakingSentenceId]

    let audioUrl = statusInfo?.audioUrl || sentence.audio_url || sentence.audioUrl

    if (!audioUrl && originalSentence && Array.isArray(originalSentence.breakingSentenceList)) {
      const breakingSentence = originalSentence.breakingSentenceList.find(
        bs => bs.breakingSentenceId == breakingSentenceId || 
             String(bs.breakingSentenceId) === String(breakingSentenceId)
      )
      if (breakingSentence) {
        audioUrl = breakingSentence.audioUrl
      }
    }

    if (!audioUrl) {
      ElMessage.warning('暂无音频可播放')
      return
    }

    if (audioPlayer) {
      audioPlayer.show(audioUrl)
    } else {
      const audio = new Audio(audioUrl)
      audio.preload = 'auto'
      audio.play().catch(error => {
        console.error('播放音频失败:', error)
        ElMessage.error('播放失败')
      })
    }
  }

  return {
    // 状态
    currentPlayingAudio,
    // 方法
    handlePlay,
    handlePlayOriginalSentence,
    stopAllPlayingAudio
  }
}

