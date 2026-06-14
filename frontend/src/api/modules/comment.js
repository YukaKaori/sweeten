import http from '@/utils/http'

/** 分页查询甜品评论 */
export const queryByDessertApi = (dessertId, page = 1, pageSize = 10) =>
  http.get(`/comments?dessertId=${dessertId}&page=${page}&pageSize=${pageSize}`)

/** 新增评论 */
export const addApi = (dessertId, content, rating = 5) =>
  http.post('/comments', { dessertId, content, rating })

/** 删除评论 */
export const deleteApi = (id) => http.delete(`/comments/${id}`)
