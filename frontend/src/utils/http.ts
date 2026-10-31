import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { config } from '@/config'
import { resolveMockData } from '@/mock'
import type { ApiResponse } from '@/types/api'

const http = axios.create({
  baseURL: config.apiBaseUrl,
  timeout: 30000,
})

// 请求拦截器：自动注入 token
http.interceptors.request.use(
  (cfg: InternalAxiosRequestConfig) => {
    try {
      const loginUser = JSON.parse(localStorage.getItem('loginUser') || '{}')
      if (loginUser?.token) {
        cfg.headers.token = loginUser.token
      }
    } catch {
      // localStorage 解析失败，忽略
    }
    return cfg
  },
  (error: AxiosError) => Promise.reject(error),
)

// 响应拦截器：统一错误处理
http.interceptors.response.use(
  (response) => response.data,
  (error: AxiosError) => {
    if (error.response) {
      const { status } = error.response as { status: number }
      const messages: Record<number, string> = {
        400: '请求参数错误',
        401: '登录已过期，请重新登录',
        403: '没有权限执行此操作',
        404: '请求的资源不存在',
        500: '服务器内部错误，请稍后重试',
        502: '网关错误',
        503: '服务不可用',
      }
      ElMessage.error(messages[status] || '接口访问异常')

      if (status === 401) {
        localStorage.removeItem('loginUser')
        window.location.href = '/login'
      }
    } else {
      // 网络不可达时尝试 mock 降级
      const mockData = resolveMockData(error.config?.url || '')
      if (mockData) {
        if (config.isDev) {
          console.info(`[Mock] ${error.config?.url} → 使用本地 mock 数据`)
        }
        return mockData as ApiResponse
      }
      ElMessage.error('网络连接异常，请检查网络')
    }
    return Promise.reject(error)
  },
)

export default http
