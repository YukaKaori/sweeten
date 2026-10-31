import { nextTick } from 'vue'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/stores/modules/user'
import { useMotionPref } from '@/composables/useMotionPref'
import { pageTitle } from '@/config/brand'
import { firstAllowedPath } from '@/config/navigation'

NProgress.configure({ showSpinner: false, speed: 300, minimum: 0.12 })

/**
 * View Transitions API 特性检测（渐进增强，零 polyfill）。
 * 不支持的浏览器直接正常导航；layout 据此决定是否关闭内层 Vue <transition>（避免双重转场）。
 */
export const supportsViewTransitions =
  typeof document !== 'undefined' && typeof document.startViewTransition === 'function'

// 顾客商城（/m/*）有自己的页面切换，不使用 View Transitions
const isStore = (path) => path === '/m' || path.startsWith('/m/')

function shouldUseViewTransition(to, from) {
  if (!supportsViewTransitions) return false
  if (from.matched.length === 0) return false // 首次加载没有旧页面可转场
  const { isStatic } = useMotionPref()
  if (isStatic.value) return false // prefers-reduced-motion
  if (isStore(to.path) || isStore(from.path)) return false
  if (to.path === from.path) return false
  if (document.hidden) return false
  return true
}

/** 路由要求的权限（取最深一级声明的 meta.permission） */
function requiredPermission(route) {
  for (let i = route.matched.length - 1; i >= 0; i--) {
    const p = route.matched[i].meta?.permission
    if (p) return p
  }
  return null
}

/**
 * 设置全局路由守卫
 * @param {import('vue-router').Router} router
 */
export function setupRouterGuards(router) {
  // 进度条延迟出现：快速导航不闪烁，慢加载（懒加载 chunk）才显示
  let npTimer = null
  const npStartDelayed = () => {
    clearTimeout(npTimer)
    npTimer = setTimeout(() => NProgress.start(), 180)
  }
  const npFinish = () => {
    clearTimeout(npTimer)
    npTimer = null
    NProgress.done()
  }

  let finishViewTransition = null

  router.beforeEach((to) => {
    npStartDelayed()
    document.title = pageTitle(to.meta?.title)

    const userStore = useUserStore()
    userStore.initFromStorage()

    // 未登录：只允许访问公开页面（登录页、顾客商城 /m/*、404）
    const isPublic = to.meta?.public || to.path === '/login' || isStore(to.path)
    if (!userStore.isLoggedIn && !isPublic) {
      return { path: '/login', query: to.fullPath !== '/' && to.fullPath !== '/index' ? { redirect: to.fullPath } : undefined }
    }

    // 已登录：不再进入登录页
    if (userStore.isLoggedIn && to.path === '/login') {
      return firstAllowedPath(userStore.can)
    }

    // 工作台页面：无权限时去第一个有权限的页面（都没有则显示 403 页）
    const permission = requiredPermission(to)
    if (userStore.isLoggedIn && permission && !userStore.can(permission)) {
      const fallback = firstAllowedPath(userStore.can)
      return fallback === to.path ? '/forbidden' : fallback
    }
    return true
  })

  // View Transitions：包裹导航（beforeResolve 是重定向定型后的最后一站）
  router.beforeResolve((to, from) => {
    if (!shouldUseViewTransition(to, from)) return
    return new Promise((resolveNav) => {
      const transition = document.startViewTransition(
        () =>
          new Promise((domUpdated) => {
            finishViewTransition = domUpdated
            resolveNav() // 旧快照已捕获 → 放行导航去更新 DOM
          })
      )
      // 新页面渲染过慢时浏览器会放弃转场并 reject；导航本身不受影响
      transition.ready.catch(() => {})
      transition.finished.catch(() => {}).finally(() => {
        finishViewTransition = null
      })
    })
  })

  router.afterEach(() => {
    npFinish()
    if (finishViewTransition) {
      const domUpdated = finishViewTransition
      finishViewTransition = null
      nextTick(() => requestAnimationFrame(() => domUpdated()))
    }
  })

  router.onError(() => {
    npFinish()
    if (finishViewTransition) {
      finishViewTransition()
      finishViewTransition = null
    }
  })
}
