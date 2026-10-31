<script setup>
/**
 * 订单管理：筛选 → 列表 / 看板 → 详情抽屉（状态流转、打印小票、删除）
 * 状态流转由后端校验（PUT /orders/status）：取消时回补库存；完成时按实付金额发放积分。
 * 操作按钮按当前员工权限显示（order:order:update / order:order:delete）。
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Download, List, Grid } from '@element-plus/icons-vue'
import {
  ORDER_STATUS,
  ORDER_STATUS_FLOW,
  ORDER_PROGRESS,
  getLabelByValue,
  parseOrderItems,
  formatOrderItems,
} from '@/utils/constants'
import { queryPageApi, queryByIdApi, deleteByIdApi, updateStatusApi } from '@/api/modules/order'
import { exportCSV } from '@/composables/useExport'
import { usePermission } from '@/composables/usePermission'
import { formatPrice, formatDateTime, formatRelative } from '@/utils/format'
import { BRAND } from '@/config/brand'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import SkeletonTable from '@/components/SkeletonTable.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import BrandLogo from '@/components/brand/BrandLogo.vue'
import SwIcon from '@/components/brand/SwIcon.vue'

const route = useRoute()
const { can } = usePermission()
const canUpdate = can('order:order:update')
const canDelete = can('order:order:delete')

/** 流转到某状态时按钮上的动作文案 */
const ACTION_LABELS = { 1: '转为待接单', 2: '接单制作', 3: '开始配送', 4: '确认完成', 5: '取消订单' }

const initialStatus = route.query.status !== undefined && route.query.status !== '' ? Number(route.query.status) : ''

// 搜索
const searchForm = ref({
  orderNo: typeof route.query.orderNo === 'string' ? route.query.orderNo : '',
  customerName: '',
  status: Number.isInteger(initialStatus) ? initialStatus : '',
  date: [],
  begin: '',
  end: '',
})

const orderList = ref([])
const loading = ref(false)
const loadFailed = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const drawerVisible = ref(false)
const currentOrder = ref(null)
const viewMode = ref('table')

const kanbanColumns = computed(() =>
  ORDER_STATUS.filter((s) => s.value !== 0 || orderList.value.some((o) => o.status === 0)).map((status) => ({
    ...status,
    orders: orderList.value.filter((o) => o.status === status.value),
  }))
)

const hasFilter = computed(() => {
  const f = searchForm.value
  return !!(f.orderNo || f.customerName || f.status !== '' || f.begin)
})

const search = async () => {
  loading.value = true
  loadFailed.value = false
  try {
    const f = searchForm.value
    const result = await queryPageApi(f.orderNo.trim(), f.customerName.trim(), f.status, f.begin, f.end, currentPage.value, pageSize.value)
    if (result.code) {
      orderList.value = result.data?.rows || []
      total.value = result.data?.total || 0
    } else {
      loadFailed.value = true
      ElMessage.error(result.msg || '订单加载失败')
    }
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

const runSearch = () => {
  currentPage.value = 1
  search()
}

const pickStatus = (value) => {
  searchForm.value.status = value
  runSearch()
}

const clear = () => {
  searchForm.value = { orderNo: '', customerName: '', status: '', date: [], begin: '', end: '' }
  runSearch()
}

watch(() => searchForm.value.date, (val) => {
  const valid = val && val.length === 2
  searchForm.value.begin = valid ? val[0] : ''
  searchForm.value.end = valid ? val[1] : ''
}, { deep: true })

const handleSizeChange = () => runSearch()
const handleCurrentChange = () => search()

const viewDetail = async (order) => {
  currentOrder.value = order
  drawerVisible.value = true
  const result = await queryByIdApi(order.id).catch(() => null)
  if (result?.code && result.data && currentOrder.value?.id === order.id) {
    currentOrder.value = result.data
  }
}

const deleteOrder = async (order) => {
  try {
    await ElMessageBox.confirm(`删除后无法恢复。确定删除订单 ${order.orderNo} 吗？`, '删除订单', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    const result = await deleteByIdApi(order.id)
    if (result.code) {
      ElMessage.success('订单已删除')
      if (currentOrder.value?.id === order.id) drawerVisible.value = false
      if (orderList.value.length === 1 && currentPage.value > 1) currentPage.value--
      search()
    } else {
      ElMessage.error(result.msg || '删除失败')
    }
  } catch {
    // 拦截器已提示
  }
}

const handleExport = () => {
  const columns = [
    { key: 'orderNo', label: '订单号' },
    { key: 'customerName', label: '客户' },
    { key: 'phone', label: '电话' },
    { key: 'itemsText', label: '商品明细' },
    { key: 'amount', label: '金额' },
    { key: 'status', label: '状态' },
    { key: 'createTime', label: '下单时间' },
  ]
  const rows = orderList.value.map((o) => ({
    ...o,
    itemsText: formatOrderItems(o.items),
    status: getLabelByValue(ORDER_STATUS, o.status),
  }))
  exportCSV(rows, columns, '喜甜订单')
}

// ---------------- 状态流转 ----------------
const statusUpdating = ref(false)

/** 下一步（非取消）与取消，分开展示：主操作醒目、取消弱化 */
const nextActions = (status) => (ORDER_STATUS_FLOW[status] || []).filter((v) => v !== 5)
const canCancel = (status) => (ORDER_STATUS_FLOW[status] || []).includes(5)
const canRemove = (order) => order.status === 4 || order.status === 5

const confirmHint = (newStatus) => {
  if (newStatus === 5) return '取消后，顾客下单时扣减的库存会自动回补；若已发放积分会一并扣回。'
  if (newStatus === 4) return '完成后将按实付金额为顾客发放会员积分。'
  return ''
}

const changeStatus = async (order, newStatus) => {
  statusUpdating.value = true
  try {
    const result = await updateStatusApi(order.id, newStatus)
    if (result.code) {
      ElMessage.success(`订单 ${order.orderNo.slice(-6)} 已${newStatus === 5 ? '取消' : `更新为「${getLabelByValue(ORDER_STATUS, newStatus)}」`}`)
      if (currentOrder.value?.id === order.id) currentOrder.value = { ...currentOrder.value, status: newStatus }
      search()
      return true
    }
    ElMessage.error(result.msg || '操作失败')
  } catch {
    // 拦截器已展示后端原因（如流转不合法、状态已被他人修改）
  } finally {
    statusUpdating.value = false
  }
  return false
}

const updateOrderStatus = async (order, newStatus) => {
  const hint = confirmHint(newStatus) || `确认后订单状态将更新为「${getLabelByValue(ORDER_STATUS, newStatus)}」。`
  try {
    await ElMessageBox.confirm(
      `${order.customerName} · ${formatPrice(order.amount)}。${hint}`,
      `${ACTION_LABELS[newStatus]} · 订单 ${order.orderNo.slice(-6)}`,
      {
        confirmButtonText: newStatus === 5 ? '确认取消订单' : '确认',
        cancelButtonText: '返回',
        type: newStatus === 5 ? 'warning' : 'info',
      }
    )
  } catch {
    return
  }
  await changeStatus(order, newStatus)
}

// ---------------- 进度 ----------------
const progressSteps = ORDER_PROGRESS.map((v) => ORDER_STATUS.find((s) => s.value === v))
const stepState = (order, value) => {
  if (!order || order.status == null || order.status === 5 || order.status === 0) return ''
  if (value < order.status) return 'is-done'
  if (value === order.status) return order.status === 4 ? 'is-done' : 'is-current'
  return ''
}

// ---------------- 打印小票 ----------------
const printOrder = ref(null)
const printTicket = async () => {
  printOrder.value = currentOrder.value
  document.body.classList.add('sw-printing')
  await new Promise((r) => requestAnimationFrame(() => requestAnimationFrame(r)))
  window.print()
  document.body.classList.remove('sw-printing')
}

const orderLines = computed(() => parseOrderItems(currentOrder.value?.items))
const printLines = computed(() => parseOrderItems(printOrder.value?.items))

onMounted(() => {
  search()
  // 从概览「订单队列」带订单号进来时，直接打开该订单
  if (searchForm.value.orderNo) {
    watch(orderList, (list) => {
      const hit = list.find((o) => o.orderNo === searchForm.value.orderNo)
      if (hit) viewDetail(hit)
    }, { once: true })
  }
})
</script>

<template>
  <div class="page-container">
    <PageHeader
      title="订单管理"
      description="接单、制作、配送与完成；取消订单会自动回补库存"
    >
      <template #actions>
        <el-button
          :disabled="!orderList.length"
          @click="handleExport"
        >
          <el-icon><Download /></el-icon>
          导出本页
        </el-button>
      </template>
    </PageHeader>

    <!-- 筛选 -->
    <section class="filters surface">
      <el-form
        :inline="true"
        :model="searchForm"
        class="filters__form"
        @submit.prevent="runSearch"
      >
        <el-form-item label="订单号">
          <el-input
            v-model="searchForm.orderNo"
            placeholder="支持部分匹配"
            clearable
            style="width: 190px"
            @keyup.enter="runSearch"
          />
        </el-form-item>
        <el-form-item label="客户">
          <el-input
            v-model="searchForm.customerName"
            placeholder="客户姓名"
            clearable
            style="width: 150px"
            @keyup.enter="runSearch"
          />
        </el-form-item>
        <el-form-item label="下单日期">
          <el-date-picker
            v-model="searchForm.date"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 250px"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
          >
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button
            :disabled="!hasFilter"
            @click="clear"
          >清空</el-button>
        </el-form-item>
      </el-form>
    </section>

    <!-- 状态 + 视图 -->
    <div class="toolbar">
      <div
        class="status-tabs"
        role="tablist"
        aria-label="按状态筛选"
      >
        <button
          v-for="s in [{ label: '全部', value: '' }, ...ORDER_STATUS]"
          :key="s.value"
          type="button"
          role="tab"
          class="status-tab"
          :class="{ 'is-active': searchForm.status === s.value }"
          :aria-selected="searchForm.status === s.value"
          @click="pickStatus(s.value)"
        >
          {{ s.label }}
        </button>
      </div>
      <div class="toolbar__right">
        <span class="toolbar__count">共 {{ total }} 笔</span>
        <el-radio-group
          v-model="viewMode"
          size="small"
          aria-label="视图"
        >
          <el-radio-button value="table">
            <el-icon :size="14"><List /></el-icon>
            <span class="view-mode-label">列表</span>
          </el-radio-button>
          <el-radio-button value="kanban">
            <el-icon :size="14"><Grid /></el-icon>
            <span class="view-mode-label">看板</span>
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 看板 -->
    <div
      v-if="viewMode === 'kanban'"
      class="kanban"
    >
      <p class="kanban__note">看板显示当前页的 {{ orderList.length }} 笔订单</p>
      <div class="kanban__cols">
        <section
          v-for="col in kanbanColumns"
          :key="col.value"
          class="kanban-col"
        >
          <header class="kanban-col__head">
            <StatusBadge
              :status="col.value"
              size="sm"
            />
            <span class="kanban-col__count sw-num">{{ col.orders.length }}</span>
          </header>
          <div class="kanban-col__body">
            <article
              v-for="order in col.orders"
              :key="order.id"
              class="kcard"
              tabindex="0"
              @click="viewDetail(order)"
              @keydown.enter="viewDetail(order)"
            >
              <div class="kcard__top">
                <span class="kcard__no sw-num">#{{ order.orderNo?.slice(-6) }}</span>
                <span class="kcard__amount sw-num">{{ formatPrice(order.amount) }}</span>
              </div>
              <p class="kcard__customer">{{ order.customerName }}</p>
              <p class="kcard__items">{{ formatOrderItems(order.items) }}</p>
              <p class="kcard__time">{{ formatRelative(order.createTime) }}</p>
              <div
                v-if="canUpdate && (nextActions(order.status).length || canCancel(order.status))"
                class="kcard__actions"
                @click.stop
              >
                <el-button
                  v-for="v in nextActions(order.status)"
                  :key="v"
                  type="primary"
                  size="small"
                  :disabled="statusUpdating"
                  @click="updateOrderStatus(order, v)"
                >
                  {{ ACTION_LABELS[v] }}
                </el-button>
                <el-button
                  v-if="canCancel(order.status)"
                  size="small"
                  text
                  :disabled="statusUpdating"
                  @click="updateOrderStatus(order, 5)"
                >
                  取消
                </el-button>
              </div>
            </article>
            <p
              v-if="!col.orders.length"
              class="kanban-col__empty"
            >暂无</p>
          </div>
        </section>
      </div>
    </div>

    <!-- 列表 -->
    <section
      v-else
      class="table-card surface"
    >
      <SkeletonTable
        v-if="loading"
        :rows="8"
        :columns="6"
      />
      <template v-else>
        <el-table
          v-if="orderList.length"
          :data="orderList"
          style="width: 100%"
          @row-dblclick="viewDetail"
        >
          <el-table-column
            label="订单号"
            min-width="190"
          >
            <template #default="{ row }">
              <button
                type="button"
                class="order-no"
                @click="viewDetail(row)"
              >{{ row.orderNo }}</button>
            </template>
          </el-table-column>
          <el-table-column
            label="客户"
            min-width="120"
          >
            <template #default="{ row }">
              <span class="cell-strong">{{ row.customerName }}</span>
              <span class="cell-sub sw-num">{{ row.phone }}</span>
            </template>
          </el-table-column>
          <el-table-column
            label="商品"
            min-width="200"
            show-overflow-tooltip
          >
            <template #default="{ row }">{{ formatOrderItems(row.items) }}</template>
          </el-table-column>
          <el-table-column
            label="金额"
            width="110"
            align="right"
          >
            <template #default="{ row }"><span class="cell-strong sw-num">{{ formatPrice(row.amount) }}</span></template>
          </el-table-column>
          <el-table-column
            label="状态"
            width="110"
          >
            <template #default="{ row }"><StatusBadge
              :status="row.status"
              size="sm"
            /></template>
          </el-table-column>
          <el-table-column
            label="下单时间"
            width="160"
          >
            <template #default="{ row }">
              <span
                class="cell-sub-only sw-num"
                :title="formatDateTime(row.createTime)"
              >{{ formatDateTime(row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            :width="canUpdate ? 190 : 90"
            align="right"
            fixed="right"
          >
            <template #default="{ row }">
              <div class="row-actions">
                <el-button
                  v-for="v in canUpdate ? nextActions(row.status) : []"
                  :key="v"
                  type="primary"
                  size="small"
                  :disabled="statusUpdating"
                  @click="updateOrderStatus(row, v)"
                >
                  {{ ACTION_LABELS[v] }}
                </el-button>
                <el-button
                  size="small"
                  @click="viewDetail(row)"
                >详情</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <EmptyState
          v-else-if="loadFailed"
          title="订单加载失败"
          description="请检查网络后重试"
        >
          <template #action><el-button @click="search">重新加载</el-button></template>
        </EmptyState>
        <EmptyState
          v-else-if="hasFilter"
          title="没有符合条件的订单"
          description="换个筛选条件试试"
        >
          <template #action><el-button @click="clear">清空筛选</el-button></template>
        </EmptyState>
        <EmptyState
          v-else
          title="还没有订单"
          description="顾客在商城下单后，订单会出现在这里"
        />
      </template>

      <div
        v-if="total > 0"
        class="pagination-wrapper"
      >
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </section>

    <!-- 详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      size="min(520px, 100vw)"
      :with-header="false"
    >
      <div
        v-if="currentOrder"
        class="od"
      >
        <header class="od__head">
          <div>
            <p class="od__label">订单编号</p>
            <h2 class="od__no sw-num">{{ currentOrder.orderNo }}</h2>
          </div>
          <button
            type="button"
            class="hdr-btn"
            aria-label="关闭"
            @click="drawerVisible = false"
          >
            <SwIcon
              name="close"
              :size="18"
            />
          </button>
        </header>

        <div class="od__status">
          <StatusBadge :status="currentOrder.status" />
          <span class="od__time">下单于 {{ formatDateTime(currentOrder.createTime) }}</span>
        </div>

        <ol
          v-if="currentOrder.status !== 5 && currentOrder.status !== 0"
          class="track"
          aria-label="订单进度"
        >
          <li
            v-for="s in progressSteps"
            :key="s.value"
            :class="stepState(currentOrder, s.value)"
          >
            <span
              class="track__dot"
              aria-hidden="true"
            ></span>
            <span class="track__label">{{ s.label }}</span>
          </li>
        </ol>
        <p
          v-else-if="currentOrder.status === 5"
          class="od__cancelled"
        >订单已取消，相关库存已回补。</p>

        <div
          v-if="canUpdate && (nextActions(currentOrder.status).length || canCancel(currentOrder.status))"
          class="od__actions"
        >
          <el-button
            v-for="v in nextActions(currentOrder.status)"
            :key="v"
            type="primary"
            :loading="statusUpdating"
            @click="updateOrderStatus(currentOrder, v)"
          >
            {{ ACTION_LABELS[v] }}
          </el-button>
          <el-button
            v-if="canCancel(currentOrder.status)"
            :disabled="statusUpdating"
            @click="updateOrderStatus(currentOrder, 5)"
          >
            取消订单
          </el-button>
        </div>

        <section class="od__section">
          <h3 class="od__title">商品明细</h3>
          <ul class="od__lines">
            <li
              v-for="(line, idx) in orderLines"
              :key="idx"
            >
              <span class="od__line-name">{{ line.name }}</span>
              <span class="od__line-qty sw-num">{{ line.qty ? `× ${line.qty}` : '' }}</span>
              <span class="od__line-price sw-num">{{ line.price != null ? formatPrice(line.price * (line.qty || 1)) : '' }}</span>
            </li>
            <li
              v-if="!orderLines.length"
              class="od__line-empty"
            >无明细</li>
          </ul>
          <div class="od__total">
            <span>订单金额</span>
            <strong class="sw-num">{{ formatPrice(currentOrder.amount) }}</strong>
          </div>
        </section>

        <section class="od__section">
          <h3 class="od__title">收货信息</h3>
          <dl class="od__info">
            <div><dt>收货人</dt><dd>{{ currentOrder.customerName }}</dd></div>
            <div><dt>联系电话</dt><dd class="sw-num">{{ currentOrder.phone }}</dd></div>
            <div><dt>配送地址</dt><dd>{{ currentOrder.address || '—' }}</dd></div>
          </dl>
        </section>

        <footer class="od__foot">
          <el-button @click="printTicket">
            <SwIcon
              name="print"
              :size="16"
            />
            <span style="margin-left: 6px">打印小票</span>
          </el-button>
          <el-tooltip
            v-if="canDelete"
            :disabled="canRemove(currentOrder)"
            content="进行中的订单请先取消再删除（取消会回补库存）"
            placement="top"
          >
            <span>
              <el-button
                type="danger"
                plain
                :disabled="!canRemove(currentOrder)"
                @click="deleteOrder(currentOrder)"
              >删除订单</el-button>
            </span>
          </el-tooltip>
        </footer>
      </div>
    </el-drawer>

    <!-- 打印用小票（仅打印时可见） -->
    <Teleport to="body">
      <div
        v-if="printOrder"
        class="sw-print-root"
        aria-hidden="true"
      >
        <div class="ticket">
          <div class="ticket__brand">
            <BrandLogo
              variant="stacked"
              tone="mono"
              :size="40"
            />
            <p class="ticket__tag">{{ BRAND.tagline }}</p>
          </div>
          <p class="ticket__row"><span>订单编号</span><span>{{ printOrder.orderNo }}</span></p>
          <p class="ticket__row"><span>下单时间</span><span>{{ formatDateTime(printOrder.createTime) }}</span></p>
          <p class="ticket__row"><span>订单状态</span><span>{{ getLabelByValue(ORDER_STATUS, printOrder.status) }}</span></p>
          <hr>
          <p
            v-for="(line, idx) in printLines"
            :key="idx"
            class="ticket__row"
          >
            <span>{{ line.name }}{{ line.qty ? ` × ${line.qty}` : '' }}</span>
            <span>{{ line.price != null ? formatPrice(line.price * (line.qty || 1)) : '' }}</span>
          </p>
          <hr>
          <p class="ticket__row ticket__total"><span>合计</span><span>{{ formatPrice(printOrder.amount) }}</span></p>
          <hr>
          <p class="ticket__row"><span>收货人</span><span>{{ printOrder.customerName }}</span></p>
          <p class="ticket__row"><span>电话</span><span>{{ printOrder.phone }}</span></p>
          <p class="ticket__addr">{{ printOrder.address }}</p>
          <p class="ticket__thanks">谢谢光临{{ BRAND.nameCn }}，{{ BRAND.taglineCn }}</p>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 1400px;
  margin: 0 auto;
}

.filters {
  padding: 16px 20px 0;
  margin-bottom: 16px;
}

.filters__form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.status-tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.status-tab {
  height: 34px;
  padding: 0 14px;
  border: 1px solid var(--sw-border);
  border-radius: var(--sw-r-pill);
  background: var(--sw-surface);
  color: var(--sw-ink-2);
  font-size: 13px;
  cursor: pointer;
  transition: background-color var(--sw-dur-1) var(--sw-ease), color var(--sw-dur-1) var(--sw-ease);
}

.status-tab:hover {
  color: var(--sw-ink);
  border-color: var(--el-border-color-hover);
}

.status-tab.is-active {
  background: var(--sw-primary);
  border-color: var(--sw-primary);
  color: var(--sw-on-primary);
  font-weight: 600;
}

.toolbar__right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.toolbar__count {
  font-size: 13px;
  color: var(--sw-ink-3);
}

.view-mode-label {
  margin-left: 4px;
}

.table-card {
  padding: 4px 4px 0;
  overflow: hidden;
}

.order-no {
  padding: 0;
  border: 0;
  background: none;
  font-family: var(--sw-font-mono);
  font-size: 13px;
  font-weight: 600;
  color: var(--sw-ink);
  cursor: pointer;
}

.order-no:hover {
  color: var(--sw-accent);
  text-decoration: underline;
  text-underline-offset: 3px;
}

.cell-strong {
  display: block;
  font-weight: 600;
  color: var(--sw-ink);
}

.cell-sub {
  display: block;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.cell-sub-only {
  font-size: 13px;
  color: var(--sw-ink-2);
}

.row-actions {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px;
  border-top: 1px solid var(--sw-border-soft);
}

/* ---------------- 看板 ---------------- */
.kanban__note {
  margin-bottom: 10px;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.kanban__cols {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(220px, 1fr);
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.kanban-col {
  display: flex;
  flex-direction: column;
  min-height: 50vh;
  max-height: calc(100vh - 300px);
  border-radius: var(--sw-r-lg);
  background: var(--sw-fill-soft);
  border: 1px solid var(--sw-border-soft);
}

.kanban-col__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 12px 8px;
}

.kanban-col__count {
  font-size: 12px;
  font-weight: 600;
  color: var(--sw-ink-3);
}

.kanban-col__body {
  flex: 1;
  overflow-y: auto;
  padding: 4px 10px 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kanban-col__empty {
  padding: 24px 0;
  text-align: center;
  font-size: 12px;
  color: var(--sw-ink-4);
}

.kcard {
  padding: 12px;
  border: 1px solid var(--sw-border-soft);
  border-radius: var(--sw-r-md);
  background: var(--sw-surface);
  cursor: pointer;
  transition: border-color var(--sw-dur-1) var(--sw-ease), box-shadow var(--sw-dur-1) var(--sw-ease);
}

.kcard:hover,
.kcard:focus-visible {
  border-color: var(--sw-border);
  box-shadow: var(--sw-shadow-sm);
}

.kcard__top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.kcard__no {
  font-family: var(--sw-font-mono);
  font-size: 12px;
  color: var(--sw-ink-3);
}

.kcard__amount {
  font-size: 14px;
  font-weight: 700;
}

.kcard__customer {
  margin-top: 4px;
  font-size: 13px;
  font-weight: 600;
}

.kcard__items {
  font-size: 12px;
  color: var(--sw-ink-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kcard__time {
  margin-top: 4px;
  font-size: 11px;
  color: var(--sw-ink-4);
}

.kcard__actions {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--sw-border);
}

.kcard__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

/* ---------------- 详情抽屉 ---------------- */
.od {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.od__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.od__label {
  font-size: 12px;
  color: var(--sw-ink-3);
}

.od__no {
  margin-top: 2px;
  font-family: var(--sw-font-mono);
  font-size: 18px;
  font-weight: 600;
  word-break: break-all;
}

.od__status {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
}

.od__time {
  font-size: 13px;
  color: var(--sw-ink-3);
}

.od__cancelled {
  margin-top: 16px;
  padding: 10px 12px;
  border-radius: var(--sw-r-md);
  background: var(--sw-fill-soft);
  font-size: 13px;
  color: var(--sw-ink-2);
}

.track {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  list-style: none;
  margin-top: 22px;
}

.track li {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--sw-ink-4);
}

.track li + li::before {
  content: '';
  position: absolute;
  top: 5px;
  right: 50%;
  width: 100%;
  height: 2px;
  background: var(--sw-border);
}

.track li.is-done + li::before {
  background: var(--sw-blush);
}

.track__dot {
  position: relative;
  z-index: 1;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid var(--sw-border);
  background: var(--sw-surface);
}

.track .is-done {
  color: var(--sw-ink-2);
}

.track .is-done .track__dot {
  border-color: var(--sw-blush);
  background: var(--sw-blush);
}

.track .is-current {
  color: var(--sw-ink);
  font-weight: 600;
}

.track .is-current .track__dot {
  border-color: var(--sw-cherry);
  background: var(--sw-cherry);
  box-shadow: 0 0 0 4px var(--sw-blush-soft);
}

.od__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 22px;
}

.od__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.od__section {
  margin-top: 28px;
}

.od__title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
}

.od__lines {
  list-style: none;
  border-top: 1px solid var(--sw-border-soft);
}

.od__lines li {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--sw-border-soft);
  font-size: 14px;
}

.od__line-name {
  flex: 1;
}

.od__line-qty {
  color: var(--sw-ink-3);
}

.od__line-price {
  min-width: 72px;
  text-align: right;
}

.od__line-empty {
  color: var(--sw-ink-3);
}

.od__total {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding-top: 12px;
  font-size: 14px;
  color: var(--sw-ink-2);
}

.od__total strong {
  font-size: 22px;
  color: var(--sw-ink);
}

.od__info div {
  display: flex;
  gap: 16px;
  padding: 8px 0;
  font-size: 14px;
}

.od__info dt {
  width: 72px;
  flex-shrink: 0;
  color: var(--sw-ink-3);
}

.od__info dd {
  color: var(--sw-ink);
  word-break: break-all;
}

.od__foot {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  padding-top: 24px;
}

@media (max-width: 640px) {
  .filters__form :deep(.el-form-item) {
    width: 100%;
    margin-right: 0;
  }

  .filters__form :deep(.el-input),
  .filters__form :deep(.el-date-editor) {
    width: 100% !important;
  }
}

/* ---------------- 打印小票（58/80mm 热敏纸或 A4 均可） ---------------- */
.sw-print-root {
  display: none;
}

@media print {
  :global(body.sw-printing .sw-print-root) {
    display: block !important;
  }
}

.ticket {
  width: 72mm;
  margin: 0 auto;
  padding: 4mm 0;
  color: #000;
  font-size: 11pt;
  line-height: 1.5;
  font-family: var(--sw-font-sans);
}

.ticket__brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
  color: #000;
}

.ticket__tag {
  font-size: 8pt;
  letter-spacing: 0.2em;
}

.ticket__row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.ticket__total {
  font-size: 13pt;
  font-weight: 700;
}

.ticket__addr {
  margin-top: 2px;
  word-break: break-all;
}

.ticket hr {
  margin: 6px 0;
  border: 0;
  border-top: 1px dashed #000;
}

.ticket__thanks {
  margin-top: 10px;
  text-align: center;
  font-size: 9pt;
}
</style>
