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
                :default-value="[new Date(2016, 6, 1), new Date(2016, 7, 1)]" unlink-panels>
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

          <el-button type="primary" class="cyber-btn search-btn" @click="handleSearch">
            <el-icon>
              <Search />
            </el-icon> 精准检索
          </el-button>
        </div>

        <div class="filter-right">
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
          <el-table-column prop="electricity" label="耗电量 (kWh)" width="150" align="right">
            <template #default="scope">
              {{ scope.row.electricity ? Number(scope.row.electricity).toFixed(2) : '0.00' }}
            </template>
          </el-table-column>
          <el-table-column prop="chilledwater" label="冷负荷 (kWh)" width="150" align="right">
            <template #default="scope">
              {{ scope.row.chilledwater ? Number(scope.row.chilledwater).toFixed(2) : '0.00' }}
            </template>
          </el-table-column>
          <el-table-column prop="airTemperature" label="室外温度 (℃)" width="120" align="right" />
          <el-table-column prop="status" label="智能诊断状态" width="160" align="center" fixed="right">
            <template #default="scope">
              <el-tag v-if="scope.row.status === 'normal'" type="success" effect="dark" class="cyber-tag">运行正常</el-tag>
              <el-tag v-else-if="scope.row.status === 'night_abnormal'" type="warning" effect="dark"
                class="cyber-tag warning-tag">夜间违规耗电</el-tag>
              <el-tag v-else-if="scope.row.status === 'critical_abnormal'" type="danger" effect="dark"
                class="cyber-tag danger-tag">严重能耗异常</el-tag>
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
import { ref, reactive, onMounted } from 'vue'
import { Search, Download, RefreshLeft, Link } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage, ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

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
    let url = `http://localhost:8080/api/energy/page?current=${queryParams.current}&size=${queryParams.size}`
    if (queryParams.buildingId) url += `&buildingId=${queryParams.buildingId}`
    if (dateRange.value && dateRange.value.length === 2) {
      url += `&startDate=${dateRange.value[0]}&endDate=${dateRange.value[1]}`
    }
    const res = await axios.get(url)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取表格失败', error)
    tableData.value = []
    total.value = 0
  }
  finally { loading.value = false }
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

const executeExport = async () => {
  isExporting.value = true
  try {
    let exportData = []
    if (exportMode.value === 'current') {
      exportData = tableData.value
    } else {
      let startPage = exportMode.value === 'all' ? 1 : customPageRange.start
      let endPage = exportMode.value === 'all' ? Math.ceil(total.value / queryParams.size) : customPageRange.end
      if (startPage > endPage) { ElMessage.error('起始页不能大于结束页'); isExporting.value = false; return }

      for (let p = startPage; p <= endPage; p++) {
        let url = `http://localhost:8080/api/energy/page?current=${p}&size=${queryParams.size}`
        if (queryParams.buildingId) url += `&buildingId=${queryParams.buildingId}`
        if (dateRange.value && dateRange.value.length === 2) {
          url += `&startDate=${dateRange.value[0]}&endDate=${dateRange.value[1]}`
        }
        const res = await axios.get(url)
        if (res.data?.records) exportData.push(...res.data.records)
      }
    }
    let csv = '\uFEFF' + "流水号,记录时间,建筑节点标识,耗电量(kWh),冷负荷(kWh),室外温度(℃),智能诊断状态\n"
    exportData.forEach(r => {
      const st = { 'normal': '运行正常', 'night_abnormal': '夜间违规耗电', 'critical_abnormal': '严重能耗异常' }[r.status] || r.status
      csv += `${r.id},${r.timestamp ? String(r.timestamp).replace('T', ' ') : ''},${r.buildingId},${r.electricity},${r.chilledwater},${r.airTemperature},${st}\n`
    })
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement("a")
    link.href = url
    link.download = `BEMS_能耗审计报表_${new Date().getTime()}.csv`
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    exportDialogVisible.value = false
    ElMessage.success('报表导出完成！')
  } finally { isExporting.value = false }
}

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

.filter-panel {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(30, 27, 56, 0.4);
  border: 1px solid #2A2946;
  border-radius: 8px;
  padding: 15px 20px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 15px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
}

.filter-label {
  color: #a0a2b8;
  margin-right: 10px;
  font-weight: bold;
  white-space: nowrap;
}

.custom-select {
  width: 220px;
}

.date-range-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.custom-date-range {
  width: 300px;
}

.reset-btn {
  padding: 8px 12px;
  color: #a0a2b8;
  border-color: #2A2946;
}

.reset-btn:hover {
  color: #FF4D4F;
  border-color: #FF4D4F;
  background-color: rgba(255, 77, 79, 0.1);
  box-shadow: 0 0 8px rgba(255, 77, 79, 0.5);
}

:deep(.el-input__wrapper),
:deep(.el-range-editor.el-input__wrapper) {
  background-color: #0b091a !important;
  box-shadow: 0 0 0 1px #2A2946 inset !important;
}

:deep(.el-input__inner),
:deep(.el-range-input) {
  color: #00F0FF !important;
  font-weight: bold;
}

:deep(.el-range-separator) {
  color: #a0a2b8;
}

:deep(.el-date-table td) {
  padding: 0 !important;
}

.custom-cell {
  position: relative;
  width: 100%;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 2px 0;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.custom-cell.is-start,
.custom-cell.is-end {
  background-color: #00F0FF !important;
  color: #000000 !important;
  box-shadow: 0 0 10px #00F0FF;
  z-index: 2;
  font-weight: bold;
}

.custom-cell.is-in-range {
  background-color: rgba(0, 240, 255, 0.5) !important;
  color: #000000 !important;
  font-weight: bold;
}

.custom-cell.is-disabled-visual .cell-text {
  color: #4a4a5e;
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
}

.cyber-btn {
  background-color: #0b091a;
  border: 1px solid;
  font-weight: bold;
  letter-spacing: 1px;
}

.search-btn {
  color: #00F0FF;
  border-color: #00F0FF;
}

.search-btn:hover {
  background-color: rgba(0, 240, 255, 0.1);
  box-shadow: 0 0 10px rgba(0, 240, 255, 0.5);
}

.export-btn {
  color: #00FF9D;
  border-color: #00FF9D;
}

.export-btn:hover {
  background-color: rgba(0, 255, 157, 0.1);
  box-shadow: 0 0 10px rgba(0, 255, 157, 0.5);
}

.table-wrapper {
  flex: 1;
  background: rgba(30, 27, 56, 0.5);
  border: 1px solid #2A2946;
  border-radius: 8px;
  overflow: hidden;
  padding: 1px;
}

.cyber-table {
  background-color: transparent !important;
  --el-table-border-color: #2A2946;
  --el-table-header-bg-color: #0b091a;
  --el-table-header-text-color: #a0a2b8;
  --el-table-tr-bg-color: transparent;
  --el-table-row-hover-bg-color: rgba(0, 240, 255, 0.1);
}

:deep(.el-table th.el-table__cell) {
  background-color: #0b091a !important;
  border-bottom: 2px solid #2A2946 !important;
  font-size: 14px;
  letter-spacing: 1px;
}

:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid #1f1d36 !important;
  color: #ffffff;
}

:deep(.el-table .el-table__fixed-right) {
  background-color: #05050f !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background-color: rgba(0, 240, 255, 0.05) !important;
}

:deep(.el-table__body tr:hover > td.el-table__cell) {
  background-color: rgba(0, 240, 255, 0.15) !important;
}

:deep(.el-table),
:deep(.el-table__expanded-cell),
:deep(.el-table tr) {
  background-color: transparent !important;
}

.cyber-tag {
  border: none;
  font-weight: bold;
}

.warning-tag {
  background-color: #FAAD14;
  color: #000;
}

.danger-tag {
  background-color: #FF4D4F;
  color: #fff;
  box-shadow: 0 0 8px rgba(255, 77, 79, 0.8);
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  background: rgba(30, 27, 56, 0.4);
  padding: 10px 20px;
  border: 1px solid #2A2946;
  border-radius: 8px;
}

.cyber-pagination {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #a0a2b8;
  --el-pagination-button-color: #a0a2b8;
  --el-pagination-button-disabled-color: #4a4a5e;
  --el-pagination-hover-color: #00F0FF;
}

:deep(.el-pager li.is-active) {
  color: #00F0FF;
  font-weight: bold;
}

/* 导出面板 */
:deep(.cyber-dialog) {
  background-color: #0b091a !important;
  border: 1px solid #00F0FF !important;
  box-shadow: 0 0 25px rgba(0, 240, 255, 0.2);
  border-radius: 8px;
}

:deep(.cyber-dialog .el-dialog__header) {
  background-color: transparent;
  margin-right: 0;
  border-bottom: 1px solid #2A2946;
  padding-bottom: 15px;
}

:deep(.cyber-dialog .el-dialog__title) {
  color: #00F0FF;
  font-weight: bold;
  letter-spacing: 1px;
  font-size: 16px;
}

:deep(.cyber-dialog .el-dialog__body) {
  padding: 25px 30px;
}

:deep(.cyber-dialog .el-dialog__footer) {
  background-color: transparent;
  border-top: 1px solid #2A2946;
  padding-top: 15px;
}

.export-desc {
  color: #a0a2b8;
  margin-bottom: 15px;
  font-size: 14px;
}

.export-options {
  display: flex;
  flex-direction: column;
}

.export-radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.cyber-radio {
  margin: 0 !important;
  border: 1px solid #2A2946 !important;
  background: rgba(30, 27, 56, 0.4);
  border-radius: 6px !important;
  transition: all 0.3s;
  display: flex;
  align-items: center;
}

.cyber-radio.is-bordered.is-checked {
  border-color: #00F0FF !important;
  background: rgba(0, 240, 255, 0.1);
  box-shadow: 0 0 10px rgba(0, 240, 255, 0.2);
}

:deep(.cyber-radio .el-radio__inner) {
  background-color: transparent;
  border-color: #4a4a5e;
}

:deep(.cyber-radio.is-checked .el-radio__inner) {
  background-color: #00F0FF;
  border-color: #00F0FF;
}

:deep(.cyber-radio .el-radio__label) {
  color: #ffffff !important;
  font-weight: bold;
}

.data-count {
  color: #00FF9D;
  font-weight: normal;
  margin-left: 5px;
}

.custom-page-inputs {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 15px;
  padding: 15px;
  background: rgba(0, 240, 255, 0.05);
  border-radius: 6px;
  border: 1px dashed #00F0FF;
}

.custom-page-inputs span {
  color: #fff;
  font-size: 14px;
}

.link-icon {
  color: #00F0FF;
  font-size: 18px;
}

.cyber-input-number {
  width: 100px;
}

:deep(.cyber-input-number .el-input__wrapper) {
  background-color: #05050f !important;
  box-shadow: 0 0 0 1px #2A2946 inset !important;
}

:deep(.cyber-input-number .el-input__inner) {
  color: #00F0FF !important;
  font-weight: bold;
}

:deep(.cyber-input-number .el-input-number__decrease),
:deep(.cyber-input-number .el-input-number__increase) {
  background-color: #0b091a !important;
  border-color: #2A2946 !important;
  color: #a0a2b8;
}

:deep(.cyber-input-number .el-input-number__decrease:hover),
:deep(.cyber-input-number .el-input-number__increase:hover) {
  color: #00F0FF;
}

.cancel-btn {
  color: #a0a2b8;
  border-color: #2A2946;
}

.cancel-btn:hover {
  color: #fff;
  border-color: #fff;
}

.pulse-btn {
  position: relative;
  overflow: hidden;
}

.pulse-btn:hover {
  box-shadow: 0 0 15px rgba(0, 240, 255, 0.6);
}
</style>

<style>
/* ==========================================
   🛡️ 终极降维打击：CSS 视觉完全接管 Element Plus 底层
========================================== */

/* 1. 精准屏蔽月份面板的原始错误数字 (0-11) */
.el-month-table .cyber-cell-text {
  font-size: 0 !important;
}

/* 2. 利用 data-text 和伪元素，重新注入完美的 1月-12月 */
.el-month-table .cyber-cell-text[data-text="0"]::after {
  content: "1月";
  font-size: 14px;
  font-weight: bold;
}

.el-month-table .cyber-cell-text[data-text="1"]::after {
  content: "2月";
  font-size: 14px;
  font-weight: bold;
}

.el-month-table .cyber-cell-text[data-text="2"]::after {
  content: "3月";
  font-size: 14px;
  font-weight: bold;
}

.el-month-table .cyber-cell-text[data-text="3"]::after {
  content: "4月";
  font-size: 14px;
  font-weight: bold;
}

.el-month-table .cyber-cell-text[data-text="4"]::after {
  content: "5月";
  font-size: 14px;
  font-weight: bold;
}

.el-month-table .cyber-cell-text[data-text="5"]::after {
  content: "6月";
  font-size: 14px;
  font-weight: bold;
}

.el-month-table .cyber-cell-text[data-text="6"]::after {
  content: "7月";
  font-size: 14px;
  font-weight: bold;
}

.el-month-table .cyber-cell-text[data-text="7"]::after {
  content: "8月";
  font-size: 14px;
  font-weight: bold;
}

.el-month-table .cyber-cell-text[data-text="8"]::after {
  content: "9月";
  font-size: 14px;
  font-weight: bold;
}

.el-month-table .cyber-cell-text[data-text="9"]::after {
  content: "10月";
  font-size: 14px;
  font-weight: bold;
}

.el-month-table .cyber-cell-text[data-text="10"]::after {
  content: "11月";
  font-size: 14px;
  font-weight: bold;
}

.el-month-table .cyber-cell-text[data-text="11"]::after {
  content: "12月";
  font-size: 14px;
  font-weight: bold;
}

/* 强制解除 Element Plus 年月面板的空间挤压 */
:deep(.el-year-table td .cell),
:deep(.el-month-table td .cell) {
  padding: 0 !important;
  height: 40px !important;
  /* 给予单元格足够的高度 */
  position: relative !important;
}

/* 确保自定义单元格能够撑满并显露底部的能量槽 */
.custom-cell {
  height: 100%;
  width: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.energy-bar {
  position: absolute;
  bottom: 2px;
  height: 4px;
  /* 稍微加粗一点，在宏观面板更有气势 */
  border-radius: 2px;
  left: 50%;
  transform: translateX(-50%);
  /* 居中对齐 */
}
</style>