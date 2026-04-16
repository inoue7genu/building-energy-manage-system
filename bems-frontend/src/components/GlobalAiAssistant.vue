<template>
    <div class="ai-copilot-container">
        <div class="floating-btn pulse-glow" ref="floatingBtnRef" @mousedown="handleBtnMouseDown" v-show="!visible">
            <el-icon :size="28">
                <Cpu />
            </el-icon>
        </div>

        <transition name="pop-up">
            <div v-show="visible" class="floating-chat-panel cyber-panel" ref="chatPanelRef">

                <div class="panel-header" @mousedown="handleMouseDown">
                    <div class="header-left">
                        <el-icon class="pulse-icon">
                            <Odometer />
                        </el-icon>
                        <span>BEMS Copilot 智能大脑</span>
                    </div>
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
                        <el-input v-model="inputMessage" type="textarea" :rows="2" placeholder="输入系统诊断指令，按 Enter 发送..."
                            class="cyber-textarea" @keydown.enter.prevent="sendMessage" :disabled="isTyping" />

                        <el-button v-if="!isTyping" type="primary" class="cyber-btn send-btn pulse-btn"
                            @click="sendMessage">
                            <el-icon :size="20">
                                <Position />
                            </el-icon>
                        </el-button>

                        <el-button v-else type="danger" class="cyber-btn stop-btn pulse-btn" @click="stopGeneration">
                            <el-icon :size="20">
                                <VideoPause />
                            </el-icon>
                        </el-button>
                    </div>
                </div>
            </div>
        </transition>
    </div>
</template>

<script setup>
import { ref, nextTick, watch, onUnmounted } from 'vue'
import { Odometer, Position, Close, Cpu, VideoPause } from '@element-plus/icons-vue'
import { marked } from 'marked'

// --- 状态定义 ---
const visible = ref(false)
const inputMessage = ref('')
const isTyping = ref(false)
const chatHistoryRef = ref(null)
const floatingBtnRef = ref(null)
const chatPanelRef = ref(null)
const activeEventSource = ref(null)

const chatList = ref([
    {
        role: 'ai',
        content: '指挥官您好，我是 BEMS 全局智能大脑。已悬浮待命，随时为您进行能源数据探查与诊断。'
    }
])

// --- 🚀 核心：精准中心对齐定位算法 ---
// 注意：现在接收外部传入的 btnRect，避免拿到 display: none 时的 0 坐标
const updatePanelPosition = (btnRect) => {
    const panel = chatPanelRef.value;
    if (!panel || !btnRect) return;

    const panelWidth = panel.offsetWidth || 420;
    const panelHeight = panel.offsetHeight || 500;
    const margin = 20;
    const gap = 10; // 🚀 优化 1：将基础间距缩短为 10px，视觉更紧凑

    // 1. 计算悬浮球的中心 X 坐标
    const btnCenterX = btnRect.left + btnRect.width / 2;

    // 2. 计算面板的起始 Left
    let targetLeft = btnCenterX - panelWidth / 2;

    // 3. X 轴边界碰撞修正
    if (targetLeft < margin) {
        targetLeft = margin;
    } else if (targetLeft + panelWidth > window.innerWidth - margin) {
        targetLeft = window.innerWidth - panelWidth - margin;
    }

    // 4. Y 轴计算：尝试弹在球的正上方
    let targetTop = btnRect.top - panelHeight - gap;

    // 5. Y 轴边界碰撞修正：如果上方放不下，弹到球下方
    if (targetTop < margin) {
        targetTop = btnRect.bottom + gap;
        // 兜底：如果下方也放不下，强行吸附在屏幕底部
        if (targetTop + panelHeight > window.innerHeight - margin) {
            targetTop = window.innerHeight - panelHeight - margin;
        }
    }

    panel.style.right = 'auto';
    panel.style.bottom = 'auto';
    panel.style.left = `${targetLeft}px`;
    panel.style.top = `${targetTop}px`;
};

// --- 面板控制与滚动 ---
const toggleAssistant = async () => {
    if (!visible.value) {
        // 🚀 关键修复：在 visible 变为 true 之前（悬浮球消失前），抓取真实的物理坐标
        const btn = floatingBtnRef.value;
        const btnRect = btn ? btn.getBoundingClientRect() : null;

        visible.value = true;

        await nextTick(); // 等待 Vue 渲染出对话框DOM

        if (btnRect) {
            updatePanelPosition(btnRect); // 使用刚才抓取的真实坐标进行定位
        }
        scrollToBottom();
    } else {
        visible.value = false;
    }
}

const openAssistant = async (autoPrompt = '') => {
    if (!visible.value) {
        const btn = floatingBtnRef.value;
        const btnRect = btn ? btn.getBoundingClientRect() : null;

        visible.value = true;
        await nextTick();

        if (btnRect) {
            updatePanelPosition(btnRect);
        }
    }

    if (autoPrompt) {
        setTimeout(() => {
            if (!isTyping.value) {
                inputMessage.value = autoPrompt;
                sendMessage();
            }
        }, 300);
    }
}
defineExpose({ openAssistant });

// --- 悬浮球拖拽逻辑 ---
let isDraggingBtn = false;
const handleBtnMouseDown = (e) => {
    const el = floatingBtnRef.value;
    if (!el) return;

    isDraggingBtn = false;
    const startX = e.clientX;
    const startY = e.clientY;
    const rect = el.getBoundingClientRect();
    const offsetX = e.clientX - rect.left;
    const offsetY = e.clientY - rect.top;

    const handleMouseMove = (moveEvent) => {
        const dx = moveEvent.clientX - startX;
        const dy = moveEvent.clientY - startY;
        if (Math.abs(dx) > 3 || Math.abs(dy) > 3) isDraggingBtn = true;

        let newLeft = moveEvent.clientX - offsetX;
        let newTop = moveEvent.clientY - offsetY;

        newLeft = Math.max(0, Math.min(window.innerWidth - rect.width, newLeft));
        newTop = Math.max(0, Math.min(window.innerHeight - rect.height, newTop));

        el.style.left = `${newLeft}px`;
        el.style.top = `${newTop}px`;
        el.style.right = 'auto';
        el.style.bottom = 'auto';
        el.style.transition = 'none';
    };

    const handleMouseUp = () => {
        el.style.transition = 'all 0.3s ease';
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
        if (!isDraggingBtn) toggleAssistant();
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
};

// --- 对话面板顶部拖拽逻辑 ---
const handleMouseDown = (e) => {
    const el = chatPanelRef.value;
    if (!el) return;

    const rect = el.getBoundingClientRect();
    const offsetX = e.clientX - rect.left;
    const offsetY = e.clientY - rect.top;

    const handleMouseMove = (moveEvent) => {
        let newLeft = moveEvent.clientX - offsetX;
        let newTop = moveEvent.clientY - offsetY;

        newLeft = Math.max(0, Math.min(window.innerWidth - rect.width, newLeft));
        newTop = Math.max(0, Math.min(window.innerHeight - rect.height, newTop));

        el.style.left = `${newLeft}px`;
        el.style.top = `${newTop}px`;
    };

    const handleMouseUp = () => {
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
};

const scrollToBottom = async () => {
    await nextTick();
    if (chatHistoryRef.value) {
        chatHistoryRef.value.scrollTop = chatHistoryRef.value.scrollHeight;
    }
};

// --- AI 通信逻辑 ---
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
    activeEventSource.value = eventSource

    eventSource.onmessage = (event) => {
        const lastMsgIndex = chatList.value.length - 1
        if (event.data === '[DONE]') {
            eventSource.close()
            activeEventSource.value = null
            isTyping.value = false
            return
        }

        const newText = event.data.replace(/\\n/g, '\n')
        chatList.value[lastMsgIndex].content += newText
        scrollToBottom()
    }

    eventSource.onerror = (error) => {
        isTyping.value = false
        eventSource.close()
        activeEventSource.value = null

        const lastMsgIndex = chatList.value.length - 1
        if (chatList.value[lastMsgIndex].content === '') {
            chatList.value[lastMsgIndex].content = '⚠️ **中枢连接异常**：大模型服务响应超时或网络阻断。请检查后端服务。'
        } else {
            if (!chatList.value[lastMsgIndex].content.includes('已手动中止生成')) {
                chatList.value[lastMsgIndex].content += '\n\n*(信号中断，停止输出)*'
            }
        }
        scrollToBottom()
    }
}

const stopGeneration = () => {
    if (activeEventSource.value) {
        activeEventSource.value.close()
        activeEventSource.value = null
        isTyping.value = false

        const lastMsgIndex = chatList.value.length - 1
        if (chatList.value[lastMsgIndex].role === 'ai') {
            chatList.value[lastMsgIndex].content += '\n\n*(已手动中止生成)*'
        }
        scrollToBottom()
    }
}

const parseMarkdown = (text) => marked.parse(text || '');

onUnmounted(() => stopGeneration());
watch(visible, (newVal) => { if (!newVal) stopGeneration() });
</script>

<style scoped>
.ai-copilot-container {
    position: relative;
    z-index: 9999;
}

/* 高级感悬浮球 */
.floating-btn {
    position: fixed;
    right: 40px;
    bottom: 40px;
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: rgba(24, 24, 27, 0.8);
    backdrop-filter: blur(12px);
    border: 1px solid rgba(255, 255, 255, 0.15);
    display: flex;
    justify-content: center;
    align-items: center;
    color: #fafafa;
    cursor: pointer;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
    transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), background 0.2s;
}

.floating-btn:hover {
    transform: scale(1.08);
    background: #27272a;
    border-color: rgba(255, 255, 255, 0.3);
}

/* 高级感对话面板 */
.floating-chat-panel {
    position: fixed;
    width: 400px;
    height: 60vh;
    min-height: 480px;
    background-color: rgba(24, 24, 27, 0.75);
    backdrop-filter: blur(24px) saturate(180%);
    -webkit-backdrop-filter: blur(24px);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    box-shadow: 0 12px 48px rgba(0, 0, 0, 0.5);
    overflow: hidden;
}

.pop-up-enter-active,
.pop-up-leave-active {
    transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
    transform-origin: center center;
}

.pop-up-enter-from,
.pop-up-leave-to {
    opacity: 0;
    transform: scale(0.95);
    pointer-events: none;
}

.panel-header {
    padding: 16px 20px;
    background-color: rgba(9, 9, 11, 0.5);
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    color: #fafafa;
    font-weight: 600;
    font-size: 14px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    cursor: grab;
    user-select: none;
}

.panel-header:active {
    cursor: grabbing;
}

.header-left {
    display: flex;
    align-items: center;
    gap: 8px;
}

.close-btn {
    color: #a1a1aa;
    transition: color 0.2s;
}

.close-btn:hover {
    color: #ef4444;
}

.chat-panel {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.chat-history {
    flex: 1;
    padding: 20px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.chat-history::-webkit-scrollbar {
    width: 4px;
}

.chat-history::-webkit-scrollbar-thumb {
    background: #3f3f46;
    border-radius: 2px;
}

/* 聊天气泡 (Apple iMessage 风格变体) */
.message-wrapper {
    display: flex;
    gap: 12px;
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
    width: 28px;
    height: 28px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    font-size: 11px;
    flex-shrink: 0;
}

.is-user .avatar {
    background: #3b82f6;
    color: #fff;
}

.is-ai .avatar {
    background: #27272a;
    color: #fafafa;
    border: 1px solid rgba(255, 255, 255, 0.1);
}

.message-bubble {
    padding: 12px 16px;
    border-radius: 18px;
    line-height: 1.5;
    font-size: 14px;
    position: relative;
    word-break: break-word;
}

.is-user .message-bubble {
    background: #3b82f6;
    color: #fff;
    border-bottom-right-radius: 4px;
}

.is-ai .message-bubble {
    background: rgba(39, 39, 42, 0.8);
    border: 1px solid rgba(255, 255, 255, 0.05);
    color: #e4e4e7;
    border-bottom-left-radius: 4px;
}

.cursor {
    display: inline-block;
    width: 2px;
    height: 14px;
    background-color: #fafafa;
    margin-left: 2px;
    vertical-align: middle;
    animation: blink 1s step-end infinite;
}

@keyframes blink {
    50% {
        opacity: 0;
    }
}

/* 输入区 */
.input-area {
    padding: 16px;
    background-color: rgba(9, 9, 11, 0.8);
    border-top: 1px solid rgba(255, 255, 255, 0.05);
    display: flex;
    gap: 10px;
    align-items: flex-end;
}

:deep(.cyber-textarea .el-textarea__inner) {
    background-color: #18181b !important;
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important;
    color: #fafafa !important;
    border-radius: 12px;
    padding: 10px 14px;
    resize: none;
    font-family: inherit;
}

:deep(.cyber-textarea .el-textarea__inner:focus) {
    box-shadow: 0 0 0 1px #3b82f6 inset !important;
}

.cyber-btn {
    background-color: #27272a;
    border: 1px solid rgba(255, 255, 255, 0.1);
    height: 44px;
    width: 44px;
    border-radius: 50%;
    padding: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    transition: all 0.2s;
}

.send-btn {
    color: #fafafa;
}

.send-btn:hover {
    background-color: #3b82f6;
    border-color: #3b82f6;
}

.stop-btn {
    border-color: rgba(239, 68, 68, 0.5);
    color: #ef4444;
}

.stop-btn:hover {
    background-color: #ef4444;
    color: #fff;
}

/* Markdown 覆写 */
:deep(.markdown-body) {
    color: inherit;
    font-size: 14px;
}

:deep(.markdown-body p) {
    margin-bottom: 8px;
    margin-top: 0;
}

:deep(.markdown-body p:last-child) {
    margin-bottom: 0;
}

:deep(.markdown-body strong) {
    font-weight: 600;
    color: #fff;
}
</style>