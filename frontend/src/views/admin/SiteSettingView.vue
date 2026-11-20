<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { adminApi } from '@/api'
import SitePresentationSettings from '@/components/admin/SitePresentationSettings.vue'
import SiteAvatarSettings from '@/components/admin/SiteAvatarSettings.vue'
import SiteIdentitySettings from '@/components/admin/SiteIdentitySettings.vue'

const loading = ref(true)
const saving = ref(false)
const updatedAt = ref(null)

const form = reactive({ icpNumber: '', policeNumber: '', policeUrl: '' })

/** 前台页脚仅在备案号非空时渲染，表单需要同步这个规则，否则用户不知道保存后会不会显示。 */
const willShowOnFooter = computed(() => !!(form.icpNumber?.trim() || form.policeNumber?.trim()))

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data = await adminApi.siteSetting()
    form.icpNumber = data.icpNumber || ''
    form.policeNumber = data.policeNumber || ''
    form.policeUrl = data.policeUrl || ''
    updatedAt.value = data.updatedAt || null
  } finally {
    loading.value = false
  }
}

async function save() {
  const policeUrl = form.policeUrl?.trim() || ''
  if (policeUrl && !/^https?:\/\//.test(policeUrl)) {
    message.warning('公安备案链接需以 http:// 或 https:// 开头')
    return
  }
  if (policeUrl && !form.policeNumber?.trim()) {
    message.warning('填写公安备案链接时必须同时填写公安备案号')
    return
  }
  saving.value = true
  try {
    const updated = await adminApi.updateSiteSetting({
      icpNumber: form.icpNumber?.trim() || null,
      policeNumber: form.policeNumber?.trim() || null,
      policeUrl: policeUrl || null
    })
    form.icpNumber = updated.icpNumber || ''
    form.policeNumber = updated.policeNumber || ''
    form.policeUrl = updated.policeUrl || ''
    updatedAt.value = updated.updatedAt || null
    message.success(
      willShowOnFooter.value ? '站点设置已保存，前台页脚已生效' : '站点设置已保存，未填写备案号，页脚不会显示'
    )
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="setting-page">
    <SiteIdentitySettings />
    <section class="glass settings-panel">
      <div class="settings-panel-head">
        <h3>备案信息</h3>
        <span v-if="updatedAt" class="muted updated-at">更新于 {{ updatedAt.replace('T', ' ') }}</span>
      </div>
      <p class="settings-hint">管理前台页脚显示的 ICP 备案和公安备案。</p>

      <a-spin :spinning="loading">
        <a-form layout="vertical" class="settings-form">
          <a-form-item label="ICP 备案号">
            <a-input
              v-model:value="form.icpNumber"
              :maxlength="60"
              placeholder="如：沪ICP备2021000000号-1"
              allow-clear
            />
            <template #extra>
              <span class="hint">留空则前台页脚不展示备案行；链接固定指向 beian.miit.gov.cn。</span>
            </template>
          </a-form-item>

          <a-form-item label="公安备案号">
            <a-input
              v-model:value="form.policeNumber"
              :maxlength="60"
              placeholder="如：沪公网安备31010402000000号"
              allow-clear
            />
          </a-form-item>

          <a-form-item label="公安备案链接">
            <a-input
              v-model:value="form.policeUrl"
              :maxlength="200"
              placeholder="https://beian.mps.gov.cn/#/query/webSearch?code=…"
              allow-clear
            />
            <template #extra>
              <span class="hint">留空时公安备案号取 ICP 查询入口作为链接。</span>
            </template>
          </a-form-item>

          <div class="preview-row" :class="{ 'is-off': !willShowOnFooter }">
            <span class="preview-dot" />
            <span>{{ willShowOnFooter ? '前台页脚将显示此备案行' : '未填写备案号，前台页脚不会显示备案行' }}</span>
          </div>
          <div class="settings-actions">
            <a-button v-magnetic type="primary" shape="round" :loading="saving" @click="save">
              保存设置
            </a-button>
            <a-button :disabled="saving" @click="load">重新载入</a-button>
          </div>
        </a-form>
      </a-spin>
    </section>

    <SiteAvatarSettings class="settings-panel-wide" />
    <SitePresentationSettings />
  </div>
</template>

<style scoped>
.setting-page {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.setting-page :deep(.settings-panel) {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 20px clamp(14px, 2vw, 22px);
}

.setting-page :deep(.settings-panel-wide) { grid-column: 1 / -1; }

.setting-page :deep(.settings-panel-head) {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 4px 12px;
}

.setting-page :deep(.settings-panel h3) {
  margin: 0;
  font-size: 14.5px;
  font-weight: 700;
}

.setting-page :deep(.settings-panel > .settings-hint) { margin: 6px 0 16px; }
.setting-page :deep(.settings-hint), .updated-at, .hint {
  font-size: 12px;
  color: var(--ink-3);
}

/* 让表单撑到卡片底部，保存按钮与同排卡片对齐。 */
.setting-page :deep(.ant-spin-nested-loading),
.setting-page :deep(.ant-spin-container),
.setting-page :deep(.settings-form) {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
}

.setting-page :deep(.settings-actions) {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 10px;
  margin-top: auto;
  padding-top: 12px;
}

@media (max-width: 1000px) {
  .setting-page { grid-template-columns: minmax(0, 1fr); }
}

/* 可见性预告：备案号为空时保存后页脚不会出现，必须让用户提前知道 */
.preview-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  padding: 9px 12px;
  border-radius: var(--radius-md);
  font-size: 12.5px;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.24);
  color: #166534;
}

.preview-row.is-off {
  background: rgba(148, 163, 184, 0.1);
  border-color: var(--surface-border);
  color: var(--ink-3);
}

.preview-dot {
  width: 6px;
  height: 6px;
  flex-shrink: 0;
  border-radius: 50%;
  background: currentColor;
}
</style>