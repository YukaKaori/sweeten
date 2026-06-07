import http from '@/utils/http'

/** 分页查询甜品 */
export const queryPageApi = (name, category, page, pageSize) =>
  http.get(`/desserts?name=${name || ''}&category=${category || ''}&page=${page}&pageSize=${pageSize}`)

/** 根据分类查询甜品列表 */
export const queryByCategoryApi = (category) => http.get(`/desserts/category/${category}`)

/** 查询各分类数量 */
export const queryCategoryCountApi = () => http.get('/desserts/categoryCount')

/** 根据 ID 查询甜品 */
export const queryByIdApi = (id) => http.get(`/desserts/${id}`)

/** 新增甜品 */
export const addApi = (dessert) => http.post('/desserts', dessert)

/** 修改甜品 */
export const updateApi = (dessert) => http.put('/desserts', dessert)

/** 删除甜品 */
export const deleteByIdApi = (id) => http.delete(`/desserts?id=${id}`)

/** 甜品上下架 */
export const updateStatusApi = (id, status) =>
  http.put(`/desserts/status?id=${id}&status=${status}`)
