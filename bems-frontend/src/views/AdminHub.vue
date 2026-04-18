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
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const chartRefs = ref([])
let chartInstances = []

// 6个园区的配置信息
const campusConfigs = [
    { name: 'Panther 教育核心园区', base: 2200, color: '#00F0FF', type: 'EDU' },
    { name: '光谷云计算中心', base: 5000, color: '#00FF9D', type: 'DATA' },
    { name: '望京嘉里办公中心', base: 1800, color: '#FFD700', type: 'OFFICE' },
    { name: '虹桥天地购物中心', base: 8500, color: '#9D00FF', type: 'MALL' },
    { name: '天河国际康复医院', base: 4000, color: '#FF3366', type: 'HOSP' },
    { name: '麓谷工业智造基地', base: 6000, color: '#FF8A00', type: 'INDUS' }
]

/**
 * 🚀 差异化双曲线数据生成算法
 * 返回：{ powerData: [], loadData: [] }
 */
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
        // 冷负荷通常滞后于用电量，且随气温变化，这里模拟一个滞后相关的波动
        const lVal = pVal * (0.6 + Math.sin(i / 12) * 0.2)

        powerData.push(Math.max(0, pVal.toFixed(1)))
        loadData.push(Math.max(0, lVal.toFixed(1)))
    }
    return { powerData, loadData }
}

const xData = Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2, '0')}:00`)

const getOption = (data, mainColor) => ({
    // 🚀 核心：添加图例
    legend: {
        show: true,
        right: '2%',
        top: '0%',
        icon: 'circle',
        itemWidth: 8,
        textStyle: { color: '#a0a2b8', fontSize: 10 },
        data: ['耗电量', '冷负荷']
    },
    tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(10, 10, 14, 0.9)',
        borderColor: 'rgba(255,255,255,0.1)',
        textStyle: { color: '#fff' }
    },
    grid: { left: '4%', right: '4%', bottom: '5%', top: '22%', containLabel: true },
    xAxis: {
        type: 'category',
        data: xData,
        axisLabel: { color: '#7a7c91', fontSize: 9, interval: 5 },
        axisLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } }
    },
    yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.03)', type: 'dashed' } },
        axisLabel: { show: false }
    },
    series: [
        {
            name: '耗电量',
            data: data.powerData,
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineStyle: { width: 2, color: '#00F0FF' }, // 统一用青色
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
            lineStyle: { width: 2, color: '#9D00FF', type: 'dashed' }, // 统一用紫色虚线区分
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
    background: rgba(0, 240, 255, 0.05);
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

/* 🚀 3列网格排版 */
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

:deep(.el-input__wrapper) {
    background: var(--bems-glass-input) !important;
    box-shadow: none !important;
}

:deep(.el-input__inner) {
    font-size: 13px !important;
    color: var(--bems-text-primary) !important;
}
</style>