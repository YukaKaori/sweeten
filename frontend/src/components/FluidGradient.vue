<script setup>
/**
 * FluidGradient.vue — 奶油流体渐变背景（SHOWCASE 第一波：纯 CSS 版）
 * ───────────────────────────────────────────────────────────────
 * 三点网格渐变（玫瑰↖ 琥珀↗ 抹茶↙）+ SVG feTurbulence 颗粒噪声 + 缓慢漂移。
 * 颜色全部取自 base.css 的 --mesh-* 令牌，浅色/暗色自动切换。
 *
 * 【OGL 升级接口 · 预留，本波不实现 WebGL】
 *   - prop `renderer`：'css'（默认）| 'webgl'。将来接入 OGL 流体着色器时，
 *     父组件传 renderer="webgl"，并通过默认插槽注入 <OglFluid/> canvas；
 *     CSS 网格自动降级为其后的静态兜底层（无 WebGL / 低端设备时可见）。
 *   - 默认插槽即 WebGL 画布的挂载位（渲染在 CSS 网格之上）。
 * 无论哪种 renderer，CSS 网格始终存在，保证零 WebGL 也惊艳。
 */
import { computed } from 'vue'

const props = defineProps({
  // 是否播放漂移动画。父组件按 useMotionPref 的 tier 传入（static 档传 false）
  animated: { type: Boolean, default: true },
  // 渲染器：本波仅 'css'；'webgl' 为将来 OGL 预留
  renderer: {
    type: String,
    default: 'css',
    validator: (v) => ['css', 'webgl'].includes(v),
  },
})

// 是否启用 WebGL 插槽层（预留接口；本波父组件不会传 webgl）
const webglSlot = computed(() => props.renderer === 'webgl')
</script>

<template>
  <div class="fluid-gradient" :class="{ 'is-static': !animated }" aria-hidden="true">
    <!-- 网格渐变三色团：玫瑰↖ 琥珀↗ 抹茶↙ -->
    <div class="fluid-gradient__mesh">
      <span class="fluid-gradient__blob fluid-gradient__blob--rose"></span>
      <span class="fluid-gradient__blob fluid-gradient__blob--amber"></span>
      <span class="fluid-gradient__blob fluid-gradient__blob--matcha"></span>
    </div>

    <!-- 颗粒噪声：消除渐变色带（Stripe/Linear 招牌手法） -->
    <div class="fluid-gradient__grain"></div>

    <!-- WebGL 升级挂载位（renderer='webgl' 时启用；本波不渲染） -->
    <div v-if="webglSlot" class="fluid-gradient__webgl">
      <slot />
    </div>
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

/* static 档（reduced-motion / 低端）：漂移静止 */
.fluid-gradient.is-static .fluid-gradient__blob {
  animation: none;
}

@keyframes fluid-drift {
  0% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(4%, 6%, 0) scale(1.08); }
  100% { transform: translate3d(-3%, -4%, 0) scale(1.04); }
}

/* 双保险：系统级 reduced-motion 直接冻结 */
@media (prefers-reduced-motion: reduce) {
  .fluid-gradient__blob { animation: none; }
}
</style>
