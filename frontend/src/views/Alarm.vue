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
                <el-table-column prop="type" label="报警类型" width="120">
                  <template #default="scope">
                    <el-tag :type="getAlarmTypeColor(scope.row.type)" size="small">
                      {{ getAlarmTypeName(scope.row.type) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="device.imei" label="设备IMEI" width="180">
                  <template #default="scope">
                    {{ scope.row.device?.imei || '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="patient.name" label="病人" width="120">
                  <template #default="scope">
                    {{ scope.row.patient?.name || '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="location.address" label="位置" min-width="150">
                  <template #default="scope">
                    {{ scope.row.location?.address || '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="time" label="报警时间" width="180" :formatter="formatDate" />
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
                    <el-tag :type="getAlarmTypeColor(selectedAlarm.type)">
                      {{ getAlarmTypeName(selectedAlarm.type) }}
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="报警时间">{{ formatDate(selectedAlarm.time) }}</el-descriptions-item>
                  <el-descriptions-item label="报警状态">
                    <el-tag :type="selectedAlarm.status === 'pending' ? 'warning' : 'success'">
                      {{ selectedAlarm.status === 'pending' ? '未处理' : '已处理' }}
                    </el-tag>
                  </el-descriptions-item>
                </el-descriptions>

                <el-descriptions title="设备信息" :column="1" border style="margin-top: 20px;">
                  <el-descriptions-item label="设备ID">{{ selectedAlarm.device?.id || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="设备IMEI">{{ selectedAlarm.device?.imei || '-' }}</el-descriptions-item>
                </el-descriptions>

                <el-descriptions title="病人信息" :column="1" border style="margin-top: 20px;">
                  <el-descriptions-item label="姓名">{{ selectedAlarm.patient?.name || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="年龄">{{ selectedAlarm.patient?.age || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="性别">{{ selectedAlarm.patient?.gender === 'male' ? '男' : '女' }}</el-descriptions-item>
                  <el-descriptions-item label="病房">{{ selectedAlarm.patient?.ward || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="床位">{{ selectedAlarm.patient?.bed || '-' }}</el-descriptions-item>
                </el-descriptions>

                <el-descriptions title="位置信息" :column="1" border style="margin-top: 20px;">
                  <el-descriptions-item label="经度">{{ selectedAlarm.location?.longitude || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="纬度">{{ selectedAlarm.location?.latitude || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="地址">{{ selectedAlarm.location?.address || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="精度">{{ selectedAlarm.location?.accuracy || '-' }}m</el-descriptions-item>
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
import { ref, reactive, onMounted } from 'vue'
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
const getAlarmTypeName = (type) => {
  const typeMap = {
    'fence_breach': '围栏越界',
    'low_battery': '低电量',
    'sos': 'SOS报警',
    'fall': '跌倒报警',
    'heart_rate': '心率异常'
  }
  return typeMap[type] || '未知类型'
}

// 获取报警类型颜色
const getAlarmTypeColor = (type) => {
  const colorMap = {
    'fence_breach': 'danger',
    'low_battery': 'warning',
    'sos': 'danger',
    'fall': 'danger',
    'heart_rate': 'danger'
  }
  return colorMap[type] || 'info'
}

// 获取报警列表
const fetchAlarms = async () => {
  loading.value = true
  try {
    const offset = (currentPage.value - 1) * pageSize.value
    const status = filterStatus.value || null
    const data = await alarmApi.getAlarms(pageSize.value, offset, status)
    alarms.value = data
    total.value = data.length // 实际应用中应该从后端获取总数
  } catch (error) {
    ElMessage.error('获取报警列表失败')
    console.error('Failed to fetch alarms:', error)
  } finally {
    loading.value = false
  }
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