<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/modules/user'
import { useTheme } from '@/composables/useTheme'
import { getProfileApi, updatePasswordApi } from '@/api/modules/emp'
import Breadcrumb from '@/components/Breadcrumb.vue'
import CommandPalette from '@/components/CommandPalette.vue'
import NotificationPanel from '@/components/NotificationPanel.vue'
import UserProfilePanel from '@/components/UserProfilePanel.vue'
import {
  HomeFilled,
  Goods,
  Wallet,
  UserFilled,
  Avatar,
  DataAnalysis,
  Document,
  SwitchButton,
  Moon,
  Sunny,
  User,
  Lock,
  Postcard,
  Phone,
  OfficeBuilding,
  Male,
  Female
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { theme, toggleTheme } = useTheme()
const isCollapsed = ref(false)

const activeMenu = computed(() => route.path)

// ---- Liquid Glass Pill ----
const navContainer = ref(null)
const pillStyle = ref({})
const pillReady = ref(false)
const movingDirection = ref('')
const prevTop = ref(0)
let moveTimeout = null

const updatePill = () => {
  if (!navContainer.value) return
  const activeEl = navContainer.value.querySelector('.nav-item.active')
  if (!activeEl) {
    pillReady.value = false
    movingDirection.value = ''
    return
  }
  const containerRect = navContainer.value.getBoundingClientRect()
  const itemRect = activeEl.getBoundingClientRect()
  const newTop = itemRect.top - containerRect.top
  const newHeight = itemRect.height

  // Detect motion direction & trigger physics class
  if (pillReady.value && prevTop.value !== newTop) {
    movingDirection.value = newTop > prevTop.value ? 'down' : 'up'
    
    if (moveTimeout) clearTimeout(moveTimeout)
    moveTimeout = setTimeout(() => {
      movingDirection.value = ''
    }, 500) // matches our 0.5s CSS spring curve
  }

  prevTop.value = newTop

  // Set pillStyle with hardware-accelerated transform translateY
  pillStyle.value = {
    transform: `translateY(${newTop}px)`,
    height: `${newHeight}px`,
  }
  pillReady.value = true
}

watch(() => route.path, () => {
  nextTick(() => updatePill())
})

onMounted(() => {
  userStore.initFromStorage()
  nextTick(() => updatePill())
})

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
  nextTick(() => updatePill())
}

// ---- 退出登录 ----
const logout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
    confirmButtonText: '确认退出',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    userStore.logout()
    ElMessage.success('已安全退出')
    router.push('/login')
  }).catch(() => {})
}

// ========== 个人信息弹窗 ==========
const profileVisible = ref(false)
const profileLoading = ref(false)
const profileData = ref(null)
const profileError = ref('')

const openProfile = async () => {
  profileLoading.value = true
  profileError.value = ''
  profileData.value = null
  profileVisible.value = true
  const userId = userStore.userInfo?.id
  if (!userId) {
    profileLoading.value = false
    profileError.value = '无法获取用户信息，请重新登录'
    return
  }
  try {
    const res = await getProfileApi(userId)
    if (res.code && res.data) {
      profileData.value = res.data
    } else {
      profileError.value = res.msg || '获取个人信息失败'
    }
  } catch {
    profileError.value = '请求失败，请确认后端服务已启动'
  } finally {
    profileLoading.value = false
  }
}

const genderLabel = (g) => ({ 1: '男', 2: '女' }[g] || '未知')
const JOB_MAP = { 1: '经理', 2: '副经理', 3: '实习', 4: '运营', 5: '财务' }

// ========== 修改密码弹窗 ==========
const passwordVisible = ref(false)
const passwordLoading = ref(false)
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordFormRef = ref(null)

const openPassword = () => {
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  passwordVisible.value = true
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const submitPassword = async () => {
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }
  passwordLoading.value = true
  try {
    const userId = userStore.userInfo?.id
    const res = await updatePasswordApi(userId, passwordForm.value.oldPassword, passwordForm.value.newPassword)
    if (res.code) {
      ElMessage.success('密码修改成功，请重新登录')
      passwordVisible.value = false
      userStore.logout()
      router.push('/login')
    } else {
      ElMessage.error(res.msg || '修改失败')
    }
  } catch {
    ElMessage.error('修改失败')
  } finally {
    passwordLoading.value = false
  }
}
</script>

<template>
  <div class="layout" :class="{ 'sidebar-collapsed': isCollapsed }">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo-area">
          <div class="logo-icon">
            <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
              <circle cx="16" cy="16" r="14" fill="rgba(232,99,122,0.15)" stroke="var(--color-primary)" stroke-width="1.5"/>
              <path d="M11 20c0-4.5 2.5-9 5-9s5 4.5 5 9" stroke="var(--color-primary)" stroke-width="1.5" stroke-linecap="round" fill="none"/>
              <path d="M8 20h16" stroke="var(--color-primary)" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <transition name="fade">
            <div v-if="!isCollapsed" class="logo-text">
              <span class="logo-title">Dessert</span>
              <span class="logo-sub">Shop</span>
            </div>
          </transition>
        </div>
        <button class="collapse-btn" @click="toggleSidebar">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path v-if="!isCollapsed" d="M10 3L5 8L10 13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path v-else d="M6 3L11 8L6 13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>

      <nav class="sidebar-nav" ref="navContainer">
        <!-- Liquid Glass Pill Indicator -->
        <div 
          class="glass-pill" 
          :class="{ 
            ready: pillReady, 
            'moving-down': movingDirection === 'down', 
            'moving-up': movingDirection === 'up' 
          }" 
          :style="pillStyle"
        >
          <div class="glass-pill-inner"></div>
        </div>

        <router-link to="/index" class="nav-item" :class="{ active: activeMenu === '/index' }">
          <el-icon :size="20"><HomeFilled /></el-icon>
          <transition name="fade">
            <span v-if="!isCollapsed" class="nav-label">首页</span>
          </transition>
        </router-link>

        <div class="nav-group">
          <div v-if="!isCollapsed" class="nav-group-title">甜品管理</div>
          <router-link to="/price" class="nav-item" :class="{ active: activeMenu === '/price' }">
            <el-icon :size="20"><Wallet /></el-icon>
            <transition name="fade">
              <span v-if="!isCollapsed" class="nav-label">价格管理</span>
            </transition>
          </router-link>
          <router-link to="/order" class="nav-item" :class="{ active: activeMenu === '/order' }">
            <el-icon :size="20"><Goods /></el-icon>
            <transition name="fade">
              <span v-if="!isCollapsed" class="nav-label">订单管理</span>
            </transition>
          </router-link>
        </div>

        <div class="nav-group">
          <div v-if="!isCollapsed" class="nav-group-title">系统管理</div>
          <router-link to="/dept" class="nav-item" :class="{ active: activeMenu === '/dept' }">
            <el-icon :size="20"><UserFilled /></el-icon>
            <transition name="fade">
              <span v-if="!isCollapsed" class="nav-label">部门管理</span>
            </transition>
          </router-link>
          <router-link to="/emp" class="nav-item" :class="{ active: activeMenu === '/emp' }">
            <el-icon :size="20"><Avatar /></el-icon>
            <transition name="fade">
              <span v-if="!isCollapsed" class="nav-label">员工管理</span>
            </transition>
          </router-link>
        </div>

        <div class="nav-group">
          <div v-if="!isCollapsed" class="nav-group-title">数据统计</div>
          <router-link to="/report" class="nav-item" :class="{ active: activeMenu === '/report' }">
            <el-icon :size="20"><DataAnalysis /></el-icon>
            <transition name="fade">
              <span v-if="!isCollapsed" class="nav-label">数据报表</span>
            </transition>
          </router-link>
          <router-link to="/log" class="nav-item" :class="{ active: activeMenu === '/log' }">
            <el-icon :size="20"><Document /></el-icon>
            <transition name="fade">
              <span v-if="!isCollapsed" class="nav-label">操作日志</span>
            </transition>
          </router-link>
        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="nav-item logout-btn" @click="logout">
          <el-icon :size="20"><SwitchButton /></el-icon>
          <transition name="fade">
            <span v-if="!isCollapsed" class="nav-label">退出登录</span>
          </transition>
        </div>
      </div>
    </aside>

    <!-- Main Area -->
    <div class="main-area">
      <!-- Header -->
      <header class="header">
        <div class="header-left">
          <Breadcrumb />
        </div>
        <div class="header-right">
          <div class="header-action" @click="toggleTheme" :title="theme === 'dark' ? '切换亮色' : '切换暗色'">
            <el-icon :size="18">
              <Moon v-if="theme === 'light'" />
              <Sunny v-else />
            </el-icon>
          </div>
          <NotificationPanel />
          <div class="header-divider"></div>
          <UserProfilePanel @open-profile="openProfile" @open-password="openPassword" @logout="logout" />
        </div>
      </header>

      <!-- Content -->
      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>

  <CommandPalette />

  <!-- ========== 个人信息弹窗（顶层，不受 header 层级影响） ========== -->
  <el-dialog v-model="profileVisible" title="个人信息" width="420px" destroy-on-close>
    <div v-loading="profileLoading" class="profile-dialog">
      <div v-if="profileError" class="profile-dialog__error">
        <span>{{ profileError }}</span>
      </div>
      <template v-else-if="profileData">
        <div class="profile-dialog__avatar">
          <div class="profile-dialog__avatar-circle">
            {{ (profileData.name || '?').charAt(0) }}
          </div>
          <span class="profile-dialog__avatar-name">{{ profileData.name }}</span>
          <span class="profile-dialog__avatar-job">{{ JOB_MAP[profileData.job] || '未知职位' }}</span>
        </div>
        <div class="profile-dialog__fields">
          <div class="profile-field">
            <el-icon :size="14"><User /></el-icon>
            <span class="profile-field__label">账号</span>
            <span class="profile-field__value">{{ profileData.username }}</span>
          </div>
          <div class="profile-field">
            <el-icon :size="14"><Postcard /></el-icon>
            <span class="profile-field__label">姓名</span>
            <span class="profile-field__value">{{ profileData.name }}</span>
          </div>
          <div class="profile-field">
            <el-icon :size="14">
              <Male v-if="profileData.gender === 1" />
              <Female v-else />
            </el-icon>
            <span class="profile-field__label">性别</span>
            <span class="profile-field__value">{{ genderLabel(profileData.gender) }}</span>
          </div>
          <div class="profile-field">
            <el-icon :size="14"><Phone /></el-icon>
            <span class="profile-field__label">手机号</span>
            <span class="profile-field__value">{{ profileData.phone || '-' }}</span>
          </div>
          <div class="profile-field">
            <el-icon :size="14"><OfficeBuilding /></el-icon>
            <span class="profile-field__label">部门</span>
            <span class="profile-field__value">{{ profileData.deptName || '-' }}</span>
          </div>
          <div class="profile-field">
            <el-icon :size="14"><Postcard /></el-icon>
            <span class="profile-field__label">入职日期</span>
            <span class="profile-field__value">{{ profileData.entryDate || '-' }}</span>
          </div>
        </div>
      </template>
    </div>
  </el-dialog>

  <!-- ========== 修改密码弹窗（顶层） ========== -->
  <el-dialog v-model="passwordVisible" title="修改密码" width="400px" destroy-on-close>
    <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="80px" class="password-form">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordVisible = false">取消</el-button>
      <el-button type="primary" :loading="passwordLoading" @click="submitPassword">确认修改</el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
/* ==============================================
   Apple Liquid Glass Layout
   ============================================== */

.layout {
  display: flex;
  min-height: 100vh;
  background: var(--color-bg-primary);
  position: relative;

  /* Subtle decorative wash behind the glass sidebar for blur to catch */
  &::before {
    content: '';
    position: fixed;
    top: -120px;
    left: -80px;
    width: 420px;
    height: 600px;
    background: radial-gradient(
      ellipse at 30% 40%,
      rgba(232, 99, 122, 0.06) 0%,
      rgba(240, 163, 92, 0.04) 30%,
      rgba(245, 240, 236, 0.03) 60%,
      transparent 100%
    );
    pointer-events: none;
    z-index: 0;
    transition: background 0.4s ease;
  }
}

/* ---- Sidebar — Apple Frosted Glass ---- */
.sidebar {
  width: 240px;
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: var(--z-sidebar);
  transition: width var(--transition-base);
  overflow: hidden;

  /* White frosted glass — Apple-style translucent sidebar */
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(40px) saturate(180%);
  -webkit-backdrop-filter: blur(40px) saturate(180%);
  border-right: 1px solid rgba(0, 0, 0, 0.08);

  /* Active nav text — dark for contrast on white pill */
  --nav-active-color: rgba(0, 0, 0, 0.8);

  /* Subtle warm inner shadow for depth */
  box-shadow:
    inset -8px 0 24px rgba(45, 35, 39, 0.04),
    1px 0 0 rgba(0, 0, 0, 0.04);

  /* Specular highlight along top edge */
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(232, 99, 122, 0.12) 30%,
      rgba(232, 99, 122, 0.2) 50%,
      rgba(232, 99, 122, 0.12) 70%,
      transparent 100%
    );
    z-index: 1;
  }

  /* Subtle warm glow from top */
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(
      180deg,
      rgba(255, 255, 255, 0.6) 0%,
      rgba(255, 245, 242, 0.15) 20%,
      transparent 60%,
      transparent 100%
    );
    pointer-events: none;
  }
}

.sidebar-collapsed .sidebar {
  width: 72px;
}

.sidebar-header {
  padding: 20px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  min-height: 68px;
  position: relative;
  z-index: 1;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden;
}

.logo-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  display: flex;
  flex-direction: column;
  white-space: nowrap;
}

.logo-title {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.logo-sub {
  font-size: 11px;
  color: var(--color-primary);
  letter-spacing: 0.15em;
  text-transform: uppercase;
  font-weight: 500;
}

.collapse-btn {
  background: rgba(0, 0, 0, 0.04);
  border: none;
  color: var(--color-text-muted);
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
  flex-shrink: 0;
  position: relative;
  z-index: 1;

  &:hover {
    background: rgba(0, 0, 0, 0.08);
    color: var(--color-text-primary);
  }
}

/* ---- Navigation ---- */
.sidebar-nav {
  flex: 1;
  padding: 12px 8px;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  z-index: 1;
}

/* ---- iOS 26 Liquid Glass Pill ---- */
.glass-pill {
  position: absolute;
  top: 0; /* Explicitly align with absolute baseline to fix padding offset */
  left: 8px;
  right: 8px;
  z-index: 0;
  border-radius: 12px;
  pointer-events: none;
  opacity: 0;
  will-change: transform, height;

  /* Apple Spring Transition - hardware accelerated */
  transition:
    transform 0.5s cubic-bezier(0.22, 1, 0.36, 1),
    height 0.5s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.4s ease;

  &.ready {
    opacity: 1;
  }
}

.glass-pill-inner {
  width: 100%;
  height: 100%;
  border-radius: inherit;

  /* White glass capsule — luminous on light glass sidebar */
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(255, 255, 255, 0.88) 50%,
    rgba(255, 255, 255, 0.82) 100%
  );
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);

  /* Crisp border for definition on white glass */
  border: 1px solid rgba(0, 0, 0, 0.08);

  /* Depth via shadows to separate from sidebar glass */
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 1),
    0 0 0 1px rgba(0, 0, 0, 0.03),
    0 2px 8px rgba(0, 0, 0, 0.06),
    0 8px 24px rgba(0, 0, 0, 0.08);

  transform-origin: center;
  will-change: transform;
}

/* Liquid Motion Keyframes with Poisson Squeeze Ratio */
.glass-pill.moving-down .glass-pill-inner {
  animation: liquidStretchDown 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

.glass-pill.moving-up .glass-pill-inner {
  animation: liquidStretchUp 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes liquidStretchDown {
  0% {
    transform: scale(1) translateY(0);
    transform-origin: top;
  }
  30% {
    transform: scale(1.08) translateY(4px); /* Scales overall by 1.08x on click/transition */
    transform-origin: top;
  }
  65% {
    transform: scale(0.96) translateY(-2px); /* Snap-back compression */
    transform-origin: top;
  }
  100% {
    transform: scale(1) translateY(0);
    transform-origin: top;
  }
}

@keyframes liquidStretchUp {
  0% {
    transform: scale(1) translateY(0);
    transform-origin: bottom;
  }
  30% {
    transform: scale(1.08) translateY(-4px); /* Scales overall by 1.08x on click/transition */
    transform-origin: bottom;
  }
  65% {
    transform: scale(0.96) translateY(2px); /* Snap-back compression */
    transform-origin: bottom;
  }
  100% {
    transform: scale(1) translateY(0);
    transform-origin: bottom;
  }
}

.nav-group {
  margin-bottom: 8px;
}

.nav-group-title {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--color-text-muted);
  padding: 16px 12px 8px;
  font-weight: 600;
  white-space: nowrap;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
  transition: transform 0.4s cubic-bezier(0.22, 1, 0.36, 1), color 0.3s ease;
  cursor: pointer;
  text-decoration: none;
  margin-bottom: 2px;
  position: relative;
  white-space: nowrap;
  z-index: 1;
  background: transparent !important;

  &:hover {
    color: var(--color-text-primary);
    background: rgba(0, 0, 0, 0.03) !important;
    transform: translateX(4px);
  }

  &.active {
    color: var(--nav-active-color, rgba(0, 0, 0, 0.8)) !important;
    background: transparent !important;
    font-weight: 600;
    text-shadow: none;

    &:hover {
      transform: none; /* Active item shouldn't slide on hover */
    }
  }
}

.nav-label {
  white-space: nowrap;
}

.sidebar-footer {
  padding: 12px 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  position: relative;
  z-index: 1;
}

.logout-btn {
  color: var(--color-text-muted);
  background: transparent !important;
  transition: transform 0.4s cubic-bezier(0.22, 1, 0.36, 1), color 0.3s ease;

  &:hover {
    color: #ef6b6b;
    background: rgba(239, 107, 107, 0.06) !important;
    transform: translateX(4px);
  }
}

.sidebar-collapsed .nav-item:hover,
.sidebar-collapsed .logout-btn:hover {
  transform: scale(1.06) !important;
}

/* ---- Main Area ---- */
.main-area {
  flex: 1;
  margin-left: 240px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  transition: margin-left var(--transition-base);
}

.sidebar-collapsed .main-area {
  margin-left: 72px;
}

/* ---- Header — Frosted Glass ---- */
.header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  position: sticky;
  top: 0;
  z-index: var(--z-header);

  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(40px) saturate(180%);
  -webkit-backdrop-filter: blur(40px) saturate(180%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 1px 3px rgba(45, 35, 39, 0.04);

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(
      90deg,
      transparent,
      rgba(255, 255, 255, 0.6) 20%,
      rgba(255, 255, 255, 0.6) 80%,
      transparent
    );
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-action {
  position: relative;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    background: rgba(45, 35, 39, 0.06);
    color: var(--color-text-primary);
  }
}

.notification-dot {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 7px;
  height: 7px;
  background: var(--color-primary);
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.8);
}

.header-divider {
  width: 1px;
  height: 24px;
  background: var(--color-border);
}

/* ---- Content ---- */
.content {
  flex: 1;
  padding: 24px 28px;
  animation: fadeIn 0.3s ease;
}

/* ---- Page Transition ---- */
.page-enter-active {
  animation: fadeInUp 0.3s ease;
}

.page-leave-active {
  animation: fadeIn 0.15s ease reverse;
}

/* ---- Fade Transition ---- */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ---- Scrollbar for sidebar ---- */
.sidebar-nav::-webkit-scrollbar {
  width: 4px;
}

.sidebar-nav::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
}

/* ---- NProgress ---- */
:global(#nprogress .bar) {
  background: var(--color-primary) !important;
  height: 3px !important;
}

:global(#nprogress .peg) {
  box-shadow: 0 0 10px var(--color-primary), 0 0 5px var(--color-primary) !important;
}

/* ---- 个人信息弹窗 ---- */
.profile-dialog {
  min-height: 200px;
}

.profile-dialog__error {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  color: var(--color-text-muted);
  font-size: 14px;
}

.profile-dialog__avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 0 24px;
}

.profile-dialog__avatar-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  box-shadow: 0 6px 20px rgba(232, 99, 122, 0.3);
}

.profile-dialog__avatar-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.profile-dialog__avatar-job {
  font-size: 13px;
  color: var(--color-text-muted);
  background: var(--color-bg-secondary);
  padding: 2px 12px;
  border-radius: 12px;
}

.profile-dialog__fields {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.profile-field {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  color: var(--color-text-muted);
}

.profile-field__label {
  width: 60px;
  font-size: 13px;
  flex-shrink: 0;
}

.profile-field__value {
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
}

/* ---- 修改密码弹窗 ---- */
.password-form {
  padding-top: 8px;
}

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .sidebar {
    width: 72px;
  }

  .main-area {
    margin-left: 72px;
  }

  .nav-group-title,
  .nav-label,
  .logo-text {
    display: none;
  }
}

/* ---- Dark Mode Overrides ---- */
:global([data-theme="dark"]) .sidebar {
  /* Dark frosted glass for dark mode */
  background: rgba(24, 18, 21, 0.78);
  backdrop-filter: blur(40px) saturate(160%);
  -webkit-backdrop-filter: blur(40px) saturate(160%);
  border-right-color: rgba(255, 255, 255, 0.05);
  box-shadow:
    inset -8px 0 24px rgba(0, 0, 0, 0.15),
    1px 0 0 rgba(0, 0, 0, 0.2);

  &::before {
    background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(240, 140, 158, 0.1) 30%,
      rgba(240, 140, 158, 0.18) 50%,
      rgba(240, 140, 158, 0.1) 70%,
      transparent 100%
    );
  }

  &::after {
    background: linear-gradient(
      180deg,
      rgba(255, 255, 255, 0.04) 0%,
      transparent 30%,
      transparent 100%
    );
  }

  /* Dark mode nav text */
  --nav-active-color: rgba(0, 0, 0, 0.8);
}

:global([data-theme="dark"]) .logo-title {
  color: #f0ebe8;
}

:global([data-theme="dark"]) .logo-sub {
  color: var(--color-primary-light);
}

:global([data-theme="dark"]) .nav-group-title {
  color: rgba(255, 255, 255, 0.3);
}

:global([data-theme="dark"]) .nav-item {
  color: rgba(255, 255, 255, 0.55);
}

:global([data-theme="dark"]) .nav-item:hover {
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.06) !important;
}

:global([data-theme="dark"]) .logout-btn {
  color: rgba(255, 255, 255, 0.35);
}

:global([data-theme="dark"]) .logout-btn:hover {
  color: #ef6b6b;
  background: rgba(239, 107, 107, 0.1) !important;
}

:global([data-theme="dark"]) .collapse-btn {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.45);
}

:global([data-theme="dark"]) .collapse-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.9);
}

:global([data-theme="dark"]) .sidebar-header {
  border-bottom-color: rgba(255, 255, 255, 0.06);
}

:global([data-theme="dark"]) .sidebar-footer {
  border-top-color: rgba(255, 255, 255, 0.06);
}

:global([data-theme="dark"]) .glass-pill-inner {
  border-color: rgba(255, 255, 255, 0.15);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.6),
    0 0 0 1px rgba(255, 255, 255, 0.04),
    0 2px 8px rgba(0, 0, 0, 0.2),
    0 8px 24px rgba(0, 0, 0, 0.25);
}

:global([data-theme="dark"]) .sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.08);
}

:global([data-theme="dark"]) .header {
  background: rgba(37, 28, 32, 0.65);
  border-bottom-color: rgba(255, 255, 255, 0.06);
}

:global([data-theme="dark"]) .notification-dot {
  border-color: rgba(37, 28, 32, 0.8);
}
</style>
