import request from '@/utils/request'

//查询
export const queryPageApi = (name,gender,begin,end,page,pageSize) => 
  request.get(`/emps?name=${name}&gender=${gender}&begin=${begin}&end=${end}&page=${page}&pageSize=${pageSize}`);


//新增
export const addApi = (emp) => request.post('/emps',emp);


//根据id查询部门
export const queryByIdApi = (id) => request.get(`/emps/${id}`);


//修改
export const updateApi = (emp) => request.put(`/emps`,emp);


//删除
export const deleteByIdApi = (ids) => request.delete(`/emps?ids=${ids}`);

//查询个人信息
export const getProfileApi = (id) => request.get(`/emps/profile/${id}`);

//修改密码
export const updatePasswordApi = (id, oldPassword, newPassword) =>
  request.put(`/emps/password?id=${id}&oldPassword=${oldPassword}&newPassword=${newPassword}`);
