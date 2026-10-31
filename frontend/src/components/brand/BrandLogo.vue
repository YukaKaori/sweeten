<script setup>
/**
 * 喜甜 SWEETEN 标志
 *
 * 图形：一道微笑弧线托起一颗樱桃 ——「喜」是笑意，樱桃是让日常更好的那一点甜（SWEETEN）。
 * 字标：「喜甜」思源宋体 700 + 「SWEETEN」思源黑体 500 宽字距，均已转为路径（wordmark.js），
 *       不依赖任何字体文件，任意尺寸都清晰。
 *
 * variant:
 *   mark        仅图形
 *   icon        应用图标（可可色圆角方块，用于折叠侧栏 / 小尺寸）
 *   horizontal  图形 + 喜甜 | SWEETEN（单行，页头）
 *   stacked     图形 + 两行字标（登录页 / 页脚）
 *   wordmark    仅两行字标
 * tone: default（可可 + 莓果樱桃）/ inverse（深色底：奶油 + 腮红樱桃）/ mono（全部 currentColor）
 */
import { computed } from 'vue'
import { WORDMARK_CN, WORDMARK_EN } from '@/assets/brand/wordmark'
import { BRAND } from '@/config/brand'

const props = defineProps({
  variant: { type: String, default: 'horizontal' },
  /** 整体高度（px） */
  size: { type: Number, default: 32 },
  tone: { type: String, default: 'default' },
})

const showMark = computed(() => props.variant !== 'wordmark')
const showWords = computed(() => !['mark', 'icon'].includes(props.variant))
const stacked = computed(() => props.variant === 'stacked' || props.variant === 'wordmark')
</script>

<template>
  <span
    class="sw-logo"
    :class="[`sw-logo--${variant}`, `sw-logo--${tone}`, { 'is-stacked': stacked }]"
    :style="{ '--logo-size': `${size}px` }"
    role="img"
    :aria-label="BRAND.fullName"
  >
    <svg
      v-if="variant === 'icon'"
      class="sw-logo__mark"
      viewBox="0 0 48 48"
      aria-hidden="true"
    >
      <rect
        x="0"
        y="0"
        width="48"
        height="48"
        rx="13"
        class="sw-logo__tile"
      />
      <path
        d="M11 25.5C15 35 33 35 37 25.5"
        class="sw-logo__smile sw-logo__smile--tile"
      />
      <circle
        cx="24"
        cy="15.8"
        r="5.4"
        class="sw-logo__cherry sw-logo__cherry--tile"
      />
      <path
        d="M24.3 10.6C24.8 7.8 26.6 5.8 29.6 5.2"
        class="sw-logo__stem sw-logo__stem--tile"
      />
    </svg>
    <svg
      v-else-if="showMark"
      class="sw-logo__mark"
      viewBox="0 0 48 48"
      aria-hidden="true"
    >
      <path
        d="M11 25.5C15 35 33 35 37 25.5"
        class="sw-logo__smile"
      />
      <circle
        cx="24"
        cy="15.8"
        r="5.4"
        class="sw-logo__cherry"
      />
      <path
        d="M24.3 10.6C24.8 7.8 26.6 5.8 29.6 5.2"
        class="sw-logo__stem"
      />
    </svg>

    <span
      v-if="showWords"
      class="sw-logo__words"
    >
      <svg
        class="sw-logo__cn"
        :viewBox="WORDMARK_CN.viewBox"
        aria-hidden="true"
      >
        <path :d="WORDMARK_CN.d" />
      </svg>
      <span
        v-if="!stacked"
        class="sw-logo__divider"
        aria-hidden="true"
      ></span>
      <svg
        class="sw-logo__en"
        :viewBox="WORDMARK_EN.viewBox"
        aria-hidden="true"
      >
        <path :d="WORDMARK_EN.d" />
      </svg>
    </span>
  </span>
</template>

<style scoped>
.sw-logo {
  --s: var(--logo-size);
  --cherry: var(--sw-cherry, #c46a64);
  display: inline-flex;
  align-items: center;
  gap: calc(var(--s) * 0.26);
  color: var(--sw-ink, #493b35);
  line-height: 0;
  flex-shrink: 0;
}

.sw-logo--inverse {
  color: var(--sw-cream-text, #f7f3ec);
  --cherry: var(--sw-blush, #e6a4a1);
}

.sw-logo--mono {
  --cherry: currentColor;
}

.sw-logo__mark {
  width: var(--s);
  height: var(--s);
  flex-shrink: 0;
  overflow: visible;
}

.sw-logo__smile,
.sw-logo__stem {
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
}

.sw-logo__smile { stroke-width: 4.2; }
.sw-logo__stem { stroke-width: 1.9; }
.sw-logo__cherry { fill: var(--cherry); }

/* 应用图标：可可底 + 奶油微笑 + 腮红樱桃 */
.sw-logo__tile { fill: var(--sw-cocoa, #493b35); }
.sw-logo__smile--tile,
.sw-logo__stem--tile { stroke: #f7f3ec; }
.sw-logo__cherry--tile { fill: #e6a4a1; }

.sw-logo__words {
  display: inline-flex;
  align-items: center;
  gap: calc(var(--s) * 0.3);
}

.sw-logo__cn,
.sw-logo__en {
  fill: currentColor;
  overflow: visible;
}

/* 单行：喜甜 | SWEETEN */
.sw-logo__cn {
  height: calc(var(--s) * 0.56);
  width: calc(var(--s) * 0.56 * 2.1148);
}

.sw-logo__en {
  height: calc(var(--s) * 0.2);
  width: calc(var(--s) * 0.2 * 8.2635);
  opacity: 0.86;
}

.sw-logo__divider {
  width: 1px;
  height: calc(var(--s) * 0.44);
  background: currentColor;
  opacity: 0.22;
}

/* 两行：喜甜 / SWEETEN */
.is-stacked .sw-logo__words {
  flex-direction: column;
  align-items: flex-start;
  gap: calc(var(--s) * 0.13);
}

.is-stacked .sw-logo__cn {
  height: calc(var(--s) * 0.62);
  width: calc(var(--s) * 0.62 * 2.1148);
}

.is-stacked .sw-logo__en {
  height: calc(var(--s) * 0.17);
  width: calc(var(--s) * 0.17 * 8.2635);
  opacity: 0.7;
}

.sw-logo--wordmark .sw-logo__words {
  gap: calc(var(--s) * 0.16);
}

.sw-logo--wordmark .sw-logo__cn {
  height: calc(var(--s) * 0.64);
  width: calc(var(--s) * 0.64 * 2.1148);
}

.sw-logo--wordmark .sw-logo__en {
  height: calc(var(--s) * 0.18);
  width: calc(var(--s) * 0.18 * 8.2635);
}
</style>
