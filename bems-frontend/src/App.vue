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
          <Odometer />
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
  DataLine, Search, Odometer,
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
/* --- 全局样式与变量 --- */
:root {
  --bg-dark: #05050f;
  --panel-bg: rgba(11, 9, 26, 0.6);
  --accent-cyan: #00F0FF;
  --accent-green: #00FF9D;
  --border-color: #2A2946;
}

body,
html {
  margin: 0;
  padding: 0;
  background-color: var(--bg-dark);
  color: #fff;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  overflow: hidden;
}

.app-container {
  display: flex;
  width: 100vw;
  height: 100vh;
  position: relative;
}

/* --- 背景装饰 --- */
.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
  overflow: hidden;
}

.glow-circle {
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  filter: blur(120px);
  opacity: 0.15;
}

.top-left {
  top: -100px;
  left: -100px;
  background: var(--accent-cyan);
}

.bottom-right {
  bottom: -100px;
  right: -100px;
  background: var(--accent-green);
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 40px 40px;
}

/* --- 侧边栏样式 --- */
.sidebar-wrapper {
  width: 240px;
  background: var(--panel-bg);
  backdrop-filter: blur(15px);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 10;
}

.sidebar-wrapper.collapsed {
  width: 64px;
}

.sidebar-logo {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 18px;
  gap: 12px;
  overflow: hidden;
}

.logo-text {
  font-size: 18px;
  font-weight: 800;
  color: var(--accent-cyan);
  white-space: nowrap;
  letter-spacing: 1px;
}

.side-menu {
  border-right: none !important;
  flex: 1;
}

/* 深度定制 Element Menu 极客风 */
.el-menu-item {
  height: 56px !important;
  margin: 4px 10px;
  border-radius: 8px;
  transition: all 0.3s;
}

.el-menu-item:hover {
  background: rgba(0, 240, 255, 0.1) !important;
  color: var(--accent-cyan) !important;
}

.el-menu-item.is-active {
  background: rgba(0, 240, 255, 0.15) !important;
  border: 1px solid rgba(0, 240, 255, 0.3);
  box-shadow: 0 0 15px rgba(0, 240, 255, 0.1);
}

.collapse-trigger {
  height: 48px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  border-top: 1px solid var(--border-color);
  color: #a0a2b8;
}

.collapse-trigger:hover {
  color: var(--accent-cyan);
}

/* --- 右侧内容区 --- */
.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.top-header {
  height: 64px;
  padding: 0 30px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(5, 5, 15, 0.4);
  backdrop-filter: blur(5px);
  border-bottom: 1px solid var(--border-color);
}

.breadcrumb-current {
  font-family: 'JetBrains Mono', monospace;
  font-weight: bold;
  font-size: 16px;
  color: #e0e2f5;
  letter-spacing: 1px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
  font-size: 14px;
  color: #a0a2b8;
}

.system-time {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  background: linear-gradient(135deg, var(--accent-cyan), #0088ff);
  font-weight: bold;
  color: #000;
  cursor: pointer;
}

.content-view {
  flex: 1;
  overflow: auto;
  padding: 24px;
  box-sizing: border-box;
}

/* 页面切换动画 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: all 0.3s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateX(15px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateX(-15px);
}

/* =========================================
   侧边栏收起状态下的图标居中 (Element Plus 专属优化)
========================================= */

/* 1. 收起时菜单项本身居中 */
.el-menu--collapse .el-menu-item {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0 !important;
}

/* 2. 🎯 核心修复：强行居中 Element Plus 自动生成的 tooltip 隐藏包裹层，并清除它的默认边距 */
.el-menu--collapse .el-tooltip__trigger {
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
  padding: 0 !important;
  width: 100%;
}

/* 3. 图标微调，去偏移 */
.el-menu--collapse .el-menu-item .el-icon {
  margin: 0 !important;
  font-size: 22px;
  /* 适当放大图标，显得更饱满 */
}

/* 4. 侧边栏收起时的宽度微调，让它更窄更精致 */
.sidebar-wrapper.collapsed {
  width: 72px;
}

/* 5. 修复折叠按钮的居中对齐 */
.collapse-trigger {
  height: 48px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  border-top: 1px solid var(--border-color);
  color: #a0a2b8;
  transition: color 0.3s, background-color 0.3s;
}

.collapse-trigger:hover {
  color: var(--accent-cyan);
  background-color: rgba(0, 240, 255, 0.05);
}
</style>