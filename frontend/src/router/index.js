import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/api/http'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/PublicLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/public/HomeView.vue') },
      { path: 'articles', name: 'articles', component: () => import('@/views/public/ArticleListView.vue') },
      { path: 'articles/:id', name: 'article', component: () => import('@/views/public/ArticleDetailView.vue') },
      { path: 'archives', name: 'archives', component: () => import('@/views/public/ArchiveView.vue') },
      { path: 'about', name: 'about', component: () => import('@/views/public/AboutView.vue') }
    ]
  },
  {
    path: '/admin/login',
    name: 'login',
    component: () => import('@/views/admin/LoginView.vue')
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'admin-dashboard', component: () => import('@/views/admin/DashboardView.vue') },
      { path: 'articles', name: 'admin-articles', component: () => import('@/views/admin/ArticleManageView.vue') },
      { path: 'articles/new', name: 'admin-article-new', component: () => import('@/views/admin/ArticleEditView.vue') },
      { path: 'articles/:id/edit', name: 'admin-article-edit', component: () => import('@/views/admin/ArticleEditView.vue') },
      { path: 'attachments', name: 'admin-attachments', component: () => import('@/views/admin/AttachmentManageView.vue') },
      { path: 'taxonomy', name: 'admin-taxonomy', component: () => import('@/views/admin/TaxonomyView.vue') },
      { path: 'comments', name: 'admin-comments', component: () => import('@/views/admin/CommentManageView.vue') },
      { path: 'profile', name: 'admin-profile', component: () => import('@/views/admin/ProfileView.vue') },
      { path: 'about', name: 'admin-about', component: () => import('@/views/admin/AboutEditView.vue') },
      { path: 'typewriter', name: 'admin-typewriter', component: () => import('@/views/admin/TypewriterSettingsView.vue') },
      { path: 'settings', name: 'admin-settings', component: () => import('@/views/admin/SiteSettingView.vue') }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/public/NotFoundView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, behavior: 'smooth', top: 88 }
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !getToken()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && getToken()) {
    return { name: 'admin-dashboard' }
  }
  return true
})

export default router
