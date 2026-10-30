import { useUserStore } from '@/stores/modules/user'

/**
 * 界面权限判断（隐藏/禁用当前员工无权执行的操作）
 *
 * 权限码格式「模块:资源:操作」，与后端 sql/rbac_init.sql、@RequirePermission 一致。
 * 前端只负责不展示无权限的入口；后端始终做最终校验（越权请求返回 403）。
 *
 * @example
 * const { can } = usePermission()
 * <el-button v-if="can('dessert:dessert:create')">新增甜品</el-button>
 */
export function usePermission() {
  const userStore = useUserStore()

  /** 满足任意一个权限码即可 */
  const can = (code) => userStore.can(code)

  /** 兼容旧接口 */
  const hasPermission = can

  /** 是否拥有某个角色码（super_admin / manager / cashier / warehouse / finance） */
  const hasRole = (role) => {
    const roles = userStore.roles
    const wanted = Array.isArray(role) ? role : [role]
    return wanted.some((r) => roles.includes(r))
  }

  return { can, hasPermission, hasRole }
}
