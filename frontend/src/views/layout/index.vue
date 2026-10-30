<script setup>
/**
 * 喜甜门店工作台 —— 外框布局（侧栏 + 页头 + 内容区）
 * 侧栏只显示当前员工有权限的页面（config/navigation.js）；后端仍对每个接口做权限校验。
 */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/modules/user'
import { useTheme } from '@/composables/useTheme'
import { useMotionPref } from '@/composables/useMotionPref'
import { supportsViewTransitions } from '@/router/guards'
import { visibleNavGroups } from '@/config/navigation'
import { BRAND } from '@/config/brand'
import { getProfileApi, updatePasswordApi } from '@/api/modules/emp'
import BrandLogo from '@/components/brand/BrandLogo.vue'
import SwIcon from '@/components/brand/SwIcon.vue'
import Breadcrumb from '@/components/Breadcrumb.vue'
import CommandPalette from '@/components/CommandPalette.vue'
import NotificationPanel from '@/components/NotificationPanel.vue'
import UserProfilePanel from '@/components/UserProfilePanel.vue'

const COLLAPSE_KEY = 'sw-sidebar-collapsed'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
userStore.initFromStorage()
const { theme, toggleTheme, enableTheme, disableTheme } = useTheme()
enableTheme() // 深色外观只在工作台内生效
onUnmounted(disableTheme)

// 换页：支持 View Transitions 时交给 ::view-transition-*（base.css），内层 <transition> 置空避免双重动画
const { isStatic } = useMotionPref()
const pageTransition = computed(() => (supportsViewTransitions && !isStatic.value ? 'vt-none' : 'page'))

const navGroups = computed(() => visibleNavGroups(userStore.can))
const isActive = (path) => route.path === path || route.path.startsWith(`${path}/`)

// ========== 侧栏：桌面折叠 / 窄屏抽屉 ==========
const DRAWER_BREAKPOINT = 1024
const collapsed = ref(localStorage.getItem(COLLAPSE_KEY) === '1')
const isNarrow = ref(false)
const drawerOpen = ref(false)

const checkWidth = () => {
  isNarrow.value = window.innerWidth < DRAWER_BREAKPOINT
  if (!isNarrow.value) drawerOpen.value = false
}

const toggleCollapse = () => {
  collapsed.value = !collapsed.value
  localStorage.setItem(COLLAPSE_KEY, collapsed.value ? '1' : '0')
}

const onKeydown = (e) => {
  if (e.key === 'Escape' && drawerOpen.value) drawerOpen.value = false
}

onMounted(() => {
  checkWidth()
  window.addEventListener('resize', checkWidth)
  document.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkWidth)
  document.removeEventListener('keydown', onKeydown)
})

watch(() => route.path, () => {
  drawerOpen.value = false
})

const sidebarCompact = computed(() => collapsed.value && !isNarrow.value)

// ========== 命令面板 ==========
const paletteRef = ref(null)
const isMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform)

// ========== 退出登录 ==========
const logout = () => {
  ElMessageBox.confirm('退出后需要重新登录才能使用工作台。', '退出登录', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }).catch(() => {})
}

// ========== 个人信息 ==========
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

const genderLabel = (g) => ({ 1: '男', 2: '女' }[g] || '未填写')
const JOB_MAP = { 1: '经理', 2: '副经理', 3: '实习生', 4: '线上运营', 5: '财务出纳' }

// ========== 修改密码 ==========
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
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
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
      ElMessage.success('密码已修改，请用新密码重新登录')
      passwordVisible.value = false
      userStore.logout()
      router.push('/login')
    } else {
      ElMessage.error(res.msg || '修改失败')
    }
  } catch {
    // 拦截器已提示具体原因
  } finally {
    passwordLoading.value = false
  }
}
</script>

<template>
  <div
    class="ws"
    :class="{ 'is-compact': sidebarCompact, 'is-narrow': isNarrow, 'drawer-open': drawerOpen }"
  >
    <a
      class="ws__skip"
      href="#ws-main"
    >跳到主要内容</a>

    <Transition name="fade">
      <div
        v-if="isNarrow && drawerOpen"
        class="ws__backdrop"
        @click="drawerOpen = false"
      ></div>
    </Transition>

    <!-- ===== 侧栏 ===== -->
    <aside
      class="sidebar"
      :aria-hidden="isNarrow && !drawerOpen ? 'true' : undefined"
      :inert="isNarrow && !drawerOpen ? true : undefined"
    >
      <div class="sidebar__brand">
        <router-link
          to="/"
          class="sidebar__logo"
          :aria-label="`${BRAND.fullName} 工作台首页`"
        >
          <BrandLogo
            v-if="sidebarCompact"
            variant="icon"
            :size="36"
          />
          <BrandLogo
            v-else
            variant="horizontal"
            :size="28"
          />
        </router-link>
        <span
          v-if="!sidebarCompact"
          class="sidebar__caption"
        >{{ BRAND.workspace }}</span>
        <button
          v-if="isNarrow"
          type="button"
          class="hdr-btn sidebar__close"
          aria-label="关闭菜单"
          @click="drawerOpen = false"
        >
          <SwIcon
            name="close"
            :size="18"
          />
        </button>
      </div>

      <nav
        class="sidebar__nav"
        aria-label="工作台导航"
      >
        <div
          v-for="group in navGroups"
          :key="group.title || 'root'"
          class="nav-group"
        >
          <p
            v-if="group.title && !sidebarCompact"
            class="nav-group__title"
          >{{ group.title }}</p>
          <div
            v-else-if="group.title"
            class="nav-group__rule"
            aria-hidden="true"
          ></div>
          <router-link
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ 'is-active': isActive(item.path) }"
            :aria-current="isActive(item.path) ? 'page' : undefined"
            :title="sidebarCompact ? item.label : undefined"
          >
            <SwIcon
              :name="item.icon"
              :size="19"
              class="nav-item__icon"
            />
            <span
              v-if="!sidebarCompact"
              class="nav-item__label"
            >{{ item.label }}</span>
          </router-link>
        </div>
      </nav>

      <div class="sidebar__foot">
        <a
          href="/m"
          target="_blank"
          rel="noopener"
          class="nav-item"
          :title="sidebarCompact ? '打开顾客商城' : undefined"
        >
          <SwIcon
            name="bag"
            :size="19"
            class="nav-item__icon"
          />
          <span
            v-if="!sidebarCompact"
            class="nav-item__label"
          >顾客商城</span>
          <SwIcon
            v-if="!sidebarCompact"
            name="external"
            :size="14"
            class="nav-item__ext"
          />
        </a>
        <button
          v-if="!isNarrow"
          type="button"
          class="nav-item"
          :aria-label="sidebarCompact ? '展开侧栏' : '收起侧栏'"
          :title="sidebarCompact ? '展开侧栏' : undefined"
          @click="toggleCollapse"
        >
          <SwIcon
            name="sidebar"
            :size="19"
            class="nav-item__icon"
          />
          <span
            v-if="!sidebarCompact"
            class="nav-item__label"
          >收起侧栏</span>
        </button>
      </div>
    </aside>

    <!-- ===== 主区域 ===== -->
    <div class="ws__main">
      <header class="topbar">
        <div class="topbar__left">
          <button
            v-if="isNarrow"
            type="button"
            class="hdr-btn"
            aria-label="打开菜单"
            :aria-expanded="drawerOpen"
            @click="drawerOpen = true"
          >
            <SwIcon
              name="menu"
              :size="20"
            />
          </button>
          <router-link
            v-if="isNarrow"
            to="/"
            class="topbar__logo"
            :aria-label="BRAND.fullName"
          >
            <BrandLogo
              variant="mark"
              :size="28"
            />
          </router-link>
          <Breadcrumb />
        </div>

        <div class="topbar__right">
          <button
            type="button"
            class="topbar__search"
            @click="paletteRef?.open()"
          >
            <SwIcon
              name="search"
              :size="16"
            />
            <span class="topbar__search-text">搜索功能</span>
            <kbd>{{ isMac ? '⌘' : 'Ctrl' }} K</kbd>
          </button>
          <button
            type="button"
            class="hdr-btn"
            :aria-label="theme === 'dark' ? '切换到浅色外观' : '切换到深色外观'"
            :title="theme === 'dark' ? '浅色外观' : '深色外观'"
            @click="toggleTheme"
          >
            <SwIcon
              :name="theme === 'dark' ? 'sun' : 'moon'"
              :size="19"
            />
          </button>
          <NotificationPanel />
          <span
            class="topbar__sep"
            aria-hidden="true"
          ></span>
          <UserProfilePanel
            @open-profile="openProfile"
            @open-password="openPassword"
            @logout="logout"
          />
        </div>
      </header>

      <main
        id="ws-main"
        class="ws__content"
        tabindex="-1"
      >
        <router-view v-slot="{ Component }">
          <transition
            :name="pageTransition"
            mode="out-in"
          >
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>

  <CommandPalette ref="paletteRef" />

  <!-- 个人信息 -->
  <el-dialog
    v-model="profileVisible"
    title="个人信息"
    width="420px"
    destroy-on-close
  >
    <div
      v-loading="profileLoading"
      class="profile"
    >
      <p
        v-if="profileError"
        class="profile__error"
      >{{ profileError }}</p>
      <template v-else-if="profileData">
        <div class="profile__head">
          <span class="profile__avatar">
            <img
              v-if="profileData.image"
              :src="profileData.image"
              alt=""
            />
            <span v-else>{{ (profileData.name || '?').charAt(0) }}</span>
          </span>
          <div>
            <p class="profile__name">{{ profileData.name }}</p>
            <p class="profile__job">{{ JOB_MAP[profileData.job] || '未设置职位' }} · {{ profileData.deptName || '未分配部门' }}</p>
          </div>
        </div>
        <dl class="profile__fields">
          <div><dt>登录账号</dt><dd>{{ profileData.username }}</dd></div>
          <div><dt>性别</dt><dd>{{ genderLabel(profileData.gender) }}</dd></div>
          <div><dt>手机号</dt><dd>{{ profileData.phone || '—' }}</dd></div>
          <div><dt>入职日期</dt><dd>{{ profileData.entryDate || '—' }}</dd></div>
        </dl>
      </template>
    </div>
  </el-dialog>

  <!-- 修改密码 -->
  <el-dialog
    v-model="passwordVisible"
    title="修改密码"
    width="420px"
    destroy-on-close
  >
    <el-form
      ref="passwordFormRef"
      :model="passwordForm"
      :rules="passwordRules"
      label-position="top"
      @submit.prevent="submitPassword"
    >
      <el-form-item
        label="原密码"
        prop="oldPassword"
      >
        <el-input
          v-model="passwordForm.oldPassword"
          type="password"
          show-password
          autocomplete="current-password"
          placeholder="请输入原密码"
        />
      </el-form-item>
      <el-form-item
        label="新密码"
        prop="newPassword"
      >
        <el-input
          v-model="passwordForm.newPassword"
          type="password"
          show-password
          autocomplete="new-password"
          placeholder="至少 6 位"
        />
      </el-form-item>
      <el-form-item
        label="确认新密码"
        prop="confirmPassword"
      >
        <el-input
          v-model="passwordForm.confirmPassword"
          type="password"
          show-password
          autocomplete="new-password"
          placeholder="再次输入新密码"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordVisible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="passwordLoading"
        @click="submitPassword"
      >确认修改</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.ws {
  display: flex;
  min-height: 100vh;
  background: var(--sw-bg);
}

.ws__skip {
  position: absolute;
  left: 12px;
  top: -48px;
  z-index: 1000;
  padding: 8px 14px;
  border-radius: var(--sw-r-md);
  background: var(--sw-primary);
  color: var(--sw-on-primary);
  font-size: 13px;
}

.ws__skip:focus {
  top: 12px;
}

/* ---------- 侧栏 ---------- */
.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: var(--z-sidebar);
  display: flex;
  flex-direction: column;
  width: var(--sw-sidebar-w);
  background: var(--sw-surface);
  border-right: 1px solid var(--sw-border-soft);
  transition: width var(--sw-dur-2) var(--sw-ease), transform var(--sw-dur-3) var(--sw-ease);
  view-transition-name: app-sidebar;
}

.is-compact .sidebar {
  width: var(--sw-sidebar-w-collapsed);
}

.sidebar__brand {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 22px 22px 18px;
}

.is-compact .sidebar__brand {
  align-items: center;
  padding: 20px 0 14px;
}

.sidebar__logo {
  display: inline-flex;
  border-radius: var(--sw-r-sm);
}

.sidebar__caption {
  padding-left: 36px; /* 与字标对齐（图形 28px + 间距） */
  font-size: 12px;
  color: var(--sw-ink-3);
  letter-spacing: 0.08em;
}

.sidebar__close {
  position: absolute;
  top: 18px;
  right: 12px;
}

.sidebar__nav {
  flex: 1;
  overflow-y: auto;
  padding: 4px 12px 12px;
}

.is-compact .sidebar__nav {
  padding: 4px 14px 12px;
}

.nav-group + .nav-group {
  margin-top: 6px;
}

.nav-group__title {
  padding: 14px 10px 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--sw-ink-3);
  letter-spacing: 0.06em;
}

.nav-group__rule {
  height: 1px;
  margin: 10px 8px;
  background: var(--sw-border-soft);
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  height: 40px;
  padding: 0 12px;
  margin-bottom: 2px;
  border: 0;
  border-radius: var(--sw-r-md);
  background: transparent;
  color: var(--sw-ink-2);
  font-size: 14px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: background-color var(--sw-dur-1) var(--sw-ease), color var(--sw-dur-1) var(--sw-ease);
}

.is-compact .nav-item {
  justify-content: center;
  padding: 0;
}

.nav-item:hover {
  background: var(--sw-fill-soft);
  color: var(--sw-ink);
}

.nav-item.is-active {
  background: var(--sw-accent-soft);
  color: var(--sw-ink);
  font-weight: 600;
}

.nav-item.is-active .nav-item__icon {
  color: var(--sw-accent);
}

.nav-item.is-active::before {
  content: '';
  position: absolute;
  left: -12px;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--sw-accent);
}

.is-compact .nav-item.is-active::before {
  left: -14px;
}

.nav-item__label {
  flex: 1;
  white-space: nowrap;
}

.nav-item__ext {
  color: var(--sw-ink-4);
}

.sidebar__foot {
  padding: 10px 12px 16px;
  border-top: 1px solid var(--sw-border-soft);
}

.is-compact .sidebar__foot {
  padding: 10px 14px 16px;
}

/* ---------- 主区域 ---------- */
.ws__main {
  flex: 1;
  min-width: 0;
  margin-left: var(--sw-sidebar-w);
  transition: margin-left var(--sw-dur-2) var(--sw-ease);
}

.is-compact .ws__main {
  margin-left: var(--sw-sidebar-w-collapsed);
}

.topbar {
  position: sticky;
  top: 0;
  z-index: var(--z-header);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: var(--sw-header-h);
  padding: 0 32px;
  background: var(--sw-bg);
  border-bottom: 1px solid var(--sw-border-soft);
  view-transition-name: app-header;
}

.topbar__left,
.topbar__right {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.topbar__left {
  gap: 10px;
}

.topbar__logo {
  display: inline-flex;
}

.topbar__search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 8px 0 12px;
  margin-right: 6px;
  border: 1px solid var(--sw-border);
  border-radius: var(--sw-r-pill);
  background: var(--sw-surface);
  color: var(--sw-ink-3);
  font-size: 13px;
  cursor: pointer;
  transition: border-color var(--sw-dur-1) var(--sw-ease), color var(--sw-dur-1) var(--sw-ease);
}

.topbar__search:hover {
  border-color: var(--el-border-color-hover);
  color: var(--sw-ink);
}

.topbar__search kbd {
  padding: 1px 6px;
  border: 1px solid var(--sw-border);
  border-radius: 5px;
  background: var(--sw-surface-2);
  font-family: var(--sw-font-sans);
  font-size: 11px;
  color: var(--sw-ink-3);
}

.topbar__sep {
  width: 1px;
  height: 22px;
  margin: 0 6px;
  background: var(--sw-border);
}

.ws__content {
  padding: 28px 32px 48px;
  outline: none;
  /* 注意：不要给内容区设置 view-transition-name —— 它会让内容区成为层叠上下文，
     Element Plus 的抽屉 / 对话框（就地渲染）会被困在页头与侧栏之下 */
}

.ws__content > :deep(*) {
  max-width: 1440px;
  margin-left: auto;
  margin-right: auto;
}

/* ---------- 窄屏：侧栏变抽屉 ---------- */
.is-narrow .sidebar {
  width: min(300px, 86vw);
  transform: translateX(-100%);
  box-shadow: none;
  z-index: calc(var(--z-header) + 10);
}

.is-narrow.drawer-open .sidebar {
  transform: none;
  box-shadow: var(--sw-shadow-lg);
}

.is-narrow .ws__main {
  margin-left: 0;
}

.ws__backdrop {
  position: fixed;
  inset: 0;
  z-index: calc(var(--z-header) + 5);
  background: rgba(36, 26, 22, 0.4);
}

@media (max-width: 1024px) {
  .topbar {
    padding: 0 16px;
  }

  .ws__content {
    padding: 20px 16px 40px;
  }
}

@media (max-width: 640px) {
  .topbar__search {
    width: 38px;
    padding: 0;
    justify-content: center;
    border: 0;
    background: transparent;
    margin-right: 0;
  }

  .topbar__search-text,
  .topbar__search kbd,
  .topbar__sep,
  .topbar :deep(.crumbs li:not(:last-child)) {
    display: none;
  }
}

/* ---------- 转场 ---------- */
.page-enter-active {
  animation: fadeInUp var(--sw-dur-3) var(--sw-ease);
}

.page-leave-active {
  animation: fadeIn var(--sw-dur-1) var(--sw-ease) reverse;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--sw-dur-2) var(--sw-ease);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ---------- 个人信息弹窗 ---------- */
.profile {
  min-height: 180px;
}

.profile__error {
  padding: 48px 0;
  text-align: center;
  color: var(--sw-ink-3);
}

.profile__head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 18px;
  margin-bottom: 8px;
  border-bottom: 1px solid var(--sw-border-soft);
}

.profile__avatar {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  overflow: hidden;
  border-radius: 50%;
  background: var(--sw-blush-soft);
  color: var(--sw-cocoa);
  font-size: 22px;
  font-weight: 600;
  flex-shrink: 0;
}

.profile__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile__name {
  font-size: 17px;
  font-weight: 600;
}

.profile__job {
  margin-top: 2px;
  font-size: 13px;
  color: var(--sw-ink-3);
}

.profile__fields div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--sw-border-soft);
  font-size: 14px;
}

.profile__fields div:last-child {
  border-bottom: 0;
}

.profile__fields dt {
  color: var(--sw-ink-3);
}

.profile__fields dd {
  font-weight: 500;
  color: var(--sw-ink);
}
</style>
