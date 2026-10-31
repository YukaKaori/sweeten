/**
 * useScrollReveal.js
 * Apple-style scroll-triggered reveal animations using Intersection Observer.
 *
 * Usage:
 *   import { useScrollReveal } from '@/composables/useScrollReveal'
 *   // In setup:
 *   useScrollReveal('.bento-card', { stagger: 0.1, threshold: 0.15 })
 */

import { onMounted, onUnmounted } from 'vue'

export function useScrollReveal(selector, options = {}) {
  const {
    root = null,
    rootMargin = '0px 0px -40px 0px',  // trigger slightly before visible
    threshold = 0.12,
    stagger = 0,       // seconds between each child reveal
    animation = 'revealSlideUp',  // CSS animation class
    once = true,       // only animate once
  } = options

  let observer = null

  onMounted(() => {
    // Respect reduced motion
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      const elements = document.querySelectorAll(selector)
      elements.forEach(el => {
        el.style.opacity = '1'
        el.style.transform = 'none'
      })
      return
    }

    observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          const el = entry.target
          const index = Array.from(el.parentNode?.children || []).indexOf(el)
          const delay = stagger > 0 ? (stagger * index) + 's' : '0s'

          el.style.animationDelay = delay
          el.style.animationName = animation
          el.style.animationFillMode = 'forwards'
          el.style.animationDuration = '0.7s'
          el.style.animationTimingFunction = 'cubic-bezier(0.16, 1, 0.3, 1)'

          if (once) {
            observer.unobserve(el)
          }
        } else if (!once) {
          const el = entry.target
          el.style.animationName = ''
          el.style.opacity = '0'
          el.style.transform = 'translateY(24px)'
        }
      })
    }, {
      root,
      rootMargin,
      threshold,
    })

    const elements = document.querySelectorAll(selector)
    elements.forEach(el => {
      // Set initial hidden state
      el.style.opacity = '0'
      if (animation === 'revealSlideUp') {
        el.style.transform = 'translateY(24px)'
      } else if (animation === 'revealScaleIn') {
        el.style.transform = 'scale(0.92)'
      }
      observer.observe(el)
    })
  })

  onUnmounted(() => {
    if (observer) {
      observer.disconnect()
    }
  })
}
