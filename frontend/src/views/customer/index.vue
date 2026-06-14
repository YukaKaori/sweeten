<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, EditPen, Delete } from '@element-plus/icons-vue'
import { queryPageApi, addApi, updateApi, deleteApi, queryOrdersApi } from '@/api/modules/customer'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import SkeletonTable from '@/components/SkeletonTable.vue'

const searchName = ref('')
const searchPhone = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const customerList = ref([])

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增客户')
const isEdit = ref(false)
const formRef = ref()
const defaultForm = {
  id: null,
  name: '',
  phone: '',
  address: '',
  gender: 1,
  remark: '',
}
const form = reactive({ ...defaultForm })
const rules = {
  name: [
    { required: true, message: '请输入客户姓名', trigger: 'blur' },
    { min: 2, max: 30, message: '姓名长度 2-30 个字符', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
}

// 详情抽屉
const detailDrawer = ref(false)
const detailCustomer = ref(null)
const detailOrders = ref([])
const detailLoading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const result = await queryPageApi(searchName.value, searchPhone.value, currentPage.value, pageSize.value)
    if (result.code) {
      customerList.value = result.data.records || result.data || []
      total.value = result.data.total || 0
    }
  } catch { /* handled by interceptor */ } finally { loading.value = false }
}

const handleSearch = () => { currentPage.value = 1; loadData() }
const handleReset = () => {
  searchName.value = ''
  searchPhone.value = ''
  currentPage.value = 1
  loadData()
}
const handlePageChange = (page) => { currentPage.value = page; loadData() }
const handleSizeChange = (size) => { pageSize.value = size; currentPage.value = 1; loadData() }

const openAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增客户'
  Object.assign(form, { ...defaultForm, id: null })
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑客户'
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    const api = isEdit.value ? updateApi : addApi
    const result = await api(form)
    if (result.code) {
      ElMessage.success(isEdit.value ? '客户信息更新成功' : '客户新增成功')
      dialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(result.msg || '操作失败')
    }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除客户「${row.name}」吗？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    const result = await deleteApi(row.id)
    if (result.code) {
      ElMessage.success('删除成功')
      if (customerList.value.length === 1 && currentPage.value > 1) currentPage.value--
      loadData()
    } else {
      ElMessage.error(result.msg || '删除失败')
    }
  } catch { /* 取消 */ }
}

const openDetail = async (row) => {
  detailCustomer.value = row
  detailDrawer.value = true
  detailLoading.value = true
  try {
    const result = await queryOrdersApi(row.id, 1, 50)
    if (result.code) {
      detailOrders.value = result.data.records || result.data || []
    } else {
      detailOrders.value = []
    }
  } catch {
    detailOrders.value = []
  } finally {
    detailLoading.value = false
  }
}

// 订单状态
const ORDER_STATUS_MAP = { 0: '待支付', 1: '待接单', 2: '制作中', 3: '配送中', 4: '已完成', 5: '已取消' }
const ORDER_STATUS_TYPE = { 0: 'info', 1: 'warning', 2: '', 3: 'primary', 4: 'success', 5: 'danger' }

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <PageHeader title="客户管理" description="管理客户信息，查看消费记录和订单历史">
      <template #actions>
        <el-button type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon>
          新增客户
        </el-button>
      </template>
    </PageHeader>

    <!-- 搜索栏 -->
    <div class="search-bar animate-fade-in-up delay-1">
      <el-input v-model="searchName" placeholder="客户姓名..." clearable style="width: 180px" @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-input v-model="searchPhone" placeholder="手机号..." clearable style="width: 180px" @keyup.enter="handleSearch" />
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
      <el-button @click="handleReset">重置</el-button>
      <span class="result-count">共 {{ total }} 条记录</span>
    </div>

    <!-- 表格 -->
    <SkeletonTable v-if="loading" :rows="8" :columns="7" />
    <div v-else class="animate-fade-in-up delay-2">
      <el-table v-if="customerList.length > 0" :data="customerList" stripe style="width: 100%" @row-click="openDetail">
        <el-table-column prop="id" label="ID" width="55" align="center" />
        <el-table-column prop="name" label="姓名" min-width="100">
          <template #default="{ row }">
            <span class="customer-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="性别" width="65" align="center">
          <template #default="{ row }">
            <el-tag :type="row.gender === 1 ? '' : 'danger'" size="small" effect="plain">
              {{ row.gender === 1 ? '男' : '女' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="注册时间" width="110" align="center">
          <template #default="{ row }">
            {{ row.createTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="remark-text">{{ row.remark || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click.stop="openEdit(row)">
              <el-icon><EditPen /></el-icon>
              编辑
            </el-button>
            <el-button type="danger" size="small" link @click.stop="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-else title="暂无客户数据" description="点击右上角「新增客户」添加第一个客户" />

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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="70px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="客户姓名" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="收货地址（选填）" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注（选填）" maxlength="100" show-word-limit />
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

    <!-- 客户详情抽屉 -->
    <el-drawer v-model="detailDrawer" :title="detailCustomer?.name + ' - 客户详情'" size="480px">
      <template v-if="detailCustomer">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="姓名">{{ detailCustomer.name }}</el-descriptions-item>
          <el-descriptions-item label="性别">
            <el-tag :type="detailCustomer.gender === 1 ? '' : 'danger'" size="small">
              {{ detailCustomer.gender === 1 ? '男' : '女' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detailCustomer.phone }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ detailCustomer.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ detailCustomer.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailCustomer.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="section-subtitle">订单历史（{{ detailOrders.length }} 笔）</h4>
        <div v-if="detailLoading" style="padding: 20px; text-align: center;">
          <el-icon class="is-loading" :size="20"><Refresh /></el-icon>
          <span style="margin-left: 8px; color: var(--color-text-muted)">加载中...</span>
        </div>
        <el-table v-else-if="detailOrders.length > 0" :data="detailOrders" size="small" style="margin-top: 12px">
          <el-table-column prop="orderNo" label="订单号" width="150" show-overflow-tooltip />
          <el-table-column label="金额" width="80" align="right">
            <template #default="{ row }">
              <span style="color: #e8637a; font-weight: 600;">¥{{ row.amount?.toFixed(2) || '0.00' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="ORDER_STATUS_TYPE[row.status] || 'info'" size="small">
                {{ ORDER_STATUS_MAP[row.status] || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="110" align="center">
            <template #default="{ row }">
              {{ row.createTime || '-' }}
            </template>
          </el-table-column>
        </el-table>
        <EmptyState v-else title="暂无订单记录" description="该客户还没有下单记录" />
      </template>
    </el-drawer>
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

.customer-name {
  font-weight: 600;
  color: var(--color-text-primary, #2d2327);
  cursor: pointer;
}

.customer-name:hover {
  color: var(--color-primary, #e8637a);
}

.remark-text {
  color: var(--color-text-muted, #a3949b);
  font-size: 13px;
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

.section-subtitle {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary, #2d2327);
  margin: 24px 0 0;
  padding-top: 16px;
  border-top: 1px solid var(--glass-border, rgba(45, 35, 39, 0.06));
}
</style>
