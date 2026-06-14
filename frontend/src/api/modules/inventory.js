import http from '@/utils/http'

/** 分页查询库存 */
export const queryPageApi = (name = '', page = 1, pageSize = 10) =>
  http.get(`/inventory?name=${name}&page=${page}&pageSize=${pageSize}`)

/** 查询低库存预警 */
export const queryAlertsApi = () => http.get('/inventory/alerts')

/** 新增库存物料 */
export const addApi = (item) => http.post('/inventory', item)

/** 更新库存物料 */
export const updateApi = (item) => http.put('/inventory', item)

/** 删除库存物料 */
export const deleteApi = (id) => http.delete(`/inventory/${id}`)

/** 调整库存数量（入库/出库） */
export const adjustStockApi = (id, quantity, remark = '') =>
  http.put('/inventory/stock', { id, quantity, remark })
