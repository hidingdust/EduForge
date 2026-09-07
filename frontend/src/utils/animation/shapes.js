/**
 * 形状渲染注册表 — 与后端 ElementContent.shape 对应
 * 后端支持: rectangle / circle / polygon
 * LLM 常生成: ellipse / spiral / arc / star / heart / diamond / triangle
 * 每种形状返回 { css: {...}, svg?: '<svg>...</svg>' }
 */

const registry = new Map()

// ===================== 基础形状 =====================

// 圆形
registry.set('circle', () => ({
  css: { borderRadius: '50%' }
}))

// 矩形
registry.set('rectangle', () => ({
  css: { borderRadius: '6px' }
}))

// 椭圆（与 circle 相同 CSS，当 size 宽高不等时自动呈现椭圆）
registry.set('ellipse', () => ({
  css: { borderRadius: '50%' }
}))

// 多边形（默认正六边形）
registry.set('polygon', () => ({
  css: { clipPath: 'polygon(50% 0, 100% 25%, 100% 75%, 50% 100%, 0 75%, 0 25%)' }
}))

// ===================== 扩展形状 =====================

// 圆角矩形
registry.set('rounded-rect', () => ({
  css: { borderRadius: '12px' }
}))

// 菱形
registry.set('diamond', () => ({
  css: { clipPath: 'polygon(50% 0, 100% 50%, 50% 100%, 0 50%)' }
}))

// 三角形
registry.set('triangle', () => ({
  css: { clipPath: 'polygon(50% 0, 100% 100%, 0 100%)' }
}))

// 星形（SVG）
registry.set('star', () => ({
  svg: `<svg viewBox="0 0 50 50" width="100%" height="100%">
    <polygon points="25,5 31,18 45,20 34,30 36,44 25,36 14,44 16,30 5,20 19,18"
      fill="currentColor" />
  </svg>`
}))

// 心形（SVG）
registry.set('heart', () => ({
  svg: `<svg viewBox="0 0 50 50" width="100%" height="100%">
    <path d="M25 45 L5 25 A12 12 0 0 1 25 15 A12 12 0 0 1 45 25 Z"
      fill="currentColor" />
  </svg>`
}))

// 螺旋星系（SVG）— 用于星系可视化
registry.set('spiral', () => ({
  svg: `<svg viewBox="0 0 100 100" width="100%" height="100%">
    <g fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
      <path d="M50,50 Q55,40 65,42 Q80,45 78,60 Q75,78 55,75 Q30,70 35,45 Q42,20 70,25 Q95,32 88,60" opacity="0.8"/>
      <path d="M50,50 Q45,60 35,58 Q20,55 22,40 Q25,22 45,25 Q70,30 65,55 Q58,80 30,75 Q5,68 12,40" opacity="0.5"/>
    </g>
    <circle cx="50" cy="50" r="4" fill="currentColor" opacity="0.9"/>
  </svg>`
}))

// 弧线（SVG）— 用于引力线、轨道线等
// 渲染为半圆弧，fill="none"，通过 stroke 显示
registry.set('arc', () => ({
  svg: `<svg viewBox="0 0 100 100" width="100%" height="100%" preserveAspectRatio="none">
    <path d="M5,50 A45,45 0 0,1 95,50"
      fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
  </svg>`
}))

/**
 * 注册自定义形状
 * @param {string} name     - 形状名称（对应 JSON "shape" 字段）
 * @param {function} factory - () => { css?: object, svg?: string }
 */
export function registerShape(name, factory) {
  if (typeof factory !== 'function') {
    console.warn(`[Shapes] "${name}" factory 必须是一个函数`)
    return
  }
  registry.set(name, factory)
}

/**
 * 获取形状渲染配置
 * @param {string} name
 * @returns {{ css: object, svg?: string }}
 */
export function getShapeRenderer(name) {
  const factory = registry.get(name)
  if (factory) return factory()
  // 未知形状默认矩形
  return { css: { borderRadius: '4px' } }
}

/**
 * 列出所有已注册形状
 */
export function listShapes() {
  return Array.from(registry.keys())
}
