import request from '@/utils/request'

//分页查询订单
export const queryPageApi = (orderNo, customerName, status, begin, end, page, pageSize) =>
  request.get(`/orders?orderNo=${orderNo || ''}&customerName=${customerName || ''}&status=${status || ''}&begin=${begin || ''}&end=${end || ''}&page=${page}&pageSize=${pageSize}`);

//根据id查询订单详情
export const queryByIdApi = (id) => request.get(`/orders/${id}`);

//新增订单
export const addApi = (order) => request.post('/orders', order);

//修改订单
export const updateApi = (order) => request.put('/orders', order);

//删除订单
export const deleteByIdApi = (id) => request.delete(`/orders?id=${id}`);
