<template>
  <div class="health-trend-chart">
    <div v-if="!points.length" class="chart-empty">暂无{{ title }}记录</div>
    <svg v-else class="trend-svg" :viewBox="`0 0 ${width} ${height}`" role="img" :aria-label="`${title}趋势图`">
      <g class="chart-grid">
        <line v-for="tick in yTicks" :key="`y-${tick.value}`" :x1="padding.left" :x2="width - padding.right" :y1="tick.y" :y2="tick.y" />
        <line :x1="padding.left" :x2="padding.left" :y1="padding.top" :y2="height - padding.bottom" />
        <line :x1="padding.left" :x2="width - padding.right" :y1="height - padding.bottom" :y2="height - padding.bottom" />
      </g>

      <g class="chart-labels">
        <text v-for="tick in yTicks" :key="`label-y-${tick.value}`" :x="padding.left - 10" :y="tick.y + 4" text-anchor="end">
          {{ formatNumber(tick.value) }}
        </text>
        <text v-for="label in xLabels" :key="`label-x-${label.index}`" :x="label.x" :y="height - padding.bottom + 25" :text-anchor="label.anchor">
          {{ label.text }}
        </text>
        <text :x="padding.left" :y="padding.top - 8">{{ unit }}</text>
      </g>

      <polyline class="trend-line" :points="linePoints" :style="{ stroke: color }" fill="none" />
      <g class="trend-points">
        <circle v-for="point in points" :key="point.key" :cx="point.x" :cy="point.y" r="4" :style="{ fill: color }">
          <title>{{ point.time }}：{{ formatNumber(point.value) }}{{ unit }}</title>
        </circle>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
  title: { type: String, default: '健康数据' },
  unit: { type: String, default: '' },
  color: { type: String, default: '#409eff' }
})

const width = 720
const height = 300
const padding = { top: 30, right: 24, bottom: 48, left: 62 }

const points = computed(() => {
  const values = props.data
    .map((item, index) => ({
      key: `${item.time || 'point'}-${index}`,
      time: item.time || '-',
      value: Number(item.value)
    }))
    .filter(item => Number.isFinite(item.value))

  if (!values.length) return []

  const rawMin = Math.min(...values.map(item => item.value))
  const rawMax = Math.max(...values.map(item => item.value))
  const range = rawMax - rawMin || Math.max(Math.abs(rawMax) * 0.1, 1)
  const min = rawMin - range * 0.1
  const max = rawMax + range * 0.1
  const plotWidth = width - padding.left - padding.right
  const plotHeight = height - padding.top - padding.bottom

  return values.map((item, index) => ({
    ...item,
    x: values.length === 1
      ? padding.left + plotWidth / 2
      : padding.left + (plotWidth * index) / (values.length - 1),
    y: padding.top + ((max - item.value) / (max - min)) * plotHeight
  }))
})

const chartRange = computed(() => {
  if (!points.value.length) return { min: 0, max: 1 }
  const values = points.value.map(point => point.value)
  const rawMin = Math.min(...values)
  const rawMax = Math.max(...values)
  const range = rawMax - rawMin || Math.max(Math.abs(rawMax) * 0.1, 1)
  return { min: rawMin - range * 0.1, max: rawMax + range * 0.1 }
})

const yTicks = computed(() => {
  const { min, max } = chartRange.value
  return Array.from({ length: 5 }, (_, index) => {
    const value = max - ((max - min) * index) / 4
    return {
      value,
      y: padding.top + ((height - padding.top - padding.bottom) * index) / 4
    }
  })
})

const linePoints = computed(() => points.value.map(point => `${point.x},${point.y}`).join(' '))

const xLabels = computed(() => {
  if (!points.value.length) return []
  const indexes = points.value.length <= 5
    ? points.value.map((_, index) => index)
    : [0, Math.floor((points.value.length - 1) / 2), points.value.length - 1]
  return [...new Set(indexes)].map((index, labelIndex, all) => ({
    index,
    x: points.value[index].x,
    text: formatTime(points.value[index].time),
    anchor: labelIndex === 0 ? 'start' : labelIndex === all.length - 1 ? 'end' : 'middle'
  }))
})

const formatTime = (value) => {
  if (!value) return '-'
  const text = String(value)
  return text.length > 16 ? text.slice(5, 16) : text
}

const formatNumber = (value) => Number(value).toFixed(1).replace(/\.0$/, '')
</script>

<style scoped>
.health-trend-chart {
  width: 100%;
  height: 300px;
  position: relative;
}

.trend-svg {
  width: 100%;
  height: 100%;
  display: block;
  overflow: visible;
}

.chart-grid line {
  stroke: var(--border-color, #dcdfe6);
  stroke-width: 1;
  stroke-dasharray: 3 4;
}

.chart-labels text {
  fill: var(--text-secondary, #909399);
  font-size: 11px;
}

.trend-line {
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.trend-points circle {
  stroke: var(--bg-card, #fff);
  stroke-width: 2;
}

.chart-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary, #909399);
  font-size: 14px;
  border: 1px dashed var(--border-color, #dcdfe6);
  border-radius: 6px;
}
</style>
