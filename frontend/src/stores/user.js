import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi } from '@/api/login'

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

  return {
    token,
    userInfo,
    initFromStorage,
    login,
    logout,
    isLoggedIn,
    username
  }
})
