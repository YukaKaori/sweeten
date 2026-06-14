<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { StarFilled, ChatDotRound, Delete } from '@element-plus/icons-vue'
import { queryByIdApi } from '@/api/modules/dessert'
import { queryByDessertApi, addApi, deleteApi } from '@/api/modules/comment'
import { useUserStore } from '@/stores/modules/user'
import { DESSERT_CATEGORIES, getLabelByValue } from '@/utils/constants'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'

const route = useRoute()
const userStore = useUserStore()
const dessertId = computed(() => Number(route.params.id))

// 甜品信息
const dessert = ref(null)
const dessertLoading = ref(false)

// 评论
const comments = ref([])
const commentsLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 评论表单
const commentContent = ref('')
const commentRating = ref(5)
const submitting = ref(false)

// 分类颜色映射
const categoryColors = {
  cake: '#e8637a',
  bread: '#f0a35c',
  drink: '#6b8cce',
  dessert: '#5cb88a',
  icecream: '#a78bfa'
}

// 加载甜品信息
const loadDessert = async () => {
  dessertLoading.value = true
  try {
    const result = await queryByIdApi(dessertId.value)
    if (result.code) {
      dessert.value = result.data
    } else {
      ElMessage.error(result.msg || '甜品信息加载失败')
    }
  } catch {
    ElMessage.error('甜品信息加载异常')
  } finally {
    dessertLoading.value = false
  }
}

// 加载评论列表
const loadComments = async () => {
  commentsLoading.value = true
  try {
    const result = await queryByDessertApi(dessertId.value, currentPage.value, pageSize.value)
    if (result.code) {
      comments.value = result.data.records || result.data || []
      total.value = result.data.total || 0
    } else {
      // 后端未实现时用空数组
      comments.value = []
      total.value = 0
    }
  } catch {
    // 后端未实现时降级为空数组
    comments.value = []
    total.value = 0
  } finally {
    commentsLoading.value = false
  }
}

// 提交评论
const submitComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  submitting.value = true
  try {
    const result = await addApi(dessertId.value, commentContent.value.trim(), commentRating.value)
    if (result.code) {
      ElMessage.success('评论发表成功')
      commentContent.value = ''
      commentRating.value = 5
      currentPage.value = 1
      await loadComments()
    } else {
      ElMessage.error(result.msg || '评论发表失败')
    }
  } catch {
    ElMessage.error('评论发表异常')
  } finally {
    submitting.value = false
  }
}

// 删除评论
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    const result = await deleteApi(id)
    if (result.code) {
      ElMessage.success('评论已删除')
      await loadComments()
    } else {
      ElMessage.error(result.msg || '删除失败')
    }
  } catch {
    // 用户取消删除
  }
}

// 分页变化
const handlePageChange = (page) => {
  currentPage.value = page
  loadComments()
}

// 格式化时间
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  return d.toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadDessert()
  loadComments()
})
</script>

<template>
  <div class="dessert-detail-page">
    <PageHeader :title="dessert?.name || '甜品详情'" description="查看甜品信息和顾客评论">
      <template #actions>
        <el-button @click="$router.back()">返回</el-button>
      </template>
    </PageHeader>

    <!-- 甜品信息卡 -->
    <SkeletonCard v-if="dessertLoading" :count="1" :columns="1" />
    <div v-else-if="dessert" class="dessert-info-card glass-panel--hover animate-fade-in-up">
      <div class="info-layout">
        <div class="info-image">
          <img v-if="dessert.image" :src="dessert.image" :alt="dessert.name" class="dessert-image" />
          <div v-else class="image-placeholder" :style="{ background: `${(categoryColors[dessert.category] || '#e8637a')}15` }">
            <span class="placeholder-emoji">🧁</span>
          </div>
        </div>
        <div class="info-body">
          <div class="info-header">
            <h2 class="dessert-name">{{ dessert.name }}</h2>
            <el-tag
              :style="{ '--tag-color': categoryColors[dessert.category] || '#e8637a' }"
              class="category-tag"
              size="small"
            >
              {{ getLabelByValue(DESSERT_CATEGORIES, dessert.category) }}
            </el-tag>
          </div>
          <p class="dessert-desc" v-if="dessert.description">{{ dessert.description }}</p>
          <div class="info-stats">
            <div class="stat-item">
              <span class="stat-label">现价</span>
              <span class="stat-value price-current">¥{{ dessert.price?.toFixed(2) }}</span>
            </div>
            <div class="stat-item" v-if="dessert.originalPrice > dessert.price">
              <span class="stat-label">原价</span>
              <span class="stat-value price-original">¥{{ dessert.originalPrice?.toFixed(2) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">月销量</span>
              <span class="stat-value">{{ dessert.sales || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">状态</span>
              <el-tag :type="dessert.status === 1 ? 'success' : 'info'" size="small">
                {{ dessert.status === 1 ? '在售' : '已下架' }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>

    <EmptyState
      v-else
      title="甜品不存在"
      description="该甜品可能已被删除或下架"
    />

    <!-- 评论区域 -->
    <div v-if="dessert" class="comment-section animate-fade-in-up delay-1">
      <div class="comment-section-header">
        <h3 class="section-title">
          <el-icon><ChatDotRound /></el-icon>
          顾客评论
          <span class="comment-count">{{ total }}</span>
        </h3>
      </div>

      <!-- 发表评论 -->
      <div class="comment-form glass-panel--hover">
        <div class="form-header">
          <span class="form-label">发表评论</span>
          <div class="rating-stars">
            <el-icon
              v-for="s in 5"
              :key="s"
              :size="20"
              :style="{ color: s <= commentRating ? '#f0a35c' : 'rgba(45,35,39,0.15)', cursor: 'pointer' }"
              @click="commentRating = s"
            >
              <StarFilled />
            </el-icon>
          </div>
        </div>
        <el-input
          v-model="commentContent"
          type="textarea"
          :rows="3"
          placeholder="写下你对这款甜品的评价..."
          maxlength="500"
          show-word-limit
        />
        <div class="form-footer">
          <span class="form-hint">以 {{ userStore.username || '管理员' }} 的身份评论</span>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="!commentContent.trim()"
            @click="submitComment"
          >
            发表评论
          </el-button>
        </div>
      </div>

      <!-- 评论列表 -->
      <SkeletonCard v-if="commentsLoading" :count="4" :columns="1" />
      <div v-else class="comment-list">
        <div
          v-for="c in comments"
          :key="c.id"
          class="comment-item glass-panel--hover"
        >
          <div class="comment-avatar" :style="{ background: c.avatarBg || '#e8637a' }">
            {{ (c.username || '匿')[0] }}
          </div>
          <div class="comment-body">
            <div class="comment-meta">
              <span class="comment-username">{{ c.username || '匿名用户' }}</span>
              <div class="comment-stars">
                <el-icon
                  v-for="s in 5"
                  :key="s"
                  :size="12"
                  :style="{ color: s <= (c.rating || 5) ? '#f0a35c' : 'rgba(45,35,39,0.1)' }"
                >
                  <StarFilled />
                </el-icon>
              </div>
              <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
            </div>
            <p class="comment-content">{{ c.content }}</p>
          </div>
          <el-button
            class="comment-delete-btn"
            type="danger"
            size="small"
            text
            @click="handleDelete(c.id)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>

        <EmptyState
          v-if="comments.length === 0"
          title="暂无评论"
          description="快来发表第一条评论吧"
        />

        <!-- 分页 -->
        <div v-if="total > pageSize" class="comment-pagination">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            small
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dessert-detail-page {
  max-width: 900px;
  margin: 0 auto;
}

/* 甜品信息卡 */
.dessert-info-card {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--glass-specular), var(--glass-shadow);
  margin-bottom: 28px;
}

.info-layout {
  display: flex;
  gap: 28px;
  align-items: center;
}

.info-image {
  width: 240px;
  height: 180px;
  border-radius: var(--radius-md);
  overflow: hidden;
  flex-shrink: 0;
}

.dessert-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-emoji {
  font-size: 48px;
}

.info-body {
  flex: 1;
  min-width: 0;
}

.info-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.dessert-name {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary, #2d2327);
  margin: 0;
}

.category-tag {
  border-radius: 6px !important;
  font-weight: 600 !important;
  border: 1px solid var(--tag-color) !important;
  color: var(--tag-color) !important;
  background: transparent !important;
}

.dessert-desc {
  font-size: 14px;
  color: var(--color-text-secondary, #6b5a60);
  line-height: 1.6;
  margin-bottom: 16px;
}

.info-stats {
  display: flex;
  gap: 32px;
  align-items: center;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-muted, #a3949b);
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary, #2d2327);
}

.price-current {
  color: var(--color-primary-dark, #c9354a);
  font-size: 20px;
}

.price-original {
  font-size: 14px;
  text-decoration: line-through;
  color: var(--color-text-muted, #a3949b);
}

/* 评论区 */
.comment-section {
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--glass-specular), var(--glass-shadow);
}

.comment-section-header {
  margin-bottom: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary, #2d2327);
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.comment-count {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-muted, #a3949b);
  background: var(--color-bg-tertiary, #f5f0ec);
  padding: 2px 10px;
  border-radius: 99px;
}

/* 评论表单 */
.comment-form {
  background: rgba(255, 255, 255, 0.35);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  padding: 20px;
  margin-bottom: 20px;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary, #2d2327);
}

.rating-stars {
  display: flex;
  gap: 2px;
}

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.form-hint {
  font-size: 12px;
  color: var(--color-text-muted, #a3949b);
}

/* 评论列表 */
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  display: flex;
  gap: 14px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.3);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  position: relative;
}

.comment-item:hover {
  transform: translateY(-1px);
  border-color: rgba(232, 99, 122, 0.15);
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.comment-username {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary, #2d2327);
}

.comment-stars {
  display: flex;
  gap: 1px;
}

.comment-time {
  font-size: 11px;
  color: var(--color-text-muted, #a3949b);
  margin-left: auto;
}

.comment-content {
  font-size: 13.5px;
  color: var(--color-text-secondary, #6b5a60);
  line-height: 1.5;
  margin: 0;
}

.comment-delete-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.comment-item:hover .comment-delete-btn {
  opacity: 1;
}

.comment-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(45, 35, 39, 0.04);
}

@media (max-width: 640px) {
  .info-layout {
    flex-direction: column;
  }
  .info-image {
    width: 100%;
    height: 200px;
  }
  .info-stats {
    flex-wrap: wrap;
    gap: 16px;
  }
}
</style>
