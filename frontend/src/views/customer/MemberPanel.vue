<script setup>
/**
 * 会员列表（工作台 · 客户管理 → 会员）
 * 数据来自 GET /members（member:member:list）与 GET /members/:id/points-log（member:points:list）。
 * 会员在顾客首次下单时按手机号自动建立；积分在订单完成时发放、取消时扣回。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { queryMembersApi, queryMemberPointsLogApi } from '@/api/modules/member'
import { usePermission } from '@/composables/usePermission'
import { formatMoney, formatCount, formatDateTime } from '@/utils/format'
import EmptyState from '@/components/EmptyState.vue'
import SkeletonTable from '@/components/SkeletonTable.vue'
import StatusBadge from '@/components/StatusBadge.vue'

const LEVELS = [
  { value: 0, label: '普通会员', next: 1000, tone: 'neutral' },
  { value: 1, label: '银卡会员', next: 3000, tone: 'info' },
  { value: 2, label: '金卡会员', next: 10000, tone: 'warning' },
  { value: 3, label: '钻石会员', next: null, tone: 'brand' },
]
const LOG_TYPES = { 1: '消费获取', 2: '生日赠送', 3: '活动赠送', 4: '积分兑换', 5: '过期扣除', 6: '调整 / 撤回' }

const { can } = usePermission()
const canSeeLog = can('member:points:list')

const filters = reactive({ name: '', phone: '', level: '' })
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const rows = ref([])
const loading = ref(false)
const failed = ref(false)

const levelOf = (lv) => LEVELS[Math.min(Math.max(Number(lv) || 0, 0), 3)]
const growthPercent = (m) => {
  const lv = levelOf(m.level)
  if (!lv.next) return 100
  return Math.min(Math.round(((m.growth || 0) / lv.next) * 100), 100)
}

async function load() {
  loading.value = true
  failed.value = false
  try {
    const res = await queryMembersApi({ ...filters, page: page.value, pageSize: pageSize.value })
    if (res?.code) {
      rows.value = res.data?.rows || []
      total.value = res.data?.total || 0
    } else {
      failed.value = true
    }
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  Object.assign(filters, { name: '', phone: '', level: '' })
  search()
}

const hasFilter = computed(() => !!(filters.name || filters.phone || filters.level !== ''))

// ---- 详情 ----
const drawer = ref(false)
const current = ref(null)
const logs = ref([])
const logLoading = ref(false)

async function openMember(row) {
  current.value = row
  drawer.value = true
  logs.value = []
  if (!canSeeLog) return
  logLoading.value = true
  try {
    const res = await queryMemberPointsLogApi(row.id)
    logs.value = res?.code && Array.isArray(res.data) ? res.data : []
  } catch {
    logs.value = []
  } finally {
    logLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="members">
    <div class="search-bar">
      <el-input
        v-model="filters.name"
        placeholder="会员姓名"
        clearable
        style="width: 160px"
        @keyup.enter="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-input
        v-model="filters.phone"
        placeholder="手机号"
        clearable
        style="width: 160px"
        @keyup.enter="search"
      />
      <el-select
        v-model="filters.level"
        placeholder="全部等级"
        clearable
        style="width: 140px"
        @change="search"
      >
        <el-option
          v-for="lv in LEVELS"
          :key="lv.value"
          :label="lv.label"
          :value="lv.value"
        />
      </el-select>
      <el-button
        type="primary"
        @click="search"
      >查询</el-button>
      <el-button
        :disabled="!hasFilter"
        @click="reset"
      >重置</el-button>
      <span class="result-count">共 {{ total }} 位会员</span>
    </div>

    <SkeletonTable
      v-if="loading"
      :rows="6"
      :columns="7"
    />
    <template v-else>
      <el-table
        v-if="rows.length"
        :data="rows"
        style="width: 100%"
        @row-click="openMember"
      >
        <el-table-column
          label="会员"
          min-width="140"
        >
          <template #default="{ row }">
            <span class="cell-strong">{{ row.name || '未留名' }}</span>
            <span class="cell-sub sw-num">{{ row.phone }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="等级"
          width="120"
        >
          <template #default="{ row }">
            <StatusBadge
              :tone="levelOf(row.level).tone"
              :label="levelOf(row.level).label"
              size="sm"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="可用积分"
          width="110"
          align="right"
        >
          <template #default="{ row }"><span class="cell-strong sw-num">{{ formatCount(row.points) }}</span></template>
        </el-table-column>
        <el-table-column
          label="成长值"
          min-width="170"
        >
          <template #default="{ row }">
            <div class="growth">
              <span class="growth__bar"><span :style="{ width: `${growthPercent(row)}%` }"></span></span>
              <span class="growth__text sw-num">
                {{ formatCount(row.growth) }}{{ levelOf(row.level).next ? ` / ${formatCount(levelOf(row.level).next)}` : '' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="累计消费"
          width="120"
          align="right"
        >
          <template #default="{ row }"><span class="sw-num">{{ formatMoney(row.totalSpent) }}</span></template>
        </el-table-column>
        <el-table-column
          label="订单"
          width="80"
          align="right"
        >
          <template #default="{ row }"><span class="sw-num">{{ formatCount(row.orderCount) }}</span></template>
        </el-table-column>
        <el-table-column
          label="入会时间"
          width="160"
        >
          <template #default="{ row }"><span class="cell-sub-only sw-num">{{ formatDateTime(row.registeredAt) }}</span></template>
        </el-table-column>
      </el-table>
      <EmptyState
        v-else-if="failed"
        title="会员数据加载失败"
        description="请检查网络后重试"
      >
        <template #action><el-button @click="load">重新加载</el-button></template>
      </EmptyState>
      <EmptyState
        v-else-if="hasFilter"
        title="没有符合条件的会员"
        description="换个条件试试"
      />
      <EmptyState
        v-else
        title="还没有会员"
        description="顾客首次在商城下单后会自动成为会员"
      />

      <div
        v-if="total > pageSize"
        class="pagination-wrap"
      >
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="search"
        />
      </div>
    </template>

    <p class="members__note">会员在顾客首次下单时按手机号自动建立；积分在订单完成时按实付金额发放，订单取消会扣回。</p>

    <el-drawer
      v-model="drawer"
      size="min(460px, 100vw)"
      :title="current ? `${current.name || '会员'} · 会员详情` : '会员详情'"
    >
      <template v-if="current">
        <div class="mcard">
          <div class="mcard__top">
            <span class="mcard__name">{{ current.name || '未留名' }}</span>
            <StatusBadge
              :tone="levelOf(current.level).tone"
              :label="levelOf(current.level).label"
              size="sm"
            />
          </div>
          <p class="mcard__phone sw-num">{{ current.phone }}</p>
          <dl class="mcard__stats">
            <div><dt>可用积分</dt><dd class="sw-num">{{ formatCount(current.points) }}</dd></div>
            <div><dt>累计积分</dt><dd class="sw-num">{{ formatCount(current.totalPoints) }}</dd></div>
            <div><dt>累计消费</dt><dd class="sw-num">{{ formatMoney(current.totalSpent) }}</dd></div>
            <div><dt>订单数</dt><dd class="sw-num">{{ formatCount(current.orderCount) }}</dd></div>
          </dl>
          <div class="growth growth--wide">
            <span class="growth__bar"><span :style="{ width: `${growthPercent(current)}%` }"></span></span>
            <span class="growth__text">
              <template v-if="levelOf(current.level).next">
                成长值 {{ formatCount(current.growth) }}，距{{ LEVELS[Math.min(Number(current.level) + 1, 3)].label }}还差
                {{ formatCount(Math.max(levelOf(current.level).next - (current.growth || 0), 0)) }}
              </template>
              <template v-else>已是最高等级</template>
            </span>
          </div>
        </div>

        <h4 class="log-title">积分流水</h4>
        <p
          v-if="!canSeeLog"
          class="log-empty"
        >当前账号没有查看积分流水的权限。</p>
        <p
          v-else-if="logLoading"
          class="log-empty"
        >正在加载…</p>
        <ul
          v-else-if="logs.length"
          class="log"
        >
          <li
            v-for="log in logs"
            :key="log.id"
          >
            <span class="log__main">
              <span class="log__reason">{{ log.reason || LOG_TYPES[log.type] || '积分变动' }}</span>
              <span class="log__meta">{{ LOG_TYPES[log.type] || '其他' }} · {{ formatDateTime(log.createdAt) }}</span>
            </span>
            <span class="log__side">
              <span
                class="log__points sw-num"
                :class="log.points > 0 ? 'is-plus' : 'is-minus'"
              >{{ log.points > 0 ? '+' : '' }}{{ log.points }}</span>
              <span class="log__balance sw-num">余 {{ formatCount(log.balance) }}</span>
            </span>
          </li>
        </ul>
        <p
          v-else
          class="log-empty"
        >暂无积分流水</p>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.search-bar :deep(.el-button + .el-button) {
  margin-left: 0;
}

.result-count {
  margin-left: auto;
  font-size: 13px;
  color: var(--sw-ink-3);
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

.growth {
  display: flex;
  align-items: center;
  gap: 10px;
}

.growth--wide {
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
  margin-top: 16px;
}

.growth__bar {
  flex: 1;
  height: 6px;
  min-width: 60px;
  overflow: hidden;
  border-radius: 3px;
  background: var(--sw-fill);
}

.growth__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--sw-blush);
}

.growth__text {
  font-size: 12px;
  color: var(--sw-ink-3);
  white-space: nowrap;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.members__note {
  margin-top: 16px;
  font-size: 12px;
  color: var(--sw-ink-3);
}

.mcard {
  padding: 18px;
  border: 1px solid var(--sw-border-soft);
  border-radius: var(--sw-r-lg);
  background: var(--sw-surface-2);
}

.mcard__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mcard__name {
  font-size: 18px;
  font-weight: 600;
}

.mcard__phone {
  margin-top: 2px;
  font-size: 13px;
  color: var(--sw-ink-3);
}

.mcard__stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 16px;
}

.mcard__stats dt {
  font-size: 12px;
  color: var(--sw-ink-3);
}

.mcard__stats dd {
  font-size: 18px;
  font-weight: 700;
}

.log-title {
  margin: 24px 0 8px;
  font-size: 14px;
  font-weight: 600;
}

.log-empty {
  padding: 20px 0;
  font-size: 13px;
  color: var(--sw-ink-3);
}

.log {
  list-style: none;
}

.log li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-top: 1px solid var(--sw-border-soft);
}

.log__main,
.log__side {
  display: flex;
  flex-direction: column;
}

.log__side {
  align-items: flex-end;
}

.log__reason {
  font-size: 13px;
  font-weight: 500;
}

.log__meta,
.log__balance {
  font-size: 12px;
  color: var(--sw-ink-3);
}

.log__points {
  font-size: 15px;
  font-weight: 700;
}

.log__points.is-plus {
  color: var(--sw-success);
}

.log__points.is-minus {
  color: var(--sw-danger);
}
</style>
