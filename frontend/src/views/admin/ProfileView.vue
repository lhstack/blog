<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { authApi } from '@/api'
import AdminFilePicker from '@/components/admin/AdminFilePicker.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const savingProfile = ref(false)
const savingPassword = ref(false)
const uploadingAvatar = ref(false)

async function uploadAvatar([file]) {
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    message.warning('请选择不超过 5 MB 的头像图片')
    return
  }
  uploadingAvatar.value = true
  try {
    const updated = await authApi.uploadAvatar(file)
    auth.profile = updated
    profileForm.avatar = updated.avatar || ''
    message.success('头像已更新')
  } catch { /* API 已提示失败，当前头像不变。 */ }
  finally { uploadingAvatar.value = false }
}

const profileForm = reactive({ nickname: '', avatar: '', email: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

onMounted(async () => {
  const profile = auth.profile || (await auth.loadProfile())
  if (profile) {
    Object.assign(profileForm, {
      nickname: profile.nickname || '',
      avatar: profile.avatar || '',
      email: profile.email || ''
    })
  }
})

async function saveProfile() {
  if (uploadingAvatar.value || savingProfile.value) return
  if (!profileForm.nickname?.trim()) {
    message.warning('昵称不能为空')
    return
  }
  savingProfile.value = true
  try {
    const updated = await authApi.updateProfile({
      nickname: profileForm.nickname.trim(),
      avatar: profileForm.avatar?.trim() || null,
      email: profileForm.email?.trim() || null
    })
    auth.profile = updated
    message.success('资料已更新')
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    message.warning('请填写完整')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    message.warning('新密码至少 6 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.warning('两次输入的新密码不一致')
    return
  }
  savingPassword.value = true
  try {
    await authApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    message.success('密码已修改，请使用新密码登录')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } finally {
    savingPassword.value = false
  }
}
</script>

<template>
  <div class="profile-page">
    <div class="two-col">
      <section class="glass panel">
        <h3>基本资料</h3>
        <div class="avatar-row">
          <a-avatar :size="64" :src="profileForm.avatar" class="avatar">
            {{ (profileForm.nickname || 'A').slice(0, 1) }}
          </a-avatar>
          <div class="avatar-meta">
            <strong>{{ auth.profile?.username || 'admin' }}</strong>
            <span class="muted">登录账号不可修改</span>
          </div>
        </div>

        <a-form layout="vertical" style="margin-top: 20px">
          <a-form-item label="昵称" required>
            <a-input v-model:value="profileForm.nickname" :maxlength="40" />
          </a-form-item>
          <a-form-item label="账号头像">
            <a-input v-model:value="profileForm.avatar" aria-label="头像 URL" placeholder="https://…" :disabled="uploadingAvatar || savingProfile" allow-clear />
            <AdminFilePicker class="avatar-upload" label="选择图片" accept="image/jpeg,image/png,image/webp"
              hint="最多 5 MB，上传后立即生效" :disabled="savingProfile" :busy="uploadingAvatar" @select="uploadAvatar" />
          </a-form-item>
          <a-form-item label="邮箱">
            <a-input v-model:value="profileForm.email" placeholder="you@example.com" />
          </a-form-item>
          <a-button v-magnetic type="primary" shape="round" :loading="savingProfile" :disabled="uploadingAvatar" @click="saveProfile">
            保存资料
          </a-button>
        </a-form>
      </section>

      <section class="glass panel">
        <h3>修改密码</h3>
        <a-form layout="vertical" style="margin-top: 18px">
          <a-form-item label="原密码" required>
            <a-input-password v-model:value="passwordForm.oldPassword" autocomplete="current-password" />
          </a-form-item>
          <a-form-item label="新密码" required>
            <a-input-password
              v-model:value="passwordForm.newPassword"
              placeholder="至少 6 位"
              autocomplete="new-password"
            />
          </a-form-item>
          <a-form-item label="确认新密码" required>
            <a-input-password v-model:value="passwordForm.confirmPassword" autocomplete="new-password" />
          </a-form-item>
          <a-button v-magnetic type="primary" shape="round" :loading="savingPassword" @click="savePassword">
            更新密码
          </a-button>
        </a-form>

        <div class="security-tip">
          <strong>安全提示</strong>
          <p>默认账号 admin / admin123 仅用于首次启动，请立即修改密码。</p>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.avatar-upload { margin-top: 8px; }
.two-col {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 14px;
  align-items: stretch;
}

.panel {
  padding: 20px 22px;
}

.panel h3 {
  margin: 0 0 18px;
  font-size: 14.5px;
  font-weight: 700;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar {
  box-shadow: 0 12px 28px -14px rgba(99, 102, 241, 0.7);
}

.avatar-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.avatar-meta strong {
  font-size: 16px;
}

.avatar-meta span {
  font-size: 12px;
}

.security-tip {
  margin-top: 22px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: rgba(251, 191, 36, 0.1);
  border: 1px solid rgba(251, 191, 36, 0.24);
  font-size: 12.5px;
  color: #92400e;
}

.security-tip strong {
  display: block;
  margin-bottom: 5px;
}

.security-tip p {
  margin: 0;
  line-height: 1.6;
}

@media (max-width: 1000px) {
  .two-col {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
