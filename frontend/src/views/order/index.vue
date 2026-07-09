<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, View, Delete, Download, List, Grid } from '@element-plus/icons-vue'
import { ORDER_STATUS, getLabelByValue, getTypeByValue } from '@/utils/constants'
import { queryPageApi, queryByIdApi, deleteByIdApi, updateApi } from '@/api/modules/order'
import { exportCSV } from '@/composables/useExport'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import SkeletonTable from '@/components/SkeletonTable.vue'

// 搜索
const searchForm = ref({
  orderNo: '',
  customerName: '',
  status: '',
  date: [],
  begin: '',
  end: ''
})

// 订单列表
const orderList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 详情抽屉
const drawerVisible = ref(false)
const currentOrder = ref(null)

// 视图模式：'table' | 'kanban'
const viewMode = ref('table')

// Kanban 列定义
const kanbanColumns = computed(() => {
  return ORDER_STATUS.map(status => ({
    ...status,
    orders: orderList.value.filter(o => o.status === status.value),
  }))
})

onMounted(() => {
  search()
})

// 查询
const search = async () => {
  loading.value = true
  try {
    const result = await queryPageApi(
      searchForm.value.orderNo,
      searchForm.value.customerName,
      searchForm.value.status,
      searchForm.value.begin,
      searchForm.value.end,
      currentPage.value,
      pageSize.value
    )
    if (result.code) {
      orderList.value = result.data.rows
      total.value = result.data.total
    }
  } finally {
    loading.value = false
  }
}

// 清空搜索
const clear = () => {
  searchForm.value = { orderNo: '', customerName: '', status: '', date: [], begin: '', end: '' }
  currentPage.value = 1
  search()
}

// 侦听日期变化
watch(() => searchForm.value.date, (newVal) => {
  if (newVal && newVal.length === 2) {
    searchForm.value.begin = newVal[0]
    searchForm.value.end = newVal[1]
  } else {
    searchForm.value.begin = ''
    searchForm.value.end = ''
  }
}, { deep: true })

// 分页
const handleSizeChange = () => {
  currentPage.value = 1
  search()
}
const handleCurrentChange = () => {
  search()
}

// 查看详情
const viewDetail = async (order) => {
  const result = await queryByIdApi(order.id)
  if (result.code) {
    currentOrder.value = result.data
  } else {
    currentOrder.value = order
  }
  drawerVisible.value = true
}

// 删除订单
const deleteOrder = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该订单吗？', '确认删除', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const result = await deleteByIdApi(id)
    if (result.code) {
      ElMessage.success('删除成功')
      search()
    } else {
      ElMessage.error(result.msg)
    }
  } catch {
    // 取消
  }
}

const handleExport = () => {
  const columns = [
    { key: 'orderNo', label: '订单号' },
    { key: 'customerName', label: '客户' },
    { key: 'items', label: '商品明细' },
    { key: 'amount', label: '金额' },
    { key: 'status', label: '状态' },
    { key: 'createTime', label: '下单时间' }
  ]
  exportCSV(orderList.value, columns, '订单数据')
}

// 状态流转映射：每个状态可以流转到的目标状态
const statusFlow = {
  0: [1, 5],       // 待支付 → 待接单、已取消
  1: [2, 5],       // 待接单 → 制作中、已取消
  2: [3],          // 制作中 → 配送中
  3: [4],          // 配送中 → 已完成
  4: [],           // 已完成（终态）
  5: []            // 已取消（终态）
}

const getFlowActions = (status) => {
  const targets = statusFlow[status] || []
  return targets.map(val => ORDER_STATUS.find(s => s.value === val)).filter(Boolean)
}

// Kanban 拖拽状态变更（带确认提示）
const kanbanStatusChange = async (order, newStatus) => {
  const targets = statusFlow[order.status] || []
  if (!targets.includes(newStatus) && order.status !== newStatus) {
    ElMessage.warning('不允许此状态变更')
    return
  }
  if (order.status === newStatus) return

  const statusLabel = ORDER_STATUS.find(s => s.value === newStatus)?.label || newStatus
  try {
    await ElMessageBox.confirm(
      `将订单 #${order.orderNo?.slice(-6)} 从「${getLabelByValue(ORDER_STATUS, order.status)}」移至「${statusLabel}」？`,
      '看板流转',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' }
    )
    const result = await updateApi({ ...order, status: newStatus })
    if (result.code) {
      ElMessage.success(`已移至「${statusLabel}」`)
      search()
    } else {
      ElMessage.error(result.msg || '操作失败')
    }
  } catch {
    // 取消
  }
}

const updateOrderStatus = async (order, newStatus) => {
  const statusLabel = ORDER_STATUS.find(s => s.value === newStatus)?.label || newStatus
  try {
    await ElMessageBox.confirm(`确认将订单状态变更为「${statusLabel}」吗？`, '状态变更', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'info'
    })
    const result = await updateApi({ ...order, status: newStatus })
    if (result.code) {
      ElMessage.success(`订单已变更为「${statusLabel}」`)
      currentOrder.value.status = newStatus
      search()
    } else {
      ElMessage.error(result.msg || '操作失败')
    }
  } catch {
    // 取消
  }
}
</script>

<template>
  <div class="page-container">
    <PageHeader title="订单管理" description="查看和管理甜品订单，跟踪订单状态">
      <template #actions>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </template>
    </PageHeader>

    <!-- 搜索栏 -->
    <div class="search-card glass-panel animate-fade-in-up delay-1">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="客户">
          <el-input v-model="searchForm.customerName" placeholder="请输入客户姓名" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option v-for="s in ORDER_STATUS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="clear">清空</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 状态标签 + 视图切换 -->
    <div class="status-tabs animate-fade-in-up delay-1">
      <div class="status-tabs__left">
        <el-check-tag
          v-for="s in [{ label: '全部', value: '' }, ...ORDER_STATUS]"
          :key="s.value"
          :checked="searchForm.status === s.value"
          @change="searchForm.status = s.value; currentPage = 1; search()"
        >
          {{ s.label }}
        </el-check-tag>
      </div>
      <div class="status-tabs__right">
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="table">
            <el-icon :size="14"><List /></el-icon>
            <span class="view-mode-label">表格</span>
          </el-radio-button>
          <el-radio-button value="kanban">
            <el-icon :size="14"><Grid /></el-icon>
            <span class="view-mode-label">看板</span>
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- ========== Kanban 看板视图 ========== -->
    <div v-if="viewMode === 'kanban'" class="kanban-container animate-fade-in-up delay-2">
      <div
        v-for="col in kanbanColumns"
        :key="col.value"
        class="kanban-column"
        :class="`kanban-column--${col.value}`"
      >
        <div class="kanban-column__header">
          <div class="kanban-column__title-row">
            <span class="kanban-column__dot" :class="`dot--${col.value}`"></span>
            <span class="kanban-column__title">{{ col.label }}</span>
          </div>
          <span class="kanban-column__count">{{ col.orders.length }}</span>
        </div>

        <div class="kanban-column__body">
          <div
            v-for="order in col.orders"
            :key="order.id"
            class="kanban-card glass-panel"
            draggable="true"
            @click="viewDetail(order)"
          >
            <div class="kanban-card__top">
              <span class="kanban-card__no">#{{ order.orderNo?.slice(-6) }}</span>
              <span class="kanban-card__amount">¥{{ (order.amount || 0).toFixed(0) }}</span>
            </div>
            <div class="kanban-card__customer">{{ order.customerName }}</div>
            <div class="kanban-card__items">{{ order.items }}</div>
            <div class="kanban-card__time">{{ order.createTime?.slice(5) || '' }}</div>

            <!-- 快捷流转按钮 -->
            <div class="kanban-card__actions" @click.stop>
              <el-button
                v-for="action in getFlowActions(order.status)"
                :key="action.value"
                :type="action.value === 5 ? 'danger' : 'primary'"
                size="small"
                plain
                class="kanban-action-btn"
                @click="kanbanStatusChange(order, action.value)"
              >
                → {{ action.label }}
              </el-button>
            </div>
          </div>

          <div v-if="col.orders.length === 0" class="kanban-column__empty">
            暂无订单
          </div>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div v-else class="table-card glass-panel animate-fade-in-up delay-2">
      <SkeletonTable v-if="loading" :rows="8" :columns="6" />
      <el-table v-else :data="orderList" style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center">
          <template #default="scope">
            <span class="row-index">{{ (currentPage - 1) * pageSize + scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单号" width="160">
          <template #default="scope">
            <span class="order-no">{{ scope.row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" width="90" align="center" />
        <el-table-column prop="items" label="商品明细" min-width="200" show-overflow-tooltip />
        <el-table-column prop="amount" label="金额" width="100" align="right">
          <template #default="scope">
            <span class="amount">¥{{ (scope.row.amount || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getTypeByValue(ORDER_STATUS, scope.row.status)" size="small" effect="plain">
              {{ getLabelByValue(ORDER_STATUS, scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170" align="center">
          <template #default="scope">
            <span class="time-text">{{ scope.row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button type="primary" size="small" plain @click="viewDetail(scope.row)">
                <el-icon><View /></el-icon>
                详情
              </el-button>
              <el-button type="danger" size="small" plain @click="deleteOrder(scope.row.id)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState
        v-if="!loading && orderList.length === 0"
        title="暂无订单数据"
        description="当客户下单后，订单将显示在这里"
      />

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :background="true"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 订单详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="订单详情" size="480px" v-if="currentOrder">
      <div class="order-detail">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">订单号</span>
              <span class="detail-value">{{ currentOrder.orderNo }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">状态</span>
              <el-tag :type="getTypeByValue(ORDER_STATUS, currentOrder.status)" size="small">
                {{ getLabelByValue(ORDER_STATUS, currentOrder.status) }}
              </el-tag>
            </div>

            <!-- 状态流转操作 -->
            <div class="detail-item full" v-if="getFlowActions(currentOrder.status).length > 0">
              <span class="detail-label">状态变更</span>
              <div class="status-actions">
                <el-button
                  v-for="action in getFlowActions(currentOrder.status)"
                  :key="action.value"
                  :type="action.value === 5 ? 'danger' : 'primary'"
                  size="small"
                  plain
                  @click="updateOrderStatus(currentOrder, action.value)"
                >
                  变更为「{{ action.label }}」
                </el-button>
              </div>
            </div>
            <div class="detail-item">
              <span class="detail-label">下单时间</span>
              <span class="detail-value">{{ currentOrder.createTime }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">订单金额</span>
              <span class="detail-value amount">¥{{ (currentOrder.amount || 0).toFixed(2) }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4>客户信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">客户姓名</span>
              <span class="detail-value">{{ currentOrder.customerName }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">联系电话</span>
              <span class="detail-value">{{ currentOrder.phone }}</span>
            </div>
            <div class="detail-item full">
              <span class="detail-label">配送地址</span>
              <span class="detail-value">{{ currentOrder.address }}</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4>商品明细</h4>
          <p class="detail-items">{{ currentOrder.items }}</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 1400px;
  margin: 0 auto;
}

.search-card {
  padding: 20px 24px;
  margin-bottom: 16px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.status-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.status-tabs :deep(.el-check-tag) {
  cursor: pointer;
  border-radius: var(--radius-sm);
  font-size: 13px;
}

.table-card {
  padding: 4px;
}

.row-index {
  font-size: 13px;
  color: var(--color-text-muted);
}

.order-no {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 13px;
  color: var(--color-primary);
  font-weight: 500;
}

.amount {
  font-weight: 600;
  color: var(--color-primary-dark);
}

.time-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.action-buttons {
  display: flex;
  gap: 6px;
  justify-content: center;
}

.pagination-wrapper {
  padding: 16px 20px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--color-border-light);
}

/* 订单详情 */
.order-detail {
  padding: 0 4px;
}

.detail-section {
  margin-bottom: 28px;
}

.detail-section h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border-light);
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item.full {
  grid-column: 1 / -1;
}

.detail-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.detail-value {
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.detail-items {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.8;
  background: var(--color-bg-primary);
  padding: 12px 16px;
  border-radius: var(--radius-md);
}

.status-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 4px;
}

/* ============================================
   Kanban Board Styles
   ============================================ */
.kanban-container {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  min-height: 60vh;
  overflow-x: auto;
  padding-bottom: 16px;

  @media (max-width: 1400px) {
    grid-template-columns: repeat(3, 1fr);
  }
  @media (max-width: 900px) {
    grid-template-columns: repeat(2, 1fr);
  }
  @media (max-width: 560px) {
    grid-template-columns: 1fr;
  }
}

.kanban-column {
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  min-width: 200px;
  max-height: calc(100vh - 280px);
  overflow: hidden;
}

.kanban-column__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  flex-shrink: 0;
}

.kanban-column__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.kanban-column__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  &.dot--0 { background: #f0a35c; }
  &.dot--1 { background: #6b8cce; }
  &.dot--2 { background: #e8637a; }
  &.dot--3 { background: #a78bfa; }
  &.dot--4 { background: #5cb88a; }
  &.dot--5 { background: #a3949b; }
}

.kanban-column__title {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.kanban-column__count {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-text-muted);
  background: rgba(255, 255, 255, 0.4);
  padding: 2px 8px;
  border-radius: 99px;
  min-width: 24px;
  text-align: center;
}

.kanban-column__body {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.kanban-column__empty {
  text-align: center;
  padding: 32px 16px;
  color: var(--color-text-muted);
  font-size: 13px;
  font-style: italic;
}

.kanban-card {
  padding: 14px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 12px;
  transition: all var(--transition-fast);
  animation: kanbanCardIn 0.3s cubic-bezier(0.16, 1, 0.3, 1) both;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--glass-shadow-hover);
    border-color: rgba(232, 99, 122, 0.2);
    background: rgba(255, 255, 255, 0.85);
  }

  &:active {
    transform: scale(0.98);
  }
}

.kanban-card__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.kanban-card__no {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-primary);
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-variant-numeric: tabular-nums;
}

.kanban-card__amount {
  font-size: 14px;
  font-weight: 800;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}

.kanban-card__customer {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 3px;
}

.kanban-card__items {
  font-size: 11px;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 6px;
}

.kanban-card__time {
  font-size: 10px;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
  margin-bottom: 8px;
}

.kanban-card__actions {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  padding-top: 8px;
  border-top: 1px dashed rgba(45, 35, 39, 0.06);

  .kanban-action-btn {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 6px;
  }
}

@keyframes kanbanCardIn {
  from { opacity: 0; transform: translateY(8px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* Status tabs layout */
.status-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
}

.status-tabs__left {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.status-tabs__right {
  flex-shrink: 0;
}

.view-mode-label {
  margin-left: 4px;
  font-size: 12px;
}

/* Dark mode kanban adjustments（整选择器入 :global，避免描述符被丢弃失效） */
:global([data-theme="dark"] .kanban-column) {
  background: rgba(48, 41, 54, 0.45);
  border-color: rgba(255, 255, 255, 0.06);
}

:global([data-theme="dark"] .kanban-card) {
  background: rgba(48, 41, 54, 0.6);
  border-color: rgba(255, 255, 255, 0.08);
}

:global([data-theme="dark"] .kanban-card:hover) {
  background: rgba(48, 41, 54, 0.82);
  border-color: rgba(240, 140, 158, 0.2);
}

:global([data-theme="dark"] .kanban-column__header) {
  border-bottom-color: rgba(255, 255, 255, 0.06);
}

:global([data-theme="dark"] .kanban-card__actions) {
  border-top-color: rgba(255, 255, 255, 0.06);
}
</style>
