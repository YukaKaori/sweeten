import http from '@/utils/http'

/** 登录 */
export const loginApi = (loginForm) => http.post('/login', loginForm)
