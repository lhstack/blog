const attachmentPattern = /^\/api\/public\/attachments\/([0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12})\/(image|audio|video|download)$/

export function attachmentKind(url) {
  return attachmentPattern.exec(url)?.[2]
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
