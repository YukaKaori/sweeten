/**
 * useToast.js
 * ───────────────────────────────────────────────────────────────
 * 封装 Element Plus 的 ElNotification，统一为「玻璃质感 + 图标微弹」的通知：
 *   - 成功 = 抹茶绿，失败 = 草莓红，警告 = 焦糖，信息 = 蓝莓
 *   - 玻璃底 + 模糊 + 品牌左边条；暗色下边缘为金色发丝
 *   - 视觉样式见 base.css 全局 `.glass-toast`（通知渲染在 body 层，需全局样式）
 *
 * 用法：
 *   import { useToast } from '@/composables/useToast'
 *   const toast = useToast()
 *   toast.success('已生成补货单')
 *   toast.error('提交失败', '请稍后重试')
 */

import { ElNotification } from 'element-plus'

const DEFAULT_TITLE = {
  success: '操作成功',
  error: '出错了',
  warning: '请注意',
  info: '提示',
}

function notify(type, message, title, options = {}) {
  return ElNotification({
    type,
    message,
    title: title ?? DEFAULT_TITLE[type],
    customClass: `glass-toast glass-toast--${type}`,
    duration: 3200,
    offset: 18,
    showClose: true,
    ...options,
  })
}

export function useToast() {
  return {
    success: (message, title, options) => notify('success', message, title, options),
    error: (message, title, options) => notify('error', message, title, options),
    warning: (message, title, options) => notify('warning', message, title, options),
    info: (message, title, options) => notify('info', message, title, options),
    raw: notify,
  }
}
