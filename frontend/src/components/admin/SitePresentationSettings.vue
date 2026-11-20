<script setup>
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { adminApi } from '@/api'

const MAX_TRACKS = 10
const MAX_FILE_SIZE = 20 * 1024 * 1024
const tracks = ref([])
const loading = ref(true)
const ready = ref(false)
const musicAction = ref('')
const fileInput = ref(null)
const uploadFile = ref(null)
const uploadName = ref('')
const renameTarget = ref(null)
const renameName = ref('')
const musicBusy = computed(() => Boolean(musicAction.value))

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data = await adminApi.sitePresentation()
    tracks.value = data.tracks
    ready.value = true
  } catch {
    // http 拦截器已经提示失败；首次加载失败时禁用表单，避免覆盖已有配置。
  } finally {
    loading.value = false
  }
}

function selectMusic(event) {
  const file = event.target.files[0]
  event.target.value = ''
  if (!file || musicBusy.value || tracks.value.length >= MAX_TRACKS) return
  if (!/\.mp3$/i.test(file.name) || !file.size || file.size > MAX_FILE_SIZE) {
    message.warning('请选择不超过 20 MB 的 MP3 文件')
    return
  }
  uploadFile.value = file
  uploadName.value = ''
}

async function uploadMusic() {
  if (!uploadFile.value) return
  await runMusicAction('upload', async () => {
    const track = await adminApi.uploadMusic(uploadFile.value, uploadName.value.trim())
    tracks.value = [...tracks.value, track]
    uploadFile.value = null
    message.success('已加入背景音乐播放列表')
  })
}

function startRename(track) {
  renameTarget.value = track
  renameName.value = track.name
}

async function saveName() {
  if (!renameTarget.value || !renameName.value.trim()) return
  await runMusicAction('rename', async () => {
    const track = await adminApi.renameMusic(renameTarget.value.id, renameName.value.trim())
    tracks.value = tracks.value.map((entry) => entry.id === track.id ? track : entry)
    renameTarget.value = null
    message.success('曲名已更新')
  })
}

async function moveTrack(index, step) {
  const target = index + step
  if (target < 0 || target >= tracks.value.length) return
  const order = tracks.value.map(({ id }) => id)
  ;[order[index], order[target]] = [order[target], order[index]]
  await runMusicAction('reorder', async () => {
    tracks.value = await adminApi.reorderMusic(order)
    message.success('播放顺序已更新')
  })
}

async function removeTrack(track) {
  await runMusicAction('remove', async () => {
    await adminApi.removeMusic(track.id)
    tracks.value = tracks.value.filter(({ id }) => id !== track.id)
    message.success('已移除背景音乐')
  })
}

async function runMusicAction(action, run) {
  if (musicBusy.value) return
  musicAction.value = action
  try {
    await run()
  } catch {
    // 保留上次已知的列表，错误由 http 拦截器提示。
  } finally {
    musicAction.value = ''
  }
}
</script>

<template>
  <section class="glass settings-panel settings-panel-wide">
    <div class="settings-panel-head">
      <h3>背景音乐</h3>
      <span v-if="ready" class="settings-hint">{{ tracks.length }} / {{ MAX_TRACKS }} 首</span>
    </div>
    <p class="settings-hint">支持 MP3，单首不超过 20 MB。上传即生效，前台按下列顺序循环播放。</p>
    <a-spin :spinning="loading">
      <div v-if="ready">
        <ol v-if="tracks.length" class="track-list">
          <li v-for="(track, index) in tracks" :key="track.id" class="track-row">
            <span class="track-number">{{ index + 1 }}</span>
            <span class="track-name" :title="track.name">{{ track.name }}</span>
            <div class="track-actions">
              <a-button type="link" size="small" :disabled="musicBusy" @click="startRename(track)">改名</a-button>
              <a-button type="link" size="small" :disabled="musicBusy || index === 0" @click="moveTrack(index, -1)">上移</a-button>
              <a-button type="link" size="small" :disabled="musicBusy || index === tracks.length - 1" @click="moveTrack(index, 1)">下移</a-button>
              <a-popconfirm title="从播放列表移除这首音乐？" ok-text="移除" cancel-text="取消" :disabled="musicBusy" @confirm="removeTrack(track)">
                <a-button type="link" danger size="small" :disabled="musicBusy">移除</a-button>
              </a-popconfirm>
            </div>
          </li>
        </ol>
        <p v-else class="settings-hint empty">尚未上传音乐，前台不会显示播放器。</p>
        <input ref="fileInput" type="file" accept=".mp3,audio/mpeg" hidden @change="selectMusic" />
        <div class="settings-actions">
          <a-button
            type="primary"
            shape="round"
            :loading="musicAction === 'upload'"
            :disabled="loading || musicBusy || tracks.length >= MAX_TRACKS"
            @click="fileInput.click()"
          >上传 MP3</a-button>
          <a-button :disabled="loading || musicBusy" @click="load">重新载入</a-button>
        </div>
      </div>
    </a-spin>

    <a-modal :open="Boolean(uploadFile)" title="上传背景音乐" ok-text="上传" cancel-text="取消"
      :confirm-loading="musicAction === 'upload'" :closable="!musicBusy" :mask-closable="!musicBusy"
      :keyboard="!musicBusy" :cancel-button-props="{ disabled: musicBusy }"
      @ok="uploadMusic" @cancel="uploadFile = null">
      <p class="track-name">{{ uploadFile?.name }}</p>
      <a-input v-model:value="uploadName" :maxlength="120" :disabled="musicBusy"
        aria-label="曲目显示名称" placeholder="显示名称（可选，留空使用文件名）" />
    </a-modal>
    <a-modal :open="Boolean(renameTarget)" title="修改曲名" ok-text="保存" cancel-text="取消"
      :confirm-loading="musicAction === 'rename'" :closable="!musicBusy" :mask-closable="!musicBusy"
      :keyboard="!musicBusy" :cancel-button-props="{ disabled: musicBusy }"
      :ok-button-props="{ disabled: !renameName.trim() }" @ok="saveName" @cancel="renameTarget = null">
      <a-input v-model:value="renameName" :maxlength="120" :disabled="musicBusy" aria-label="新曲名" />
    </a-modal>
  </section>
</template>

<style scoped>
.empty {
  margin: 0;
  padding: 8px 0;
}

.track-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.track-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 1px solid var(--surface-border);
  border-radius: var(--radius-md);
}

.track-number {
  color: var(--ink-3);
  font-size: 12px;
}

.track-name {
  flex: 1;
  min-width: 5em;
  overflow-wrap: anywhere;
}

.track-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
