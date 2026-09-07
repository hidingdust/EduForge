/**
 * 样式引擎：将 JSON 元素映射为 CSS
 * 后端 Size DTO 中 width/height 为 String 类型（"auto" 已被 Validator 替换为 100）
 * 后端 Position/Size 都带 unit 字段（"%" | "px"）
 */

/**
 * 将 JSON style 对象转换为 CSS 属性
 */
export function computeElementCSS(element) {
  const style = element.style || {}
  const css = {}

  if (style.color) css.color = style.color
  if (style.fontSize) css.fontSize = style.fontSize
  if (style.fontWeight) css.fontWeight = style.fontWeight
  if (style.textAlign) css.textAlign = style.textAlign
  if (style.backgroundColor) css.backgroundColor = style.backgroundColor
  if (style.borderColor) css.borderColor = style.borderColor
  if (style.borderWidth != null) css.borderWidth = typeof style.borderWidth === 'number' ? `${style.borderWidth}px` : style.borderWidth
  if (style.opacity != null) css.opacity = style.opacity
  if (style.boxShadow) css.boxShadow = style.boxShadow
  if (style.padding) css.padding = style.padding

  return css
}

/**
 * 获取元素的位置样式（wrapper 层静态定位）
 * 支持 % 和 px 两种单位
 */
export function getPositionStyle(element) {
  const pos = element.position || { x: 0, y: 0 }
  const size = element.size || { width: 100, height: 100 }
  const posUnit = pos.unit || '%'
  const sizeUnit = size.unit || '%'

  // 后端 Size DTO 中 width/height 为 String，Validator 会把 "auto" 替换为 100
  // 用 isNaN 判断而非 || 兜底，避免 size 0 被错误替换为 100
  const parsedW = parseFloat(size.width)
  const parsedH = parseFloat(size.height)
  const w = isNaN(parsedW) ? 100 : parsedW
  const h = isNaN(parsedH) ? 100 : parsedH

  return {
    left: `${pos.x}${posUnit}`,
    top: `${pos.y}${posUnit}`,
    width: `${w}${sizeUnit}`,
    height: `${h}${sizeUnit}`
  }
}
