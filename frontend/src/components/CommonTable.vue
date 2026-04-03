<template>
  <div class="common-table">
    <el-table
      v-loading="loading"
      :data="dataList"
      style="width: 100%"
      @selection-change="handleSelectionChange"
      :border="border"
      :stripe="stripe"
    >
      <el-table-column
        v-if="showSelection"
        type="selection"
        width="55"
        align="center"
      />
      
      <el-table-column
        v-for="column in columns"
        :key="column.prop"
        :prop="column.prop"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth"
        :align="column.align || 'left'"
        :sortable="column.sortable"
      >
        <template v-if="column.formatter" #default="scope">
          {{ column.formatter(scope.row, scope.column, scope.row[column.prop], scope.$index) }}
        </template>
        <template v-else-if="column.template" #default="scope">
          <component :is="column.template" :row="scope.row" :index="scope.$index" @action="handleAction" />
        </template>
      </el-table-column>
      
      <el-table-column
        v-if="showActions"
        label="操作"
        width="150"
        align="center"
      >
        <template #default="scope">
          <slot name="actions" :row="scope.row" :index="scope.$index" />
        </template>
      </el-table-column>
    </el-table>
    
    <div v-if="showPagination" class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="pageSizes"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, defineProps, defineEmits } from 'vue'

const props = defineProps({
  // 表格数据
  data: {
    type: Object,
    default: () => ({})
  },
  // 表格列配置
  columns: {
    type: Array,
    default: () => []
  },
  // 是否显示加载状态
  loading: {
    type: Boolean,
    default: false
  },
  // 是否显示选择框
  showSelection: {
    type: Boolean,
    default: false
  },
  // 是否显示操作列
  showActions: {
    type: Boolean,
    default: false
  },
  // 是否显示分页
  showPagination: {
    type: Boolean,
    default: true
  },
  // 是否显示边框
  border: {
    type: Boolean,
    default: true
  },
  // 是否显示斑马纹
  stripe: {
    type: Boolean,
    default: true
  },
  // 分页配置
  pagination: {
    type: Object,
    default: () => ({
      currentPage: 1,
      pageSize: 20,
      pageSizes: [10, 20, 50, 100]
    })
  }
})

const emit = defineEmits(['selection-change', 'size-change', 'current-change', 'action'])

// 计算属性：表格数据列表
const dataList = computed(() => {
  if (Array.isArray(props.data)) {
    return props.data
  } else if (props.data.list) {
    return props.data.list
  } else if (props.data.content) {
    return props.data.content
  } else if (props.data.data) {
    return props.data.data
  } else {
    return []
  }
})

// 计算属性：总数据量
const total = computed(() => {
  if (props.data.total) {
    return props.data.total
  } else if (props.data.totalElements) {
    return props.data.totalElements
  } else if (Array.isArray(props.data)) {
    return props.data.length
  } else {
    return 0
  }
})

// 当前页码
const currentPage = ref(props.pagination.currentPage)

// 每页大小
const pageSize = ref(props.pagination.pageSize)

// 每页大小选项
const pageSizes = ref(props.pagination.pageSizes)

// 处理选择变化
const handleSelectionChange = (selection) => {
  emit('selection-change', selection)
}

// 处理每页大小变化
const handleSizeChange = (size) => {
  pageSize.value = size
  emit('size-change', size)
}

// 处理当前页码变化
const handleCurrentChange = (current) => {
  currentPage.value = current
  emit('current-change', current)
}

// 处理操作
const handleAction = (action, row, index) => {
  emit('action', action, row, index)
}
</script>

<style scoped>
.common-table {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>