<template>
  <div class="alarm-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>报警管理</h2>
          <div class="header-actions">
            <el-select v-model="filterStatus" placeholder="筛选状态" size="small" @change="fetchAlarms">
              <el-option label="全部" value="" />
              <el-option label="未处理" value="pending" />
              <el-option label="已处理" value="handled" />
            </el-select>
            <el-select v-model="alarmInterval" placeholder="同一病人报警间隔" size="small" @change="saveAlarmInterval">
              <el-option label="1分钟" :value="1" />
              <el-option label="5分钟" :value="5" />
              <el-option label="10分钟" :value="10" />
              <el-option label="15分钟" :value="15" />
              <el-option label="30分钟" :value="30" />
              <el-option label="60分钟" :value="60" />
            </el-select>
            <el-switch v-model="soundEnabled" active-text="声音提醒" inactive-text="" @change="toggleSound" />
            <el-button type="primary" size="small" @click="refreshAlarms">
              刷新
            </el-button>
          </div>
        </div>
      </template>
      <div class="alarm-content">
        <el-row :gutter="20">
          <el-col :span="14">
            <div class="alarm-list-panel">
              <h3>报警列表
                <el-badge v-if="pendingCount > 0" :value="pendingCount" class="pending-badge" />
              </h3>
              <el-input
                v-model="searchQuery"
                placeholder="搜索病人姓名或设备IMEI"
                prefix-icon="Search"
                class="search-input"
                clearable
              />
              <el-table :data="filteredAlarms" style="width: 100%" v-loading="loading"
                        @row-click="selectAlarm" :row-class-name="getRowClassName">
                <el-table-column label="报警类型" width="110">
                  <template #default="scope">
                    <el-tag :type="getAlarmTypeColor(scope.row)" size="small">
                      {{ getAlarmTypeName(scope.row) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="病人" width="140">
                  <template #default="scope">
                    <div class="patient-cell">
                      <span class="patient-name">{{ scope.row.patient ? scope.row.patient.name : '-' }}</span>
                      <span v-if="scope.row.patient && scope.row.patient.ward" class="patient-ward">{{ scope.row.patient.ward }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="设备IMEI" width="150">
                  <template #default="scope">
                    {{ scope.row.device ? scope.row.device.imei : '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="位置" min-width="120">
                  <template #default="scope">
                    {{ scope.row.address || '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="报警时间" width="160">
                  <template #default="scope">
                    {{ formatDate(scope.row.triggeredTime || scope.row.alertTime || scope.row.createdAt) }}
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template #default="scope">
                    <el-tag v-if="scope.row.status === 'pending'" :type="isOverdue(scope.row) ? 'danger' : 'warning'" size="small">
                      {{ isOverdue(scope.row) ? '超时' : '未处理' }}
                    </el-tag>
                    <el-tag v-else type="success" size="small">已处理</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120" fixed="right">
                  <template #default="scope">
                    <el-button type="primary" size="small" @click="handleAlarm(scope.row)">
                      {{ scope.row.status === 'pending' ? '处理' : '查看' }}
                    </el-button>
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
          </el-col>
          <el-col :span="10">
            <div class="alarm-detail-panel">
              <h3>报警详情</h3>
              <el-empty v-if="!selectedAlarm" description="请选择一条报警记录" />
              <div v-else class="alarm-detail">
                <el-descriptions title="基本信息" :column="1" border>
                  <el-descriptions-item label="报警类型">
                    <el-tag :type="getAlarmTypeColor(selectedAlarm)">
                      {{ getAlarmTypeName(selectedAlarm) }}
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="报警时间">{{ formatDate(selectedAlarm.triggeredTime || selectedAlarm.alertTime) }}</el-descriptions-item>
                  <el-descriptions-item label="报警状态">
                    <el-tag :type="selectedAlarm.status === 'pending' ? (isOverdue(selectedAlarm) ? 'danger' : 'warning') : 'success'">
                      {{ selectedAlarm.status === 'pending' ? (isOverdue(selectedAlarm) ? '超时未处理' : '未处理') : '已处理' }}
                    </el-tag>
                  </el-descriptions-item>
                </el-descriptions>

                <el-descriptions title="病人信息" :column="1" border style="margin-top: 20px;">
                  <el-descriptions-item label="姓名">{{ selectedAlarm.patient ? selectedAlarm.patient.name : '-' }}</el-descriptions-item>
                  <el-descriptions-item label="年龄">{{ selectedAlarm.patient ? selectedAlarm.patient.age : '-' }}</el-descriptions-item>
                  <el-descriptions-item label="性别">{{ selectedAlarm.patient ? (selectedAlarm.patient.gender === 'male' ? '男' : '女') : '-' }}</el-descriptions-item>
                  <el-descriptions-item label="病房">{{ selectedAlarm.patient ? selectedAlarm.patient.ward : '-' }}</el-descriptions-item>
                  <el-descriptions-item label="床位">{{ selectedAlarm.patient ? selectedAlarm.patient.bedNumber : '-' }}</el-descriptions-item>
                </el-descriptions>

                <el-descriptions title="设备信息" :column="1" border style="margin-top: 20px;">
                  <el-descriptions-item label="设备IMEI">{{ selectedAlarm.device ? selectedAlarm.device.imei : '-' }}</el-descriptions-item>
                </el-descriptions>

                <el-descriptions title="位置信息" :column="1" border style="margin-top: 20px;">
                  <el-descriptions-item label="地址">{{ selectedAlarm.address || '-' }}</el-descriptions-item>
                </el-descriptions>

                <div v-if="selectedAlarm.status === 'pending'" class="quick-handle-section">
                  <h4>快捷处理</h4>
                  <div class="quick-actions">
                    <el-button type="success" @click="quickHandle('已通知医生')">已通知医生</el-button>
                    <el-button type="primary" @click="quickHandle('病人已返回')">病人已返回</el-button>
                    <el-button type="warning" @click="quickHandle('误报')">误报</el-button>
                    <el-button type="info" @click="quickHandle('需进一步处理')">需进一步处理</el-button>
                  </div>
                  <el-button class="custom-handle-btn" @click="openHandleDialog">自定义处理</el-button>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 自定义处理报警对话框 -->
    <el-dialog v-model="handleDialogVisible" title="处理报警" width="400px">
      <el-form :model="handleForm" ref="handleFormRef" label-width="80px">
        <el-form-item label="处理结果">
          <el-radio-group v-model="handleForm.result">
            <el-radio-button value="已处理">已处理</el-radio-button>
            <el-radio-button value="误报">误报</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input v-model="handleForm.remark" type="textarea" placeholder="请输入处理备注" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveHandleResult" :loading="handleLoading">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { alarmApi } from '../api/alarm'
import { ElMessage } from 'element-plus'
import { formatBeijingTime } from '../utils/time'

const alarms = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const searchQuery = ref('')
const filterStatus = ref('')
const selectedAlarm = ref(null)
const alarmInterval = ref(5)
const soundEnabled = ref(true)
let refreshTimer = null
let audioContext = null

const handleDialogVisible = ref(false)
const handleFormRef = ref()
const handleForm = reactive({ result: '已处理', remark: '' })
const handleLoading = ref(false)

const OVERDUE_MINUTES = 15

/**
 * 计算未处理报警数
 */
const pendingCount = computed(() => alarms.value.filter(a => a.status === 'pending').length)

/**
 * 搜索过滤后的报警列表
 */
const filteredAlarms = computed(() => {
  if (!searchQuery.value) return alarms.value
  const query = searchQuery.value.toLowerCase()
  return alarms.value.filter(a => {
    const patientName = a.patient?.name?.toLowerCase() || ''
    const imei = a.device?.imei?.toLowerCase() || ''
    return patientName.includes(query) || imei.includes(query)
  })
})

/**
 * 判断报警是否超时未处理
 */
const isOverdue = (alarm) => {
  if (alarm.status !== 'pending') return false
  const triggeredTime = new Date(alarm.triggeredTime || alarm.alertTime || alarm.createdAt)
  const now = new Date()
  return (now - triggeredTime) > OVERDUE_MINUTES * 60 * 1000
}

/**
 * 获取行样式类名
 */
const getRowClassName = ({ row }) => {
  if (row.status === 'pending' && isOverdue(row)) return 'alarm-row-overdue'
  if (row.status === 'pending') return 'alarm-row-pending'
  return ''
}

/**
 * 播放报警提示音
 */
const playAlarmSound = () => {
  if (!soundEnabled.value) return
  try {
    if (!audioContext) {
      audioContext = new (window.AudioContext || window.webkitAudioContext)()
    }
    const oscillator = audioContext.createOscillator()
    const gainNode = audioContext.createGain()
    oscillator.connect(gainNode)
    gainNode.connect(audioContext.destination)
    oscillator.frequency.value = 800
    oscillator.type = 'sine'
    gainNode.gain.value = 0.3
    oscillator.start()
    setTimeout(() => {
      oscillator.frequency.value = 600
      setTimeout(() => {
        oscillator.frequency.value = 800
        setTimeout(() => oscillator.stop(), 200)
      }, 200)
    }, 200)
  } catch (e) {
    // 忽略音频播放失败
  }
}

/**
 * 发送浏览器通知
 */
const sendNotification = (alarm) => {
  if (!('Notification' in window)) return
  if (Notification.permission === 'granted') {
    new Notification('新报警提醒', {
      body: `${alarm.patient?.name || '未知病人'} - ${getAlarmTypeName(alarm)}`,
      icon: '/favicon.ico'
    })
  } else if (Notification.permission !== 'denied') {
    Notification.requestPermission()
  }
}

/**
 * 切换声音提醒
 */
const toggleSound = () => {
  localStorage.setItem('alarmSoundEnabled', soundEnabled.value ? '1' : '0')
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  return formatBeijingTime(dateString)
}

const getAlarmTypeName = (alarm) => {
  const type = alarm.alertType || alarm.alarmType
  const typeMap = {
    'fence_breach': '围栏越界',
    'low_battery': '低电量',
    'sos': 'SOS报警',
    'fall': '跌倒报警',
    'heart_rate': '心率异常',
    'blood_pressure_abnormal': '血压异常',
    'blood_oxygen_abnormal': '血氧异常',
    'body_temperature_abnormal': '体温异常',
    'heart_rate_abnormal': '心率异常'
  }
  return typeMap[type] || '未知类型'
}

const getAlarmTypeColor = (alarm) => {
  const type = alarm.alertType || alarm.alarmType
  const colorMap = {
    'fence_breach': 'danger',
    'low_battery': 'warning',
    'sos': 'danger',
    'fall': 'danger',
    'heart_rate': 'danger',
    'blood_pressure_abnormal': 'danger',
    'blood_oxygen_abnormal': 'danger',
    'body_temperature_abnormal': 'danger',
    'heart_rate_abnormal': 'danger'
  }
  return colorMap[type] || 'info'
}

/**
 * 获取报警列表
 */
const fetchAlarms = async () => {
  loading.value = true
  try {
    const offset = (currentPage.value - 1) * pageSize.value
    const status = filterStatus.value || null
    const result = await alarmApi.getAlarms(pageSize.value, offset, status)

    const prevPendingIds = new Set(alarms.value.filter(a => a.status === 'pending').map(a => a.id))

    if (Array.isArray(result)) {
      alarms.value = result
      total.value = result.length || 0
    } else if (result && result.content) {
      alarms.value = result.content
      total.value = result.total || result.totalElements || 0
    } else {
      alarms.value = []
      total.value = 0
    }

    const newPending = alarms.value.filter(a => a.status === 'pending' && !prevPendingIds.has(a.id))
    if (newPending.length > 0) {
      playAlarmSound()
      sendNotification(newPending[0])
    }
  } catch (error) {
    ElMessage.error('获取报警列表失败')
  } finally {
    loading.value = false
  }
}

const saveAlarmInterval = () => {
  localStorage.setItem('alarmInterval', alarmInterval.value)
  ElMessage.success(`报警间隔已设置为${alarmInterval.value}分钟`)
}

const loadAlarmInterval = () => {
  const savedInterval = localStorage.getItem('alarmInterval')
  if (savedInterval) alarmInterval.value = parseInt(savedInterval)
  const savedSound = localStorage.getItem('alarmSoundEnabled')
  if (savedSound !== null) soundEnabled.value = savedSound === '1'
}

const refreshAlarms = () => { fetchAlarms() }

const selectAlarm = (row) => { selectedAlarm.value = row }

const handleAlarm = (alarm) => {
  selectedAlarm.value = alarm
  if (alarm.status === 'pending') {
    handleDialogVisible.value = true
  }
}

/**
 * 打开自定义处理对话框
 */
const openHandleDialog = () => {
  handleForm.result = '已处理'
  handleForm.remark = ''
  handleDialogVisible.value = true
}

/**
 * 快捷处理报警
 */
const quickHandle = async (result) => {
  if (!selectedAlarm.value) return
  try {
    await alarmApi.handleAlarm(selectedAlarm.value.id, result)
    ElMessage.success('报警处理成功')
    selectedAlarm.value.status = 'handled'
    fetchAlarms()
  } catch (error) {
    ElMessage.error('报警处理失败')
  }
}

/**
 * 保存自定义处理结果
 */
const saveHandleResult = async () => {
  if (!selectedAlarm.value) return
  handleLoading.value = true
  try {
    await alarmApi.handleAlarm(selectedAlarm.value.id, handleForm.result)
    ElMessage.success('报警处理成功')
    handleDialogVisible.value = false
    selectedAlarm.value.status = 'handled'
    fetchAlarms()
  } catch (error) {
    ElMessage.error('报警处理失败')
  } finally {
    handleLoading.value = false
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchAlarms()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchAlarms()
}

onMounted(() => {
  loadAlarmInterval()
  fetchAlarms()
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission()
  }
  refreshTimer = setInterval(fetchAlarms, 30000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.alarm-container { width: 100%; padding: 20px; background: transparent; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; gap: 10px; align-items: center; }
.alarm-content { padding: 20px 0; }

.alarm-list-panel {
  background: var(--bg-card); border: 1px solid var(--border-color);
  padding: 15px; border-radius: var(--radius-lg); backdrop-filter: blur(10px);
}
.alarm-list-panel h3 { margin: 0 0 15px 0; font-size: 16px; font-weight: bold; color: var(--text-primary); display: flex; align-items: center; gap: 8px; }
.pending-badge { margin-left: 8px; }
.search-input { margin-bottom: 20px; width: 300px; }

.patient-cell { display: flex; flex-direction: column; }
.patient-name { font-weight: 500; color: var(--text-primary); }
.patient-ward { font-size: 12px; color: var(--text-muted); }

:deep(.alarm-row-pending) { background-color: rgba(230, 162, 60, 0.05); }
:deep(.alarm-row-overdue) { background-color: rgba(245, 108, 108, 0.1); }

.alarm-detail-panel {
  background: var(--bg-card); border: 1px solid var(--border-color);
  padding: 15px; border-radius: var(--radius-lg); height: 650px;
  overflow-y: auto; backdrop-filter: blur(10px);
}
.alarm-detail-panel h3 { margin: 0 0 15px 0; font-size: 16px; font-weight: bold; color: var(--text-primary); }
.alarm-detail { overflow-y: auto; max-height: 580px; }

.quick-handle-section { margin-top: 20px; padding: 16px; background: rgba(0,0,0,0.02); border-radius: 8px; }
.quick-handle-section h4 { margin: 0 0 12px 0; font-size: 14px; color: var(--text-primary); }
.quick-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.custom-handle-btn { width: 100%; }

.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
