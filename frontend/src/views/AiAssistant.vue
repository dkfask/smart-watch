<template>
  <div class="ai-container">
    <el-card shadow="hover" class="ai-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <h2>AI 助手</h2>
            <span class="header-subtitle">实时查询平台数据，用自然语言了解设备、患者与告警情况</span>
          </div>
          <div class="header-actions">
            <el-tag v-if="statusLoaded && status.configured" type="primary" effect="light" size="small">
              {{ status.model }}
            </el-tag>
            <el-tag v-else-if="statusLoaded" type="info" effect="light" size="small">未配置</el-tag>
          </div>
        </div>
      </template>

      <el-alert
        v-if="statusLoaded && !status.configured"
        type="warning"
        :closable="false"
        show-icon
        title="AI 助手尚未启用"
        description="请在服务端配置环境变量 AI_API_KEY（默认对接 DeepSeek，可用 AI_BASE_URL / AI_MODEL 切换供应商），重启后即可使用。"
        class="config-alert"
      />

      <div ref="chatWindowEl" class="chat-window">
        <div v-if="messages.length === 0" class="chat-empty">
          <div class="empty-icon">🤖</div>
          <h3>你好，我是监护平台 AI 助手</h3>
          <p>可以问我设备在线状态、患者健康数据、告警情况等问题</p>
          <div class="suggest-list">
            <button
              v-for="q in suggestions"
              :key="q"
              class="suggest-item"
              type="button"
              @click="sendSuggestion(q)"
            >{{ q }}</button>
          </div>
        </div>

        <div
          v-for="(m, index) in messages"
          :key="index"
          class="chat-row"
          :class="m.role === 'user' ? 'row-user' : 'row-assistant'"
        >
          <div class="avatar" :class="m.role === 'user' ? 'avatar-user' : 'avatar-ai'">
            {{ m.role === 'user' ? '我' : 'AI' }}
          </div>
          <div class="bubble" :class="{ 'bubble-error': m.error }">
            <div v-if="m.toolLabel" class="tool-hint">
              <span class="tool-spinner"></span>正在查询{{ m.toolLabel }}…
            </div>
            <div class="bubble-text">{{ m.content }}<span v-if="m.streaming" class="cursor"></span></div>
            <div v-if="m.time" class="bubble-time">{{ m.time }}</div>
          </div>
        </div>
      </div>

      <div class="chat-input-area">
        <el-input
          v-model="input"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="输入问题，Enter 发送，Shift+Enter 换行"
          resize="none"
          :disabled="!statusLoaded || status.configured"
          @keydown="onKeydown"
        />
        <el-button v-if="!busy" type="primary" :disabled="!input.trim()" @click="send">发送</el-button>
        <el-button v-else type="warning" plain @click="stop">停止</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { chatStream, getAiStatus } from '../api/ai'

const suggestions = [
  '现在有多少设备在线？',
  '今天有哪些待处理告警？',
  '列出所有在院患者',
  '哪些设备电量低于 20%？'
]

const status = ref({})
const statusLoaded = ref(false)
const messages = ref([])
const input = ref('')
const busy = ref(false)
const chatWindowEl = ref(null)
let abortController = null

function formatTime() {
  return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function scrollToBottom() {
  nextTick(() => {
    if (chatWindowEl.value) {
      chatWindowEl.value.scrollTop = chatWindowEl.value.scrollHeight
    }
  })
}

function onKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    send()
  }
}

function sendSuggestion(question) {
  if (busy.value) return
  input.value = question
  send()
}

function buildHistory() {
  return messages.value
    .filter(m => (m.role === 'user' || m.role === 'assistant') && m.content && !m.error)
    .slice(-16)
    .map(m => ({ role: m.role, content: m.content }))
}

async function send() {
  const question = input.value.trim()
  if (!question || busy.value) return

  input.value = ''
  busy.value = true
  messages.value.push({ role: 'user', content: question, time: formatTime() })
  const placeholder = { role: 'assistant', content: '', streaming: true, toolLabel: '', time: '' }
  messages.value.push(placeholder)
  scrollToBottom()

  abortController = new AbortController()
  try {
    await chatStream({
      messages: buildHistory().slice(0, -1),
      onDelta: text => {
        placeholder.content += text
        placeholder.toolLabel = ''
        scrollToBottom()
      },
      onTool: label => {
        placeholder.toolLabel = label
        scrollToBottom()
      },
      onDone: content => {
        placeholder.content = content
        placeholder.streaming = false
        placeholder.toolLabel = ''
        placeholder.time = formatTime()
        scrollToBottom()
      }
    }, abortController.signal)
  } catch (error) {
    if (error.name === 'AbortError') {
      placeholder.content = placeholder.content || '（已停止回答）'
    } else {
      placeholder.content = `抱歉，出了点问题：${error.message}`
      placeholder.error = true
      ElMessage.error(error.message)
    }
    placeholder.streaming = false
    placeholder.toolLabel = ''
    placeholder.time = formatTime()
  } finally {
    busy.value = false
    abortController = null
    scrollToBottom()
  }
}

function stop() {
  if (abortController) {
    abortController.abort()
  }
}

onMounted(async () => {
  try {
    status.value = await getAiStatus()
    statusLoaded.value = true
  } catch (error) {
    // 状态探测失败不阻塞页面，仅隐藏未配置提示
    statusLoaded.value = false
  }
})
</script>

<style scoped>
.ai-container {
  padding: 20px 24px;
  height: 100%;
}

.ai-card {
  height: calc(100vh - 110px);
  display: flex;
  flex-direction: column;
}

.ai-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title h2 {
  margin: 0;
  font-size: 18px;
  color: var(--color-text-primary);
}

.header-subtitle {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.config-alert {
  margin-bottom: 12px;
}

.chat-window {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: 10px;
}

.chat-empty {
  text-align: center;
  padding: 60px 20px 30px;
  color: var(--color-text-secondary);
}

.empty-icon {
  font-size: 42px;
  margin-bottom: 8px;
}

.chat-empty h3 {
  margin: 0 0 6px;
  color: var(--color-text-primary);
}

.chat-empty p {
  margin: 0 0 24px;
  font-size: 13px;
}

.suggest-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

.suggest-item {
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  color: var(--color-text-primary);
  border-radius: 16px;
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.suggest-item:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-subtle);
}

.chat-row {
  display: flex;
  margin: 14px 12px;
  gap: 10px;
}

.row-user {
  flex-direction: row-reverse;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

.avatar-user {
  background: var(--color-primary);
  color: var(--color-text-inverse);
}

.avatar-ai {
  background: var(--color-primary-subtle);
  color: var(--color-primary-dark);
  font-weight: 600;
}

.bubble {
  max-width: 72%;
  border-radius: 12px;
  padding: 10px 14px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}

.row-user .bubble {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.row-user .bubble-text {
  color: var(--color-text-inverse);
}

.bubble-error {
  border-color: var(--color-danger);
}

.bubble-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-primary);
}

.cursor {
  display: inline-block;
  width: 7px;
  height: 15px;
  margin-left: 2px;
  vertical-align: text-bottom;
  background: var(--color-primary);
  animation: blink 0.9s infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

.tool-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.tool-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid var(--color-border-strong);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.bubble-time {
  margin-top: 4px;
  font-size: 11px;
  color: var(--color-text-muted);
  text-align: right;
}

.chat-input-area {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding-top: 12px;
}

.chat-input-area .el-textarea {
  flex: 1;
}

@media (max-width: 768px) {
  .bubble {
    max-width: 85%;
  }
}
</style>
