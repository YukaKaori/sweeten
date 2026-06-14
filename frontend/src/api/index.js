/**
 * API 统一导出
 * 使用方式：
 *   import { authApi, deptApi } from '@/api'
 *   // 或者按模块导入
 *   import { loginApi } from '@/api/modules/auth'
 */

// 按模块整体导出
export * as authApi from './modules/auth'
export * as dashboardApi from './modules/dashboard'
export * as deptApi from './modules/dept'
export * as dessertApi from './modules/dessert'
export * as empApi from './modules/emp'
export * as inventoryApi from './modules/inventory'
export * as logApi from './modules/log'
export * as commentApi from './modules/comment'
export * as customerApi from './modules/customer'
export * as orderApi from './modules/order'
