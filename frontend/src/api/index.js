import http from './http'

function uploadFile(path, file) {
  const form = new FormData()
  form.append('file', file)
  return http.post(path, form, { timeout: 120000 })
}

export const publicApi = {
  site: () => http.get('/public/site'),
  about: () => http.get('/public/about'),
  typewriter: (config) => http.get('/public/typewriter', config),
  home: (config) => http.get('/public/home', config),
  articles: (params) => http.get('/public/articles', { params }),
  article: (id) => http.get(`/public/articles/${id}`),
  archives: () => http.get('/public/archives'),
  categories: () => http.get('/public/categories'),
  tags: () => http.get('/public/tags'),
  comments: (id) => http.get(`/public/articles/${id}/comments`),
  submitComment: (id, payload) => http.post(`/public/articles/${id}/comments`, payload)
}

export const authApi = {
  login: (payload) => http.post('/auth/login', payload),
  profile: () => http.get('/admin/profile'),
  updateProfile: (payload) => http.put('/admin/profile', payload),
  uploadAvatar: (file) => uploadFile('/admin/profile/avatar', file),
  changePassword: (payload) => http.put('/admin/profile/password', payload),
  logout: () => http.post('/admin/profile/logout')
}

export const adminApi = {
  dashboard: () => http.get('/admin/dashboard'),

  attachments: (params) => http.get('/admin/attachments', { params }),
  uploadAttachment: (file) => uploadFile('/admin/attachments', file),
  renameAttachment: (id, name) => http.put(`/admin/attachments/${id}`, { name }),
  deleteAttachment: (id) => http.delete(`/admin/attachments/${id}`),
  attachmentContent: (url, signal) => http.get(url.replace('/api/public/', '/admin/'), { responseType: 'blob', signal }),

  articles: (params, config) => http.get('/admin/articles', { ...config, params }),
  article: (id, config) => http.get(`/admin/articles/${id}`, config),
  createArticle: (payload) => http.post('/admin/articles', payload),
  updateArticle: (id, payload) => http.put(`/admin/articles/${id}`, payload),
  publishArticle: (id, pinned = false) =>
    http.post(`/admin/articles/${id}/publish`, null, { params: { pinned } }),
  withdrawArticle: (id) => http.post(`/admin/articles/${id}/withdraw`),
  pinArticle: (id, pinned) => http.post(`/admin/articles/${id}/pin`, null, { params: { pinned } }),
  deleteArticle: (id) => http.delete(`/admin/articles/${id}`),

  categories: () => http.get('/admin/categories'),
  createCategory: (payload) => http.post('/admin/categories', payload),
  updateCategory: (id, payload) => http.put(`/admin/categories/${id}`, payload),
  deleteCategory: (id) => http.delete(`/admin/categories/${id}`),

  tags: () => http.get('/admin/tags'),
  createTag: (payload) => http.post('/admin/tags', payload),
  updateTag: (id, payload) => http.put(`/admin/tags/${id}`, payload),
  deleteTag: (id) => http.delete(`/admin/tags/${id}`),

  comments: (params) => http.get('/admin/comments', { params }),
  moderateComment: (id, action) =>
    http.post(`/admin/comments/${id}/moderate`, null, { params: { action } }),
  deleteComment: (id) => http.delete(`/admin/comments/${id}`),

  siteIdentity: () => http.get('/admin/settings/site/identity'),
  updateSiteIdentity: (data) => http.put('/admin/settings/site/identity', data),
  siteSetting: () => http.get('/admin/settings/site'),
  updateSiteSetting: (payload) => http.put('/admin/settings/site', payload),
  sitePresentation: () => http.get('/admin/settings/site/presentation'),
  about: () => http.get('/admin/settings/site/presentation/about'),
  updateAbout: (markdown) => http.put('/admin/settings/site/presentation/about', { markdown }),
  typewriterSettings: () => http.get('/admin/settings/typewriter'),
  saveTypewriterSettings: (entries) => http.put('/admin/settings/typewriter', { entries }),
  siteAvatar: () => http.get('/admin/settings/site/avatar'),
  updateSiteAvatar: (url) => http.put('/admin/settings/site/avatar', { url }),
  uploadSiteAvatar: (file) => {
    const data = new FormData()
    data.append('file', file)
    return http.post('/admin/settings/site/avatar', data, { timeout: 60000 })
  },
  uploadMusic: (file, name) => {
    const data = new FormData()
    data.append('file', file)
    if (name) data.append('name', name)
    return http.post('/admin/settings/site/presentation/music', data, { timeout: 120000 })
  },
  renameMusic: (id, name) => http.put(`/admin/settings/site/presentation/music/${id}/name`, { name }),
  reorderMusic: (ids) => http.put('/admin/settings/site/presentation/music/order', { ids }),
  removeMusic: (id) => http.delete(`/admin/settings/site/presentation/music/${id}`)
}
