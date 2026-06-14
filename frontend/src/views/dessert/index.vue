<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, EditPen, Delete } from '@element-plus/icons-vue'
import { DESSERT_CATEGORIES } from '@/utils/constants'
import { queryPageApi, addApi, updateApi, deleteByIdApi, updateStatusApi } from '@/api/modules/dessert'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import SkeletonTable from '@/components/SkeletonTable.vue'

// 搜索
const searchName = ref('')
const searchCategory = ref('')

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const dessertList = ref([])

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增甜品')
const isEdit = ref(false)
const formRef = ref()

const defaultForm = {
  id: null,
  name: '',
  category: 'cake',
  description: '',
  price: 0,
  originalPrice: 0,
  image: '',
  status: 1,
}
const form = reactive({ ...defaultForm })

const rules = {
  name: [
    { required: true, message: '请输入甜品名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度 2-50 个字符', trigger: 'blur' },
  ],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
}

// 分类颜色
const categoryColors = {
  cake: '#e8637a',
  bread: '#f0a35c',
  drink: '#6b8cce',
  dessert: '#5cb88a',
  icecream: '#a78bfa',
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const result = await queryPageApi(
      searchName.value,
      searchCategory.value,
      currentPage.value,
      pageSize.value,
    )
    if (result.code) {
      dessertList.value = result.data.records || result.data || []
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
  searchCategory.value = ''
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

// 新增
const openAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增甜品'
  Object.assign(form, { ...defaultForm, id: null })
  dialogVisible.value = true
}

// 编辑
const openEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑甜品'
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
      ElMessage.success(isEdit.value ? '甜品更新成功' : '甜品新增成功')
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
    await ElMessageBox.confirm(`确定要删除甜品「${row.name}」吗？此操作不可恢复。`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    const result = await deleteByIdApi(row.id)
    if (result.code) {
      ElMessage.success('删除成功')
      if (dessertList.value.length === 1 && currentPage.value > 1) {
        currentPage.value--
      }
      loadData()
    } else {
      ElMessage.error(result.msg || '删除失败')
    }
  } catch {
    // 取消
  }
}

// 上下架
const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '上架' : '下架'
  const result = await updateStatusApi(row.id, newStatus)
  if (result.code) {
    row.status = newStatus
    ElMessage.success(`已${action}`)
  } else {
    ElMessage.error(result.msg || '操作失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="page-container">
    <PageHeader title="甜品管理" description="管理所有甜品的新增、编辑、上下架和删除">
      <template #actions>
        <el-button type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon>
          新增甜品
        </el-button>
      </template>
    </PageHeader>

    <!-- 搜索栏 -->
    <div class="search-bar animate-fade-in-up delay-1">
      <el-input
        v-model="searchName"
        placeholder="搜索甜品名称..."
        clearable
        style="width: 220px"
        @keyup.enter="handleSearch"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="searchCategory" placeholder="全部分类" clearable style="width: 140px">
        <el-option
          v-for="cat in DESSERT_CATEGORIES"
          :key="cat.value"
          :label="cat.label"
          :value="cat.value"
        />
      </el-select>
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
      <el-button @click="handleReset">重置</el-button>
      <span class="result-count">共 {{ total }} 条记录</span>
    </div>

    <!-- 表格 -->
    <SkeletonTable v-if="loading" :rows="8" :columns="8" />
    <div v-else class="animate-fade-in-up delay-2">
      <el-table
        v-if="dessertList.length > 0"
        :data="dessertList"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column label="图片" width="80" align="center">
          <template #default="{ row }">
            <img
              v-if="row.image"
              :src="row.image"
              class="dessert-thumb"
              :alt="row.name"
            />
            <span v-else class="thumb-placeholder">🧁</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="甜品名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="分类" width="90" align="center">
          <template #default="{ row }">
            <el-tag
              size="small"
              :style="{ color: categoryColors[row.category], borderColor: categoryColors[row.category] }"
              effect="plain"
            >
              {{ DESSERT_CATEGORIES.find(c => c.value === row.category)?.label || row.category }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column label="原价" width="90" align="right">
          <template #default="{ row }">
            <span class="price-original-col">¥{{ row.originalPrice?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="现价" width="90" align="right">
          <template #default="{ row }">
            <span class="price-current-col">¥{{ row.price?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              active-text="在售"
              inactive-text="下架"
              inline-prompt
              size="small"
              @change="toggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openEdit(row)">
              <el-icon><EditPen /></el-icon>
              编辑
            </el-button>
            <el-button
              size="small"
              link
              @click="$router.push(`/dessert/${row.id}`)"
            >
              评论
            </el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState
        v-else
        title="暂无甜品数据"
        description="点击右上角「新增甜品」按钮添加第一个甜品"
      />

      <!-- 分页 -->
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
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="甜品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入甜品名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" style="width: 100%">
            <el-option
              v-for="cat in DESSERT_CATEGORIES"
              :key="cat.value"
              :label="cat.label"
              :value="cat.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="甜品描述（选填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="原价" prop="originalPrice">
              <el-input-number
                v-model="form.originalPrice"
                :min="0"
                :precision="2"
                :step="1"
                style="width: 100%"
                placeholder="原价"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="现价" prop="price">
              <el-input-number
                v-model="form.price"
                :min="0"
                :precision="2"
                :step="1"
                style="width: 100%"
                placeholder="现价"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="图片" prop="image">
          <el-input v-model="form.image" placeholder="图片URL地址（选填）" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="在售"
            inactive-text="下架"
          />
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

.dessert-thumb {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
}

.thumb-placeholder {
  font-size: 24px;
}

.price-original-col {
  text-decoration: line-through;
  color: var(--color-text-muted, #a3949b);
  font-size: 13px;
}

.price-current-col {
  color: var(--color-primary-dark, #c9354a);
  font-weight: 600;
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
</style>
