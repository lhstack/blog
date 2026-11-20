// 端到端页面验证脚本：通过 CDP 驱动 Chrome，采集控制台错误并截图。
// 用法：node tools/verify-ui.mjs [baseUrl]
import { spawn } from 'node:child_process'
import { writeFileSync, mkdirSync } from 'node:fs'
import { setTimeout as sleep } from 'node:timers/promises'

const CHROME = 'D:\\Program Files\\chrome-for-testing\\chrome.exe'
const BASE = process.argv[2] || 'http://127.0.0.1:5173'
const OUT = 'D:\\Projects\\Java\\blog\\.shots'
const PORT = 9333
mkdirSync(OUT, { recursive: true })

const chrome = spawn(
  CHROME,
  [
    '--headless=new',
    '--disable-gpu',
    '--hide-scrollbars',
    '--no-first-run',
    '--no-default-browser-check',
    `--remote-debugging-port=${PORT}`,
    '--user-data-dir=D:\\Projects\\Java\\blog\\.chrome-profile',
    '--window-size=1440,1000',
    'about:blank'
  ],
  { stdio: 'ignore', detached: false }
)

const waitForTarget = async () => {
  for (let i = 0; i < 60; i++) {
    try {
      const res = await fetch(`http://127.0.0.1:${PORT}/json/version`)
      if (res.ok) return await res.json()
    } catch {
      /* 等待 CDP 端口就绪 */
    }
    await sleep(250)
  }
  throw new Error('CDP 未就绪')
}

const version = await waitForTarget()
const ws = new WebSocket(version.webSocketDebuggerUrl)
await new Promise((resolve, reject) => {
  ws.onopen = resolve
  ws.onerror = reject
})

let seq = 0
const pending = new Map()
const events = []

ws.onmessage = (msg) => {
  const data = JSON.parse(msg.data)
  if (data.id && pending.has(data.id)) {
    const { resolve, reject } = pending.get(data.id)
    pending.delete(data.id)
    data.error ? reject(new Error(JSON.stringify(data.error))) : resolve(data.result)
  } else if (data.method) {
    events.push(data)
  }
}

const send = (method, params = {}, sessionId) =>
  new Promise((resolve, reject) => {
    const id = ++seq
    pending.set(id, { resolve, reject })
    ws.send(JSON.stringify({ id, method, params, sessionId }))
  })

// 建立页面会话
const { targetInfos } = await send('Target.getTargets')
const pageTarget = targetInfos.find((t) => t.type === 'page')
const { sessionId } = await send('Target.attachToTarget', {
  targetId: pageTarget.targetId,
  flatten: true
})

const sendS = (method, params) => send(method, params, sessionId)
await sendS('Page.enable')
await sendS('Runtime.enable')
await sendS('Log.enable')
await sendS('Network.enable')
await sendS('Emulation.setDeviceMetricsOverride', {
  width: 1440,
  height: 1000,
  deviceScaleFactor: 1,
  mobile: false
})

// profile 是复用的：上次验证留下的 token 已随服务重启失效，
// 不清掉会让 /admin/login 的守卫直接跳仪表盘并产生预期外的 401。
await sendS('Page.navigate', { url: `${BASE}/` })
await sleep(800)
await sendS('Runtime.evaluate', { expression: 'localStorage.clear()' })

const consoleErrors = []
const failedRequests = []

const collect = () => {
  for (const evt of events.splice(0)) {
    if (evt.method === 'Runtime.consoleAPICalled' && evt.params.type === 'error') {
      consoleErrors.push(evt.params.args.map((a) => a.value ?? a.description).join(' '))
    }
    if (evt.method === 'Runtime.exceptionThrown') {
      consoleErrors.push(evt.params.exceptionDetails.exception?.description || 'Uncaught exception')
    }
    if (evt.method === 'Log.entryAdded' && evt.params.entry.level === 'error') {
      consoleErrors.push(evt.params.entry.text)
    }
    if (evt.method === 'Network.loadingFailed') {
      failedRequests.push(evt.params.errorText)
    }
  }
}

const navigate = async (path, waitMs = 2200) => {
  await sendS('Page.navigate', { url: `${BASE}${path}` })
  await sleep(waitMs)
  collect()
}

const shot = async (name) => {
  const { data } = await sendS('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false })
  writeFileSync(`${OUT}\\${name}.png`, Buffer.from(data, 'base64'))
}

const evaluate = async (expression) => {
  const res = await sendS('Runtime.evaluate', {
    expression,
    awaitPromise: true,
    returnByValue: true
  })
  return res.result?.value
}

const report = []
const check = async (label, path, shotName, assertions) => {
  await navigate(path)
  const payload = await assertions()
  if (shotName) await shot(shotName)
  report.push({ label, path, ...payload })
}

await check('首页', '/', 'home', async () => ({
  hero: await evaluate(`document.querySelector('.hero-title')?.textContent?.trim()`),
  cards: await evaluate(`document.querySelectorAll('.article-card').length`),
  canvas: await evaluate(`!!document.querySelector('.particle-canvas')`),
  glow: await evaluate(`!!document.querySelector('.cursor-glow')`),
  bodyBg: await evaluate(`getComputedStyle(document.querySelector('.aurora-stage')).backgroundImage.slice(0,40)`),
  live: await evaluate(`document.querySelector('.live-head')?.textContent?.trim()`)
}))

await check('文章列表', '/articles', 'articles', async () => ({
  count: await evaluate(`document.querySelectorAll('.article-card').length`),
  filters: await evaluate(`document.querySelectorAll('.filter-chip').length`),
  total: await evaluate(`document.querySelector('.list-head p')?.textContent?.trim()`)
}))

// 详情页需要真实的已发布文章 id：库里可能为空，不能硬编码 /articles/1。
const firstArticleId = await (async () => {
  try {
    const res = await fetch(`${BASE}/api/public/articles?page=1&size=1`)
    if (!res.ok) return null
    const data = await res.json()
    return data.items?.[0]?.id ?? null
  } catch {
    return null
  }
})()

if (firstArticleId) {
  await check('文章详情', `/articles/${firstArticleId}`, 'detail', async () => ({
    title: await evaluate(`document.querySelector('.article-head h1')?.textContent?.trim()`),
    headings: await evaluate(`document.querySelectorAll('.markdown-body h2').length`),
    codeBlocks: await evaluate(`document.querySelectorAll('.markdown-body pre').length`),
    toc: await evaluate(`document.querySelectorAll('.toc-card li').length`),
    comments: await evaluate(`document.querySelectorAll('.comment-item').length`),
    progress: await evaluate(`!!document.querySelector('.read-progress')`)
  }))
} else {
  report.push({ label: '文章详情', path: '(skipped)', skipped: '库中没有已发布文章' })
}

await check('归档', '/archives', 'archives', async () => ({
  months: await evaluate(`document.querySelectorAll('.month-block').length`),
  items: await evaluate(`document.querySelectorAll('.month-block li').length`)
}))

await check('关于', '/about', 'about', async () => ({
  h1: await evaluate(`document.querySelector('.hero-card h1')?.textContent?.trim()`),
  stack: await evaluate(`document.querySelectorAll('.stack-card').length`)
}))

await check('登录页', '/admin/login', 'login', async () => ({
  title: await evaluate(`document.querySelector('.login-brand h1')?.textContent?.trim()`)
}))

// 后台：先取真实 token 再访问受保护页面（不要在取到有效凭证前访问后台，否则会产生预期内的 401）
// 登录地址跟随 BASE：否则换端口（如单体 8080、dev 5173）验证时会取不到 token。
const authResp = await fetch(`${BASE}/api/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin123' })
}).then((r) => r.json())
await evaluate(`localStorage.setItem('blog-token', ${JSON.stringify(authResp.token)})`)

await check('后台仪表盘', '/admin/dashboard', 'admin-dashboard', async () => ({
  cards: await evaluate(`document.querySelectorAll('.stat-card').length`),
  bars: await evaluate(`document.querySelectorAll('.bar-item').length`),
  recent: await evaluate(`document.querySelectorAll('.recent-list li').length`)
}))

// 收起态：只剩圆点看不出菜单含义，必须验证收起后图标仍在、文字消失。
await navigate('/admin/dashboard', 1200)
const expandedWidth = await evaluate(`document.querySelector('.admin-sider')?.getBoundingClientRect().width`)
await evaluate(`document.querySelector('.collapse-btn')?.click()`)
await sleep(700)
const collapsed = {
  siderWidth: await evaluate(`document.querySelector('.admin-sider')?.getBoundingClientRect().width`),
  labels: await evaluate(`document.querySelectorAll('.nav-label').length`),
  icons: await evaluate(`document.querySelectorAll('.nav-icon svg').length`),
  brandHidden: await evaluate(`!document.querySelector('.brand-name')`),
  // 收起后工具栏必须还在（不能因为宽度收缩把内容挤掉）
  siteLinkVisible: await evaluate(
    `(() => { const el = document.querySelector('.site-link'); if (!el) return false; const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0 })()`
  )
}
await shot('admin-collapsed')
await evaluate(`document.querySelector('.collapse-btn')?.click()`)
await sleep(700)
report.push({ label: '后台侧栏收起', path: '/admin/dashboard', expandedWidth, collapsed })

await check('文章管理', '/admin/articles', 'admin-articles', async () => ({
  rows: await evaluate(`document.querySelectorAll('.ant-table-tbody tr').length`),
  toolbar: await evaluate(`!!document.querySelector('.toolbar')`)
}))

await check('写文章', '/admin/articles/new', 'admin-edit', async () => ({
  editor: await evaluate(`!!document.querySelector('.content-input')`),
  side: await evaluate(`!!document.querySelector('.side-card')`)
}))

await check('分类标签', '/admin/taxonomy', 'admin-taxonomy', async () => ({
  tables: await evaluate(`document.querySelectorAll('.ant-table').length`),
  rows: await evaluate(`document.querySelectorAll('.ant-table-tbody tr').length`)
}))

await check('评论管理', '/admin/comments', 'admin-comments', async () => ({
  items: await evaluate(`document.querySelectorAll('.comment-card').length`)
}))

await check('账号设置', '/admin/profile', 'admin-profile', async () => ({
  panels: await evaluate(`document.querySelectorAll('.panel').length`)
}))

await check('站点设置', '/admin/settings', 'admin-settings', async () => ({
  fields: await evaluate(`document.querySelectorAll('.setting-form .ant-input').length`),
  actions: await evaluate(`document.querySelectorAll('.form-actions .ant-btn').length`)
}))

// 备案信息：后台写入 → 前台页脚可见（写入→清空两头都要验，否则无法区分“没读到”与“读到了空值”）
const fillFiling = async (icp, police, url) => {
  await navigate('/admin/settings', 1400)
  await evaluate(
    `(() => {
      const inputs = [...document.querySelectorAll('.setting-form .ant-input')]
      const set = (el, v) => {
        el.value = v
        el.dispatchEvent(new Event('input', { bubbles: true }))
      }
      set(inputs[0], ${JSON.stringify(icp)})
      set(inputs[1], ${JSON.stringify(police)})
      set(inputs[2], ${JSON.stringify(url)})
      return inputs.length
    })()`
  )
  await sleep(200)
  await evaluate(`[...document.querySelectorAll('.form-actions .ant-btn')][0].click()`)
  await sleep(1200)
}

await fillFiling('沪ICP备2021000000号-1', '沪公网安备31010402000000号', 'https://beian.mps.gov.cn/#/query/webSearch?code=1')
await check('备案信息已保存', '/admin/settings', 'admin-settings-filled', async () => ({
  saved: await evaluate(`document.querySelectorAll('.setting-form .ant-input')[0]?.value`)
}))

await check('前台页脚备案', '/', null, async () => ({
  links: await evaluate(`document.querySelectorAll('.filing-link').length`),
  texts: await evaluate(`[...document.querySelectorAll('.filing-link')].map((a) => a.textContent.trim())`),
  icpHref: await evaluate(`document.querySelector('.filing-link')?.getAttribute('href')`),
  // 备案必须与版权行同处一行：同一父容器，且纵向位置一致
  sameRowAsCopyright: await evaluate(
    `(() => {
      const link = document.querySelector('.filing-link')
      const copy = document.querySelector('.footer-left span')
      const links = document.querySelector('.footer-links')
      if (!link || !copy || !links) return null
      return {
        sameParent: link.parentElement === copy.parentElement,
        linkTop: Math.round(link.getBoundingClientRect().top),
        copyTop: Math.round(copy.getBoundingClientRect().top),
        linksTop: Math.round(links.getBoundingClientRect().top)
      }
    })()`
  )
}))
// 页脚在首屏外，必须滚动到底才能截图到备案行
await evaluate(`window.scrollTo(0, document.body.scrollHeight)`)
await sleep(800)
await shot('home-footer-filing')

await fillFiling('', '', '')
await check('备案清空后页脚隐藏', '/', 'home-no-filing', async () => ({
  filingLinks: await evaluate(`document.querySelectorAll('.filing-link').length`)
}))

await check('404', '/no-such-page', 'notfound', async () => ({
  code: await evaluate(`document.querySelector('.code')?.textContent?.trim()`)
}))

console.log(JSON.stringify({ report, consoleErrors, failedRequests }, null, 2))

await sendS('Page.close').catch(() => {})
chrome.kill('SIGKILL')
process.exit(0)
