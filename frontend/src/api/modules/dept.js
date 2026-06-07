import http from '@/utils/http'

/** 查询全部部门 */
export const queryAllApi = () => http.get('/depts')

/** 新增部门 */
export const addApi = (dept) => http.post('/depts', dept)

/** 根据 ID 查询部门 */
export const queryByIdApi = (id) => http.get(`/depts/${id}`)

/** 修改部门 */
export const updateApi = (dept) => http.put('/depts', dept)

/** 删除部门 */
export const deleteByIdApi = (id) => http.delete(`/depts?id=${id}`)
