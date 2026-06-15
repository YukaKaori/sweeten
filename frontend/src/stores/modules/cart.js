import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 购物车 Store
 * 数据结构：{ id, name, price, originalPrice, icon, image, qty }
 */
export const useCartStore = defineStore('cart', () => {
  const items = ref(loadFromStorage())

  // ─── Getters ───────────────────────────────────────────────
  const totalQty = computed(() =>
    items.value.reduce((sum, i) => sum + i.qty, 0)
  )

  const totalAmount = computed(() =>
    parseFloat(items.value.reduce((sum, i) => sum + i.price * i.qty, 0).toFixed(2))
  )

  const isEmpty = computed(() => items.value.length === 0)

  // ─── Actions ───────────────────────────────────────────────

  /** 加入购物车（已有则 +1，没有则添加） */
  function addItem(product) {
    const existing = items.value.find((i) => i.id === product.id)
    if (existing) {
      existing.qty++
    } else {
      items.value.push({
        id: product.id,
        name: product.name,
        price: product.price,
        originalPrice: product.originalPrice,
        icon: product.icon || '🍰',
        image: product.image || null,
        qty: 1,
      })
    }
    persist()
  }

  /** 减少数量（到 0 则移除） */
  function decreaseItem(id) {
    const idx = items.value.findIndex((i) => i.id === id)
    if (idx === -1) return
    if (items.value[idx].qty > 1) {
      items.value[idx].qty--
    } else {
      items.value.splice(idx, 1)
    }
    persist()
  }

  /** 直接设置数量 */
  function setQty(id, qty) {
    const item = items.value.find((i) => i.id === id)
    if (!item) return
    if (qty <= 0) {
      removeItem(id)
    } else {
      item.qty = qty
      persist()
    }
  }

  /** 移除商品 */
  function removeItem(id) {
    const idx = items.value.findIndex((i) => i.id === id)
    if (idx !== -1) {
      items.value.splice(idx, 1)
      persist()
    }
  }

  /** 清空购物车 */
  function clear() {
    items.value = []
    persist()
  }

  /** 获取某商品数量（用于商品列表显示步进器） */
  function getQty(id) {
    return items.value.find((i) => i.id === id)?.qty ?? 0
  }

  // ─── 持久化（localStorage） ─────────────────────────────────
  function persist() {
    try {
      localStorage.setItem('cart_items', JSON.stringify(items.value))
    } catch { /* ignore */ }
  }

  function loadFromStorage() {
    try {
      return JSON.parse(localStorage.getItem('cart_items') || '[]')
    } catch {
      return []
    }
  }

  return {
    items,
    totalQty,
    totalAmount,
    isEmpty,
    addItem,
    decreaseItem,
    setQty,
    removeItem,
    clear,
    getQty,
  }
})
