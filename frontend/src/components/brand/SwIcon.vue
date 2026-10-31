<script setup>
/**
 * 喜甜线性图标：24 网格、1.6 描边、圆角端点，与标志的微笑弧线同一笔触。
 * 用法：<SwIcon name="bag" :size="20" />；装饰性图标默认 aria-hidden，需要朗读时传 label。
 *
 * 图形用 path / circle / rect 描述（不用 v-html）。新增图标在 ICONS 里补一项即可。
 */
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 20 },
  stroke: { type: Number, default: 1.6 },
  label: { type: String, default: '' },
})

// p: path d；c: [cx, cy, r, filled?]；r: [x, y, w, h, rx]
const ICONS = {
  home: { p: ['M4 10.5 12 4l8 6.5', 'M6.5 9v11h11V9', 'M10 20v-5h4v5'] },
  grid: { r: [[4.5, 4.5, 6, 6, 1.6], [13.5, 4.5, 6, 6, 1.6], [4.5, 13.5, 6, 6, 1.6], [13.5, 13.5, 6, 6, 1.6]] },
  bag: { p: ['M5.2 8.5h13.6l-1 11.5H6.2L5.2 8.5z', 'M9 8.5V7a3 3 0 0 1 6 0v1.5'] },
  receipt: { p: ['M6 3.5h12v17l-2-1.3-2 1.3-2-1.3-2 1.3-2-1.3-2 1.3z', 'M9 8h6', 'M9 11.5h6', 'M9 15h3.5'] },
  user: { c: [[12, 8.5, 3.6]], p: ['M5 20c1-3.6 3.8-5.6 7-5.6s6 2 7 5.6'] },
  users: { c: [[9, 8.5, 3.2], [16.5, 9.5, 2.5]], p: ['M3.5 19.5c.8-3.2 3-5 5.5-5s4.7 1.8 5.5 5', 'M15.5 14.6c2.3-.4 4.3 1.1 5 4'] },
  search: { c: [[11, 11, 6.5]], p: ['M16 16l4 4'] },
  close: { p: ['M6 6l12 12', 'M18 6 6 18'] },
  plus: { p: ['M12 5v14', 'M5 12h14'] },
  minus: { p: ['M5 12h14'] },
  'chevron-left': { p: ['M15 5l-7 7 7 7'] },
  'chevron-right': { p: ['M9 5l7 7-7 7'] },
  'chevron-down': { p: ['M5 9l7 7 7-7'] },
  'arrow-right': { p: ['M5 12h14', 'M13 6l6 6-6 6'] },
  'arrow-left': { p: ['M19 12H5', 'M11 6l-6 6 6 6'] },
  check: { p: ['M5 12.5l4.5 4.5L19 7.5'] },
  clock: { c: [[12, 12, 8.5]], p: ['M12 7.5V12l3 2'] },
  truck: { p: ['M3 6.5h11v9.5H3z', 'M14 9.5h3.8l3.2 3.3v3.2h-7'], c: [[7, 17.5, 1.8], [17, 17.5, 1.8]] },
  star: { p: ['M12 3.8l2.3 5 5.4.6-4 3.7 1.1 5.3L12 15.7l-4.8 2.7 1.1-5.3-4-3.7 5.4-.6z'] },
  gift: { r: [[4, 8.5, 16, 4, 1]], p: ['M5.5 12.5V20h13v-7.5', 'M12 8.5V20', 'M12 8.5C10.8 5.4 7 4.6 7 7c0 1.3 2.3 1.5 5 1.5zm0 0c1.2-3.1 5-3.9 5-1.5 0 1.3-2.3 1.5-5 1.5z'] },
  trash: { p: ['M5 7h14', 'M9.5 7V5h5v2', 'M7 7l1 13h8l1-13', 'M10.5 11v5', 'M13.5 11v5'] },
  refresh: { p: ['M19 12a7 7 0 1 1-2.05-4.95', 'M19 4.5V9h-4.5'] },
  copy: { r: [[8.5, 8.5, 11, 11, 2]], p: ['M15.5 8.5V6.5a2 2 0 0 0-2-2h-7a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h2'] },
  phone: { p: ['M6.5 4h3l1.5 4-2 1.2a10 10 0 0 0 5.8 5.8l1.2-2 4 1.5v3a2 2 0 0 1-2 2A15 15 0 0 1 4.5 6a2 2 0 0 1 2-2z'] },
  pin: { p: ['M12 21s6.5-6.1 6.5-11A6.5 6.5 0 0 0 5.5 10c0 4.9 6.5 11 6.5 11z'], c: [[12, 10, 2.3]] },
  info: { c: [[12, 12, 8.5]], p: ['M12 11v5', 'M12 7.9v.1'] },
  alert: { p: ['M12 4.5 20.5 19h-17L12 4.5z', 'M12 10v4', 'M12 16.6v.1'] },
  leaf: { p: ['M5 19c0-8 5-13 14-14 0 9-5 14-13 14', 'M5 19l8-8'] },
  cake: { p: ['M4 20h16', 'M5.5 20v-6.5h13V20', 'M5.5 16.5c1.4 0 1.4 1 2.9 1s1.4-1 2.9-1 1.4 1 2.9 1 1.4-1 2.9-1 1.4 1 2.9 1', 'M12 13.5v-3'], c: [[12, 8.3, 1.9, true]] },
  tag: { p: ['M4 4.5h7.5L20 13l-7 7-8.5-8.5z'], c: [[8.5, 8.5, 1.3, true]] },
  box: { p: ['M4 8l8-4 8 4v8l-8 4-8-4z', 'M4 8l8 4 8-4', 'M12 12v8'] },
  building: { p: ['M5 20V5.5L13 4v16', 'M13 9h6v11', 'M3.5 20h17', 'M8 8.5h2', 'M8 12h2', 'M8 15.5h2', 'M16 12.5h.5', 'M16 16h.5'] },
  chart: { p: ['M4 20h16', 'M7 16.5V11', 'M12 16.5V6.5', 'M17 16.5v-3.5'] },
  doc: { p: ['M7 3.5h7l4 4v13H7z', 'M14 3.5v4h4', 'M9.5 12h6', 'M9.5 15.5h6'] },
  bell: { p: ['M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z', 'M10 20.5a2 2 0 0 0 4 0'] },
  menu: { p: ['M4 7h16', 'M4 12h16', 'M4 17h16'] },
  sidebar: { r: [[4, 4.5, 16, 15, 3]], p: ['M9.5 4.5v15'] },
  external: { p: ['M14 5h5v5', 'M19 5l-8 8', 'M18 14v5H5V6h5'] },
  print: { p: ['M7 9V4h10v5', 'M7 15H5a1 1 0 0 1-1-1V10a1 1 0 0 1 1-1h14a1 1 0 0 1 1 1v4a1 1 0 0 1-1 1h-2', 'M7 13h10v7H7z'] },
  download: { p: ['M12 4v11', 'M7 10l5 5 5-5', 'M5 20h14'] },
  edit: { p: ['M5 19h4L19.5 8.5a2.1 2.1 0 0 0-3-3L6 16v3z', 'M14.5 7.5l2 2'] },
  eye: { p: ['M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z'], c: [[12, 12, 3]] },
  moon: { p: ['M19.5 14.5A7.5 7.5 0 1 1 9.5 4.5a6 6 0 0 0 10 10z'] },
  sun: { c: [[12, 12, 4]], p: ['M12 2.5v2', 'M12 19.5v2', 'M2.5 12h2', 'M19.5 12h2', 'M5.3 5.3l1.4 1.4', 'M17.3 17.3l1.4 1.4', 'M5.3 18.7l1.4-1.4', 'M17.3 6.7l1.4-1.4'] },
  logout: { p: ['M10 4.5H6a1.5 1.5 0 0 0-1.5 1.5v12A1.5 1.5 0 0 0 6 19.5h4', 'M14.5 8l4 4-4 4', 'M18.5 12H9'] },
  lock: { r: [[5.5, 10.5, 13, 9.5, 2]], p: ['M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5'] },
  card: { r: [[3.5, 5.5, 17, 13, 2.5]], p: ['M3.5 10h17', 'M7 15h4'] },
  sparkle: { p: ['M12 4c.6 4 2 5.4 6 6-4 .6-5.4 2-6 6-.6-4-2-5.4-6-6 4-.6 5.4-2 6-6z'] },
}

const icon = computed(() => ICONS[props.name] || ICONS.info)
</script>

<template>
  <svg
    class="sw-icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="stroke"
    stroke-linecap="round"
    stroke-linejoin="round"
    :role="label ? 'img' : undefined"
    :aria-label="label || undefined"
    :aria-hidden="label ? undefined : 'true'"
    focusable="false"
  >
    <rect
      v-for="(r, i) in icon.r || []"
      :key="`r${i}`"
      :x="r[0]"
      :y="r[1]"
      :width="r[2]"
      :height="r[3]"
      :rx="r[4]"
    />
    <path
      v-for="(d, i) in icon.p || []"
      :key="`p${i}`"
      :d="d"
    />
    <circle
      v-for="(c, i) in icon.c || []"
      :key="`c${i}`"
      :cx="c[0]"
      :cy="c[1]"
      :r="c[2]"
      :fill="c[3] ? 'currentColor' : 'none'"
      :stroke="c[3] ? 'none' : undefined"
    />
  </svg>
</template>

<style scoped>
.sw-icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
