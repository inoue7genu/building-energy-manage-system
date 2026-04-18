<template>
  <div class="app-container">
    <div class="bg-decoration">
      <div class="glow-circle top-left"></div>
      <div class="glow-circle bottom-right"></div>
      <div class="grid-overlay"></div>
    </div>

    <aside class="sidebar-wrapper" :class="{ 'collapsed': isCollapsed }" v-if="!isLoginPage">
      <div class="sidebar-logo">
        <el-icon :size="28" color="#00F0FF">
          <Monitor />
        </el-icon>
        <span v-show="!isCollapsed" class="logo-text">BEMS 运营中枢</span>
      </div>

      <el-menu :default-active="activeIndex" class="side-menu" router :collapse="isCollapsed"
        background-color="transparent" text-color="#a0a2b8" active-text-color="#00F0FF">

        <el-menu-item v-for="menu in menuList" :key="menu.path" :index="menu.path">
          <el-icon>
            <component :is="menu.icon" />
          </el-icon>
          <template #title>{{ menu.name }}</template>
        </el-menu-item>

        <el-menu-item index="/admin-hub" v-if="userRole === 'ADMIN'">
          <el-icon>
            <Platform />
          </el-icon>
          <template #title>管理中枢系统</template>
        </el-menu-item>

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
      <header class="top-header" v-if="!isLoginPage">
        <div class="header-left">
          <span class="breadcrumb-current">{{ currentPathName }}</span>
        </div>

        <div class="header-right">

          <el-button circle text @click="toggleTheme"
            style="font-size: 18px; color: var(--bems-text-primary); margin-right: 15px; transition: all 0.3s;">
            <el-icon>
              <Sunny v-if="!isLightMode" />
              <Moon v-else />
            </el-icon>
          </el-button>

          <div class="system-time">
            <el-icon>
              <Timer />
            </el-icon>
            <span>{{ currentTime }}</span>
          </div>
          <el-divider direction="vertical" />

          <el-dropdown trigger="click" @command="handleCommand">
            <div class="avatar-wrapper">
              <el-avatar :size="32" class="user-avatar">AD</el-avatar>
              <el-icon class="avatar-arrow">
                <ArrowDown />
              </el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout" style="color: var(--bems-color-danger);">
                  <el-icon>
                    <SwitchButton />
                  </el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

        </div>
      </header>

      <section class="content-view" :style="{ padding: isLoginPage ? '0' : '24px' }">
        <router-view />
      </section>
    </main>

    <GlobalAiAssistant ref="aiAssistantRef" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DataLine, Search, Monitor,
  Timer, Fold, Expand,
  Sunny, Moon, ArrowDown, SwitchButton,
  Platform
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import GlobalAiAssistant from './components/GlobalAiAssistant.vue'

const route = useRoute()
const router = useRouter() // 🚀 获取路由跳转能力

// 🚀 新增：用来判断当前是不是登录页
const isLoginPage = computed(() => route.path === '/login')

const userRole = computed(() => localStorage.getItem('bems-role'))


const isCollapsed = ref(false)
const aiAssistantRef = ref(null)
const currentTime = ref('')

const menuItems = computed(() => {
  // 1. 定义所有可能的菜单项
  const allMenus = [
    { name: '管理中枢系统', path: '/admin-hub', icon: 'Platform', role: 'ADMIN' },
    { name: '能效态势看板', path: '/dashboard', icon: 'DataLine', role: 'ALL' },
    { name: '数据中心与报表', path: '/query', icon: 'Search', role: 'ALL' }
  ]

  // 2. 核心修改：使用 filter 进行智能过滤
  return allMenus.filter(item => {
    // 如果菜单是 ALL，所有人都能看
    if (item.role === 'ALL') return true
    // 如果是 ADMIN 菜单，只有角色匹配时才返回
    return item.role === userRole.value
  })
})

// 🚀 处理下拉菜单的点击事件
const handleCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('bems-token')
    ElMessage.success('已安全退出系统')
    router.push('/login')
  }
}

// 动态路由名称
const currentPathName = computed(() => {
  const map = {
    // 🚀 新增：管理中枢系统的路由映射
    '/admin-hub': '管理中枢系统 // Management Hub',
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

// 🚀 主题切换逻辑
const isLightMode = ref(false)

const toggleTheme = () => {
  isLightMode.value = !isLightMode.value
  const theme = isLightMode.value ? 'light' : 'dark'

  // 核心魔法：改变 HTML 根节点的属性，触发变量瞬间翻转
  document.documentElement.setAttribute('data-theme', theme)
  // 将偏好存入本地，刷新不丢失
  localStorage.setItem('bems-theme', theme)

  // 🚀 核心新增：向全局发送一个“主题已切换”的广播事件
  window.dispatchEvent(new CustomEvent('theme-changed'))
}

onMounted(() => {
  updateTime()
  setInterval(updateTime, 1000)

  // 🚀 页面加载时恢复用户的主题选择
  const savedTheme = localStorage.getItem('bems-theme')
  if (savedTheme === 'light') {
    isLightMode.value = true
    document.documentElement.setAttribute('data-theme', 'light')
  }
})

// 全局 AI 调用接口
const triggerAiWithContext = (promptText) => {
  if (aiAssistantRef.value) aiAssistantRef.value.openAssistant(promptText)
}
provide('callBemsAi', triggerAiWithContext)
</script>

<style>
/* =========================================================
   💎 终极版：BEMS 黑曜石设计令牌 (Design Tokens)
   规则：全站禁止硬编码 RGBA/HEX，必须使用 var(--bems-xxx)
========================================================= */
:root {
  /* 1. 空间底层 */
  --bems-bg-base: #050507;

  /* 2. 玻璃材质体系 (Glassmorphism) */
  --bems-glass-heavy: rgba(12, 12, 14, 0.55);
  /* 侧边栏、全局弹窗 */
  --bems-glass-card: rgba(22, 22, 26, 0.45);
  /* 数据卡片、控制台 */
  --bems-glass-header: rgba(5, 5, 7, 0.4);
  /* 表头、顶栏 */
  --bems-glass-input: rgba(0, 0, 0, 0.3);
  /* 输入框、下凹区域 */

  /* 3. 物理厚度与切面反光 */
  --bems-border-base: rgba(255, 255, 255, 0.08);
  /* 极细磨砂外边框 */
  --bems-rim-light: rgba(255, 255, 255, 0.12);
  /* 顶部受光内边缘 */

  /* 4. 空间投影 (Z-Axis) */
  --bems-shadow-ambient: 0 12px 40px rgba(0, 0, 0, 0.4);
  /* 卡片底影 */
  --bems-shadow-float: 0 24px 60px rgba(0, 0, 0, 0.6);
  /* 悬浮层深影 */

  /* 5. 交互反馈 */
  --bems-hover-overlay: rgba(255, 255, 255, 0.06);
  /* 悬浮时的微微亮起 */
  --bems-active-overlay: rgba(59, 130, 246, 0.1);
  /* 选中态背景 */

  /* 6. 核心排版 */
  --bems-text-primary: #f4f4f5;
  /* 主标题、高亮数据 (锌白) */
  --bems-text-regular: #a1a1aa;
  /* 正文、普通描述 */
  --bems-text-secondary: #71717a;
  /* 次要信息、表头 (沉稳灰) */

  /* 7. 品牌强调色 */
  --bems-color-primary: #3b82f6;
  /* 静谧蓝 */
  --bems-color-success: #10b981;
  /* 翠玉绿 */
  --bems-color-warning: #f59e0b;
  --bems-color-danger: #ef4444;
}

/* =========================================================
   ❄️ 浅色白水晶材质字典 (Frost Glass Tokens)
   当 html 标签具有 data-theme='light' 时，自动替换全局变量
========================================================= */
:root[data-theme='light'] {
  /* 1. 极净白底色 */
  --bems-bg-base: #f8fafc;

  /* 2. 高透白玻璃面板 */
  --bems-glass-heavy: rgba(255, 255, 255, 0.85);
  --bems-glass-card: rgba(255, 255, 255, 0.65);
  --bems-glass-header: rgba(241, 245, 249, 0.85);
  --bems-glass-input: rgba(248, 250, 252, 0.8);

  /* 3. 极淡的黑色边框与纯白高光 */
  --bems-border-base: rgba(0, 0, 0, 0.06);
  --bems-rim-light: rgba(255, 255, 255, 0.9);

  /* 4. 清爽的蓝灰色投影 (绝不能用纯黑阴影) */
  --bems-shadow-ambient: 0 12px 40px rgba(148, 163, 184, 0.15);
  --bems-shadow-float: 0 24px 60px rgba(148, 163, 184, 0.25);

  /* 5. 交互暗化反馈 */
  --bems-hover-overlay: rgba(0, 0, 0, 0.03);
  --bems-active-overlay: rgba(59, 130, 246, 0.1);

  /* 6. 高反差藏青色排版 */
  --bems-text-primary: #0f172a;
  --bems-text-regular: #475569;
  --bems-text-secondary: #64748b;

  /* 7. 品牌色在浅色下保持不变，依然亮眼 */
}

/* --- 全局物理环境 --- */
body,
html {
  margin: 0;
  padding: 0;
  background-color: var(--bems-bg-base);
  color: var(--bems-text-primary);
  font-family: 'Inter', -apple-system, system-ui, sans-serif;
  overflow: hidden;
}

.app-container {
  display: flex;
  width: 100vw;
  height: 100vh;
  position: relative;
  overflow: hidden;
}

/* 注入微米级噪点，还原真实物理磨砂感 */
.app-container::after {
  content: "";
  position: fixed;
  inset: 0;
  z-index: 10000;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.7' numOctaves='3'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  opacity: 0.015;
  pointer-events: none;
}

/* 装饰背景 */
.bg-decoration {
  position: absolute;
  inset: 0;
  z-index: -1;
  background: var(--bems-bg-base);
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
  background: var(--bems-color-primary);
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

/* 🚀 侧边栏：修复布局间隙与材质 */
.sidebar-wrapper {
  width: 240px;
  flex-shrink: 0;
  /* 防止被挤压 */
  background: var(--bems-glass-heavy);
  backdrop-filter: blur(32px) saturate(160%);
  border-right: 1px solid var(--bems-border-base);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
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
  overflow: hidden;
  white-space: nowrap;
}

.logo-text {
  font-size: 15px;
  font-weight: 700;
  color: var(--bems-text-primary);
  letter-spacing: 0.5px;
}

/* 侧边菜单 */
.side-menu {
  border-right: none !important;
  flex: 1;
}

.el-menu-item {
  height: 48px !important;
  margin: 8px 16px;
  border-radius: 12px;
  color: var(--bems-text-secondary) !important;
  transition: all 0.2s;
}

.el-menu-item:hover {
  background: var(--bems-hover-overlay) !important;
  color: var(--bems-text-primary) !important;
}

.el-menu-item.is-active {
  background: var(--bems-active-overlay) !important;
  color: var(--bems-color-primary) !important;
  box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.2);
}

/* 🚀 侧边栏：折叠状态选中框与图标绝对居中修复 */
.el-menu--collapse .el-menu-item {
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
  padding: 0 !important;
  /* 核心：将原本的 16px 左右边距改为 auto，确保在 72px 宽度内绝对居中 */
  margin: 8px auto !important;
  width: 48px !important;
  height: 48px !important;
  border-radius: 12px !important;
  /* 核心：消除 Element Plus 默认行高导致的下移 */
  line-height: 1 !important;
}

/* 🚀 强制图标在折叠状态下不产生偏移 */
.el-menu--collapse .el-menu-item .el-icon {
  margin: 0 !important;
  font-size: 20px;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
}

/* 修复折叠后鼠标悬停时，Tooltip 触发器导致的布局偏移 */
.el-menu--collapse .el-tooltip__trigger {
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
  padding: 0 !important;
  height: 48px !important;
  width: 48px !important;
}

.collapse-trigger {
  height: 48px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  border-top: 1px solid var(--bems-border-base);
  color: var(--bems-text-secondary);
  transition: all 0.2s;
}

.collapse-trigger:hover {
  color: var(--bems-text-primary);
  background: var(--bems-hover-overlay);
}

/* 🚀 右侧主内容：min-width: 0 彻底解决留白问题 */
.main-container {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  position: relative;
}

.top-header {
  height: 64px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--bems-border-base);
  background: var(--bems-glass-header);
  backdrop-filter: blur(12px);
  flex-shrink: 0;
}

.breadcrumb-current {
  font-weight: 600;
  font-size: 15px;
  color: var(--bems-text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
  font-size: 13px;
  color: var(--bems-text-secondary);
}

.system-time {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  background: #18181b;
  border: 1px solid var(--bems-border-base);
  color: var(--bems-text-primary);
}

.content-view {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  box-sizing: border-box;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: all 0.2s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 🚀 头像下拉菜单交互样式 */
.avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  /* 鼠标变成小手 */
  outline: none;
  /* 去除点击时的默认黑框 */
}

.avatar-arrow {
  color: var(--bems-text-secondary);
  font-size: 14px;
  transition: all 0.3s;
}

.avatar-wrapper:hover .avatar-arrow {
  color: var(--bems-text-primary);
}
</style>