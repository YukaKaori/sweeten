import http from '@/utils/http'

/** 分页查询客户 */
export const queryPageApi = (name = '', phone = '', page = 1, pageSize = 10) =>
  http.get(`/customers?name=${name}&phone=${phone}&page=${page}&pageSize=${pageSize}`)

/** 根据 ID 查询客户详情 */
export const queryByIdApi = (id) => http.get(`/customers/${id}`)

/** 查询客户订单历史 */
export const queryOrdersApi = (customerId, page = 1, pageSize = 10) =>
  http.get(`/customers/${customerId}/orders?page=${page}&pageSize=${pageSize}`)

/** 新增客户 */
export const addApi = (customer) => http.post('/customers', customer)

/** 修改客户 */
export const updateApi = (customer) => http.put('/customers', customer)

/** 删除客户 */
export const deleteApi = (id) => http.delete(`/customers/${id}`)
