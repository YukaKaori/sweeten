<script setup>
/**
 * 喜甜 SWEETEN 标志
 *
 * 路径数据由 scripts/brand/build_brand_assets.py 生成（src/assets/brand/logo.js），
 * 与 public/brand/ 下的 SVG / PNG 导出图同源，任意尺寸都清晰，不依赖字体文件。
 *
 * variant（用法见 BRAND.md）：
 *   primary     主标志（竖版）：喜甜 / SWEETEN / 标语 / 分隔线 / 品类 —— 登录页、页脚等品牌展示位
 *   horizontal  横版：喜甜 + SWEETEN 与标语 —— 高度充裕的页头
 *   inline      单行：喜甜 + SWEETEN —— 导航栏、侧栏等高度有限处
 *   stacked     两行：喜甜 / SWEETEN —— 小票、窄而高的位置
 *   symbol      图形：两片叶子托起一颗爱心 —— 折叠侧栏、小尺寸、装饰与加载状态
 * tone：default（可可棕 + 草莓腮红爱心）/ inverse（深色或照片底：奶油白 + 草莓腮红）/ mono（全部 currentColor，单色印刷）
 * size：标志高度（px），宽度按比例
 */
import { computed } from 'vue'
import { LOGO_LOCKUPS, LOGO_PARTS } from '@/assets/brand/logo'
import { BRAND } from '@/config/brand'

const props = defineProps({
  variant: {
    type: String,
    default: 'inline',
    validator: (v) => v in LOGO_LOCKUPS,
  },
  size: { type: Number, default: 32 },
  tone: {
    type: String,
    default: 'default',
    validator: (v) => ['default', 'inverse', 'mono'].includes(v),
  },
})

const lockup = computed(() => LOGO_LOCKUPS[props.variant])
const width = computed(() => {
  const [, , w, h] = lockup.value.viewBox.split(' ').map(Number)
  return Math.round((props.size * w) / h * 10) / 10
})
</script>

<template>
  <svg
    class="sw-logo"
    :class="[`sw-logo--${variant}`, `sw-logo--${tone}`]"
    :viewBox="lockup.viewBox"
    :width="width"
    :height="size"
    role="img"
    :aria-label="BRAND.fullName"
    focusable="false"
  >
    <g
      v-for="[part, transform] in lockup.items"
      :key="part"
      :transform="transform"
    >
      <path
        class="sw-logo__ink"
        :d="LOGO_PARTS[part].ink"
      />
      <path
        v-if="LOGO_PARTS[part].accent"
        class="sw-logo__accent"
        :d="LOGO_PARTS[part].accent"
      />
    </g>
  </svg>
</template>

<style scoped>
.sw-logo {
  --sw-logo-accent: var(--sw-blush, #e6a4a1);
  display: inline-block;
  flex-shrink: 0;
  overflow: visible;
  vertical-align: middle;
  color: var(--sw-ink, #493b35);
}

/* 深色底 / 照片上：奶油白字标，爱心保持草莓腮红 */
.sw-logo--inverse {
  color: var(--sw-cream-text, #f7f3ec);
}

/* 单色（小票打印等）：爱心随文字色 */
.sw-logo--mono {
  --sw-logo-accent: currentColor;
}

.sw-logo__ink {
  fill: currentColor;
}

.sw-logo__accent {
  fill: var(--sw-logo-accent);
}
</style>
