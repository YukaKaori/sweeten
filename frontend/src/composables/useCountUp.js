import { ref, onMounted, watch } from 'vue'

/**
 * 数字滚动动画
 * @param {import('vue').Ref<number>} targetValue - 目标数值
 * @param {number} duration - 动画时长（毫秒）
 * @param {boolean} autoStart - 是否自动开始
 */
export function useCountUp(targetValue, { duration = 1200, autoStart = true, decimals = 0 } = {}) {
  const current = ref(0)
  let animationId = null
  let startTime = null

  const easeOutExpo = (t) => (t === 1 ? 1 : 1 - Math.pow(2, -10 * t))

  const animate = (timestamp) => {
    if (!startTime) startTime = timestamp
    const progress = Math.min((timestamp - startTime) / duration, 1)
    const easedProgress = easeOutExpo(progress)

    current.value = parseFloat((easedProgress * targetValue.value).toFixed(decimals))

    if (progress < 1) {
      animationId = requestAnimationFrame(animate)
    } else {
      current.value = parseFloat(targetValue.value.toFixed(decimals))
    }
  }

  const start = () => {
    cancelAnimationFrame(animationId)
    startTime = null
    current.value = 0
    animationId = requestAnimationFrame(animate)
  }

  // 目标值变化时重新动画
  watch(targetValue, () => {
    start()
  })

  if (autoStart) {
    onMounted(() => start())
  }

  return { current, start }
}
