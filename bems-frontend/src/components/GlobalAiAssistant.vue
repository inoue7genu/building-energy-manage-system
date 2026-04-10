<template>
    <el-drawer v-model="visible" title="BEMS_COPILOT_V2 // 智能运维大脑" direction="rtl" size="450px" class="cyber-drawer"
        :with-header="false" append-to-body>
        <div class="diagnosis-container-drawer">
            <div class="panel-header">
                <el-icon class="pulse-icon">
                    <Odometer />
                </el-icon>
                <span>BEMS 全局智能中枢</span>
                <el-button link class="close-btn" @click="visible = false">
                    <el-icon :size="20">
                        <Close />
                    </el-icon>
                </el-button>
            </div>

            <div class="chat-panel">
                <div class="chat-history" ref="chatHistoryRef">
                    <div v-for="(msg, index) in chatList" :key="index" class="message-wrapper"
                        :class="msg.role === 'user' ? 'is-user' : 'is-ai'">
                        <div class="avatar">{{ msg.role === 'user' ? 'You' : 'AI' }}</div>
                        <div class="message-bubble">
                            <div class="markdown-body" v-html="parseMarkdown(msg.content)"></div>
                            <span v-if="isTyping && index === chatList.length - 1 && msg.role === 'ai'"
                                class="cursor">_</span>
                        </div>
                    </div>
                </div>

                <div class="input-area">
                    <el-input v-model="inputMessage" type="textarea" :rows="3" placeholder="输入系统诊断指令，按 Enter 发送..."
                        class="cyber-textarea" @keydown.enter.prevent="sendMessage" :disabled="isTyping" />
                    <el-button type="primary" class="cyber-btn send-btn pulse-btn" @click="sendMessage"
                        :loading="isTyping">
                        <el-icon>
                            <Position />
                        </el-icon> 发 送
                    </el-button>
                </div>
            </div>
        </div>
    </el-drawer>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { Odometer, Position, Close } from '@element-plus/icons-vue'
import { marked } from 'marked'

const visible = ref(false)
const inputMessage = ref('')
const isTyping = ref(false)
const chatHistoryRef = ref(null)

const chatList = ref([
    {
        role: 'ai',
        content: '指挥官您好，我是 BEMS 全局智能大脑。已悬浮待命，随时为您进行数据探查与诊断。'
    }
])

// 💡 暴露给全局调用的唤醒方法，支持带参数自动质询
const openAssistant = (autoPrompt = '') => {
    visible.value = true
    if (autoPrompt) {
        setTimeout(() => {
            if (!isTyping.value) {
                inputMessage.value = autoPrompt
                sendMessage()
            }
        }, 300)
    }
}
defineExpose({ openAssistant })

const scrollToBottom = async () => {
    await nextTick()
    if (chatHistoryRef.value) {
        chatHistoryRef.value.scrollTop = chatHistoryRef.value.scrollHeight
    }
}

const sendMessage = () => {
    const text = inputMessage.value.trim()
    if (!text || isTyping.value) return

    chatList.value.push({ role: 'user', content: text })
    inputMessage.value = ''
    isTyping.value = true
    scrollToBottom()

    chatList.value.push({ role: 'ai', content: '' })

    const url = `http://localhost:8080/api/ai/chat/stream?message=${encodeURIComponent(text)}`
    const eventSource = new EventSource(url)

    eventSource.onmessage = (event) => {
        const lastMsgIndex = chatList.value.length - 1
        const newText = event.data.replace(/\\n/g, '\n')
        chatList.value[lastMsgIndex].content += newText
        scrollToBottom()
    }

    eventSource.onerror = () => {
        isTyping.value = false
        eventSource.close()
    }
}

const parseMarkdown = (text) => {
    if (!text) return ''
    return marked.parse(text)
}
</script>

<style scoped>
/* 融合你原本的样式，适应抽屉布局 */
.diagnosis-container-drawer {
    height: 100%;
    display: flex;
    flex-direction: column;
    background-color: #05050f;
}

.panel-header {
    padding: 15px 20px;
    background-color: #0b091a;
    border-bottom: 1px solid #2A2946;
    color: #00F0FF;
    font-weight: bold;
    font-size: 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.close-btn {
    color: #a0a2b8;
}

.close-btn:hover {
    color: #FF4D4F;
}

.chat-panel {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

/* ... (此处保留你 SmartDiagnosis.vue 中的 .chat-history, .message-wrapper, .avatar, .is-user, .is-ai, .input-area, .cyber-btn 等所有极客样式) ... */
.chat-history {
    flex: 1;
    padding: 20px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 20px;
}

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
    max-width: 90%;
}

.is-user {
    align-self: flex-end;
    flex-direction: row-reverse;
}

.is-ai {
    align-self: flex-start;
}

.avatar {
    width: 36px;
    height: 36px;
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
    padding: 12px 16px;
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
    animation: blink 1s step-end infinite;
}

.input-area {
    padding: 15px;
    background-color: #0b091a;
    border-top: 1px solid #2A2946;
    display: flex;
    gap: 15px;
}

:deep(.cyber-textarea .el-textarea__inner) {
    background-color: #05050f !important;
    box-shadow: 0 0 0 1px #2A2946 inset !important;
    color: #00F0FF !important;
    border-radius: 4px;
    font-family: monospace;
}

:deep(.cyber-textarea .el-textarea__inner:focus) {
    box-shadow: 0 0 0 1px #00F0FF inset !important;
}

.cyber-btn {
    background-color: #0b091a;
    border: 1px solid;
    font-weight: bold;
}

.send-btn {
    height: auto;
    color: #00F0FF;
    border-color: #00F0FF;
}

.send-btn:hover {
    background-color: rgba(0, 240, 255, 0.1);
    box-shadow: 0 0 15px rgba(0, 240, 255, 0.4);
}

:deep(.markdown-body) {
    color: #E0E2F5;
    line-height: 1.6;
    font-size: 13px;
}

:deep(.markdown-body p) {
    margin-bottom: 8px;
}

:deep(.markdown-body strong) {
    color: #00FF9D;
    text-shadow: 0 0 5px rgba(0, 255, 157, 0.4);
    font-weight: bold;
}

:deep(.markdown-body ul),
:deep(.markdown-body ol) {
    margin-left: 15px;
    margin-bottom: 8px;
}

:deep(.markdown-body li::marker) {
    color: #00F0FF;
}

/* 深度定制 Element 抽屉底色 */
:deep(.cyber-drawer) {
    background-color: rgba(5, 5, 15, 0.95) !important;
    backdrop-filter: blur(10px);
    border-left: 1px solid #00F0FF;
    box-shadow: -5px 0 30px rgba(0, 240, 255, 0.1);
}

:deep(.el-drawer__body) {
    padding: 0;
}
</style>