<template>
  <div class="patient-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>病人管理</h2>
          <el-button type="primary" @click="handleAddPatient">添加病人</el-button>
        </div>
      </template>
      <div class="patient-content">
        <el-input
          v-model="searchQuery"
          placeholder="搜索病人姓名或ID"
          prefix-icon="Search"
          class="search-input"
          clearable
        />
        <el-select v-model="filterWard" placeholder="筛选病房" clearable class="ward-filter" @change="fetchPatients">
          <el-option v-for="ward in wardOptions" :key="ward" :label="ward" :value="ward" />
        </el-select>
        <el-table :data="patients" style="width: 100%" v-loading="loading" @row-click="goToPatientDetail">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column prop="age" label="年龄" width="80" />
          <el-table-column prop="gender" label="性别" width="80">
            <template #default="scope">
              {{ scope.row.gender === 'male' ? '男' : '女' }}
            </template>
          </el-table-column>
          <el-table-column prop="ward" label="病房" width="120" />
          <el-table-column prop="bed" label="床位" width="80" />
          <el-table-column label="监控状态" width="100">
            <template #default="scope">
              <el-tag v-if="scope.row.deviceId" type="success" size="small">已监控</el-tag>
              <el-tag v-else type="info" size="small">未监控</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="idCard" label="身份证号" width="180">
            <template #default="scope">
              {{ scope.row.idCard || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="联系电话" width="120">
            <template #default="scope">
              {{ scope.row.phone || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180" :formatter="formatDate" />
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="scope">
              <el-button type="primary" size="small" @click.stop="goToPatientDetail(scope.row)">
                详情
              </el-button>
              <el-button type="default" size="small" @click.stop="handleEditPatient(scope.row)">
                编辑
              </el-button>
              <el-button type="warning" size="small" @click.stop="handleViewHealthData(scope.row)">
                健康
              </el-button>
              <el-button type="danger" size="small" @click.stop="handleDeletePatient(scope.row.id)">
                删除
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
    </el-card>

    <!-- 添加/编辑病人对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="patientForm" :rules="rules" ref="patientFormRef" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="patientForm.name" placeholder="请输入姓名" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input v-model.number="patientForm.age" type="number" placeholder="请输入年龄" min="0" max="120" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="patientForm.gender" placeholder="请选择性别">
            <el-option label="男" value="male" />
            <el-option label="女" value="female" />
          </el-select>
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="patientForm.idCard" placeholder="请输入身份证号" maxlength="18" show-word-limit />
        </el-form-item>
        <el-form-item label="病房" prop="ward">
          <el-input v-model="patientForm.ward" placeholder="请输入病房号" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="床位" prop="bed">
          <el-input v-model="patientForm.bed" placeholder="请输入床位号" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="patientForm.phone" placeholder="请输入联系电话" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="紧急联系人">
          <el-input v-model="patientForm.emergencyContact" placeholder="请输入紧急联系人" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="紧急联系电话">
          <el-input v-model="patientForm.emergencyPhone" placeholder="请输入紧急联系电话" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="诊断信息">
          <el-input v-model="patientForm.diagnosis" type="textarea" placeholder="请输入诊断信息" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSavePatient" :loading="dialogLoading">保存</el-button>
        </span>
      </template>
    </el-dialog>



    <!-- 健康数据查看对话框 -->
    <el-dialog v-model="showHealthData" title="健康数据" width="800px">
      <div v-if="selectedPatientForHealth" class="health-data-container">
        <h3>{{ selectedPatientForHealth.name }} - 健康数据</h3>
        <el-row :gutter="20" style="margin-bottom: 20px;">
          <el-col :span="6">
            <el-card shadow="hover">
              <template #header>
                <div class="card-header">
                  <h4>最新体温</h4>
                </div>
              </template>
              <div class="health-value">
                <div class="value">{{ latestHealthData.temperature?.value || '-' }}°C</div>
                <div class="time">{{ latestHealthData.temperature?.time ? formatDate(latestHealthData.temperature.time) : '-' }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <template #header>
                <div class="card-header">
                  <h4>最新心率</h4>
                </div>
              </template>
              <div class="health-value">
                <div class="value">{{ latestHealthData.heart_rate?.value || '-' }}次/分</div>
                <div class="time">{{ latestHealthData.heart_rate?.time ? formatDate(latestHealthData.heart_rate.time) : '-' }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <template #header>
                <div class="card-header">
                  <h4>最新血压</h4>
                </div>
              </template>
              <div class="health-value">
                <div class="value">{{ latestHealthData.blood_pressure?.value || '-' }} mmHg</div>
                <div class="time">{{ latestHealthData.blood_pressure?.time ? formatDate(latestHealthData.blood_pressure.time) : '-' }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <template #header>
                <div class="card-header">
                  <h4>最新血氧</h4>
                </div>
              </template>
              <div class="health-value">
                <div class="value">{{ latestHealthData.spo2?.value || '-' }}%</div>
                <div class="time">{{ latestHealthData.spo2?.time ? formatDate(latestHealthData.spo2.time) : '-' }}</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <h4>健康数据趋势图</h4>
            </div>
          </template>
          <el-tabs v-model="activeTab">
            <!-- 体温趋势图 -->
            <el-tab-pane label="体温" name="temperature">
              <div class="chart-container">
                <el-chart :height="300" :data="temperatureChartData">
                  <el-line :x-field="'time'" :y-field="'value'" :smooth="true">
                    <el-tooltip :show-markers="true" :show-content="true" />
                    <el-line-style :color="'#ff6b6b'" />
                  </el-line>
                  <el-axis :orient="'left'" :title="'体温 (°C)'" />
                  <el-axis :orient="'bottom'" :title="'时间'" :label-rotate="45" />
                  <el-grid />
                </el-chart>
              </div>
            </el-tab-pane>
            
            <!-- 心率趋势图 -->
            <el-tab-pane label="心率" name="heart_rate">
              <div class="chart-container">
                <el-chart :height="300" :data="heartRateChartData">
                  <el-line :x-field="'time'" :y-field="'value'" :smooth="true">
                    <el-tooltip :show-markers="true" :show-content="true" />
                    <el-line-style :color="'#4ecdc4'" />
                  </el-line>
                  <el-axis :orient="'left'" :title="'心率 (次/分)'" />
                  <el-axis :orient="'bottom'" :title="'时间'" :label-rotate="45" />
                  <el-grid />
                </el-chart>
              </div>
            </el-tab-pane>
            
            <!-- 血氧趋势图 -->
            <el-tab-pane label="血氧" name="spo2">
              <div class="chart-container">
                <el-chart :height="300" :data="spo2ChartData">
                  <el-line :x-field="'time'" :y-field="'value'" :smooth="true">
                    <el-tooltip :show-markers="true" :show-content="true" />
                    <el-line-style :color="'#95e1d3'" />
                  </el-line>
                  <el-axis :orient="'left'" :title="'血氧饱和度 (%)'" />
                  <el-axis :orient="'bottom'" :title="'时间'" :label-rotate="45" />
                  <el-grid />
                </el-chart>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
        
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <h4>健康记录历史</h4>
            </div>
          </template>
          <el-table :data="healthRecords" v-loading="healthLoading" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="dataType" label="数据类型" width="120">
              <template #default="scope">
                {{ {
                  temperature: '体温',
                  heart_rate: '心率',
                  blood_pressure: '血压',
                  spo2: '血氧'
                }[scope.row.dataType] || scope.row.dataType }}
              </template>
            </el-table-column>
            <el-table-column prop="value" label="数值" width="120" />
            <el-table-column prop="unit" label="单位" width="80" />
            <el-table-column prop="deviceId" label="设备ID" width="120" />
            <el-table-column prop="time" label="记录时间" width="180" :formatter="formatDate" />
          </el-table>
        </el-card>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showHealthData = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { patientApi } from '../api/patient'
import { deviceApi } from '../api/device'
import { healthApi } from '../api/health'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const patients = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const searchQuery = ref('')
const filterWard = ref('')

/**
 * 病房选项列表
 */
const wardOptions = computed(() => {
  const wards = new Set(patients.value.map(p => p.ward).filter(Boolean))
  return Array.from(wards).sort()
})

/**
 * 跳转到病人详情页
 */
const goToPatientDetail = (row) => {
  router.push(`/patients/${row.id}`)
}

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('添加病人')
const patientFormRef = ref()
const patientForm = reactive({
  id: null,
  name: '',
  age: null,
  gender: 'male',
  idCard: '',
  ward: '',
  bed: '',
  phone: '',
  emergencyContact: '',
  emergencyPhone: '',
  diagnosis: ''
})
const dialogLoading = ref(false)



// 健康数据相关
const showHealthData = ref(false)
const selectedPatientForHealth = ref(null)
const healthRecords = ref([])
const latestHealthData = ref({})
const healthLoading = ref(false)

// 图表相关
const activeTab = ref('temperature')
const temperatureChartData = ref([])
const heartRateChartData = ref([])
const spo2ChartData = ref([])

const rules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 1, max: 50, message: '姓名长度在 1 到 50 个字符', trigger: 'blur' }
  ],
  age: [
    { required: true, message: '请输入年龄', trigger: 'blur' },
    { type: 'number', min: 0, max: 120, message: '年龄必须在 0 到 120 之间', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { min: 18, max: 18, message: '身份证号必须为 18 位', trigger: 'blur' },
    { pattern: /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/, message: '请输入有效的身份证号', trigger: 'blur' }
  ],
  phone: [
    { required: false, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }
  ],
  emergencyPhone: [
    { required: false, message: '请输入紧急联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的紧急联系手机号码', trigger: 'blur' }
  ],
  ward: [
    { required: true, message: '请输入病房号', trigger: 'blur' }
  ],
  bed: [
    { required: true, message: '请输入床位号', trigger: 'blur' }
  ]
}



// 格式化日期
const formatDate = (row, column, cellValue) => {
  if (!cellValue) return ''
  return new Date(cellValue).toLocaleString()
}

// 监听搜索条件变化
watch(searchQuery, () => {
  currentPage.value = 1
  fetchPatients()
})

// 获取病人列表
const fetchPatients = async () => {
  loading.value = true
  try {
    const offset = (currentPage.value - 1) * pageSize.value
    const params = {
      limit: pageSize.value,
      offset: offset
    }
    
    // 如果有搜索条件，添加到参数中
    if (searchQuery.value) {
      params.search = searchQuery.value
    }
    
    const response = await patientApi.getPatients(params)
    let allPatients = []
    if (response && Array.isArray(response.content)) {
      allPatients = response.content
      total.value = response.total || 0
    } else if (response && Array.isArray(response.data)) {
      allPatients = response.data
      total.value = response.total || 0
    } else if (Array.isArray(response)) {
      allPatients = response
      total.value = response.length
    } else {
      allPatients = []
      total.value = 0
    }

    // 前端病房筛选
    if (filterWard.value) {
      patients.value = allPatients.filter(p => p.ward === filterWard.value)
    } else {
      patients.value = allPatients
    }
  } catch (error) {
    ElMessage.error('获取病人列表失败')
    console.error('Failed to fetch patients:', error)
  } finally {
    loading.value = false
  }
}

// 添加病人
const handleAddPatient = () => {
  dialogTitle.value = '添加病人'
  resetForm()
  dialogVisible.value = true
}

// 编辑病人
const handleEditPatient = (patient) => {
  dialogTitle.value = '编辑病人'
  Object.assign(patientForm, patient)
  dialogVisible.value = true
}

// 保存病人
const handleSavePatient = async () => {
  if (!patientFormRef.value) return
  
  await patientFormRef.value.validate(async (valid) => {
    if (valid) {
      dialogLoading.value = true
      try {
        if (patientForm.id) {
          // 更新病人
          await patientApi.updatePatient(patientForm.id, patientForm)
          ElMessage.success('病人更新成功')
        } else {
          // 创建病人
          await patientApi.createPatient(patientForm)
          ElMessage.success('病人添加成功')
        }
        dialogVisible.value = false
        fetchPatients()
      } catch (error) {
        ElMessage.error(patientForm.id ? '病人更新失败' : '病人添加失败')
        console.error('Failed to save patient:', error)
      } finally {
        dialogLoading.value = false
      }
    }
  })
}

// 删除病人
const handleDeletePatient = (id) => {
  ElMessageBox.confirm('确定要删除该病人吗？', '删除病人', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await patientApi.deletePatient(id)
      ElMessage.success('病人删除成功')
      fetchPatients()
    } catch (error) {
      ElMessage.error('病人删除失败')
      console.error('Failed to delete patient:', error)
    }
  }).catch(() => {
    // 取消删除
  })
}

// 处理健康记录，生成图表数据
const processHealthRecordsForChart = (records) => {
  // 清空现有图表数据
  temperatureChartData.value = []
  heartRateChartData.value = []
  spo2ChartData.value = []
  
  // 按数据类型分组
  const groupedData = {
    temperature: [],
    heart_rate: [],
    spo2: []
  }
  
  records.forEach(record => {
    if (groupedData[record.dataType]) {
      groupedData[record.dataType].push({
        time: new Date(record.time).toLocaleString(),
        value: parseFloat(record.value)
      })
    }
  })
  
  // 按时间排序
  Object.keys(groupedData).forEach(key => {
    groupedData[key].sort((a, b) => new Date(a.time) - new Date(b.time))
  })
  
  // 将处理后的数据赋值给图表数据
  temperatureChartData.value = groupedData.temperature
  heartRateChartData.value = groupedData.heart_rate
  spo2ChartData.value = groupedData.spo2
}

// 获取健康记录
const fetchHealthRecords = async (patientId) => {
  healthLoading.value = true
  try {
    const data = await healthApi.getHealthRecords(patientId, 20, 0)
    healthRecords.value = Array.isArray(data) ? data : data.data || []
    // 处理健康记录，生成图表数据
    processHealthRecordsForChart(healthRecords.value)
  } catch (error) {
    ElMessage.error('获取健康记录失败')
    console.error('Failed to fetch health records:', error)
  } finally {
    healthLoading.value = false
  }
}

// 获取最新健康数据
const fetchLatestHealthData = async (patientId) => {
  try {
    const data = await healthApi.getLatestHealthRecords(patientId)
    latestHealthData.value = data
  } catch (error) {
    console.error('Failed to fetch latest health data:', error)
  }
}

// 查看健康数据
const handleViewHealthData = (patient) => {
  selectedPatientForHealth.value = patient
  showHealthData.value = true
  fetchHealthRecords(patient.id)
  fetchLatestHealthData(patient.id)
}

// 重置表单
const resetForm = () => {
  if (patientFormRef.value) {
    patientFormRef.value.resetFields()
  }
  patientForm.id = null
  patientForm.name = ''
  patientForm.age = null
  patientForm.gender = 'male'
  patientForm.idCard = ''
  patientForm.ward = ''
  patientForm.bed = ''
  patientForm.phone = ''
  patientForm.emergencyContact = ''
  patientForm.emergencyPhone = ''
  patientForm.diagnosis = ''
}

// 分页大小变化
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchPatients()
}

// 当前页码变化
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchPatients()
}

// 初始化
onMounted(() => {
  fetchPatients()
})
</script>

<style scoped>
.patient-container {
  width: 100%;
  padding: 20px;
  background: transparent;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.patient-content {
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

.ward-filter {
  margin-bottom: 20px;
  margin-left: 12px;
  width: 160px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 图表容器样式 */
.chart-container {
  width: 100%;
  height: 300px;
  margin: 0 auto;
}
</style>