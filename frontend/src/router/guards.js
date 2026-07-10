import { nextTick } from 'vue'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/stores/modules/user'
import { useMotionPref } from '@/composables/useMotionPref'

NProgress.configure({ showSpinner: false, speed: 400 })

/**
 * View Transitions API 特性检测（渐进增强，零 polyfill）。
 * 不支持的浏览器（部分 Firefox / 旧 Safari）直接正常导航。
 * layout 也据此决定是否关闭内层 Vue <transition>（避免双重转场）。
 */
export const supportsViewTransitions =
  typeof document !== 'undefined' && typeof document.startViewTransition === 'function'

// 移动端 H5 点餐页（/m/*）保持轻量，不加转场
const isMobileH5 = (path) => path.startsWith('/m')

function shouldUseViewTransition(to, from) {
  if (!supportsViewTransitions) return false
  // 初始导航（硬加载/刷新）：没有旧页面可转场，跑 VT 只会白冻结一帧首屏
  if (from.matched.length === 0) return false
  // static 档（prefers-reduced-motion）：跳过 startViewTransition 直接导航
  const { isStatic } = useMotionPref()
  if (isStatic.value) return false
  if (isMobileH5(to.path) || isMobileH5(from.path)) return false
  // 同路径（仅 query 变化）或后台标签页不做转场
  if (to.path === from.path) return false
  if (document.hidden) return false
  return true
}

/**
 * 设置全局路由守卫
 * @param {import('vue-router').Router} router
 */
export function setupRouterGuards(router) {
  // NProgress 延迟启动：快速导航（< 180ms）只看转场不闪进度条，
  // 慢请求（懒加载 chunk / 接口阻塞）才显示进度条 → 与 View Transitions 无视觉冲突
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

  // 当前转场的「DOM 已更新」回调：afterEach 里等新页面渲染完成后 resolve，
  // 浏览器随即捕获 new 快照并播放 ::view-transition-* 动画
  let finishViewTransition = null

  router.beforeEach((to, from, next) => {
    npStartDelayed()

    // 设置页面标题
    const title = to.meta.title ? `${to.meta.title} - DessertShop` : 'DessertShop'
    document.title = title

    // 检查登录状态
    const userStore = useUserStore()
    userStore.initFromStorage()

    // 未登录 → 只允许访问公开页面（/login 和 /m/* 移动端点餐页）
    const isPublic = to.meta?.public || to.path === '/login' || to.path.startsWith('/m')
    if (!userStore.isLoggedIn && !isPublic) {
      next('/login')
      return
    }

    // 已登录 → 不允许再访问 /login
    if (userStore.isLoggedIn && to.path === '/login') {
      next('/')
      return
    }

    next()
  })

  // View Transitions：包裹导航（beforeResolve 是重定向都定型后的最后一站）
  router.beforeResolve((to, from) => {
    if (!shouldUseViewTransition(to, from)) return
    // vt-active 期间关闭 glass-pill 自身的 CSS 过渡，
    // 让高亮块的位移完全交给 ::view-transition-group(nav-pill) 形变
    document.documentElement.classList.add('vt-active')
    return new Promise((resolveNav) => {
      const transition = document.startViewTransition(
        () =>
          new Promise((domUpdated) => {
            finishViewTransition = domUpdated
            resolveNav() // 旧快照已捕获 → 放行导航去更新 DOM
          })
      )
      transition.finished.finally(() => {
        document.documentElement.classList.remove('vt-active')
        finishViewTransition = null
      })
    })
  })

  router.afterEach(() => {
    npFinish()
    if (finishViewTransition) {
      const domUpdated = finishViewTransition
      finishViewTransition = null
      // 等 Vue 完成组件切换 + 一帧绘制，再让浏览器捕获 new 快照
      nextTick(() => requestAnimationFrame(() => domUpdated()))
    }
  })

  router.onError(() => {
    npFinish()
    if (finishViewTransition) {
      finishViewTransition()
      finishViewTransition = null
    }
    document.documentElement.classList.remove('vt-active')
  })
}
