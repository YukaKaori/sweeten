import { useUserStore } from '@/stores/modules/user'

/**
 * 权限判断 composable
 *
 * @example
 * const { hasPermission, hasRole } = usePermission()
 * if (hasPermission('system:dept:add')) { ... }
 * if (hasRole('admin')) { ... }
 */
export function usePermission() {
  const userStore = useUserStore()

  /**
   * 判断是否有指定权限
   * @param {string|string[]} permission - 权限标识
   */
  const hasPermission = (permission) => {
    const roles = userStore.userInfo?.roles || []
    if (roles.includes('admin')) return true
    const permissions = Array.isArray(permission) ? permission : [permission]
    return permissions.some((p) => roles.includes(p))
  }

  /**
   * 判断是否有指定角色
   * @param {string|string[]} role - 角色标识
   */
  const hasRole = (role) => {
    const roles = userStore.userInfo?.roles || []
    const roleArr = Array.isArray(role) ? role : [role]
    return roleArr.some((r) => roles.includes(r))
  }

  return { hasPermission, hasRole }
}
