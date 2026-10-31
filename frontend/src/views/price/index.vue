<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, EditPen, Plus, Search } from '@element-plus/icons-vue'
import { DESSERT_CATEGORIES, CATEGORY_COLORS } from '@/utils/constants'
import { queryByCategoryApi, queryCategoryCountApi, updatePriceApi, updateStatusApi } from '@/api/modules/dessert'
import { usePermission } from '@/composables/usePermission'
import { formatPrice } from '@/utils/format'
import ProductImg from '@/components/ProductImg.vue'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'

const router = useRouter()
const { can } = usePermission()
const canUpdate = can('dessert:dessert:update')
const canCreate = can('dessert:dessert:create')

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
  const result = await queryCategoryCountApi().catch(() => null)
  if (result?.code) {
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
      dessertList.value = result.data || []
    }
  } catch {
    dessertList.value = []
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
const editForm = ref({ id: null, name: '', price: undefined, originalPrice: undefined })
const editFormRef = ref()
const saving = ref(false)

const validateOriginalPrice = (rule, value, callback) => {
  if (value == null) return callback()
  if (editForm.value.price != null && value < editForm.value.price) return callback(new Error('原价不应低于现价'))
  callback()
}

const rules = {
  price: [
    { required: true, message: '请输入现价', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '现价必须大于 0', trigger: 'blur' },
  ],
  originalPrice: [{ validator: validateOriginalPrice, trigger: 'blur' }],
}

// 只取调价需要的字段；提交走 PUT /desserts/price 窄接口，不会覆写描述/图片/库存等其它字段
const openEdit = (item) => {
  editForm.value = {
    id: item.id,
    name: item.name,
    price: item.price,
    originalPrice: item.originalPrice ?? undefined,
  }
  dialogVisible.value = true
}

const savePrice = async () => {
  if (!editFormRef.value || saving.value) return
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const { id, price, originalPrice } = editForm.value
    const result = await updatePriceApi(id, price, originalPrice ?? undefined)
    if (result.code) {
      ElMessage.success('价格更新成功')
      dialogVisible.value = false
      loadCategoryData(activeCategory.value)
    } else {
      ElMessage.error(result.msg || '价格更新失败')
    }
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (item) => {
  if (item._toggling) return
  const newStatus = item.status === 1 ? 0 : 1
  item._toggling = true
  try {
    const result = await updateStatusApi(item.id, newStatus)
    if (result.code) {
      item.status = newStatus
      ElMessage.success(newStatus === 1 ? '已上架' : '已下架')
      loadCategoryCounts()
    } else {
      ElMessage.error(result.msg || '操作失败')
    }
  } catch {
    // 拦截器已提示
  } finally {
    item._toggling = false
  }
}

// 颜色映射（全站统一）
const categoryColors = CATEGORY_COLORS
</script>

<template>
  <div class="page-container">
    <PageHeader title="价格管理" description="按分类调整售价与划线价，管理上下架">
      <template v-if="canCreate" #actions>
        <el-button type="primary" @click="router.push({ path: '/dessert', query: { action: 'create' } })">
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
        class="category-tab"
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
        class="dessert-card"
        :class="{ disabled: item.status === 0 }"
      >
        <div class="card-image">
          <ProductImg :item="item" sizes="(min-width: 1400px) 320px, (min-width: 768px) 30vw, 90vw" :alt="item.name" class="dessert-image" />
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
            <span class="sales">销量 {{ item.sales ?? 0 }}</span>
            <span class="sales" :class="{ 'stock-low': (item.stock ?? 0) < 10 }">库存 {{ item.stock ?? 0 }}</span>
          </div>
          <div class="price-row">
            <span class="current-price sw-num">{{ formatPrice(item.price) }}</span>
            <span v-if="item.price < item.originalPrice" class="original-price sw-num">{{ formatPrice(item.originalPrice) }}</span>
          </div>
          <div class="card-actions">
            <el-button v-if="canUpdate" type="primary" size="small" @click="openEdit(item)">
              <el-icon><EditPen /></el-icon>
              调价
            </el-button>
            <el-button
              v-if="canUpdate"
              size="small"
              :loading="item._toggling"
              @click="toggleStatus(item)"
            >
              {{ item.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button
              size="small"
              plain
              @click="router.push(`/dessert/${item.id}`)"
            >
              <el-icon><ChatDotRound /></el-icon>
              详情
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <EmptyState
      v-if="!loading && filteredList.length === 0"
      title="该分类暂无甜品"
      :description="canCreate ? '请切换分类，或去甜品管理上新' : '请切换其他分类看看'"
    />

    <!-- 调价对话框 -->
    <el-dialog v-model="dialogVisible" title="调整价格" width="420" destroy-on-close>
      <el-form :model="editForm" :rules="rules" ref="editFormRef" label-width="80px">
        <el-form-item label="甜品名称">
          <el-input :model-value="editForm.name" disabled />
        </el-form-item>
        <el-form-item label="现价" prop="price">
          <el-input-number v-model="editForm.price" :min="0.01" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原价" prop="originalPrice">
          <el-input-number
            v-model="editForm.originalPrice"
            :min="0.01"
            :precision="2"
            :step="1"
            style="width: 100%"
            placeholder="选填，划线价"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="savePrice">确认修改</el-button>
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
  background: var(--sw-surface);
  border: 1px solid var(--sw-border);
  border-radius: var(--sw-r-pill);
  cursor: pointer;
  transition: border-color var(--transition-fast), background-color var(--transition-fast);
  position: relative;
}

.category-tab:hover {
  border-color: var(--el-border-color-hover);
}

.category-tab.active {
  border-color: var(--sw-primary);
  background: var(--sw-primary);
}

.category-tab.active .cat-name {
  color: var(--sw-on-primary);
  font-weight: 600;
}

.category-tab.active .cat-count {
  background: rgba(255, 254, 252, 0.18);
  color: var(--sw-on-primary);
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
  background: var(--sw-surface);
  border-radius: var(--sw-r-lg);
  overflow: hidden;
  border: 1px solid var(--sw-border-soft);
  transition: box-shadow var(--transition-base), border-color var(--transition-base);
  position: relative;
}

.dessert-card:hover {
  box-shadow: var(--sw-shadow-md);
  border-color: var(--sw-border);
}

.dessert-card.disabled {
  opacity: 0.6;
}

.card-image {
  position: relative;
  height: 168px;
  overflow: hidden;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stock-low {
  color: var(--sw-berry);
  font-weight: 600;
}

.dessert-image {
  width: 100%;
  height: 100%;
  background: var(--color-bg-tertiary);   /* 远程图加载前的占位底色 */
  object-fit: cover;
  transition: transform var(--transition-spring);
}

.dessert-card:hover .dessert-image {
  transform: scale(1.03);
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
  background: rgba(79, 110, 75, 0.92);
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
  display: flex;
  gap: 12px;
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
  color: var(--sw-ink);
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
