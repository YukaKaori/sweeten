import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi } from '@/api/modules/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userInfo = ref(null)

  // 初始化：从 localStorage 恢复
  const initFromStorage = () => {
    try {
      const stored = JSON.parse(localStorage.getItem('loginUser') || '{}')
      if (stored && stored.token) {
        token.value = stored.token
        userInfo.value = stored
      }
    } catch {
      token.value = ''
      userInfo.value = null
    }
  }

  // 登录
  const login = async (loginForm) => {
    const result = await loginApi(loginForm)
    if (result.code) {
      token.value = result.data.token
      userInfo.value = result.data
      localStorage.setItem('loginUser', JSON.stringify(result.data))
      return { success: true, data: result.data }
    }
    return { success: false, msg: result.msg }
  }

  // 登出
  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('loginUser')
  }

  // 是否已登录（响应式 computed）
  const isLoggedIn = computed(() => !!token.value)

  // 用户名（响应式 computed）
  const username = computed(() => userInfo.value?.username || '')
  const displayName = computed(() => userInfo.value?.name || userInfo.value?.username || '')

  // 角色码 / 权限码（登录响应下发，见后端 LoginInfo）
  const roles = computed(() => (Array.isArray(userInfo.value?.roles) ? userInfo.value.roles : []))
  const permissions = computed(() => (Array.isArray(userInfo.value?.permissions) ? userInfo.value.permissions : null))

  /**
   * 界面权限判断（只决定显示/隐藏，真正的鉴权在后端 @RequirePermission）
   * @param {string|string[]} [code] 权限码；数组表示满足任意一个即可；不传视为无需权限
   * 登录信息里没有 permissions 字段（RBAC 上线前的旧会话）时不在前端拦截，交由后端判定。
   */
  const can = (code) => {
    if (!code || (Array.isArray(code) && code.length === 0)) return true
    const owned = permissions.value
    if (!owned) return true
    const codes = Array.isArray(code) ? code : [code]
    return codes.some((c) => owned.includes(c))
  }

  return {
    token,
    userInfo,
    initFromStorage,
    login,
    logout,
    isLoggedIn,
    username,
    displayName,
    roles,
    permissions,
    can,
  }
})
