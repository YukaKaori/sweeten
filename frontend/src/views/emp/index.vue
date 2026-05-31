<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { queryPageApi, queryByIdApi, addApi, updateApi, deleteByIdApi } from '@/api/emp'
import { querryAllApi as queryDeptListApi } from '@/api/dept'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, EditPen, Delete, Search } from '@element-plus/icons-vue'
import { JOB_LIST, GENDER_LIST } from '@/utils/constants'
import { useUserStore } from '@/stores/user'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import SkeletonTable from '@/components/SkeletonTable.vue'

// 搜索表单
const searchEmp = ref({ name: '', gender: '', date: [], begin: '', end: '' })
// 员工列表
const empList = ref([])
// 部门列表
const deptList = ref([])
// loading
const loading = ref(false)
// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 员工表单
const initEmployeeForm = () => ({
  username: '',
  name: '',
  gender: '',
  phone: '',
  job: '',
  salary: '',
  deptId: '',
  entryDate: '',
  image: '',
  exprList: []
})

const employee = ref(initEmployeeForm())
const dialogVisible = ref(false)
const dialogTitle = ref('')
const empFormRef = ref()

// token — from store
const userStore = useUserStore()
const token = computed(() => userStore.token)

// 表单校验规则
const rules = ref({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度应在2到20个字符之间', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 10, message: '姓名长度应在2到10个字符之间', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
  ]
})

onMounted(() => {
  userStore.initFromStorage()
  search()
  queryDeptList()
})

const queryDeptList = async () => {
  const result = await queryDeptListApi()
  if (result.code) {
    deptList.value = result.data
  }
}

// 查询
const search = async () => {
  loading.value = true
  try {
    const result = await queryPageApi(
      searchEmp.value.name,
      searchEmp.value.gender,
      searchEmp.value.begin,
      searchEmp.value.end,
      currentPage.value,
      pageSize.value
    )
    if (result.code) {
      empList.value = result.data.rows
      total.value = result.data.total
    }
  } finally {
    loading.value = false
  }
}

// 清空搜索
const clear = () => {
  searchEmp.value = { name: '', gender: '', date: [], begin: '', end: '' }
  currentPage.value = 1
  search()
}

// 侦听日期变化
watch(() => searchEmp.value.date, (newVal) => {
  if (newVal && newVal.length === 2) {
    searchEmp.value.begin = newVal[0]
    searchEmp.value.end = newVal[1]
  } else {
    searchEmp.value.begin = ''
    searchEmp.value.end = ''
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

// 侦听工作经历日期变化
watch(() => employee.value.exprList, (newVal) => {
  if (newVal && newVal.length > 0) {
    newVal.forEach((expr) => {
      if (expr.exprDate && expr.exprDate.length === 2) {
        expr.begin = expr.exprDate[0]
        expr.end = expr.exprDate[1]
      } else {
        expr.begin = ''
        expr.end = ''
      }
    })
  }
}, { deep: true })

// 新增员工
const addEmp = () => {
  employee.value = initEmployeeForm()
  dialogTitle.value = '新增员工'
  dialogVisible.value = true
  if (empFormRef.value) {
    empFormRef.value.resetFields()
  }
}

// 编辑员工
const edit = async (id) => {
  const result = await queryByIdApi(id)
  if (result.code) {
    employee.value = result.data
    dialogTitle.value = '编辑员工'
    dialogVisible.value = true
    // 恢复工作经历日期
    if (employee.value.exprList && employee.value.exprList.length > 0) {
      employee.value.exprList.forEach((expr) => {
        expr.exprDate = [expr.begin, expr.end]
      })
    }
  } else {
    ElMessage.error(result.msg)
  }
}

// 保存员工
const saveEmp = async () => {
  if (!empFormRef.value) return
  empFormRef.value.validate(async (valid) => {
    if (valid) {
      let result
      if (employee.value.id) {
        result = await updateApi(employee.value)
      } else {
        result = await addApi(employee.value)
      }
      if (result.code) {
        ElMessage.success('保存成功')
        dialogVisible.value = false
        search()
      } else {
        ElMessage.error(result.msg)
      }
    } else {
      ElMessage.error('请填写正确的员工信息')
    }
  })
}

// 删除员工
const deleteById = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该员工吗？删除后将无法恢复。', '确认删除', {
      confirmButtonText: '确认删除',
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
    ElMessage.info('已取消删除')
  }
}

// 批量删除
const selectedIds = ref([])
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}
const deleteByIds = async () => {
  if (!selectedIds.value || selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的员工')
    return
  }
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 名员工吗？`, '批量删除', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const result = await deleteByIdApi(selectedIds.value)
    if (result.code) {
      ElMessage.success('删除成功')
      search()
    } else {
      ElMessage.error(result.msg)
    }
  } catch {
    ElMessage.info('已取消删除')
  }
}

// 头像上传
const handleAvatarSuccess = (response) => {
  employee.value.image = response.data
}
const beforeAvatarUpload = (rawFile) => {
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
    ElMessage.error('只支持 JPG/PNG 格式图片')
    return false
  }
  if (rawFile.size / 1024 / 1024 > 10) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  return true
}

// 工作经历
const addExprItem = () => {
  employee.value.exprList.push({ company: '', job: '', begin: '', end: '', exprDate: [] })
}
const removeExprItem = (index) => {
  employee.value.exprList.splice(index, 1)
}

// 职位/性别映射
const getJobLabel = (value) => {
  const job = JOB_LIST.find(j => j.value === value)
  return job ? job.label : '其他'
}
const getGenderLabel = (value) => value === 1 ? '男' : '女'
</script>

<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <PageHeader title="员工管理" description="管理人员信息、考勤和工作经历">
      <template #actions>
        <el-button type="danger" plain @click="deleteByIds">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
        <el-button type="primary" @click="addEmp">
          <el-icon><Plus /></el-icon>
          新增员工
        </el-button>
      </template>
    </PageHeader>

    <!-- 搜索栏 -->
    <div class="search-card glass-panel animate-fade-in-up delay-1">
      <el-form :inline="true" :model="searchEmp" class="search-form">
        <el-form-item label="姓名">
          <el-input v-model="searchEmp.name" placeholder="请输入姓名" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="searchEmp.gender" placeholder="请选择" clearable style="width: 140px">
            <el-option label="男" value="1" />
            <el-option label="女" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="入职时间">
          <el-date-picker
            v-model="searchEmp.date"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
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

    <!-- 表格 -->
    <div class="table-card glass-panel animate-fade-in-up delay-2">
      <SkeletonTable v-if="loading" :rows="8" :columns="7" />
      <el-table
        v-else
        :data="empList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column type="index" label="#" width="60" align="center">
          <template #default="scope">
            <span class="row-index">{{ (currentPage - 1) * pageSize + scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="100" align="center">
          <template #default="scope">
            <span class="emp-name">{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="性别" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.gender === 1 ? '' : 'danger'" size="small" effect="plain">
              {{ getGenderLabel(scope.row.gender) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="头像" width="80" align="center">
          <template #default="scope">
            <el-avatar :size="36" :src="scope.row.image" v-if="scope.row.image">
              {{ scope.row.name?.charAt(0) }}
            </el-avatar>
            <el-avatar :size="36" v-else>{{ scope.row.name?.charAt(0) }}</el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="部门" width="120" align="center">
          <template #default="scope">
            <span class="dept-tag">{{ scope.row.deptName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="职位" width="120" align="center">
          <template #default="scope">
            <span>{{ getJobLabel(scope.row.job) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="entryDate" label="入职日期" width="130" align="center" />
        <el-table-column prop="updateTime" label="更新时间" min-width="170" align="center">
          <template #default="scope">
            <span class="time-text">{{ scope.row.updateTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button type="primary" size="small" plain @click="edit(scope.row.id)">
                <el-icon><EditPen /></el-icon>
                编辑
              </el-button>
              <el-button type="danger" size="small" plain @click="deleteById(scope.row.id)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <EmptyState
        v-if="!loading && empList.length === 0"
        title="暂无员工数据"
        description='点击"新增员工"按钮添加第一位员工'
      >
        <template #action>
          <el-button type="primary" @click="addEmp">
            <el-icon><Plus /></el-icon>
            新增员工
          </el-button>
        </template>
      </EmptyState>

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

    <!-- 对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720" destroy-on-close>
      <el-form :model="employee" :rules="rules" ref="empFormRef" label-width="80px">
        <!-- 基本信息 -->
        <div class="form-section-title">基本信息</div>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="employee.username" placeholder="请输入用户名（2-20个字符）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="employee.name" placeholder="请输入姓名（2-10个字符）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="employee.gender" placeholder="请选择性别" style="width: 100%">
                <el-option v-for="g in GENDER_LIST" :key="g.value" :label="g.label" :value="g.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="employee.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="职位">
              <el-select v-model="employee.job" placeholder="请选择职位" style="width: 100%">
                <el-option v-for="j in JOB_LIST" :key="j.value" :label="j.label" :value="j.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="薪资">
              <el-input v-model="employee.salary" placeholder="请输入薪资">
                <template #append>元/月</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属部门">
              <el-select v-model="employee.deptId" placeholder="请选择部门" style="width: 100%">
                <el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入职日期">
              <el-date-picker
                v-model="employee.entryDate"
                type="date"
                placeholder="选择日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 头像上传 -->
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="头像">
              <el-upload
                class="avatar-uploader"
                action="/api/upload"
                :headers="{ token: token }"
                :show-file-list="false"
                :on-success="handleAvatarSuccess"
                :before-upload="beforeAvatarUpload"
              >
                <img v-if="employee.image" :src="employee.image" class="avatar-preview" />
                <div v-else class="avatar-placeholder">
                  <el-icon :size="24"><Plus /></el-icon>
                  <span>上传头像</span>
                </div>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 工作经历 -->
        <div class="form-section-title">
          工作经历
          <el-button type="primary" link size="small" @click="addExprItem">
            <el-icon><Plus /></el-icon>
            添加
          </el-button>
        </div>

        <div v-for="(expr, index) in employee.exprList" :key="index" class="expr-item">
          <el-row :gutter="12">
            <el-col :span="10">
              <el-form-item size="small" label="时间" label-width="56px">
                <el-date-picker
                  v-model="expr.exprDate"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始"
                  end-placeholder="结束"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="7">
              <el-form-item size="small" label="公司" label-width="48px">
                <el-input v-model="expr.company" placeholder="公司名称" />
              </el-form-item>
            </el-col>
            <el-col :span="5">
              <el-form-item size="small" label="职位" label-width="48px">
                <el-input v-model="expr.job" placeholder="职位" />
              </el-form-item>
            </el-col>
            <el-col :span="2">
              <el-button type="danger" link @click="removeExprItem(index)" style="margin-top: 2px">
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-col>
          </el-row>
        </div>

        <div v-if="employee.exprList.length === 0" class="expr-empty">
          <el-text type="info" size="small">暂无工作经历，点击上方"添加"按钮添加</el-text>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveEmp">确认保存</el-button>
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

/* 搜索栏 */
.search-card {
  padding: 20px 24px;
  margin-bottom: 20px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.search-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--color-text-secondary);
}

/* 表格卡片 */
.table-card {
  padding: 4px;
}

.row-index {
  font-size: 13px;
  color: var(--color-text-muted);
  font-weight: 500;
}

.emp-name {
  font-weight: 600;
  color: var(--color-text-primary);
}

.dept-tag {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.time-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

/* 分页 */
.pagination-wrapper {
  padding: 16px 20px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--color-border-light);
}

/* 对话框 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.form-section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 头像上传 */
.avatar-uploader :deep(.el-upload) {
  width: 80px;
  height: 80px;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  overflow: hidden;
  transition: border-color var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: var(--color-primary);
}

.avatar-preview {
  width: 80px;
  height: 80px;
  object-fit: cover;
  display: block;
}

.avatar-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--color-text-muted);
}

.avatar-placeholder span {
  font-size: 11px;
}

/* 工作经历 */
.expr-item {
  background: var(--color-bg-primary);
  border-radius: var(--radius-md);
  padding: 12px 12px 0;
  margin-bottom: 12px;
  border: 1px solid var(--color-border-light);
}

.expr-empty {
  text-align: center;
  padding: 16px;
  background: var(--color-bg-primary);
  border-radius: var(--radius-md);
  border: 1px dashed var(--color-border);
}
</style>
