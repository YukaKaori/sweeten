import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi } from '@/api/modules/auth'
import type { ApiResponse } from '@/types/api'

interface LoginForm {
  username: string
  password: string
}

interface UserInfo {
  token: string
  username: string
  name?: string
}

interface LoginResult {
  success: boolean
  data?: UserInfo
  msg?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userInfo = ref<UserInfo | null>(null)

  // 初始化：从 localStorage 恢复
  const initFromStorage = (): void => {
    try {
      const stored = JSON.parse(localStorage.getItem('loginUser') || '{}') as Partial<UserInfo>
      if (stored && stored.token) {
        token.value = stored.token
        userInfo.value = stored as UserInfo
      }
    } catch {
      token.value = ''
      userInfo.value = null
    }
  }

  // 登录
  const login = async (loginForm: LoginForm): Promise<LoginResult> => {
    const result = await loginApi(loginForm) as ApiResponse<UserInfo>
    if (result.code) {
      token.value = result.data.token
      userInfo.value = result.data
      localStorage.setItem('loginUser', JSON.stringify(result.data))
      return { success: true, data: result.data }
    }
    return { success: false, msg: result.msg }
  }

  // 登出
  const logout = (): void => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('loginUser')
  }

  // 是否已登录
  const isLoggedIn = computed<boolean>(() => !!token.value)

  // 用户名
  const username = computed<string>(() => userInfo.value?.username || '')

  return {
    token,
    userInfo,
    initFromStorage,
    login,
    logout,
    isLoggedIn,
    username,
  }
})
