import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 顾客商城的界面状态：购物车面板开关 + 轻提示
 * （取代旧的 window.__openCartSheet / window.__showToast 全局函数）
 */
export const useStoreUi = defineStore('storeUi', () => {
  const cartOpen = ref(false)
  const toast = ref(null) // { id, text, tone }
  let toastTimer = null
  let seq = 0

  const openCart = () => {
    cartOpen.value = true
  }

  const closeCart = () => {
    cartOpen.value = false
  }

  /**
   * @param {string} text
   * @param {'default'|'success'|'warning'} [tone]
   */
  const showToast = (text, tone = 'default') => {
    clearTimeout(toastTimer)
    toast.value = { id: ++seq, text, tone }
    toastTimer = setTimeout(() => { toast.value = null }, 2400)
  }

  return { cartOpen, toast, openCart, closeCart, showToast }
})
