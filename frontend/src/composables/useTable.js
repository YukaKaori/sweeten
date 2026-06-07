import { ref, onMounted, reactive } from 'vue'

/**
 * 通用表格逻辑（分页 + 搜索 + 加载状态）
 *
 * @param {Function} fetchApi - 数据请求函数，接收分页参数，返回 { rows, total }
 * @param {Object} options
 * @param {boolean} [options.immediate=true] - 是否在 onMounted 时自动加载
 * @param {number} [options.defaultPageSize=10] - 默认每页条数
 *
 * @example
 * const {
 *   loading, tableData, total, pagination,
 *   searchParams, fetchData, handleSearch, handleReset,
 *   handlePageChange, handleSizeChange,
 * } = useTable((params) => queryPageApi(params.name, params.page, params.pageSize))
 */
export function useTable(fetchApi, options = {}) {
  const { immediate = true, defaultPageSize = 10 } = options

  const loading = ref(false)
  const tableData = ref([])
  const total = ref(0)
  const pagination = reactive({
    page: 1,
    pageSize: defaultPageSize,
  })
  const searchParams = ref({})

  const fetchData = async () => {
    loading.value = true
    try {
      const result = await fetchApi({
        ...pagination,
        ...searchParams.value,
      })
      if (result?.code) {
        tableData.value = result.data?.rows || result.data || []
        total.value = result.data?.total || 0
      }
    } catch {
      tableData.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  const handleSearch = () => {
    pagination.page = 1
    fetchData()
  }

  const handleReset = () => {
    searchParams.value = {}
    pagination.page = 1
    fetchData()
  }

  const handlePageChange = (page) => {
    pagination.page = page
    fetchData()
  }

  const handleSizeChange = (size) => {
    pagination.pageSize = size
    pagination.page = 1
    fetchData()
  }

  if (immediate) {
    onMounted(() => fetchData())
  }

  return {
    loading,
    tableData,
    total,
    pagination,
    searchParams,
    fetchData,
    handleSearch,
    handleReset,
    handlePageChange,
    handleSizeChange,
  }
}
