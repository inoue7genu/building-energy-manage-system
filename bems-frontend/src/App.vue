<template>
  <div class="app-wrapper">
    <div class="top-nav-bar">
      <div class="logo">
        <el-icon :size="24" color="#00F0FF">
          <Odometer />
        </el-icon>
        <span class="logo-text">BEMS 智能运营中枢</span>
      </div>

      <el-menu :default-active="activeIndex" class="bems-menu" mode="horizontal" router background-color="transparent"
        text-color="#a0a2b8" active-text-color="#00F0FF">
        <el-menu-item index="/dashboard">
          <el-icon>
            <DataLine />
          </el-icon>能效态势大屏
        </el-menu-item>
        <el-menu-item index="/query">
          <el-icon>
            <Search />
          </el-icon>数据中心与报表
        </el-menu-item>

        <div class="ai-trigger-wrapper">
          <el-button class="cyber-ai-btn pulse-glow" @click="triggerAi">
            <el-icon>
              <Cpu />
            </el-icon> BEMS Copilot
          </el-button>
        </div>
      </el-menu>
    </div>

    <div class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>

    <GlobalAiAssistant ref="aiAssistantRef" />
  </div>
</template>

<script setup>
import { computed, ref, provide } from 'vue' // 新增 provide
import { useRoute } from 'vue-router'
import { DataLine, Search, Cpu, Odometer } from '@element-plus/icons-vue'
import GlobalAiAssistant from './components/GlobalAiAssistant.vue'

const route = useRoute()
const activeIndex = computed(() => route.path)

const aiAssistantRef = ref(null)

// 普通唤醒
const triggerAi = () => {
  if (aiAssistantRef.value) aiAssistantRef.value.openAssistant()
}

// 带参唤醒 (供底层子页面调用)
const triggerAiWithContext = (promptText) => {
  if (aiAssistantRef.value) aiAssistantRef.value.openAssistant(promptText)
}

// 💡 注册全局通信频道，任何页面都可以一键呼叫 AI
provide('callBemsAi', triggerAiWithContext)
</script>

<style>
/* --- 全局样式重置 --- */
body,
html {
  margin: 0;
  padding: 0;
  background-color: #05050f;
  /* 极度深邃的暗黑底色 */
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  color: #ffffff;
}

.app-wrapper {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

/* --- 导航栏样式定制 (强制单行) --- */
.top-nav-bar {
  display: flex;
  flex-direction: row;
  /* 强制横向排列 */
  align-items: center;
  /* 垂直居中 */
  justify-content: space-between;
  /* 两端对齐：Logo在左，菜单在右 */
  height: 60px;
  /* 锁死高度 */
  background-color: #0b091a;
  border-bottom: 1px solid #1f1d36;
  padding: 0 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
  box-sizing: border-box;
}

.logo {
  display: flex;
  align-items: center;
  white-space: nowrap;
  /* 强制 Logo 文字不换行 */
}

.logo-text {
  font-size: 20px;
  font-weight: bold;
  color: #ffffff;
  margin-left: 10px;
  letter-spacing: 1px;
}

/* 清除 Element Plus 菜单自带的底边框，使其融入容器 */
.bems-menu {
  flex: 1;
  justify-content: flex-end;
  /* 菜单项靠右 */
  border-bottom: none !important;
  height: 60px;
}

.el-menu-item {
  font-size: 16px;
  height: 60px !important;
  line-height: 60px !important;
}

/* --- 内容区与动画 --- */
.main-content {
  flex: 1;
  overflow: auto;
  padding: 20px;
  box-sizing: border-box;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>