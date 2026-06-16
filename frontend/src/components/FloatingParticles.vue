<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  count: { type: Number, default: 15 },
  color: { type: String, default: 'primary' },
  speed: { type: Number, default: 1 },
})

const canvasRef = ref(null)
let ctx = null
let particles = []
let animationId = null

const colorMap = {
  primary: ['rgba(232, 99, 122, 0.08)', 'rgba(240, 140, 158, 0.06)', 'rgba(253, 232, 236, 0.1)'],
  matcha: ['rgba(107, 142, 78, 0.08)', 'rgba(139, 168, 106, 0.06)', 'rgba(232, 240, 223, 0.1)'],
  amber: ['rgba(240, 163, 92, 0.08)', 'rgba(245, 185, 122, 0.06)', 'rgba(252, 220, 197, 0.1)'],
  blue: ['rgba(107, 140, 206, 0.08)', 'rgba(139, 163, 216, 0.06)', 'rgba(230, 236, 245, 0.1)'],
}

class Particle {
  constructor(w, h, colors) {
    this.x = Math.random() * w
    this.y = Math.random() * h
    this.radius = Math.random() * 4 + 1.5
    this.color = colors[Math.floor(Math.random() * colors.length)]
    this.vx = (Math.random() - 0.5) * 0.3
    this.vy = (Math.random() - 0.5) * 0.3 - 0.15
    this.opacity = Math.random() * 0.5 + 0.3
    this.phase = Math.random() * Math.PI * 2
  }

  update(w, h, time) {
    // Gentle sinusoidal drift
    this.x += this.vx + Math.sin(time * 0.001 + this.phase) * 0.08
    this.y += this.vy + Math.cos(time * 0.0013 + this.phase) * 0.06

    // Wrap around edges
    if (this.x < -10) this.x = w + 10
    if (this.x > w + 10) this.x = -10
    if (this.y < -10) this.y = h + 10
    if (this.y > h + 10) this.y = -10
  }

  draw(ctx, time) {
    const pulse = 1 + Math.sin(time * 0.002 + this.phase) * 0.2
    const r = this.radius * pulse
    ctx.beginPath()
    ctx.arc(this.x, this.y, r, 0, Math.PI * 2)
    ctx.fillStyle = this.color
    ctx.fill()
  }
}

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return

  ctx = canvas.getContext('2d')
  const resize = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
  }
  resize()
  window.addEventListener('resize', resize)

  const colors = colorMap[props.color] || colorMap.primary

  // Initialize particles
  particles = Array.from({ length: props.count }, () =>
    new Particle(canvas.width, canvas.height, colors)
  )

  // Animation loop
  const animate = (time) => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    particles.forEach((p) => {
      p.update(canvas.width, canvas.height, time)
      p.draw(ctx, time)
    })
    animationId = requestAnimationFrame(animate)
  }
  animationId = requestAnimationFrame(animate)

  onUnmounted(() => {
    cancelAnimationFrame(animationId)
    window.removeEventListener('resize', resize)
  })
})
</script>

<template>
  <canvas ref="canvasRef" class="floating-particles"></canvas>
</template>

<style scoped>
.floating-particles {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  opacity: 0.7;
}
</style>
