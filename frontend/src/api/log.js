import request from '@/utils/request'

// 分页查询操作日志（operate_log 表）
export const queryOperateLogApi = (page, pageSize, className, methodName) =>
  request.get(`/operateLogs?page=${page}&pageSize=${pageSize}&className=${className || ''}&methodName=${methodName || ''}`)

// 根据 id 查询操作日志详情
export const queryOperateLogByIdApi = (id) =>
  request.get(`/operateLogs/${id}`)

// 分页查询员工日志（emp_log 表）
export const queryEmpLogApi = (page, pageSize) =>
  request.get(`/empLogs?page=${page}&pageSize=${pageSize}`)
