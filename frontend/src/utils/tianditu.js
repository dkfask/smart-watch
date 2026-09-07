/**
 * 天地图（Tianditu）图层管理工具
 * 
 * 天地图使用国家2000大地坐标系（CGCS2000，与WGS-84亚毫米级一致），
 * 手环上传的原生GPS经纬度无需经由GCJ-02偏移计算，可直接对齐天地图底图。
 */
import L from 'leaflet'

// 默认天地图应用 Key（通过环境变量 VITE_TIANDITU_KEY 或后端 /api/config/map 接口动态注入）
export const DEFAULT_TIANDITU_KEY = import.meta.env.VITE_TIANDITU_KEY || ''

let currentKey = DEFAULT_TIANDITU_KEY

export function setTiandituKey(key) {
  if (key && typeof key === 'string' && key.trim()) {
    currentKey = key.trim()
  }
}

export function getTiandituKey() {
  return currentKey
}

/**
 * 创建天地图瓦片图层
 * 天地图由底图（vec_w/img_w）和注记图层（cva_w/cia_w）组合而成
 * @param {string} [type='vec'] 图层类型：vec(矢量), img(卫星影像)
 * @param {string} [key] 自定义天地图Key
 * @returns {L.LayerGroup} Leaflet 图层组，包含底图和文字注记
 */
export function createTiandituLayers(type = 'vec', key = currentKey) {
  const isVec = type !== 'img'
  const baseLayerType = isVec ? 'vec_w' : 'img_w'
  const annoLayerType = isVec ? 'cva_w' : 'cia_w'
  const subdomains = ['0', '1', '2', '3', '4', '5', '6', '7']

  // 1. 底图图层 (球面墨卡托切片 EPSG:3857)
  const baseLayer = L.tileLayer(
    `https://t{s}.tianditu.gov.cn/${baseLayerType}/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=${isVec ? 'vec' : 'img'}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${key}`,
    {
      subdomains,
      maxZoom: 18,
      minZoom: 1,
      tileSize: 256,
      attribution: '© 天地图 GS(2024)0082号'
    }
  )

  // 2. 中文注记图层 (道路名、地名等文字标注)
  const annoLayer = L.tileLayer(
    `https://t{s}.tianditu.gov.cn/${annoLayerType}/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=${isVec ? 'cva' : 'cia'}&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=${key}`,
    {
      subdomains,
      maxZoom: 18,
      minZoom: 1,
      tileSize: 256
    }
  )

  return L.layerGroup([baseLayer, annoLayer])
}

/**
 * 为已有的 Leaflet 地图实例添加天地图图层
 * @param {L.Map} map Leaflet Map 实例
 * @param {string} [type='vec'] 'vec' 或 'img'
 * @param {string} [key] 天地图 key
 * @returns {L.LayerGroup} 添加的图层组
 */
export function addTiandituToMap(map, type = 'vec', key = currentKey) {
  const group = createTiandituLayers(type, key)
  group.addTo(map)
  return group
}
