/**
 * 缓动函数注册表
 * 新增缓动只需 registerEasing('name', fn)，渲染器零修改
 */

const registry = new Map()

// 内置缓动
registry.set('linear', (t) => t)
registry.set('ease-in', (t) => t * t)
registry.set('ease-out', (t) => 1 - Math.pow(1 - t, 2))
registry.set('ease-in-out', (t) => t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2)
registry.set('ease-out-cubic', (t) => 1 - Math.pow(1 - t, 3))
registry.set('ease-out-quart', (t) => 1 - Math.pow(1 - t, 4))

/**
 * 注册自定义缓动函数
 * @param {string} name - 缓动名称（对应 JSON easing 字段）
 * @param {function} fn - (progress: 0~1) => easedValue: 0~1
 */
export function registerEasing(name, fn) {
  if (typeof fn !== 'function') {
    console.warn(`[Easings] "${name}" 必须是一个函数`)
    return
  }
  registry.set(name, fn)
}

/**
 * 获取缓动函数
 * @param {string} name
 * @returns {function}
 */
export function getEasing(name) {
  return registry.get(name) || registry.get('linear')
}

/**
 * 列出所有已注册的缓动名称
 * @returns {string[]}
 */
export function listEasings() {
  return Array.from(registry.keys())
}
