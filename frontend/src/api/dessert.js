import request from '@/utils/request'

//分页查询甜品
export const queryPageApi = (name, category, page, pageSize) =>
  request.get(`/desserts?name=${name || ''}&category=${category || ''}&page=${page}&pageSize=${pageSize}`);

//根据分类查询甜品列表
export const queryByCategoryApi = (category) => request.get(`/desserts/category/${category}`);

//查询各分类数量
export const queryCategoryCountApi = () => request.get('/desserts/categoryCount');

//根据id查询甜品
export const queryByIdApi = (id) => request.get(`/desserts/${id}`);

//新增甜品
export const addApi = (dessert) => request.post('/desserts', dessert);

//修改甜品
export const updateApi = (dessert) => request.put('/desserts', dessert);

//删除甜品
export const deleteByIdApi = (id) => request.delete(`/desserts?id=${id}`);

//甜品上下架
export const updateStatusApi = (id, status) => request.put(`/desserts/status?id=${id}&status=${status}`);
