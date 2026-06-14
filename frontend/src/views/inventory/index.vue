<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, EditPen, Delete, WarningFilled } from '@element-plus/icons-vue'
import { queryPageApi, addApi, updateApi, deleteApi, adjustStockApi } from '@/api/modules/inventory'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import SkeletonTable from '@/components/SkeletonTable.vue'

// 搜索
const searchName = ref('')

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const itemList = ref([])

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增物料')
const isEdit = ref(false)
const formRef = ref()

const defaultForm = {
  id: null,
  name: '',
  category: 'dairy',
  stock: 0,
  unit: 'kg',
  safetyThreshold: 10,
  supplier: '',
  expiryDate: '',
  remark: '',
}
const form = reactive({ ...defaultForm })

const rules = {
  name: [
    { required: true, message: '请输入物料名称', trigger: 'blur' },
    { min: 2, max: 30, message: '名称长度 2-30 个字符', trigger: 'blur' },
  ],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  stock: [{ required: true, message: '请输入库存数量', trigger: 'blur' }],
  unit: [{ required: true, message: '请选择单位', trigger: 'change' }],
  safetyThreshold: [{ required: true, message: '请输入安全阈值', trigger: 'blur' }],
}

// 分类
const CATEGORIES = [
  { label: '乳制品', value: 'dairy', color: '#6b8cce' },
  { label: '面粉/谷物', value: 'grain', color: '#f0a35c' },
  { label: '糖/甜味剂', value: 'sugar', color: '#e8637a' },
  { label: '水果', value: 'fruit', color: '#5cb88a' },
  { label: '巧克力/可可', value: 'chocolate', color: '#8b5e3c' },
  { label: '油脂', value: 'oil', color: '#a78bfa' },
  { label: '包装材料', value: 'package', color: '#909399' },
  { label: '其他', value: 'other', color: '#b0b0b0' },
]

const UNITS = ['kg', 'g', 'L', 'ml', 'pcs', 'box', 'bag', 'bottle']

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const result = await queryPageApi(searchName.value, currentPage.value, pageSize.value)
    if (result.code) {
      itemList.value = result.data.records || result.data || []
      total.value = result.data.total || 0
    }
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const handleReset = () => {
  searchName.value = ''
  currentPage.value = 1
  loadData()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadData()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadData()
}

// 库存状态
const getStockStatus = (row) => {
  if (row.stock <= 0) return 'empty'
  if (row.stock <= row.safetyThreshold) return 'low'
  if (row.stock <= row.safetyThreshold * 2) return 'warning'
  return 'normal'
}

const stockStatusText = { empty: '已耗尽', low: '库存不足', warning: '库存偏低', normal: '正常' }
const stockStatusType = { empty: 'danger', low: 'danger', warning: 'warning', normal: 'success' }

// 新增
const openAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增物料'
  Object.assign(form, { ...defaultForm, id: null })
  dialogVisible.value = true
}

// 编辑
const openEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑物料'
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

// 提交
const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    const api = isEdit.value ? updateApi : addApi
    const result = await api(form)
    if (result.code) {
      ElMessage.success(isEdit.value ? '物料更新成功' : '物料新增成功')
      dialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(result.msg || '操作失败')
    }
  })
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除物料「${row.name}」吗？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    const result = await deleteApi(row.id)
    if (result.code) {
      ElMessage.success('删除成功')
      if (itemList.value.length === 1 && currentPage.value > 1) currentPage.value--
      loadData()
    } else {
      ElMessage.error(result.msg || '删除失败')
    }
  } catch {
    // 取消
  }
}

// 库存调整对话框
const adjustVisible = ref(false)
const adjustForm = reactive({ id: null, name: '', quantity: 0, remark: '' })
const adjustType = ref('in')

const openAdjust = (row, type) => {
  adjustForm.id = row.id
  adjustForm.name = row.name
  adjustForm.quantity = 0
  adjustForm.remark = ''
  adjustType.value = type
  adjustVisible.value = true
}

const submitAdjust = async () => {
  if (adjustForm.quantity <= 0) {
    ElMessage.warning('请输入有效的调整数量')
    return
  }
  const qty = adjustType.value === 'in' ? adjustForm.quantity : -adjustForm.quantity
  const result = await adjustStockApi(adjustForm.id, qty, adjustForm.remark)
  if (result.code) {
    ElMessage.success(adjustType.value === 'in' ? '入库成功' : '出库成功')
    adjustVisible.value = false
    loadData()
  } else {
    ElMessage.error(result.msg || '操作失败')
  }
}

// 一键采购（快捷操作）
const handleReorder = (row) => {
  const needQty = Math.max(row.safetyThreshold * 3 - row.stock, row.safetyThreshold)
  openAdjust(row, 'in')
  // 预填建议采购量
  setTimeout(() => {
    adjustForm.quantity = needQty
    adjustForm.remark = `安全库存补货建议（阈值: ${row.safetyThreshold}${row.unit}）`
  }, 100)
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="page-container">
    <PageHeader title="库存管理" description="管理原料、物料的库存、效期与采购补货">
      <template #actions>
        <el-button type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon>
          新增物料
        </el-button>
      </template>
    </PageHeader>

    <!-- 搜索栏 -->
    <div class="search-bar animate-fade-in-up delay-1">
      <el-input
        v-model="searchName"
        placeholder="搜索物料名称..."
        clearable
        style="width: 260px"
        @keyup.enter="handleSearch"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
      <el-button @click="handleReset">重置</el-button>
      <span class="result-count">共 {{ total }} 条记录</span>
    </div>

    <!-- 表格 -->
    <SkeletonTable v-if="loading" :rows="8" :columns="9" />
    <div v-else class="animate-fade-in-up delay-2">
      <el-table v-if="itemList.length > 0" :data="itemList" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="55" align="center" />
        <el-table-column prop="name" label="物料名称" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="name-cell">
              <span class="item-name">{{ row.name }}</span>
              <el-icon v-if="getStockStatus(row) === 'empty' || getStockStatus(row) === 'low'" class="alert-icon">
                <WarningFilled />
              </el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              size="small"
              :color="(CATEGORIES.find(c => c.value === row.category) || {}).color || '#909399'"
              effect="dark"
            >
              {{ (CATEGORIES.find(c => c.value === row.category) || {}).label || row.category }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前库存" width="110" align="right" sortable prop="stock">
          <template #default="{ row }">
            <span class="stock-value" :class="getStockStatus(row)">
              {{ row.stock }} {{ row.unit }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="安全阈值" width="100" align="right">
          <template #default="{ row }">
            <span class="threshold-value">{{ row.safetyThreshold }} {{ row.unit }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="stockStatusType[getStockStatus(row)]" size="small" effect="plain">
              {{ stockStatusText[getStockStatus(row)] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="供应商" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="supplier-text">{{ row.supplier || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="效期" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.expiryDate" class="expiry-text">{{ row.expiryDate }}</span>
            <span v-else class="na-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" link @click="openAdjust(row, 'in')">
              入库
            </el-button>
            <el-button type="warning" size="small" link @click="openAdjust(row, 'out')">
              出库
            </el-button>
            <el-button type="primary" size="small" link @click="openEdit(row)">
              <el-icon><EditPen /></el-icon>
            </el-button>
            <el-button
              v-if="getStockStatus(row) === 'low' || getStockStatus(row) === 'empty'"
              size="small"
              type="warning"
              link
              @click="handleReorder(row)"
            >
              采购
            </el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState
        v-else
        title="暂无库存数据"
        description="点击右上角「新增物料」添加第一个库存物料"
      />

      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          small
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="14">
            <el-form-item label="物料名称" prop="name">
              <el-input v-model="form.name" placeholder="如：法国淡奶油" maxlength="30" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="分类" prop="category">
              <el-select v-model="form.category" style="width: 100%">
                <el-option v-for="c in CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="当前库存" prop="stock">
              <el-input-number v-model="form.stock" :min="0" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unit">
              <el-select v-model="form.unit" style="width: 100%">
                <el-option v-for="u in UNITS" :key="u" :label="u" :value="u" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="安全阈值" prop="safetyThreshold">
              <el-input-number v-model="form.safetyThreshold" :min="0" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="供应商" prop="supplier">
          <el-input v-model="form.supplier" placeholder="供应商名称（选填）" />
        </el-form-item>
        <el-form-item label="效期" prop="expiryDate">
          <el-date-picker
            v-model="form.expiryDate"
            type="date"
            placeholder="选择到期日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息（选填）" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">
            {{ isEdit ? '保存修改' : '确认新增' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 库存调整对话框 -->
    <el-dialog
      v-model="adjustVisible"
      :title="adjustType === 'in' ? '入库操作' : '出库操作'"
      width="420px"
      destroy-on-close
    >
      <div class="adjust-info">
        <span class="adjust-label">物料：</span>
        <span class="adjust-name">{{ adjustForm.name }}</span>
      </div>
      <el-form label-width="80px" style="margin-top: 16px">
        <el-form-item label="调整数量">
          <el-input-number
            v-model="adjustForm.quantity"
            :min="1"
            :max="adjustType === 'out' ? 99999 : 99999"
            :step="1"
            style="width: 100%"
            :placeholder="adjustType === 'in' ? '入库数量' : '出库数量'"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="adjustForm.remark"
            type="textarea"
            :rows="2"
            :placeholder="adjustType === 'in' ? '入库原因（选填）' : '出库用途（选填）'"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="adjustVisible = false">取消</el-button>
          <el-button
            :type="adjustType === 'in' ? 'success' : 'warning'"
            @click="submitAdjust"
          >
            {{ adjustType === 'in' ? '确认入库' : '确认出库' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 1400px;
  margin: 0 auto;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.result-count {
  font-size: 13px;
  color: var(--color-text-muted, #a3949b);
  margin-left: auto;
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.item-name {
  font-weight: 600;
  color: var(--color-text-primary, #2d2327);
}

.alert-icon {
  color: #e8637a;
  font-size: 14px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.stock-value {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.stock-value.empty { color: #e8637a; }
.stock-value.low { color: #e8637a; }
.stock-value.warning { color: #f0a35c; }
.stock-value.normal { color: #5cb88a; }

.threshold-value {
  color: var(--color-text-secondary, #6b5a60);
}

.supplier-text,
.expiry-text,
.na-text {
  font-size: 13px;
  color: var(--color-text-secondary, #6b5a60);
}

.na-text {
  color: var(--color-text-muted, #a3949b);
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.adjust-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(45, 35, 39, 0.03);
  border-radius: 8px;
}

.adjust-label {
  font-size: 13px;
  color: var(--color-text-muted, #a3949b);
}

.adjust-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary, #2d2327);
}
</style>
