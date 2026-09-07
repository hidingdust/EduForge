/**
 * 动画策略注册表 — 与后端 AnimationClip.type 一一对应
 * 后端支持: fadeIn / fadeOut / zoomIn / zoomOut / move / rotate / slideIn / slideOut
 * LLM 常生成: pulse / bounce / float / zoom / draw
 * 每种动画提供一个函数：(anim, progress, eased, element) => { opacity, transform?, positionOverride? }
 * 新增动画类型只需 registerAnimation('name', fn)，渲染器零修改
 */

import { getEasing } from './easings.js'

const registry = new Map()

// ===================== 入场动画 =====================

// slideIn：从指定方向滑入
// 支持方向: left / right / top / bottom / center / top-left / top-right / bottom-left / bottom-right
registry.set('slideIn', (anim, progress) => {
  const eased = getEasing(anim.easing || 'ease-out')(progress)
  const dir = anim.direction || 'left'
  let dx = 0, dy = 0

  if (dir === 'left')        dx = -60 * (1 - eased)
  else if (dir === 'right')  dx =  60 * (1 - eased)
  else if (dir === 'top')    dy = -60 * (1 - eased)
  else if (dir === 'bottom') dy =  60 * (1 - eased)
  else if (dir === 'center') {
    // 从中心放大入场
    const scale = 0.3 + 0.7 * eased
    return { opacity: Math.min(1, eased + 0.1), transform: `scale(${scale})` }
  }
  else if (dir === 'top-left')       { dx = -60 * (1 - eased); dy = -60 * (1 - eased) }
  else if (dir === 'top-right')      { dx =  60 * (1 - eased); dy = -60 * (1 - eased) }
  else if (dir === 'bottom-left')    { dx = -60 * (1 - eased); dy =  60 * (1 - eased) }
  else if (dir === 'bottom-right')   { dx =  60 * (1 - eased); dy =  60 * (1 - eased) }

  return {
    opacity: Math.min(1, eased + 0.1),
    transform: `translate(${dx}%, ${dy}%)`
  }
})

// fadeIn：淡入
registry.set('fadeIn', (anim, progress) => {
  const eased = getEasing(anim.easing || 'ease-in')(progress)
  return { opacity: eased, transform: 'translate(0, 0)' }
})

// zoomIn：缩放入场（从小到大）
registry.set('zoomIn', (anim, progress) => {
  const eased = getEasing(anim.easing || 'ease-out')(progress)
  const fromScale = (anim.customParams?.scale != null) ? anim.customParams.scale : 0
  const s = fromScale + (1 - fromScale) * eased
  return {
    opacity: Math.min(1, eased + 0.2),
    transform: `scale(${s})`
  }
})

// rotate：旋转动画（可用作入场或中间动作）
registry.set('rotate', (anim, progress) => {
  const eased = getEasing(anim.easing || 'ease-in-out')(progress)
  const angle = anim.customParams?.angle || 360
  return {
    opacity: 1,
    transform: `rotate(${angle * eased}deg)`
  }
})

// ===================== 退场动画 =====================

// fadeOut：淡出
registry.set('fadeOut', (anim, progress) => {
  const eased = getEasing(anim.easing || 'ease-out')(progress)
  return { opacity: 1 - eased, transform: 'translate(0, 0)' }
})

// slideOut：向指定方向滑出
registry.set('slideOut', (anim, progress) => {
  const eased = getEasing(anim.easing || 'ease-in')(progress)
  const dir = anim.direction || 'right'
  let dx = 0, dy = 0
  if (dir === 'left')        dx = -60 * eased
  else if (dir === 'right')  dx =  60 * eased
  else if (dir === 'top')    dy = -60 * eased
  else if (dir === 'bottom') dy =  60 * eased

  return {
    opacity: 1 - eased,
    transform: `translate(${dx}%, ${dy}%)`
  }
})

// zoomOut：缩放退场
registry.set('zoomOut', (anim, progress) => {
  const eased = getEasing(anim.easing || 'ease-in')(progress)
  const toScale = (anim.customParams?.scale != null) ? anim.customParams.scale : 0
  const s = 1 - (1 - toScale) * eased
  return {
    opacity: 1 - eased,
    transform: `scale(${s})`
  }
})

// ===================== 中间动作动画 =====================

// move：移动到目标位置
// 核心：通过返回 positionOverride 直接设置 wrapper 的 left/top，
// 因为 CSS transform 的 % 是相对元素自身，无法正确表示相对父容器的位移
registry.set('move', (anim, progress, eased, element) => {
  const pos = element?.position || {}
  const unit = pos.unit || '%'
  const startX = pos.x ?? 0
  const startY = pos.y ?? 0
  const targetX = anim.customParams?.targetX ?? startX
  const targetY = anim.customParams?.targetY ?? startY

  const currentX = startX + (targetX - startX) * eased
  const currentY = startY + (targetY - startY) * eased

  return {
    opacity: 1,
    transform: 'translate(0, 0)',
    positionOverride: {
      left: `${currentX}${unit}`,
      top: `${currentY}${unit}`
    }
  }
})

// zoom：缩放到指定比例（不淡出，区别于 zoomIn/zoomOut）
// LLM 常用 customParams.scale 指定目标缩放比例
registry.set('zoom', (anim, progress) => {
  const eased = getEasing(anim.easing || 'ease-in')(progress)
  const targetScale = anim.customParams?.scale ?? 1
  const s = 1 + (targetScale - 1) * eased
  return {
    opacity: 1,
    transform: `scale(${s})`
  }
})

// draw：绘制效果（用于弧线、路径等 SVG 元素）
// 简化实现：用 opacity 渐显模拟绘制过程
registry.set('draw', (anim, progress) => {
  const eased = getEasing(anim.easing || 'ease-out')(progress)
  return {
    opacity: eased,
    transform: 'translate(0, 0)'
  }
})

// ===================== LLM 常生成的动画（兼容兜底） =====================

// pulse：脉冲缩放
registry.set('pulse', (anim, progress) => {
  const scaleMin = anim.customParams?.scaleMin ?? 0.95
  const scaleMax = anim.customParams?.scaleMax ?? 1.05
  const scale = scaleMin + (scaleMax - scaleMin) * (0.5 + 0.5 * Math.sin(progress * Math.PI * 2))
  return { opacity: 1, transform: `scale(${scale})` }
})

// bounce：弹跳出现
registry.set('bounce', (anim, progress) => {
  const bounce = Math.abs(Math.sin(progress * Math.PI * 3)) * (1 - progress)
  return { opacity: progress >= 1 ? 1 : progress + 0.2, transform: `translateY(${-bounce * 30}px)` }
})

// float：轻微浮动
registry.set('float', (anim, progress) => {
  const y = Math.sin(progress * Math.PI * 2) * 10
  return { opacity: 1, transform: `translateY(${y}px)` }
})

// ==================== 注册表 API ====================

/**
 * 注册自定义动画策略
 * @param {string} name        - 动画名称（对应 JSON "type" 字段）
 * @param {function} handler   - (anim, progress, eased, element) => { opacity, transform?, positionOverride? }
 */
export function registerAnimation(name, handler) {
  if (typeof handler !== 'function') {
    console.warn(`[Animations] "${name}" handler 必须是一个函数`)
    return
  }
  registry.set(name, handler)
}

// 兜底：让未知动画类型至少能正常显示，不刷屏警告
const fallbackHandler = (anim, progress) => ({
  opacity: progress >= 1 ? 1 : Math.min(1, progress + 0.2),
  transform: 'translate(0, 0)'
})

/**
 * 获取动画策略
 * @param {string} name
 * @returns {function}
 */
export function getAnimation(name) {
  return registry.get(name) || fallbackHandler
}

/**
 * 计算某个元素在当前时刻的动画状态
 * 支持多动画同时叠加（如 move + rotate 并行）
 *
 * 合并策略：
 * - opacity: 取所有活跃动画中的最小值
 * - transform: 收集所有非零 transform 并拼接（如 "rotate(180deg) scale(0.8)"）
 * - positionOverride: 取最后一个提供 positionOverride 的动画结果
 * - 已完成的 move/rotate/zoom 保持终态，与新活跃动画叠加
 *
 * @param {object} element      - JSON 元素（完整对象，含 position/style/animations）
 * @param {number} currentTime  - 当前时间（ms）
 * @returns {{ opacity: number, transform: string, positionOverride?: object }}
 */
export function computeElementState(element, currentTime) {
  if (!element.animations || element.animations.length === 0) {
    return { opacity: 1, transform: 'translate(0, 0)' }
  }

  // 分类：当前活跃的 + 已完成的
  const activeAnims = []
  const completedAnims = []

  for (const anim of element.animations) {
    const start = anim.startTime || 0
    const dur = anim.duration || 500
    if (currentTime >= start && currentTime <= start + dur) {
      activeAnims.push(anim)
    } else if (currentTime > start + dur) {
      completedAnims.push(anim)
    }
  }

  // 还没开始任何动画 → 不可见
  if (activeAnims.length === 0 && completedAnims.length === 0) {
    return { opacity: 0, transform: 'translate(0, 0)' }
  }

  // 没有活跃动画 → 取最后一个已完成动画的终态
  if (activeAnims.length === 0) {
    const last = completedAnims[completedAnims.length - 1]
    const handler = getAnimation(last.type)
    const result = handler(last, 1, 1, element)
    return {
      opacity: result.opacity ?? 1,
      transform: result.transform || 'translate(0, 0)',
      positionOverride: result.positionOverride || null
    }
  }

  // ---- 有活跃动画：合并所有活跃 + 已完成动画的结果 ----

  let opacity = 1
  const transforms = []
  let positionOverride = null

  // 先处理已完成的动画，保持终态（move 停在目标位置、rotate 保持角度、zoom 保持缩放）
  for (const anim of completedAnims) {
    const handler = getAnimation(anim.type)
    const result = handler(anim, 1, 1, element)

    if (result.opacity != null && result.opacity < 1) {
      opacity = Math.min(opacity, result.opacity)
    }
    if (result.transform && result.transform !== 'translate(0, 0)') {
      transforms.push(result.transform)
    }
    if (result.positionOverride) {
      positionOverride = result.positionOverride
    }
  }

  // 再处理当前活跃的动画，叠加在上
  for (const anim of activeAnims) {
    const start = anim.startTime || 0
    const dur = anim.duration || 500
    const progress = Math.max(0, Math.min(1, (currentTime - start) / dur))
    const eased = getEasing(anim.easing || 'linear')(progress)

    const handler = getAnimation(anim.type)
    const result = handler(anim, progress, eased, element)

    if (result.opacity != null) {
      opacity = Math.min(opacity, result.opacity)
    }
    if (result.transform && result.transform !== 'translate(0, 0)') {
      transforms.push(result.transform)
    }
    if (result.positionOverride) {
      positionOverride = result.positionOverride
    }
  }

  return {
    opacity,
    transform: transforms.length > 0 ? transforms.join(' ') : 'translate(0, 0)',
    positionOverride
  }
}

/**
 * 列出所有已注册的动画类型
 */
export function listAnimations() {
  return Array.from(registry.keys())
}
