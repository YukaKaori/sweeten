import http from '@/utils/http'

/** 分页查询员工 */
export const queryPageApi = (name, gender, begin, end, page, pageSize) =>
  http.get(`/emps?name=${name}&gender=${gender}&begin=${begin}&end=${end}&page=${page}&pageSize=${pageSize}`)

/** 新增员工 */
export const addApi = (emp) => http.post('/emps', emp)

/** 根据 ID 查询员工 */
export const queryByIdApi = (id) => http.get(`/emps/${id}`)

/** 修改员工 */
export const updateApi = (emp) => http.put('/emps', emp)

/** 删除员工（支持批量） */
export const deleteByIdApi = (ids) => http.delete(`/emps?ids=${ids}`)

/** 查询个人信息 */
export const getProfileApi = (id) => http.get(`/emps/profile/${id}`)

/** 修改密码 */
export const updatePasswordApi = (id, oldPassword, newPassword) =>
  http.put(`/emps/password?id=${id}&oldPassword=${oldPassword}&newPassword=${newPassword}`)
