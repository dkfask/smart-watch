<template>
  <div class="fence-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>围栏管理</h2>
          <el-button type="primary" @click="handleAdd">创建围栏</el-button>
        </div>
      </template>
      <div class="fence-content">
        <el-table :data="fences" style="width: 100%" v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="围栏名称" width="160" />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="scope">
              <el-tag size="small">{{ scope.row.type === 'circle' ? '圆形' : scope.row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="中心坐标" width="200">
            <template #default="scope">
              {{ scope.row.centerLat?.toFixed(4) }}, {{ scope.row.centerLng?.toFixed(4) }}
            </template>
          </el-table-column>
          <el-table-column prop="radius" label="半径(米)" width="100" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'active' ? 'success' : 'info'" size="small">
                {{ scope.row.status === 'active' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="关联病人" min-width="150">
            <template #default="scope">
              <span v-if="scope.row.patientIds && scope.row.patientIds.length > 0">
                {{ scope.row.patientIds.length }}人
              </span>
              <span v-else class="text-muted">未关联</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="scope">
              <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 创建/编辑围栏对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="fenceForm" ref="fenceFormRef" label-width="100px">
        <el-form-item label="围栏名称" prop="name" :rules="[{ required: true, message: '请输入围栏名称' }]">
          <el-input v-model="fenceForm.name" placeholder="请输入围栏名称" />
        </el-form-item>
        <el-form-item label="围栏类型">
          <el-select v-model="fenceForm.type" placeholder="请选择围栏类型">
            <el-option label="圆形" value="circle" />
          </el-select>
        </el-form-item>
        <el-form-item label="中心纬度" prop="centerLat" :rules="[{ required: true, message: '请输入中心纬度' }]">
          <el-input-number v-model="fenceForm.centerLat" :precision="6" :step="0.001" />
        </el-form-item>
        <el-form-item label="中心经度" prop="centerLng" :rules="[{ required: true, message: '请输入中心经度' }]">
          <el-input-number v-model="fenceForm.centerLng" :precision="6" :step="0.001" />
        </el-form-item>
        <el-form-item label="半径(米)" prop="radius" :rules="[{ required: true, message: '请输入半径' }]">
          <el-input-number v-model="fenceForm.radius" :min="10" :max="50000" :step="100" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="fenceForm.status">
            <el-option label="启用" value="active" />
            <el-option label="停用" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联病人">
          <el-select v-model="fenceForm.patientIds" multiple placeholder="选择关联病人" filterable>
            <el-option v-for="p in allPatients" :key="p.id" :label="`${p.name} (${p.ward || '未分配'})`" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saveLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { fenceApi } from '../api/fence'
import { patientApi } from '../api/patient'
import { ElMessage, ElMessageBox } from 'element-plus'

const fences = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('创建围栏')
const saveLoading = ref(false)
const fenceFormRef = ref()
const allPatients = ref([])

const fenceForm = reactive({
  id: null,
  name: '',
  type: 'circle',
  centerLat: 39.9087,
  centerLng: 116.3975,
  radius: 500,
  status: 'active',
  patientIds: []
})

/**
 * 获取围栏列表
 */
const fetchFences = async () => {
  loading.value = true
  try {
    const data = await fenceApi.getFences()
    fences.value = Array.isArray(data) ? data : (data?.content || [])
  } catch (e) {
    ElMessage.error('获取围栏列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 获取病人列表用于关联选择
 */
const fetchPatients = async () => {
  try {
    const data = await patientApi.getPatients({ limit: 200, offset: 0 })
    allPatients.value = data?.content || (Array.isArray(data) ? data : [])
  } catch (e) {
    // 忽略
  }
}

const handleAdd = () => {
  dialogTitle.value = '创建围栏'
  Object.assign(fenceForm, { id: null, name: '', type: 'circle', centerLat: 39.9087, centerLng: 116.3975, radius: 500, status: 'active', patientIds: [] })
  dialogVisible.value = true
}

const handleEdit = (fence) => {
  dialogTitle.value = '编辑围栏'
  Object.assign(fenceForm, {
    id: fence.id,
    name: fence.name || '',
    type: fence.type || 'circle',
    centerLat: fence.centerLat,
    centerLng: fence.centerLng,
    radius: fence.radius,
    status: fence.status || 'active',
    patientIds: fence.patientIds || []
  })
  dialogVisible.value = true
}

/**
 * 保存围栏
 */
const handleSave = async () => {
  if (!fenceFormRef.value) return
  await fenceFormRef.value.validate(async (valid) => {
    if (!valid) return
    saveLoading.value = true
    try {
      if (fenceForm.id) {
        await fenceApi.updateFence(fenceForm.id, fenceForm)
        ElMessage.success('围栏更新成功')
      } else {
        await fenceApi.createFence(fenceForm)
        ElMessage.success('围栏创建成功')
      }
      dialogVisible.value = false
      fetchFences()
    } catch (e) {
      ElMessage.error(fenceForm.id ? '围栏更新失败' : '围栏创建失败')
    } finally {
      saveLoading.value = false
    }
  })
}

/**
 * 删除围栏
 */
const handleDelete = (fence) => {
  ElMessageBox.confirm(`确定要删除围栏「${fence.name}」吗？`, '删除围栏', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await fenceApi.deleteFence(fence.id)
      ElMessage.success('围栏删除成功')
      fetchFences()
    } catch (e) {
      ElMessage.error('围栏删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchFences()
  fetchPatients()
})
</script>

<style scoped>
.fence-container { width: 100%; padding: 20px; background: transparent; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.fence-content {
  background: var(--bg-card); border: 1px solid var(--border-color);
  padding: 20px; border-radius: var(--radius-lg); backdrop-filter: blur(10px);
}
.text-muted { color: var(--text-muted); }
</style>
