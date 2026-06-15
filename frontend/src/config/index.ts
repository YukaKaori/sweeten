/**
 * 应用配置 — 统一读取环境变量（TypeScript）
 */

export interface AppConfig {
  apiBaseUrl: string
  apiProxyTarget: string
  appTitle: string
  appEnv: string
  isDev: boolean
  isProd: boolean
}

export const config: AppConfig = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '/api',
  apiProxyTarget: import.meta.env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
  appTitle: import.meta.env.VITE_APP_TITLE || 'DessertShop',
  appEnv: import.meta.env.VITE_APP_ENV || 'development',
  isDev: import.meta.env.DEV,
  isProd: import.meta.env.PROD,
}
