// 向后兼容 — 推荐使用 '@/api/modules/dept'
// 注意：原 querryAllApi 拼写已修正为 queryAllApi
export {
  queryAllApi,
  addApi,
  queryByIdApi,
  updateApi,
  deleteByIdApi,
} from './modules/dept'

// 旧名称兼容导出（拼写错误版本，逐步淘汰）
import { queryAllApi as _queryAllApi, queryByIdApi as _queryByIdApi } from './modules/dept'
/** @deprecated 使用 queryAllApi 代替 */
export const querryAllApi = _queryAllApi
/** @deprecated 使用 queryByIdApi 代替 */
export const querryByIdApi = _queryByIdApi
