import http from '@/utils/http'

const blank = (v) => v === '' || v === null || v === undefined

/** 分页查询订单（status 为 0「待支付」时也要带上，不能用 || 判空） */
export const queryPageApi = (orderNo, customerName, status, begin, end, page, pageSize) =>
  http.get('/orders', {
    params: {
      orderNo: orderNo || undefined,
      customerName: customerName || undefined,
      status: blank(status) ? undefined : status,
      begin: begin || undefined,
      end: end || undefined,
      page,
      pageSize,
    },
  })

/** 根据 ID 查询订单详情 */
export const queryByIdApi = (id) => http.get(`/orders/${id}`)

/** 新增订单 */
export const addApi = (order) => http.post('/orders', order)

/** 修改订单 */
export const updateApi = (order) => http.put('/orders', order)

/** 订单状态流转（后端校验流转规则；取消时回补库存） */
export const updateStatusApi = (id, status) =>
  http.put('/orders/status', null, { params: { id, status } })

/** 删除订单 */
export const deleteByIdApi = (id) => http.delete(`/orders?id=${id}`)
