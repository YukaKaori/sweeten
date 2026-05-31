<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  value: { type: [Number, String], required: true },
  duration: { type: Number, default: 1200 },
  prefix: { type: String, default: '' },
  suffix: { type: String, default: '' },
  decimals: { type: Number, default: 0 }
})

const display = ref('0')
let animationId = null

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

const easeOutExpo = (t) => (t === 1 ? 1 : 1 - Math.pow(2, -10 * t))

const animate = (target) => {
  cancelAnimationFrame(animationId)
  const startValue = 0
  let startTime = null

  const step = (timestamp) => {
    if (!startTime) startTime = timestamp
    const progress = Math.min((timestamp - startTime) / props.duration, 1)
    const easedProgress = easeOutExpo(progress)
    const current = startValue + (target - startValue) * easedProgress

    display.value = formatNumber(current)

    if (progress < 1) {
      animationId = requestAnimationFrame(step)
    } else {
      display.value = formatNumber(target)
    }
  }

  animationId = requestAnimationFrame(step)
}

const startAnimation = () => {
  const target = parseNumber(props.value)
  animate(target)
}

watch(() => props.value, startAnimation)
onMounted(startAnimation)
</script>

<template>
  <span class="count-up">{{ prefix }}{{ display }}{{ suffix }}</span>
</template>

<style scoped>
.count-up {
  font-variant-numeric: tabular-nums;
}
</style>
