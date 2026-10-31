import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 全局应用状态
 * 管理侧边栏、设备类型等 UI 状态
 */
export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const device = ref('desktop') // 'desktop' | 'mobile'

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  const setDevice = (val) => {
    device.value = val
  }

  return {
    sidebarCollapsed,
    device,
    toggleSidebar,
    setDevice,
  }
})
