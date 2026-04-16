<template>
    <div class="login-wrapper">
        <div class="login-card">
            <div class="login-header">
                <el-icon :size="40" color="var(--bems-color-primary)">
                    <Monitor />
                </el-icon>
                <h1>BEMS 运营中枢</h1>
                <p>Building Energy Management System</p>
            </div>

            <el-form :model="loginForm" class="login-form" autocomplete="off">
                <el-form-item>
                    <el-input v-model="loginForm.username" placeholder="请输入账号" :prefix-icon="User" autocomplete="off"
                        name="bems_username" />
                </el-form-item>
                <el-form-item>
                    <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" :prefix-icon="Lock"
                        show-password autocomplete="new-password" name="bems_password" />
                </el-form-item>
                <el-button type="primary" class="login-btn" @click="handleLogin" :loading="loading">
                    进入系统
                </el-button>
            </el-form>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Monitor, User, Lock } from '@element-plus/icons-vue' // 确保引入了图标
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const loginForm = ref({ username: '', password: '' })

// 🚀 核心新增：每次进入登录页时，强行清空变量，确保干干净净
onMounted(() => {
    loginForm.value = {
        username: '',
        password: ''
    }
})

const handleLogin = () => {
    if (loginForm.value.username === 'admin' && loginForm.value.password === '123456') {
        loading.value = true
        localStorage.setItem('bems-token', 'mock-token-123456')
        ElMessage.success('欢迎回来，管理员')
        setTimeout(() => {
            router.push('/dashboard')
        }, 800)
    } else {
        ElMessage.error('账号或密码错误')
    }
}
</script>

<style scoped>
/* 🚀 核心修复：去掉了多余的背景层，利用绝对定位和极高 z-index 确保卡片可点击 */
.login-wrapper {
    width: 100vw;
    height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    position: absolute;
    top: 0;
    left: 0;
    z-index: 9999;
}

.login-card {
    width: 400px;
    padding: 40px;
    border-radius: 24px;
    background: var(--bems-glass-heavy);
    backdrop-filter: blur(40px);
    border: 1px solid var(--bems-border-base);
    box-shadow: var(--bems-shadow-float);
    text-align: center;
}

.login-header h1 {
    color: var(--bems-text-primary);
    margin: 20px 0 5px;
    font-size: 24px;
}

.login-header p {
    color: var(--bems-text-secondary);
    font-size: 12px;
    margin-bottom: 30px;
}

.login-form :deep(.el-input__wrapper) {
    background: var(--bems-glass-input) !important;
    box-shadow: none !important;
    border: 1px solid var(--bems-border-base);
    height: 45px;
    border-radius: 12px;
}

.login-btn {
    width: 100%;
    height: 45px;
    border-radius: 12px;
    font-weight: 600;
    margin-top: 10px;
    background: var(--bems-color-primary);
    border: none;
}
</style>