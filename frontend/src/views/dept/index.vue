<script setup>
import { onMounted, ref } from 'vue'
import { EditPen, Delete, Plus, Search } from '@element-plus/icons-vue'
import { queryAllApi, addApi, queryByIdApi, updateApi, deleteByIdApi } from '@/api/modules/dept'
import { ElMessage, ElMessageBox } from 'element-plus'
import SkeletonTable from '@/components/SkeletonTable.vue'

const deptList = ref([])
const dialogFormVisible = ref(false)
const dept = ref({ name: '' })
const formTitle = ref('')
const loading = ref(false)

onMounted(() => {
  search()
})

const search = async () => {
  loading.value = true
  try {
    const result = await queryAllApi()
    if (result.code) {
      deptList.value = result.data
    }
  } finally {
    loading.value = false
  }
}

const addDept = () => {
  dialogFormVisible.value = true
  formTitle.value = '新增部门'
  dept.value = { name: '' }
  if (deptFormRef.value) {
    deptFormRef.value.resetFields()
  }
}

const save = async () => {
  if (!deptFormRef.value) return
  deptFormRef.value.validate(async (valid) => {
    if (valid) {
      let result
      if (dept.value.id) {
        result = await updateApi(dept.value)
      } else {
        result = await addApi(dept.value)
      }
      if (result.code) {
        ElMessage.success('保存成功')
        dialogFormVisible.value = false
        search()
      } else {
        ElMessage.error(result.message)
      }
    } else {
      ElMessage.error('请填写正确的部门名称')
    }
  })
}

const rules = ref({
  name: [
    { required: true, message: '请输入部门名称', trigger: 'blur' },
    { min: 3, max: 10, message: '部门名称长度必须在3到10个字符之间', trigger: 'blur' },
  ]
})

const deptFormRef = ref()

const edit = async (id) => {
  const result = await queryByIdApi(id)
  if (result.code) {
    dept.value = result.data
    dialogFormVisible.value = true
    formTitle.value = '编辑部门'
    if (deptFormRef.value) {
      deptFormRef.value.resetFields()
    }
  }
}

const deleteById = async (id) => {
  ElMessageBox.confirm(
    '删除后将无法恢复，确定要删除该部门吗？',
    '确认删除',
    {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      const result = await deleteByIdApi(id)
      if (result.code) {
        ElMessage.success('删除成功')
        search()
      } else {
        ElMessage.error(result.msg)
      }
    })
    .catch(() => {
      ElMessage.info('已取消删除')
    })
}
</script>

<template>
  <div class="page-container">
    <!-- Page Header -->
    <div class="page-header animate-fade-in-up">
      <div class="page-header-left">
        <h1 class="page-title">部门管理</h1>
        <p class="page-desc">管理公司组织架构中的部门信息</p>
      </div>
    </div>

    <!-- Action Bar -->
    <div class="action-bar animate-fade-in-up delay-1">
      <el-button type="primary" @click="addDept">
        <el-icon><Plus /></el-icon>
        新增部门
      </el-button>
    </div>

    <!-- Table Card -->
    <div class="table-card glass-panel animate-fade-in-up delay-2">
      <SkeletonTable v-if="loading" :rows="6" :columns="4" />
      <el-table v-else :data="deptList" style="width: 100%">
        <el-table-column type="index" label="#" width="80" align="center">
          <template #default="scope">
            <span class="row-index">{{ scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="部门名称" min-width="200">
          <template #default="scope">
            <span class="dept-name">{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="最后更新时间" min-width="200" align="center">
          <template #default="scope">
            <span class="time-text">{{ scope.row.updateTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
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

      <!-- Empty State -->
      <div v-if="!loading && deptList.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
            <circle cx="32" cy="32" r="28" stroke="#ede6e1" stroke-width="2" fill="none"/>
            <path d="M24 28h16M24 36h10" stroke="#a3949b" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <p class="empty-text">暂无部门数据</p>
        <p class="empty-desc">点击"新增部门"按钮创建第一个部门</p>
      </div>
    </div>

    <!-- Dialog -->
    <el-dialog v-model="dialogFormVisible" :title="formTitle" width="460" destroy-on-close>
      <el-form :model="dept" :rules="rules" ref="deptFormRef" label-width="80px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="dept.name" placeholder="请输入部门名称（3-10个字符）" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确认保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.page-desc {
  font-size: 14px;
  color: var(--color-text-muted);
}

.action-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 12px;
}

.table-card {
  padding: 4px;
}

.row-index {
  font-size: 13px;
  color: var(--color-text-muted);
  font-weight: 500;
}

.dept-name {
  font-weight: 600;
  color: var(--color-text-primary);
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.empty-icon {
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.empty-desc {
  font-size: 13px;
  color: var(--color-text-muted);
}
</style>
