<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Modal } from 'ant-design-vue'
import {
  CommentOutlined,
  DashboardOutlined,
  ExportOutlined,
  FileTextOutlined,
  IdcardOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  PaperClipOutlined,
  SettingOutlined,
  TagsOutlined,
  FontSizeOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)

const menuItems = [
  { key: 'admin-dashboard', label: '仪表盘', path: '/admin/dashboard', icon: DashboardOutlined },
  { key: 'admin-articles', label: '文章管理', path: '/admin/articles', icon: FileTextOutlined },
  { key: 'admin-attachments', label: '附件管理', path: '/admin/attachments', icon: PaperClipOutlined },
  { key: 'admin-taxonomy', label: '分类与标签', path: '/admin/taxonomy', icon: TagsOutlined },
  { key: 'admin-comments', label: '评论管理', path: '/admin/comments', icon: CommentOutlined },
  { key: 'admin-about', label: '关于页', path: '/admin/about', icon: IdcardOutlined },
  { key: 'admin-typewriter', label: '首页打字机', path: '/admin/typewriter', icon: FontSizeOutlined },
  { key: 'admin-settings', label: '站点设置', path: '/admin/settings', icon: SettingOutlined },
  { key: 'admin-profile', label: '账号设置', path: '/admin/profile', icon: UserOutlined }
]

const activeKeys = computed(() => {
  const name = route.name
  if (name === 'admin-article-new' || name === 'admin-article-edit') return ['admin-articles']
  return [name]
})

const pageTitle = computed(() => {
  const map = {
    'admin-dashboard': '仪表盘',
    'admin-articles': '文章管理',
    'admin-article-new': '写新文章',
    'admin-article-edit': '编辑文章',
    'admin-attachments': '附件管理',
    'admin-taxonomy': '分类与标签',
    'admin-comments': '评论管理',
    'admin-about': '关于页',
    'admin-typewriter': '首页打字机',
    'admin-settings': '站点设置',
    'admin-profile': '账号设置'
  }
  return map[route.name] || '后台'
})

function onMenuClick({ key }) {
  const item = menuItems.find((entry) => entry.key === key)
  if (item) router.push(item.path)
}

function confirmLogout() {
  Modal.confirm({
    title: '确认退出登录？',
    content: '退出后需要重新输入账号密码。',
    okText: '退出',
    cancelText: '取消',
    okButtonProps: { danger: true },
    async onOk() {
      await auth.logout()
      router.replace('/admin/login')
    }
  })
}

onMounted(() => {
  if (!auth.profile) {
    auth.loadProfile().catch(() => {})
  }
})
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sider glass" :class="{ 'is-collapsed': collapsed }">
      <div class="sider-brand">
        <span class="mark">B</span>
        <span v-if="!collapsed" class="brand-name">后台控制台</span>
      </div>

      <nav class="sider-nav">
        <a-tooltip
          v-for="item in menuItems"
          :key="item.key"
          placement="right"
          :title="collapsed ? item.label : undefined"
        >
          <button
            type="button"
            class="nav-item"
            :class="{ 'is-active': activeKeys.includes(item.key) }"
            @click="onMenuClick({ key: item.key })"
          >
            <component :is="item.icon" class="nav-icon" />
            <span v-if="!collapsed" class="nav-label">{{ item.label }}</span>
          </button>
        </a-tooltip>
      </nav>

      <div class="sider-foot">
        <a-tooltip :title="collapsed ? '展开菜单' : '收起菜单'" placement="right">
          <a-button type="text" size="small" class="collapse-btn" @click="collapsed = !collapsed">
            <MenuUnfoldOutlined v-if="collapsed" />
            <template v-else>
              <MenuFoldOutlined />
              <span>收起</span>
            </template>
          </a-button>
        </a-tooltip>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-header glass">
        <div class="header-left">
          <h1>{{ pageTitle }}</h1>
        </div>
        <div class="header-right">
          <a-tooltip title="打开前台页面">
            <a-button type="text" size="small" class="site-link" @click="router.push('/')">
              <template #icon><ExportOutlined /></template>
              查看站点
            </a-button>
          </a-tooltip>
          <a-dropdown>
            <a class="user-chip" @click.prevent>
              <a-avatar :size="28" :src="auth.profile?.avatar">
                {{ (auth.profile?.nickname || auth.profile?.username || 'A').slice(0, 1) }}
              </a-avatar>
              <span class="user-name">{{ auth.profile?.nickname || auth.profile?.username || '管理员' }}</span>
            </a>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile" @click="router.push('/admin/profile')">账号设置</a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" danger @click="confirmLogout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </header>

      <main class="admin-body">
        <RouterView v-slot="{ Component }">
          <Transition name="route-fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 216px minmax(0, 1fr);
  gap: 18px;
  min-height: 100vh;
  padding: 18px;
  transition: grid-template-columns var(--dur-mid) var(--ease-out);
}

.admin-layout:has(.admin-sider.is-collapsed) {
  grid-template-columns: 76px minmax(0, 1fr);
}

.admin-sider {
  position: sticky;
  top: 18px;
  height: calc(100vh - 36px);
  display: flex;
  flex-direction: column;
  padding: 18px 12px;
  overflow: hidden;
}

.sider-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 6px 18px;
  border-bottom: 1px solid var(--surface-border);
}

.mark {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  border-radius: 9px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, var(--brand-1), var(--brand-2));
  box-shadow: 0 8px 18px -10px rgba(99, 102, 241, 0.9);
}

.brand-name {
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
}

/* 收起态：图标居中，避免圆点/图标贴左，也不保留文字占位 */
.admin-sider.is-collapsed .sider-brand {
  justify-content: center;
  padding-inline: 0;
}

.admin-sider.is-collapsed .nav-item {
  justify-content: center;
  gap: 0;
  padding-inline: 0;
}

.admin-sider.is-collapsed .nav-item:hover {
  transform: none;
}

.sider-nav {
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin-top: 16px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 12px;
  border: none;
  border-radius: 10px;
  background: transparent;
  font-size: 13.5px;
  color: var(--ink-2);
  cursor: pointer;
  text-align: left;
  white-space: nowrap;
  transition: background var(--dur-fast) linear, color var(--dur-fast) linear,
    transform var(--dur-fast) var(--ease-out);
}

.nav-icon {
  font-size: 15px;
  line-height: 1;
  flex-shrink: 0;
  opacity: 0.7;
  transition: opacity var(--dur-fast) linear;
}

.nav-item:hover {
  background: rgba(99, 102, 241, 0.08);
  color: var(--brand-1);
  transform: translateX(2px);
}

.nav-item:hover .nav-icon {
  opacity: 1;
}

.nav-item.is-active {
  background: linear-gradient(120deg, rgba(99, 102, 241, 0.16), rgba(34, 211, 238, 0.12));
  color: var(--brand-1);
  font-weight: 600;
}

.nav-item.is-active .nav-icon {
  opacity: 1;
}

.sider-foot {
  padding-top: 12px;
  border-top: 1px solid var(--surface-border);
  text-align: center;
}

.collapse-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--ink-3);
}

.collapse-btn:hover {
  color: var(--brand-1);
}

.admin-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 22px;
  position: sticky;
  top: 18px;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-left h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 次要入口：文字型按钮，避免和主操作抢视觉焦点 */
.site-link {
  color: var(--ink-3);
  font-size: 13px;
}

.site-link:hover {
  color: var(--brand-1) !important;
  background: rgba(99, 102, 241, 0.08) !important;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.12);
  color: var(--ink-1);
  transition: background var(--dur-fast) linear;
}

.user-chip:hover {
  background: rgba(99, 102, 241, 0.12);
}

.user-name {
  font-size: 13px;
}

.admin-body {
  flex: 1;
  min-width: 0;
}

/* 只约束列表页面，编辑器和设置页仍使用正常页面滚动。 */
.admin-layout:has(.admin-list-page) {
  height: 100dvh;
  min-height: 0;
}

.admin-layout:has(.admin-list-page) .admin-main,
.admin-body:has(.admin-list-page) {
  min-height: 0;
}

.admin-main:has(.admin-list-page) .admin-header { flex: none; }
.admin-body:has(.admin-list-page) { display: grid; }

.admin-body :deep(.admin-list-page) {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
  min-height: 0;
}

.admin-body :deep(.admin-list-scroll) {
  flex: 1;
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
}

.admin-body :deep(.admin-list-pager) {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 14px;
  font-size: 13px;
}

@media (max-width: 900px) {
  .admin-layout,
  .admin-layout:has(.admin-sider.is-collapsed) {
    grid-template-columns: 1fr;
    padding: 12px;
  }
  .admin-layout:has(.admin-list-page) {
    grid-template-rows: auto minmax(0, 1fr);
  }
  .admin-sider {
    position: static;
    height: auto;
    flex-direction: row;
    align-items: center;
    padding: 12px;
    gap: 10px;
  }
  .sider-brand {
    padding: 0;
    border: none;
  }
  .sider-nav {
    flex-direction: row;
    margin: 0;
    overflow-x: auto;
  }
  .sider-foot {
    display: none;
  }
  .admin-header {
    position: static;
  }
  .user-name {
    display: none;
  }
}
</style>
