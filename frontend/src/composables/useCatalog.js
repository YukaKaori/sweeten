import { ref, computed } from 'vue'
import { getMobileProducts } from '@/api/modules/mobile'
import { DESSERT_CATEGORIES } from '@/utils/constants'

/**
 * 顾客商城的在售商品目录（会话内共享，60 秒内复用）
 *
 * 一次拉取全部在售商品（GET /mobile/products?category=all，后端按销量降序），
 * 分类筛选 / 搜索 / 排序都在前端完成：切换分类即时响应，分类数量也与实际可买的商品一致。
 */
const TTL = 60_000

const products = ref([])
const loading = ref(false)
const error = ref('')
let loadedAt = 0
let pending = null

async function load(force = false) {
  if (!force && products.value.length && Date.now() - loadedAt < TTL) return products.value
  if (pending) return pending
  loading.value = true
  error.value = ''
  pending = getMobileProducts('all', { silent: true })
    .then((r) => {
      if (r?.code === 1 && Array.isArray(r.data)) {
        products.value = r.data
        loadedAt = Date.now()
      } else {
        error.value = r?.msg || '甜品加载失败，请稍后重试'
      }
      return products.value
    })
    .catch((e) => {
      error.value = e?.displayMessage || '网络连接异常，请检查网络后重试'
      return products.value
    })
    .finally(() => {
      loading.value = false
      pending = null
    })
  return pending
}

export function useCatalog() {
  /** 有在售商品的分类（含数量） */
  const categories = computed(() =>
    DESSERT_CATEGORIES.map((c) => ({ ...c, count: products.value.filter((p) => p.category === c.value).length })).filter(
      (c) => c.count > 0
    )
  )

  /** 销量最高、仍有库存的商品 */
  const bestsellers = computed(() => products.value.filter((p) => p.stock !== 0).slice(0, 4))

  const byId = (id) => products.value.find((p) => String(p.id) === String(id))

  const categoryLabel = (value) => DESSERT_CATEGORIES.find((c) => c.value === value)?.label || ''

  return { products, loading, error, load, categories, bestsellers, byId, categoryLabel }
}
