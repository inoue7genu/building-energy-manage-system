<template>
    <div class="admin-hub-container">
        <div class="global-control bento-panel">
            <div class="control-left">
                <div class="control-item">
                    <span class="control-label">监控维度</span>
                    <div class="dimension-tag">全域看板 (6 个纳管点)</div>
                </div>
                <div class="control-item">
                    <span class="control-label">监控日期</span>
                    <el-date-picker v-model="selectedDate" type="date" placeholder="选择日期" format="YYYY-MM-DD"
                        value-format="YYYY-MM-DD" :clearable="false" @change="handleDateChange"
                        class="cyber-date-picker" size="small" />
                </div>
                <div class="control-item" style="margin-left: 10px;">
                    <el-button plain class="theme-btn" @click="showIndicatorDrawer = true" size="small">
                        <el-icon style="margin-right: 5px;">
                            <Setting />
                        </el-icon> 指标配置中枢
                    </el-button>
                </div>
            </div>
            <div class="header-stats-mini">
                <div class="mini-stat">
                    <span class="ss-label">网点在线率</span>
                    <span class="ss-value active">100.0%</span>
                </div>
                <el-divider direction="vertical" />
                <div class="mini-stat">
                    <span class="ss-label">更新频率</span>
                    <span class="ss-value active">15min</span>
                </div>
            </div>
        </div>

        <div class="campus-grid">
            <div v-for="(item, index) in campusConfigs" :key="index" class="bento-panel chart-panel">
                <div class="panel-header-mini">
                    <div class="panel-title-mini">
                        <span class="dot"
                            :style="{ background: item.color, boxShadow: `0 0 8px ${item.color}` }"></span>
                        {{ item.name }}
                    </div>
                </div>
                <div :ref="(el) => (chartRefs[index] = el)" class="chart-container"></div>
            </div>
        </div>

        <el-drawer v-model="showIndicatorDrawer" title="⚙️ 能源监控指标库" direction="rtl" size="380px" class="theme-drawer">
            <div class="drawer-content">
                <div class="section-title">活跃指标 (Active)</div>
                <div class="indicator-list">
                    <div class="indicator-item" v-for="(ind, idx) in activeIndicators" :key="idx">
                        <div class="ind-info">
                            <span class="ind-color" :style="{ background: ind.color }"></span>
                            <span class="ind-name">{{ ind.name }}</span>
                        </div>
                        <div class="ind-actions">
                            <el-switch v-model="ind.status" size="small" style="margin-right: 12px;" />
                            <el-button type="danger" link @click="handleDeleteIndicator(idx)" :icon="Delete" />
                        </div>
                    </div>
                </div>

                <el-divider border-style="dashed" style="border-color: var(--bems-border-base);" />

                <div class="section-title">挂载能效指标 (Add New)</div>
                <div class="add-indicator-box">
                    <el-select v-model="newIndicator" placeholder="选择建筑能效指标" class="theme-select"
                        style="width: 100%; margin-bottom: 15px;">
                        <el-option label="🏢 单位面积电耗 (kWh/m²)" value="单位面积电耗 (kWh/m²)" />
                        <el-option label="❄️ 暖通空调综合能效 (COP)" value="暖通空调综合能效 (COP)" />
                        <el-option label="💨 建筑碳排放量估算 (kgCO₂)" value="建筑碳排放量估算 (kgCO₂)" />
                        <el-option label="☀️ 光伏绿电产出率 (%)" value="光伏绿电产出率 (%)" />
                        <el-option label="🚰 人均用水强度 (L/人)" value="人均用水强度 (L/人)" />
                    </el-select>
                    <el-button class="theme-btn-primary" style="width: 100%;" @click="handleAddIndicator">
                        <el-icon style="margin-right: 5px;">
                            <Plus />
                        </el-icon> 确认挂载指标
                    </el-button>
                </div>

                <div class="mock-tip">
                    * 提示：添加或移除指标后，系统将向物联网关下发新指令，数据同步预计会有 3-5 分钟延迟。
                </div>
            </div>
        </el-drawer>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
// 🚀 引入图标和消息组件
import { Setting, Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// --- 🚀 新增：指标管理抽屉的状态与逻辑 ---
const showIndicatorDrawer = ref(false)
const newIndicator = ref('')

const activeIndicators = ref([
    { name: '耗电量', color: '#00F0FF', status: true },
    { name: '冷负荷', color: '#9D00FF', status: true }
])

const handleAddIndicator = () => {
    if (!newIndicator.value) return ElMessage.warning('请先选择要添加的指标')
    if (activeIndicators.value.some(item => item.name === newIndicator.value)) {
        return ElMessage.warning('该指标已在监控序列中')
    }
    const colors = ['#00FF9D', '#FFD700', '#FF3366', '#FF8A00']
    const randomColor = colors[Math.floor(Math.random() * colors.length)]

    activeIndicators.value.push({ name: newIndicator.value, color: randomColor, status: true })
    ElMessage.success(`成功挂载 [${newIndicator.value}]`)
    newIndicator.value = ''
}

const handleDeleteIndicator = (index) => {
    const name = activeIndicators.value[index].name
    ElMessageBox.confirm(`确定要从全局监控中移除指标 [${name}] 吗？`, '移除确认', {
        confirmButtonText: '确定移除',
        cancelButtonText: '取消',
        type: 'warning',
    }).then(() => {
        activeIndicators.value.splice(index, 1)
        ElMessage.success('指标已成功移除')
    }).catch(() => { })
}
// ------------------------------------------

// --- 你的原版图表逻辑 (一字未改，确保稳定) ---
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const chartRefs = ref([])
let chartInstances = []

const campusConfigs = [
    { name: 'Panther 教育核心园区', base: 2200, color: '#00F0FF', type: 'EDU' },
    { name: '光谷云计算中心', base: 5000, color: '#00FF9D', type: 'DATA' },
    { name: '望京嘉里办公中心', base: 1800, color: '#FFD700', type: 'OFFICE' },
    { name: '虹桥天地购物中心', base: 8500, color: '#9D00FF', type: 'MALL' },
    { name: '天河国际康复医院', base: 4000, color: '#FF3366', type: 'HOSP' },
    { name: '麓谷工业智造基地', base: 6000, color: '#FF8A00', type: 'INDUS' }
]

const generateDoubleData = (base, dateStr, type) => {
    const seed = dateStr.split('-').reduce((acc, cur) => acc + parseInt(cur), 0)
    const powerData = []
    const loadData = []

    for (let i = 0; i < 24; i++) {
        let factor = 0
        let noise = Math.sin(seed + i * 1.5) * (base * 0.05)

        switch (type) {
            case 'EDU': factor = i >= 8 && i <= 18 ? (i === 12 ? 0.65 : 0.85) : 0.15; break;
            case 'DATA': factor = 0.95; break;
            case 'OFFICE': factor = i >= 9 && i <= 17 ? 0.9 : 0.08; break;
            case 'MALL': factor = i >= 10 && i <= 22 ? (i >= 18 ? 1.0 : 0.7) : 0.1; break;
            case 'HOSP': factor = 0.5 + (i >= 8 && i <= 17 ? 0.4 : 0); break;
            case 'INDUS': factor = i >= 8 && i <= 20 ? 0.9 : 0.1; break;
        }

        const pVal = (base * factor) + noise
        const lVal = pVal * (0.6 + Math.sin(i / 12) * 0.2)

        powerData.push(Math.max(0, pVal.toFixed(1)))
        loadData.push(Math.max(0, lVal.toFixed(1)))
    }
    return { powerData, loadData }
}

const xData = Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`)

const getOption = (data, mainColor) => ({
    legend: {
        show: true,
        right: '2%',
        top: '0%',
        icon: 'circle',
        itemWidth: 8,
        // 🚀 使用 CSS 变量适配深浅色
        textStyle: { color: 'var(--bems-text-secondary)', fontSize: 10 },
        data: ['耗电量', '冷负荷']
    },
    tooltip: {
        trigger: 'axis',
        // 🚀 提示框使用系统玻璃面板颜色，完美适配深浅色
        backgroundColor: 'var(--bems-glass-card)',
        borderColor: 'var(--bems-border-base)',
        textStyle: { color: 'var(--bems-text-primary)' }
    },
    grid: { left: '4%', right: '4%', bottom: '5%', top: '22%', containLabel: true },
    xAxis: {
        type: 'category',
        data: xData,
        axisLabel: { color: 'var(--bems-text-secondary)', fontSize: 9, interval: 5 },
        axisLine: { lineStyle: { color: 'var(--bems-border-base)' } }
    },
    yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: 'var(--bems-border-base)', type: 'dashed' } },
        axisLabel: { show: false }
    },
    series: [
        {
            name: '耗电量',
            data: data.powerData,
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineStyle: { width: 2, color: '#00F0FF' },
            areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    { offset: 0, color: 'rgba(0, 240, 255, 0.2)' },
                    { offset: 1, color: 'transparent' }
                ])
            }
        },
        {
            name: '冷负荷',
            data: data.loadData,
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineStyle: { width: 2, color: '#9D00FF', type: 'dashed' },
            areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    { offset: 0, color: 'rgba(157, 0, 255, 0.1)' },
                    { offset: 1, color: 'transparent' }
                ])
            }
        }
    ]
})

const initAll = () => {
    const date = selectedDate.value

    campusConfigs.forEach((cfg, idx) => {
        const el = chartRefs.value[idx]
        if (el) {
            if (!chartInstances[idx]) {
                chartInstances[idx] = echarts.init(el)
            }
            const data = generateDoubleData(cfg.base, date, cfg.type)
            chartInstances[idx].setOption(getOption(data, cfg.color))
        }
    })
}

const handleDateChange = () => initAll()

onMounted(() => {
    nextTick(() => {
        initAll()
        window.addEventListener('resize', () => chartInstances.forEach(c => c && c.resize()))
    })
})

onUnmounted(() => {
    chartInstances.forEach(c => c && c.dispose())
})
</script>

<style scoped>
.admin-hub-container {
    display: flex;
    flex-direction: column;
    gap: 16px;
    height: 100%;
    overflow-y: auto;
    padding-bottom: 20px;
}

/* --- 🚀 基础面板使用变量，支持主题切换 --- */
.bento-panel {
    background: var(--bems-glass-card);
    border: 1px solid var(--bems-border-base);
    border-radius: 12px;
}

.global-control {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 20px !important;
    flex-shrink: 0;
}

.control-left {
    display: flex;
    gap: 24px;
    align-items: center;
}

.control-item {
    display: flex;
    align-items: center;
    gap: 10px;
}

.control-label {
    color: var(--bems-text-secondary);
    font-size: 13px;
}

.dimension-tag {
    border: 1px solid var(--bems-color-primary);
    color: var(--bems-color-primary);
    padding: 2px 10px;
    border-radius: 4px;
    font-size: 12px;
    background: var(--bems-glass-input);
    /* 适配浅色 */
}

.header-stats-mini {
    display: flex;
    align-items: center;
    gap: 20px;
}

.mini-stat {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
}

.ss-label {
    font-size: 11px;
    color: var(--bems-text-secondary);
}

.ss-value {
    font-size: 15px;
    font-weight: bold;
    color: var(--bems-text-primary);
}

.ss-value.active {
    color: #00FF9D;
}

.campus-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
    gap: 16px;
}

.chart-panel {
    height: 280px;
    display: flex;
    flex-direction: column;
    padding: 15px !important;
}

.panel-header-mini {
    margin-bottom: 10px;
}

.panel-title-mini {
    color: var(--bems-text-primary);
    font-weight: 600;
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 8px;
}

.chart-container {
    flex: 1;
    width: 100%;
}

.dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    display: inline-block;
}

/* --- 🚀 按钮与表单深浅色适配 --- */
.theme-btn {
    background: var(--bems-glass-input) !important;
    border: 1px solid var(--bems-border-base) !important;
    color: var(--bems-text-primary) !important;
}

.theme-btn:hover {
    border-color: var(--bems-color-primary) !important;
    color: var(--bems-color-primary) !important;
}

.theme-btn-primary {
    background: var(--bems-color-primary) !important;
    border: none !important;
    color: #000 !important;
    font-weight: bold;
}

/* --- 🚀 侧边抽屉深浅色主题完美适配 --- */
:deep(.theme-drawer) {
    background: var(--bems-glass-card) !important;
    backdrop-filter: blur(20px);
}

:deep(.theme-drawer .el-drawer__header) {
    margin-bottom: 0;
    padding: 20px;
    color: var(--bems-text-primary);
    border-bottom: 1px solid var(--bems-border-base);
    font-weight: 600;
}

.drawer-content {
    padding: 20px;
}

.section-title {
    color: var(--bems-text-secondary);
    font-size: 13px;
    margin-bottom: 15px;
    text-transform: uppercase;
}

.indicator-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.indicator-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: var(--bems-glass-input);
    padding: 10px 15px;
    border-radius: 8px;
    border: 1px solid var(--bems-border-base);
}

.ind-info {
    display: flex;
    align-items: center;
    gap: 10px;
}

.ind-color {
    width: 10px;
    height: 10px;
    border-radius: 2px;
}

.ind-name {
    color: var(--bems-text-primary);
    font-size: 14px;
}

.ind-actions {
    display: flex;
    align-items: center;
}

.mock-tip {
    margin-top: 30px;
    font-size: 12px;
    color: var(--bems-text-secondary);
    line-height: 1.6;
    padding: 10px;
    background: var(--bems-glass-input);
    border-radius: 8px;
    border: 1px solid var(--bems-border-base);
}

:deep(.el-input__wrapper) {
    background: var(--bems-glass-input) !important;
    box-shadow: none !important;
}

:deep(.el-input__inner) {
    font-size: 13px !important;
    color: var(--bems-text-primary) !important;
}
</style>