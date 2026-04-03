import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/styles.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// 按需导入ElementPlus组件，减少初始包大小
// 注意：这里只导入了常用组件，其他组件可以在需要时单独导入
import {
  ElButton,
  ElInput,
  ElForm,
  ElFormItem,
  ElDialog,
  ElTable,
  ElTableColumn,
  ElPagination,
  ElMessage,
  ElNotification,
  ElMessageBox,
  ElMenu,
  ElSubMenu,
  ElMenuItem,
  ElMenuItemGroup,
  ElDropdown,
  ElDropdownMenu,
  ElDropdownItem,
  ElCard,
  ElDivider,
  ElBadge,
  ElTag,
  ElTooltip,
  ElPopover,
  ElCollapse,
  ElCollapseItem,
  ElTabs,
  ElTabPane,
  ElSelect,
  ElOption,
  ElDatePicker,
  ElTimePicker,
  ElUpload,
  ElProgress,
  ElAlert,
  ElEmpty,
  ElSkeleton,
  ElResult
} from 'element-plus'

import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/notification/style/css'
import 'element-plus/es/components/message-box/style/css'

// 注册ElementPlus组件
app.component('ElButton', ElButton)
app.component('ElInput', ElInput)
app.component('ElForm', ElForm)
app.component('ElFormItem', ElFormItem)
app.component('ElDialog', ElDialog)
app.component('ElTable', ElTable)
app.component('ElTableColumn', ElTableColumn)
app.component('ElPagination', ElPagination)
app.component('ElMenu', ElMenu)
app.component('ElSubMenu', ElSubMenu)
app.component('ElMenuItem', ElMenuItem)
app.component('ElMenuItemGroup', ElMenuItemGroup)
app.component('ElDropdown', ElDropdown)
app.component('ElDropdownMenu', ElDropdownMenu)
app.component('ElDropdownItem', ElDropdownItem)
app.component('ElCard', ElCard)
app.component('ElDivider', ElDivider)
app.component('ElBadge', ElBadge)
app.component('ElTag', ElTag)
app.component('ElTooltip', ElTooltip)
app.component('ElPopover', ElPopover)
app.component('ElCollapse', ElCollapse)
app.component('ElCollapseItem', ElCollapseItem)
app.component('ElTabs', ElTabs)
app.component('ElTabPane', ElTabPane)
app.component('ElSelect', ElSelect)
app.component('ElOption', ElOption)
app.component('ElDatePicker', ElDatePicker)
app.component('ElTimePicker', ElTimePicker)
app.component('ElUpload', ElUpload)
app.component('ElProgress', ElProgress)
app.component('ElAlert', ElAlert)
app.component('ElEmpty', ElEmpty)
app.component('ElSkeleton', ElSkeleton)
app.component('ElResult', ElResult)

// 全局方法
app.config.globalProperties.$message = ElMessage
app.config.globalProperties.$notify = ElNotification
app.config.globalProperties.$confirm = ElMessageBox.confirm
app.config.globalProperties.$alert = ElMessageBox.alert
app.config.globalProperties.$prompt = ElMessageBox.prompt

app.mount('#app')
