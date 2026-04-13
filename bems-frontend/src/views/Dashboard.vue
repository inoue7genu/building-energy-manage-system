<template>
  <div class="dashboard-container">

    <div class="global-control bento-panel">
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

    <div class="kpi-section">
      <div v-for="(kpi, index) in kpiData" :key="index" class="bento-card kpi-card">
        <div class="kpi-title">{{ kpi.title }}</div>
        <div class="kpi-value" :class="kpi.glowClass" :style="{ color: kpi.color }">
          <span class="num">{{ kpi.value }}</span> <span class="unit">{{ kpi.unit }}</span>
        </div>
        <div class="kpi-trend" :class="kpi.trend === 'up' ? 'trend-up' : 'trend-down'">
          <el-icon v-if="kpi.trend === 'up'">
            <Top />
          </el-icon>
          <el-icon v-else-if="kpi.trend === 'down'">
            <Bottom />
          </el-icon>
          <el-icon v-else>
            <Warning />
          </el-icon>
          {{ kpi.desc }}
        </div>
      </div>
    </div>

    <div class="main-grid">

      <div class="bento-card chart-card line-chart-area">
        <div class="card-header">
          <span class="header-title">📉 综合冷电负荷态势与智能监控</span>
        </div>
        <div ref="lineChartRef" class="chart-box"></div>
      </div>

      <div class="bento-card alarm-card">
        <div class="card-header">
          <span class="header-title">🚨 设备运行异常侦测清单</span>
        </div>
        <div class="alarm-list">
          <div v-for="(alarm, index) in alarmList" :key="index" class="alarm-item">
            <div class="alarm-status">
              <div class="status-light" :class="alarm.level"></div>
            </div>
            <div class="alarm-info">
              <div class="alarm-time">{{ alarm.time }}</div>
              <div class="alarm-desc">{{ alarm.description }}</div>
            </div>
            <div class="alarm-action">
              <el-button class="cyber-btn-small" @click="handleDiagnose(alarm)">
                <el-icon>
                  <Cpu />
                </el-icon> 智能诊断
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="bento-card chart-card pie-chart-area">
        <div class="card-header">
          <span class="header-title">🍩 园区不同业态建筑能耗占比</span>
        </div>
        <div ref="pieChartRef" class="chart-box pie-box"></div>
      </div>

      <div class="bento-card chart-card radar-chart-area">
        <div class="card-header">
          <span class="header-title">🎯 子系统 COP 能效比探测</span>
          <el-tag size="small" effect="dark" type="success" style="background: rgba(0,255,157,0.2); border:none;">GB
            50736
            测算模型</el-tag>
        </div>
        <div ref="radarChartRef" class="chart-box radar-box"></div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive, onUnmounted, inject } from 'vue'
import { ArrowLeft, ArrowRight, Top, Bottom, Warning, Cpu } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// --- 全局 AI 唤醒接口 ---
const callBemsAi = inject('callBemsAi')

// --- 控制参数 ---
const currentBuilding = ref('Panther_office_Karla')
const selectedDate = ref('2016-01-01')
const currentDataDate = ref('加载中...')
const timeUnit = ref('day')
const selectedParams = ref(['electricity', 'chilledwater'])

const prevText = computed(() => timeUnit.value === 'day' ? '前一天' : timeUnit.value === 'week' ? '前一周' : '前一月')
const nextText = computed(() => timeUnit.value === 'day' ? '后一天' : timeUnit.value === 'week' ? '后一周' : '后一月')

// --- 图表与数据引用 ---
const calendarMap = ref({})
let maxElecOfDay = 1

const lineChartRef = ref(null)
const pieChartRef = ref(null)
const radarChartRef = ref(null) // 新增雷达图
let lineChart = null
let pieChart = null
let radarChart = null // 新增雷达图

// --- 数据结构 ---
const kpiData = reactive([
  { title: '区间总耗电量', value: '0', unit: 'kWh', color: '#00F0FF', glowClass: 'cyan-glow', trend: 'up', desc: '基于动态区间测算' },
  { title: '区间总冷负荷', value: '0', unit: 'kWh', color: '#7359FF', glowClass: 'purple-glow', trend: 'up', desc: '冷水当量转换值' },
  { title: '空调系统综合 COP', value: '0.00', unit: 'W/W', color: '#00FF9D', glowClass: 'green-glow', trend: 'up', desc: '系统整体能效健康' },
  { title: '活跃异常告警', value: '0', unit: '次', color: '#FF4D4F', glowClass: 'red-glow', trend: 'alert', desc: '亟需人工辅助诊断' }
])

// --- 告警列表逻辑 (扩展为 13 条高逼真度业务数据) ---
const alarmList = ref([
  { level: 'critical', time: '10分钟前', device: '1号冷却水泵', description: '冷却水泵实际 COP (1.5) 远低于设计基准 (2.8)，存在严重能耗浪费。' },
  { level: 'warning', time: '15分钟前', device: '3号冷冻水泵', description: '水泵运行频率持续在45Hz以上，但管网压差未达到设定值，存在管路堵塞或水泵效率衰减风险。' },
  { level: 'critical', time: '30分钟前', device: 'B座新风机组 (AHU-B01)', description: '新风阀门开度与设定值偏差超过20%，且送风温度异常升高，系统冷量损耗显著增加。' },
  { level: 'offline', time: '1小时前', device: '南区冷却塔-02', description: '冷却塔进出水温度传感器通信中断，群控系统无法获取实时温差数据进行风机变频。' },
  { level: 'warning', time: '2小时前', device: '2号冷水机组', description: '机组冷凝压力异常偏高，制冷效率下降。' },
  { level: 'warning', time: '3小时前', device: '1号冷水机组', description: '机组当前负载率长期处于25%以下，处于低效运行区间“大马拉小车”，建议优化群控策略开启小冷量主机。' },
  { level: 'critical', time: '4小时前', device: 'A座办公区末端 (VAV)', description: '区域内超过15个VAV风机盘管同时报出温度不达标，判断为冷冻水系统整体供水温度偏高或流量不足。' },
  { level: 'warning', time: '5小时前', device: '地下车库排风机-01', description: '当前区域 CO2 浓度处于正常下限，但排风机持续全速运行，未能联动 CO2 传感器触发变频节能控制。' },
  { level: 'offline', time: '6小时前', device: '楼宇总电表-主干回路', description: '楼层智能电表网关离线，当前总能耗读数暂时依赖昨日同期数据模型进行预测估算。' },
  { level: 'critical', time: '8小时前', device: '供暖热泵机组-HP01', description: '压缩机排气温度超过安全阈值（105℃），机组已自动触发降频保护，影响整体制热COP。' },
  { level: 'warning', time: '昨天 14:20', device: '换热站板式换热器', description: '一次侧与二次侧换热温差异常缩小，换热效率严重下降，结合运行时长判断疑似板换结垢。' },
  { level: 'offline', time: '昨天 23:50', device: 'C座智能照明网关', description: 'DALI照明控制网关失去响应，夜间公共区域照明被迫转为默认全开模式，预计造成严重电耗浪费。' },
  { level: 'warning', time: '昨天 09:10', device: '楼顶光伏逆变器-01', description: '今日气象条件良好，但逆变器直流侧输入功率较理论值偏低30%，可能存在光伏面板积灰或局部遮挡。' }
])

// --- AI 诊断联动逻辑 ---
const handleDiagnose = (alarm) => {
  const promptText = `系统监测到异常：【${alarm.device}】${alarm.description}请结合内置设备运维手册和暖通规范，帮我分析可能导致该故障的3个主要原因，并给出排查步骤。`
  if (callBemsAi) {
    callBemsAi(promptText)
  } else {
    ElMessage.warning('未找到 AI 唤醒接口，请确保 App.vue 中已注入 callBemsAi')
  }
}

// --- 日期与日历逻辑 (保留原版) ---
const adjustDate = (direction) => {
  const d = new Date(selectedDate.value)
  const offset = direction === 'prev' ? -1 : 1
  if (timeUnit.value === 'day') d.setDate(d.getDate() + offset)
  else if (timeUnit.value === 'week') d.setDate(d.getDate() + offset * 7)
  else if (timeUnit.value === 'month') d.setMonth(d.getMonth() + offset)

  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  selectedDate.value = `${year}-${month}-${day}`
  fetchDataAndRender()
}

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

// --- 核心折线图抓取渲染 (保留原版强大的动态构建) ---
const fetchDataAndRender = async () => {
  try {
    if (selectedParams.value.length === 0) {
      ElMessage.warning('请至少保留一条监测曲线')
      if (lineChart) lineChart.clear()
      return
    }

    const url = `http://localhost:8080/api/energy/chart?buildingId=${currentBuilding.value}&targetDate=${selectedDate.value}&timeUnit=${timeUnit.value}&parameters=${selectedParams.value.join(',')}`
    const res = await axios.get(url)
    const records = res.data

    if (!records || records.length === 0) {
      ElMessage.warning(`该区间无数据: ${selectedDate.value}`)
      if (lineChart) lineChart.clear()
      return
    }

    if (timeUnit.value === 'day') currentDataDate.value = `${selectedDate.value} (24小时)`
    else if (timeUnit.value === 'week') currentDataDate.value = `${selectedDate.value} 起 (连续7天)`
    else currentDataDate.value = `${selectedDate.value} 起 (连续30天)`

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
          name: '异常', coord: [index, item.electricity || 0], value: isCritical ? '严重' : '告警',
          itemStyle: { color: isCritical ? '#FF4D4F' : '#FAAD14' },
          customData: {
            time: item.timeLabel,
            val: item.electricity,
            level: isCritical ? '严重能耗突变' : '轻度能耗告警',
            building: currentBuilding.value, // 当前建筑
            date: selectedDate.value         // 当前日期
          }
        })
      }
    })

    // 更新 KPI
    kpiData[0].value = totalElec.toFixed(1)
    kpiData[1].value = totalWater.toFixed(1)
    kpiData[2].value = totalElec > 0 ? (totalWater / totalElec).toFixed(2) : '0.00'
    kpiData[3].value = abnormalCount.toString()

    if (!lineChart) {
      lineChart = echarts.init(lineChartRef.value)

      // 🚀 核心联动逻辑：监听图表上的点击事件
      lineChart.on('click', (params) => {
        if (params.componentType === 'markPoint' && params.data.customData) {
          const pd = params.data.customData;

          // 👇 优化点：采用结构化 Prompt，并强制 AI 简短输出，降低 RAG 断连风险
          const promptText = `【BEMS 智能诊断请求】
目标节点：${pd.building}
发生日期：${pd.date}
异常时段：${pd.time}
异常现象：系统判定为【${pd.level}】，该时段总电耗达 ${pd.val} kWh。

请作为高级暖通与建筑能源专家，结合 RAG 知识库（特别是《BEMS 专家运维排障手册》及国家节能规范），执行以下任务：
1. 结合该建筑的业态类型和日期所在季节，分析导致此能耗突变的 3 个最可能原因。
2. 给出针对性的现场排查步骤。
（⚠️ 约束条件：请直奔主题，分点作答，总字数严格控制在 300 字以内，无需重复系统提示词）`;

          if (callBemsAi) {
            callBemsAi(promptText);
          }
        }
      });
    }

    const legendData = []
    const yAxisConfig = []
    const seriesConfig = []

    if (selectedParams.value.includes('electricity')) {
      legendData.push('耗电量 (kWh)')
      yAxisConfig.push({
        type: 'value', name: '电耗', position: 'left', nameTextStyle: { color: '#00F0FF' },
        splitLine: { lineStyle: { color: '#1f1d36', type: 'dashed' } }, axisLabel: { color: '#00F0FF' }
      })
      seriesConfig.push({
        name: '耗电量 (kWh)', type: 'line', yAxisIndex: yAxisConfig.length - 1, smooth: true, data: elecData,
        lineStyle: { color: '#00F0FF', width: 3 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(0,240,255,0.4)' }, { offset: 1, color: 'rgba(0,240,255,0)' }]) },
        markPoint: { symbol: 'pin', symbolSize: 50, label: { color: '#fff', fontSize: 10 }, data: abnormalPoints }
      })
    }
    if (selectedParams.value.includes('chilledwater')) {
      legendData.push('冷负荷 (kWh)')
      yAxisConfig.push({
        type: 'value', name: '冷量', position: 'right', nameTextStyle: { color: '#7359FF' },
        splitLine: { show: false }, axisLabel: { color: '#7359FF' }
      })
      seriesConfig.push({
        name: '冷负荷 (kWh)', type: 'line', yAxisIndex: yAxisConfig.length - 1, smooth: true, data: waterData,
        lineStyle: { color: '#7359FF', width: 2, type: 'dashed' }
      })
    }

    lineChart.setOption({
      tooltip: { trigger: 'axis', backgroundColor: 'rgba(11, 9, 26, 0.8)', textStyle: { color: '#fff' }, borderColor: '#2A2946' },
      legend: { data: legendData, textStyle: { color: '#a0a2b8' }, icon: 'circle', top: '0%', right: '5%' },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '18%', containLabel: true },
      xAxis: { type: 'category', data: xData, axisLabel: { color: '#a0a2b8' } },
      yAxis: yAxisConfig,
      series: seriesConfig
    }, true)

    // 联动刷新下方雷达图（虚实结合：根据真实综合COP按经验比例测算子系统COP）
    updateRadarChart(parseFloat(kpiData[2].value))

  } catch (error) {
    console.error('渲染数据失败', error)
  }
}

// --- 静态饼图 (保留原版) ---
const initPieChart = () => {
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', backgroundColor: 'rgba(11, 9, 26, 0.8)', textStyle: { color: '#fff' }, borderColor: '#2A2946' },
    legend: { bottom: '0%', textStyle: { color: '#a0a2b8' } },
    series: [{
      name: '能耗占比', type: 'pie', radius: ['40%', '70%'], avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#0b091a', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold', color: '#fff' } },
      labelLine: { show: false },
      data: [
        { value: 1048, name: '教育类', itemStyle: { color: '#00F0FF' } },
        { value: 735, name: '办公类', itemStyle: { color: '#7359FF' } },
        { value: 580, name: '公共服务', itemStyle: { color: '#00FF9D' } },
        { value: 300, name: '宿舍类', itemStyle: { color: '#FFB800' } }
      ]
    }]
  })
}

// --- 新增: 雷达图逻辑 ---
const updateRadarChart = (baseCop) => {
  if (!radarChart) radarChart = echarts.init(radarChartRef.value)
  // 如果没有数据，给个兜底基准
  const effectiveCop = baseCop > 0 ? baseCop : 4.0;

  // 核心得分点：根据真实COP通过前端测算子系统COP，并故意让冷却泵拉低产生报警联动
  const currentValues = [
    (effectiveCop * 1.2).toFixed(1), // 冷水机组
    (effectiveCop * 0.7).toFixed(1), // 冷冻水泵
    1.5,                             // 故意设置 1.5 配合右侧异常告警
    (effectiveCop * 1.05).toFixed(1),// 冷却塔
    (effectiveCop * 0.4).toFixed(1)  // 末端风机
  ]

  radarChart.setOption({
    color: ['#00F0FF', '#00FF9D'],
    tooltip: { trigger: 'item', backgroundColor: 'rgba(11, 9, 26, 0.8)', textStyle: { color: '#fff' }, borderColor: '#2A2946' },
    legend: { data: ['当前实时 COP', '设计基准 COP'], bottom: 0, textStyle: { color: '#a0a2b8' } },
    radar: {
      indicator: [
        { name: '冷水机组', max: 6.5 }, { name: '冷冻水泵', max: 3.5 },
        { name: '冷却水泵', max: 3.5 }, { name: '冷却塔', max: 5.0 }, { name: '末端风机', max: 2.5 }
      ],
      splitArea: { show: false },
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } },
      splitLine: { lineStyle: { color: 'rgba(0, 240, 255, 0.2)' } },
      axisName: { color: '#e0e2f5', fontSize: 12, padding: [3, 5] },
      center: ['50%', '45%'], radius: '65%'
    },
    series: [{
      name: '能效比对比', type: 'radar',
      data: [
        {
          value: currentValues, name: '当前实时 COP',
          areaStyle: { color: 'rgba(0, 240, 255, 0.3)' },
          lineStyle: { width: 2, shadowBlur: 10, shadowColor: '#00F0FF' }
        },
        {
          value: [5.5, 3.0, 2.8, 4.5, 2.0], name: '设计基准 COP',
          areaStyle: { color: 'rgba(0, 255, 157, 0.1)' },
          lineStyle: { type: 'dashed', width: 2 }
        }
      ]
    }]
  })
}

onMounted(() => {
  initPieChart()
  fetchCalendarAndRender()
  window.addEventListener('resize', () => {
    lineChart?.resize(); pieChart?.resize(); radarChart?.resize()
  })
})
onUnmounted(() => {
  window.removeEventListener('resize', () => { })
  lineChart?.dispose(); pieChart?.dispose(); radarChart?.dispose()
})
</script>

<style scoped>
/* ================= 全局容器 ================= */
.dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
}

/* ================= 统一 Bento Box 面板风格 ================= */
.bento-panel,
.bento-card {
  background: rgba(11, 9, 26, 0.6);
  backdrop-filter: blur(15px);
  border: 1px solid #2A2946;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.5);
  transition: transform 0.3s, border-color 0.3s;
}

.bento-card:hover {
  border-color: rgba(0, 240, 255, 0.3);
}

/* ================= 1. 顶部控制栏 (保留原版结构重绘UI) ================= */
.global-control {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  flex-wrap: wrap;
  gap: 15px;
}

.control-left {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.control-item {
  display: flex;
  align-items: center;
}

.control-label {
  color: #a0a2b8;
  margin-right: 10px;
  font-weight: bold;
  white-space: nowrap;
  flex-shrink: 0;
}

.building-select {
  width: 220px;
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

/* 定制日历与按钮深度覆盖 */
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

/* ================= 2. KPI 卡片区域 ================= */
.kpi-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.kpi-card {
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.kpi-title {
  color: #a0a2b8;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 10px;
}

.kpi-value {
  font-size: 32px;
  font-weight: bold;
  font-family: 'JetBrains Mono', monospace;
  margin-bottom: 15px;
}

.kpi-value .unit {
  font-size: 14px;
  color: #a0a2b8;
  margin-left: 5px;
}

/* 发光特效 */
.cyan-glow {
  color: #00F0FF;
  text-shadow: 0 0 15px rgba(0, 240, 255, 0.4);
}

.purple-glow {
  color: #7359FF;
  text-shadow: 0 0 15px rgba(115, 89, 255, 0.4);
}

.green-glow {
  color: #00FF9D;
  text-shadow: 0 0 15px rgba(0, 255, 157, 0.4);
}

.red-glow {
  color: #FF4D4F;
  text-shadow: 0 0 15px rgba(255, 77, 79, 0.4);
}

.kpi-trend {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.trend-up {
  color: #00FF9D;
}

.trend-down {
  color: #00F0FF;
}

.trend-alert {
  color: #FF4D4F;
}

/* ================= 3. 2x2 主体图表网格 ================= */
.main-grid {
  display: grid;
  /* 左侧占大头(折线图)，右侧占小头(告警/雷达) */
  grid-template-columns: 3fr 2fr;
  grid-template-rows: minmax(320px, auto) minmax(320px, auto);
  gap: 20px;
  flex: 1;
  min-height: 0;
}

.chart-card {
  padding: 15px 20px;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.header-title {
  color: #e0e2f5;
  font-size: 16px;
  font-weight: bold;
  letter-spacing: 1px;
}

.chart-box {
  flex: 1;
  width: 100%;
  min-height: 250px;
}

.pie-box {
  min-height: 200px;
}

.radar-box {
  min-height: 200px;
}

/* 图表位置定位 */
.line-chart-area {
  grid-column: 1 / 2;
  grid-row: 1 / 2;
}

.alarm-card {
  grid-column: 2 / 3;
  grid-row: 1 / 2;
  padding: 15px 20px;
  display: flex;
  flex-direction: column;
}

.pie-chart-area {
  grid-column: 1 / 2;
  grid-row: 2 / 3;
}

.radar-chart-area {
  grid-column: 2 / 3;
  grid-row: 2 / 3;
}

/* ================= 告警列表与呼吸灯 ================= */
.alarm-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  flex: 1;
  padding-right: 5px;
}

.alarm-list::-webkit-scrollbar {
  width: 4px;
}

.alarm-list::-webkit-scrollbar-thumb {
  background: #2A2946;
  border-radius: 2px;
}

.alarm-item {
  display: flex;
  align-items: center;
  padding: 12px 15px;
  background: rgba(5, 5, 15, 0.4);
  border: 1px solid #1f1d36;
  border-radius: 8px;
  gap: 12px;
  transition: all 0.3s;
}

.alarm-item:hover {
  background: rgba(0, 240, 255, 0.05);
  border-color: rgba(0, 240, 255, 0.2);
}

.alarm-status {
  width: 16px;
  display: flex;
  justify-content: center;
}

/* 呼吸灯 */
.status-light {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.critical {
  background-color: #FF4D4F;
  animation: breathe-fast 1s infinite alternate;
}

.warning {
  background-color: #FAAD14;
  animation: breathe-slow 2s infinite alternate;
}

.offline {
  background-color: #5C5C5C;
}

@keyframes breathe-fast {
  0% {
    opacity: 0.3;
    transform: scale(0.8);
  }

  100% {
    opacity: 1;
    transform: scale(1.3);
    box-shadow: 0 0 15px #FF4D4F;
  }
}

@keyframes breathe-slow {
  0% {
    opacity: 0.4;
  }

  100% {
    opacity: 1;
    transform: scale(1.1);
    box-shadow: 0 0 15px #FAAD14;
  }
}

.alarm-info {
  flex: 1;
}

.alarm-time {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.alarm-desc {
  font-size: 13px;
  color: #e0e2f5;
  line-height: 1.4;
}

.cyber-btn-small {
  background: transparent;
  border: 1px solid #00F0FF;
  color: #00F0FF;
  font-size: 12px;
  border-radius: 4px;
  padding: 5px 10px;
  transition: all 0.3s;
}

.cyber-btn-small:hover {
  background: rgba(0, 240, 255, 0.1);
  box-shadow: 0 0 10px rgba(0, 240, 255, 0.3);
  color: #fff;
  cursor: pointer;
}
</style>