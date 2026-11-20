<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { adminApi } from '@/api'

const rows = ref([])
const zone = ref('')
const loading = ref(true)
const ready = ref(false)
const saving = ref(false)
const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'].map((label, i) => ({ label, value: 1 << i }))
let nextKey = 0
const clock = (n) => `${String(Math.floor(n / 60)).padStart(2, '0')}:${String(n % 60).padStart(2, '0')}`
const minutes = (s) => Number(s.slice(0, 2)) * 60 + Number(s.slice(3))

function apply(data) {
  zone.value = data.zone
  rows.value = data.entries.map(entry => ({
    key: ++nextKey, text: entry.text, days: weekdays.filter(day => entry.daysMask & day.value).map(day => day.value),
    start: clock(entry.startMinute), end: clock(entry.endMinute)
  }))
}

async function load() {
  loading.value = true
  try {
    apply(await adminApi.typewriterSettings())
    ready.value = true
  } catch { /* API 已提示 */ } finally { loading.value = false }
}

function add() {
  rows.value.push({ key: ++nextKey, text: '', days: weekdays.map(day => day.value), start: '00:00', end: '00:00' })
}

function move(index, offset) {
  const [row] = rows.value.splice(index, 1)
  rows.value.splice(index + offset, 0, row)
}

async function save() {
  const invalid = rows.value.findIndex(row => !row.text.trim() || !row.days.length || !row.start || !row.end)
  if (invalid >= 0) return message.warning(`请填写第 ${invalid + 1} 条文案的内容、星期和时段`)
  saving.value = true
  try {
    apply(await adminApi.saveTypewriterSettings(rows.value.map(row => ({ text: row.text.trim(),
      daysMask: row.days.reduce((mask, day) => mask | day, 0), startMinute: minutes(row.start), endMinute: minutes(row.end) }))))
    message.success('打字机配置已保存')
  } catch { /* API 已提示，保留编辑 */ } finally { saving.value = false }
}

onMounted(load)
</script>

<template>
  <section class="glass typewriter-settings">
    <h2>首页打字机</h2>
    <p class="muted">按列表顺序循环播放当前生效的文案。未命中时显示站点简介。{{ zone ? `站点时区：${zone}。` : '' }}</p>
    <p class="muted">开始和结束相同表示全天。结束早于开始时跨到次日，勾选的是开始日。例如周五 22:00–06:00 会持续到周六早上。</p>
    <a-spin :spinning="loading">
      <template v-if="ready">
        <a-empty v-if="!rows.length" description="尚未配置文案" />
        <section v-for="(row, index) in rows" :key="row.key" class="text-entry">
          <div class="entry-head"><strong>文案 {{ index + 1 }}</strong>
            <a-space wrap>
              <a-button size="small" :disabled="saving || index === 0" @click="move(index, -1)">上移</a-button>
              <a-button size="small" :disabled="saving || index === rows.length - 1" @click="move(index, 1)">下移</a-button>
              <a-button size="small" danger :disabled="saving" @click="rows.splice(index, 1)">移除</a-button>
            </a-space>
          </div>
          <a-textarea v-model:value="row.text" :maxlength="200" :rows="2" :disabled="saving" placeholder="输入首页文案" aria-label="文案内容" show-count />
          <a-checkbox-group v-model:value="row.days" :options="weekdays" :disabled="saving" aria-label="生效星期" />
          <div class="entry-times">
            <label>开始 <a-time-picker v-model:value="row.start" :disabled="saving" value-format="HH:mm" format="HH:mm" :allow-clear="false" /></label>
            <label>结束 <a-time-picker v-model:value="row.end" :disabled="saving" value-format="HH:mm" format="HH:mm" :allow-clear="false" /></label>
          </div>
        </section>
        <a-space wrap>
          <a-button :disabled="saving || rows.length >= 30" @click="add">添加文案</a-button>
          <a-button type="primary" :loading="saving" @click="save">保存配置</a-button>
        </a-space>
      </template>
      <a-button v-else-if="!loading" @click="load">重新载入</a-button>
    </a-spin>
  </section>
</template>

<style scoped>
.typewriter-settings { padding: 20px; }
h2 { margin: 0 0 8px; font-size: 18px; }
.text-entry { display: grid; gap: 12px; padding: 16px; margin-bottom: 12px; border: 1px solid var(--surface-border); border-radius: 12px; }
.entry-head, .entry-times { display: flex; align-items: center; flex-wrap: wrap; gap: 8px 16px; }
.entry-head { justify-content: space-between; }
</style>
