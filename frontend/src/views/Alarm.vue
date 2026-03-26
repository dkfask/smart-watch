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
              <h3>报警列表</h3>
              <el-input
                v-model="searchQuery"
                placeholder="搜索设备IMEI或病人姓名"
                prefix-icon="Search"
                class="search-input"
                clearable
              />
              <el-table :data="alarms" style="width: 100%" v-loading="loading" @row-click="selectAlarm">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column label="报警类型" width="120">
                  <template #default="scope">
                    <el-tag :type="getAlarmTypeColor(scope.row)" size="small">
                      {{ getAlarmTypeName(scope.row) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="设备IMEI" width="180">
                  <template #default="scope">
                    {{ scope.row.device ? scope.row.device.imei : '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="病人" width="120">
                  <template #default="scope">
                    {{ scope.row.patient ? scope.row.patient.name : '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="位置" min-width="150">
                  <template #default="scope">
                    {{ scope.row.address || '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="报警时间" width="180">
                  <template #default="scope">
                    {{ formatDate(scope.row.triggeredTime || scope.row.alertTime || scope.row.createdAt) }}
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="100">
                  <template #default="scope">
                    <el-tag :type="scope.row.status === 'pending' ? 'warning' : 'success'" size="small">
                      {{ scope.row.status === 'pending' ? '未处理' : '已处理' }}
                    </el-tag>
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
                  <el-descriptions-item label="报警ID">{{ selectedAlarm.id }}</el-descriptions-item>
                  <el-descriptions-item label="报警类型">
                    <el-tag :type="getAlarmTypeColor(selectedAlarm)">
                      {{ getAlarmTypeName(selectedAlarm) }}
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="报警时间">{{ formatDate(selectedAlarm.triggeredTime) }}</el-descriptions-item>
                  <el-descriptions-item label="报警状态">
                    <el-tag :type="selectedAlarm.status === 'pending' ? 'warning' : 'success'">
                      {{ selectedAlarm.status === 'pending' ? '未处理' : '已处理' }}
                    </el-tag>
                  </el-descriptions-item>
                </el-descriptions>

                <el-descriptions title="设备信息" :column="1" border style="margin-top: 20px;">
                  <el-descriptions-item label="设备ID">{{ selectedAlarm.deviceId || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="设备IMEI">{{ selectedAlarm.device ? selectedAlarm.device.imei : '-' }}</el-descriptions-item>
                </el-descriptions>

                <el-descriptions title="病人信息" :column="1" border style="margin-top: 20px;">
                  <el-descriptions-item label="姓名">{{ selectedAlarm.patient ? selectedAlarm.patient.name : '-' }}</el-descriptions-item>
                  <el-descriptions-item label="年龄">{{ selectedAlarm.patient ? selectedAlarm.patient.age : '-' }}</el-descriptions-item>
                  <el-descriptions-item label="性别">{{ selectedAlarm.patient ? (selectedAlarm.patient.gender === 'male' ? '男' : '女') : '-' }}</el-descriptions-item>
                  <el-descriptions-item label="病房">{{ selectedAlarm.patient ? selectedAlarm.patient.ward : '-' }}</el-descriptions-item>
                  <el-descriptions-item label="床位">{{ selectedAlarm.patient ? selectedAlarm.patient.bedNumber : '-' }}</el-descriptions-item>
                </el-descriptions>

                <el-descriptions title="位置信息" :column="1" border style="margin-top: 20px;">
                  <el-descriptions-item label="经度">{{ selectedAlarm.longitude || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="纬度">{{ selectedAlarm.latitude || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="地址">{{ selectedAlarm.address || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="精度">{{ '-' }}m</el-descriptions-item>
                </el-descriptions>

                <div v-if="selectedAlarm.status === 'pending'" class="alarm-handle-actions">
                  <el-button type="primary" @click="handleAlarm(selectedAlarm)">
                    处理此报警
                  </el-button>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 处理报警对话框 -->
    <el-dialog v-model="handleDialogVisible" title="处理报警" width="400px">
      <el-form :model="handleForm" :rules="handleRules" ref="handleFormRef" label-width="80px">
        <el-form-item label="处理结果">
          <el-radio-group v-model="handleForm.result">
            <el-radio-button label="已处理">已处理</el-radio-button>
            <el-radio-button label="误报">误报</el-radio-button>
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
import { ref, reactive, onMounted, watch } from 'vue'
import { alarmApi } from '../api/alarm'
import { ElMessage } from 'element-plus'

const alarms = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const searchQuery = ref('')
const filterStatus = ref('')
const selectedAlarm = ref(null)

// 报警间隔设置
const alarmInterval = ref(5) // 默认5分钟

// 处理报警对话框
const handleDialogVisible = ref(false)
const handleFormRef = ref()
const handleForm = reactive({
  result: '已处理',
  remark: ''
})
const handleLoading = ref(false)

const handleRules = {
  result: [
    { required: true, message: '请选择处理结果', trigger: 'change' }
  ]
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString()
}

// 获取报警类型名称
const getAlarmTypeName = (alarm) => {
  // 获取报警类型，处理两种可能的字段名：alertType（FenceAlert）和alarmType（Alarm）
  const type = alarm.alertType || alarm.alarmType
  console.log('Alarm type received:', type)
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

// 获取报警类型颜色
const getAlarmTypeColor = (alarm) => {
  // 获取报警类型，处理两种可能的字段名：alertType（FenceAlert）和alarmType（Alarm）
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

// 获取报警列表
const fetchAlarms = async () => {
  loading.value = true
  try {
    const offset = (currentPage.value - 1) * pageSize.value
    const status = filterStatus.value || null
    const result = await alarmApi.getAlarms(pageSize.value, offset, status)
    console.log('Alarm API result:', result)
    
    // 处理API返回结果，可能是数组或分页对象
    if (Array.isArray(result)) {
      // 如果返回的是数组，直接使用
      alarms.value = result
      total.value = result.length || 0
      console.log('Alarms after processing (array case):', alarms.value)
      // 添加调试信息，查看第一个报警对象的结构
      if (result.length > 0) {
        console.log('First alarm device:', result[0].device)
        console.log('First alarm device imei:', result[0].device?.imei)
        console.log('First alarm patient:', result[0].patient)
        console.log('First alarm patient name:', result[0].patient?.name)
      }
    } else if (result && result.content) {
      // 如果返回的是分页对象，使用content字段作为数据列表
      alarms.value = result.content
      total.value = result.totalElements || 0
      console.log('Alarms after processing (page case):', alarms.value)
      // 添加调试信息，查看第一个报警对象的结构
      if (result.content.length > 0) {
        console.log('First alarm device:', result.content[0].device)
        console.log('First alarm device imei:', result.content[0].device?.imei)
        console.log('First alarm patient:', result.content[0].patient)
        console.log('First alarm patient name:', result.content[0].patient?.name)
      }
    } else {
      alarms.value = []
      total.value = 0
    }
  } catch (error) {
    ElMessage.error('获取报警列表失败')
    console.error('Failed to fetch alarms:', error)
  } finally {
    loading.value = false
  }
}

// 添加watch，监听alarms变化
watch(() => alarms.value, (newVal) => {
  console.log('Alarms changed:', newVal)
  if (newVal.length > 0) {
    console.log('First alarm in watch:', newVal[0])
    console.log('First alarm device in watch:', newVal[0].device)
    console.log('First alarm device imei in watch:', newVal[0].device?.imei)
  }
}, { deep: true })

// 保存报警间隔设置
const saveAlarmInterval = () => {
  // 保存到本地存储
  localStorage.setItem('alarmInterval', alarmInterval.value)
  // 发送到后端保存
  // 这里可以添加API调用，将报警间隔保存到数据库
  ElMessage.success(`报警间隔已设置为${alarmInterval.value}分钟`)
}

// 加载报警间隔设置
const loadAlarmInterval = () => {
  // 从本地存储加载
  const savedInterval = localStorage.getItem('alarmInterval')
  if (savedInterval) {
    alarmInterval.value = parseInt(savedInterval)
  }
  // 这里可以添加API调用，从数据库获取报警间隔设置
}

// 刷新报警列表
const refreshAlarms = () => {
  fetchAlarms()
}

// 选择报警
const selectAlarm = (row) => {
  selectedAlarm.value = row
}

// 处理报警
const handleAlarm = (alarm) => {
  selectedAlarm.value = alarm
  handleDialogVisible.value = true
}

// 保存处理结果
const saveHandleResult = async () => {
  if (!handleFormRef.value || !selectedAlarm.value) return
  
  await handleFormRef.value.validate(async (valid) => {
    if (valid) {
      handleLoading.value = true
      try {
        await alarmApi.handleAlarm(selectedAlarm.value.id, handleForm.result)
        ElMessage.success('报警处理成功')
        handleDialogVisible.value = false
        fetchAlarms()
        // 更新选中的报警状态
        selectedAlarm.value.status = 'handled'
      } catch (error) {
        ElMessage.error('报警处理失败')
        console.error('Failed to handle alarm:', error)
      } finally {
        handleLoading.value = false
      }
    }
  })
}

// 分页大小变化
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchAlarms()
}

// 当前页码变化
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchAlarms()
}

onMounted(() => {
  loadAlarmInterval()
  fetchAlarms()
})
</script>

<style scoped>
.alarm-container {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.alarm-content {
  padding: 20px 0;
}

.alarm-list-panel {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
}

.alarm-list-panel h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: bold;
}

.search-input {
  margin-bottom: 20px;
  width: 300px;
}

.alarm-detail-panel {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  height: 650px;
  overflow-y: auto;
}

.alarm-detail-panel h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: bold;
}

.alarm-detail {
  overflow-y: auto;
  max-height: 580px;
}

.alarm-handle-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>