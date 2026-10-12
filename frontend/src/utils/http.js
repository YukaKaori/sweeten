import axios from 'axios'
import { ElMessage } from 'element-plus'
import { config } from '@/config'
import { resolveMockData } from '@/mock'

const http = axios.create({
  baseURL: config.apiBaseUrl,
  timeout: 30000,
})

// 请求拦截器：自动注入 token
http.interceptors.request.use(
  (cfg) => {
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
  (error) => Promise.reject(error)
)

// 响应拦截器：统一错误处理
http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      const messages = {
        400: '请求参数错误',
        401: '登录已过期，请重新登录',
        403: '没有权限执行此操作',
        404: '请求的资源不存在',
        500: '服务器内部错误，请稍后重试',
        502: '网关错误',
        503: '服务不可用',
      }
      // 4xx 优先展示后端给出的具体原因（如"库存不足"、"名称已存在"、字段校验信息）；5xx 不透出内部信息
      const serverMsg = data && typeof data === 'object' && typeof data.msg === 'string' ? data.msg : ''
      const useServerMsg = serverMsg && status >= 400 && status < 500 && status !== 401
      // 调用方可通过 error.displayMessage 复用同一文案，避免重复弹窗
      error.displayMessage = useServerMsg ? serverMsg : messages[status] || '接口访问异常'
      if (!error.config?.silent) ElMessage.error(error.displayMessage)

      if (status === 401) {
        localStorage.removeItem('loginUser')
        window.location.href = '/login'
      }
    } else {
      // 网络不可达时的 mock 降级仅限开发环境：生产环境若静默返回假数据，员工会把演示订单当成真实订单处理
      const mockData = config.isDev ? resolveMockData(error.config?.url || '') : null
      if (mockData) {
        console.info(`[Mock] ${error.config?.url} → 使用本地 mock 数据`)
        return mockData
      }
      error.displayMessage = '网络连接异常，请检查网络'
      if (!error.config?.silent) ElMessage.error(error.displayMessage)
    }
    return Promise.reject(error)
  }
)

export default http
