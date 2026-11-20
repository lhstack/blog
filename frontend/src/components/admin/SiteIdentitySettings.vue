<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { adminApi } from '@/api'

const fields = [
  { key: 'name', label: '站点名称', max: 40 },
  { key: 'title', label: '个人身份／副标题', max: 80 },
  { key: 'description', label: '站点简介', max: 500 }
]
const form = reactive({ name: '', title: '', description: '' })
const loading = ref(false)
const ready = ref(false)
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    Object.assign(form, await adminApi.siteIdentity())
    ready.value = true
  } catch { ready.value = false } finally { loading.value = false }
}

async function save() {
  saving.value = true
  try {
    Object.assign(form, await adminApi.updateSiteIdentity(form))
    message.success('站点资料已保存')
  } catch { /* API 已提示 */ } finally { saving.value = false }
}

onMounted(load)
</script>

<template>
  <section class="glass settings-panel">
    <h3>站点资料</h3>
    <p class="settings-hint">首页、关于页和订阅源共用同一套资料，留空恢复对应默认值。</p>
    <a-spin :spinning="loading">
      <a-form v-if="ready" :model="form" layout="vertical" class="settings-form" @finish="save">
        <a-form-item v-for="field in fields" :key="field.key" :label="field.label" :html-for="`site-${field.key}`">
          <a-textarea v-if="field.key === 'description'" :id="`site-${field.key}`" v-model:value="form[field.key]" :maxlength="field.max" :rows="3" show-count />
          <a-input v-else :id="`site-${field.key}`" v-model:value="form[field.key]" :maxlength="field.max" />
        </a-form-item>
        <div class="settings-actions">
          <a-button type="primary" shape="round" html-type="submit" :loading="saving">保存站点资料</a-button>
        </div>
      </a-form>
      <div v-else-if="!loading" class="settings-actions">
        <a-button @click="load">重新载入</a-button>
      </div>
    </a-spin>
  </section>
</template>
