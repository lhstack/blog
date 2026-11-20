import { adminApi } from '@/api'

export async function downloadPrivate(url, name) {
  const blob = await adminApi.attachmentContent(url)
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = name.trim() || '附件'
  document.body.append(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
}
