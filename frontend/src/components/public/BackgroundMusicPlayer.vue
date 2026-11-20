<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({ tracks: { type: Array, required: true } })
const audioRef = ref(null)
const currentIndex = ref(0)
const playing = ref(false)
const status = ref('')
const duration = ref(0)
const elapsed = ref(0)
const volume = ref(0.6)
const expanded = ref(false)
const volumeOpen = ref(false)
const playlistOpen = ref(false)
const volumeControl = ref(null)
const volumeButton = ref(null)
const shuffled = ref(false)
const played = []
const modeLabel = computed(() => shuffled.value ? '随机播放' : '顺序播放')
const failedTracks = new Set()
let wantsPlayback = true
let playToken = 0

const currentTrack = computed(() => props.tracks[currentIndex.value])
const buttonLabel = computed(() => {
  if (status.value === 'unavailable') return '重试背景音乐'
  return playing.value ? '暂停背景音乐' : '播放背景音乐'
})

onMounted(() => {
  audioRef.value.volume = volume.value
  loadCurrent()
  document.addEventListener('pointerdown', dismissOutsideVolume, true)
  document.addEventListener('visibilitychange', onVisibilityChange)
  window.addEventListener('blur', dismissVolume)
})

function selectTrack(index, play = false) {
  if (play) wantsPlayback = true
  if (index === currentIndex.value && status.value !== 'unavailable') {
    if (play && audioRef.value.paused) requestPlay()
    return
  }
  if (shuffled.value && index !== currentIndex.value) {
    if (played.length >= 50) played.shift()
    played.push(currentIndex.value)
  }
  failedTracks.clear()
  currentIndex.value = index
  loadCurrent()
}

function toggleMode() {
  shuffled.value = !shuffled.value
  played.length = 0
}

function stepTrack(step) {
  const count = props.tracks.length
  if (shuffled.value && count > 1) {
    if (step < 0 && played.length) {
      failedTracks.clear()
      currentIndex.value = played.pop()
      loadCurrent()
    } else {
      const pick = Math.floor(Math.random() * (count - 1))
      selectTrack(pick >= currentIndex.value ? pick + 1 : pick)
    }
    return
  }
  selectTrack((currentIndex.value + step + count) % count)
}

function syncProgress() {
  const audio = audioRef.value
  duration.value = Number.isFinite(audio.duration) ? audio.duration : 0
  elapsed.value = audio.currentTime
}

function seek(event) {
  if (!duration.value) return
  audioRef.value.currentTime = Number(event.target.value)
  syncProgress()
}

function setVolume(event) {
  const audio = audioRef.value
  const target = Number(event.target.value)
  audio.volume = target
  // 某些移动浏览器仅允许通过系统按键调整音量。
  volume.value = audio.volume
  event.target.value = audio.volume
}

function toggleExpanded() {
  expanded.value = !expanded.value
  if (!expanded.value) dismissVolume()
}

function dismissVolume() {
  volumeOpen.value = false
}

function dismissOutsideVolume(event) {
  if (volumeOpen.value && !event.composedPath().includes(volumeControl.value)) dismissVolume()
}

function onVisibilityChange() {
  if (document.hidden) dismissVolume()
}

function onEscape() {
  if (!volumeOpen.value) return
  dismissVolume()
  volumeButton.value?.focus()
}

function formatTime(seconds) {
  const value = Math.floor(seconds)
  return `${Math.floor(value / 60)}:${String(value % 60).padStart(2, '0')}`
}

function loadCurrent() {
  const audio = audioRef.value
  const track = currentTrack.value
  if (!audio || !track) return
  ++playToken
  elapsed.value = 0
  duration.value = 0
  playing.value = false
  status.value = ''
  audio.src = `/api/public/music/${track.id}`
  audio.load()
  if (wantsPlayback) requestPlay()
}

async function requestPlay() {
  const token = ++playToken
  try {
    await audioRef.value.play()
  } catch (error) {
    if (token !== playToken) return
    if (error.name === 'NotAllowedError') {
      wantsPlayback = false
    } else if (error.name !== 'AbortError') {
      skipFailedTrack()
    }
  }
}

function skipFailedTrack() {
  if (status.value === 'unavailable') return
  failedTracks.add(currentTrack.value.id)
  if (failedTracks.size === props.tracks.length) {
    ++playToken
    playing.value = false
    status.value = 'unavailable'
    return
  }
  do {
    currentIndex.value = (currentIndex.value + 1) % props.tracks.length
  } while (failedTracks.has(currentTrack.value.id))
  loadCurrent()
}

function onPlaying() {
  playing.value = true
  failedTracks.clear()
  status.value = ''
}

function onEnded() {
  if (props.tracks.length === 1) loadCurrent()
  else stepTrack(1)
}

function onError(event) {
  if (event.currentTarget.error) skipFailedTrack()
}

function togglePlayback() {
  const audio = audioRef.value
  if (playing.value || !audio.paused) {
    ++playToken
    wantsPlayback = false
    audio.pause()
    return
  }
  wantsPlayback = true
  if (status.value === 'unavailable') {
    failedTracks.clear()
    currentIndex.value = 0
    loadCurrent()
  } else {
    requestPlay()
  }
}

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', dismissOutsideVolume, true)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  window.removeEventListener('blur', dismissVolume)
  ++playToken
  const audio = audioRef.value
  audio.pause()
  audio.removeAttribute('src')
  audio.load()
})
</script>

<template>
  <div class="music-widget">
    <button class="music-launcher" type="button" :aria-expanded="expanded" aria-controls="background-music-player"
      :aria-label="expanded ? '收起背景音乐播放器' : '展开背景音乐播放器'" @click="toggleExpanded">
      <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor" aria-hidden="true">
        <path d="M9 5v10.2a4 3 0 1 0 2 2.6V8l8-2v7.2a4 3 0 1 0 2 2.6V2z" />
      </svg>
    </button>
    <div v-show="expanded" id="background-music-player" class="music-player" role="region" aria-label="背景音乐" @keydown.esc="onEscape">
    <audio ref="audioRef" preload="metadata" @playing="onPlaying" @pause="playing = false" @ended="onEnded" @error="onError"
      @timeupdate="syncProgress" @durationchange="syncProgress" @loadedmetadata="syncProgress" />

    <header class="music-heading">
      <p class="music-name" :title="currentTrack.name">{{ currentTrack.name }}</p>
      <p v-if="status === 'unavailable'" class="music-hint" role="status">音乐暂不可播放</p>
    </header>

    <div class="music-progress">
      <span aria-hidden="true">{{ formatTime(elapsed) }}</span>
      <input type="range" aria-label="播放进度" min="0" :max="duration" step="0.01" :value="elapsed" :disabled="!duration"
        :style="{ '--fill': `${duration ? Math.min(100, elapsed / duration * 100) : 0}%` }"
        :aria-valuetext="`${formatTime(elapsed)} / ${formatTime(duration)}`" @input="seek" />
      <span aria-hidden="true">{{ formatTime(duration) }}</span>
      <div ref="volumeControl" class="volume-control">
        <button ref="volumeButton" class="icon-button" type="button" :class="{ 'is-active': volumeOpen }"
          :aria-expanded="volumeOpen" aria-controls="background-music-volume" aria-label="音量设置" title="音量设置"
          @click="volumeOpen = !volumeOpen">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M3 9h4l5-4v14l-5-4H3z" />
            <path v-if="volume > 0" d="M15 9a4 4 0 0 1 0 6m2-9a8 8 0 0 1 0 12" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
            <path v-else d="m16 10 4 4m0-4-4 4" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
          </svg>
        </button>
        <div v-show="volumeOpen" id="background-music-volume" class="music-volume">
          <span class="volume-value" aria-hidden="true">{{ Math.round(volume * 100) }}%</span>
          <input type="range" aria-label="音量" aria-orientation="vertical" min="0" max="1" step="0.01" :value="volume"
            :aria-valuetext="`${Math.round(volume * 100)}%`" @input="setVolume" />
        </div>
      </div>
    </div>

    <div class="music-actions">
      <button class="icon-button mode-toggle" type="button" :aria-label="modeLabel" :title="modeLabel"
        :aria-pressed="shuffled" :class="{ 'is-active': shuffled }" @click="toggleMode">
        <svg viewBox="0 0 24 24" aria-hidden="true" style="fill: none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
          <path v-if="shuffled" d="M4 7h3c4 0 6 10 10 10h3m-3-3 3 3-3 3M4 17h3c1.4 0 2.6-1.4 3.4-2.6M13.6 9.6C14.5 8.2 15.6 7 17 7h3m-3-3 3 3-3 3" />
          <path v-else d="m16 3 4 4-4 4m4-4H7a3 3 0 0 0-3 3m4 11-4-4 4-4m-4 4h13a3 3 0 0 0 3-3" />
        </svg>
      </button>
      <div class="transport" role="group" aria-label="播放控制">
        <button class="icon-button" type="button" aria-label="上一首" title="上一首" :disabled="tracks.length < 2" @click="stepTrack(-1)">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 5h2v14H5zm4 7 10-7v14z" /></svg>
        </button>
        <button class="icon-button music-toggle" type="button" :aria-label="buttonLabel" :title="buttonLabel" @click="togglePlayback">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path v-if="playing" d="M7 5h4v14H7zm6 0h4v14h-4z" />
            <path v-else d="M8 5v14l11-7z" />
          </svg>
        </button>
        <button class="icon-button" type="button" aria-label="下一首" title="下一首" :disabled="tracks.length < 2" @click="stepTrack(1)">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 5l10 7-10 7zm12 0h2v14h-2z" /></svg>
        </button>
      </div>
      <button class="icon-button playlist-toggle" type="button" :class="{ 'is-active': playlistOpen }"
        :aria-expanded="playlistOpen" aria-controls="background-music-playlist" aria-label="播放列表" title="播放列表"
        @click="playlistOpen = !playlistOpen">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M4 5h16v2H4zm0 6h16v2H4zm0 6h9v2H4zm12-2 5 3-5 3z" />
        </svg>
      </button>
    </div>

    <ol v-show="playlistOpen" id="background-music-playlist" class="music-playlist" aria-label="播放列表">
      <li v-for="(track, index) in tracks" :key="track.id">
        <button type="button" :aria-current="index === currentIndex ? 'true' : undefined" @click="selectTrack(index, true)">
          <span class="track-index" aria-hidden="true">{{ index + 1 }}</span>
          <span class="track-name" :title="track.name">{{ track.name }}</span>
          <span v-if="index === currentIndex" class="current-marker">{{ playing ? '播放中' : '当前曲目' }}</span>
        </button>
      </li>
    </ol>
    </div>
  </div>
</template>

<style scoped>
.music-widget {
  position: fixed;
  right: calc(var(--music-inset) + env(safe-area-inset-right, 0px));
  bottom: var(--music-bottom);
  z-index: 30;
  --music-inset: 16px;
  --music-bottom: calc(var(--music-inset) + env(safe-area-inset-bottom, 0px));
  --launcher-size: 44px;
  --panel-gap: 8px;
}
.music-launcher {
  display: grid; place-items: center;
  width: var(--launcher-size); height: var(--launcher-size);
  color: white; background: var(--brand-1); border: 0; border-radius: 50%;
  box-shadow: var(--shadow-soft); cursor: pointer;
}
.music-player {
  position: absolute;
  right: 0;
  bottom: calc(100% + var(--panel-gap));
  box-sizing: border-box;
  display: grid;
  gap: 6px;
  width: min(300px, calc(100vw - 2 * var(--music-inset) - env(safe-area-inset-left, 0px) - env(safe-area-inset-right, 0px)));
  overflow: visible;
  padding: 12px;
  border: 1px solid var(--surface-border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  color: var(--ink-1);
  box-shadow: 0 12px 32px -16px rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(16px);
}

.music-heading {
  min-width: 0;
  text-align: center;
}

.music-name {
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 700;
}

.music-hint {
  margin: 2px 0 0;
  color: var(--ink-3);
  font-size: 11px;
}

.music-progress, .music-volume {
  align-items: center;
  gap: 8px;
  color: var(--ink-3);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

.music-progress {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) 32px auto;
}

.music-progress > span { text-align: center; }

/* 音量按钮和浮层共用外部点击边界，浮层不进入滚动裁剪容器。 */
.volume-control { position: relative; }

.music-playlist {
  position: absolute;
  box-sizing: border-box;
  border: 1px solid var(--surface-border);
  background: #fff;
  box-shadow: 0 8px 24px -12px rgba(15, 23, 42, 0.42);
}

.music-volume {
  position: absolute;
  z-index: 3;
  bottom: calc(100% + 6px);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 7px;
  border: 1px solid var(--surface-border);
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 8px 24px -12px rgba(15, 23, 42, 0.42);
}

.volume-value { min-width: 30px; text-align: center; }

.music-volume input {
  writing-mode: vertical-lr;
  direction: rtl;
  width: 22px;
  height: 88px;
  margin: 0;
  accent-color: var(--brand-1);
  cursor: pointer;
}

.music-progress > input {
  appearance: none;
  width: 100%;
  min-width: 0;
  height: 22px;
  margin: 0;
  background: transparent;
  cursor: pointer;
}

.music-progress > input::-webkit-slider-runnable-track {
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(to right, var(--brand-1) 0 var(--fill), #e2e8f0 var(--fill) 100%);
}

.music-progress > input::-moz-range-track {
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(to right, var(--brand-1) 0 var(--fill), #e2e8f0 var(--fill) 100%);
}

.music-progress > input::-webkit-slider-thumb {
  appearance: none;
  width: 10px;
  height: 10px;
  margin-top: -3px;
  border-radius: 50%;
  background: var(--brand-1);
}

.music-progress > input::-moz-range-thumb {
  width: 10px;
  height: 10px;
  border: 0;
  border-radius: 50%;
  background: var(--brand-1);
}

.music-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
}

.transport {
  grid-column: 2;
  display: flex;
  align-items: center;
  gap: 4px;
}

.icon-button {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--ink-2);
  cursor: pointer;
}

.icon-button:hover:not(:disabled), .icon-button.is-active {
  color: var(--brand-1);
  background: rgba(99, 102, 241, 0.1);
}

.icon-button:disabled, input[type='range']:disabled {
  opacity: 0.45;
  cursor: default;
}

.icon-button:focus-visible, input[type='range']:focus-visible {
  outline: 2px solid var(--brand-1);
  outline-offset: 2px;
}

.icon-button svg { width: 19px; height: 19px; fill: currentColor; }

.music-toggle, .music-toggle:hover:not(:disabled) {
  width: 40px;
  height: 40px;
  color: #fff;
  background: var(--brand-1);
}

.music-toggle:hover:not(:disabled) { filter: brightness(0.9); }
.music-toggle svg { width: 22px; height: 22px; }
.mode-toggle { grid-column: 1; grid-row: 1; }
.playlist-toggle { grid-column: 3; justify-self: end; }

.music-playlist {
  z-index: 2;
  --track-row-height: 36px;
  --playlist-padding: 5px;
  bottom: calc(100% + 8px);
  inset-inline: -1px;
  max-height: min(calc(5 * var(--track-row-height) + 2 * var(--playlist-padding) + 2px), max(32px, calc(100dvh - 230px)), 42dvh);
  overflow-y: auto;
  overscroll-behavior: contain;
  margin: 0;
  padding: var(--playlist-padding);
  border-radius: 12px;
  list-style: none;
}

.music-playlist button {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  height: var(--track-row-height);
  padding: 0 8px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--ink-2);
  font: inherit;
  font-size: 12px;
  text-align: start;
  cursor: pointer;
}

.music-playlist button:focus-visible { outline: 2px solid var(--brand-1); outline-offset: -2px; }
.music-playlist button:hover, .music-playlist button[aria-current] { color: var(--brand-1); background: rgba(99, 102, 241, 0.1); }
.track-index, .current-marker { flex: none; font-size: 11px; }
.track-name { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

@media (max-width: 300px) {
  .music-actions .icon-button:not(.music-toggle) { width: 28px; }
  .transport { gap: 0; }
}

@media (max-width: 640px) {
  .music-widget { --music-inset: 12px; }
}
</style>
