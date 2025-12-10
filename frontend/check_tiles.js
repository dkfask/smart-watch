// check_tiles.js
// 简单脚本：检测几个常用瓦片 URL 的可达性（GET 请求），用于诊断地图纯白问题。
// 使用方式: 在 frontend 目录运行 `node check_tiles.js`

const https = require('https')
const http = require('http')

const tests = [
  { name: 'OpenStreetMap (a)', url: 'https://a.tile.openstreetmap.org/0/0/0.png' },
  { name: 'OpenStreetMap (b)', url: 'https://b.tile.openstreetmap.org/0/0/0.png' },
  { name: 'Stamen Toner (a)', url: 'https://stamen-tiles-a.a.ssl.fastly.net/toner/0/0/0.png' },
  { name: 'Stamen Watercolor (a)', url: 'https://stamen-tiles-a.a.ssl.fastly.net/watercolor/0/0/0.jpg' },
  { name: 'Carto Positron (a)', url: 'https://a.basemaps.cartocdn.com/light_all/0/0/0.png' },
  { name: 'Esri World Imagery', url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/0/0/0' }
]

function checkUrl(test) {
  return new Promise((resolve) => {
    try {
      const url = new URL(test.url)
      const lib = url.protocol === 'https:' ? https : http
      const opts = {
        method: 'GET',
        hostname: url.hostname,
        path: url.pathname + (url.search || ''),
        port: url.port || (url.protocol === 'https:' ? 443 : 80),
        headers: {
          'User-Agent': 'tile-checker/1.0',
          'Accept': '*/*'
        },
        timeout: 8000
      }
      const req = lib.request(opts, (res) => {
        const info = { name: test.name, url: test.url, statusCode: res.statusCode, headers: res.headers }
        // consume a small chunk then abort to be polite
        res.once('data', () => {
          req.abort()
          resolve({ ok: true, info })
        })
        // if no data (some endpoints respond with empty body), end event
        res.on('end', () => resolve({ ok: true, info }))
      })
      req.on('timeout', () => {
        req.destroy(new Error('timeout'))
      })
      req.on('error', (err) => {
        resolve({ ok: false, error: err.message, url: test.url, name: test.name })
      })
      req.end()
    } catch (e) {
      resolve({ ok: false, error: e.message, url: test.url, name: test.name })
    }
  })
}

;(async () => {
  console.log('Checking tile providers...')
  for (const t of tests) {
    process.stdout.write(`- ${t.name}: `)
    const r = await checkUrl(t)
    if (r.ok) {
      console.log(`OK (HTTP ${r.info.statusCode}) content-type=${r.info.headers['content-type'] || 'unknown'}`)
    } else {
      console.log(`FAIL (${r.error})`)
    }
  }
  console.log('Done.')
})()

