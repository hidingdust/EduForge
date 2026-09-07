<!--
  AnimationViewer.vue — 基于注册表的动画渲染引擎
  
  Props:
    animationJson 或 animationData  { elements[], globalConfig, metadata }
  
  Backend DTO 对应:
    动画类型: fadeIn/Out, zoomIn/Out, move, rotate, slideIn/Out
    元素类型: text, shape, line, image
    形状类型: rectangle, circle, polygon

  扩展方式（不动本组件）:
    import { registerAnimation, registerShape } from '@/utils/animation'
    registerAnimation('bounce', (anim, p, eased, el) => ({ ... }))
    registerShape('hexagon', () => ({ css: { clipPath: '...' } }))
-->
<template>
  <div class="animation-viewer" :style="{ background: config.background }">
    <div v-for="el in elementList" :key="el.elementId" ref="elRefs" class="anim-element" :class="{
      'anim-text': el.type === 'text',
      'anim-shape': el.type === 'shape'
    }" :style="getElementStyle(el)">
      <!-- 动画内容层：驱动 opacity（淡入淡出）和 transform（滑动/缩放/旋转） -->
      <div class="anim-content" :style="getContentStyle(el)">
        <!-- 文本元素 -->
        <span v-if="el.type === 'text'">{{ el.content.text }}</span>
        <!-- 圆形图形 -->
        <div v-if="el.type === 'shape'" class="shape-circle" :style="{ background: el.content.fill }"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted, onUnmounted } from 'vue'
import { VideoPlay, VideoPause, RefreshRight } from '@element-plus/icons-vue'
import {
  computeElementState,
  getShapeRenderer,
  computeElementCSS,
  getPositionStyle
} from '@/utils/animation'

const props = defineProps({
  animationJson: { type: Object, default: null },
  animationData: { type: Object, default: null },
  autoLayout: { type: Boolean, default: true }
})

const canvasRef = ref(null)
const elementList = ref([])
const config = ref({ background: '#FFFFFF' })
const currentTime = ref(0)
const totalDuration = ref(8000)
const isPlaying = ref(false)
const layoutOffsets = ref(new Map())

let rafId = null
let startTimestamp = 0
let lastPausedTime = 0

// ---- 计算属性 ----

const progressPercent = computed(() => {
  if (totalDuration.value <= 0) return 0
  return Math.min(100, (currentTime.value / totalDuration.value) * 100)
})

// 动画结束后的元素保持可见（computeElementState 返回终态）
const visibleElements = computed(() => {
  const t = currentTime.value
  return elementList.value.filter(el => {
    if (!el.animations?.length) return true
    const firstStart = Math.min(...el.animations.map(a => a.startTime || 0))
    return t >= firstStart - 100
  })
})

// ---- 文本分类与自动布局 ----

const LIST_ITEM_RE = /^\s*\d+[.、.．)\]\}\s]+/

/**
 * 判断文本元素类型
 * - list-item：以数字编号开头（如 "1. 需营养"）
 * - title：靠上、宽度大、文字短
 * - normal：其他
 */
function classifyText(el) {
  const text = el.content?.text || ''
  const y = el.position?.y ?? 50
  const width = parseFloat(el.size?.width) || 100

  if (LIST_ITEM_RE.test(text)) return 'list-item'
  if (y < 20 && width >= 50 && text.length <= 30) return 'title'
  return 'normal'
}

/**
 * 估算文本元素渲染后的宽高（单位：% 相对画布）
 * 优化：区分中英文宽度，提升碰撞检测精度
 */
function estimateTextSize(el) {
  const text = el.content?.text || ''
  const maxWidth = parseFloat(el.size?.width) || 100

  // 更保守的字符宽度，避免低估导致碰撞不准
  const charWidthPercent = 2.8
  const lineHeightPercent = 6.5

  // 中文按1字符宽度，英文/数字按0.6宽度
  let totalChars = 0
  for (const ch of text) {
    if (/[\u4e00-\u9fa5]/.test(ch)) totalChars += 1
    else totalChars += 0.6
  }

  const charsPerLine = Math.max(1, Math.floor(maxWidth / charWidthPercent))
  const lines = Math.max(1, Math.ceil(totalChars / charsPerLine))
  const width = Math.min(totalChars * charWidthPercent, maxWidth)
  const height = lines * lineHeightPercent

  return { width, height }
}

function boxesOverlap(a, b) {
  return a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y
}

/**
 * 获取元素在最终布局下的有效边界框（单位：%）
 */
function getEffectiveBox(el) {
  const x = el.position?.x ?? 0
  const y = el.position?.y ?? 0
  const w = parseFloat(el.size?.width) || 100
  const h = parseFloat(el.size?.height) || 100

  if (el.type === 'text') {
    const kind = classifyText(el)
    const est = estimateTextSize(el)
    const tw = est.width
    const th = est.height

    if (kind === 'list-item' || el.style?.textAlign === 'left') {
      return { x, y: y - th / 2, w: tw, h: th }
    }
    if (el.style?.textAlign === 'right') {
      return { x: x - tw, y: y - th / 2, w: tw, h: th }
    }
    return { x: x - tw / 2, y: y - th / 2, w: tw, h: th }
  }

  return { x: x - w / 2, y: y - h / 2, w, h }
}

/**
 * 自动避让：检测所有元素重叠，次要元素自动偏移
 * 优先级：标题 > 列表项 > 普通文本 > 图形 > 图片 > 线条
 * 文字间重叠垂直错开，文字与图形水平错开
 */
function computeAutoLayout(elements) {
  const offsets = new Map()
  elements.forEach(el => offsets.set(el.elementId, { x: 0, y: 0 }))

  const priority = (el) => {
    if (el.type === 'text') {
      const kind = classifyText(el)
      if (kind === 'title') return 10
      if (kind === 'list-item') return 8
      return 6
    }
    if (el.type === 'shape') return 4
    if (el.type === 'image') return 2
    return 0
  }

  const sorted = [...elements].sort((a, b) => priority(b) - priority(a))

  for (let i = 0; i < sorted.length; i++) {
    const elA = sorted[i]
    const boxA = getEffectiveBox(elA)
    const offA = offsets.get(elA.elementId)
    boxA.x += offA.x
    boxA.y += offA.y

    for (let j = i + 1; j < sorted.length; j++) {
      const elB = sorted[j]
      const boxB = getEffectiveBox(elB)
      const offB = offsets.get(elB.elementId)
      boxB.x += offB.x
      boxB.y += offB.y

      if (!boxesOverlap(boxA, boxB)) continue

      const overlapY = Math.min(boxA.y + boxA.h - boxB.y, boxB.y + boxB.h - boxA.y)
      const overlapX = Math.min(boxA.x + boxA.w - boxB.x, boxB.x + boxB.w - boxA.x)

      // 文字与文字重叠：优先级低的向下垂直错开，偏移量加大
      if (elA.type === 'text' && elB.type === 'text') {
        offB.y += overlapY + 2.5
      }
      // 文字与图形/图片重叠：图形水平错开
      else if (elA.type === 'text' && (elB.type === 'shape' || elB.type === 'image')) {
        const centerA = boxA.x + boxA.w / 2
        const centerB = boxB.x + boxB.w / 2
        const dir = centerB < centerA ? -1 : 1
        offB.x += dir * (overlapX + 2.5)
      }
    }
  }

  return offsets
}

// ---- 样式计算 ----

// 根据背景色判断对比文字色
function getContrastColor(bgColor) {
  if (!bgColor) return '#1f2937'
  const rgb = parseColor(bgColor)
  if (!rgb) return '#1f2937'
  const luminance = (0.299 * rgb.r + 0.587 * rgb.g + 0.114 * rgb.b) / 255
  return luminance > 0.5 ? '#1f2937' : '#f0f0f0'
}

function parseColor(color) {
  if (typeof color !== 'string') return null
  let hex = color.trim()
  if (hex.startsWith('#')) {
    hex = hex.slice(1)
    if (hex.length === 3) hex = hex.split('').map(c => c + c).join('')
    if (hex.length === 4) hex = hex.split('').map(c => c + c).join('')
    if (hex.length === 6 || hex.length === 8) {
      return { r: parseInt(hex.slice(0, 2), 16), g: parseInt(hex.slice(2, 4), 16), b: parseInt(hex.slice(4, 6), 16) }
    }
  }
  const rgbMatch = color.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/)
  if (rgbMatch) {
    return { r: parseInt(rgbMatch[1]), g: parseInt(rgbMatch[2]), b: parseInt(rgbMatch[3]) }
  }
  return null
}

// wrapper 层样式：静态定位 + 自动布局偏移 + 可能的 move 动画位置覆盖
function getWrapperStyle(el) {
  const basePos = getPositionStyle(el)
  const state = computeElementState(el, currentTime.value)
  const userCSS = computeElementCSS(el)

  const styles = { ...basePos, ...userCSS }

  // 动画驱动的 opacity（覆盖元素静态 style.opacity，让淡入淡出生效）
  if (state.opacity != null) {
    styles.opacity = state.opacity
  }

  // 自动避让偏移
  const offset = layoutOffsets.value.get(el.elementId)
  if (offset && (offset.x || offset.y)) {
    styles.left = `calc(${styles.left} + ${offset.x}%)`
    styles.top = `calc(${styles.top} + ${offset.y}%)`
  }

  // move 动画返回 positionOverride，直接覆盖 left/top
  if (state.positionOverride) {
    Object.assign(styles, state.positionOverride)
  }

  // 文本元素：高度自适应，自动根据背景对比度选文字颜色
  if (el.type === 'text') {
    styles.height = 'auto'
    styles.minHeight = '1.4em'
    if (!styles.color) {
      styles.color = getContrastColor(config.value.background)
    }

    const kind = classifyText(el)
    const rawWidth = styles.width

    if (kind === 'list-item') {
      // 列表项：左边缘对齐 x，宽度按内容自适应，避免覆盖左侧圆点
      styles.textAlign = 'left'
      styles.width = 'auto'
      styles.maxWidth = rawWidth
      styles.transform = 'translate(0, -50%)'
    } else if (kind === 'title') {
      // 标题：保持居中
      styles.textAlign = 'center'
      styles.transform = 'translate(-50%, -50%)'
    } else {
      // 普通文本：默认左对齐，减少居中堆叠
      const align = styles.textAlign || 'left'
      if (align === 'left') styles.transform = 'translate(0, -50%)'
      else if (align === 'right') styles.transform = 'translate(-100%, -50%)'
      else styles.transform = 'translate(-50%, -50%)'
    }
  }

  // 线条元素
  if (el.type === 'line') {
    styles.height = 'auto'
    styles.backgroundColor = 'transparent'
  }

  // arc / 轨道线等 shape：size 为 0 时撑满画布，让 SVG 弧线覆盖整个可视区
  if (el.type === 'shape') {
    const shapeName = el.content?.shape
    const sw = parseFloat(el.size?.width)
    const sh = parseFloat(el.size?.height)
    if ((isNaN(sw) || sw === 0) && (isNaN(sh) || sh === 0)) {
      styles.width = '100%'
      styles.height = '100%'
      styles.left = '50%'
      styles.top = '50%'
      // 不要 translate(-50%,-50%)，因为 move 动画的 positionOverride 已经处理位置
      if (!state.positionOverride) {
        styles.transform = 'translate(-50%, -50%)'
      }
    }
  }

  return styles
}

// content 层样式：动画 transform + opacity
function getContentStyle(el) {
  const state = computeElementState(el, currentTime.value)
  return {
    opacity: state.opacity,
    transform: state.transform || 'translate(0, 0)'
  }
}

// 形状特有样式
function getShapeInfo(shapeName) {
  return getShapeRenderer(shapeName || 'rectangle')
}

function getShapeStyle(el) {
  const shapeName = el.content?.shape || 'rectangle'
  const { css } = getShapeRenderer(shapeName)
  const styles = { width: '100%', height: '100%', ...css }
  const fill = el.content?.fill || el.style?.backgroundColor
  if (fill && fill !== 'none') styles.backgroundColor = fill
  return styles
}

// SVG 形状的颜色：currentColor 需要通过 CSS color 属性设置
function getSVGColorStyle(el) {
  let color = el.content?.fill
  if (!color || color === 'none') {
    color = el.content?.stroke
  }
  if (!color || color === 'none') {
    color = el.style?.backgroundColor
  }
  return color ? { color } : {}
}

// 线条坐标（将百分比/像素坐标转为 viewBox 坐标）
function lineCoords(el) {
  const start = el.content?.startPoint || { x: 0, y: 0 }
  const end = el.content?.endPoint || { x: 100, y: 100 }
  return { x1: start.x, y1: start.y, x2: end.x, y2: end.y }
}

function lineViewBox(el) {
  const start = el.content?.startPoint || { x: 0, y: 0 }
  const end = el.content?.endPoint || { x: 100, y: 100 }
  return {
    w: Math.max(start.x, end.x) + 10,
    h: Math.max(start.y, end.y) + 10
  }
}

// ---- 播放控制 ----

function tick(timestamp) {
  if (!startTimestamp) startTimestamp = timestamp
  const elapsed = timestamp - startTimestamp + lastPausedTime
  const capped = Math.min(elapsed, totalDuration.value)
  currentTime.value = capped

  if (capped < totalDuration.value && isPlaying.value) {
    rafId = requestAnimationFrame(tick)
  } else if (capped >= totalDuration.value) {
    isPlaying.value = false
    rafId = null
  }
}

function play() {
  if (isPlaying.value) return
  isPlaying.value = true
  startTimestamp = 0
  rafId = requestAnimationFrame(tick)
}

function pause() {
  isPlaying.value = false
  lastPausedTime = currentTime.value
  startTimestamp = 0
  if (rafId) { cancelAnimationFrame(rafId); rafId = null }
}

function togglePlay() { isPlaying.value ? pause() : play() }

function replay() {
  pause()
  currentTime.value = 0
  lastPausedTime = 0
  startTimestamp = 0
  play()
}

function seekTo(time) {
  const wasPlaying = isPlaying.value
  if (wasPlaying) pause()
  currentTime.value = Math.max(0, Math.min(time, totalDuration.value))
  lastPausedTime = currentTime.value
  startTimestamp = 0
  if (wasPlaying) play()
}

// ---- 数据加载 ----

const effectiveData = computed(() => props.animationJson || props.animationData)

function loadAnimationData() {
  const data = effectiveData.value
  if (!data) { elementList.value = []; layoutOffsets.value = new Map(); return }
  config.value = data.globalConfig || { background: '#FFFFFF' }
  totalDuration.value = data.metadata?.totalDuration ||
    Math.max(...data.elements.map(el =>
      Math.max(...(el.animations || []).map(a => (a.startTime || 0) + (a.duration || 500)))
    ), 1000)
  elementList.value = data.elements || []

  // 自动避让：根据估算尺寸预计算偏移，让 text 与 shape/image 不重叠
  if (props.autoLayout && elementList.value.length) {
    layoutOffsets.value = computeAutoLayout(elementList.value)
  } else {
    layoutOffsets.value = new Map()
  }
}

// ---- 生命周期 ----

watch(() => props.animationJson || props.animationData, (newVal) => {
  if (newVal) {
    pause()
    currentTime.value = 0
    lastPausedTime = 0
    startTimestamp = 0
    loadAnimationData()
    setTimeout(() => play(), 100)
  }
}, { immediate: true })

onMounted(() => {
  if (effectiveData.value) {
    loadAnimationData()
    setTimeout(() => play(), 100)
  }
})

onUnmounted(() => { if (rafId) cancelAnimationFrame(rafId) })

// 模板直接引用 getElementStyle，这里给个别名
const getElementStyle = getWrapperStyle

// ---- 工具 ----

function formatTime(ms) { return (ms / 1000).toFixed(1) + 's' }

defineExpose({ play, pause, togglePlay, replay, seekTo, currentTime, isPlaying })
</script>

<style scoped>
.animation-viewer {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 12px;
  overflow: hidden;
  background: #0a0a1a;
}

.canvas {
  position: relative;
  width: 100%;
  height: 100%;
}

.anim-element {
  position: absolute;
  transform: translate(-50%, -50%);
}

.anim-content {
  display: block;
  width: 100%;
  height: 100%;
}

/* ✅ 文字：强制左对齐、半透衬底、层级置顶 */
.anim-text {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  font-size: clamp(13px, 2.4vw, 28px);
  color: #f0f0f0;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
  text-align: left;

  padding: 5px 12px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.68);
  backdrop-filter: blur(4px);

  min-height: 1.4em;
  max-width: 28%;

  z-index: 9999;
  box-sizing: border-box;
}

/* 图形、线条、图片层级低于文字 */
.anim-shape,
.anim-line,
.anim-image {
  z-index: 1;
}

.anim-shape {
  overflow: visible;
}

.shape-svg {
  display: flex;
  align-items: center;
  justify-content: center;
}
  .inner-text {
    word-break: break-word;
    white-space: pre-wrap;
  }
.inner-text {
  word-break: break-word;
  white-space: pre-wrap;
}

.inner-shape {
  box-sizing: border-box;
}

.is-rectangle {
  border-radius: 14px;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.12);
}

.shape-svg :deep(svg) {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

/* ---------- 动画keyframes ---------- */
@keyframes slideInTop {
  from {
    transform: translateY(-40px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes slideInLeft {
  from {
    transform: translateX(-40px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes slideInRight {
  from {
    transform: translateX(40px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes pulseAnim {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.slide-in-top {
  animation: slideInTop 0.6s ease-out forwards;
}

.slide-in-left {
  animation: slideInLeft 0.5s ease-out forwards;
}

.slide-in-right {
  animation: slideInRight 0.5s ease-out forwards;
}

.fade-in {
  animation: fadeIn 0.4s ease-in forwards;
}

.pulse-anim {
  animation: pulseAnim 1.2s ease-in-out;
}
  .slide-in-left {
    animation: slideInLeft 0.5s ease-out forwards;
  }

  .slide-in-right {
    animation: slideInRight 0.5s ease-out forwards;
  }

  .fade-in {
    animation: fadeIn 0.4s ease-in forwards;
  }

  .pulse-anim {
    animation: pulseAnim 1.2s ease-in-out;
  }
  </style>

