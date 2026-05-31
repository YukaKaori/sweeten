<script setup>
import { ref, onMounted, computed } from 'vue'
import { Search, View, Download } from '@element-plus/icons-vue'
import { queryOperateLogApi, queryOperateLogByIdApi } from '@/api/log'
import { exportCSV } from '@/composables/useExport'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import SkeletonTable from '@/components/SkeletonTable.vue'

// 搜索
const searchForm = ref({
  className: '',
  methodName: ''
})

// 日志列表
const logList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 详情抽屉
const drawerVisible = ref(false)
const currentLog = ref(null)

// 方法名中文映射
const methodLabels = {
  'login': '登录',
  'insert': '新增',
  'update': '修改',
  'delete': '删除',
  'select': '查询',
  'export': '导出',
  'upload': '上传'
}

// 方法名对应的标签类型
const methodTagType = (method) => {
  if (!method) return 'info'
  const m = method.toLowerCase()
  if (m.includes('insert') || m.includes('add') || m.includes('save')) return 'success'
  if (m.includes('update') || m.includes('edit')) return 'primary'
  if (m.includes('delete') || m.includes('remove')) return 'danger'
  if (m.includes('login') || m.includes('logout')) return 'info'
  if (m.includes('export')) return 'warning'
  return 'info'
}

// 获取方法中文名
const getMethodLabel = (method) => {
  if (!method) return '-'
  const m = method.toLowerCase()
  for (const [key, label] of Object.entries(methodLabels)) {
    if (m.includes(key)) return label
  }
  return method
}

// 提取类名中的简短名称
const getShortClassName = (className) => {
  if (!className) return '-'
  const parts = className.split('.')
  return parts[parts.length - 1]
}

onMounted(() => {
  search()
})

// 查询
const search = async () => {
  loading.value = true
  try {
    const result = await queryOperateLogApi(
      currentPage.value,
      pageSize.value,
      searchForm.value.className,
      searchForm.value.methodName
    )
    if (result.code) {
      logList.value = result.data.rows || result.data
      total.value = result.data.total || logList.value.length
    }
  } catch {
    // API 未实现时使用空数据
    logList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 清空搜索
const clear = () => {
  searchForm.value = { className: '', methodName: '' }
  currentPage.value = 1
  search()
}

// 分页
const handleSizeChange = () => {
  currentPage.value = 1
  search()
}
const handleCurrentChange = () => {
  search()
}

// 查看详情
const viewDetail = async (row) => {
  try {
    const result = await queryOperateLogByIdApi(row.id)
    if (result.code) {
      currentLog.value = result.data
    } else {
      currentLog.value = row
    }
  } catch {
    currentLog.value = row
  }
  drawerVisible.value = true
}

// 格式化参数
const formatParams = (params) => {
  if (!params) return '-'
  try {
    const parsed = JSON.parse(params)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return params
  }
}

// 格式化返回值
const formatReturnValue = (value) => {
  if (!value) return '-'
  try {
    const parsed = JSON.parse(value)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return value
  }
}

// 导出
const handleExport = () => {
  const columns = [
    { key: 'operateEmpId', label: '操作人ID' },
    { key: 'className', label: '操作类' },
    { key: 'methodName', label: '方法名' },
    { key: 'costTime', label: '耗时(ms)' },
    { key: 'operateTime', label: '操作时间' }
  ]
  exportCSV(logList.value, columns, '操作日志')
}
</script>

<template>
  <div class="page-container">
    <PageHeader title="操作日志" description="查看系统操作记录，追溯关键行为">
      <template #actions>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出日志
        </el-button>
      </template>
    </PageHeader>

    <!-- 搜索栏 -->
    <div class="search-card glass-panel animate-fade-in-up delay-1">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="操作类">
          <el-input v-model="searchForm.className" placeholder="如：DeptController" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="方法名">
          <el-input v-model="searchForm.methodName" placeholder="如：delete" clearable style="width: 160px" />
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

    <!-- 日志列表 -->
    <div class="table-card glass-panel animate-fade-in-up delay-2">
      <SkeletonTable v-if="loading" :rows="8" :columns="6" />
      <el-table v-else :data="logList" style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center">
          <template #default="scope">
            <span class="row-index">{{ (currentPage - 1) * pageSize + scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="operateEmpId" label="操作人ID" width="100" align="center">
          <template #default="scope">
            <span class="emp-id">{{ scope.row.operateEmpId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="操作类" min-width="180">
          <template #default="scope">
            <span class="class-name">{{ getShortClassName(scope.row.className) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="methodName" label="方法" width="120" align="center">
          <template #default="scope">
            <el-tag :type="methodTagType(scope.row.methodName)" size="small" effect="plain">
              {{ scope.row.methodName || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时" width="100" align="center">
          <template #default="scope">
            <span :class="['cost-time', { slow: scope.row.costTime > 1000 }]">
              {{ scope.row.costTime ? scope.row.costTime + 'ms' : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="operateTime" label="操作时间" width="180" align="center">
          <template #default="scope">
            <span class="time-text">{{ scope.row.operateTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="scope">
            <el-button type="primary" link size="small" @click="viewDetail(scope.row)">
              <el-icon><View /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState
        v-if="!loading && logList.length === 0"
        title="暂无操作日志"
        description="当系统中有操作发生时，日志将自动记录在这里"
      />

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :background="true"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="日志详情" size="520px" v-if="currentLog">
      <div class="log-detail">
        <!-- 基本信息 -->
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">日志ID</span>
              <span class="detail-value">{{ currentLog.id }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">操作人ID</span>
              <span class="detail-value">{{ currentLog.operateEmpId || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">操作类</span>
              <span class="detail-value">{{ currentLog.className || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">方法名</span>
              <el-tag :type="methodTagType(currentLog.methodName)" size="small">
                {{ currentLog.methodName || '-' }}
              </el-tag>
            </div>
            <div class="detail-item">
              <span class="detail-label">耗时</span>
              <span class="detail-value" :class="{ slow: currentLog.costTime > 1000 }">
                {{ currentLog.costTime ? currentLog.costTime + 'ms' : '-' }}
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-label">操作时间</span>
              <span class="detail-value">{{ currentLog.operateTime }}</span>
            </div>
          </div>
        </div>

        <!-- 请求参数 -->
        <div class="detail-section">
          <h4>请求参数</h4>
          <pre class="code-block">{{ formatParams(currentLog.methodParams) }}</pre>
        </div>

        <!-- 返回值 -->
        <div class="detail-section">
          <h4>返回值</h4>
          <pre class="code-block">{{ formatReturnValue(currentLog.returnValue) }}</pre>
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
  margin-bottom: 20px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.table-card {
  padding: 4px;
}

.row-index {
  font-size: 13px;
  color: var(--color-text-muted);
}

.emp-id {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.class-name {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 13px;
  color: var(--color-primary);
  font-weight: 500;
}

.cost-time {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.cost-time.slow {
  color: #ef6b6b;
  font-weight: 600;
}

.time-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.pagination-wrapper {
  padding: 16px 20px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--color-border-light);
}

/* 日志详情 */
.log-detail {
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

.detail-label {
  font-size: 12px;
  color: var(--color-text-muted);
  font-weight: 500;
}

.detail-value {
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.detail-value.slow {
  color: #ef6b6b;
}

.code-block {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
}
</style>
