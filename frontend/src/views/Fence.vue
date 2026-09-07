<template>
  <div class="fence-container">
    <!-- 页面标题栏 -->
    <div class="page-header">
      <h2 class="page-title">围栏管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        创建围栏
      </el-button>
    </div>

    <!-- 左右分栏布局 -->
    <div class="fence-layout">
      <!-- 左侧：围栏列表 -->
      <div class="fence-list-panel">
        <div class="list-card">
          <div class="list-card-header">
            <h3>围栏列表</h3>
            <el-tag type="info" size="small">{{ fences.length }} 个围栏</el-tag>
          </div>
          <el-table
            :data="fences"
            style="width: 100%"
            v-loading="loading"
            highlight-current-row
            @current-change="handleRowSelect"
            :row-class-name="getRowClassName"
            max-height="560"
          >
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="名称" min-width="120" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="90">
              <template #default="scope">
                <el-tag :type="getTypeTagType(scope.row.type)" size="small" effect="light">
                  {{ getTypeLabel(scope.row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="中心坐标" width="140" show-overflow-tooltip>
              <template #default="scope">
                <span v-if="scope.row.centerLat && scope.row.centerLng">
                  {{ scope.row.centerLat.toFixed(4) }}, {{ scope.row.centerLng.toFixed(4) }}
                </span>
                <span v-else class="text-muted">--</span>
              </template>
            </el-table-column>
            <el-table-column label="半径/顶点" width="90">
              <template #default="scope">
                <span v-if="scope.row.type === 'circle'">
                  {{ scope.row.radius ? scope.row.radius + 'm' : '--' }}
                </span>
                <span v-else>
                  {{ getVertexCount(scope.row) }}个顶点
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'active' ? 'success' : 'info'" size="small" effect="light">
                  {{ scope.row.status === 'active' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="关联病人" width="80">
              <template #default="scope">
                <span v-if="scope.row.patientIds && scope.row.patientIds.length > 0">
                  {{ scope.row.patientIds.length }}人
                </span>
                <span v-else class="text-muted">未关联</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="scope">
                <el-button type="primary" link size="small" @click="handleEdit(scope.row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="handleDelete(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 右侧：地图区域 -->
      <div class="fence-map-panel">
        <div class="map-card">
          <div id="fence-map" class="fence-map"></div>
          <!-- 地图绘制提示 -->
          <div class="map-hint" v-if="!drawingHintDismissed">
            <el-icon><InfoFilled /></el-icon>
            <span>使用地图上方绘制工具可在地图上绘制圆形、多边形或矩形围栏</span>
            <el-button link @click="drawingHintDismissed = true">关闭</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建/编辑围栏对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="580px" :close-on-click-modal="false" @closed="handleDialogClosed">
      <el-form :model="fenceForm" ref="fenceFormRef" label-width="100px">
        <el-form-item label="围栏名称" prop="name" :rules="[{ required: true, message: '请输入围栏名称', trigger: 'blur' }]">
          <el-input v-model="fenceForm.name" placeholder="请输入围栏名称" />
        </el-form-item>
        <el-form-item label="围栏类型" prop="type">
          <el-select v-model="fenceForm.type" placeholder="请选择围栏类型" style="width: 100%">
            <el-option label="圆形" value="circle" />
            <el-option label="多边形" value="polygon" />
            <el-option label="矩形" value="rectangle" />
          </el-select>
        </el-form-item>
        <!-- 圆形围栏：中心坐标和半径 -->
        <template v-if="fenceForm.type === 'circle'">
          <el-form-item label="中心纬度">
            <el-input-number v-model="fenceForm.centerLat" :precision="6" :step="0.001" :disabled="isCoordFromMap" style="width: 100%" />
          </el-form-item>
          <el-form-item label="中心经度">
            <el-input-number v-model="fenceForm.centerLng" :precision="6" :step="0.001" :disabled="isCoordFromMap" style="width: 100%" />
          </el-form-item>
          <el-form-item label="半径(米)" prop="radius" :rules="[{ required: true, message: '请输入半径', trigger: 'blur' }]">
            <el-input-number v-model="fenceForm.radius" :min="10" :max="50000" :step="100" :disabled="isCoordFromMap" style="width: 100%" />
          </el-form-item>
        </template>
        <!-- 多边形/矩形围栏：顶点信息 -->
        <template v-if="fenceForm.type === 'polygon' || fenceForm.type === 'rectangle'">
          <el-form-item label="顶点数量">
            <el-input :model-value="getFormVertexCount() + ' 个顶点'" disabled style="width: 100%" />
          </el-form-item>
        </template>
        <el-form-item label="状态">
          <el-radio-group v-model="fenceForm.status">
            <el-radio value="active">启用</el-radio>
            <el-radio value="inactive">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关联病人">
          <el-select v-model="fenceForm.patientIds" multiple placeholder="选择关联病人" filterable style="width: 100%">
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
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { fenceApi } from '../api/fence'
import { patientApi } from '../api/patient'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, InfoFilled } from '@element-plus/icons-vue'
import { addTiandituToMap } from '../utils/tianditu'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import 'leaflet-draw'
import 'leaflet-draw/dist/leaflet.draw.css'

/** 修复 Leaflet 默认图标路径问题 */
delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png'
})

/** 围栏列表数据 */
const fences = ref([])
/** 列表加载状态 */
const loading = ref(false)
/** 对话框可见性 */
const dialogVisible = ref(false)
/** 对话框标题 */
const dialogTitle = ref('创建围栏')
/** 保存按钮加载状态 */
const saveLoading = ref(false)
/** 表单引用 */
const fenceFormRef = ref()
/** 所有病人列表 */
const allPatients = ref([])
/** 当前选中的围栏ID */
const selectedFenceId = ref(null)
/** 绘制提示是否已关闭 */
const drawingHintDismissed = ref(false)
/** 坐标是否来自地图绘制 */
const isCoordFromMap = ref(false)

/** 围栏表单数据 */
const fenceForm = reactive({
  id: null,
  name: '',
  type: 'circle',
  centerLat: null,
  centerLng: null,
  radius: 500,
  coordinates: null,
  status: 'active',
  patientIds: []
})

/** Leaflet 地图实例 */
let map = null
/** 围栏图层映射 */
let fenceLayers = new Map()
/** 绘制控件 */
let drawControl = null
/** 绘制图层组 */
let drawnItems = null
/** 临时绘制图层 */
let tempDrawnLayer = null

/** 活跃围栏颜色 */
const ACTIVE_COLOR = '#2563EB'
/** 停用围栏颜色 */
const INACTIVE_COLOR = '#94A3B8'
/** 选中高亮颜色 */
const SELECTED_COLOR = '#FF6B6B'

/**
 * 初始化 Leaflet 地图
 */
const initMap = () => {
  map = L.map('fence-map', { center: [39.9042, 116.4074], zoom: 10, minZoom: 3, maxZoom: 18 })
  addTiandituToMap(map)
  drawnItems = new L.FeatureGroup()
  map.addLayer(drawnItems)
  initDrawControl()
}

/**
 * 初始化 leaflet-draw 绘制控件
 */
const initDrawControl = () => {
  drawControl = new L.Control.Draw({
    position: 'topleft',
    draw: {
      circle: { shapeOptions: { color: ACTIVE_COLOR, fillColor: ACTIVE_COLOR, fillOpacity: 0.15, weight: 2 } },
      polygon: { shapeOptions: { color: ACTIVE_COLOR, fillColor: ACTIVE_COLOR, fillOpacity: 0.15, weight: 2 }, allowIntersection: false },
      rectangle: { shapeOptions: { color: ACTIVE_COLOR, fillColor: ACTIVE_COLOR, fillOpacity: 0.15, weight: 2 } },
      polyline: false, marker: false, circlemarker: false
    },
    edit: { featureGroup: drawnItems, edit: false, remove: false }
  })
  map.addControl(drawControl)
  map.on(L.Draw.Event.CREATED, handleDrawCreated)
}

/**
 * 处理地图绘制完成事件
 */
const handleDrawCreated = (e) => {
  const layer = e.layer
  const layerType = e.layerType
  if (tempDrawnLayer) drawnItems.removeLayer(tempDrawnLayer)
  drawnItems.addLayer(layer)
  tempDrawnLayer = layer
  resetFenceForm()

  if (layerType === 'circle') {
    const latlng = layer.getLatLng()
    const radius = layer.getRadius()
    fenceForm.type = 'circle'
    fenceForm.centerLat = parseFloat(latlng.lat.toFixed(6))
    fenceForm.centerLng = parseFloat(latlng.lng.toFixed(6))
    fenceForm.radius = Math.round(radius)
    fenceForm.coordinates = JSON.stringify([latlng.lng, latlng.lat])
  } else if (layerType === 'polygon') {
    const latlngs = layer.getLatLngs()[0]
    fenceForm.type = 'polygon'
    fenceForm.coordinates = JSON.stringify(latlngs.map(ll => [parseFloat(ll.lng.toFixed(6)), parseFloat(ll.lat.toFixed(6))]))
    const center = layer.getBounds().getCenter()
    fenceForm.centerLat = parseFloat(center.lat.toFixed(6))
    fenceForm.centerLng = parseFloat(center.lng.toFixed(6))
    fenceForm.radius = null
  } else if (layerType === 'rectangle') {
    const latlngs = layer.getLatLngs()[0]
    fenceForm.type = 'rectangle'
    fenceForm.coordinates = JSON.stringify(latlngs.map(ll => [parseFloat(ll.lng.toFixed(6)), parseFloat(ll.lat.toFixed(6))]))
    const center = layer.getBounds().getCenter()
    fenceForm.centerLat = parseFloat(center.lat.toFixed(6))
    fenceForm.centerLng = parseFloat(center.lng.toFixed(6))
    fenceForm.radius = null
  }

  isCoordFromMap.value = true
  dialogTitle.value = '创建围栏'
  dialogVisible.value = true
}

/**
 * 在地图上绘制所有围栏
 */
const drawAllFences = () => {
  if (!map) return
  clearAllFenceLayers()
  fences.value.forEach(fence => drawFenceOnMap(fence))
}

/**
 * 在地图上绘制单个围栏
 */
const drawFenceOnMap = (fence) => {
  if (!map || !fence) return
  removeFenceLayer(fence.id)
  const isSelected = fence.id === selectedFenceId.value
  const color = isSelected ? SELECTED_COLOR : (fence.status === 'active' ? ACTIVE_COLOR : INACTIVE_COLOR)
  let layer = null

  if (fence.type === 'circle') {
    if (fence.centerLat && fence.centerLng && fence.radius) {
      layer = L.circle([fence.centerLat, fence.centerLng], {
        radius: fence.radius, color, fillColor: color,
        fillOpacity: isSelected ? 0.25 : 0.12, weight: isSelected ? 3 : 2, interactive: true
      })
    }
  } else if (fence.type === 'polygon' || fence.type === 'rectangle') {
    let coords = fence.coordinates
    if (typeof coords === 'string') { try { coords = JSON.parse(coords) } catch { return } }
    if (Array.isArray(coords) && coords.length >= 3) {
      const latLngs = coords.map(c => Array.isArray(c) && c.length === 2 ? [c[1], c[0]] : null).filter(Boolean)
      if (latLngs.length >= 3) {
        layer = L.polygon(latLngs, { color, fillColor: color, fillOpacity: isSelected ? 0.25 : 0.12, weight: isSelected ? 3 : 2, interactive: true })
      }
    }
  }

  if (layer) {
    layer.bindPopup(`<b>${fence.name || '未命名'}</b><br>类型: ${getTypeLabel(fence.type)}<br>状态: ${fence.status === 'active' ? '启用' : '停用'}`)
    layer.on('click', () => selectFence(fence.id))
    layer.addTo(map)
    layer._fenceId = fence.id
    fenceLayers.set(fence.id, layer)
  }
}

/** 从地图上移除指定围栏图层 */
const removeFenceLayer = (fenceId) => {
  if (map && fenceLayers.has(fenceId)) { map.removeLayer(fenceLayers.get(fenceId)); fenceLayers.delete(fenceId) }
}

/** 清除地图上所有围栏图层 */
const clearAllFenceLayers = () => {
  if (!map) return
  fenceLayers.forEach(layer => map.removeLayer(layer))
  fenceLayers.clear()
}

/**
 * 选中指定围栏
 */
const selectFence = (fenceId) => {
  const prevId = selectedFenceId.value
  selectedFenceId.value = fenceId
  if (prevId !== null && prevId !== fenceId) {
    const prevFence = fences.value.find(f => f.id === prevId)
    if (prevFence) drawFenceOnMap(prevFence)
  }
  const fence = fences.value.find(f => f.id === fenceId)
  if (fence) {
    drawFenceOnMap(fence)
    flyToFence(fence)
  }
}

/** 地图飞到指定围栏位置 */
const flyToFence = (fence) => {
  if (!map || !fence) return
  if (fence.type === 'circle' && fence.centerLat && fence.centerLng) {
    map.flyTo([fence.centerLat, fence.centerLng], 14, { duration: 0.8 })
  } else if (fence.type === 'polygon' || fence.type === 'rectangle') {
    const layer = fenceLayers.get(fence.id)
    if (layer && layer.getBounds) map.flyToBounds(layer.getBounds(), { padding: [50, 50], duration: 0.8 })
  }
}

/**
 * 获取围栏列表
 */
const fetchFences = async () => {
  loading.value = true
  try {
    const data = await fenceApi.getFences()
    let fenceList = []
    if (Array.isArray(data)) fenceList = data
    else if (data && Array.isArray(data.list)) fenceList = data.list
    else if (data && Array.isArray(data.content)) fenceList = data.content
    else if (data && Array.isArray(data.data)) fenceList = data.data
    fences.value = fenceList
    await nextTick()
    drawAllFences()
  } catch (e) {
    ElMessage.error('获取围栏列表失败')
  } finally {
    loading.value = false
  }
}

/** 获取病人列表 */
const fetchPatients = async () => {
  try {
    const data = await patientApi.getPatients({ limit: 100 })
    allPatients.value = data?.content || (Array.isArray(data) ? data : [])
  } catch { /* 忽略 */ }
}

/** 重置围栏表单 */
const resetFenceForm = () => {
  Object.assign(fenceForm, { id: null, name: '', type: 'circle', centerLat: null, centerLng: null, radius: 500, coordinates: null, status: 'active', patientIds: [] })
  isCoordFromMap.value = false
}

/** 处理创建围栏按钮点击 */
const handleAdd = () => {
  resetFenceForm()
  dialogTitle.value = '创建围栏'
  dialogVisible.value = true
}

/**
 * 处理编辑围栏
 */
const handleEdit = (fence) => {
  dialogTitle.value = '编辑围栏'
  Object.assign(fenceForm, {
    id: fence.id, name: fence.name || '', type: fence.type || 'circle',
    centerLat: fence.centerLat, centerLng: fence.centerLng, radius: fence.radius,
    coordinates: typeof fence.coordinates === 'string' ? fence.coordinates : (fence.coordinates ? JSON.stringify(fence.coordinates) : null),
    status: fence.status || 'active', patientIds: fence.patientIds || []
  })
  isCoordFromMap.value = false
  selectFence(fence.id)
  dialogVisible.value = true
}

/**
 * 保存围栏
 */
const handleSave = async () => {
  if (!fenceFormRef.value) return
  try { await fenceFormRef.value.validate() } catch { return }
  saveLoading.value = true
  try {
    const submitData = { ...fenceForm }
    if (submitData.type === 'circle') {
      submitData.coordinates = JSON.stringify([submitData.centerLng, submitData.centerLat])
    } else if (submitData.type === 'polygon' || submitData.type === 'rectangle') {
      if (!submitData.coordinates) { ElMessage.warning('请先在地图上绘制围栏区域'); saveLoading.value = false; return }
      if (typeof submitData.coordinates !== 'string') submitData.coordinates = JSON.stringify(submitData.coordinates)
    }
    if (submitData.id) {
      await fenceApi.updateFence(submitData.id, submitData)
      ElMessage.success('围栏更新成功')
    } else {
      await fenceApi.createFence(submitData)
      ElMessage.success('围栏创建成功')
    }
    dialogVisible.value = false
    if (tempDrawnLayer) { drawnItems.removeLayer(tempDrawnLayer); tempDrawnLayer = null }
    await fetchFences()
  } catch (e) {
    ElMessage.error(fenceForm.id ? '围栏更新失败' : '围栏创建失败')
  } finally {
    saveLoading.value = false
  }
}

/**
 * 删除围栏
 */
const handleDelete = (fence) => {
  ElMessageBox.confirm(`确定要删除围栏「${fence.name}」吗？此操作不可撤销。`, '删除围栏', {
    confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning'
  }).then(async () => {
    try {
      await fenceApi.deleteFence(fence.id)
      ElMessage.success('围栏删除成功')
      removeFenceLayer(fence.id)
      if (selectedFenceId.value === fence.id) selectedFenceId.value = null
      await fetchFences()
    } catch { ElMessage.error('围栏删除失败') }
  }).catch(() => {})
}

/** 处理表格行选中事件 */
const handleRowSelect = (row) => { if (row) selectFence(row.id) }

/** 获取表格行的 CSS 类名 */
const getRowClassName = ({ row }) => row.id === selectedFenceId.value ? 'fence-row-selected' : ''

/** 对话框关闭后回调 */
const handleDialogClosed = () => {
  if (tempDrawnLayer) { drawnItems.removeLayer(tempDrawnLayer); tempDrawnLayer = null }
  isCoordFromMap.value = false
}

/** 获取围栏类型中文标签 */
const getTypeLabel = (type) => ({ circle: '圆形', polygon: '多边形', rectangle: '矩形' }[type] || type || '未知')

/** 获取围栏类型 Tag 类型 */
const getTypeTagType = (type) => ({ circle: 'primary', polygon: 'success', rectangle: 'warning' }[type] || 'info')

/** 获取围栏顶点数量 */
const getVertexCount = (fence) => {
  let coords = fence.coordinates
  if (typeof coords === 'string') { try { coords = JSON.parse(coords) } catch { return 0 } }
  return Array.isArray(coords) ? coords.length : 0
}

/** 获取表单中的顶点数量 */
const getFormVertexCount = () => {
  if (!fenceForm.coordinates) return 0
  let coords = fenceForm.coordinates
  if (typeof coords === 'string') { try { coords = JSON.parse(coords) } catch { return 0 } }
  return Array.isArray(coords) ? coords.length : 0
}

onMounted(async () => {
  await nextTick()
  initMap()
  await fetchFences()
  fetchPatients()
})

onBeforeUnmount(() => {
  if (map) {
    map.off(L.Draw.Event.CREATED, handleDrawCreated)
    clearAllFenceLayers()
    if (drawnItems) drawnItems.clearLayers()
    if (drawControl) map.removeControl(drawControl)
    map.remove()
    map = null
  }
  fenceLayers.clear()
  drawnItems = null
  drawControl = null
  tempDrawnLayer = null
})
</script>

<style scoped>
.fence-container {
  width: 100%;
  padding: var(--space-5, 24px);
  font-family: var(--font-sans, "Inter", "PingFang SC", "Microsoft YaHei", system-ui, sans-serif);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-5, 24px);
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary, #1E293B);
  margin: 0;
}

.fence-layout {
  display: grid;
  grid-template-columns: 33% 66%;
  gap: var(--space-5, 24px);
  min-height: 620px;
}

.map-card {
  background: var(--color-surface, #FFFFFF);
  border: 1px solid var(--color-border, #E2E8F0);
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  position: relative;
  height: 620px;
}

.fence-map { width: 100%; height: 100%; z-index: 1; }

.map-hint {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  background: var(--color-surface, #FFFFFF);
  border: 1px solid var(--color-border, #E2E8F0);
  border-radius: 12px;
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--color-text-secondary, #64748B);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  white-space: nowrap;
}

.map-hint .el-icon { color: var(--color-primary, #2563EB); font-size: 16px; }

.list-card {
  background: var(--color-surface, #FFFFFF);
  border: 1px solid var(--color-border, #E2E8F0);
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 620px;
}

.list-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid var(--color-border, #E2E8F0);
  background: var(--color-surface-raised, #F1F5F9);
}

.list-card-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: var(--color-text-primary, #1E293B); }

:deep(.fence-row-selected) { background-color: var(--color-primary-light, #DBEAFE) !important; }
:deep(.fence-row-selected:hover > td) { background-color: var(--color-primary-light, #DBEAFE) !important; }

.text-muted { color: var(--color-text-muted, #94A3B8); font-size: 12px; }

:deep(.leaflet-draw-toolbar a) { background-color: var(--color-surface, #FFFFFF) !important; border-color: var(--color-border, #E2E8F0) !important; }
:deep(.leaflet-draw-toolbar a:hover) { background-color: var(--color-surface-raised, #F1F5F9) !important; }
:deep(.leaflet-draw-actions) { background-color: var(--color-surface, #FFFFFF) !important; border: 1px solid var(--color-border, #E2E8F0) !important; border-radius: 8px !important; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08) !important; }
:deep(.leaflet-draw-actions a) { background-color: var(--color-surface, #FFFFFF) !important; color: var(--color-text-primary, #1E293B) !important; border-color: var(--color-border, #E2E8F0) !important; }
:deep(.leaflet-draw-actions a:hover) { background-color: var(--color-surface-raised, #F1F5F9) !important; }
:deep(.leaflet-popup-content-wrapper) { border-radius: 12px !important; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.08) !important; }
:deep(.leaflet-popup-content) { margin: 12px 16px !important; font-size: 14px !important; color: var(--color-text-primary, #1E293B) !important; }

@media (max-width: 768px) {
  .fence-layout { grid-template-columns: 1fr; min-height: auto; }
  .map-card { height: 400px; }
  .list-card { height: auto; max-height: 500px; }
}
</style>
