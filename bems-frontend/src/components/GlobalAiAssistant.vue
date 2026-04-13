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
                        <el-button type="primary" class="cyber-btn send-btn pulse-btn" @click="sendMessage"
                            :loading="isTyping">
                            <el-icon :size="20">
                                <Position />
                            </el-icon>
                        </el-button>
                    </div>
                </div>
            </div>
        </transition>
    </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { Odometer, Position, Close, Cpu } from '@element-plus/icons-vue'
import { marked } from 'marked'

const visible = ref(false)
const inputMessage = ref('')
const isTyping = ref(false)
const chatHistoryRef = ref(null)

// --- 悬浮球拖拽专用逻辑 ---
const floatingBtnRef = ref(null);
let isDraggingBtn = false; // 用于区分是“拖拽”还是“点击”

const handleBtnMouseDown = (e) => {
    const el = floatingBtnRef.value;
    if (!el) return;

    isDraggingBtn = false; // 按下时重置拖拽标记
    const startX = e.clientX;
    const startY = e.clientY;

    const rect = el.getBoundingClientRect();
    const offsetX = e.clientX - rect.left;
    const offsetY = e.clientY - rect.top;

    // 冻结当前坐标，接管 CSS 的 right 和 bottom
    el.style.left = rect.left + 'px';
    el.style.top = rect.top + 'px';
    el.style.right = 'auto';
    el.style.bottom = 'auto';
    el.style.transition = 'none'; // 拖拽时取消 CSS 动画，防止跟手卡顿

    const handleMouseMove = (moveEvent) => {
        moveEvent.preventDefault();

        // 计算移动距离
        const dx = moveEvent.clientX - startX;
        const dy = moveEvent.clientY - startY;

        // ⚠️ 核心：如果移动距离超过 3 像素，判定为拖拽操作
        if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
            isDraggingBtn = true;
        }

        let newLeft = moveEvent.clientX - offsetX;
        let newTop = moveEvent.clientY - offsetY;

        // 屏幕边界碰撞检测
        newLeft = Math.max(0, Math.min(window.innerWidth - rect.width, newLeft));
        newTop = Math.max(0, Math.min(window.innerHeight - rect.height, newTop));

        el.style.left = newLeft + 'px';
        el.style.top = newTop + 'px';
    };

    const handleMouseUp = () => {
        el.style.transition = 'all 0.3s ease'; // 恢复原来的 CSS hover 动画
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);

        // ⚠️ 核心修复点：在这里判断，如果没有发生拖拽，就视为“点击”，执行打开逻辑
        if (!isDraggingBtn) {
            toggleAssistant();
        }
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
};


const chatPanelRef = ref(null);
const position = ref({ x: 0, y: 0 }); // 记录偏移量

const handleMouseDown = (e) => {
    const el = chatPanelRef.value;
    if (!el) return;

    // 1. 获取面板当前的真实物理位置和尺寸（不受 right/bottom 影响）
    const rect = el.getBoundingClientRect();

    // 2. 计算鼠标点击点距离面板左上角的偏移量
    const offsetX = e.clientX - rect.left;
    const offsetY = e.clientY - rect.top;

    // 3. ⚠️ 关键步骤：冻结当前坐标，彻底解除 CSS 的 right 和 bottom 锁定
    el.style.left = rect.left + 'px';
    el.style.top = rect.top + 'px';
    el.style.right = 'auto';
    el.style.bottom = 'auto';

    const handleMouseMove = (moveEvent) => {
        // 防止选中文字等默认行为导致拖拽卡顿
        moveEvent.preventDefault();

        // 4. 计算移动后的新坐标
        let newLeft = moveEvent.clientX - offsetX;
        let newTop = moveEvent.clientY - offsetY;

        // 5. 屏幕边界碰撞检测（保证悬浮框不会被拖出屏幕外面）
        newLeft = Math.max(0, Math.min(window.innerWidth - rect.width, newLeft));
        newTop = Math.max(0, Math.min(window.innerHeight - rect.height, newTop));

        // 6. 更新位置
        el.style.left = newLeft + 'px';
        el.style.top = newTop + 'px';
    };

    const handleMouseUp = () => {
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
};

const chatList = ref([
    {
        role: 'ai',
        content: '指挥官您好，我是 BEMS 全局智能大脑。已悬浮待命，随时为您进行能源数据探查与诊断。'
    }
])

// 切换面板显示状态
const toggleAssistant = () => {
    visible.value = !visible.value;
    if (visible.value) scrollToBottom();
}

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

    eventSource.onerror = (error) => {
        isTyping.value = false
        eventSource.close()

        // 🚨 新增：捕获异常并给用户友好的提示
        const lastMsgIndex = chatList.value.length - 1
        if (chatList.value[lastMsgIndex].content === '') {
            // 如果 AI 一句话都没回就断了
            chatList.value[lastMsgIndex].content = '⚠️ **中枢连接异常**：大模型服务响应超时或知识库上下文超载。请联系系统管理员检查 RAG 参数或 API 状态。'
        } else {
            // 如果说到一半断了
            chatList.value[lastMsgIndex].content += '\n\n*(信号中断，停止输出)*'
        }
        scrollToBottom()
    }
}

const parseMarkdown = (text) => {
    if (!text) return ''
    return marked.parse(text)
}
</script>

<style scoped>
.ai-copilot-container {
    position: relative;
    z-index: 9999;
}

/* --- 1. 悬浮唤醒按钮 --- */
.floating-btn {
    position: fixed;
    right: 40px;
    bottom: 40px;
    width: 60px;
    height: 60px;
    border-radius: 50%;
    background: linear-gradient(135deg, #0b091a, #1f1d36);
    border: 2px solid #00F0FF;
    display: flex;
    justify-content: center;
    align-items: center;
    color: #00F0FF;
    cursor: pointer;
    box-shadow: 0 0 15px rgba(0, 240, 255, 0.3);
    transition: all 0.3s ease;
}

.floating-btn:hover {
    transform: scale(1.1);
    box-shadow: 0 0 25px rgba(0, 240, 255, 0.6);
}

.pulse-glow {
    animation: glow 2s infinite alternate;
}

@keyframes glow {
    from {
        box-shadow: 0 0 10px rgba(0, 240, 255, 0.2);
    }

    to {
        box-shadow: 0 0 20px rgba(0, 240, 255, 0.6), 0 0 10px rgba(0, 255, 157, 0.4);
    }
}

/* --- 2. 悬浮对话面板 (毛玻璃科技风) --- */
.floating-chat-panel {
    position: fixed;
    right: 40px;
    bottom: 120px;
    /* 位于悬浮球上方 */
    width: 420px;
    height: 65vh;
    /* 动态高度，适配各种屏幕 */
    min-height: 500px;
    background-color: rgba(11, 9, 26, 0.85);
    /* 半透明极深蓝 */
    backdrop-filter: blur(12px);
    /* 毛玻璃滤镜 */
    -webkit-backdrop-filter: blur(12px);
    border: 1px solid #2A2946;
    border-radius: 12px;
    display: flex;
    flex-direction: column;
    box-shadow: 0 15px 35px rgba(0, 0, 0, 0.6), 0 0 20px rgba(0, 240, 255, 0.15);
    overflow: hidden;
}

/* 动画效果 */
.pop-up-enter-active,
.pop-up-leave-active {
    transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.pop-up-enter-from,
.pop-up-leave-to {
    opacity: 0;
    transform: translateY(20px) scale(0.95);
    pointer-events: none;
}

/* --- 内部布局与样式 --- */
.panel-header {
    padding: 15px 20px;
    background-color: rgba(5, 5, 15, 0.6);
    border-bottom: 1px solid #2A2946;
    color: #00F0FF;
    font-weight: bold;
    font-size: 15px;
    display: flex;
    align-items: center;
    justify-content: space-between;

    cursor: grab;
    /* 鼠标移上去变成张开的小手 */
    user-select: none;
    /* 防止拖拽时意外选中文字变蓝，影响观感 */
}

/* 拖拽按下时的鼠标样式 */
.panel-header:active {
    cursor: grabbing;
    /* 鼠标按下时变成抓取状态 */
}

.header-left {
    display: flex;
    align-items: center;
    gap: 8px;
}

.close-btn {
    color: #a0a2b8;
    transition: color 0.3s;
}

.close-btn:hover {
    color: #FF4D4F;
    transform: rotate(90deg);
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
    gap: 12px;
    max-width: 92%;
}

.is-user {
    align-self: flex-end;
    flex-direction: row-reverse;
}

.is-ai {
    align-self: flex-start;
}

.avatar {
    width: 32px;
    height: 32px;
    border-radius: 6px;
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
}

.is-ai .avatar {
    background: rgba(0, 240, 255, 0.1);
    color: #00F0FF;
    border-color: #00F0FF;
}

.message-bubble {
    padding: 10px 14px;
    border-radius: 8px;
    line-height: 1.6;
    font-size: 13px;
    position: relative;
    word-break: break-all;
}

.is-user .message-bubble {
    background: linear-gradient(135deg, #00FF9D, #00cc7e);
    color: #000;
    border-top-right-radius: 0;
    font-weight: 500;
}

.is-ai .message-bubble {
    background: rgba(30, 28, 48, 0.7);
    border: 1px solid #2A2946;
    color: #E0E2F5;
    border-top-left-radius: 0;
}

.cursor {
    display: inline-block;
    width: 6px;
    height: 14px;
    background-color: #00F0FF;
    margin-left: 2px;
    vertical-align: middle;
    animation: blink 1s step-end infinite;
}

.input-area {
    padding: 15px;
    background-color: rgba(5, 5, 15, 0.8);
    border-top: 1px solid #2A2946;
    display: flex;
    gap: 10px;
    align-items: flex-end;
}

:deep(.cyber-textarea .el-textarea__inner) {
    background-color: rgba(5, 5, 15, 0.5) !important;
    box-shadow: 0 0 0 1px #2A2946 inset !important;
    color: #00F0FF !important;
    border-radius: 6px;
    font-family: monospace;
    resize: none;
}

:deep(.cyber-textarea .el-textarea__inner:focus) {
    box-shadow: 0 0 0 1px #00F0FF inset !important;
}

.cyber-btn {
    background-color: transparent;
    border: 1px solid #00F0FF;
    height: 52px;
    /* 匹配两行 textarea 的高度 */
    width: 52px;
    border-radius: 6px;
    padding: 0;
    display: flex;
    justify-content: center;
    align-items: center;
}

.send-btn {
    color: #00F0FF;
}

.send-btn:hover {
    background-color: rgba(0, 240, 255, 0.1);
    box-shadow: 0 0 15px rgba(0, 240, 255, 0.4);
}

/* Markdown 样式覆盖 */
:deep(.markdown-body) {
    color: #E0E2F5;
}

:deep(.markdown-body p) {
    margin-bottom: 6px;
    margin-top: 0;
}

:deep(.markdown-body p:last-child) {
    margin-bottom: 0;
}

:deep(.markdown-body strong) {
    color: #00F0FF;
}
</style>