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
        />
        <el-table :data="devices" style="width: 100%" v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="imei" label="IMEI" width="180" />
          <el-table-column prop="isOnline" label="在线状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.isOnline ? 'success' : 'danger'" size="small">
                {{ scope.row.isOnline ? '在线' : '离线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="patient.name" label="关联病人" width="120">
            <template #default="scope">
              {{ scope.row.patient?.name || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="mcc" label="MCC" width="80" />
          <el-table-column prop="mnc" label="MNC" width="80" />
          <el-table-column prop="apn" label="APN" width="120" />
          <el-table-column prop="iccid" label="ICCID" width="180" />
          <el-table-column prop="imsi" label="IMSI" width="180" />
          <el-table-column prop="createdAt" label="创建时间" width="180" :formatter="formatDate" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="scope">
              <el-button type="primary" size="small" @click="handleEditDevice(scope.row)">
                编辑
              </el-button>
              <el-button 
                type="success" 
                size="small" 
                @click="handleAssignPatient(scope.row)"
                :disabled="!!scope.row.patient"
              >
                {{ scope.row.patient ? '已关联' : '关联病人' }}
              </el-button>
              <el-dropdown trigger="click" @click.stop>
                <el-button size="small" type="primary" plain>
                  更多操作
                  <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-menu class="custom-dropdown-menu" mode="vertical" background-color="#1a202c" text-color="#e2e8f0" active-text-color="#409eff">
                    <!-- 设备控制组 -->
                    <el-sub-menu index="device-control">
                      <template #title>
                        <span>设备控制</span>
                      </template>
                      <el-menu-item index="sendCommand" @click="handleCommand(scope.row.id, 'sendCommand')">
                        <el-icon class="menu-icon"><Operation /></el-icon>
                        下发指令
                      </el-menu-item>
                      <el-menu-item index="locateNow" @click="handleCommand(scope.row.id, 'locateNow')">
                        <el-icon class="menu-icon"><Location /></el-icon>
                        立即定位
                      </el-menu-item>
                      <el-menu-item index="restart" @click="handleCommand(scope.row.id, 'restart')">
                        <el-icon class="menu-icon"><RefreshRight /></el-icon>
                        重启
                      </el-menu-item>
                      <el-menu-item index="shutdown" @click="handleCommand(scope.row.id, 'shutdown')">
                        <el-icon class="menu-icon"><SwitchButton /></el-icon>
                        关机
                      </el-menu-item>
                    </el-sub-menu>
                    
                    <!-- 通信组 -->
                    <el-sub-menu index="communication">
                      <template #title>
                        <span>通信功能</span>
                      </template>
                      <el-menu-item index="shortCommand" @click="handleCommand(scope.row.id, 'shortCommand')">
                        <el-icon class="menu-icon"><Message /></el-icon>
                        短信命令
                      </el-menu-item>
                      <el-menu-item index="sendMessage" @click="handleCommand(scope.row.id, 'sendMessage')">
                        <el-icon class="menu-icon"><ChatDotRound /></el-icon>
                        发信息
                      </el-menu-item>
                    </el-sub-menu>
                    
                    <!-- 配置组 -->
                    <el-sub-menu index="configuration">
                      <template #title>
                        <span>设备配置</span>
                      </template>
                      <el-menu-item index="initialConfig" @click="handleCommand(scope.row.id, 'initialConfig')">
                        <el-icon class="menu-icon"><Setting /></el-icon>
                        初始配置
                      </el-menu-item>
                      <el-menu-item index="timezone" @click="handleCommand(scope.row.id, 'timezone')">
                        <el-icon class="menu-icon"><Clock /></el-icon>
                        时区
                      </el-menu-item>
                      <el-menu-item index="timezoneNew" @click="handleCommand(scope.row.id, 'timezoneNew')">
                        <el-icon class="menu-icon"><Clock /></el-icon>
                        时区 New
                      </el-menu-item>
                      <el-menu-item index="setSos" @click="handleCommand(scope.row.id, 'setSos')">
                        <el-icon class="menu-icon"><Warning /></el-icon>
                        设置SOS码
                      </el-menu-item>
                      <el-menu-item index="setAlarm" @click="handleCommand(scope.row.id, 'setAlarm')">
                        <el-icon class="menu-icon"><Bell /></el-icon>
                        设置闹钟
                      </el-menu-item>
                    </el-sub-menu>
                    
                    <!-- 健康监测组 -->
                    <el-sub-menu index="health-monitoring">
                      <template #title>
                        <span>健康监测</span>
                      </template>
                      <el-menu-item index="heartRate" @click="handleCommand(scope.row.id, 'heartRate')">
                        <el-icon class="menu-icon"><Connection /></el-icon>
                        心率
                      </el-menu-item>
                      <el-menu-item index="bloodPressure" @click="handleCommand(scope.row.id, 'bloodPressure')">
                        <el-icon class="menu-icon"><Document /></el-icon>
                        血压
                      </el-menu-item>
                      <el-menu-item index="bloodOxygen" @click="handleCommand(scope.row.id, 'bloodOxygen')">
                        <el-icon class="menu-icon"><CirclePlus /></el-icon>
                        血氧
                      </el-menu-item>
                      <el-menu-item index="temperature" @click="handleCommand(scope.row.id, 'temperature')">
                        <el-icon class="menu-icon"><Monitor /></el-icon>
                        体温
                      </el-menu-item>
                      <el-menu-item index="takePhoto" @click="handleCommand(scope.row.id, 'takePhoto')">
                        <el-icon class="menu-icon"><Camera /></el-icon>
                        拍照
                      </el-menu-item>
                    </el-sub-menu>
                    
                    <!-- 数据查询组 -->
                    <el-sub-menu index="data-query">
                      <template #title>
                        <span>数据查询</span>
                      </template>
                      <el-menu-item index="historyTrack" @click="handleViewHistory(scope.row.id)">
                        <el-icon class="menu-icon"><MapLocation /></el-icon>
                        历史轨迹
                      </el-menu-item>
                      <el-menu-item index="getLogs" @click="handleCommand(scope.row.id, 'getLogs')">
                        <el-icon class="menu-icon"><DocumentCopy /></el-icon>
                        获取日志
                      </el-menu-item>
                      <el-menu-item index="rawLogs" @click="handleCommand(scope.row.id, 'rawLogs')">
                        <el-icon class="menu-icon"><Document /></el-icon>
                        原始日志
                      </el-menu-item>
                      <el-menu-item index="alarmCalendar" @click="handleCommand(scope.row.id, 'alarmCalendar')">
                        <el-icon class="menu-icon"><Calendar /></el-icon>
                        报警日历
                      </el-menu-item>
                      <el-menu-item index="batteryReport" @click="handleCommand(scope.row.id, 'batteryReport')">
                        <el-icon class="menu-icon"><Document /></el-icon>
                        电池报告
                      </el-menu-item>
                      <el-menu-item index="realTimeTracking" @click="handleCommand(scope.row.id, 'realTimeTracking')">
                        <el-icon class="menu-icon"><VideoPlay /></el-icon>
                        实时追踪
                      </el-menu-item>
                      <el-menu-item index="exportStatusHistory" @click="handleCommand(scope.row.id, 'exportStatusHistory')">
                        <el-icon class="menu-icon"><Download /></el-icon>
                        导出状态历史
                      </el-menu-item>
                    </el-sub-menu>
                  </el-menu>
                </template>
              </el-dropdown>
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
  Message, ChatDotRound, Setting, Clock, Warning, Bell, 
  Connection, Document, CirclePlus, Camera, 
  MapLocation, DocumentCopy, Calendar, VideoPlay, 
  Download
} from '@element-plus/icons-vue'

const router = useRouter()
const devices = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const searchQuery = ref('')

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
    const data = await deviceApi.getDevices(pageSize.value, offset)
    console.log('Devices API response:', data)
    // 检查返回数据结构，后端返回格式为 { list: [], total: 0 }
    if (data && Array.isArray(data.list)) {
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
  // 这里可以添加搜索逻辑
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
      case 'initialConfig':
        // 初始配置
        response = await downlinkApi.sendInitialConfig(imei, {})
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
      case 'getLogs':
        // 获取日志
        response = await downlinkApi.getLogs(imei)
        // 处理日志数据
        console.log('Device logs:', response)
        ElMessage.success('获取日志成功')
        return
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
      case 'setAlarm':
        // 设置闹钟
        const alarmConfig = await ElMessageBox.prompt('请输入闹钟配置（JSON格式）', '设置闹钟', {
          inputType: 'textarea'
        })
        response = await downlinkApi.setAlarm(imei, JSON.parse(alarmConfig.value))
        break
      case 'realTimeTracking':
        // 实时追踪
        const interval = await ElMessageBox.prompt('请输入追踪间隔（秒）', '实时追踪', {
          inputType: 'number',
          default: 5
        })
        response = await downlinkApi.startRealTimeTracking(imei, parseInt(interval.value))
        break
      case 'alarmCalendar':
        // 报警日历
        const now = new Date()
        response = await downlinkApi.getAlarmCalendar(imei, now.getFullYear(), now.getMonth() + 1)
        // 处理报警日历数据
        console.log('Alarm calendar:', response)
        ElMessage.success('获取报警日历成功')
        return
      case 'rawLogs':
        // 原始日志
        // 打开原始日志对话框
        handleOpenRawLogs(device)
        return
      case 'batteryReport':
        // 电池报告
        response = await downlinkApi.getBatteryReport(imei)
        // 处理电池报告数据
        console.log('Battery report:', response)
        ElMessage.success('获取电池报告成功')
        return
      case 'exportStatusHistory':
        // 导出状态历史
        response = await downlinkApi.exportStatusHistory(imei, new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(), new Date().toISOString())
        // 处理导出文件
        const blob = new Blob([response], { type: 'application/vnd.ms-excel' })
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `device_${imei}_status_history.xlsx`
        a.click()
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
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.devices-content {
  padding: 20px 0;
}

.search-input {
  margin-bottom: 20px;
  width: 300px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 自定义下拉菜单样式 - 深色主题 */
:deep(.custom-dropdown-menu) {
  min-width: 220px;
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  border: 1px solid #2d3748;
  background-color: #1a202c;
  overflow: hidden;
}

/* 分组标题样式 */
:deep(.dropdown-group-title) {
  padding: 8px 16px;
  background-color: #2d3748;
  color: #cbd5e0;
  font-size: 12px;
  font-weight: 600;
  border-bottom: 1px solid #4a5568;
  letter-spacing: 0.5px;
}

/* 菜单项样式 */
:deep(.el-dropdown-menu__item) {
  padding: 10px 16px;
  height: auto;
  line-height: 20px;
  font-size: 14px;
  color: #e2e8f0;
  display: flex;
  align-items: center;
  transition: all 0.2s ease;
  border-radius: 0;
  background-color: transparent;
}

:deep(.el-dropdown-menu__item:hover) {
  background-color: #2d3748;
  color: #409eff;
}

/* 图标样式 */
:deep(.menu-icon) {
  font-size: 16px;
  margin-right: 8px;
  width: 18px;
  text-align: center;
  color: #a0aec0;
  transition: color 0.2s ease;
}

:deep(.el-dropdown-menu__item:hover .menu-icon) {
  color: #409eff;
}

/* 修复分组标题与菜单项间距 */
:deep(.el-dropdown-menu__item) {
  margin: 0;
}

/* 调整第一个菜单项的上间距 */
:deep(.el-dropdown-menu > .el-dropdown-item:first-child) {
  margin-top: 0;
}

/* 原始日志对话框样式 */
.raw-log-header {
  margin-bottom: 20px;
}

.raw-log-content {
  max-height: 500px;
  overflow-y: auto;
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px;
}

.raw-log-text {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  line-height: 1.5;
  color: #303133;
  margin: 0;
}

.empty-logs {
  text-align: center;
  color: #909399;
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
  background: #f1f1f1;
  border-radius: 4px;
}

.raw-log-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.raw-log-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>
