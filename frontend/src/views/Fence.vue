<template>
  <div class="fence-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>电子围栏管理</h2>
          <el-button type="primary" @click="handleAddFence">添加围栏</el-button>
        </div>
      </template>
      <div class="fence-content">
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="fence-list-panel">
              <h3>围栏列表</h3>
              <el-input
                v-model="searchQuery"
                placeholder="搜索围栏名称或ID"
                prefix-icon="Search"
                class="search-input"
                clearable
              />
              <el-scrollbar height="600px">
                <el-radio-group v-model="selectedFenceId" class="fence-radio-group">
                <div v-if="fences.length === 0" class="no-fences">
                  暂无围栏数据
                </div>
                <el-radio-button
                  v-else
                  v-for="fence in fences"
                  :key="fence.id"
                  :label="fence.id"
                  class="fence-radio"
                  @change="highlightFence(fence)"
                >
                  <div class="fence-info">
                    <div class="fence-name">{{ fence.name }}</div>
                    <div class="fence-type">
                      <el-tag size="small">
                        {{ fence.type === 'circle' ? '圆形' : '多边形' }}
                      </el-tag>
                    </div>
                    <!-- 显示关联的病人信息 -->
                    <div v-if="fence.patientIds && fence.patientIds.length > 0" class="fence-patients">
                      <el-tag size="small" type="success" v-for="patientId in fence.patientIds" :key="patientId">
                        {{ getPatientName(patientId) }}
                      </el-tag>
                    </div>
                    <div v-else class="no-patients">
                      <el-tag size="small" type="info">未关联病人</el-tag>
                    </div>
                  </div>
                </el-radio-button>
              </el-radio-group>
              </el-scrollbar>
              <div class="fence-actions">
                <el-button type="primary" size="small" @click="handleEditFence" :disabled="!selectedFenceId">
                  编辑
                </el-button>
                <el-button type="danger" size="small" @click="handleDeleteFence" :disabled="!selectedFenceId">
                  删除
                </el-button>
              </div>
            </div>
          </el-col>
          <el-col :span="16">
            <div class="map-panel">
              <div class="map-search">
                <el-input
                  v-model="searchAddress"
                  placeholder="搜索地址"
                  prefix-icon="Search"
                  clearable
                >
                  <template #append>
                    <el-button @click="handleSearchLocation" :loading="searchLoading">
                      搜索
                    </el-button>
                  </template>
                </el-input>
              </div>
              <div id="fence-map" class="fence-map"></div>
              <div class="map-controls">
                <el-radio-group v-model="drawMode" size="small" @change="changeDrawMode">
                  <el-radio-button label="none">查看</el-radio-button>
                  <el-radio-button label="circle">圆形围栏</el-radio-button>
                  <el-radio-button label="polygon">多边形围栏</el-radio-button>
                </el-radio-group>
                <el-button size="small" @click="clearDrawings" :disabled="drawMode === 'none'">
                  清除绘制
                </el-button>
                <el-button size="small" @click="clearSearch" :disabled="!searchResult">
                  清除搜索
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 添加/编辑围栏对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="fenceForm" :rules="rules" ref="fenceFormRef" label-width="100px">
        <el-form-item label="围栏名称" prop="name">
          <el-input v-model="fenceForm.name" placeholder="请输入围栏名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="围栏类型" prop="type">
          <el-select v-model="fenceForm.type" placeholder="请选择围栏类型" @change="handleFenceTypeChange">
            <el-option label="圆形" value="circle" />
            <el-option label="多边形" value="polygon" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联病人">
          <el-select v-model="fenceForm.patientIds" placeholder="请选择关联的病人（可选）" multiple>
            <el-option
              v-for="patient in patients"
              :key="patient.id"
              :label="`${patient.name} - ${patient.ward}病房${patient.bed}床`"
              :value="patient.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="fenceForm.type === 'circle'" label="半径（米）" prop="radius">
          <el-input v-model.number="fenceForm.radius" type="number" placeholder="请输入半径" min="1" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="fenceForm.status" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="fenceForm.remark" type="textarea" placeholder="请输入备注" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveFence" :loading="dialogLoading">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { fenceApi } from '../api/fence'
import { patientApi } from '../api/patient'
import api from '../api/axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import 'leaflet-draw'
import 'leaflet-draw/dist/leaflet.draw.css'

// 初始化认证store
const authStore = useAuthStore()

const fences = ref([])
const loading = ref(false)
const searchQuery = ref('')
const selectedFenceId = ref(null)
const drawMode = ref('none')
let map = null
let drawnItems = null
let drawControl = null
let currentDrawing = null
let searchMarker = null

// 搜索相关数据
const searchAddress = ref('')
const searchLoading = ref(false)
const searchResult = ref(null)

// 病人相关数据
const patients = ref([])
const selectedPatientId = ref(null)

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('添加围栏')
const fenceFormRef = ref()
const fenceForm = reactive({
  id: null,
  name: '',
  type: 'circle',
  radius: 50,
  coordinates: [],
  status: true,
  remark: '',
  patientIds: []
})
const dialogLoading = ref(false)

const rules = {
  name: [
    { required: true, message: '请输入围栏名称', trigger: 'blur' },
    { min: 1, max: 50, message: '围栏名称长度在 1 到 50 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择围栏类型', trigger: 'change' }
  ],
  radius: [
    { required: (() => fenceForm.type === 'circle'), message: '请输入半径', trigger: 'blur' },
    { type: 'number', min: 1, message: '半径必须大于 0', trigger: 'blur' }
  ]
}

// 初始化地图
const initMap = () => {
  // 创建地图实例
  map = L.map('fence-map').setView([39.9042, 116.4074], 13)

  // 添加基础图层
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    attribution: '© 高德地图'
  }).addTo(map)

  // 初始化绘制图层
  drawnItems = new L.FeatureGroup()
  map.addLayer(drawnItems)

  // 初始化绘制控件
  initDrawControl()

  // 监听绘制事件
  map.on(L.Draw.Event.CREATED, handleDrawingCreated)
  map.on(L.Draw.Event.EDITED, handleDrawingEdited)
  map.on(L.Draw.Event.DELETED, handleDrawingDeleted)
}

// 初始化绘制控件
const initDrawControl = () => {
  drawControl = new L.Control.Draw({
    draw: {
      polygon: {
        shapeOptions: {
          color: '#4ecdc4',
          fillColor: '#4ecdc4',
          fillOpacity: 0.3,
          weight: 2
        },
        allowIntersection: false,
        drawError: {
          color: '#ff6b6b',
          message: '⚠️ 多边形不能交叉！'
        },
        showArea: true,
        metric: true,
        zIndexOffset: 2000
      },
      circle: {
        shapeOptions: {
          color: '#ff6b6b',
          fillColor: '#ff6b6b',
          fillOpacity: 0.3,
          weight: 2
        },
        showRadius: true,
        metric: true,
        zIndexOffset: 2000
      },
      marker: false,
      polyline: false,
      rectangle: false
    },
    edit: {
      featureGroup: drawnItems,
      remove: false
    }
  })
  map.addControl(drawControl)
}

// 更改绘制模式
const changeDrawMode = () => {
  // 清除现有绘制控件
  map.removeControl(drawControl)

  // 根据选择的模式重新初始化绘制控件
  let drawOptions = {
    polygon: {
      shapeOptions: {
        color: '#4ecdc4',
        fillColor: '#4ecdc4',
        fillOpacity: 0.3,
        weight: 2
      },
      allowIntersection: false,
      drawError: {
        color: '#ff6b6b',
        message: '⚠️ 多边形不能交叉！'
      },
      showArea: true,
      metric: true,
      zIndexOffset: 2000
    },
    circle: {
      shapeOptions: {
        color: '#ff6b6b',
        fillColor: '#ff6b6b',
        fillOpacity: 0.3,
        weight: 2
      },
      showRadius: true,
      metric: true,
      zIndexOffset: 2000
    },
    marker: false,
    polyline: false,
    rectangle: false
  }

  // 根据选择的模式启用对应的绘制工具
  if (drawMode.value === 'circle') {
    drawOptions.polygon = false
    // 切换到绘制模式时清除现有绘制
    clearDrawings()
  } else if (drawMode.value === 'polygon') {
    drawOptions.circle = false
    // 切换到绘制模式时清除现有绘制
    clearDrawings()
  } else {
    // 查看模式，禁用所有绘制工具
    drawOptions.polygon = false
    drawOptions.circle = false
    // 切换到查看模式时不清除现有绘制，保留已绘制的围栏
  }

  drawControl = new L.Control.Draw({
    draw: drawOptions,
    edit: {
      featureGroup: drawnItems,
      remove: false
    }
  })
  map.addControl(drawControl)
}

// 处理绘制完成事件
const handleDrawingCreated = (e) => {
  const layer = e.layer
  drawnItems.addLayer(layer)

  // 保存绘制数据到表单
  if (e.layerType === 'circle') {
    fenceForm.type = 'circle'
    fenceForm.radius = Math.round(layer.getRadius())
    const center = layer.getLatLng()
    fenceForm.coordinates = [center.lng, center.lat]
    console.log('Circle fence saved to form:', fenceForm)
  } else if (e.layerType === 'polygon') {
    fenceForm.type = 'polygon'
    fenceForm.radius = null
    const latLngs = layer.getLatLngs()[0]
    fenceForm.coordinates = latLngs.map(latLng => [latLng.lng, latLng.lat])
    // 确保多边形闭合
    if (fenceForm.coordinates.length >= 3) {
      const firstCoord = fenceForm.coordinates[0]
      const lastCoord = fenceForm.coordinates[fenceForm.coordinates.length - 1]
      if (firstCoord[0] !== lastCoord[0] || firstCoord[1] !== lastCoord[1]) {
        fenceForm.coordinates.push([firstCoord[0], firstCoord[1]])
      }
    }
    console.log('Polygon fence saved to form:', fenceForm)
  }

  // 切换回查看模式
  drawMode.value = 'none'
  changeDrawMode()
  
  // 绘制完成后自动弹出填写围栏信息的对话框
  dialogTitle.value = '添加围栏'
  dialogVisible.value = true
  
  // 显示绘制成功提示
  ElMessage.success('围栏绘制成功，请填写围栏信息并保存')
}

// 处理绘制编辑事件
const handleDrawingEdited = (e) => {
  // 编辑后的处理逻辑
}

// 处理绘制删除事件
const handleDrawingDeleted = (e) => {
  // 删除后的处理逻辑
}

// 清除绘制
const clearDrawings = () => {
  // 清除绘制图层组中的所有图层
  drawnItems.clearLayers()
  
  // 清除直接添加到地图上的绘制图层
  map.eachLayer(layer => {
    // 清除没有fenceId的临时绘制图层
    if (layer.fenceId === undefined && (layer instanceof L.Circle || layer instanceof L.Polygon)) {
      map.removeLayer(layer)
    }
  })
  
  // 重置表单中的围栏数据
  fenceForm.coordinates = []
  if (fenceForm.type === 'circle') {
    fenceForm.radius = 50
  }
  
  // 显示清除成功提示
  ElMessage.success('绘制已清除')
}

// 获取围栏列表
const fetchFences = async () => {
  loading.value = true
  try {
    const data = await fenceApi.getFences(100, 0)
    let fenceList = []
    
    // 确保返回的数据是数组，如果是对象则提取data或content字段
    if (Array.isArray(data)) {
      fenceList = data
    } else if (data && Array.isArray(data.data)) {
      fenceList = data.data
    } else if (data && Array.isArray(data.content)) {
      fenceList = data.content
    }
    
    // 转换围栏数据格式以匹配前端要求
    fences.value = fenceList.map(fence => ({
      id: fence.id,
      name: fence.name,
      type: fence.type,
      radius: fence.radius,
      // 将JSON字符串转换为坐标数组，处理可能的异常
      coordinates: typeof fence.coordinates === 'string' ? JSON.parse(fence.coordinates) : [],
      // 将字符串状态转换为布尔值
      status: fence.status === 'active',
      // 将description映射到remark字段
      remark: fence.description || '',
      // 添加其他需要的字段
      centerLat: fence.centerLat,
      centerLng: fence.centerLng,
      createdBy: fence.createdBy,
      createdAt: fence.createdAt,
      // 处理多种可能的字段名：patientIds, patientId, patient_id
      patientIds: 
        // 优先使用patientIds数组
        (fence.patientIds && Array.isArray(fence.patientIds) ? fence.patientIds : 
        // 然后尝试patientId（驼峰式）
        (fence.patientId ? [fence.patientId] : 
        // 最后尝试patient_id（下划线式）
        (fence.patient_id ? [fence.patient_id] : [])))
    }))
    
    // 在地图上绘制所有围栏
    drawAllFences()
  } catch (error) {
    ElMessage.error('获取围栏列表失败')
    console.error('Failed to fetch fences:', error)
    // 确保fences始终是数组
    fences.value = []
  } finally {
    loading.value = false
  }
}

// 获取病人列表
const fetchPatients = async () => {
  try {
    const data = await patientApi.getPatients(100, 0)
    // 确保返回的数据是数组，如果是对象则提取data或content字段
    if (Array.isArray(data)) {
      patients.value = data
    } else if (data && Array.isArray(data.data)) {
      patients.value = data.data
    } else if (data && Array.isArray(data.content)) {
      patients.value = data.content
    } else {
      patients.value = []
    }
  } catch (error) {
    ElMessage.error('获取病人列表失败')
    console.error('Failed to fetch patients:', error)
    // 确保patients始终是数组
    patients.value = []
  }
}

// 在地图上绘制所有围栏
const drawAllFences = () => {
  if (!map) return

  // 清除现有围栏
  clearFences()

  // 绘制所有围栏
  fences.value.forEach(fence => {
    drawFence(fence)
  })
  
  // 如果有选中的围栏，自动定位到该围栏位置
  if (selectedFenceId.value) {
    const selectedFence = fences.value.find(fence => fence.id === selectedFenceId.value)
    if (selectedFence) {
      highlightFence(selectedFence)
    }
  }
}

// 根据病人ID获取病人名称
const getPatientName = (patientId) => {
  if (!patientId) return ''
  const patient = patients.value.find(p => p.id === patientId)
  return patient ? `${patient.name} - ${patient.ward}病房${patient.bed}床` : `病人ID: ${patientId}`
}

// 在地图上绘制单个围栏
const drawFence = (fence) => {
  if (!map) return

  let layer
  if (fence.type === 'circle') {
    // 绘制圆形围栏
    layer = L.circle(
      [fence.coordinates[1], fence.coordinates[0]],
      {
        radius: fence.radius,
        color: '#ff6b6b',
        fillColor: '#ff6b6b',
        fillOpacity: 0.3,
        weight: 2
      }
    )
  } else if (fence.type === 'polygon') {
    // 绘制多边形围栏
    const latLngs = fence.coordinates.map(coord => [coord[1], coord[0]])
    layer = L.polygon(latLngs, {
      color: '#4ecdc4',
      fillColor: '#4ecdc4',
      fillOpacity: 0.3,
      weight: 2
    })
  }

  if (layer) {
    // 构建弹出信息，包含关联的病人信息
    let popupContent = `<b>${fence.name}</b><br>类型: ${fence.type === 'circle' ? '圆形' : '多边形'}`
    if (fence.patientIds && fence.patientIds.length > 0) {
      popupContent += '<br><br><b>关联病人:</b><br>'
      fence.patientIds.forEach(patientId => {
        const patient = patients.value.find(p => p.id === patientId)
        if (patient) {
          popupContent += `- ${patient.name} - ${patient.ward}病房${patient.bed}床<br>`
        } else {
          popupContent += `- 病人ID: ${patientId}<br>`
        }
      })
    } else {
      popupContent += '<br><br><b>关联病人:</b><br>未关联病人'
    }
    layer.bindPopup(popupContent)
    layer.addTo(map)
    // 保存围栏ID到图层
    layer.fenceId = fence.id
  }
}

// 清除地图上的所有围栏
const clearFences = () => {
  if (!map) return

  // 遍历所有图层，移除围栏图层
  map.eachLayer(layer => {
    if (layer.fenceId !== undefined) {
      map.removeLayer(layer)
    }
  })
}

// 高亮显示指定围栏
const highlightFence = (fence) => {
  if (!map) return

  // 清除所有高亮
  map.eachLayer(layer => {
    if (layer.fenceId !== undefined) {
      if (fence.type === 'circle') {
        layer.setStyle({
          color: '#ff6b6b',
          fillColor: '#ff6b6b',
          fillOpacity: 0.3,
          weight: 2
        })
      } else {
        layer.setStyle({
          color: '#4ecdc4',
          fillColor: '#4ecdc4',
          fillOpacity: 0.3,
          weight: 2
        })
      }
    }
  })

  // 高亮指定围栏
  map.eachLayer(layer => {
    if (layer.fenceId === fence.id) {
      layer.setStyle({
        color: '#20a0ff',
        fillColor: '#20a0ff',
        fillOpacity: 0.5,
        weight: 3
      })
      // 缩放地图以显示该围栏
      map.fitBounds(layer.getBounds())
    }
  })
}

// 添加围栏
const handleAddFence = () => {
  dialogTitle.value = '添加围栏'
  // 重置表单并设置默认值
  if (fenceFormRef.value) {
    fenceFormRef.value.resetFields()
  }
  fenceForm.id = null
  fenceForm.name = ''
  fenceForm.type = 'circle'
  fenceForm.radius = 50
  // 设置默认坐标（北京市中心）
  fenceForm.coordinates = [116.4074, 39.9042]
  fenceForm.status = true
  fenceForm.remark = ''
  fenceForm.patientIds = []
  dialogVisible.value = true
}

// 编辑围栏
const handleEditFence = () => {
  if (!selectedFenceId.value) return

  const fence = fences.value.find(f => f.id === selectedFenceId.value)
  if (!fence) return

  dialogTitle.value = '编辑围栏'
  Object.assign(fenceForm, fence)
  dialogVisible.value = true
}

// 保存围栏
const handleSaveFence = async () => {
  if (!fenceFormRef.value) return
  
  // 额外的自定义验证
  if (fenceForm.coordinates.length === 0) {
    ElMessage.error('请在地图上绘制围栏或选择围栏区域')
    return
  }
  
  if (fenceForm.type === 'circle') {
    if (fenceForm.radius <= 0) {
      ElMessage.error('圆形围栏半径必须大于0')
      return
    }
    if (fenceForm.coordinates.length !== 2) {
      ElMessage.error('圆形围栏必须有有效的中心点坐标')
      return
    }
  } else if (fenceForm.type === 'polygon') {
    if (fenceForm.coordinates.length < 3) {
      ElMessage.error('多边形围栏必须至少有3个顶点')
      return
    }
    // 确保多边形闭合（自动添加闭合点）
    const firstCoord = fenceForm.coordinates[0]
    const lastCoord = fenceForm.coordinates[fenceForm.coordinates.length - 1]
    if (firstCoord[0] !== lastCoord[0] || firstCoord[1] !== lastCoord[1]) {
      // 自动闭合多边形
      fenceForm.coordinates.push([firstCoord[0], firstCoord[1]])
    }
  }
  
  await fenceFormRef.value.validate(async (valid) => {
    if (valid) {
      dialogLoading.value = true
      try {
        // 从authStore获取当前用户ID
        const currentUserId = authStore.user?.id || null
        
        // 转换数据格式以匹配后端要求
        const fenceData = {
          name: fenceForm.name,
          type: fenceForm.type,
          radius: fenceForm.radius,
          // 将坐标数组转换为JSON字符串
          coordinates: JSON.stringify(fenceForm.coordinates),
          // 将布尔值转换为字符串状态
          status: fenceForm.status ? 'active' : 'inactive',
          // 将remark映射到description字段
          description: fenceForm.remark,
          // 支持多选病人，将patientIds数组转换为字符串列表
          patientIds: fenceForm.patientIds || [],
          // 兼容旧格式，保留patientId字段（取第一个选中的病人）
          patientId: fenceForm.patientIds && fenceForm.patientIds.length > 0 ? fenceForm.patientIds[0] : null,
          // 设置当前用户ID作为创建者
          createdBy: currentUserId
        }
        
        // 如果是圆形围栏，添加中心点坐标
        if (fenceForm.type === 'circle' && fenceForm.coordinates.length === 2) {
          fenceData.centerLng = fenceForm.coordinates[0]
          fenceData.centerLat = fenceForm.coordinates[1]
        }
        
        if (fenceForm.id) {
          // 更新围栏
          await fenceApi.updateFence(fenceForm.id, fenceData)
          ElMessage.success('围栏更新成功')
        } else {
          // 创建围栏
          await fenceApi.createFence(fenceData)
          ElMessage.success('围栏添加成功')
        }
        dialogVisible.value = false
        fetchFences()
      } catch (error) {
        ElMessage.error(fenceForm.id ? '围栏更新失败' : '围栏添加失败')
        console.error('Failed to save fence:', error)
      } finally {
        dialogLoading.value = false
      }
    }
  })
}

// 删除围栏
const handleDeleteFence = () => {
  if (!selectedFenceId.value) return

  ElMessageBox.confirm('确定要删除该围栏吗？', '删除围栏', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await fenceApi.deleteFence(selectedFenceId.value)
      ElMessage.success('围栏删除成功')
      selectedFenceId.value = null
      fetchFences()
    } catch (error) {
      ElMessage.error('围栏删除失败')
      console.error('Failed to delete fence:', error)
    }
  }).catch(() => {
    // 取消删除
  })
}

// 围栏类型变化处理
const handleFenceTypeChange = () => {
  if (fenceForm.type === 'polygon') {
    fenceForm.radius = null
  } else {
    fenceForm.radius = 50
  }
}

// 搜索位置
const handleSearchLocation = async () => {
  if (!searchAddress.value.trim()) {
    ElMessage.warning('请输入搜索地址')
    return
  }
  
  searchLoading.value = true
  try {
    console.log('开始搜索，地址:', searchAddress.value)
    const response = await api.get('/locations/search', { params: { address: searchAddress.value } })
    console.log('API响应:', response)
    
    let result = response.data
    console.log('原始响应数据:', result)
    
    // 处理后端返回的{code, message, data}格式
    if (result && result.code === 200 && result.data) {
      result = result.data
      console.log('提取data字段后:', result)
    }
    
    console.log('判断条件:', { result: !!result, location: !!result?.location })
    
    if (result && result.location) {
      searchResult.value = result
      
      // 在地图上显示搜索结果
      const lat = result.location.lat
      const lng = result.location.lng
      console.log('位置信息:', { lat, lng })
      
      // 清除现有搜索标记
      if (searchMarker) {
        console.log('清除现有搜索标记')
        map.removeLayer(searchMarker)
        searchMarker = null
      }
      
      // 创建新的搜索标记
      console.log('创建新的搜索标记')
      searchMarker = L.marker([lat, lng], {
        icon: L.icon({
          iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
          iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
          shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
          iconSize: [25, 41],
          iconAnchor: [12, 41],
          popupAnchor: [1, -34],
          shadowSize: [41, 41]
        })
      })
      
      searchMarker.bindPopup(`<b>${result.address}</b><br/>经度: ${lng}<br/>纬度: ${lat}`)
      searchMarker.addTo(map)
      console.log('搜索标记已添加到地图')
      
      // 缩放地图到搜索结果
      map.setView([lat, lng], 15)
      console.log('地图已缩放')
      
      // 可以选择将搜索结果作为围栏的中心点
      ElMessage.success('搜索成功')
      console.log('搜索成功，显示成功消息')
    } else {
      console.error('未找到位置信息，result:', result)
      ElMessage.warning('未找到相关位置')
    }
  } catch (error) {
    console.error('Search failed:', error)
    console.error('Error response:', error.response)
    ElMessage.error('搜索失败，请稍后重试')
  } finally {
    searchLoading.value = false
  }
}

// 清除搜索结果
const clearSearch = () => {
  if (searchMarker) {
    map.removeLayer(searchMarker)
    searchMarker = null
  }
  searchResult.value = null
  searchAddress.value = ''
  ElMessage.success('搜索结果已清除')
}

// 重置表单
const resetForm = () => {
  if (fenceFormRef.value) {
    fenceFormRef.value.resetFields()
  }
  fenceForm.id = null
  fenceForm.name = ''
  fenceForm.type = 'circle'
  fenceForm.radius = 50
  fenceForm.coordinates = []
  fenceForm.status = true
  fenceForm.remark = ''
  fenceForm.patientIds = []
  clearDrawings()
}

onMounted(() => {
  initMap()
  fetchFences()
  fetchPatients()
})

onBeforeUnmount(() => {
  if (map) {
    map.remove()
  }
})
</script>

<style scoped>
.fence-container {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.fence-content {
  padding: 20px 0;
}

.fence-list-panel {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  height: 650px;
  display: flex;
  flex-direction: column;
}

.fence-list-panel h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: bold;
}

.search-input {
  margin-bottom: 15px;
}

.fence-radio-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.fence-radio {
  width: 100%;
  text-align: left;
}

.fence-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: 100%;
  gap: 8px;
  padding: 5px 0;
}

.fence-name {
  font-size: 14px;
  font-weight: 500;
  width: 100%;
}

.fence-type {
  margin-left: 0;
}

.fence-patients {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 5px;
}

.no-patients {
  width: 100%;
  margin-top: 5px;
}

.fence-patients .el-tag,
.no-patients .el-tag {
  margin: 0;
}

.fence-actions {
  margin-top: 15px;
  display: flex;
  gap: 10px;
}

.map-panel {
  height: 650px;
  display: flex;
  flex-direction: column;
}

.map-search {
  margin-bottom: 10px;
}

.fence-map {
  flex: 1;
  border-radius: 8px;
  overflow: hidden;
}

.map-controls {
  margin-top: 15px;
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>