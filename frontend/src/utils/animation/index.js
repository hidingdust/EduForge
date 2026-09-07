/**
 * 动画引擎统一入口
 *
 * 用法：
 *   import {
 *     registerEasing, registerAnimation, registerShape,
 *     computeElementState, getShapeRenderer, computeElementCSS,
 *     getPositionStyle
 *   } from '@/utils/animation'
 */

// 注册表（可动态扩展）
export { registerEasing, getEasing, listEasings } from './easings.js'
export { registerAnimation, getAnimation, listAnimations, computeElementState } from './animations.js'
export { registerShape, getShapeRenderer, listShapes } from './shapes.js'

// 样式引擎
export { computeElementCSS, getPositionStyle } from './styleEngine.js'
