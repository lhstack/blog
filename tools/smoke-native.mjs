// Verify the actual native executable with an isolated data directory.
import assert from 'node:assert/strict'
import { execFileSync, spawn } from 'node:child_process'
import { once } from 'node:events'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import { setTimeout as delay } from 'node:timers/promises'

const [executable, image] = process.argv.slice(2)
const docker = executable === '--docker'
if (!executable || (docker && !image)) {
  throw new Error('Usage: node tools/smoke-native.mjs <executable> | --docker <image>')
}

const password = 'NativeSmoke-NotForProduction2026'
const env = { BLOG_ADMIN_USERNAME: 'smokeadmin', BLOG_ADMIN_PASSWORD: password }
const logging = '--logging.level.org.springframework.boot.web.embedded.tomcat.TomcatWebServer=INFO'
const dockerCmd = (...args) => execFileSync('docker', args, { encoding: 'utf8' }).trim()
const work = docker ? null : await mkdtemp(join(tmpdir(), 'blog-native-smoke-'))
const container = docker ? dockerCmd('run', '-d', '-p', '127.0.0.1::8080',
  '-e', `BLOG_ADMIN_USERNAME=${env.BLOG_ADMIN_USERNAME}`,
  '-e', `BLOG_ADMIN_PASSWORD=${password}`, image, logging) : null
const child = docker ? spawn('docker', ['logs', '--follow', container], { stdio: ['ignore', 'pipe', 'pipe'] }) :
  spawn(resolve(executable), [
    '--spring.profiles.active=prod', '--server.address=127.0.0.1', '--server.port=0', logging
  ], {
    cwd: work,
    env: { ...process.env, ...env, BLOG_DATA_DIR: join(work, 'data'), BLOG_LOG_DIR: join(work, 'logs') },
    stdio: ['ignore', 'pipe', 'pipe']
  })
let output = ''
for (const stream of [child.stdout, child.stderr]) {
  stream.setEncoding('utf8')
  stream.on('data', chunk => { output = (output + chunk).slice(-50000) })
}
const closed = new Promise(resolve => child.once('close', resolve))
try {
  const mappedPort = container ? dockerCmd('port', container, '8080/tcp').match(/^127\.0\.0\.1:(\d+)$/)?.[1] : null
  if (container) assert(mappedPort, 'Expected a Docker port mapped to loopback')
  const port = await Promise.race([
    (async () => {
      for (let i = 0; i < 300; i++) {
        const match = output.match(/Tomcat started on port (\d+)/)
        if (match && /DataInitializer[^\r\n]*smokeadmin/.test(output)) return mappedPort ?? match[1]
        await delay(100, undefined, { ref: false })
      }
      throw new Error('Timed out waiting for Tomcat')
    })(),
    once(child, 'exit').then(([code, signal]) => { throw new Error(`Blog exited: ${code ?? signal}`) })
  ])
  const request = (path, options) => fetch(`http://127.0.0.1:${port}${path}`, {
    ...options, signal: AbortSignal.timeout(5000)
  })
  const ok = async (path, options) => {
    const response = await request(path, options)
    assert.equal(response.status, 200, `${path}: ${response.status === 200 ? '' : await response.text()}`)
    return response
  }

  const index = await (await ok('/')).text()
  assert.equal(await (await ok('/archives')).text(), index)
  const script = index.match(/src="(\/assets\/[^"\s]+\.js)"/)
  assert(script, 'Expected a compiled frontend script in index.html')
  await (await ok(script[1])).arrayBuffer()
  assert.equal((await request('/doc.html')).status, 404)
  assert.match(await (await ok('/rss.xml')).text(), /<rss version="2\.0"/)
  assert.equal((await (await ok('/api/public/site')).json()).articleCount, 0)
  assert.deepEqual((await (await ok('/api/public/articles')).json()).items, [])

  const stream = await ok('/api/public/site/stats/stream', { headers: { Accept: 'text/event-stream' } })
  assert.match(stream.headers.get('content-type'), /^text\/event-stream/)
  const reader = stream.body.getReader()
  let event = ''
  try {
    while (!event.includes('\n\n')) {
      const { value, done } = await reader.read()
      assert(!done, 'SSE ended before the first event')
      event += Buffer.from(value).toString('utf8')
    }
    assert.equal(JSON.parse(event.match(/^data:\s*(.+)$/m)[1]).articles, 0)
  } finally {
    await reader.cancel()
  }

  const profilePath = '/api/admin/profile'
  assert.equal((await request(profilePath)).status, 401)
  const login = await (await ok('/api/auth/login', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'smokeadmin', password })
  })).json()
  assert.equal(login.tokenName, 'blog-token')
  assert(login.token, 'Expected a Sa-Token')
  const admin = { 'blog-token': login.token }
  const profile = await (await ok(profilePath, { headers: admin })).json()
  assert.equal(profile.username, 'smokeadmin')

  const draft = await (await ok('/api/admin/articles', {
    method: 'POST', headers: { ...admin, 'Content-Type': 'application/json' },
    body: JSON.stringify({ title: 'Native smoke', summary: 'Smoke test', content: 'Test content' })
  })).json()
  assert.equal(draft.status, 'DRAFT')
  const published = await (await ok(`/api/admin/articles/${draft.id}/publish`, { method: 'POST', headers: admin })).json()
  assert.equal(published.status, 'PUBLISHED')
  const articles = await (await ok('/api/public/articles')).json()
  assert.equal(articles.items[0].title, draft.title)
  assert.match(await (await ok('/rss.xml')).text(), /<title>Native smoke<\/title>/)

  for (const path of ['/api/admin/profile/avatar', '/api/admin/settings/site/avatar']) {
    const body = new FormData()
    body.set('file', new Blob(['placeholder'], { type: 'image/png' }), 'avatar.png')
    const response = await request(path, { method: 'POST', headers: admin, body })
    assert.equal(response.status, 501)
    assert.match((await response.json()).message, /图片 URL/)
  }
  console.log('Native smoke passed: frontend, SQLite, article publishing, RSS, SSE, authentication, and avatar fallback')
} catch (error) {
  console.error(output)
  throw error
} finally {
  if (child.exitCode === null && child.signalCode === null) child.kill()
  await Promise.race([closed, delay(5000, undefined, { ref: false })])
  if (child.exitCode === null && child.signalCode === null) child.kill('SIGKILL')
  await closed
  if (work) await rm(work, { recursive: true, force: true, maxRetries: 3 })
  if (container) dockerCmd('rm', '-f', container)
}
