<template>
  <div class="devices-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>设备列表</h2>
          <el-button type="primary" @click="handleAddDevice">添加设备</el-button>
        </div>
      </template>
      <div class="devices-content">
        <el-input
          v-model="searchQuery"
          placeholder="搜索设备 IMEI"
          prefix-icon="Search"
          class="search-input"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button class="search-button" type="primary" @click="handleSearch">搜索</el-button>
        <el-table class="full-width-table" :data="devices" style="width: 100%" v-loading="loading">
          <el-table-column prop="id" label="ID" min-width="80" />
          <el-table-column prop="imei" label="IMEI" min-width="220" />
          <el-table-column prop="isOnline" label="在线状态" min-width="120">
            <template #default="scope">
              <el-tag :type="scope.row.isOnline ? 'success' : 'danger'" size="small">
                {{ scope.row.isOnline ? '在线' : '离线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="patient.name" label="关联病人" min-width="160">
            <template #default="scope">
              {{ scope.row.patient?.name || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="batteryLevel" label="电量" min-width="100">
            <template #default="scope">
              <span v-if="scope.row.batteryLevel">{{ scope.row.batteryLevel }}%</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" min-width="220" :formatter="formatDate" />
          <el-table-column label="操作" width="320" fixed="right">
            <template #default="scope">
              <div class="action-row">
                <el-button class="action-button action-edit" type="primary" size="small" @click="handleEditDevice(scope.row)">
                  编辑
                </el-button>
                <el-button
                  class="action-button action-link"
                  type="success"
                  size="small"
                  @click="handleAssignPatient(scope.row)"
                  :disabled="!!scope.row.patient"
                >
                  {{ scope.row.patient ? '已关联' : '关联病人' }}
                </el-button>
                <el-dropdown trigger="click" :hide-on-click="false" popper-class="device-action-popper">
                  <el-button class="action-button action-more" size="small" type="primary" plain @click.stop>
                    更多操作
                    <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                <template #dropdown>
                  <div class="device-action-panel" @click.stop>
                    <button class="panel-action muted" type="button" disabled>
                      <el-icon class="panel-icon"><Connection /></el-icon>
                      {{ scope.row.patient ? '已关联病人' : '未关联病人' }}
                    </button>
                    <button
                      v-if="scope.row.patient"
                      class="panel-action"
                      type="button"
                      @click="handleUnassignPatient(scope.row)"
                    >
                      <el-icon class="panel-icon"><RefreshRight /></el-icon>
                      取消关联
                    </button>

                    <div class="device-action-section">
                      <button class="section-toggle" type="button" @click="toggleActionSection('deviceControl')">
                        <span>设备控制</span>
                        <el-icon :class="{ expanded: isActionSectionOpen('deviceControl') }"><ArrowDown /></el-icon>
                      </button>
                      <div v-show="isActionSectionOpen('deviceControl')" class="section-items">
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'sendCommand')"><el-icon class="panel-icon"><Operation /></el-icon>下发指令</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'locateNow')"><el-icon class="panel-icon"><Location /></el-icon>立即定位</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'realTimeTracking')"><el-icon class="panel-icon"><Setting /></el-icon>开启实时追踪</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'stopTracking')"><el-icon class="panel-icon"><RefreshRight /></el-icon>停止实时追踪</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'restart')"><el-icon class="panel-icon"><RefreshRight /></el-icon>重启</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'shutdown')"><el-icon class="panel-icon"><SwitchButton /></el-icon>关机</button>
                      </div>
                    </div>

                    <div class="device-action-section">
                      <button class="section-toggle" type="button" @click="toggleActionSection('communication')">
                        <span>通信功能</span>
                        <el-icon :class="{ expanded: isActionSectionOpen('communication') }"><ArrowDown /></el-icon>
                      </button>
                      <div v-show="isActionSectionOpen('communication')" class="section-items">
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'shortCommand')"><el-icon class="panel-icon"><Message /></el-icon>短信命令</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'sendMessage')"><el-icon class="panel-icon"><ChatDotRound /></el-icon>发信息</button>
                      </div>
                    </div>

                    <div class="device-action-section">
                      <button class="section-toggle" type="button" @click="toggleActionSection('configuration')">
                        <span>设备配置</span>
                        <el-icon :class="{ expanded: isActionSectionOpen('configuration') }"><ArrowDown /></el-icon>
                      </button>
                      <div v-show="isActionSectionOpen('configuration')" class="section-items">
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'timezone')"><el-icon class="panel-icon"><Clock /></el-icon>同步时区</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'timezoneNew')"><el-icon class="panel-icon"><Clock /></el-icon>自定义时区</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'setSos')"><el-icon class="panel-icon"><Warning /></el-icon>设置SOS码</button>
                      </div>
                    </div>

                    <div class="device-action-section">
                      <button class="section-toggle" type="button" @click="toggleActionSection('health')">
                        <span>健康监测</span>
                        <el-icon :class="{ expanded: isActionSectionOpen('health') }"><ArrowDown /></el-icon>
                      </button>
                      <div v-show="isActionSectionOpen('health')" class="section-items">
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'heartRate')"><el-icon class="panel-icon"><Connection /></el-icon>心率</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'bloodPressure')"><el-icon class="panel-icon"><Document /></el-icon>血压</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'bloodOxygen')"><el-icon class="panel-icon"><CirclePlus /></el-icon>血氧</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'temperature')"><el-icon class="panel-icon"><Monitor /></el-icon>体温</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'takePhoto')"><el-icon class="panel-icon"><Camera /></el-icon>拍照</button>
                      </div>
                    </div>

                    <div class="device-action-section">
                      <button class="section-toggle" type="button" @click="toggleActionSection('dataQuery')">
                        <span>数据查询</span>
                        <el-icon :class="{ expanded: isActionSectionOpen('dataQuery') }"><ArrowDown /></el-icon>
                      </button>
                      <div v-show="isActionSectionOpen('dataQuery')" class="section-items">
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'historyTrack')"><el-icon class="panel-icon"><MapLocation /></el-icon>历史轨迹</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'getLogs')"><el-icon class="panel-icon"><Document /></el-icon>获取日志</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'rawLogs')"><el-icon class="panel-icon"><DocumentCopy /></el-icon>原始日志</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'alarmCalendar')"><el-icon class="panel-icon"><Clock /></el-icon>报警日历</button>
                        <button class="panel-action featured" type="button" @click="handleDropdownCommand(scope.row, 'batteryReport')"><el-icon class="panel-icon"><Document /></el-icon>电池报告</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'realTimeTracking')"><el-icon class="panel-icon"><VideoPlay /></el-icon>实时追踪</button>
                        <button class="panel-action" type="button" @click="handleDropdownCommand(scope.row, 'exportStatusHistory')"><el-icon class="panel-icon"><Download /></el-icon>导出状态历史</button>
                      </div>
                    </div>
                  </div>
                </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>

    <!-- 添加/编辑设备对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="deviceForm" :rules="rules" ref="deviceFormRef" label-width="100px">
        <el-form-item label="IMEI" prop="imei">
          <el-input v-model="deviceForm.imei" placeholder="请输入设备 IMEI" maxlength="15" show-word-limit />
        </el-form-item>
        <el-form-item label="MCC" prop="mcc">
          <el-input v-model="deviceForm.mcc" placeholder="请输入 MCC" maxlength="8" show-word-limit />
        </el-form-item>
        <el-form-item label="MNC" prop="mnc">
          <el-input v-model="deviceForm.mnc" placeholder="请输入 MNC" maxlength="8" show-word-limit />
        </el-form-item>
        <el-form-item label="APN" prop="apn">
          <el-input v-model="deviceForm.apn" placeholder="请输入 APN" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="ICCID" prop="iccid">
          <el-input v-model="deviceForm.iccid" placeholder="请输入 ICCID" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="IMSI" prop="imsi">
          <el-input v-model="deviceForm.imsi" placeholder="请输入 IMSI" maxlength="64" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveDevice" :loading="dialogLoading">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 关联病人对话框 -->
    <el-dialog v-model="assignDialogVisible" title="关联病人" width="500px">
      <el-form :model="assignForm" :rules="assignRules" ref="assignFormRef" label-width="100px">
        <el-form-item label="设备IMEI">
          <el-input :value="devices.find(d => d.id === assignForm.deviceId)?.imei" disabled placeholder="设备IMEI" />
        </el-form-item>
        <el-form-item label="病人" prop="patientId">
          <el-select v-model="assignForm.patientId" placeholder="请选择病人">
            <el-option
              v-for="patient in availablePatients"
              :key="patient.id"
              :label="`${patient.name} - ${patient.ward}病房${patient.bed}床`"
              :value="patient.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="assignDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveAssign" :loading="assignLoading">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 下发指令对话框 -->
    <el-dialog v-model="commandDialogVisible" title="下发指令" width="500px">
      <el-form :model="commandForm" :rules="commandRules" ref="commandFormRef" label-width="80px">
        <el-form-item label="IMEI" prop="imei">
          <el-input v-model="commandForm.imei" disabled placeholder="设备IMEI" />
        </el-form-item>
        <el-form-item label="指令" prop="command">
          <el-input v-model="commandForm.command" placeholder="请输入指令" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="commandDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSendCommand" :loading="commandLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 原始日志对话框 -->
    <el-dialog v-model="rawLogDialogVisible" title="原始日志" width="1000px">
      <div class="raw-log-header">
        <el-form :model="rawLogForm" inline>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="rawLogForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              :disabled-date="(time) => {
                // 只能选择最近15天的日期
                return time.getTime() < Date.now() - 15 * 24 * 60 * 60 * 1000 || time.getTime() > Date.now();
              }"
            />
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="rawLogForm.keyword" placeholder="请输入关键词" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQueryRawLogs" :loading="rawLogLoading">查询</el-button>
            <el-button @click="handleResetRawLogs">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      <div class="raw-log-content" v-loading="rawLogLoading">
        <pre v-if="rawLogContent" class="raw-log-text">{{ rawLogContent }}</pre>
        <div v-else-if="!rawLogLoading" class="empty-logs">暂无日志数据</div>
      </div>
      <!-- 分页控件 -->
      <div class="raw-log-pagination" v-if="rawLogTotal > 0">
        <el-pagination
          v-model:current-page="rawLogForm.page"
          v-model:page-size="rawLogForm.size"
          :page-sizes="[50, 100, 200, 500]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="rawLogTotal"
          @size-change="handleQueryRawLogs"
          @current-change="handleQueryRawLogs"
        />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="rawLogDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { deviceApi } from '../api/device'
import { patientApi } from '../api/patient'
import { downlinkApi } from '../api/downlink'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  ArrowDown, Operation, Location, RefreshRight, SwitchButton, 
  Message, ChatDotRound, Setting, Clock, Warning,
  Connection, Document, CirclePlus, Camera, Monitor,
  MapLocation, DocumentCopy, VideoPlay,
  Download
} from '@element-plus/icons-vue'

const router = useRouter()
const devices = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const searchQuery = ref('')
const openActionSections = ref(new Set(['dataQuery']))

const isActionSectionOpen = (section) => openActionSections.value.has(section)

const toggleActionSection = (section) => {
  const next = new Set(openActionSections.value)
  if (next.has(section)) {
    next.delete(section)
  } else {
    next.add(section)
  }
  openActionSections.value = next
}

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('添加设备')
const deviceFormRef = ref()
const deviceForm = reactive({
  id: null,
  imei: '',
  mcc: '',
  mnc: '',
  apn: '',
  iccid: '',
  imsi: ''
})
const dialogLoading = ref(false)

// 关联病人对话框
const assignDialogVisible = ref(false)
const assignFormRef = ref()
const assignForm = reactive({
  deviceId: null,
  patientId: null
})
const assignLoading = ref(false)
const availablePatients = ref([])

const rules = {
  imei: [
    { required: true, message: '请输入设备 IMEI', trigger: 'blur' },
    { min: 15, max: 15, message: 'IMEI 必须为 15 位', trigger: 'blur' }
  ]
}

// 关联病人规则
const assignRules = {
  patientId: [
    { required: true, message: '请选择病人', trigger: 'change' }
  ]
}

// 下发指令对话框
const commandDialogVisible = ref(false)
const commandFormRef = ref()
const commandForm = reactive({
  deviceId: null,
  imei: '',
  command: ''
})
const commandLoading = ref(false)

// 下发指令规则
const commandRules = {
  command: [
    { required: true, message: '请输入指令', trigger: 'blur' }
  ]
}

// 原始日志对话框相关
const rawLogDialogVisible = ref(false)
const rawLogLoading = ref(false)
const rawLogContent = ref('')
const rawLogTotal = ref(0)
const rawLogForm = reactive({
  dateRange: [],
  keyword: '',
  page: 1,
  size: 100,
  deviceId: null,
  imei: ''
})

// 查询原始日志
const handleQueryRawLogs = async () => {
  if (!rawLogForm.imei) {
    ElMessage.error('设备IMEI不存在')
    return
  }
  
  // 检查时间范围
  if (!rawLogForm.dateRange || rawLogForm.dateRange.length !== 2) {
    ElMessage.error('请选择时间范围')
    return
  }
  
  rawLogLoading.value = true
  try {
    // 设置查询时间范围
    const startDate = new Date(rawLogForm.dateRange[0])
    const endDate = new Date(rawLogForm.dateRange[1])
    const startTime = new Date(startDate.setHours(0, 0, 0, 0)).toISOString()
    const endTime = new Date(endDate.setHours(23, 59, 59, 999)).toISOString()
    
    const response = await downlinkApi.getRawLogs(
      rawLogForm.imei, 
      startTime, 
      endTime,
      rawLogForm.page,
      rawLogForm.size,
      rawLogForm.keyword
    )
    rawLogContent.value = response.logs || ''
    rawLogTotal.value = response.total || 0
  } catch (error) {
    console.error('Failed to get raw logs:', error)
    ElMessage.error('获取原始日志失败')
  } finally {
    rawLogLoading.value = false
  }
}

// 重置原始日志查询
const handleResetRawLogs = () => {
  // 设置默认时间范围为最近7天
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 7)
  rawLogForm.dateRange = [start, end]
  rawLogForm.keyword = ''
  rawLogForm.page = 1
  rawLogForm.size = 100
  rawLogContent.value = ''
  rawLogTotal.value = 0
}

// 打开原始日志对话框
const handleOpenRawLogs = (device) => {
  rawLogForm.deviceId = device.id
  rawLogForm.imei = device.imei
  // 设置默认时间范围为最近7天
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 7)
  rawLogForm.dateRange = [start, end]
  rawLogForm.keyword = ''
  rawLogForm.page = 1
  rawLogForm.size = 100
  rawLogContent.value = ''
  rawLogTotal.value = 0
  rawLogDialogVisible.value = true
}

// 格式化日期
const formatDate = (row, column, cellValue) => {
  if (!cellValue) return ''
  return new Date(cellValue).toLocaleString()
}

// 获取设备列表
const fetchDevices = async () => {
  loading.value = true
  try {
    const offset = (currentPage.value - 1) * pageSize.value
    console.log('Fetching devices with limit:', pageSize.value, 'offset:', offset)
    const data = await deviceApi.getDevices(pageSize.value, offset, searchQuery.value.trim())
    console.log('Devices API response:', data)
    if (data && Array.isArray(data.content)) {
      devices.value = data.content
      total.value = data.total || 0
    } else if (data && Array.isArray(data.list)) {
      devices.value = data.list
      total.value = data.total || 0
    } else if (data && Array.isArray(data.items)) {
      devices.value = data.items
      total.value = data.total || data.items.length
    } else if (Array.isArray(data)) {
      devices.value = data
      total.value = data.length
    } else {
      devices.value = []
      total.value = 0
      console.warn('Unexpected devices data format:', data)
    }
    console.log('Devices list updated:', devices.value)
    console.log('Total devices:', total.value)
  } catch (error) {
    console.error('Failed to fetch devices:', error)
    console.error('Error config:', error.config)
    console.error('Error response:', error.response)
    console.error('Error message:', error.message)
    devices.value = []
    total.value = 0
    ElMessage.error('获取设备列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索设备
const handleSearch = () => {
  currentPage.value = 1
  fetchDevices()
}

// 分页大小变化
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchDevices()
}

// 当前页码变化
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchDevices()
}

// 添加设备
const handleAddDevice = () => {
  dialogTitle.value = '添加设备'
  resetForm()
  dialogVisible.value = true
}

// 编辑设备
const handleEditDevice = (device) => {
  dialogTitle.value = '编辑设备'
  Object.assign(deviceForm, device)
  dialogVisible.value = true
}

// 查看设备详情
const handleViewDevice = (id) => {
  router.push(`/devices/${id}`)
}

// 查看历史轨迹
const handleViewHistory = (deviceId) => {
  router.push(`/history/${deviceId}`)
}

const handleDropdownCommand = (device, command) => {
  if (!device) return

  switch (command) {
    case 'viewDetail':
      handleViewDevice(device.id)
      return
    case 'historyTrack':
      handleViewHistory(device.id)
      return
    case 'openRealtime':
      router.push({ path: '/realtime', query: { deviceId: device.id } })
      return
    case 'deleteDevice':
      handleDeleteDevice(device.id)
      return
    case 'exportCurrentStatus':
      exportCurrentStatus(device)
      return
    default:
      handleCommand(device.id, command)
  }
}

const exportCurrentStatus = (device) => {
  const rows = [
    ['ID', 'IMEI', '在线状态', '关联病人', '电量', '最后定位时间', '纬度', '经度', 'ICCID', 'IMSI'],
    [
      device.id,
      device.imei,
      device.isOnline ? '在线' : '离线',
      device.patient?.name || '',
      device.batteryLevel ?? '',
      device.lastLocationTime || '',
      device.lastLatitude ?? '',
      device.lastLongitude ?? '',
      device.iccid || '',
      device.imsi || ''
    ]
  ]
  const csv = rows.map(row => row.map(formatCsvValue).join(',')).join('\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `device_${device.imei || device.id}_current_status.csv`
  link.click()
  window.URL.revokeObjectURL(url)
  ElMessage.success('当前设备状态已导出')
}

const formatCsvValue = (value) => {
  const text = String(value ?? '')
  return `"${text.replace(/"/g, '""')}"`
}

// 设备控制方法
const handleCommand = async (deviceId, commandType) => {
  const device = devices.value.find(d => d.id === deviceId)
  if (!device) {
    ElMessage.error('设备不存在')
    return
  }
  
  const imei = device.imei
  if (!imei) {
    ElMessage.error('设备IMEI不存在')
    return
  }
  
  try {
    let response
    
    switch (commandType) {
      case 'sendCommand':
        // 显示下发指令对话框
        commandForm.deviceId = deviceId
        commandForm.imei = imei
        commandForm.command = ''
        commandDialogVisible.value = true
        return
      case 'shortCommand':
        // 短信命令
        const shortCmd = await ElMessageBox.prompt('请输入短信命令', '短信命令', {
          inputType: 'text'
        })
        response = await downlinkApi.sendShortCommand(imei, shortCmd.value)
        break
      case 'timezone':
        // 时区设置
        response = await downlinkApi.sendBP00(imei)
        break
      case 'timezoneNew':
        // 时区设置（新）
        const timezone = await ElMessageBox.prompt('请输入时区（如8）', '时区设置', {
          inputType: 'number'
        })
        response = await downlinkApi.sendBP00(imei, parseInt(timezone.value))
        break
      case 'locateNow':
        // 立即定位
        response = await downlinkApi.sendBP16(imei)
        break
      case 'sendMessage':
        // 发信息（使用BP40协议）
        const message = await ElMessageBox.prompt('请输入要发送的信息', '发送信息', {
          inputType: 'textarea'
        })
        response = await downlinkApi.sendBP40(imei, message.value)
        break
      case 'takePhoto':
        // 拍照
        response = await downlinkApi.sendBP46(imei)
        break
      case 'heartRate':
        // 心率监测
        response = await downlinkApi.sendBPXL(imei)
        break
      case 'bloodPressure':
        // 血压监测
        response = await downlinkApi.sendBPXY(imei)
        break
      case 'bloodOxygen':
        // 血氧监测
        response = await downlinkApi.sendBPXZ(imei)
        break
      case 'temperature':
        // 体温监测
        response = await downlinkApi.sendBPXX(imei)
        break
      case 'restart':
        // 重启
        await ElMessageBox.confirm('确定要重启该设备吗？', '重启设备', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        response = await downlinkApi.sendBP18(imei)
        break
      case 'shutdown':
        // 关机
        await ElMessageBox.confirm('确定要关闭该设备吗？', '关闭设备', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        response = await downlinkApi.sendBP31(imei)
        break
      case 'setSos':
        // 设置SOS码
        const sosNumbers = await ElMessageBox.prompt('请输入SOS号码（多个号码用逗号分隔）', '设置SOS码', {
          inputType: 'text'
        })
        response = await downlinkApi.sendBP12(imei, sosNumbers.value.split(','))
        break
      case 'realTimeTracking':
        // 实时追踪
        const interval = await ElMessageBox.prompt('请输入追踪间隔（秒）', '实时追踪', {
          inputType: 'number',
          inputValue: 5
        })
        response = await downlinkApi.startRealTimeTracking(imei, parseInt(interval.value))
        break
      case 'stopTracking':
        await ElMessageBox.confirm('确定要停止该设备的实时追踪吗？', '停止实时追踪', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        response = await downlinkApi.stopRealTimeTracking(imei)
        break
      case 'getLogs':
        response = await downlinkApi.getLogs(imei)
        console.log('Device logs:', response)
        ElMessage.success('获取日志成功')
        return
      case 'rawLogs':
        handleOpenRawLogs(device)
        return
      case 'alarmCalendar':
        const now = new Date()
        response = await downlinkApi.getAlarmCalendar(imei, now.getFullYear(), now.getMonth() + 1)
        console.log('Alarm calendar:', response)
        ElMessage.success('获取报警日历成功')
        return
      case 'batteryReport':
        response = await downlinkApi.getBatteryReport(imei)
        console.log('Battery report:', response)
        ElMessage.success('获取电池报告成功')
        return
      case 'exportStatusHistory':
        const endTime = new Date().toISOString()
        const startTime = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString()
        response = await downlinkApi.exportStatusHistory(imei, startTime, endTime)
        const blob = response instanceof Blob
          ? response
          : new Blob([response], { type: 'application/vnd.ms-excel' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `device_${imei}_status_history.xlsx`
        link.click()
        window.URL.revokeObjectURL(url)
        ElMessage.success('导出状态历史成功')
        return
      default:
        ElMessage.error('未知命令类型')
        return
    }
    
    if (response) {
      ElMessage.success('指令下发成功')
    }
  } catch (error) {
    console.error('Failed to send command:', error)
    if (error.response && error.response.status === 503) {
      ElMessage.error('设备不在线')
    } else if (error.name === 'CanceledError') {
      // 用户取消操作，不显示错误信息
    } else {
      ElMessage.error('指令下发失败')
    }
  }
}

// 发送指令
const handleSendCommand = async () => {
  if (!commandFormRef.value) return
  
  await commandFormRef.value.validate(async (valid) => {
    if (valid) {
      commandLoading.value = true
      try {
        const { imei, command } = commandForm
        // 调用下发指令的API
        await downlinkApi.sendCommand(imei, command)
        ElMessage.success('指令下发成功')
        commandDialogVisible.value = false
      } catch (error) {
        console.error('Failed to send command:', error)
        if (error.response && error.response.status === 503) {
          ElMessage.error('设备不在线')
        } else {
          ElMessage.error('指令下发失败')
        }
      } finally {
        commandLoading.value = false
      }
    }
  })
}



// 删除设备
const handleDeleteDevice = (id) => {
  ElMessageBox.confirm('确定要删除该设备吗？', '删除设备', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deviceApi.deleteDevice(id)
      ElMessage.success('设备删除成功')
      fetchDevices()
    } catch (error) {
      ElMessage.error('设备删除失败')
      console.error('Failed to delete device:', error)
    }
  }).catch(() => {
    // 取消删除
  })
}

// 获取可用病人
const fetchAvailablePatients = async () => {
  try {
    const data = await patientApi.getPatients(100, 0)
    console.log('获取到的病人数据:', data)
    
    // 确保返回的数据是数组，如果是对象则提取list或data字段
    let patientList = []
    if (Array.isArray(data)) {
      patientList = data
    } else if (data && Array.isArray(data.list)) {
      patientList = data.list
    } else if (data && Array.isArray(data.data)) {
      patientList = data.data
    }
    
    availablePatients.value = patientList
    console.log('处理后的病人列表:', availablePatients.value)
  } catch (error) {
    ElMessage.error('获取病人列表失败')
    console.error('Failed to fetch patients:', error)
  }
}

// 关联病人
const handleAssignPatient = (device) => {
  assignForm.deviceId = device.id
  assignForm.patientId = null
  fetchAvailablePatients()
  assignDialogVisible.value = true
}

const handleUnassignPatient = async (device) => {
  if (!device?.patient?.id) return

  try {
    await ElMessageBox.confirm('确定要取消该设备与病人的关联吗？', '取消关联', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await patientApi.unassignDevice(device.patient.id, device.id)
    ElMessage.success('设备关联已取消')
    fetchDevices()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error('取消关联失败')
    console.error('Failed to unassign device:', error)
  }
}

// 保存关联关系
const handleSaveAssign = async () => {
  if (!assignFormRef.value) return
  
  await assignFormRef.value.validate(async (valid) => {
    if (valid) {
      assignLoading.value = true
      try {
        await patientApi.assignDevice(assignForm.patientId, assignForm.deviceId)
        ElMessage.success('设备关联成功')
        assignDialogVisible.value = false
        fetchDevices()
      } catch (error) {
        ElMessage.error('设备关联失败')
        console.error('Failed to assign device:', error)
      } finally {
        assignLoading.value = false
      }
    }
  })
}

// 保存设备
const handleSaveDevice = async () => {
  if (!deviceFormRef.value) return
  
  await deviceFormRef.value.validate(async (valid) => {
    if (valid) {
      dialogLoading.value = true
      try {
        const deviceData = {
          imei: deviceForm.imei,
          mcc: deviceForm.mcc,
          mnc: deviceForm.mnc,
          apn: deviceForm.apn,
          iccid: deviceForm.iccid,
          imsi: deviceForm.imsi
        }
        
        if (deviceForm.id) {
          // 更新设备
          await deviceApi.updateDevice(deviceForm.id, deviceData)
          ElMessage.success('设备更新成功')
        } else {
          // 创建设备
          await deviceApi.createDevice(deviceData)
          ElMessage.success('设备添加成功')
        }
        
        dialogVisible.value = false
        fetchDevices()
      } catch (error) {
        ElMessage.error(deviceForm.id ? '设备更新失败' : '设备添加失败')
        console.error('Failed to save device:', error)
      } finally {
        dialogLoading.value = false
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  if (deviceFormRef.value) {
    deviceFormRef.value.resetFields()
  }
  deviceForm.id = null
  deviceForm.imei = ''
  deviceForm.mcc = ''
  deviceForm.mnc = ''
  deviceForm.apn = ''
  deviceForm.iccid = ''
  deviceForm.imsi = ''
}

// 初始化
onMounted(() => {
  fetchDevices()
})
</script>

<style scoped>
.devices-container {
  width: 100%;
  background: transparent;
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.devices-content {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 20px;
  backdrop-filter: blur(10px);
}

.search-input {
  margin-bottom: 20px;
  width: 300px;
}

.search-button {
  margin-left: 12px;
  margin-bottom: 20px;
}

.search-input :deep(.el-input__wrapper) {
  background: var(--bg-input);
  box-shadow: none;
  border: 1px solid var(--border-color);
}

.search-input :deep(.el-input__inner) {
  color: var(--text-primary);
}

.search-input :deep(.el-input__inner::placeholder) {
  color: var(--text-muted);
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.action-row .el-button + .el-button {
  margin-left: 0;
}

.action-button {
  height: 28px;
  border-radius: 999px;
  padding: 0 12px;
  flex: 0 0 auto;
}

.action-more {
  padding: 0 10px;
}

:global(.device-action-popper) {
  padding: 0 !important;
  border: 1px solid #e6edf6 !important;
  border-radius: 0 !important;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.14) !important;
  overflow: hidden;
}

:global(.device-action-popper .device-action-panel) {
  width: 180px;
  max-height: 720px;
  overflow-y: auto;
  background: #ffffff;
  color: #0f1f3a;
  padding: 8px 0;
  font-size: 14px;
}

:global(.device-action-popper .panel-action),
:global(.device-action-popper .section-toggle) {
  width: 100%;
  min-height: 42px;
  padding: 0 18px;
  border: 0;
  background: transparent;
  color: #0f1f3a;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  cursor: pointer;
  font: inherit;
}

:global(.device-action-popper .panel-action:hover),
:global(.device-action-popper .section-toggle:hover) {
  background: #f5f8fc;
  color: #0f62fe;
}

:global(.device-action-popper .panel-action.muted) {
  color: #a7b0bf;
  cursor: not-allowed;
}

:global(.device-action-popper .panel-action.muted:hover) {
  background: transparent;
  color: #a7b0bf;
}

:global(.device-action-popper .panel-action.featured) {
  color: #0f62fe;
}

:global(.device-action-popper .panel-icon) {
  width: 18px;
  font-size: 15px;
  color: #66758f;
  flex: 0 0 auto;
}

:global(.device-action-popper .featured .panel-icon) {
  color: #0f62fe;
}

:global(.device-action-popper .device-action-section) {
  border-top: 1px solid #edf2f7;
}

:global(.device-action-popper .section-toggle) {
  justify-content: space-between;
  font-weight: 500;
}

:global(.device-action-popper .section-toggle .el-icon) {
  transition: transform 0.16s ease;
}

:global(.device-action-popper .section-toggle .el-icon.expanded) {
  transform: rotate(180deg);
}

:global(.device-action-popper .section-items) {
  padding: 2px 0 8px;
}

:global(.device-action-popper .section-items .panel-action) {
  min-height: 40px;
  padding-left: 28px;
}

/* 原始日志对话框样式 */
.raw-log-header {
  margin-bottom: 20px;
}

.raw-log-content {
  max-height: 500px;
  overflow-y: auto;
  background-color: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 10px;
}

.raw-log-text {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  line-height: 1.5;
  color: var(--color-primary);
  margin: 0;
}

.empty-logs {
  text-align: center;
  color: var(--text-muted);
  padding: 20px;
}

/* 分页控件样式 */
.raw-log-pagination {
  margin-top: 15px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

/* 滚动条样式 */
.raw-log-content::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.raw-log-content::-webkit-scrollbar-track {
  background: transparent;
  border-radius: var(--radius-sm);
}

.raw-log-content::-webkit-scrollbar-thumb {
  background: var(--text-muted);
  border-radius: var(--radius-sm);
}

.raw-log-content::-webkit-scrollbar-thumb:hover {
  background: var(--text-secondary);
}
</style>
