// 定向诊断：首页 Hero 标题渲染 + 字符渐显动画状态
import { spawn } from 'node:child_process'
import { setTimeout as sleep } from 'node:timers/promises'

const CHROME = 'D:\\Program Files\\chrome-for-testing\\chrome.exe'
const chrome = spawn(
  CHROME,
  [
    '--headless=new',
    '--disable-gpu',
    '--hide-scrollbars',
    '--remote-debugging-port=9340',
    '--user-data-dir=D:\\Projects\\Java\\blog\\.chrome-p2',
    '--window-size=1440,1000',
    'about:blank'
  ],
  { stdio: 'ignore' }
)

for (let i = 0; i < 60; i++) {
  try {
    const r = await fetch('http://127.0.0.1:9340/json/version')
    if (r.ok) break
  } catch {
    /* wait */
  }
  await sleep(250)
}

const version = await (await fetch('http://127.0.0.1:9340/json/version')).json()
const ws = new WebSocket(version.webSocketDebuggerUrl)
await new Promise((r) => (ws.onopen = r))

let id = 0
const pending = new Map()
ws.onmessage = (m) => {
  const d = JSON.parse(m.data)
  if (d.id && pending.has(d.id)) {
    pending.get(d.id)(d.result)
    pending.delete(d.id)
  }
}
const send = (method, params = {}, sessionId) =>
  new Promise((res) => {
    const i = ++id
    pending.set(i, res)
    ws.send(JSON.stringify({ id: i, method, params, sessionId }))
  })

const { targetInfos } = await send('Target.getTargets')
const { sessionId } = await send('Target.attachToTarget', {
  targetId: targetInfos.find((t) => t.type === 'page').targetId,
  flatten: true
})
const S = (m, p) => send(m, p, sessionId)

await S('Page.enable')
await S('Runtime.enable')
await S('Page.navigate', { url: 'http://127.0.0.1:5173/' })
await sleep(2600)

const ev = async (expression) => {
  const r = await S('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  if (r?.exceptionDetails) return `EXCEPTION: ${r.exceptionDetails.text}`
  return r?.result?.value
}

const out = {
  h1Text: await ev("document.querySelector('.hero-title').textContent"),
  h1Rect: await ev("JSON.stringify(document.querySelector('.hero-title').getBoundingClientRect())"),
  spanCount: await ev("document.querySelectorAll('.char-reveal > span').length"),
  span0: await ev(`JSON.stringify((() => {
    const s = document.querySelector('.char-reveal > span');
    const cs = getComputedStyle(s);
    return { opacity: cs.opacity, animName: cs.animationName, animDelay: cs.animationDelay, animDur: cs.animationDuration, fill: cs.webkitTextFillColor, display: cs.display };
  })())`),
  h1Style: await ev(`JSON.stringify((() => {
    const cs = getComputedStyle(document.querySelector('.hero-title'));
    return { fill: cs.webkitTextFillColor, bgImage: cs.backgroundImage.slice(0, 60), fontSize: cs.fontSize, color: cs.color };
  })())`),
  heroCardRect: await ev("JSON.stringify(document.querySelector('.hero-inner').getBoundingClientRect())"),
  reducedMotion: await ev("window.matchMedia('(prefers-reduced-motion: reduce)').matches")
}

console.log(JSON.stringify(out, null, 2))

chrome.kill('SIGKILL')
process.exit(0)
