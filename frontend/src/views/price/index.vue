<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, Plus, Search } from '@element-plus/icons-vue'
import { DESSERT_CATEGORIES } from '@/utils/constants'
import { queryByCategoryApi, queryCategoryCountApi, updateApi, updateStatusApi } from '@/api/modules/dessert'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'

// 当前分类
const activeCategory = ref('cake')

// 搜索
const searchText = ref('')

// 甜品数据
const dessertList = ref([])
const loading = ref(false)

// 筛选后的列表
const filteredList = computed(() => {
  let list = dessertList.value
  if (searchText.value) {
    list = list.filter(item => item.name.includes(searchText.value))
  }
  return list
})

// 各分类数量
const categoryCounts = ref({})

// 加载分类数量
const loadCategoryCounts = async () => {
  const result = await queryCategoryCountApi()
  if (result.code) {
    const counts = {}
    DESSERT_CATEGORIES.forEach(cat => { counts[cat.value] = 0 })
    result.data.forEach(item => {
      counts[item.category] = item.count
    })
    categoryCounts.value = counts
  }
}

// 加载分类数据
const loadCategoryData = async (category) => {
  loading.value = true
  try {
    const result = await queryByCategoryApi(category)
    if (result.code) {
      dessertList.value = result.data
    }
  } finally {
    loading.value = false
  }
}

// 切换分类
const switchCategory = (category) => {
  activeCategory.value = category
  searchText.value = ''
  loadCategoryData(category)
}

onMounted(() => {
  loadCategoryData(activeCategory.value)
  loadCategoryCounts()
})

// 编辑对话框
const dialogVisible = ref(false)
const editForm = ref({ id: null, name: '', price: 0, originalPrice: 0 })
const editFormRef = ref()

const rules = {
  name: [{ required: true, message: '请输入甜品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

const openEdit = (item) => {
  editForm.value = { ...item }
  dialogVisible.value = true
}

const savePrice = async () => {
  if (!editFormRef.value) return
  editFormRef.value.validate(async (valid) => {
    if (valid) {
      const result = await updateApi(editForm.value)
      if (result.code) {
        ElMessage.success('价格更新成功')
        dialogVisible.value = false
        loadCategoryData(activeCategory.value)
        loadCategoryCounts()
      } else {
        ElMessage.error(result.msg)
      }
    }
  })
}

const toggleStatus = async (item) => {
  const newStatus = item.status === 1 ? 0 : 1
  const result = await updateStatusApi(item.id, newStatus)
  if (result.code) {
    item.status = newStatus
    ElMessage.success(newStatus === 1 ? '已上架' : '已下架')
    loadCategoryCounts()
  } else {
    ElMessage.error(result.msg)
  }
}

// 颜色映射
const categoryColors = {
  cake: '#e8637a',
  bread: '#f0a35c',
  drink: '#6b8cce',
  dessert: '#5cb88a',
  icecream: '#a78bfa'
}
</script>

<template>
  <div class="page-container">
    <PageHeader title="价格管理" description="管理甜品价格和上下架状态">
      <template #actions>
        <el-button type="primary">
          <el-icon><Plus /></el-icon>
          新增甜品
        </el-button>
      </template>
    </PageHeader>

    <!-- 分类标签 -->
    <div class="category-tabs animate-fade-in-up delay-1">
      <div
        v-for="cat in DESSERT_CATEGORIES"
        :key="cat.value"
        class="category-tab glass-panel--hover"
        :class="{ active: activeCategory === cat.value }"
        :style="{ '--cat-color': categoryColors[cat.value] }"
        @click="switchCategory(cat.value)"
      >
        <span class="cat-name">{{ cat.label }}</span>
        <span class="cat-count">{{ categoryCounts[cat.value] || 0 }}</span>
      </div>
    </div>

    <!-- 搜索 -->
    <div class="search-bar animate-fade-in-up delay-1">
      <el-input
        v-model="searchText"
        placeholder="搜索甜品名称..."
        clearable
        style="width: 280px"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <span class="result-count">共 {{ filteredList.length }} 个甜品</span>
    </div>

    <!-- 甜品卡片网格 -->
    <SkeletonCard v-if="loading" :count="6" :columns="4" />
    <div v-else class="dessert-grid animate-fade-in-up delay-2">
      <div
        v-for="item in filteredList"
        :key="item.id"
        class="dessert-card glass-panel--hover"
        :class="{ disabled: item.status === 0 }"
      >
        <div class="card-image">
          <img v-if="item.image" :src="item.image" :alt="item.name" class="dessert-image" />
          <div v-else class="image-placeholder" :style="{ background: `${categoryColors[item.category]}15` }">
            <span style="font-size: 32px">🧁</span>
          </div>
          <div class="card-badge" v-if="item.price < item.originalPrice">
            {{ Math.round((1 - item.price / item.originalPrice) * 100) }}% OFF
          </div>
          <div class="status-indicator" :class="{ active: item.status === 1 }">
            {{ item.status === 1 ? '在售' : '已下架' }}
          </div>
        </div>

        <div class="card-body">
          <h4 class="dessert-name">{{ item.name }}</h4>
          <div class="dessert-meta">
            <span class="sales">月销 {{ item.sales }}</span>
          </div>
          <div class="price-row">
            <span class="current-price">¥{{ item.price.toFixed(2) }}</span>
            <span v-if="item.price < item.originalPrice" class="original-price">¥{{ item.originalPrice.toFixed(2) }}</span>
          </div>
          <div class="card-actions">
            <el-button type="primary" size="small" plain @click="openEdit(item)">
              <el-icon><EditPen /></el-icon>
              调价
            </el-button>
            <el-button
              :type="item.status === 1 ? 'warning' : 'success'"
              size="small"
              plain
              @click="toggleStatus(item)"
            >
              {{ item.status === 1 ? '下架' : '上架' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <EmptyState
      v-if="filteredList.length === 0"
      title="该分类暂无甜品"
      description="请切换分类或新增甜品"
    />

    <!-- 调价对话框 -->
    <el-dialog v-model="dialogVisible" title="调整价格" width="420" destroy-on-close>
      <el-form :model="editForm" :rules="rules" ref="editFormRef" label-width="80px">
        <el-form-item label="甜品名称">
          <el-input :model-value="editForm.name" disabled />
        </el-form-item>
        <el-form-item label="原价">
          <el-input :model-value="`¥${editForm.originalPrice?.toFixed(2)}`" disabled />
        </el-form-item>
        <el-form-item label="现价" prop="price">
          <el-input-number v-model="editForm.price" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="savePrice">确认修改</el-button>
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

/* 分类标签 */
.category-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.category-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 2px solid var(--glass-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
  box-shadow: var(--glass-specular), var(--glass-shadow);
  position: relative;
  overflow: hidden;
}

.category-tab:hover {
  border-color: var(--cat-color);
  transform: translateY(-1px);
}

.category-tab.active {
  border-color: var(--cat-color);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.category-tab.active .cat-name {
  color: var(--cat-color);
  font-weight: 600;
}

.cat-name {
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.cat-count {
  font-size: 12px;
  color: var(--color-text-muted);
  background: var(--color-bg-tertiary);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}

/* 搜索栏 */
.search-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.result-count {
  font-size: 13px;
  color: var(--color-text-muted);
}

/* 卡片网格 */
.dessert-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.dessert-card {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--glass-specular), var(--glass-shadow);
  border: 1px solid var(--glass-border);
  transition: all var(--transition-spring);
  position: relative;
}

.dessert-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--glass-specular), var(--glass-shadow-hover);
  border-color: rgba(232, 99, 122, 0.2);
}

.dessert-card.disabled {
  opacity: 0.6;
}

.card-image {
  position: relative;
  height: 140px;
  overflow: hidden;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dessert-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-spring);
}

.dessert-card:hover .dessert-image {
  transform: scale(1.08);
}

.card-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: var(--color-primary);
  color: white;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: var(--radius-sm);
}

.status-indicator {
  position: absolute;
  top: 10px;
  left: 10px;
  font-size: 11px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  background: rgba(0, 0, 0, 0.5);
  color: white;
}

.status-indicator.active {
  background: rgba(92, 184, 138, 0.9);
}

.card-body {
  padding: 16px;
}

.dessert-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.dessert-meta {
  margin-bottom: 10px;
}

.sales {
  font-size: 12px;
  color: var(--color-text-muted);
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 14px;
}

.current-price {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary-dark);
}

.original-price {
  font-size: 13px;
  color: var(--color-text-muted);
  text-decoration: line-through;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
