<template>
  <div class="app-container">
    <div class="bg-decoration">
      <div class="glow-circle top-left"></div>
      <div class="glow-circle bottom-right"></div>
      <div class="grid-overlay"></div>
    </div>

    <aside class="sidebar-wrapper" :class="{ 'collapsed': isCollapsed }">
      <div class="sidebar-logo">
        <el-icon :size="28" color="#00F0FF">
          <Monitor />
        </el-icon>
        <span v-show="!isCollapsed" class="logo-text">BEMS 运营中枢</span>
      </div>

      <el-menu :default-active="activeIndex" class="side-menu" router :collapse="isCollapsed"
        background-color="transparent" text-color="#a0a2b8" active-text-color="#00F0FF">
        <el-menu-item index="/dashboard">
          <el-icon>
            <DataLine />
          </el-icon>
          <template #title>能效态势大屏</template>
        </el-menu-item>

        <el-menu-item index="/query">
          <el-icon>
            <Search />
          </el-icon>
          <template #title>数据中心与报表</template>
        </el-menu-item>

      </el-menu>

      <div class="collapse-trigger" @click="isCollapsed = !isCollapsed">
        <el-icon v-if="isCollapsed">
          <Expand />
        </el-icon>
        <el-icon v-else>
          <Fold />
        </el-icon>
      </div>
    </aside>

    <main class="main-container">
      <header class="top-header">
        <div class="header-left">
          <span class="breadcrumb-current">{{ currentPathName }}</span>
        </div>

        <div class="header-right">
          <div class="system-time">
            <el-icon>
              <Timer />
            </el-icon>
            <span>{{ currentTime }}</span>
          </div>
          <el-divider direction="vertical" />
          <el-avatar :size="32" class="user-avatar">Admin</el-avatar>
        </div>
      </header>

      <section class="content-view">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </section>
    </main>

    <GlobalAiAssistant ref="aiAssistantRef" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, provide } from 'vue'
import { useRoute } from 'vue-router'
import {
  DataLine, Search, Monitor,
  Timer, Fold, Expand
} from '@element-plus/icons-vue'
import GlobalAiAssistant from './components/GlobalAiAssistant.vue'

const route = useRoute()
const isCollapsed = ref(false)
const aiAssistantRef = ref(null)
const currentTime = ref('')

// 动态路由名称
const currentPathName = computed(() => {
  const map = {
    '/dashboard': '能效态势看板 // Energy Dashboard',
    '/query': '数据中心与智能报表 // Data Center'
  }
  return map[route.path] || '系统主页'
})

const activeIndex = computed(() => route.path)

// 更新系统时间逻辑
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString()
}

onMounted(() => {
  updateTime()
  setInterval(updateTime, 1000)
})

// 全局 AI 调用接口
const triggerAiWithContext = (promptText) => {
  if (aiAssistantRef.value) aiAssistantRef.value.openAssistant(promptText)
}
provide('callBemsAi', triggerAiWithContext)
</script>

<style>
/* --- 黑曜石磨砂 (Obsidian Glass) 核心变量 --- */
:root {
  --bg-dark: #050507;
  --glass-bg: rgba(22, 22, 26, 0.45);
  --glass-border: rgba(255, 255, 255, 0.08);
  --glass-rim: rgba(255, 255, 255, 0.12);
  /* 模拟受光切面 */
  --accent-primary: #3b82f6;
  --accent-green: #10b981;
  --text-main: #f4f4f5;
  --text-muted: #71717a;
}

body,
html {
  margin: 0;
  padding: 0;
  background-color: var(--bg-dark);
  color: var(--text-main);
  font-family: 'Inter', -apple-system, system-ui, sans-serif;
  overflow: hidden;
}

.app-container {
  display: flex;
  width: 100vw;
  height: 100vh;
  position: relative;
}

/* 🚀 核心：注入微米级噪点背景，消除塑料感 */
.app-container::before {
  content: "";
  position: fixed;
  inset: 0;
  z-index: -1;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
  opacity: 0.015;
  /* 极低透明度，若隐若现 */
  pointer-events: none;
}

/* 🚀 极致细节：全局注入 1% 噪点，消除数码味 */
.app-container::after {
  content: "";
  position: fixed;
  inset: 0;
  z-index: 10000;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.7' numOctaves='3'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  opacity: 0.015;
  pointer-events: none;
}

/* --- 极简背景 --- */
.bg-decoration {
  position: absolute;
  inset: 0;
  z-index: -1;
  overflow: hidden;
  background: var(--bg-dark);
}

.glow-circle {
  position: absolute;
  width: 800px;
  height: 800px;
  border-radius: 50%;
  filter: blur(150px);
  opacity: 0.06;
}

.top-left {
  top: -200px;
  left: -200px;
  background: var(--accent-primary);
}

.bottom-right {
  bottom: -200px;
  right: -200px;
  background: #8b5cf6;
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
}

/* 侧边栏：极致通透的悬浮玻璃 */
.sidebar-wrapper {
  width: 240px;
  background: rgba(12, 12, 14, 0.4);
  backdrop-filter: blur(32px) saturate(160%);
  -webkit-backdrop-filter: blur(32px) saturate(160%);
  border-right: 1px solid var(--glass-border);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 10;
}

.sidebar-wrapper.collapsed {
  width: 72px;
}

.sidebar-logo {
  height: 80px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 14px;
}

.logo-text {
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}

.side-menu {
  border-right: none !important;
  flex: 1;
}

/* 菜单项：取消发光，改用深度阴影 */
.el-menu-item {
  height: 48px !important;
  margin: 8px 16px;
  border-radius: 12px;
  color: var(--text-muted) !important;
  transition: all 0.2s;
}

.el-menu-item.is-active {
  background: rgba(59, 130, 246, 0.08) !important;
  color: var(--accent-primary) !important;
  box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.2);
}

.el-menu-item:hover {
  background: rgba(255, 255, 255, 0.04) !important;
  color: #fff !important;
}

.collapse-trigger {
  height: 48px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  border-top: 1px solid var(--border-color);
  color: var(--text-muted);
  transition: all 0.2s;
}

.collapse-trigger:hover {
  color: var(--text-main);
  background: rgba(255, 255, 255, 0.02);
}

/* --- 顶栏内容区 --- */
.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.top-header {
  height: 64px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--glass-border);
  background: rgba(5, 5, 7, 0.3);
  backdrop-filter: blur(10px);
}

.breadcrumb-current {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-main);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
  font-size: 13px;
  color: var(--text-muted);
}

.system-time {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  background: #27272a;
  border: 1px solid var(--border-color);
  color: var(--text-main);
  font-size: 12px;
}

.content-view {
  flex: 1;
  overflow: auto;
  padding: 24px;
  box-sizing: border-box;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: all 0.2s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Element Plus 折叠修复 */
.el-menu--collapse .el-menu-item {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0 !important;
}

.el-menu--collapse .el-tooltip__trigger {
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
  padding: 0 !important;
  width: 100%;
}

.el-menu--collapse .el-menu-item .el-icon {
  margin: 0 !important;
  font-size: 20px;
}
</style>