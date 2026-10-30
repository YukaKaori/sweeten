/**
 * 商品图片来源 — 全站唯一入口
 *
 * 当前阶段所有甜品临时统一使用同一张草莓蛋糕图。切换方式只需改环境变量（.env），无需改组件：
 *
 *   VITE_PRODUCT_IMAGE_URL=<url>       统一图片地址（当前为已上传并校验过的阿里云 OSS 地址）
 *   VITE_PRODUCT_IMAGE_URL=            留空 → 使用仓库内置的本地图（src/assets/images/strawberrycake.jpg）
 *   VITE_PRODUCT_IMAGE_UNIFORM=false   恢复"各商品显示自己的 image 字段"，缺图时回退到上面的统一图
 *   VITE_PRODUCT_IMAGE_FOCUS=50% 55%   统一图的取景焦点（object-position），裁成方图/竖图时主体不被切掉
 *
 * 组件里统一使用 <ProductImg>（components/ProductImg.vue），不要直接读 item.image：
 *   - 固定尺寸（缩略图）：<ProductImg :item :width="48" />            → 1x / 2x
 *   - 响应式（卡片、详情大图）：<ProductImg :item sizes="(min-width: 1024px) 25vw, 50vw" ratio="4/5" />
 *   - 后台编辑预览要看到「刚上传的那张」：加 own（无视统一模式）
 *
 * 以后接入真实商品摄影：把 VITE_PRODUCT_IMAGE_UNIFORM 设为 false，并在后台为每个甜品上传图片即可，组件无需改动。
 *
 * 加载性能（OSS 图）：按显示宽度让 OSS 缩放并转 webp（2MB 原图 → 十几到几十 KB），宽度归入固定档位
 * 以提高缓存命中；首屏外图片懒加载；启动时预连接图床域名；新上传对象带 Cache-Control: immutable（见后端 AliyunOSSOperator）。
 */
import localStrawberryCake from '@/assets/images/strawberrycake.jpg'

const configuredUrl = (import.meta.env.VITE_PRODUCT_IMAGE_URL || '').trim()
const uniform = String(import.meta.env.VITE_PRODUCT_IMAGE_UNIFORM ?? 'true').trim() !== 'false'

/** 本地内置图（OSS 不可用时的兜底，也用于登录页与商城首页主视觉） */
export const LOCAL_PRODUCT_IMAGE = localStrawberryCake

/** 统一商品图：配置了 URL 用 URL，否则用本地图 */
export const DEFAULT_PRODUCT_IMAGE = configuredUrl || LOCAL_PRODUCT_IMAGE

/**
 * 品牌主视觉（商城首页、登录页）：使用打包进来的本地招牌甜品图。
 * 主视觉是首屏最大元素，本地图不依赖图床握手，图床不可达时也能完整显示；
 * 它代表品牌，不对应某一件商品，所以不随「各商品自有图」切换。
 */
export const BRAND_HERO_IMAGE = LOCAL_PRODUCT_IMAGE

/** 是否处于「全站统一图」模式 */
export const UNIFORM_PRODUCT_IMAGE = uniform

/** 统一图的取景焦点（object-position） */
export const PRODUCT_IMAGE_FOCUS = (import.meta.env.VITE_PRODUCT_IMAGE_FOCUS || '50% 55%').trim()

const OSS_HOST = /\.aliyuncs\.com$/i
/** 源图宽度上限（当前草莓蛋糕原图 1536px），超过就不再请求更大的缩放图 */
const MAX_WIDTH = 1536
/** 响应式图片的宽度档位（同一档位的缩放图可被浏览器 / CDN 复用） */
const WIDTH_STEPS = [160, 240, 320, 480, 640, 960, 1280, 1536]

function isOss(url) {
  try {
    return OSS_HOST.test(new URL(url, window.location.origin).hostname)
  } catch {
    return false
  }
}

/**
 * 阿里云 OSS 图片按显示尺寸缩放并转 webp。非 OSS 地址、已带查询参数的地址原样返回。
 */
function sized(url, width) {
  if (!width || !url || url.includes('?') || !isOss(url)) return url
  const w = Math.min(Math.round(width), MAX_WIDTH)
  return `${url}?x-oss-process=image/resize,w_${w}/quality,q_82/format,webp`
}

/** 商品原始图片地址（未缩放）。own=true 时忽略统一模式，用于后台查看商品自己的图片 */
function baseUrl(item, own = false) {
  const ownImage = (item?.image || '').trim()
  if (own) return ownImage || DEFAULT_PRODUCT_IMAGE
  return uniform || !ownImage ? DEFAULT_PRODUCT_IMAGE : ownImage
}

/** 商品是否设置了自己的图片 */
export function hasOwnProductImage(item) {
  return !!(item?.image || '').trim()
}

/**
 * 解析商品展示图（兼容旧调用）
 * @param {{ image?: string }} [item] 商品对象
 * @param {number} [width] 期望显示宽度（CSS 像素），用于 OSS 缩放
 */
export function productImage(item, width) {
  return sized(baseUrl(item), width)
}

/**
 * 固定尺寸图片的 src + srcset（1x / 2x）。非 OSS 地址没有缩放版本，srcset 为空。
 * @param {{ image?: string }} [item]
 * @param {number} width 显示宽度（CSS 像素）
 * @param {{ own?: boolean }} [options]
 */
export function productImageSet(item, width, { own = false } = {}) {
  const base = baseUrl(item, own)
  const src = sized(base, width)
  if (!width || !isOss(base) || base.includes('?')) return { src, srcset: undefined }
  return { src, srcset: `${src} 1x, ${sized(base, width * 2)} 2x` }
}

/**
 * 响应式图片的 src + srcset（宽度描述符，配合 sizes 使用）
 * @param {{ image?: string }} [item]
 * @param {{ own?: boolean, maxWidth?: number }} [options]
 */
export function productImageSources(item, { own = false, maxWidth = MAX_WIDTH } = {}) {
  const base = baseUrl(item, own)
  if (!isOss(base) || base.includes('?')) return { src: base, srcset: undefined }
  const steps = WIDTH_STEPS.filter((w) => w <= maxWidth)
  return {
    src: sized(base, Math.min(640, maxWidth)),
    srcset: steps.map((w) => `${sized(base, w)} ${w}w`).join(', '),
  }
}

/** <img @error> 兜底：远程图加载失败时换成本地图（只换一次，避免死循环）。须同时去掉 srcset */
export function onProductImageError(event) {
  const img = event?.target
  if (!img || img.dataset.fallback) return false
  img.dataset.fallback = '1'
  img.removeAttribute('srcset')
  img.src = LOCAL_PRODUCT_IMAGE
  return true
}

/** 启动时预连接图床域名：提前完成 DNS/TCP/TLS 握手，首张商品图不必再等握手 */
export function preconnectImageOrigin() {
  if (typeof document === 'undefined' || !isOss(DEFAULT_PRODUCT_IMAGE)) return
  const origin = new URL(DEFAULT_PRODUCT_IMAGE).origin
  if (document.querySelector(`link[rel="preconnect"][href="${origin}"]`)) return
  for (const rel of ['preconnect', 'dns-prefetch']) {
    const link = document.createElement('link')
    link.rel = rel
    link.href = origin // 不加 crossorigin：<img> 是非 CORS 请求，只能复用非匿名连接
    document.head.appendChild(link)
  }
}
