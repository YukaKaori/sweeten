<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, EditPen, Delete, Upload } from '@element-plus/icons-vue'
import { DESSERT_CATEGORIES, CATEGORY_COLORS, getLabelByValue } from '@/utils/constants'
import { queryPageApi, addApi, updateApi, deleteByIdApi, updateStatusApi } from '@/api/modules/dessert'
import { uploadImageApi, UPLOAD_ACCEPT, UPLOAD_MAX_MB } from '@/api/modules/upload'
import ProductImg from '@/components/ProductImg.vue'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import SkeletonTable from '@/components/SkeletonTable.vue'

/** 库存低于该值时在列表中标黄提醒（与首页库存预警口径接近） */
const LOW_STOCK = 10

// 搜索（支持从首页库存预警带 ?name= 定位到具体甜品）
const route = useRoute()
const searchName = ref(typeof route.query.name === 'string' ? route.query.name : '')
const searchCategory = ref('')

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const loadFailed = ref(false)
const dessertList = ref([])

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增甜品')
const isEdit = ref(false)
const formRef = ref()
const submitting = ref(false)
const uploading = ref(false)

const defaultForm = {
  id: null,
  name: '',
  category: 'cake',
  description: '',
  price: undefined,
  originalPrice: undefined,
  stock: 50,
  image: '',
  status: 1,
}
const form = reactive({ ...defaultForm })

const validateOriginalPrice = (rule, value, callback) => {
  if (value == null || value === '') return callback()
  if (form.price != null && value < form.price) return callback(new Error('原价不应低于现价'))
  callback()
}

const rules = {
  name: [
    { required: true, message: '请输入甜品名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度 2-50 个字符', trigger: 'blur' },
  ],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [
    { required: true, message: '请输入现价', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '现价必须大于 0', trigger: 'blur' },
  ],
  originalPrice: [{ validator: validateOriginalPrice, trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
}

// 加载数据（后端 PageResult 结构为 { total, rows }）
const loadData = async () => {
  loading.value = true
  loadFailed.value = false
  try {
    const result = await queryPageApi(
      searchName.value.trim(),
      searchCategory.value,
      currentPage.value,
      pageSize.value,
    )
    if (result.code) {
      dessertList.value = result.data?.rows || []
      total.value = result.data?.total || 0
    } else {
      loadFailed.value = true
      ElMessage.error(result.msg || '甜品列表加载失败')
    }
  } catch {
    loadFailed.value = true
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
  Object.assign(form, { ...defaultForm })
  dialogVisible.value = true
}

// 编辑：只带表单字段，避免把 createTime/sales 等列表快照回写
const openEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑甜品'
  Object.assign(form, {
    id: row.id,
    name: row.name,
    category: row.category,
    description: row.description || '',
    price: row.price,
    originalPrice: row.originalPrice ?? undefined,
    stock: row.stock ?? 0,
    image: row.image || '',
    status: row.status ?? 1,
  })
  dialogVisible.value = true
}

// 图片上传（走后端 /upload → 阿里云 OSS）
const beforeImageUpload = (file) => {
  if (!UPLOAD_ACCEPT.includes(file.type)) {
    ElMessage.error('仅支持 JPG / PNG / GIF / WEBP 图片')
    return false
  }
  if (file.size / 1024 / 1024 > UPLOAD_MAX_MB) {
    ElMessage.error(`图片不能超过 ${UPLOAD_MAX_MB}MB`)
    return false
  }
  return true
}

const uploadImage = async ({ file }) => {
  uploading.value = true
  try {
    const result = await uploadImageApi(file)
    if (result.code && result.data) {
      form.image = result.data
      ElMessage.success('图片已上传')
    } else {
      ElMessage.error(result.msg || '图片上传失败')
    }
  } catch {
    // 拦截器已提示
  } finally {
    uploading.value = false
  }
}

// 提交
const submitForm = async () => {
  if (!formRef.value || submitting.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = {
      ...form,
      name: form.name.trim(),
      description: form.description?.trim() || '',
      image: form.image?.trim() || undefined,
      originalPrice: form.originalPrice ?? undefined,
    }
    const result = await (isEdit.value ? updateApi(payload) : addApi(payload))
    if (result.code) {
      ElMessage.success(isEdit.value ? '甜品更新成功' : '甜品新增成功')
      dialogVisible.value = false
      if (!isEdit.value) currentPage.value = 1
      loadData()
    } else {
      ElMessage.error(result.msg || '操作失败')
    }
  } catch {
    // 拦截器已展示后端原因（如名称重复、字段校验）
  } finally {
    submitting.value = false
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除甜品「${row.name}」吗？此操作不可恢复。`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return // 取消
  }
  try {
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
    // 拦截器已提示
  }
}

// 上下架（请求中禁用开关，防止连点）
const toggleStatus = async (row) => {
  if (row._toggling) return
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '上架' : '下架'
  row._toggling = true
  try {
    const result = await updateStatusApi(row.id, newStatus)
    if (result.code) {
      row.status = newStatus
      ElMessage.success(`「${row.name}」已${action}`)
    } else {
      ElMessage.error(result.msg || '操作失败')
    }
  } catch {
    // 拦截器已提示
  } finally {
    row._toggling = false
  }
}

const stockTagType = (stock) => (stock <= 0 ? 'danger' : stock < LOW_STOCK ? 'warning' : 'info')

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="page-container">
    <PageHeader title="甜品管理" description="管理甜品的上新、价格、库存与上下架">
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
        @clear="handleSearch"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select
        v-model="searchCategory"
        placeholder="全部分类"
        clearable
        style="width: 140px"
        @change="handleSearch"
      >
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
            <ProductImg :item="row" :width="48" :alt="row.name" class="dessert-thumb" />
          </template>
        </el-table-column>
        <el-table-column label="甜品名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <router-link :to="`/dessert/${row.id}`" class="name-link">{{ row.name }}</router-link>
            <p v-if="row.description" class="row-desc">{{ row.description }}</p>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="90" align="center">
          <template #default="{ row }">
            <el-tag
              size="small"
              :style="{ color: CATEGORY_COLORS[row.category], borderColor: CATEGORY_COLORS[row.category] }"
              effect="plain"
            >
              {{ getLabelByValue(DESSERT_CATEGORIES, row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="原价" width="90" align="right">
          <template #default="{ row }">
            <span v-if="row.originalPrice > row.price" class="price-original-col">¥{{ row.originalPrice.toFixed(2) }}</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="现价" width="90" align="right">
          <template #default="{ row }">
            <span class="price-current-col">¥{{ row.price?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="stockTagType(row.stock ?? 0)" size="small" effect="light" round>
              {{ (row.stock ?? 0) <= 0 ? '售罄' : row.stock }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="80" align="right" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="row._toggling"
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
            <el-button size="small" link @click="$router.push(`/dessert/${row.id}`)">
              详情
            </el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState
        v-else-if="loadFailed"
        title="甜品列表加载失败"
        description="请检查网络或稍后重试"
      >
        <template #action>
          <el-button @click="loadData">重新加载</el-button>
        </template>
      </EmptyState>
      <EmptyState
        v-else-if="searchName || searchCategory"
        title="没有符合条件的甜品"
        description="换个关键词或分类试试"
      >
        <template #action>
          <el-button @click="handleReset">清除筛选</el-button>
        </template>
      </EmptyState>
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
      width="580px"
      destroy-on-close
      :close-on-click-modal="!submitting"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        @submit.prevent="submitForm"
      >
        <el-form-item label="甜品名称" prop="name">
          <el-input v-model="form.name" placeholder="如：草莓奶油蛋糕" maxlength="50" show-word-limit />
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
            :rows="2"
            placeholder="一句话介绍口味与用料（选填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="现价" prop="price">
              <el-input-number
                v-model="form.price"
                :min="0.01"
                :precision="2"
                :step="1"
                controls-position="right"
                style="width: 100%"
                placeholder="必填"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原价" prop="originalPrice">
              <el-input-number
                v-model="form.originalPrice"
                :min="0.01"
                :precision="2"
                :step="1"
                controls-position="right"
                style="width: 100%"
                placeholder="选填，划线价"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="库存" prop="stock">
              <el-input-number
                v-model="form.stock"
                :min="0"
                :precision="0"
                :step="10"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-switch
                v-model="form.status"
                :active-value="1"
                :inactive-value="0"
                active-text="在售"
                inactive-text="下架"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="图片" prop="image">
          <div class="image-field">
            <ProductImg :key="form.image" :item="{ image: form.image }" :width="96" class="image-preview" eager />
            <div class="image-field__body">
              <el-upload
                :show-file-list="false"
                :accept="UPLOAD_ACCEPT.join(',')"
                :before-upload="beforeImageUpload"
                :http-request="uploadImage"
                :disabled="uploading"
              >
                <el-button :loading="uploading" size="small">
                  <el-icon v-if="!uploading"><Upload /></el-icon>
                  {{ form.image ? '更换图片' : '上传图片' }}
                </el-button>
              </el-upload>
              <el-input v-model="form.image" size="small" placeholder="或粘贴图片 URL（选填）" clearable />
              <p class="image-hint">
                JPG/PNG/WEBP，≤ {{ UPLOAD_MAX_MB }}MB，上传至阿里云 OSS。当前全站临时统一展示草莓蛋糕图，商品自己的图片在关闭统一模式后生效。
              </p>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button :disabled="submitting" @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" :disabled="uploading" @click="submitForm">
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

.name-link {
  color: var(--color-text-primary);
  font-weight: 500;
  text-decoration: none;
}

.name-link:hover {
  color: var(--color-primary);
}

.row-desc {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.muted {
  color: var(--color-text-muted);
}

.image-field {
  display: flex;
  gap: 14px;
  width: 100%;
}

.image-preview {
  width: 96px;
  height: 96px;
  flex-shrink: 0;
  border-radius: var(--radius-lg);
  object-fit: cover;
  border: 1px solid var(--color-border);
  background: var(--color-bg-tertiary);
}

.image-field__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.image-hint {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-muted);
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
