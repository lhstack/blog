const attachmentPattern = /^\/media\/([a-z0-9][a-z0-9-]{0,47}-[0-9a-f]{16}\.([a-z0-9]{2,4}))$/
const kinds = new Map([
  ...['jpg', 'png', 'webp', 'gif', 'avif'].map(ext => [ext, 'image']),
  ...['wav', 'mp3', 'm4a'].map(ext => [ext, 'audio']),
  ...['mp4', 'webm'].map(ext => [ext, 'video']),
  ['bin', 'download']
])

export function attachmentFilename(url) {
  return attachmentPattern.exec(url)?.[1]
}

export function attachmentKind(url) {
  return kinds.get(attachmentPattern.exec(url)?.[2])
}

export function attachmentMarkdown(file) {
  const label = file.name.replace(/[\\`*_\[\]<>!&~]/g, (character) => `\\${character}`)
  return `${file.kind === 'download' ? '' : '!'}[${label}](${file.url})`
}

export function fileSize(bytes) {
  return bytes >= 1024 * 1024 ? `${(bytes / (1024 * 1024)).toFixed(1)} MB`
    : bytes >= 1024 ? `${(bytes / 1024).toFixed(1)} KB` : `${bytes} B`
}

export async function copyText(text) {
  if (navigator.clipboard?.writeText) {
    try { await navigator.clipboard.writeText(text); return } catch { /* allow HTTP deployments */ }
  }
  const input = document.createElement('textarea')
  const focused = document.activeElement
  input.value = text
  input.readOnly = true
  input.style.cssText = 'position:fixed;opacity:0;pointer-events:none'
  document.body.append(input)
  try {
    input.select()
    if (!document.execCommand('copy')) throw new Error('复制失败')
  } finally {
    input.remove()
    focused?.focus()
  }
}
