import http from '@/utils/http'

/** 分页查询订单 */
export const queryPageApi = (orderNo, customerName, status, begin, end, page, pageSize) =>
  http.get(`/orders?orderNo=${orderNo || ''}&customerName=${customerName || ''}&status=${status || ''}&begin=${begin || ''}&end=${end || ''}&page=${page}&pageSize=${pageSize}`)

/** 根据 ID 查询订单详情 */
export const queryByIdApi = (id) => http.get(`/orders/${id}`)

/** 新增订单 */
export const addApi = (order) => http.post('/orders', order)

/** 修改订单 */
export const updateApi = (order) => http.put('/orders', order)

/** 删除订单 */
export const deleteByIdApi = (id) => http.delete(`/orders?id=${id}`)
