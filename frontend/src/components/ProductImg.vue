<script setup>
/**
 * 商品图统一组件 —— 图片来源全部由 config/productImage.js 决定（统一图 / 各商品自有图、OSS 缩放）。
 *
 * - 外层是一个取景框：ratio 定比例（不传则填满父元素），加载前显示奶油色占位，加载后淡入
 * - 固定尺寸传 width（1x/2x）；响应式传 sizes（宽度档位 srcset）
 * - 远程图失败 → 换本地内置图；本地图也失败 → 显示品牌标志占位
 * - 首屏关键图传 eager，其余懒加载
 */
import { computed, ref, watch, onMounted } from 'vue'
import {
  PRODUCT_IMAGE_FOCUS,
  productImageSet,
  productImageSources,
  onProductImageError,
} from '@/config/productImage'

const props = defineProps({
  item: { type: Object, default: null },
  /** 固定显示宽度（CSS 像素） */
  width: { type: Number, default: 0 },
  /** 响应式 sizes，如 "(min-width: 1024px) 25vw, 50vw" */
  sizes: { type: String, default: '' },
  /** 宽高比，如 "1/1"、"4/5"、"4/3"；不传则填满父元素 */
  ratio: { type: String, default: '' },
  alt: { type: String, default: '' },
  eager: { type: Boolean, default: false },
  /** 后台预览：显示商品自己的图片（忽略统一模式） */
  own: { type: Boolean, default: false },
  fit: { type: String, default: 'cover' },
  position: { type: String, default: '' },
})

const image = computed(() => {
  if (props.sizes) return productImageSources(props.item, { own: props.own })
  return productImageSet(props.item, props.width || 320, { own: props.own })
})

const loaded = ref(false)
const failed = ref(false)
const imgRef = ref(null)

watch(() => image.value.src, () => {
  loaded.value = false
  failed.value = false
})

function onLoad() {
  loaded.value = true
}

function onError(event) {
  // 第一次失败换本地图；再失败显示占位
  if (!onProductImageError(event)) failed.value = true
}

onMounted(() => {
  const img = imgRef.value
  if (img?.complete && img.naturalWidth > 0) loaded.value = true
})
</script>

<template>
  <span
    class="pimg"
    :class="{ 'is-loaded': loaded, 'is-failed': failed, 'has-ratio': !!ratio }"
    :style="ratio ? { aspectRatio: ratio } : undefined"
  >
    <img
      v-if="!failed"
      :key="image.src"
      ref="imgRef"
      class="pimg__img"
      :src="image.src"
      :srcset="image.srcset"
      :sizes="sizes || undefined"
      :alt="alt"
      :loading="eager ? 'eager' : 'lazy'"
      :fetchpriority="eager ? 'high' : 'auto'"
      decoding="async"
      :style="{ objectFit: fit, objectPosition: position || PRODUCT_IMAGE_FOCUS }"
      @load="onLoad"
      @error="onError"
    >
    <span
      v-if="failed"
      class="pimg__fallback"
      role="img"
      :aria-label="alt || '图片暂不可用'"
    >
      <svg
        viewBox="0 0 48 48"
        aria-hidden="true"
      >
        <path d="M11 25.5C15 35 33 35 37 25.5" />
        <circle
          cx="24"
          cy="15.8"
          r="5.4"
        />
      </svg>
    </span>
  </span>
</template>

<style scoped>
.pimg {
  position: relative;
  display: block;
  overflow: hidden;
  width: 100%;
  height: 100%;
  background:
    radial-gradient(120% 90% at 50% 40%, var(--sw-fill-soft, #f4efe7) 0%, var(--sw-fill, #efe8dd) 100%);
  isolation: isolate;
}

.pimg.has-ratio {
  height: auto;
}

.pimg__img {
  display: block;
  width: 100%;
  height: 100%;
  opacity: 0;
  transition: opacity var(--sw-dur-3, 320ms) var(--sw-ease, ease), transform var(--sw-dur-3, 320ms) var(--sw-ease, ease);
}

.pimg.is-loaded .pimg__img {
  opacity: 1;
}

.pimg__fallback {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: var(--sw-ink-4, #8e8982);
}

.pimg__fallback svg {
  width: 32%;
  max-width: 64px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.4;
  stroke-linecap: round;
  opacity: 0.6;
}

.pimg__fallback circle {
  fill: currentColor;
  stroke: none;
}
</style>
