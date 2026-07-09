<script setup>
import { ref, watch, onMounted, computed, nextTick } from 'vue'
import { useMotionPref } from '@/composables/useMotionPref'

const props = defineProps({
  value: { type: [Number, String], required: true },
  duration: { type: Number, default: 1200 },
  prefix: { type: String, default: '' },
  suffix: { type: String, default: '' },
  decimals: { type: Number, default: 0 },
  // odometer 模式：每位数字上下滚动（CSS transform，无第三方库）
  odometer: { type: Boolean, default: false },
})

const { allowMotion } = useMotionPref()

/* ---------- 公共：解析 / 格式化 ---------- */
const parseNumber = (val) => {
  if (typeof val === 'number') return val
  const cleaned = String(val).replace(/[^0-9.\-]/g, '')
  return parseFloat(cleaned) || 0
}

const formatNumber = (num) => {
  const fixed = num.toFixed(props.decimals)
  const parts = fixed.split('.')
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return parts.join('.')
}

/* ================= 经典模式（rAF 数字滚动） ================= */
const display = ref('0')
let animationId = null
const easeOutExpo = (t) => (t === 1 ? 1 : 1 - Math.pow(2, -10 * t))

const animate = (target) => {
  cancelAnimationFrame(animationId)
  let startTime = null
  const step = (timestamp) => {
    if (!startTime) startTime = timestamp
    const progress = Math.min((timestamp - startTime) / props.duration, 1)
    display.value = formatNumber(target * easeOutExpo(progress))
    if (progress < 1) animationId = requestAnimationFrame(step)
    else display.value = formatNumber(target)
  }
  animationId = requestAnimationFrame(step)
}

const startClassic = () => {
  const target = parseNumber(props.value)
  if (!allowMotion.value) { display.value = formatNumber(target); return } // static 档：瞬时
  animate(target)
}

/* ================= odometer 模式（逐位翻滚） ================= */
// 目标格式串（结构：数字位 + 分隔符），如 "12,860"
const targetStr = computed(() => formatNumber(parseNumber(props.value)))

// 拆成单元：digit 位可滚动，sep（逗号/点）静止
const cells = computed(() =>
  targetStr.value.split('').map((ch) => {
    const isDigit = ch >= '0' && ch <= '9'
    return { isDigit, char: ch, target: isDigit ? Number(ch) : 0 }
  })
)

// 每位当前显示的数字（先 0，挂载后滚到 target）
const shown = ref([])
const rolled = ref(false)

const syncOdometer = async () => {
  shown.value = cells.value.map(() => 0)
  rolled.value = false
  if (!allowMotion.value) {          // static：直接到位、不滚动
    shown.value = cells.value.map((c) => c.target)
    rolled.value = true
    return
  }
  await nextTick()
  // 下一帧再落到目标值，触发 transform 过渡
  requestAnimationFrame(() => {
    shown.value = cells.value.map((c) => c.target)
    rolled.value = true
  })
}

/* ---------- 生命周期 ---------- */
onMounted(() => {
  if (props.odometer) syncOdometer()
  else startClassic()
})

watch(() => props.value, () => {
  if (props.odometer) syncOdometer()
  else startClassic()
})
</script>

<template>
  <!-- odometer 模式 -->
  <span v-if="odometer" class="count-up count-up--odometer" :class="{ 'no-anim': !allowMotion }">
    <span v-if="prefix" class="count-up__affix">{{ prefix }}</span>
    <template v-for="(cell, i) in cells" :key="i">
      <span v-if="cell.isDigit" class="count-up__digit">
        <span
          class="count-up__strip"
          :style="{
            transform: `translateY(${-(shown[i] || 0) * 10}%)`,
            transitionDelay: rolled ? `${i * 55}ms` : '0ms',
          }"
        >
          <span v-for="n in 10" :key="n" class="count-up__num">{{ n - 1 }}</span>
        </span>
      </span>
      <span v-else class="count-up__sep">{{ cell.char }}</span>
    </template>
    <span v-if="suffix" class="count-up__affix">{{ suffix }}</span>
  </span>

  <!-- 经典模式 -->
  <span v-else class="count-up">{{ prefix }}{{ display }}{{ suffix }}</span>
</template>

<style scoped>
.count-up {
  font-variant-numeric: tabular-nums;
}

/* ---- odometer ---- */
.count-up--odometer {
  display: inline-flex;
  align-items: flex-end;   /* 对齐各位底边，配合 line-height:1 使基线一致 */
  line-height: 1;
}

.count-up__affix,
.count-up__sep {
  display: inline-block;
  line-height: 1;
}

.count-up__digit {
  display: inline-block;
  height: 1em;
  overflow: hidden;
  vertical-align: baseline;
}

.count-up__strip {
  display: flex;
  flex-direction: column;
  /* 数字上下滚动：弹性缓出，逐位错峰（transitionDelay 由内联控制） */
  transition: transform 0.9s cubic-bezier(0.16, 1, 0.3, 1);
  will-change: transform;
}

.count-up__num {
  height: 1em;
  line-height: 1em;
  display: block;
  text-align: center;
}

/* static 档：无滚动过渡 */
.count-up--odometer.no-anim .count-up__strip {
  transition: none;
}

@media (prefers-reduced-motion: reduce) {
  .count-up__strip { transition: none; }
}
</style>
