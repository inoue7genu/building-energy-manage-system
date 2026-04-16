<template>
  <div class="data-query-container">
    <el-config-provider :locale="zhCn || {}">

      <div class="filter-panel">
        <div class="filter-left">
          <div class="filter-item">
            <span class="filter-label">目标节点</span>
            <el-select v-model="queryParams.buildingId" @change="handleBuildingChange" placeholder="全局监控 (不限)" clearable
              class="custom-select" effect="dark">
              <el-option label="Panther 办公楼 (Karla)" value="Panther_office_Karla" />
              <el-option label="Panther 教育楼 (Vincent)" value="Panther_education_Vincent" />
              <el-option label="Panther 教育楼 (Annetta)" value="Panther_education_Annetta" />
              <el-option label="Panther 教育楼 (Aurora)" value="Panther_education_Aurora" />
              <el-option label="Panther 办公楼 (Graham)" value="Panther_office_Graham" />
            </el-select>
          </div>

          <div class="filter-item">
            <span class="filter-label">时间区间</span>
            <div class="date-range-wrapper">
              <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
                end-placeholder="结束日期" value-format="YYYY-MM-DD" class="custom-date-range" :disabled-date="disabledDate"
                :default-value="[new Date(2016, 6, 1), new Date(2016, 7, 1)]" unlink-panels @change="handleSearch">
                <template #default="cell">
                  <div class="custom-cell" :class="{
                    'is-start': cell?.start,
                    'is-end': cell?.end,
                    'is-in-range': cell?.inRange,
                    'is-disabled-visual': !hasData(cell)
                  }">
                    <span class="cell-text cyber-cell-text" :data-text="cell?.text">
                      {{ cell?.text !== undefined ? cell.text : '--' }}
                    </span>
                    <div v-if="hasData(cell)" class="energy-bar" :style="getEnergyBarStyle(cell)"></div>
                  </div>
                </template>
              </el-date-picker>

              <el-button @click="resetDateRange" class="cyber-btn reset-btn" title="清空时间区间">
                <el-icon>
                  <RefreshLeft />
                </el-icon>
              </el-button>
            </div>
          </div>

          <div class="filter-item">
            <span class="filter-label">监测指标</span>
            <el-checkbox-group v-model="selectedParams" class="cyber-checkbox-group" @change="handleSearch">
              <el-checkbox v-for="param in availableParams" :key="param.value" :value="param.value"> {{ param.label }}
              </el-checkbox>
            </el-checkbox-group>
          </div>

          <el-button type="primary" class="cyber-btn search-btn" @click="handleSearch">
            <el-icon>
              <Search />
            </el-icon> 精准检索
          </el-button>
        </div>

        <div class="filter-right" style="display: flex; gap: 10px;">
          <el-upload action="http://localhost:8080/api/energy/upload" :show-file-list="false"
            :on-success="handleUploadSuccess" :on-error="handleUploadError" accept=".xlsx, .xls">
            <el-button type="primary" class="cyber-btn upload-btn" title="批量导入历史 .xlsx 数据">
              <el-icon>
                <Upload />
              </el-icon> 数据融合 (导入)
            </el-button>
          </el-upload>

          <el-button type="success" class="cyber-btn export-btn" @click="openExportDialog">
            <el-icon>
              <Download />
            </el-icon> 导出审计报表
          </el-button>
        </div>
      </div>

      <div class="table-wrapper">
        <el-table v-loading="loading" :data="tableData" element-loading-background="rgba(11, 9, 26, 0.8)"
          element-loading-text="数据检索中..." class="cyber-table" height="100%" stripe>

          <el-table-column prop="id" label="流水号" width="100" align="center" />
          <el-table-column prop="timestamp" label="记录时间" width="200" align="center">
            <template #default="scope">
              <span style="color: #00F0FF; font-weight: bold;">
                {{ scope.row.timestamp ? String(scope.row.timestamp).replace('T', ' ') : '--' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="buildingId" label="建筑节点标识" min-width="200" />

          <el-table-column v-if="selectedParams.includes('electricity')" prop="electricity" label="耗电量 (kWh)"
            width="150" align="right">
            <template #default="scope">
              {{ scope.row.electricity ? Number(scope.row.electricity).toFixed(2) : '0.00' }}
            </template>
          </el-table-column>

          <el-table-column v-if="selectedParams.includes('chilledwater')" prop="chilledwater" label="冷负荷 (kWh)"
            width="150" align="right">
            <template #default="scope">
              {{ scope.row.chilledwater ? Number(scope.row.chilledwater).toFixed(2) : '0.00' }}
            </template>
          </el-table-column>

          <el-table-column v-if="selectedParams.includes('airTemperature')" prop="airTemperature" label="室外温度 (℃)"
            width="120" align="right" />

          <el-table-column v-if="selectedParams.includes('cop')" prop="cop" label="系统 COP" width="120" align="center" />

          <el-table-column prop="status" label="智能诊断状态" width="160" align="center" fixed="right">
            <template #default="scope">
              <el-tag v-if="scope.row.status === 'normal'" type="success" effect="dark" class="cyber-tag">运行正常</el-tag>

              <el-tag v-else-if="scope.row.status === 'night_abnormal'" type="warning" effect="dark"
                class="cyber-tag warning-tag interactive-tag" title="点击呼叫 BEMS Copilot 深度诊断"
                @click="analyzeAnomaly(scope.row)">
                夜间违规耗电 ⚡
              </el-tag>

              <el-tag v-else-if="scope.row.status === 'critical_abnormal'" type="danger" effect="dark"
                class="cyber-tag danger-tag interactive-tag" title="点击呼叫 BEMS Copilot 深度诊断"
                @click="analyzeAnomaly(scope.row)">
                严重能耗异常 🚨
              </el-tag>

              <el-tag v-else type="info" effect="dark" class="cyber-tag">未知异常</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="queryParams.current" v-model:page-size="queryParams.size"
          :page-sizes="[15, 30, 50, 100]" layout="total, sizes, prev, pager, next, jumper" :total="total"
          @size-change="handleSizeChange" @current-change="handleCurrentChange" class="cyber-pagination" />
      </div>

      <el-dialog v-model="exportDialogVisible" title="📊 审计报表导出中枢" width="500px" class="cyber-dialog" destroy-on-close
        align-center>
        <div class="export-options">
          <div class="export-desc">请选择系统需要提取的数据切片范围：</div>

          <el-radio-group v-model="exportMode" class="export-radio-group">
            <el-radio value="current" size="large" border class="cyber-radio">
              仅提取当前视图 <span class="data-count">({{ tableData.length }} 条记录)</span>
            </el-radio>
            <el-radio value="all" size="large" border class="cyber-radio">
              提取全局检索结果 <span class="data-count">({{ total }} 条记录)</span>
            </el-radio>
            <el-radio value="custom" size="large" border class="cyber-radio">
              自定义切片提取范围
            </el-radio>
          </el-radio-group>

          <transition name="el-zoom-in-top">
            <div v-if="exportMode === 'custom'" class="custom-page-inputs">
              <el-icon class="link-icon">
                <Link />
              </el-icon>
              <span>从第</span>
              <el-input-number v-model="customPageRange.start" :min="1"
                :max="Math.max(1, Math.ceil(total / queryParams.size))" size="small" controls-position="right"
                class="cyber-input-number" />
              <span>页，至</span>
              <el-input-number v-model="customPageRange.end" :min="customPageRange.start"
                :max="Math.max(1, Math.ceil(total / queryParams.size))" size="small" controls-position="right"
                class="cyber-input-number" />
              <span>页</span>
            </div>
          </transition>
        </div>

        <template #footer>
          <span class="dialog-footer">
            <el-button @click="exportDialogVisible = false" class="cyber-btn cancel-btn">终 止</el-button>
            <el-button type="primary" @click="executeExport" :loading="isExporting"
              class="cyber-btn search-btn pulse-btn">
              <el-icon v-if="!isExporting">
                <Download />
              </el-icon> {{ isExporting ? '采集中...' : '执 行 导 出' }}
            </el-button>
          </span>
        </template>
      </el-dialog>

    </el-config-provider>
  </div>
</template>

<script setup>
/* 🚀 声明区：将所有依赖变量提至最顶端，彻底消灭 ReferenceError！ */
// 💡 修改 import，加上 inject
import { ref, reactive, onMounted, inject } from 'vue'
import { Search, Download, RefreshLeft, Link, Upload } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage, ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

// 💡 接入 App.vue 提供的全局 AI 呼叫引线
const callBemsAi = inject('callBemsAi')

// 1. 全局状态声明
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dateRange = ref([])
const queryParams = reactive({ current: 1, size: 15, buildingId: '' })

// 2. 热力图相关数据结构声明 (必须在下面被调用前声明！)
const calendarMap = ref({}) // 注意：刚才报错就是因为这个变量在下面被提前调用了！
const dayMap = ref({})
const monthMap = ref({})
const yearMap = ref({})

let maxDay = 1, maxMonth = 1, maxYear = 1
let minDatasetYear = 2099
let maxDatasetYear = 1970

// 1. 全局状态新增 (加在 yearMap 附近)
const seasonMonthMap = ref({})
let maxSeasonMonth = 1

/* ==========================================
   🛡️ 核心防弹解析器：杜绝一切不可预知的日历报错
========================================== */
const getStandardDateStr = (obj) => {
  if (!obj) return null
  let d = obj.dayjs ? obj.dayjs.toDate() : (obj.date || obj)
  if (!(d instanceof Date) || isNaN(d.getTime())) return null

  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/* ==========================================
   🛡️ 核心防弹解析器：支持年月维度的能量槽渲染
========================================== */
const getCellLevelInfo = (cell) => {
  if (!cell) return { level: 'none' }

  const rawText = cell.text
  const textStr = String(rawText).trim()

  // 1. 🎯 年份面板：直接匹配 4 位数字
  if (/^\d{4}$/.test(textStr)) {
    return { level: 'year', key: textStr, max: maxYear, map: yearMap.value || {} }
  }

  // 2. 🎯 日期面板：依赖底层的 Date/Dayjs 对象
  const dateObj = cell.dayjs ? cell.dayjs.toDate() : (cell.date || null)
  if (dateObj instanceof Date && !isNaN(dateObj.getTime())) {
    const year = dateObj.getFullYear()
    const month = String(dateObj.getMonth() + 1).padStart(2, '0')
    const day = String(dateObj.getDate()).padStart(2, '0')
    return { level: 'day', key: `${year}-${month}-${day}`, max: maxDay, map: dayMap.value || {} }
  }

  // 3. 🎯 月份面板：利用季节性聚合降维打击 (匹配 0-11)
  if (rawText !== undefined && !isNaN(rawText) && Number(rawText) >= 0 && Number(rawText) <= 11) {
    const monthOnly = String(Number(rawText) + 1).padStart(2, '0') // 将 0-11 转为 '01'-'12'
    return { level: 'month', key: monthOnly, max: maxSeasonMonth, map: seasonMonthMap.value || {} }
  }

  return { level: 'none' }
}

const disabledDate = (time) => {
  if (!(time instanceof Date) || isNaN(time.getTime())) return true
  // 🛡️ 加入安全防空判断，即便 calendarMap 尚未获取到，也保证不报错崩溃！
  if (!calendarMap.value || Object.keys(calendarMap.value).length === 0) return false
  const timeYear = time.getFullYear()
  if (timeYear < minDatasetYear || timeYear > maxDatasetYear) return true
  return false
}

const hasData = (cell) => {
  if (!cell) return false
  const info = getCellLevelInfo(cell)
  return info.level !== 'none' && !!info.map[info.key]
}

const getEnergyBarStyle = (cell) => {
  if (!cell) return {}
  const info = getCellLevelInfo(cell)
  if (info.level === 'none') return {}
  const val = info.map[info.key]
  if (!val) return {}

  const ratio = val / (info.max || 1)
  let color = '#00F0FF'
  if (ratio > 0.75) color = '#FF4D4F'
  else if (ratio > 0.4) color = '#FAAD14'

  if (cell.start || cell.end) color = '#FFFFFF'

  return { width: `${Math.max(ratio * 100, 15)}%`, backgroundColor: color }
}

/* ==========================================
   业务拉取逻辑
========================================== */
// 🌟 终极防弹解析：自动适配任何后端日期格式
const fetchCalendarSummary = async () => {
  try {
    const res = await axios.get(`http://localhost:8080/api/energy/calendar?buildingId=${queryParams.buildingId}`)
    const cMap = {}, dMap = {}, mMap = {}, yMap = {}, smMap = {}
    let mDay = 0, mMonth = 0, mYear = 0, mSeasonMonth = 0
    minDatasetYear = 2099
    maxDatasetYear = 1970

    const dataList = Array.isArray(res.data) ? res.data : []

    dataList.forEach(item => {
      const dateStr = item.recordDate
      const val = parseFloat(item.totalElec) || 0

      // 1. 将后端的日期字符串直接丢给 Date 对象解析（替换 '-' 为 '/' 是为了兼容部分浏览器底层）
      const dateObj = new Date(String(dateStr).replace(/-/g, '/'))
      if (isNaN(dateObj.getTime())) return // 如果遇到脏数据直接跳过

      // 2. 提取标准的年月日字符串
      const yyyy = String(dateObj.getFullYear())
      const mm = String(dateObj.getMonth() + 1).padStart(2, '0')
      const dd = String(dateObj.getDate()).padStart(2, '0')

      const fullDateStr = `${yyyy}-${mm}-${dd}`
      const monthStr = `${yyyy}-${mm}`

      // 🚀 日视图数据装载
      cMap[fullDateStr] = val
      dMap[fullDateStr] = val
      if (val > mDay) mDay = val

      // 🚀 年月视图数据装载
      mMap[monthStr] = (mMap[monthStr] || 0) + val
      if (mMap[monthStr] > mMonth) mMonth = mMap[monthStr]

      // 🚀 纯年份视图数据装载 (例如: "2016")
      yMap[yyyy] = (yMap[yyyy] || 0) + val
      if (yMap[yyyy] > mYear) mYear = yMap[yyyy]

      // 🚀 纯月份季节聚合装载 (例如: "01" 到 "12")
      smMap[mm] = (smMap[mm] || 0) + val
      if (smMap[mm] > mSeasonMonth) mSeasonMonth = smMap[mm]

      const yearNum = dateObj.getFullYear()
      if (yearNum < minDatasetYear) minDatasetYear = yearNum
      if (yearNum > maxDatasetYear) maxDatasetYear = yearNum
    })

    calendarMap.value = cMap
    dayMap.value = dMap
    monthMap.value = mMap
    yearMap.value = yMap
    seasonMonthMap.value = smMap

    maxDay = mDay || 1
    maxMonth = mMonth || 1
    maxYear = mYear || 1
    maxSeasonMonth = mSeasonMonth || 1
  } catch (error) {
    console.error('获取日历台账失败', error)
  }
}

const handleBuildingChange = () => {
  queryParams.current = 1
  fetchCalendarSummary()
  fetchTableData()
}

const resetDateRange = () => {
  dateRange.value = []
  queryParams.current = 1
  fetchTableData()
}

const fetchTableData = async () => {
  loading.value = true
  try {
    // 💡 确保至少选中一项，否则没有意义
    if (selectedParams.value.length === 0) {
      ElMessage.warning('请至少选择一项监测指标')
      loading.value = false
      return
    }

    // 🚀 修复点2：使用正确的 queryParams.current 和 queryParams.size
    let url = `http://localhost:8080/api/energy/page?current=${queryParams.current}&size=${queryParams.size}`

    if (queryParams.buildingId) {
      url += `&buildingId=${queryParams.buildingId}`
    }
    if (dateRange.value && dateRange.value.length === 2) {
      url += `&startDate=${dateRange.value[0]}&endDate=${dateRange.value[1]}`
    }

    // 🚀 拼接上选中的参数列表
    url += `&parameters=${selectedParams.value.join(',')}`

    const res = await axios.get(url)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取表格失败', error)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/* ==========================================
   🚀 BEMS Copilot: 异常数据一键联动分析
========================================== */
/* ==========================================
   🚀 BEMS Copilot: 异常数据一键联动分析 (动态参数适配版)
========================================== */
const analyzeAnomaly = (row) => {
  if (!callBemsAi) {
    ElMessage.warning('AI 智能中枢未连接，请稍后再试')
    return
  }

  // 1. 提取当前行的核心特征
  const statusName = row.status === 'night_abnormal' ? '夜间违规耗电' : '严重能耗异常'
  const timeStr = row.timestamp ? String(row.timestamp).replace('T', ' ') : '未知时间'

  // 2. 🚀 动态探查当前 row 里存在哪些指标数据，并自动拼装
  let metrics = []
  if (row.electricity !== undefined && row.electricity !== null) {
    metrics.push(`耗电量 ${Number(row.electricity).toFixed(2)} kWh`)
  }
  if (row.chilledwater !== undefined && row.chilledwater !== null) {
    metrics.push(`冷负荷 ${Number(row.chilledwater).toFixed(2)} kWh`)
  }
  if (row.airTemperature !== undefined && row.airTemperature !== null) {
    metrics.push(`室外温度 ${Number(row.airTemperature).toFixed(2)} ℃`)
  }
  if (row.cop !== undefined && row.cop !== null) {
    metrics.push(`系统 COP 为 ${Number(row.cop).toFixed(2)}`)
  }

  const metricsStr = metrics.length > 0 ? `当前实测${metrics.join('，')}。` : '当前暂无具体的实测指标数据。'

  // 3. 组装终极 Prompt
  const prompt = `系统告警：建筑节点【${row.buildingId}】在【${timeStr}】触发了【${statusName}】。${metricsStr}请结合内部知识库，告诉我这条异常的判定逻辑是什么，并给出具体的故障排查步骤。`

  // 4. 呼叫全局悬浮窗！
  callBemsAi(prompt)
}

const handleSearch = () => { queryParams.current = 1; fetchTableData() }
const handleSizeChange = (s) => { queryParams.size = s; queryParams.current = 1; fetchTableData() }
const handleCurrentChange = (p) => { queryParams.current = p; fetchTableData() }

// 导出模块...
const exportDialogVisible = ref(false)
const exportMode = ref('current')
const isExporting = ref(false)
const customPageRange = reactive({ start: 1, end: 1 })

const openExportDialog = () => {
  if (total.value === 0) return ElMessage.warning('没有检索到任何数据可供导出')
  exportDialogVisible.value = true
  customPageRange.start = queryParams.current
  customPageRange.end = queryParams.current
}

/* ==========================================
   🚀 导入成功回调
========================================== */
const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    ElMessage.success('历史数据融合成功，正在重新校准基座...')
    fetchTableData() // 刷新表格
  } else {
    ElMessage.error('融合失败：' + response.msg)
  }
}
const handleUploadError = () => {
  ElMessage.error('网络连接中断，数据融合失败')
}

/* ==========================================
   🚀 终极重构：对接后端 EasyExcel 下载流 (支持多模式切片)
========================================== */
const executeExport = async () => {
  isExporting.value = true
  try {
    // 1. 基础参数构建：继承当前的搜索框条件
    let url = `http://localhost:8080/api/energy/export?`
    if (queryParams.buildingId) url += `buildingId=${queryParams.buildingId}&`
    if (dateRange.value && dateRange.value.length === 2) {
      url += `startDate=${dateRange.value[0]}&endDate=${dateRange.value[1]}&`
    }
    // 同时也带上当前选中的监测指标，确保 Excel 表头对应
    if (selectedParams.value.length > 0) {
      url += `parameters=${selectedParams.value.join(',')}&`
    }

    // 2. 🚀 核心逻辑：根据导出模式(exportMode) 注入切片参数
    if (exportMode.value === 'current') {
      // 模式 A：仅当前视图 (携带当前页码和每页条数)
      url += `current=${queryParams.current}&size=${queryParams.size}`
    } else if (exportMode.value === 'all') {
      // 模式 B：全局检索 (不传分页参数，后端将默认导出所有匹配的数据)
      // 注意：这里什么都不加，后端接口会处理为导出全量
    } else if (exportMode.value === 'custom') {
      // 模式 C：自定义切片范围 (计算需要导出的起始和结束记录)
      // 我们将自定义的页码范围转换给后端
      url += `startPage=${customPageRange.start}&endPage=${customPageRange.end}&size=${queryParams.size}`
    }

    // 3. 发送请求：必须告诉 Axios 我们接收的是二进制文件流 (blob)
    const res = await axios.get(url, { responseType: 'blob' })

    // 4. 模拟 A 标签下载
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl

    // 生成动态文件名，标识导出模式
    const modeName = exportMode.value === 'all' ? '全局全量' : '数据切片'
    link.setAttribute('download', `BEMS_能耗审计_${modeName}_${new Date().getTime()}.xlsx`)

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)

    exportDialogVisible.value = false
    ElMessage.success(`报表导出成功！已成功提取并封装数据流。`)
  } catch (error) {
    console.error('导出失败', error)
    ElMessage.error('导出中枢响应异常，请检查后端服务')
  } finally {
    isExporting.value = false
  }
}

// 💡 定义用户当前选中的监测指标（默认选中耗电量和冷冻水）
const selectedParams = ref(['electricity', 'chilledwater', 'airTemperature']);

// 可供选择的指标字典（用于渲染复选框）
const availableParams = [
  { label: '耗电量 (kWh)', value: 'electricity' },
  { label: '冷冻水负荷 (kWh)', value: 'chilledwater' },
  { label: '室内温度 (°C)', value: 'airTemperature' },
  { label: '系统 COP', value: 'cop' }
]

onMounted(() => {
  fetchCalendarSummary()
  fetchTableData()
})
</script>

<style scoped>
.data-query-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 🚀 极致磨砂：控制面板 */
.filter-panel {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  background: rgba(22, 22, 26, 0.45);
  backdrop-filter: blur(28px) saturate(180%);
  -webkit-backdrop-filter: blur(28px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 24px 32px;
  /* 加大呼吸感 */
  margin-bottom: 24px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12), 0 12px 40px rgba(0, 0, 0, 0.4);
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 28px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
}

.filter-label {
  color: #71717a;
  margin-right: 14px;
  font-weight: 500;
  font-size: 13px;
  white-space: nowrap;
}

/* 控件深层覆写 */
.custom-select,
.custom-date-range {
  width: 240px;
}

:deep(.el-input__wrapper),
:deep(.el-range-editor.el-input__wrapper) {
  background: rgba(0, 0, 0, 0.3) !important;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.08) inset !important;
  border-radius: 10px;
}

:deep(.el-input__inner),
:deep(.el-range-input) {
  color: #f4f4f5 !important;
  font-weight: 500;
}

:deep(.el-range-separator) {
  color: #71717a;
}

/* 按钮组 */
.cyber-btn {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #f4f4f5;
  font-weight: 500;
  border-radius: 10px;
  padding: 0 20px;
  height: 36px;
  transition: all 0.2s;
}

.search-btn {
  background: #3b82f6;
  color: #fff;
  border: none;
}

.search-btn:hover {
  background: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.export-btn:hover,
.upload-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.15);
}

/* 🚀 极致磨砂：表格容器 */
.table-wrapper {
  flex: 1;
  overflow: hidden;
  background: rgba(22, 22, 26, 0.45);
  backdrop-filter: blur(28px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12), 0 12px 40px rgba(0, 0, 0, 0.4);
  padding: 1px;
  /* 让内部表格贴合边框 */
}

/* 高级感表格重写 */
.cyber-table {
  --el-table-border-color: rgba(255, 255, 255, 0.04);
  --el-table-header-bg-color: transparent;
  background-color: transparent !important;
}

/* ==========================================
   🚀 终极修复：彻底剿灭表格深层的所有白色幽灵
========================================== */

/* 1. 彻底覆盖 JS 动态注入的 hover-row 类（这是核心元凶） */
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell),
:deep(.el-table .el-table__body tr.hover-row > td.el-table__cell) {
  background-color: rgba(255, 255, 255, 0.06) !important;
}

/* 2. 如果开启了斑马纹，强制压制斑马纹的 Hover 状态 */
:deep(.el-table--striped .el-table__body tr.el-table__row--striped:hover > td.el-table__cell),
:deep(.el-table--striped .el-table__body tr.el-table__row--striped.hover-row > td.el-table__cell) {
  background-color: rgba(255, 255, 255, 0.06) !important;
}

/* 3. 修复固定列 (Fixed Columns) 的纯白遮挡层 */
:deep(.el-table .el-table__fixed),
:deep(.el-table .el-table__fixed-right),
:deep(.el-table .el-table__fixed-header-wrapper),
:deep(.el-table .el-table__fixed-body-wrapper) {
  background-color: transparent !important;
}

/* 4. 修复固定列在 Hover 时不同步变成白色的问题 */
:deep(.el-table__fixed-body-wrapper .el-table__body tr:hover > td.el-table__cell),
:deep(.el-table__fixed-body-wrapper .el-table__body tr.hover-row > td.el-table__cell) {
  background-color: rgba(255, 255, 255, 0.06) !important;
}

/* 5. 干掉表格容器四周和固定列底部的绝对定位白线 */
:deep(.el-table::before),
:deep(.el-table::after),
:deep(.el-table__inner-wrapper::before),
:deep(.el-table__fixed::before),
:deep(.el-table__fixed-right::before) {
  display: none !important;
}

/* 6. 如果开启了单行点击高亮 (Highlight Current Row) */
:deep(.el-table .current-row > td.el-table__cell) {
  background-color: rgba(59, 130, 246, 0.12) !important;
  /* 给予淡淡的品牌蓝 */
}

:deep(.el-table th.el-table__cell) {
  background-color: rgba(0, 0, 0, 0.2) !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06) !important;
  color: #71717a;
  font-size: 13px;
  font-weight: 600;
  padding: 14px 0;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.03) !important;
  color: #f4f4f5;
  font-size: 14px;
  padding: 16px 0;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background-color: rgba(255, 255, 255, 0.01) !important;
}

:deep(.el-table),
:deep(.el-table__expanded-cell),
:deep(.el-table tr) {
  background-color: transparent !important;
}

/* 标签精修 */
.cyber-tag {
  border: none;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 6px;
}

.warning-tag {
  background-color: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.danger-tag {
  background-color: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

/* 分页器精修 */
.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

.cyber-pagination {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #71717a;
  --el-pagination-button-color: #71717a;
  --el-pagination-hover-color: #f4f4f5;
}

:deep(.el-pager li.is-active) {
  color: #fff;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
}

/* ==========================================
   🚀 彻底洗掉表格内所有残留的霓虹光晕
========================================== */
:deep(.el-table [class*="glow"]),
:deep(.el-table .cyan-glow),
:deep(.el-table .time-text),
:deep(.el-table td .cell span) {
  text-shadow: none !important;
  color: #f4f4f5 !important;
}

:deep(.el-table td.el-table__cell) {
  font-family: 'Inter', 'JetBrains Mono', monospace;
  letter-spacing: 0.5px;
}

:deep(.el-table .time-column-text) {
  color: #a1a1aa !important;
  /* 沉稳的银灰色 */
  font-weight: 500;
}

/* 对话框/导出面板：同样采用厚玻璃 */
:deep(.cyber-dialog) {
  background: rgba(18, 18, 22, 0.85) !important;
  backdrop-filter: blur(40px) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.15), 0 30px 60px rgba(0, 0, 0, 0.6) !important;
  border-radius: 20px;
}

:deep(.cyber-dialog .el-dialog__header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  padding: 20px 24px;
}

:deep(.cyber-dialog .el-dialog__title) {
  color: #fff;
  font-weight: 600;
  font-size: 16px;
}

:deep(.cyber-dialog .el-dialog__footer) {
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  padding: 20px 24px;
}

.export-radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  margin-top: 16px;
}

.cyber-radio {
  margin: 0 !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 12px !important;
  transition: all 0.2s;
  padding: 0 20px;
  height: 56px;
}

.cyber-radio.is-bordered.is-checked {
  border-color: #3b82f6 !important;
  background: rgba(59, 130, 246, 0.1);
}
</style>

<style>
/* 日历组件去霓虹并修复遮挡 */
:deep(.el-year-table td .cell),
:deep(.el-month-table td .cell) {
  padding: 0 !important;
  height: 40px !important;
}

.custom-cell {
  height: 100%;
  width: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}

.custom-cell.is-start,
.custom-cell.is-end {
  background-color: #3b82f6 !important;
  color: #fff !important;
}

.custom-cell.is-in-range {
  background-color: rgba(59, 130, 246, 0.15) !important;
  color: #f4f4f5 !important;
}

/* ==========================================
   🚀 修复：日期选择器范围高亮对比度
========================================== */
/* 修复选中范围内的背景和字体颜色 */
.el-date-table td.in-range .el-date-table-cell {
  background-color: rgba(59, 130, 246, 0.25) !important;
  /* 加深一点背景蓝 */
}

.el-date-table td.in-range .el-date-table-cell__text,
.el-date-table td.in-range .custom-cell .cell-text {
  color: #ffffff !important;
  /* 强制文字纯白 */
  font-weight: 500 !important;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
  /* 增加极细的黑色文字阴影，使其在蓝底上更锐利 */
}

/* 修复起点和终点（选中那两天的首尾）的高亮颜色 */
.el-date-table td.start-date .el-date-table-cell__text,
.el-date-table td.end-date .el-date-table-cell__text,
.el-date-table td.start-date .custom-cell .cell-text,
.el-date-table td.end-date .custom-cell .cell-text {
  background-color: #3b82f6 !important;
  /* 静谧蓝主体色 */
  color: #ffffff !important;
  font-weight: 600 !important;
  border-radius: 50% !important;
  /* 保证高亮是个圆润的圈 */
}

/* 确保今日（Today）的文字也能看清 */
.el-date-table td.today .el-date-table-cell__text {
  color: #3b82f6 !important;
  font-weight: 700;
}
</style>
