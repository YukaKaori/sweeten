import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useUserStore } from '../stores/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 600000
})

// 请求拦截器：自动注入 token
request.interceptors.request.use(
  (config) => {
    try {
      const loginUser = JSON.parse(localStorage.getItem('loginUser') || '{}')
      if (loginUser && loginUser.token) {
        config.headers.token = loginUser.token
      }
    } catch {
      // localStorage 解析失败，忽略
    }
    return config
  }
)

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        ElMessage.error('登录已过期，请重新登录')
        // 同步清理 store 内存状态 + localStorage，确保 router guard 检测到未登录
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      } else if (status === 403) {
        ElMessage.error('没有权限执行此操作')
      } else if (status === 500) {
        ElMessage.error('服务器内部错误，请稍后重试')
      } else {
        ElMessage.error('接口访问异常')
      }
    } else {
      ElMessage.error('网络连接异常，请检查网络')
    }
    return Promise.reject(error)
  }
)

export default request
