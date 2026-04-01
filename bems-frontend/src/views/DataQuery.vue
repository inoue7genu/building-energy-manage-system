<template>
  <div class="data-query-container">

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
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
            end-placeholder="结束日期" value-format="YYYY-MM-DD" class="custom-date-range" :disabled-date="disabledDate">
            <template #default="cell">
              <div class="custom-calendar-cell"
                :class="{ 'is-disabled': disabledDate(cell.date || cell.dayjs?.toDate()) }">
                <span class="cell-text">{{ cell.text }}</span>
                <div v-if="hasData(cell)" class="energy-bar" :style="getEnergyBarStyle(cell)"></div>
              </div>
            </template>
          </el-date-picker>
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
            <span style="color: #00F0FF; font-weight: bold;">{{ scope.row.timestamp.replace('T', ' ') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="buildingId" label="建筑节点标识" min-width="200" />
        <el-table-column prop="electricity" label="耗电量 (kWh)" width="150" align="right">
          <template #default="scope">
            {{ scope.row.electricity ? scope.row.electricity.toFixed(2) : '0.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="chilledwater" label="冷负荷 (kWh)" width="150" align="right">
          <template #default="scope">
            {{ scope.row.chilledwater ? scope.row.chilledwater.toFixed(2) : '0.00' }}
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

    <el-dialog v-model="exportDialogVisible" title="报表导出控制台" width="450px" class="cyber-dialog" destroy-on-close>
      <div class="export-options">
        <el-radio-group v-model="exportMode" class="export-radio-group" text-color="#00F0FF" fill="#0b091a">
          <el-radio value="current" size="large">仅导出当前页 ({{ tableData.length }} 条)</el-radio>
          <el-radio value="all" size="large">导出全部检索结果 ({{ total }} 条)</el-radio>
          <el-radio value="custom" size="large">自定义导出页数范围</el-radio>
        </el-radio-group>

        <div v-if="exportMode === 'custom'" class="custom-page-inputs">
          <span>从第</span>
          <el-input-number v-model="customPageRange.start" :min="1" :max="Math.ceil(total / queryParams.size)"
            size="small" />
          <span>页，至第</span>
          <el-input-number v-model="customPageRange.end" :min="customPageRange.start"
            :max="Math.ceil(total / queryParams.size)" size="small" />
          <span>页</span>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="exportDialogVisible = false" class="cyber-btn">取 消</el-button>
          <el-button type="primary" @click="executeExport" :loading="isExporting" class="cyber-btn search-btn">
            开 始 导 出
          </el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Download } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// 表格状态
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dateRange = ref([])

// 查询参数
const queryParams = reactive({
  current: 1,
  size: 15,
  buildingId: ''
})

/* ==========================================
   范围选择器 - 能量槽热力图逻辑
========================================== */
const calendarMap = ref({})
let maxElecOfDay = 1

// 拉取日历台账
const fetchCalendarSummary = async () => {
  if (!queryParams.buildingId) {
    calendarMap.value = {} // 不选建筑就不展示热力
    return
  }
  try {
    const res = await axios.get(`http://localhost:8080/api/energy/calendar?buildingId=${queryParams.buildingId}`)
    const map = {}
    let max = 0
    res.data.forEach(item => {
      const dateStr = item.recordDate
      const val = parseFloat(item.totalElec) || 0
      map[dateStr] = val
      if (val > max) max = val
    })
    calendarMap.value = map
    maxElecOfDay = max || 1
  } catch (error) {
    console.error('获取日历台账失败', error)
  }
}

// 切换建筑时触发
const handleBuildingChange = () => {
  queryParams.current = 1
  dateRange.value = [] // 切换建筑清空旧日期
  fetchCalendarSummary()
  fetchTableData()
}

const disabledDate = (time) => {
  // 只有当明确选择了某栋建筑时，才禁用没数据的日子。全局查询时允许随便点。
  if (!queryParams.buildingId) return false
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
  if (!queryParams.buildingId) return false
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


/* ==========================================
   核心表格查询逻辑
========================================== */
const fetchTableData = async () => {
  loading.value = true
  try {
    let url = `http://localhost:8080/api/energy/page?current=${queryParams.current}&size=${queryParams.size}`
    if (queryParams.buildingId) {
      url += `&buildingId=${queryParams.buildingId}`
    }
    if (dateRange.value && dateRange.value.length === 2) {
      url += `&startDate=${dateRange.value[0]}&endDate=${dateRange.value[1]}`
    }

    const res = await axios.get(url)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取表格数据失败', error)
    ElMessage.error('无法连接到数据库引擎')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.current = 1
  fetchTableData()
}
const handleSizeChange = (newSize) => {
  queryParams.size = newSize
  queryParams.current = 1
  fetchTableData()
}
const handleCurrentChange = (newPage) => {
  queryParams.current = newPage
  fetchTableData()
}


/* ==========================================
   🚀 高级报表导出引擎 (支持全部/跨页导出)
========================================== */
const exportDialogVisible = ref(false)
const exportMode = ref('current')
const isExporting = ref(false)
const customPageRange = reactive({ start: 1, end: 1 })

const openExportDialog = () => {
  if (total.value === 0) {
    ElMessage.warning('没有检索到任何数据')
    return
  }
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
      // 需要往后端发请求拉取多页数据
      let startPage = exportMode.value === 'all' ? 1 : customPageRange.start
      let endPage = exportMode.value === 'all' ? Math.ceil(total.value / queryParams.size) : customPageRange.end

      if (startPage > endPage) {
        ElMessage.error('起始页不能大于结束页')
        isExporting.value = false
        return
      }

      ElMessage.info(`正在抓取第 ${startPage} 到 ${endPage} 页的底层数据，请稍候...`)

      // 循环遍历抓取所需页码的数据
      for (let p = startPage; p <= endPage; p++) {
        let url = `http://localhost:8080/api/energy/page?current=${p}&size=${queryParams.size}`
        if (queryParams.buildingId) url += `&buildingId=${queryParams.buildingId}`
        if (dateRange.value && dateRange.value.length === 2) {
          url += `&startDate=${dateRange.value[0]}&endDate=${dateRange.value[1]}`
        }
        const res = await axios.get(url)
        exportData.push(...res.data.records)
      }
    }

    // 生成 CSV 字符串
    let csvContent = '\uFEFF' // 解决中文乱码
    csvContent += "流水号,记录时间,建筑节点标识,耗电量(kWh),冷负荷(kWh),室外温度(℃),智能诊断状态\n"

    exportData.forEach(row => {
      const statusMap = { 'normal': '运行正常', 'night_abnormal': '夜间违规耗电', 'critical_abnormal': '严重能耗异常' }
      const statusText = statusMap[row.status] || row.status
      const timeText = row.timestamp ? row.timestamp.replace('T', ' ') : ''
      csvContent += `${row.id},${timeText},${row.buildingId},${row.electricity},${row.chilledwater},${row.airTemperature},${statusText}\n`
    })

    // 触发下载
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement("a")
    const url = URL.createObjectURL(blob)
    link.setAttribute("href", url)
    link.setAttribute("download", `BEMS_能耗审计报表_${new Date().getTime()}.csv`)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    exportDialogVisible.value = false
    ElMessage.success(`报表导出成功！共导出 ${exportData.length} 条记录。`)
  } catch (error) {
    console.error("导出失败", error)
    ElMessage.error("抓取底层数据失败")
  } finally {
    isExporting.value = false
  }
}

onMounted(() => {
  fetchTableData()
  // 初始不加载日历，等用户选了具体建筑再加载
})
</script>

<style scoped>
.data-query-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 控制面板 */
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

.custom-date-range {
  width: 320px;
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

/* 范围选择器的热力图格子样式 */
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

/* 核心表格区 */
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

/* 解决白条纹 Bug */
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

/* 导出控制台弹窗暗黑样式定制 */
:deep(.cyber-dialog .el-dialog__header) {
  background-color: #0b091a;
  margin-right: 0;
  border-bottom: 1px solid #2A2946;
}

:deep(.cyber-dialog .el-dialog__title) {
  color: #00F0FF;
  font-weight: bold;
  letter-spacing: 1px;
}

:deep(.cyber-dialog .el-dialog__body) {
  background-color: #05050f;
  color: #a0a2b8;
}

:deep(.cyber-dialog .el-dialog__footer) {
  background-color: #0b091a;
  border-top: 1px solid #2A2946;
}

.export-options {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.export-radio-group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

:deep(.el-radio.el-radio--large) {
  height: auto;
}

:deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #00F0FF;
  font-weight: bold;
}

:deep(.el-radio__inner) {
  background-color: transparent;
  border-color: #2A2946;
}

:deep(.el-radio__input.is-checked .el-radio__inner) {
  background-color: #00F0FF;
  border-color: #00F0FF;
}

.custom-page-inputs {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: 28px;
  padding: 10px;
  background: rgba(30, 27, 56, 0.5);
  border-radius: 4px;
  border: 1px dashed #2A2946;
}

.custom-page-inputs span {
  color: #fff;
  font-size: 14px;
}

.custom-page-inputs :deep(.el-input-number) {
  width: 100px;
}
</style>