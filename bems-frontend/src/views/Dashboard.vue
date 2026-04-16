<template>
  <div class="dashboard-container">

    <div class="global-control bento-panel">
      <div class="control-left" style="width: 100%;">

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

          <el-switch v-model="enablePrediction" active-text="⚡ 启动 AI 预测推演" class="ai-predict-switch"
            @change="fetchDataAndRender" style="margin-left: 20px;" />
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

          <div class="inline-status">
            当前态势：<span class="data-date">{{ currentDataDate }}</span>
          </div>

        </div>
      </div>
    </div>

    <div class="kpi-section">
      <div v-for="(kpi, index) in kpiData" :key="index" class="bento-card kpi-card">
        <div class="kpi-title">
          {{ kpi.title }}

          <el-popover v-if="kpi.title === '全局冷量综合 COP'" placement="bottom-start" width="280" trigger="hover"
            :teleported="true" popper-class="cyber-popover">
            <template #reference>
              <el-icon class="help-icon">
                <QuestionFilled />
              </el-icon>
            </template>
            <div class="cop-explainer">
              <div class="explainer-title">📊 COP 实时推算公式</div>
              <div class="explainer-formula">
                <span class="hl-green">{{ kpiData[1].value }}</span> kWh (产出冷量) <br />
                ÷ <span class="hl-cyan">{{ kpiData[0].value }}</span> kWh (消耗电量) <br />
                = <span class="hl-purple">{{ kpi.value }}</span> (综合能效)
              </div>
              <div class="explainer-text">
                💡 <b>通俗理解：</b><br />
                空调相当于“热量搬运工”。当前消耗 1 块钱的电费，能为您搬走相当于 <b class="hl-purple">{{ kpi.value }}</b> 块钱的热量。<br />
                <div style="margin-top: 5px;">
                  状态：<span :class="kpi.value >= 3.5 ? 'hl-green' : 'hl-orange'">
                    {{ kpi.value >= 3.5 ? '高效节能 (极度省钱) 🌟' : '效率一般 (存在优化空间) ⚠️' }}
                  </span>
                </div>
              </div>
            </div>
          </el-popover>

        </div>
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
          <el-tag v-if="enablePrediction" size="small" effect="dark" type="warning"
            style="background: rgba(115, 89, 255, 0.2); border:1px solid #8b5cf6; color: #8b5cf6;">
            Weka 随机森林在线推理中
          </el-tag>
        </div>
        <div ref="lineChartRef" class="chart-box"></div>
      </div>

      <div class="bento-card alarm-card">
        <div class="card-header">
          <span class="header-title">🚨 设备运行异常侦测清单</span>
          <span class="live-indicator"><span class="dot"></span> 实时监控中</span>
        </div>
        <transition-group name="list-anim" tag="div" class="alarm-list">
          <div v-for="alarm in alarmList" :key="alarm.id" class="alarm-item">
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
        </transition-group>
      </div>

      <div class="bento-card chart-card pie-chart-area">
        <div class="card-header">
          <span class="header-title">🌐 基于当前节点的园区全域能耗推演</span>
          <el-tag size="small" effect="dark" type="warning"
            style="background: rgba(250, 173, 20, 0.2); border:none; margin-left:10px;">
            <el-icon style="margin-right:2px">
              <Cpu />
            </el-icon> AI 算法拓扑
          </el-tag>
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
import { ArrowLeft, ArrowRight, Top, Bottom, Warning, Cpu, QuestionFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const callBemsAi = inject('callBemsAi')

const currentBuilding = ref('Panther_office_Karla')
const selectedDate = ref('2016-01-01')
const currentDataDate = ref('加载中...')
const timeUnit = ref('day')
const selectedParams = ref(['electricity', 'chilledwater'])

// 🚀 新增：预测开关状态
const enablePrediction = ref(false)

const prevText = computed(() => timeUnit.value === 'day' ? '前一天' : timeUnit.value === 'week' ? '前一周' : '前一月')
const nextText = computed(() => timeUnit.value === 'day' ? '后一天' : timeUnit.value === 'week' ? '后一周' : '后一月')

const calendarMap = ref({})
let maxElecOfDay = 1

const lineChartRef = ref(null)
const pieChartRef = ref(null)
const radarChartRef = ref(null)
let lineChart = null
let pieChart = null
let radarChart = null

let projectedParkElecCache = 1;

const kpiData = reactive([
  { title: '区间总耗电量', value: '0', unit: 'kWh', color: '#3b82f6', glowClass: 'cyan-glow', trend: 'up', desc: '基于动态区间测算' },
  { title: '区间总冷负荷', value: '0', unit: 'kWh', color: '#8b5cf6', glowClass: 'purple-glow', trend: 'up', desc: '冷水当量转换值' },
  { title: '全局冷量综合 COP', value: '0.00', unit: 'W/W', color: '#10b981', glowClass: 'green-glow', trend: 'up', desc: '宏观制冷能效评估' },
  { title: '活跃异常告警', value: '0', unit: '次', color: '#FF4D4F', glowClass: 'red-glow', trend: 'alert', desc: '亟需人工辅助诊断' }
])

const alarmList = ref([
  { id: 'init_1', level: 'critical', time: '10分钟前', device: '1号冷水机组', description: '蒸发器冷媒低压报警，且压缩机排气温度异常升高，疑似冷媒泄露。' },
  { id: 'init_2', level: 'warning', time: '15分钟前', device: '13层开放办公区', description: '当前室内温度(28℃)超出设定死区，但VAV末端风阀未响应开度指令。' },
  { id: 'init_3', level: 'critical', time: '30分钟前', device: '楼宇总供水管网', description: '主干管网压力突降，疑似发生严重漏水或水泵气蚀。' },
  { id: 'init_4', level: 'offline', time: '1小时前', device: 'A座智能电表网关', description: '网关心跳包丢失，部分楼层电耗数据正在进行AI插值拟合。' },
  { id: 'init_5', level: 'warning', time: '2小时前', device: '高区变频冷却水泵', description: '电机振动频谱出现异常高频谐波，预测轴承寿命将在15天内耗尽。' },
  { id: 'init_6', level: 'critical', time: '3小时前', device: '2号主变压器', description: '三相电流不平衡度连续10分钟超过安全阈值(15%)，存在发热隐患。' },
  { id: 'init_7', level: 'offline', time: '4小时前', device: 'B座楼顶微型气象站', description: '数据流中断超30分钟，已自动平滑切换至云端第三方气象API。' },
  { id: 'init_8', level: 'warning', time: '5小时前', device: 'C座非工作时间能耗', description: '凌晨02:00-04:00期间存在不明基础电耗激增，疑似未执行夜间休眠。' }
]);

let alarmFeedTimer = null;
const mockAlarmPool = [
  { level: 'critical', device: '1号冷水机组', desc: '蒸发器冷媒低压报警，且压缩机排气温度异常升高，疑似冷媒泄露或电子膨胀阀卡滞。' },
  { level: 'warning', device: '13层开放办公区', desc: '当前室内温度(28℃)超出设定死区，但VAV末端风阀未响应开度指令。' },
  { level: 'critical', device: '数据中心机房', desc: '精密空调回风温度告警，机房局部热点突现，有宕机风险！' },
  { level: 'warning', device: '南区空气源热泵机组', desc: '外机盘管风阻急剧增大，AI算法判定为严重结霜，且机组自带的自动化霜程序未能成功启动。' },
  { level: 'critical', device: '楼宇总供水管网', desc: '主干管网压力突降，疑似发生严重漏水或水泵气蚀。' },
  { level: 'warning', device: '高区变频冷却水泵', desc: '水泵电机振动频谱出现异常高频谐波，基于声学诊断模型，预测轴承寿命将在15天内耗尽（建议预防性更换）。' },
  { level: 'critical', device: '2号主变压器', desc: '三相电流不平衡度连续10分钟超过安全阈值(15%)，存在零线过载发热及电气火灾隐患！' },
  { level: 'warning', device: '大堂光照与照明联动', desc: '室外自然采光照度充足(>1500lx)，但大堂核心区照明回路未触发自动降功率节能控制。' },
  { level: 'offline', device: 'A座智能电表网关', desc: '网关心跳包丢失，部分楼层电耗数据正在进行AI插值拟合。' },
  { level: 'offline', device: 'B座楼顶微型气象站', desc: '太阳辐射与温湿度数据流中断超30分钟，系统已自动平滑切换至云端第三方气象API作为群控参考边界。' },
  { level: 'warning', device: '地下车库 CO2 传感器', desc: 'CO2浓度持续超标，但联动排风机未能正常启动，检测到控制下发链路响应超时。' },
  { level: 'warning', device: 'C座非工作时间能耗', desc: '系统检测到凌晨 02:00-04:00 期间存在不明基础电耗激增，疑似部分大功率设备未执行夜间休眠策略。' }
];

const generateLiveAlarm = () => {
  const randomAlarm = mockAlarmPool[Math.floor(Math.random() * mockAlarmPool.length)];
  const newAlarm = { id: 'live_' + Date.now(), level: randomAlarm.level, time: '刚刚', device: randomAlarm.device, description: randomAlarm.desc };
  alarmList.value.unshift(newAlarm);
  if (alarmList.value.length > 15) alarmList.value.pop();
};

const handleDiagnose = (alarm) => {
  const promptText = `系统监测到异常：【${alarm.device}】${alarm.description}请结合内置设备运维手册和暖通规范，帮我分析可能导致该故障的3个主要原因，并给出排查步骤。`
  if (callBemsAi) callBemsAi(promptText)
  else ElMessage.warning('未找到 AI 唤醒接口，请确保 App.vue 中已注入 callBemsAi')
}

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

const calculateSubEnergy = (totalElec, totalWater, type) => {
  let hvacElec = totalWater / 3.8;
  if (hvacElec > totalElec * 0.6) hvacElec = totalElec * 0.6;
  if (hvacElec <= 0 || isNaN(hvacElec)) hvacElec = totalElec * 0.4;

  const remainElec = totalElec - hvacElec;

  if (type === 'education') {
    return [
      { name: '空调系统', value: Math.round(hvacElec), itemStyle: { color: 'rgba(0, 240, 255, 0.8)' } },
      { name: '照明插座', value: Math.round(remainElec * 0.65), itemStyle: { color: 'rgba(0, 240, 255, 0.5)' } },
      { name: '动力设备', value: Math.round(remainElec * 0.35), itemStyle: { color: 'rgba(0, 240, 255, 0.3)' } }
    ];
  } else if (type === 'office') {
    return [
      { name: '空调系统', value: Math.round(hvacElec), itemStyle: { color: 'rgba(115, 89, 255, 0.8)' } },
      { name: '照明插座', value: Math.round(remainElec * 0.55), itemStyle: { color: 'rgba(115, 89, 255, 0.5)' } },
      { name: '数据机房', value: Math.round(remainElec * 0.20), itemStyle: { color: 'rgba(115, 89, 255, 0.3)' } },
      { name: '动力设备', value: Math.round(remainElec * 0.25), itemStyle: { color: 'rgba(115, 89, 255, 0.3)' } }
    ];
  } else if (type === 'service') {
    return [
      { name: '动力水泵', value: Math.round(hvacElec * 0.8), itemStyle: { color: 'rgba(0, 255, 157, 0.8)' } },
      { name: '公共照明', value: Math.round(remainElec * 0.70), itemStyle: { color: 'rgba(0, 255, 157, 0.5)' } },
      { name: '其他用电', value: Math.round(remainElec * 0.30), itemStyle: { color: 'rgba(0, 255, 157, 0.3)' } }
    ];
  } else {
    return [
      { name: '空调制热', value: Math.round(hvacElec), itemStyle: { color: 'rgba(255, 184, 0, 0.8)' } },
      { name: '生活热水', value: Math.round(remainElec * 0.60), itemStyle: { color: 'rgba(255, 184, 0, 0.5)' } },
      { name: '独立电表', value: Math.round(remainElec * 0.40), itemStyle: { color: 'rgba(255, 184, 0, 0.3)' } }
    ];
  }
}

const initPieChart = () => {
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: {
      trigger: 'item', backgroundColor: 'rgba(11, 9, 26, 0.85)', textStyle: { color: '#fff' }, borderColor: '#2A2946',
      formatter: (params) => {
        const total = projectedParkElecCache > 0 ? projectedParkElecCache : 1;
        const percent = ((params.value / total) * 100).toFixed(1);
        return `<div style="color:${params.color};font-weight:bold;">${params.name}</div>
                <div style="margin-top:4px;">推演预测值: ${params.value} kWh</div>
                <div>全园推演占比: ${percent}%</div>`;
      }
    },
    series: {
      type: 'sunburst', radius: ['20%', '70%'], center: ['50%', '50%'],
      itemStyle: { borderRadius: 4, borderColor: '#0b091a', borderWidth: 2 },
      levels: [{}, { label: { position: 'inner', fontWeight: 'bold', fontSize: 13, color: '#000' } }, { label: { position: 'outside', padding: 3, silent: false, color: '#a0a2b8', fontSize: 11 } }],
      data: []
    }
  })
}

const updateSunburstChart = (baseElec, baseWater) => {
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)
  const parkElec = baseElec * 4.5;
  const parkWater = baseWater * 4.5;
  projectedParkElecCache = parkElec;

  const colors = { education: '#3b82f6', office: '#8b5cf6', service: '#10b981', dorm: '#FFB800' }

  pieChart.setOption({
    series: [{
      animationType: 'scale', animationDuration: 1500, animationEasing: 'cubicOut',
      data: [
        { name: '教育类', itemStyle: { color: colors.education }, children: calculateSubEnergy(parkElec * 0.35, parkWater * 0.35, 'education') },
        { name: '办公类', itemStyle: { color: colors.office }, children: calculateSubEnergy(parkElec * 0.30, parkWater * 0.30, 'office') },
        { name: '公共服务', itemStyle: { color: colors.service }, children: calculateSubEnergy(parkElec * 0.20, parkWater * 0.20, 'service') },
        { name: '宿舍类', itemStyle: { color: colors.dorm }, children: calculateSubEnergy(parkElec * 0.15, parkWater * 0.15, 'dorm') }
      ]
    }]
  })
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
  let color = '#3b82f6'
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

// 🚀 核心预测获取与渲染逻辑重构
// 🚀 核心预测获取与渲染逻辑重构 (冷电双控版)
const fetchDataAndRender = async () => {
  try {
    if (selectedParams.value.length === 0) {
      ElMessage.warning('请至少保留一条监测曲线')
      if (lineChart) lineChart.clear()
      return
    }

    // 1. 获取历史真实数据
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

    // 基础轴数据
    const xData = [];
    const elecData = [];
    const waterData = [];
    const abnormalPoints = [];

    // 预测用数据通道 (电)
    const predElecData = [];
    const lowerBoundData = [];
    const upperBoundDiffData = [];

    // 🚀 预测用数据通道 (冷)
    const predWaterData = [];
    const lowerBoundWaterData = [];
    const upperBoundDiffWaterData = [];

    // 填充历史数据
    records.forEach((item, index) => {
      totalElec += (item.electricity || 0)
      totalWater += (item.chilledwater || 0)

      xData.push(item.timeLabel)
      elecData.push(item.electricity || 0)
      waterData.push(item.chilledwater || 0)

      // 为了在图表中留出未来空间，同步给预测数组补空位
      predElecData.push(null)
      lowerBoundData.push(null)
      upperBoundDiffData.push(null)

      predWaterData.push(null)
      lowerBoundWaterData.push(null)
      upperBoundDiffWaterData.push(null)

      if (item.status && item.status !== 'normal') {
        abnormalCount++
        const isCritical = item.status === 'critical_abnormal'
        abnormalPoints.push({
          name: '异常', coord: [index, item.electricity || 0], value: isCritical ? '严重' : '告警',
          itemStyle: { color: isCritical ? '#FF4D4F' : '#FAAD14' },
          customData: {
            time: item.timeLabel, val: item.electricity, level: isCritical ? '严重能耗突变' : '轻度能耗告警',
            building: currentBuilding.value, date: selectedDate.value
          }
        })
      }
    })

    // 2. 如果开启预测，调用后端 Weka 服务
    if (enablePrediction.value && timeUnit.value === 'day') {
      try {
        const predRes = await axios.get(`http://localhost:8080/api/energy/predict?buildingId=${currentBuilding.value}&targetDate=${selectedDate.value}`);
        const predRecords = predRes.data;

        // 🌟 魔法步骤：缝合历史与未来，避免图表断线
        if (elecData.length > 0 && predRecords.length > 0) {
          // 缝合电
          const lastRealElec = elecData[elecData.length - 1];
          predElecData[predElecData.length - 1] = lastRealElec;
          lowerBoundData[lowerBoundData.length - 1] = lastRealElec;
          upperBoundDiffData[upperBoundDiffData.length - 1] = 0;

          // 🚀 缝合冷
          const lastRealWater = waterData[waterData.length - 1];
          predWaterData[predWaterData.length - 1] = lastRealWater;
          lowerBoundWaterData[lowerBoundWaterData.length - 1] = lastRealWater;
          upperBoundDiffWaterData[upperBoundDiffWaterData.length - 1] = 0;
        }

        // 追加未来 24 小时的数据
        predRecords.forEach(item => {
          xData.push(item.timeLabel);
          elecData.push(null); // 历史线停止生长
          waterData.push(null);

          // 电量预测
          predElecData.push(parseFloat(item.predictedElec));
          const lowerElec = parseFloat(item.lowerBound);
          const upperElec = parseFloat(item.upperBound);
          lowerBoundData.push(lowerElec);
          upperBoundDiffData.push(upperElec - lowerElec);

          // 🚀 冷量预测
          predWaterData.push(parseFloat(item.predictedWater));
          const lowerWater = parseFloat(item.waterLowerBound);
          const upperWater = parseFloat(item.waterUpperBound);
          lowerBoundWaterData.push(lowerWater);
          upperBoundDiffWaterData.push(upperWater - lowerWater);
        });

      } catch (err) {
        console.error("AI 预测获取失败", err);
        ElMessage.warning('Weka 模型拉取失败，请检查后端运行状态');
      }
    }

    kpiData[0].value = totalElec.toFixed(1)
    kpiData[1].value = totalWater.toFixed(1)
    kpiData[2].value = totalElec > 0 ? (totalWater / totalElec).toFixed(2) : '0.00'
    kpiData[3].value = abnormalCount.toString()

    if (!lineChart) {
      lineChart = echarts.init(lineChartRef.value)
      lineChart.on('click', (params) => {
        if (params.componentType === 'markPoint' && params.data.customData) {
          const pd = params.data.customData;
          const promptText = `【BEMS 智能诊断请求】\n目标节点：${pd.building}\n发生日期：${pd.date}\n异常时段：${pd.time}\n异常现象：系统判定为【${pd.level}】，该时段总电耗达 ${pd.val} kWh。\n\n请作为高级暖通与建筑能源专家，结合 RAG 知识库，分析导致此能耗突变的 3 个最可能原因并给出针对性排查步骤。`;
          if (callBemsAi) callBemsAi(promptText);
        }
      });
    }

    const legendData = []
    const yAxisConfig = []
    const seriesConfig = []

    // ---- 组装电耗系列 ----
    if (selectedParams.value.includes('electricity')) {
      legendData.push('耗电量 (实测)')
      if (enablePrediction.value) legendData.push('耗电量 (AI预测)')

      yAxisConfig.push({
        type: 'value', name: '电耗', position: 'left', nameTextStyle: { color: '#3b82f6' },
        splitLine: { lineStyle: { color: '#1f1d36', type: 'dashed' } }, axisLabel: { color: '#3b82f6' }
      })

      // 实体线：历史数据
      seriesConfig.push({
        name: '耗电量 (实测)', type: 'line', yAxisIndex: yAxisConfig.length - 1, smooth: true, data: elecData,
        lineStyle: { color: '#3b82f6', width: 3 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(0,240,255,0.4)' }, { offset: 1, color: 'rgba(0,240,255,0)' }]) },
        markPoint: { symbol: 'pin', symbolSize: 50, label: { color: '#fff', fontSize: 10 }, data: abnormalPoints },
        markLine: enablePrediction.value ? {
          silent: true, data: [{ xAxis: xData[records.length - 1], label: { formatter: '现在', color: '#8b5cf6' } }],
          lineStyle: { color: '#8b5cf6', type: 'dashed', width: 2 }
        } : null,
        animationDuration: 1500
      })

      // 虚线与阴影：AI预测数据
      if (enablePrediction.value) {
        seriesConfig.push({
          name: '耗电量 (AI预测)', type: 'line', yAxisIndex: yAxisConfig.length - 1, smooth: true, data: predElecData,
          lineStyle: { color: '#8b5cf6', width: 3, type: 'dashed' }, symbol: 'none'
        });
        seriesConfig.push({
          name: '电量下限支撑', type: 'line', yAxisIndex: yAxisConfig.length - 1, smooth: true, data: lowerBoundData,
          lineStyle: { opacity: 0 }, stack: 'confidence-band-elec', symbol: 'none'
        });
        seriesConfig.push({
          name: '电耗置信区间', type: 'line', yAxisIndex: yAxisConfig.length - 1, smooth: true, data: upperBoundDiffData,
          lineStyle: { opacity: 0 }, stack: 'confidence-band-elec',
          areaStyle: { color: 'rgba(115, 89, 255, 0.2)' }, symbol: 'none'
        });
      }
    }

    // ---- 🚀 组装冷负荷系列 ----
    if (selectedParams.value.includes('chilledwater')) {
      legendData.push('冷负荷 (实测)')
      if (enablePrediction.value) legendData.push('冷负荷 (AI预测)')

      yAxisConfig.push({
        type: 'value', name: '冷量', position: 'right', nameTextStyle: { color: '#10b981' },
        splitLine: { show: false }, axisLabel: { color: '#10b981' }
      })

      // 实线
      seriesConfig.push({
        name: '冷负荷 (实测)', type: 'line', yAxisIndex: yAxisConfig.length - 1, smooth: true, data: waterData,
        lineStyle: { color: '#10b981', width: 2 }
      })

      // 🚀 虚线与阴影
      if (enablePrediction.value) {
        seriesConfig.push({
          name: '冷负荷 (AI预测)', type: 'line', yAxisIndex: yAxisConfig.length - 1, smooth: true, data: predWaterData,
          lineStyle: { color: '#10b981', width: 2, type: 'dashed' }, symbol: 'none'
        });
        seriesConfig.push({
          name: '冷量下限支撑', type: 'line', yAxisIndex: yAxisConfig.length - 1, smooth: true, data: lowerBoundWaterData,
          lineStyle: { opacity: 0 }, stack: 'confidence-band-water', symbol: 'none'
        });
        seriesConfig.push({
          name: '冷负荷置信区间', type: 'line', yAxisIndex: yAxisConfig.length - 1, smooth: true, data: upperBoundDiffWaterData,
          lineStyle: { opacity: 0 }, stack: 'confidence-band-water',
          areaStyle: { color: 'rgba(0, 255, 157, 0.2)' }, symbol: 'none'
        });
      }
    }

    lineChart.setOption({
      tooltip: {
        trigger: 'axis', backgroundColor: 'rgba(11, 9, 26, 0.8)', textStyle: { color: '#fff' }, borderColor: '#2A2946',
        formatter: function (params) {
          let html = `${params[0].axisValue}<br/>`;
          params.forEach(p => {
            // 过滤掉所有用于画图的隐形支撑线
            if (!p.seriesName.includes('支撑') && !p.seriesName.includes('置信区间') && p.value !== undefined && p.value !== null) {
              html += `${p.marker} ${p.seriesName}: <b>${parseFloat(p.value).toFixed(1)}</b><br/>`;
            }
          });
          return html;
        }
      },
      legend: { data: legendData, textStyle: { color: '#a0a2b8' }, icon: 'circle', top: '0%', right: '5%' },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '18%', containLabel: true },
      xAxis: { type: 'category', data: xData, axisLabel: { color: '#a0a2b8' } },
      yAxis: yAxisConfig,
      series: seriesConfig
    }, true)

    updateRadarChart(parseFloat(kpiData[2].value))
    updateSunburstChart(totalElec, totalWater)

  } catch (error) {
    console.error('渲染数据失败', error)
  }
}

const updateRadarChart = (baseCop) => {
  if (!radarChart) radarChart = echarts.init(radarChartRef.value)
  const effectiveCop = baseCop > 0 ? baseCop : 4.0;

  const currentValues = [
    (effectiveCop * 1.2).toFixed(1), (effectiveCop * 0.7).toFixed(1), 1.5,
    (effectiveCop * 1.05).toFixed(1), (effectiveCop * 0.4).toFixed(1)
  ]

  radarChart.setOption({
    color: ['#3b82f6', '#10b981'],
    tooltip: { trigger: 'item', backgroundColor: 'rgba(11, 9, 26, 0.8)', textStyle: { color: '#fff' }, borderColor: '#2A2946' },
    legend: { data: ['当前实时 COP', '设计基准 COP'], bottom: 0, textStyle: { color: '#a0a2b8' } },
    radar: {
      indicator: [
        { name: '冷水机组', max: 6.5 }, { name: '冷冻水泵', max: 3.5 },
        { name: '冷却水泵', max: 3.5 }, { name: '冷却塔', max: 5.0 }, { name: '末端风机', max: 2.5 }
      ],
      splitArea: { show: false }, axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.1)' } },
      splitLine: { lineStyle: { color: 'rgba(0, 240, 255, 0.2)' } },
      axisName: { color: '#e0e2f5', fontSize: 12, padding: [3, 5] },
      center: ['50%', '45%'], radius: '65%'
    },
    series: [{
      name: '能效比对比', type: 'radar', animationType: 'scale', animationDuration: 2500, animationEasing: 'elasticOut',
      data: [
        { value: currentValues, name: '当前实时 COP', areaStyle: { color: 'rgba(0, 240, 255, 0.3)' }, lineStyle: { width: 2, shadowBlur: 10, shadowColor: '#3b82f6' } },
        { value: [5.5, 3.0, 2.8, 4.5, 2.0], name: '设计基准 COP', areaStyle: { color: 'rgba(0, 255, 157, 0.1)' }, lineStyle: { type: 'dashed', width: 2 } }
      ]
    }]
  })
}

const handleResize = () => {
  lineChart?.resize()
  pieChart?.resize()
  radarChart?.resize()
}

onMounted(() => {
  initPieChart()
  fetchCalendarAndRender()
  window.addEventListener('resize', handleResize)
  alarmFeedTimer = setInterval(generateLiveAlarm, 12000)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (alarmFeedTimer) clearInterval(alarmFeedTimer)
  lineChart?.dispose()
  pieChart?.dispose()
  radarChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 40px;
  box-sizing: border-box;
  padding-top: 0 !important;
}

/* 高级便当盒卡片 */
/* 🚀 核心：模拟玻璃厚度的卡片设计 */
.bento-card,
.bento-panel {
  background: var(--glass-bg);
  backdrop-filter: blur(28px) saturate(180%);
  -webkit-backdrop-filter: blur(28px) saturate(180%);
  border: 1px solid var(--glass-border);
  border-radius: 20px;
  /* 维持大圆角的高级感 */
  padding: 32px;
  /* 加大填充，确保文字远离圆角边缘 */

  /* 模拟物理厚度：顶部微弱白线，底部深色投影 */
  box-shadow:
    inset 0 1px 0 var(--glass-rim),
    0 12px 40px rgba(0, 0, 0, 0.4);

  transition: all 0.4s cubic-bezier(0.23, 1, 0.32, 1);
}

.bento-card:hover {
  background: rgba(30, 30, 35, 0.5);
  transform: translateY(-2px);
  border-color: rgba(255, 255, 255, 0.2);
}

/* 悬浮岛吸顶栏 */
/* 悬浮中控台：像一块沉重的磨砂玻璃横梁 */
/* 悬浮中控台：全平齐且无缝 */
.global-control {
  position: sticky;
  top: -30px;
  z-index: 2000;
  margin: -30px -20px 24px -20px;
  width: calc(100% + 40px);
  padding: 20px 40px;
  background: rgba(9, 9, 11, 0.8) !important;
  backdrop-filter: blur(40px) brightness(1.1);
  border-bottom: 1px solid var(--glass-rim);
  border-radius: 0 0 24px 24px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
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
  color: var(--text-muted);
  margin-right: 10px;
  font-weight: 500;
  font-size: 13px;
}

.building-select {
  width: 220px;
}

.inline-status {
  display: flex;
  align-items: center;
  color: var(--text-muted);
  font-size: 13px;
  margin-left: 20px;
  padding-left: 20px;
  border-left: 1px solid var(--border-color);
}

.data-date {
  color: var(--text-main);
  font-weight: 600;
  font-size: 14px;
  margin-left: 5px;
}

/* 控件覆写 */
:deep(.el-radio-button__inner) {
  background-color: transparent;
  border-color: var(--border-color);
  color: var(--text-muted);
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #27272a;
  color: var(--text-main);
  border-color: #3f3f46;
  box-shadow: -1px 0 0 0 #3f3f46;
}

:deep(.cyber-checkbox-group .el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: var(--accent-primary);
  border-color: var(--accent-primary);
}

:deep(.cyber-checkbox-group .el-checkbox__input.is-checked + .el-checkbox__label) {
  color: var(--text-main);
  font-weight: 500;
  text-shadow: none;
}

:deep(.cyber-checkbox-group .el-checkbox) {
  margin-right: 15px;
}

:deep(.ai-predict-switch .el-switch__core) {
  border-color: var(--border-color);
  background-color: #27272a;
}

:deep(.ai-predict-switch.is-checked .el-switch__core) {
  background-color: var(--accent-primary);
  border-color: var(--accent-primary);
}

:deep(.ai-predict-switch .el-switch__label) {
  color: var(--text-muted);
}

:deep(.ai-predict-switch.is-checked .el-switch__label) {
  color: var(--accent-primary);
  font-weight: 600;
  text-shadow: none;
}

.date-navigator :deep(.el-button) {
  background-color: transparent;
  border-color: var(--border-color);
  color: var(--text-muted);
}

.date-navigator :deep(.el-button:hover) {
  color: var(--text-main);
  border-color: #52525b;
  background: rgba(255, 255, 255, 0.05);
}

.date-picker-custom {
  width: 140px;
  margin: 0;
}

/* 日期选择器与下拉框：深度定制为嵌入式质感 */
/* 控件外观：完全契合黑曜石风格 */
:deep(.el-input__wrapper) {
  background: rgba(0, 0, 0, 0.3) !important;
  border: 1px solid var(--glass-border) !important;
  box-shadow: none !important;
  border-radius: 10px;
}

:deep(.el-input__inner) {
  color: #fff !important;
  font-weight: 500;
}

/* 日历热力图 */
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
  font-weight: 500;
  font-size: 13px;
}

.energy-bar {
  position: absolute;
  bottom: 4px;
  height: 3px;
  border-radius: 2px;
  transition: all 0.3s ease;
}

.is-disabled .cell-text {
  color: #52525b;
}

/* KPI 卡片 */
.kpi-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.kpi-card {
  position: relative;
  overflow: hidden;
  /* 裁剪流光 */
  background: rgba(15, 15, 20, 0.4);
  /* 🚀 增加一个极细的内发光边缘，模拟高定质感 */
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.05),
    0 20px 50px rgba(0, 0, 0, 0.3);
}

/* 🚀 动态流光边框特效（仅在重点卡片上使用） */
.kpi-card::after {
  content: "";
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: conic-gradient(transparent,
      rgba(59, 130, 246, 0.1),
      transparent 20%);
  animation: rotate-glow 8s linear infinite;
  pointer-events: none;
}

@keyframes rotate-glow {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

.kpi-title {
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  margin-bottom: 16px;
}

/* 状态数值：极简大气 */
.kpi-value {
  font-size: 42px;
  font-weight: 700;
  color: #fff;
  font-family: 'Inter', -apple-system, sans-serif;
  letter-spacing: -2px;
  margin-bottom: 4px;
}

.kpi-value .unit {
  font-size: 14px;
  color: var(--text-muted);
  font-weight: 400;
  letter-spacing: 0;
}

/* 取消所有的霓虹发光，替换为高级质感色 */
.cyan-glow {
  color: var(--text-main);
  text-shadow: none;
}

.purple-glow {
  color: var(--text-main);
  text-shadow: none;
}

.green-glow {
  color: var(--text-main);
  text-shadow: none;
}

.red-glow {
  color: var(--text-main);
  text-shadow: none;
}

.kpi-trend {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--text-muted);
}

.trend-up {
  color: var(--accent-green);
}

.trend-down {
  color: var(--accent-primary);
}

.trend-alert {
  color: #ef4444;
}

/* 图表区域 */
.main-grid {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  grid-template-rows: 400px 350px;
  gap: 24px;
  flex: 1;
  min-height: 0;
}

.chart-card {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-title {
  color: var(--text-main);
  font-size: 15px;
  font-weight: 600;
}

.chart-box {
  flex: 1;
  width: 100%;
  min-height: 250px;
}

.line-chart-area {
  grid-column: 1 / 2;
  grid-row: 1 / 2;
}

.alarm-card {
  grid-column: 2 / 3;
  grid-row: 1 / 2;
  padding: 20px;
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

/* 告警列表精修 */
.alarm-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  flex: 1;
  padding-right: 8px;
}

.alarm-list::-webkit-scrollbar {
  width: 4px;
}

.alarm-list::-webkit-scrollbar-thumb {
  background: #3f3f46;
  border-radius: 2px;
}

/* 告警项：磨砂条效果 */
.alarm-item {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 14px;
  padding: 16px 20px;
  margin-bottom: 12px;
  transition: all 0.3s ease;
}

.alarm-item:hover {
  background: rgba(255, 255, 255, 0.05);
  transform: scale(1.01);
}

.alarm-status {
  width: 12px;
  display: flex;
  justify-content: center;
}

.status-light {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.critical {
  background-color: #ef4444;
  box-shadow: 0 0 8px rgba(239, 68, 68, 0.4);
}

.warning {
  background-color: #f59e0b;
}

.offline {
  background-color: #71717a;
}

.alarm-info {
  flex: 1;
}

.alarm-time {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.alarm-desc {
  font-size: 13px;
  color: var(--text-main);
  line-height: 1.5;
}

.cyber-btn-small {
  background: #27272a;
  border: 1px solid var(--border-color);
  color: var(--text-main);
  font-size: 12px;
  border-radius: 6px;
  padding: 6px 12px;
  transition: all 0.2s;
}

.cyber-btn-small:hover {
  background: #3f3f46;
  border-color: #52525b;
  cursor: pointer;
}

/* 动画和指示器 */
.list-anim-move,
.list-anim-enter-active {
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
}

.list-anim-leave-active {
  transition: all 0.3s ease;
  position: absolute;
  width: 100%;
}

.list-anim-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.list-anim-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.live-indicator {
  font-size: 12px;
  color: var(--accent-green);
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(16, 185, 129, 0.1);
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 500;
}

.live-indicator .dot {
  width: 6px;
  height: 6px;
  background-color: var(--accent-green);
  border-radius: 50%;
  animation: blink-dot 2s infinite;
}

@keyframes blink-dot {

  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0.4;
  }
}

/* COP 科普悬浮 */
.help-icon {
  margin-left: 6px;
  cursor: pointer;
  color: var(--text-muted);
  transition: color 0.2s;
}

.help-icon:hover {
  color: var(--text-main);
}
</style>

<style>
/* 全局弹窗高级定制 */
.el-popper.cyber-popover {
  background: rgba(18, 18, 22, 0.8) !important;
  backdrop-filter: blur(24px) !important;
  border: 1px solid var(--glass-rim) !important;
  border-radius: 16px !important;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.6) !important;
  padding: 20px !important;
}

.el-popper.cyber-popover .el-popper__arrow::before {
  background: #18181b !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
}

.cyber-popover .explainer-title {
  color: #fafafa !important;
  font-weight: 600;
  margin-bottom: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  padding-bottom: 8px;
  font-size: 14px;
}

.cyber-popover .explainer-formula {
  background: rgba(0, 0, 0, 0.3) !important;
  color: #e4e4e7 !important;
  border-radius: 6px;
  margin: 12px 0;
  padding: 12px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.cyber-popover .hl-green {
  color: #10b981;
  font-weight: 600;
}

.cyber-popover .hl-cyan {
  color: #3b82f6;
  font-weight: 600;
}

.cyber-popover .hl-purple {
  color: #a855f7;
  font-weight: 600;
}

.cyber-popover .hl-orange {
  color: #f59e0b;
  font-weight: 600;
}

.cyber-popover .explainer-text {
  color: #a1a1aa;
  font-size: 12px;
  line-height: 1.6;
}
</style>