// AI 助手 API：流式回答用 fetch 读取 SSE（axios 不支持浏览器端流式读取），
// 且 axios 实例有 10s 超时，不适合长回答。
const API_BASE = import.meta.env.VITE_API_BASE || '/api'

function handleUnauthorized(response) {
  if (response.status === 401) {
    localStorage.removeItem('user')
    window.location.href = '/login'
    return true
  }
  return false
}

/**
 * 查询 AI 助手配置状态：{ enabled, configured, model }（ApiResponse 信封）
 */
export async function getAiStatus() {
  const response = await fetch(`${API_BASE}/ai/status`, { credentials: 'include' })
  if (handleUnauthorized(response)) {
    throw new Error('未登录')
  }
  const body = await response.json()
  return body.data ?? body
}

/**
 * 非流式回答（降级通道），返回 { content }
 */
export async function chat(messages) {
  const response = await fetch(`${API_BASE}/ai/chat`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ messages })
  })
  if (handleUnauthorized(response)) {
    throw new Error('未登录')
  }
  const body = await response.json()
  if (!response.ok) {
    throw new Error(body.message || `AI 服务错误 (${response.status})`)
  }
  return body.data ?? body
}

/**
 * 流式回答。
 * @param {Object} options
 * @param {Array<{role:string,content:string}>} options.messages 会话历史（含最新一条用户消息）
 * @param {(text:string)=>void} options.onDelta 收到增量文本
 * @param {(label:string)=>void} options.onTool 后端正在查询某类数据
 * @param {(content:string)=>void} options.onDone 回答完成（完整内容）
 * @param {AbortSignal} [signal] 中断信号
 */
export async function chatStream({ messages, onDelta, onTool, onDone }, signal) {
  const response = await fetch(`${API_BASE}/ai/chat/stream`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ messages }),
    signal
  })
  if (handleUnauthorized(response)) {
    throw new Error('未登录')
  }
  if (!response.ok || !response.body) {
    let message = `AI 服务错误 (${response.status})`
    try {
      const body = await response.json()
      if (body && body.message) message = body.message
    } catch (e) {
      // 非 JSON 错误体，保留默认提示
    }
    throw new Error(message)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  // SSE 分帧：event: <名> + data: <JSON>，空行结束一个事件
  const dispatch = (eventName, dataText) => {
    let payload = null
    try {
      payload = JSON.parse(dataText)
    } catch (e) {
      payload = { text: dataText }
    }
    if (eventName === 'delta' && payload.text) {
      onDelta && onDelta(payload.text)
    } else if (eventName === 'tool' && payload.label) {
      onTool && onTool(payload.label)
    } else if (eventName === 'done') {
      onDone && onDone(payload.content ?? '')
    } else if (eventName === 'error') {
      throw new Error(payload.message || 'AI 服务暂时不可用')
    }
  }

  let currentEvent = ''
  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    let separator
    while ((separator = buffer.indexOf('\n')) >= 0) {
      const line = buffer.slice(0, separator).replace(/\r$/, '')
      buffer = buffer.slice(separator + 1)
      if (line.startsWith('event:')) {
        currentEvent = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dispatch(currentEvent, line.slice(5).trim())
        currentEvent = ''
      }
    }
  }
}
