import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 购物车 Store
 * 数据结构：{ id, name, price, originalPrice, icon, image, stock, qty }
 * stock 为加入时/最近一次同步时的库存，用于前端限购；最终以下单时后端扣减结果为准。
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

  /** 某商品还能再加几件（未知库存时不限制） */
  function remaining(product) {
    const stock = product.stock
    if (stock === null || stock === undefined) return Infinity
    return Math.max(stock - getQty(product.id), 0)
  }

  /**
   * 加入购物车（已有则 +qty，没有则添加）
   * @returns {boolean} 库存不足时返回 false，不做任何修改
   */
  function addItem(product, qty = 1) {
    if (remaining(product) < qty) return false
    const existing = items.value.find((i) => i.id === product.id)
    if (existing) {
      existing.qty += qty
      if (product.stock !== undefined) existing.stock = product.stock
    } else {
      items.value.push({
        id: product.id,
        name: product.name,
        price: product.price,
        originalPrice: product.originalPrice,
        icon: product.icon || '🍰',
        image: product.image || null,
        stock: product.stock ?? null,
        qty,
      })
    }
    persist()
    return true
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

  /** 直接设置数量（超过库存时截到库存上限） */
  function setQty(id, qty) {
    const item = items.value.find((i) => i.id === id)
    if (!item) return
    if (qty <= 0) {
      removeItem(id)
    } else {
      item.qty = item.stock != null ? Math.min(qty, item.stock) : qty
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

  /**
   * 用最新在售商品列表校正购物车：同步名称/价格/库存，移除已下架或售罄商品，数量超库存时截断。
   * 购物车存在 localStorage，价格可能已过期；结算前调用可避免后端以"价格已更新"拒单。
   * @param {Array} products 后端 /mobile/products?category=all 返回的在售商品
   * @returns {{ removed: string[], adjusted: string[], repriced: string[] }}
   */
  function syncWithProducts(products) {
    const byId = new Map(products.map((p) => [p.id, p]))
    const removed = []
    const adjusted = []
    const repriced = []
    items.value = items.value.filter((item) => {
      const p = byId.get(item.id)
      if (!p || p.stock === 0) {
        removed.push(item.name)
        return false
      }
      if (p.price !== item.price) repriced.push(p.name)
      item.name = p.name
      item.price = p.price
      item.originalPrice = p.originalPrice
      item.image = p.image || null
      item.icon = p.icon || item.icon
      item.stock = p.stock ?? null
      if (item.stock != null && item.qty > item.stock) {
        item.qty = item.stock
        adjusted.push(p.name)
      }
      return true
    })
    persist()
    return { removed, adjusted, repriced }
  }

  // ─── 持久化（localStorage） ─────────────────────────────────
  function persist() {
    try {
      localStorage.setItem('cart_items', JSON.stringify(items.value))
    } catch { /* ignore */ }
  }

  function loadFromStorage() {
    try {
      const list = JSON.parse(localStorage.getItem('cart_items') || '[]')
      return Array.isArray(list) ? list.filter((i) => i && i.id != null && i.qty > 0) : []
    } catch {
      return []
    }
  }

  return {
    items,
    totalQty,
    totalAmount,
    isEmpty,
    remaining,
    addItem,
    decreaseItem,
    setQty,
    removeItem,
    clear,
    getQty,
    syncWithProducts,
  }
})
