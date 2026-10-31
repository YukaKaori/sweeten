import http from '@/utils/http'

/** 分页查询操作日志 */
export const queryOperateLogApi = (page, pageSize, className, methodName) =>
  http.get(`/operateLogs?page=${page}&pageSize=${pageSize}&className=${className || ''}&methodName=${methodName || ''}`)

/** 根据 ID 查询操作日志详情 */
export const queryOperateLogByIdApi = (id) => http.get(`/operateLogs/${id}`)

/** 分页查询员工日志 */
export const queryEmpLogApi = (page, pageSize) =>
  http.get(`/empLogs?page=${page}&pageSize=${pageSize}`)
