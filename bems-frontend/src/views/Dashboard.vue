<template>
  <div class="dashboard-container">
    <div class="global-control">
      <div class="control-left">

        <div class="control-item">
          <span class="control-label">监控节点</span>
          <el-select v-model="currentBuilding" @change="fetchCalendarAndRender" class="building-select" effect="dark">
            <el-option label="Panther 办公楼 (Karla)" value="Panther_office_Karla" />
            <el-option label="Panther 教育楼 (Vincent)" value="Panther_education_Vincent" />
            <el-option label="Panther 教育楼 (Annetta)" value="Panther_education_Annetta" />
            <el-option label="Panther 教育楼 (Aurora)" value="Panther_education_Aurora" />
            <el-option label="Panther 办公楼 (Graham)" value="Panther_office_Graham" />
          </el-select>
        </div>

        <div class="control-item">
          <span class="control-label">时间粒度</span>
          <el-radio-group v-model="timeUnit" size="default" @change="fetchDataAndRender" class="time-unit-group">
            <el-radio-button value="day">日</el-radio-button>
            <el-radio-button value="week">周</el-radio-button>
            <el-radio-button value="month">月</el-radio-button>
          </el-radio-group>
        </div>

        <div class="control-item">
          <span class="control-label" style="margin-left: 15px;">动态曲线</span>
          <el-checkbox-group v-model="selectedParams" @change="fetchDataAndRender" class="cyber-checkbox-group">
            <el-checkbox value="electricity">耗电态势</el-checkbox>
            <el-checkbox value="chilledwater">冷负荷态势</el-checkbox>
          </el-checkbox-group>
        </div>

        <div class="control-item">
          <span class="control-label">态势起点</span>
          <el-button-group class="date-navigator">
            <el-button @click="adjustDate('prev')">
              <el-icon>
                <ArrowLeft />
              </el-icon> {{ prevText }}
            </el-button>

            <el-date-picker v-model="selectedDate" type="date" placeholder="选择起始日期" format="YYYY/MM/DD"
              value-format="YYYY-MM-DD" @change="fetchDataAndRender" class="date-picker-custom" :clearable="false"
              :disabled-date="disabledDate">
              <template #default="cell">
                <div class="custom-calendar-cell"
                  :class="{ 'is-disabled': disabledDate(cell.date || cell.dayjs?.toDate()) }">
                  <span class="cell-text">{{ cell.text }}</span>
                  <div v-if="hasData(cell)" class="energy-bar" :style="getEnergyBarStyle(cell)"></div>
                </div>
              </template>
            </el-date-picker>

            <el-button @click="adjustDate('next')">
              {{ nextText }} <el-icon>
                <ArrowRight />
              </el-icon>
            </el-button>
          </el-button-group>
        </div>

      </div>

      <div class="control-right">
        当前态势：<span class="data-date">{{ currentDataDate }}</span>
      </div>
    </div>

    <el-row :gutter="20" class="kpi-row">
      <el-col :span="6" v-for="(kpi, index) in kpiData" :key="index">
        <el-card class="kpi-card" shadow="hover">
          <div class="kpi-title">{{ kpi.title }}</div>
          <div class="kpi-value" :style="{ color: kpi.color }">
            {{ kpi.value }} <span class="kpi-unit">{{ kpi.unit }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>📉 综合冷电负荷态势与智能监控</span>
            </div>
          </template>
          <div ref="lineChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>🍩 园区不同业态建筑能耗占比</span>
            </div>
          </template>
          <div ref="pieChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive, onUnmounted } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const currentBuilding = ref('Panther_office_Karla')
const selectedDate = ref('2016-01-01')
const currentDataDate = ref('加载中...')
const timeUnit = ref('day')

const prevText = computed(() => timeUnit.value === 'day' ? '前一天' : timeUnit.value === 'week' ? '前一周' : '前一月')
const nextText = computed(() => timeUnit.value === 'day' ? '后一天' : timeUnit.value === 'week' ? '后一周' : '后一月')

// 💡 定义默认选中的折线图
const selectedParams = ref(['electricity', 'chilledwater'])

const adjustDate = (direction) => {
  const d = new Date(selectedDate.value)
  const offset = direction === 'prev' ? -1 : 1

  if (timeUnit.value === 'day') {
    d.setDate(d.getDate() + offset)
  } else if (timeUnit.value === 'week') {
    d.setDate(d.getDate() + offset * 7)
  } else if (timeUnit.value === 'month') {
    d.setMonth(d.getMonth() + offset)
  }

  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  selectedDate.value = `${year}-${month}-${day}`

  fetchDataAndRender()
}

const calendarMap = ref({})
let maxElecOfDay = 1

const lineChartRef = ref(null)
const pieChartRef = ref(null)
let lineChart = null
let pieChart = null

const kpiData = reactive([
  { title: '区间总耗电量', value: '0', unit: 'kWh', color: '#00F0FF' },
  { title: '区间总冷负荷', value: '0', unit: 'kWh', color: '#7359FF' },
  { title: '综合能效比 (COP)', value: '0', unit: '', color: '#00FF9D' },
  { title: '捕捉异常告警', value: '0', unit: '次', color: '#FF4D4F' }
])

const disabledDate = (time) => {
  if (!time) return true
  const dateStr = new Date(time.getTime() - (time.getTimezoneOffset() * 60000)).toISOString().split('T')[0]
  return !calendarMap.value[dateStr]
}

const getCellDateStr = (cell) => {
  const dateObj = cell.date || (cell.dayjs ? cell.dayjs.toDate() : null)
  if (!dateObj) return null
  return new Date(dateObj.getTime() - (dateObj.getTimezoneOffset() * 60000)).toISOString().split('T')[0]
}

const hasData = (cell) => {
  const dateStr = getCellDateStr(cell)
  return dateStr && !!calendarMap.value[dateStr]
}

const getEnergyBarStyle = (cell) => {
  const dateStr = getCellDateStr(cell)
  const val = calendarMap.value[dateStr]
  if (!val) return {}
  const ratio = val / maxElecOfDay
  let color = '#00F0FF'
  if (ratio > 0.75) color = '#FF4D4F'
  else if (ratio > 0.4) color = '#FAAD14'
  return { width: `${Math.max(ratio * 100, 15)}%`, backgroundColor: color, boxShadow: `0 0 6px ${color}` }
}

const fetchCalendarAndRender = async () => {
  try {
    const res = await axios.get(`http://localhost:8080/api/energy/calendar?buildingId=${currentBuilding.value}`)
    const map = {}
    let max = 0
    let latestDate = ''
    res.data.forEach(item => {
      const dateStr = item.recordDate
      const val = parseFloat(item.totalElec) || 0
      map[dateStr] = val
      if (val > max) max = val
      if (dateStr > latestDate) latestDate = dateStr
    })
    calendarMap.value = map
    maxElecOfDay = max || 1
    if (!calendarMap.value[selectedDate.value]) selectedDate.value = latestDate || '2016-01-01'
    await fetchDataAndRender()
  } catch (error) {
    console.error('获取日历台账失败', error)
  }
}

/* ==========================================
   🚀 核心重构：动态图表构建引擎
========================================== */
const fetchDataAndRender = async () => {
  try {
    // 1. 安全拦截
    if (selectedParams.value.length === 0) {
      ElMessage.warning('请至少保留一条监测曲线')
      if (lineChart) lineChart.clear()
      return
    }

    // 2. 拼接携带参数的 URL
    const url = `http://localhost:8080/api/energy/chart?buildingId=${currentBuilding.value}&targetDate=${selectedDate.value}&timeUnit=${timeUnit.value}&parameters=${selectedParams.value.join(',')}`
    const res = await axios.get(url)
    const records = res.data

    if (!records || records.length === 0) {
      ElMessage.warning(`该区间无数据: ${selectedDate.value}`)
      if (lineChart) lineChart.clear()
      return
    }

    // 更新界面标题
    if (timeUnit.value === 'day') {
      currentDataDate.value = `${selectedDate.value} (24小时)`
    } else if (timeUnit.value === 'week') {
      currentDataDate.value = `${selectedDate.value} 起 (连续7天)`
    } else {
      currentDataDate.value = `${selectedDate.value} 起 (连续30天)`
    }

    // 数据清洗装载
    let totalElec = 0; let totalWater = 0; let abnormalCount = 0;
    const xData = []; const elecData = []; const waterData = []; const abnormalPoints = [];

    records.forEach((item, index) => {
      totalElec += (item.electricity || 0)
      totalWater += (item.chilledwater || 0)
      xData.push(item.timeLabel)
      elecData.push(item.electricity || 0)
      waterData.push(item.chilledwater || 0)

      if (item.status && item.status !== 'normal') {
        abnormalCount++
        const isCritical = item.status === 'critical_abnormal'
        abnormalPoints.push({
          name: '异常', coord: [index, item.electricity || 0],
          value: isCritical ? '严重' : '告警',
          itemStyle: { color: isCritical ? '#FF4D4F' : '#FAAD14' }
        })
      }
    })

    // 刷新 KPI 数值
    kpiData[0].value = totalElec.toFixed(1)
    kpiData[1].value = totalWater.toFixed(1)
    kpiData[2].value = totalElec > 0 ? (totalWater / totalElec).toFixed(2) : '0.00'
    kpiData[3].value = abnormalCount.toString()

    if (!lineChart) lineChart = echarts.init(lineChartRef.value)

    // 3. 🚀 动态拼装 ECharts 坐标轴和数据列
    const legendData = []
    const yAxisConfig = []
    const seriesConfig = []

    if (selectedParams.value.includes('electricity')) {
      legendData.push('耗电量 (kWh)')
      yAxisConfig.push({
        type: 'value', name: '电耗', position: 'left',
        nameTextStyle: { color: '#00F0FF' },
        splitLine: { lineStyle: { color: '#1f1d36', type: 'dashed' } },
        axisLabel: { color: '#00F0FF' }
      })
      seriesConfig.push({
        name: '耗电量 (kWh)', type: 'line',
        yAxisIndex: yAxisConfig.length - 1, // 动态绑定 Y 轴
        smooth: true, data: elecData,
        lineStyle: { color: '#00F0FF', width: 3 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(0,240,255,0.4)' }, { offset: 1, color: 'rgba(0,240,255,0)' }]) },
        markPoint: { symbol: 'pin', symbolSize: 50, label: { color: '#fff', fontSize: 10 }, data: abnormalPoints }
      })
    }

    if (selectedParams.value.includes('chilledwater')) {
      legendData.push('冷负荷 (kWh)')
      yAxisConfig.push({
        type: 'value', name: '冷量', position: 'right',
        nameTextStyle: { color: '#7359FF' },
        splitLine: { show: false },
        axisLabel: { color: '#7359FF' }
      })
      seriesConfig.push({
        name: '冷负荷 (kWh)', type: 'line',
        yAxisIndex: yAxisConfig.length - 1, // 动态绑定 Y 轴
        smooth: true, data: waterData,
        lineStyle: { color: '#7359FF', width: 2, type: 'dashed' }
      })
    }

    // 4. 将动态配置注入实例
    lineChart.setOption({
      tooltip: { trigger: 'axis', backgroundColor: 'rgba(11, 9, 26, 0.8)', textStyle: { color: '#fff' } },
      legend: {
        data: legendData,
        textStyle: { color: '#a0a2b8' },
        icon: 'circle', top: '2%', right: '5%'
      },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '18%', containLabel: true },
      xAxis: { type: 'category', data: xData, axisLabel: { color: '#a0a2b8' } },
      yAxis: yAxisConfig,
      series: seriesConfig
    }, true) // 🚨 注意此处的 true 参数极其关键：它告诉 ECharts 彻底清除旧图层，使用新传入的 series

  } catch (error) {
    console.error('渲染数据失败', error)
  }
}

const initPieChart = () => {
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', backgroundColor: 'rgba(11, 9, 26, 0.8)', textStyle: { color: '#fff' } },
    legend: { bottom: '0%', textStyle: { color: '#a0a2b8' } },
    series: [{
      name: '能耗占比', type: 'pie', radius: ['40%', '70%'], avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#0b091a', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold', color: '#fff' } },
      labelLine: { show: false },
      data: [
        { value: 1048, name: '教育类 (Education)', itemStyle: { color: '#00F0FF' } },
        { value: 735, name: '办公类 (Office)', itemStyle: { color: '#7359FF' } },
        { value: 580, name: '公共服务', itemStyle: { color: '#00FF9D' } },
        { value: 300, name: '宿舍类', itemStyle: { color: '#FFB800' } }
      ]
    }]
  })
}

onMounted(() => {
  initPieChart()
  fetchCalendarAndRender()
  window.addEventListener('resize', () => { lineChart?.resize(); pieChart?.resize() })
})
onUnmounted(() => { window.removeEventListener('resize', () => { }) })
</script>

<style scoped>
/* 赛博风复选框样式补充 */
:deep(.cyber-checkbox-group .el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #00F0FF;
  border-color: #00F0FF;
}

:deep(.cyber-checkbox-group .el-checkbox__input.is-checked + .el-checkbox__label) {
  color: #00F0FF;
  font-weight: bold;
  text-shadow: 0 0 5px rgba(0, 240, 255, 0.4);
}

:deep(.cyber-checkbox-group .el-checkbox) {
  margin-right: 15px;
}

.dashboard-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 🚀 重点优化：控制栏的 Flex 布局防挤压 */
.global-control {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(30, 27, 56, 0.4);
  border: 1px solid #2A2946;
  border-radius: 8px;
  padding: 12px 20px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  /* 允许在极小屏幕换行兜底 */
  gap: 15px;
}

.control-left {
  display: flex;
  align-items: center;
  gap: 20px;
  /* 让每个控制模块间距舒适 */
  flex-wrap: wrap;
}

/* 控制项盒子 */
.control-item {
  display: flex;
  align-items: center;
}

/* 标签强制不换行 */
.control-label {
  color: #a0a2b8;
  margin-right: 10px;
  font-weight: bold;
  white-space: nowrap;
  /* 绝对不换行！ */
  flex-shrink: 0;
}

.building-select {
  width: 220px;
}

/* 定制时间粒度按钮 */
:deep(.el-radio-button__inner) {
  background-color: transparent;
  border-color: #2A2946;
  color: #a0a2b8;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #0b091a;
  color: #00F0FF;
  border-color: #00F0FF;
  box-shadow: -1px 0 0 0 #00F0FF;
}

/* 组合日期步进器样式 */
.date-navigator {
  display: flex;
  align-items: center;
}

.date-navigator :deep(.el-button) {
  background-color: transparent;
  border-color: #2A2946;
  color: #a0a2b8;
}

.date-navigator :deep(.el-button:hover) {
  color: #00F0FF;
  border-color: #00F0FF;
}

.date-picker-custom {
  width: 140px;
  margin: 0;
}

:deep(.el-input__wrapper) {
  background-color: #0b091a !important;
  box-shadow: 0 0 0 1px #2A2946 inset !important;
  border-radius: 0;
}

:deep(.el-input__inner) {
  color: #00F0FF !important;
  font-weight: bold;
  text-align: center;
}

:deep(.el-picker-panel__body) {
  padding: 10px;
}

.custom-calendar-cell {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}

.cell-text {
  z-index: 10;
  font-weight: bold;
}

.energy-bar {
  position: absolute;
  bottom: 2px;
  height: 3px;
  border-radius: 2px;
  transition: all 0.3s ease;
}

.custom-calendar-cell:hover .energy-bar {
  height: 5px;
}

.is-disabled .cell-text {
  color: #4a4a5e;
  font-weight: normal;
}

.control-right {
  color: #a0a2b8;
  font-size: 14px;
  white-space: nowrap;
  flex-shrink: 0;
}

.data-date {
  color: #00FF9D;
  font-weight: bold;
  font-size: 16px;
  margin-left: 5px;
  letter-spacing: 1px;
}

.kpi-row {
  margin-bottom: 20px;
}

.kpi-card {
  background: rgba(30, 27, 56, 0.5);
  border: 1px solid #2A2946;
  border-radius: 8px;
}

.kpi-title {
  font-size: 14px;
  color: #a0a2b8;
  margin-bottom: 10px;
}

.kpi-value {
  font-size: 28px;
  font-weight: bold;
}

.kpi-unit {
  font-size: 12px;
  font-weight: normal;
  color: #a0a2b8;
}

.chart-row {
  flex: 1;
}

.chart-card {
  height: 100%;
  background: rgba(30, 27, 56, 0.5);
  border: 1px solid #2A2946;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}

.chart-card :deep(.el-card__body) {
  flex: 1;
  padding: 10px;
}

.card-header {
  color: #ffffff;
  font-weight: bold;
  font-size: 16px;
  letter-spacing: 1px;
}

.chart-box {
  width: 100%;
  height: 380px;
}
</style>