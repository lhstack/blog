import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import { attachmentKind } from './attachments'

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(code, { language: lang }).value}</code></pre>`
      } catch {
        /* 落空时走无高亮分支 */
      }
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(code)}</code></pre>`
  }
})

// 外链统一在新窗口打开，并加 rel 防止 tabnabbing
const defaultLinkOpen =
  md.renderer.rules.link_open ||
  ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options))

md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const href = tokens[idx].attrGet('href') || ''
  if (env.privateAttachments && attachmentKind(href)) {
    tokens[idx].attrSet('data-attachment-link', href)
    tokens[idx].attrSet('href', '#')
  }
  if (/^https?:\/\//.test(href)) {
    tokens[idx].attrSet('target', '_blank')
    tokens[idx].attrSet('rel', 'noopener noreferrer')
  }
  return defaultLinkOpen(tokens, idx, options, env, self)
}

const defaultImage = md.renderer.rules.image
md.renderer.rules.image = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  const url = token.attrGet('src') || ''
  const kind = attachmentKind(url)
  if (!kind || kind === 'download') return defaultImage(tokens, idx, options, env, self)
  const caption = md.utils.escapeHtml(self.renderInlineAsText(token.children, options, env))
  const src = `${env.privateAttachments ? 'data-attachment-src' : 'src'}="${md.utils.escapeHtml(url)}"`
  if (kind === 'image') return `<img class="attachment-media attachment-image" ${src} alt="${caption}" loading="lazy">`
  return `<${kind} class="attachment-media" ${src} controls preload="none" aria-label="${caption}"></${kind}>`
}

/** 生成目录与锚点共用的 heading id。 */
function slugifyHeading(text) {
  return String(text)
    .trim()
    .toLowerCase()
    .replace(/[^\w\u4e00-\u9fa5]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

/**
 * 同一篇出现重复标题时必须追加序号，否则后一个锚点会指向前一个。
 * 渲染与目录提取共用同一规则，否则两边 id 不一致。
 */
function uniqueHeadingId(base, seen) {
  if (!base) return ''
  const used = seen.get(base) || 0
  seen.set(base, used + 1)
  return used === 0 ? base : `${base}-${used}`
}

// 目录跳转依赖标题带 id：默认渲染不会输出 id，必须在 heading_open 上补。
md.renderer.rules.heading_open = (tokens, idx, options, env, self) => {
  const level = Number(tokens[idx].tag.replace('h', ''))
  if (level === 2 || level === 3) {
    env.headingSeen = env.headingSeen || new Map()
    const inline = tokens[idx + 1]
    const id = uniqueHeadingId(slugifyHeading(inline?.content || ''), env.headingSeen)
    if (id) {
      tokens[idx].attrSet('id', id)
    }
  }
  return self.renderToken(tokens, idx, options)
}

export function renderMarkdown(source, privateAttachments = false) {
  if (!source) return ''
  return md.render(source, { privateAttachments })
}

/** 生成目录：把 h2/h3 抽出来，供文章页侧边导航使用。 */
export function extractHeadings(source) {
  if (!source) return []
  const tokens = md.parse(source, {})
  const seen = new Map()
  const headings = []
  tokens.forEach((token, index) => {
    if (token.type !== 'heading_open') return
    const level = Number(token.tag.replace('h', ''))
    if (level !== 2 && level !== 3) return
    const inline = tokens[index + 1]
    const text = inline?.content || ''
    const id = uniqueHeadingId(slugifyHeading(text), seen)
    headings.push({ level, text, id })
  })
  return headings
}

export function plainText(source, limit = 160) {
  if (!source) return ''
  const text = source
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/[#*_>`~\-|[\]]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  return text.length > limit ? `${text.slice(0, limit)}…` : text
}
