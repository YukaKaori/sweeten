/**
 * 商品图片来源 — 全站唯一入口
 *
 * 当前阶段所有甜品临时统一使用同一张草莓蛋糕图。切换方式只需改环境变量（.env），无需改组件：
 *
 *   VITE_PRODUCT_IMAGE_URL=<url>     统一图片地址（当前为已上传并校验过的阿里云 OSS 地址）
 *   VITE_PRODUCT_IMAGE_URL=          留空 → 使用仓库内置的本地图（src/assets/images/strawberrycake.jpg）
 *   VITE_PRODUCT_IMAGE_UNIFORM=false 恢复"各商品显示自己的 image 字段"，缺图时回退到上面的统一图
 *
 * 组件里统一使用 <ProductImg :item :width />（components/ProductImg.vue），或 productImage(item, width)，
 * 不要直接读 item.image。
 *
 * 加载性能（OSS 图）：按显示宽度让 OSS 缩放并转 webp（2MB 原图 → 2~40KB）、srcset 提供 1x/2x、
 * 首屏外图片懒加载、启动时预连接图床域名；新上传的对象带 Cache-Control: immutable（见后端 AliyunOSSOperator）。
 */
import localStrawberryCake from '@/assets/images/strawberrycake.jpg'

const configuredUrl = (import.meta.env.VITE_PRODUCT_IMAGE_URL || '').trim()
const uniform = String(import.meta.env.VITE_PRODUCT_IMAGE_UNIFORM ?? 'true').trim() !== 'false'

/** 本地内置图（OSS 不可用时的兜底，也用于登录页主视觉） */
export const LOCAL_PRODUCT_IMAGE = localStrawberryCake

/** 统一商品图：配置了 URL 用 URL，否则用本地图 */
export const DEFAULT_PRODUCT_IMAGE = configuredUrl || LOCAL_PRODUCT_IMAGE

const OSS_HOST = /\.aliyuncs\.com$/i
/** 源图宽度上限（当前草莓蛋糕原图 1536px），超过就不再请求更大的缩放图 */
const MAX_WIDTH = 1536

/**
 * 阿里云 OSS 图片按显示尺寸缩放并转 webp（OSS 图片处理参数，原图 2MB → 缩略图几十 KB）。
 * 非 OSS 地址、已带查询参数的地址原样返回。
 */
function sized(url, width) {
  if (!width || !url || url.includes('?')) return url
  try {
    const { hostname } = new URL(url, window.location.origin)
    if (!OSS_HOST.test(hostname)) return url
  } catch {
    return url
  }
  const w = Math.min(Math.round(width), MAX_WIDTH)
  return `${url}?x-oss-process=image/resize,w_${w}/quality,q_82/format,webp`
}

/** 是否为可缩放的 OSS 地址 */
function isOss(url) {
  try {
    return OSS_HOST.test(new URL(url, window.location.origin).hostname)
  } catch {
    return false
  }
}

/**
 * 解析商品展示图
 * @param {{ image?: string }} [item] 商品对象
 * @param {number} [width] 期望显示宽度（CSS 像素 × 设备像素比），用于 OSS 缩放
 */
export function productImage(item, width) {
  const own = (item?.image || '').trim()
  const url = uniform || !own ? DEFAULT_PRODUCT_IMAGE : own
  return sized(url, width)
}

/**
 * 解析商品图的 src + srcset（1x / 2x）。非 OSS 地址没有可用的缩放版本，srcset 为空。
 * @param {{ image?: string }} [item]
 * @param {number} width 显示宽度（CSS 像素）
 */
export function productImageSet(item, width) {
  const src = productImage(item, width)
  const base = productImage(item)
  if (!width || !isOss(base) || base.includes('?')) return { src, srcset: undefined }
  return { src, srcset: `${src} 1x, ${sized(base, width * 2)} 2x` }
}

/** <img @error> 兜底：远程图加载失败时换成本地图（只换一次，避免死循环）。须同时去掉 srcset，否则浏览器仍按 srcset 取远程图 */
export function onProductImageError(event) {
  const img = event?.target
  if (!img || img.dataset.fallback) return
  img.dataset.fallback = '1'
  img.removeAttribute('srcset')
  img.src = LOCAL_PRODUCT_IMAGE
}

/** 启动时预连接图床域名：提前完成 DNS/TCP/TLS 握手，首张商品图不必再等握手 */
export function preconnectImageOrigin() {
  if (typeof document === 'undefined' || !isOss(DEFAULT_PRODUCT_IMAGE)) return
  const origin = new URL(DEFAULT_PRODUCT_IMAGE).origin
  if (document.querySelector(`link[rel="preconnect"][href="${origin}"]`)) return
  for (const rel of ['preconnect', 'dns-prefetch']) {
    const link = document.createElement('link')
    link.rel = rel
    link.href = origin
    document.head.appendChild(link)
  }
}
