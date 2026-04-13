<template>
  <div class="common-form">
    <el-form
      :model="formData"
      :rules="rules"
      ref="formRef"
      label-position="top"
      :label-width="labelWidth"
    >
      <el-form-item
        v-for="(field, index) in fields"
        :key="field.prop"
        :label="field.label"
        :prop="field.prop"
        :required="field.required"
        :span="field.span || 24"
      >
        <!-- 文本输入框 -->
        <el-input
          v-if="field.type === 'input'"
          v-model="formData[field.prop]"
          :placeholder="field.placeholder"
          :disabled="field.disabled"
          :maxlength="field.maxlength"
          :show-word-limit="field.showWordLimit"
          :type="field.inputType || 'text'"
        />
        
        <!-- 文本域 -->
        <el-input
          v-else-if="field.type === 'textarea'"
          v-model="formData[field.prop]"
          :placeholder="field.placeholder"
          :disabled="field.disabled"
          :rows="field.rows || 3"
          :maxlength="field.maxlength"
          :show-word-limit="field.showWordLimit"
          type="textarea"
        />
        
        <!-- 选择器 -->
        <el-select
          v-else-if="field.type === 'select'"
          v-model="formData[field.prop]"
          :placeholder="field.placeholder"
          :disabled="field.disabled"
          :multiple="field.multiple"
          :filterable="field.filterable"
        >
          <el-option
            v-for="option in field.options"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        
        <!-- 日期选择器 -->
        <el-date-picker
          v-else-if="field.type === 'date'"
          v-model="formData[field.prop]"
          :placeholder="field.placeholder"
          :disabled="field.disabled"
          :type="field.dateType || 'date'"
          :format="field.format || 'YYYY-MM-DD'"
          value-format="YYYY-MM-DD"
        />
        
        <!-- 时间选择器 -->
        <el-time-picker
          v-else-if="field.type === 'time'"
          v-model="formData[field.prop]"
          :placeholder="field.placeholder"
          :disabled="field.disabled"
          :format="field.format || 'HH:mm:ss'"
          value-format="HH:mm:ss"
        />
        
        <!-- 开关 -->
        <el-switch
          v-else-if="field.type === 'switch'"
          v-model="formData[field.prop]"
          :disabled="field.disabled"
          :active-text="field.activeText"
          :inactive-text="field.inactiveText"
        />
        
        <!-- 滑块 -->
        <el-slider
          v-else-if="field.type === 'slider'"
          v-model="formData[field.prop]"
          :disabled="field.disabled"
          :min="field.min || 0"
          :max="field.max || 100"
          :step="field.step || 1"
          :show-input="field.showInput"
        />
        
        <!-- 自定义组件 -->
        <component
          v-else-if="field.type === 'custom'"
          :is="field.component"
          v-model="formData[field.prop]"
          :props="field.props"
          :disabled="field.disabled"
        />
      </el-form-item>
    </el-form>
    
    <div class="form-actions">
      <slot name="actions">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
      </slot>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, defineProps, defineEmits, watch } from 'vue'

const props = defineProps({
  // 表单数据
  model: {
    type: Object,
    default: () => ({})
  },
  // 表单字段配置
  fields: {
    type: Array,
    default: () => []
  },
  // 表单验证规则
  rules: {
    type: Object,
    default: () => ({})
  },
  // 标签宽度
  labelWidth: {
    type: String,
    default: '120px'
  }
})

const emit = defineEmits(['submit', 'cancel', 'reset'])

// 表单引用
const formRef = ref(null)

// 表单数据
const formData = reactive({ ...props.model })

// 监听model变化，更新表单数据
watch(() => props.model, (newModel) => {
  Object.assign(formData, newModel)
}, { deep: true })

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    emit('submit', { ...formData })
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

// 取消操作
const handleCancel = () => {
  emit('cancel')
}

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  emit('reset')
}

// 暴露方法
defineExpose({
  resetForm,
  validate: () => formRef.value?.validate(),
  formData
})
</script>

<style scoped>
.common-form {
  margin-bottom: 20px;
}

.form-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.el-form-item__label) {
  color: var(--color-text-secondary) !important;
  font-weight: 500;
}

:deep(.el-input__wrapper) {
  background: var(--color-surface) !important;
  border-color: var(--color-border) !important;
  box-shadow: 0 0 0 1px var(--color-border) !important;
  border-radius: var(--radius-md) !important;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--color-border-strong) !important;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-primary), 0 0 0 3px var(--color-primary-light) !important;
}

:deep(.el-input__inner) {
  color: var(--color-text-primary) !important;
}

:deep(.el-input__inner::placeholder) {
  color: var(--color-text-muted) !important;
}

:deep(.el-select .el-input__wrapper) {
  background: var(--color-surface) !important;
}

:deep(.el-textarea__inner) {
  background: var(--color-surface);
  border-color: var(--color-border);
  color: var(--color-text-primary);
  border-radius: var(--radius-md);
}

:deep(.el-textarea__inner:focus) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-light);
}
</style>