<script setup>
/**
 * FluidGradient.vue — 流体渐变背景
 * ───────────────────────────────────────────────────────────────
 * 两层策略：
 *   1) CSS 网格渐变（玫瑰↖ 琥珀↗ 抹茶↙）+ feTurbulence 颗粒噪声 —— 永远存在的兜底，
 *      零 WebGL / 低端 / reduced-motion 也成立；颜色取自 --mesh-* 令牌，深浅自适应。
 *   2) OGL WebGL 真流体（simplex domain-warp 三色 mesh）—— 仅 full 档 + 支持 WebGL +
 *      父组件请求 `webgl` 时叠加在 CSS 之上；组件动态 import() 懒加载，ogl 不进主 bundle。
 *      暗色下自动切「暖黑底 + 金/琥珀流光」（见 OglFluid.vue）。
 */
import { computed, defineAsyncComponent } from 'vue'
import { useMotionPref } from '@/composables/useMotionPref'

const props = defineProps({
  // 是否播放动画。父组件按 useMotionPref 的 tier 传入（static 档传 false）
  animated: { type: Boolean, default: true },
  // 是否请求 WebGL 真流体（能力不足时自动回退 CSS）
  webgl: { type: Boolean, default: false },
})

const { isFull, hasWebGL } = useMotionPref()

// 仅 full 档 + 支持 WebGL + 请求 webgl 且允许动画时启用 OGL；否则纯 CSS mesh 兜底
const useWebGL = computed(() => props.webgl && props.animated && isFull.value && hasWebGL)

// OGL 组件懒加载：动态 import → 独立 chunk，ogl 只在真正渲染时才拉取
const OglFluid = defineAsyncComponent(() => import('./OglFluid.vue'))
</script>

<template>
  <div class="fluid-gradient" :class="{ 'is-static': !animated }" aria-hidden="true">
    <!-- 网格渐变三色团：玫瑰↖ 琥珀↗ 抹茶↙（CSS 兜底层，始终存在） -->
    <div class="fluid-gradient__mesh">
      <span class="fluid-gradient__blob fluid-gradient__blob--rose"></span>
      <span class="fluid-gradient__blob fluid-gradient__blob--amber"></span>
      <span class="fluid-gradient__blob fluid-gradient__blob--matcha"></span>
    </div>

    <!-- 颗粒噪声：消除渐变色带（Stripe/Linear 招牌手法） -->
    <div class="fluid-gradient__grain"></div>

    <!-- WebGL 真流体：仅在能力足够时叠加（覆盖 CSS 兜底） -->
    <component :is="OglFluid" v-if="useWebGL" class="fluid-gradient__webgl" />
  </div>
</template>

<style scoped>
.fluid-gradient {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: var(--mesh-base, #fdf7f2);
  pointer-events: none;
  z-index: 0;
}

.fluid-gradient__mesh {
  position: absolute;
  inset: -10%;
}

/* 每团用软性 radial-gradient（自带柔边，无需昂贵的 filter:blur），
   仅用 transform 漂移 → GPU 合成，60fps 友好 */
.fluid-gradient__blob {
  position: absolute;
  display: block;
  width: 62%;
  height: 62%;
  border-radius: 50%;
  will-change: transform;
  animation: fluid-drift 22s ease-in-out infinite alternate;
}

.fluid-gradient__blob--rose {
  top: -8%;
  left: -6%;
  background: radial-gradient(circle at 50% 50%, var(--mesh-1) 0%, transparent 68%);
  animation-duration: 24s;
}

.fluid-gradient__blob--amber {
  top: -12%;
  right: -8%;
  left: auto;
  background: radial-gradient(circle at 50% 50%, var(--mesh-2) 0%, transparent 68%);
  animation-duration: 28s;
  animation-delay: -6s;
}

.fluid-gradient__blob--matcha {
  bottom: -14%;
  left: 4%;
  top: auto;
  width: 70%;
  height: 70%;
  background: radial-gradient(circle at 50% 50%, var(--mesh-3) 0%, transparent 70%);
  animation-duration: 32s;
  animation-delay: -12s;
}

/* SVG feTurbulence 噪声贴层，压掉渐变色带 */
.fluid-gradient__grain {
  position: absolute;
  inset: 0;
  opacity: var(--mesh-noise-opacity, 0.05);
  mix-blend-mode: soft-light;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 140px 140px;
  pointer-events: none;
}

.fluid-gradient__webgl {
  position: absolute;
  inset: 0;
}

/* static 档（reduced-motion / 低端）：漂移静止，同时撤掉 will-change 释放合成层 */
.fluid-gradient.is-static .fluid-gradient__blob {
  animation: none;
  will-change: auto;
}

@keyframes fluid-drift {
  0% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(4%, 6%, 0) scale(1.08); }
  100% { transform: translate3d(-3%, -4%, 0) scale(1.04); }
}

/* 双保险：系统级 reduced-motion 直接冻结 */
@media (prefers-reduced-motion: reduce) {
  .fluid-gradient__blob { animation: none; will-change: auto; }
}
</style>
