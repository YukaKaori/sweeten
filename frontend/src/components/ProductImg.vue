<script setup>
/**
 * 商品图统一组件：src/srcset 由 config/productImage.js 决定（统一图 / 各商品自有图、OSS 缩放、1x/2x），
 * 远程图失败自动换本地图。首屏内的大图传 eager，其余默认懒加载。
 */
import { computed } from 'vue'
import { productImageSet, onProductImageError } from '@/config/productImage'

const props = defineProps({
  item: { type: Object, default: null },
  /** 显示宽度（CSS 像素），用于请求合适尺寸的缩放图 */
  width: { type: Number, required: true },
  alt: { type: String, default: '' },
  /** 首屏关键图片：立即加载并提高优先级 */
  eager: { type: Boolean, default: false },
})

const image = computed(() => productImageSet(props.item, props.width))
</script>

<template>
  <img
    :src="image.src"
    :srcset="image.srcset"
    :alt="alt"
    :loading="eager ? 'eager' : 'lazy'"
    :fetchpriority="eager ? 'high' : 'auto'"
    decoding="async"
    @error="onProductImageError"
  >
</template>
