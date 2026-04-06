<template>
  <div class="diagnosis-container">
    <el-config-provider :locale="zhCn || {}">

      <div class="context-panel">
        <div class="panel-header">
          <el-icon class="pulse-icon">
            <Odometer />
          </el-icon>
          <span>智能运维中枢状态</span>
        </div>

        <div class="status-box">
          <div class="status-item">
            <span class="label">大模型引擎</span>
            <span class="value success-text">Qwen-Max (已连线)</span>
          </div>
          <div class="status-item">
            <span class="label">MCP 检索协议</span>
            <span class="value warning-text">待挂载</span>
          </div>
          <div class="status-item">
            <span class="label">RagFlow 知识库</span>
            <span class="value warning-text">待同步</span>
          </div>
        </div>

        <div class="action-box">
          <div class="action-desc">快捷指令注入：</div>
          <el-button class="cyber-btn quick-btn" @click="insertPrompt('帮我分析一下 Panther 办公楼最近的能耗趋势是否正常？')">
            <el-icon>
              <DataLine />
            </el-icon> 诊断全局能耗
          </el-button>
          <el-button class="cyber-btn quick-btn" @click="insertPrompt('针对夜间违规耗电(night_abnormal)，运维规范里有什么排查建议？')">
            <el-icon>
              <Document />
            </el-icon> 检索运维手册
          </el-button>
        </div>
      </div>

      <div class="chat-panel">
        <div class="chat-header">
          <span>终端会话日志 // BEMS_COPILOT_V2</span>
          <div class="typing-indicator" v-if="isTyping">AI 计算中...</div>
        </div>

        <div class="chat-history" ref="chatHistoryRef">
          <div v-for="(msg, index) in chatList" :key="index" class="message-wrapper"
            :class="msg.role === 'user' ? 'is-user' : 'is-ai'">
            <div class="avatar">{{ msg.role === 'user' ? 'You' : 'AI' }}</div>
            <div class="message-bubble">
              <div class="markdown-body" v-html="parseMarkdown(msg.content)"></div>

              <span v-if="isTyping && index === chatList.length - 1 && msg.role === 'ai'" class="cursor">_</span>
            </div>
          </div>
        </div>

        <div class="input-area">
          <el-input v-model="inputMessage" type="textarea" :rows="3" placeholder="输入系统诊断指令，按 Enter 发送..."
            class="cyber-textarea" @keydown.enter.prevent="sendMessage" :disabled="isTyping" />
          <el-button type="primary" class="cyber-btn send-btn pulse-btn" @click="sendMessage" :loading="isTyping">
            <el-icon>
              <Position />
            </el-icon> 发 送
          </el-button>
        </div>
      </div>

    </el-config-provider>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { Odometer, DataLine, Document, Position } from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { ElMessage } from 'element-plus'

// 🚀 新增：引入 Markdown 渲染引擎
import { marked } from 'marked'

// 状态管理
const inputMessage = ref('')
const isTyping = ref(false)
const chatHistoryRef = ref(null)

// 初始欢迎语
const chatList = ref([
  {
    role: 'ai',
    content: '指挥官您好，我是 BEMS 智能运维大脑。\n大模型中枢已激活，正在等待您的数据探查与诊断指令。'
  }
])

// 滚动到底部动画
const scrollToBottom = async () => {
  await nextTick()
  if (chatHistoryRef.value) {
    chatHistoryRef.value.scrollTop = chatHistoryRef.value.scrollHeight
  }
}

// 快捷插入提示词
const insertPrompt = (text) => {
  if (!isTyping.value) {
    inputMessage.value = text
  }
}

// 🚀 核心：发送消息并接收流式回复
const sendMessage = () => {
  const text = inputMessage.value.trim()
  if (!text || isTyping.value) return

  // 1. 用户消息上屏
  chatList.value.push({ role: 'user', content: text })
  inputMessage.value = ''
  isTyping.value = true
  scrollToBottom()

  // 2. 预先塞入一个空的 AI 消息框，准备接水
  chatList.value.push({ role: 'ai', content: '' })

  // 3. 发起 SSE 连接 (对接我们刚刚写好的 Spring Boot 接口)
  const url = `http://localhost:8080/api/ai/chat/stream?message=${encodeURIComponent(text)}`
  const eventSource = new EventSource(url)

  // 接水的过程：每收到一个字，就追加到最后一个气泡里
  eventSource.onmessage = (event) => {
    const lastMsgIndex = chatList.value.length - 1
    // 处理后端传来的换行符 (SSE 传输时有时会把 \n 转义)
    const newText = event.data.replace(/\\n/g, '\n')
    chatList.value[lastMsgIndex].content += newText
    scrollToBottom()
  }

  // 结束的信号：大模型输出完毕后，连接会自动断开触发 error
  eventSource.onerror = () => {
    isTyping.value = false
    eventSource.close() // 极其重要：掐断连接，防止浏览器无限重连请求
  }
}

// 🚀 新增：Markdown 解析函数
const parseMarkdown = (text) => {
  if (!text) return ''
  return marked.parse(text)
}
</script>

<style scoped>
.diagnosis-container {
  height: 100%;
  display: flex;
  gap: 20px;
  padding: 10px;
}

/* 左侧战术面板 */
.context-panel {
  width: 320px;
  background: rgba(11, 9, 26, 0.8);
  border: 1px solid #2A2946;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 0 15px rgba(0, 240, 255, 0.05);
}

.panel-header {
  padding: 15px 20px;
  border-bottom: 1px solid #2A2946;
  color: #00F0FF;
  font-weight: bold;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  letter-spacing: 1px;
}

.pulse-icon {
  animation: pulse-glow 2s infinite;
}

.status-box {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.status-item .label {
  color: #a0a2b8;
}

.status-item .value {
  font-weight: bold;
  font-family: monospace;
}

.success-text {
  color: #00FF9D;
}

.warning-text {
  color: #FAAD14;
}

.action-box {
  padding: 0 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-desc {
  color: #4a4a5e;
  font-size: 12px;
  margin-bottom: 5px;
}

.quick-btn {
  justify-content: flex-start;
  color: #a0a2b8;
  border-color: #2A2946;
}

.quick-btn:hover {
  color: #00F0FF;
  border-color: #00F0FF;
  background-color: rgba(0, 240, 255, 0.1);
}

/* 右侧对话面板 */
.chat-panel {
  flex: 1;
  background: rgba(30, 27, 56, 0.4);
  border: 1px solid #2A2946;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  padding: 15px 20px;
  background-color: #0b091a;
  border-bottom: 1px solid #2A2946;
  color: #a0a2b8;
  font-family: monospace;
  font-size: 14px;
  display: flex;
  justify-content: space-between;
}

.typing-indicator {
  color: #00F0FF;
  animation: blink 1s infinite;
}

/* 对话流 */
.chat-history {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 隐藏滚动条 */
.chat-history::-webkit-scrollbar {
  width: 6px;
}

.chat-history::-webkit-scrollbar-thumb {
  background: #2A2946;
  border-radius: 3px;
}

.message-wrapper {
  display: flex;
  gap: 15px;
  max-width: 85%;
}

.is-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.is-ai {
  align-self: flex-start;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 12px;
  border: 1px solid;
  flex-shrink: 0;
}

.is-user .avatar {
  background: rgba(0, 255, 157, 0.1);
  color: #00FF9D;
  border-color: #00FF9D;
  box-shadow: 0 0 10px rgba(0, 255, 157, 0.2);
}

.is-ai .avatar {
  background: rgba(0, 240, 255, 0.1);
  color: #00F0FF;
  border-color: #00F0FF;
  box-shadow: 0 0 10px rgba(0, 240, 255, 0.2);
}

.message-bubble {
  padding: 12px 18px;
  border-radius: 8px;
  line-height: 1.6;
  font-size: 14px;
  position: relative;
}

.is-user .message-bubble {
  background: #00FF9D;
  color: #000;
  border-top-right-radius: 0;
}

.is-ai .message-bubble {
  background: rgba(11, 9, 26, 0.8);
  border: 1px solid #2A2946;
  color: #fff;
  border-top-left-radius: 0;
}

.cursor {
  display: inline-block;
  width: 8px;
  height: 15px;
  background-color: #00F0FF;
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 1s step-end infinite;
}

/* 输入区 */
.input-area {
  padding: 15px;
  background-color: #0b091a;
  border-top: 1px solid #2A2946;
  display: flex;
  gap: 15px;
  align-items: flex-end;
}

:deep(.cyber-textarea .el-textarea__inner) {
  background-color: #05050f !important;
  box-shadow: 0 0 0 1px #2A2946 inset !important;
  color: #00F0FF !important;
  font-family: monospace;
  border-radius: 4px;
}

:deep(.cyber-textarea .el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #00F0FF inset !important;
}

.cyber-btn {
  background-color: #0b091a;
  border: 1px solid;
  font-weight: bold;
  letter-spacing: 1px;
}

.send-btn {
  height: 75px;
  width: 100px;
  color: #00F0FF;
  border-color: #00F0FF;
}

.send-btn:hover {
  background-color: rgba(0, 240, 255, 0.1);
  box-shadow: 0 0 15px rgba(0, 240, 255, 0.4);
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

@keyframes pulse-glow {
  0% {
    text-shadow: 0 0 5px #00F0FF;
  }

  50% {
    text-shadow: 0 0 15px #00F0FF, 0 0 20px #00F0FF;
  }

  100% {
    text-shadow: 0 0 5px #00F0FF;
  }
}

/* =========================================
   🚀 专属 Markdown 赛博朋克渲染样式
========================================= */
:deep(.markdown-body) {
  color: #E0E2F5;
  font-family: "PingFang SC", "Microsoft YaHei", monospace;
  line-height: 1.6;
}

:deep(.markdown-body p) {
  margin-bottom: 12px;
}

:deep(.markdown-body strong) {
  color: #00FF9D;
  /* 加粗文字变成赛博绿 */
  text-shadow: 0 0 5px rgba(0, 255, 157, 0.4);
  font-weight: bold;
}

:deep(.markdown-body ul),
:deep(.markdown-body ol) {
  margin-left: 20px;
  margin-bottom: 12px;
}

:deep(.markdown-body li) {
  margin-bottom: 6px;
}

:deep(.markdown-body li::marker) {
  color: #00F0FF;
  /* 列表圆点变成霓虹蓝 */
}

/* 行内小代码块 */
:deep(.markdown-body code:not(pre code)) {
  background: rgba(0, 240, 255, 0.15);
  color: #00F0FF;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid rgba(0, 240, 255, 0.3);
  font-family: monospace;
}

/* 独立的大代码块 */
:deep(.markdown-body pre) {
  background: #05050f;
  border: 1px solid #2A2946;
  border-left: 4px solid #00F0FF;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin-bottom: 12px;
}

:deep(.markdown-body pre code) {
  color: #00F0FF;
  font-family: monospace;
}
</style>