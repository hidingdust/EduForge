/**
 * 动画数据示例 — 生物的基本特征
 * 
 * 用法：
 *   import { animationData } from '@/utils/animationData.js'
 *   <AnimationViewer :animationJson="animationData" />
 * 
 * 你的业务中可以替换为 API 返回的 JSON：
 *   const animationData = await fetch('/api/animation/xxx').then(r => r.json())
 */
export const animationData = {
  version: '1.0',
  globalConfig: {
    background: '#FFFFFF',
    fps: 30,
    loop: false
  },
  metadata: {
    title: '生物基本特征',
    description: '七条核心生命特征可视化演示',
    totalDuration: 8000
  },
  elements: [
    {
      elementId: 'title_text',
      type: 'text',
      content: { text: '生物的基本特征' },
      position: { unit: '%', x: 50, y: 12 },
      size: { unit: '%', width: 60, height: 100 },
      style: {},
      animations: [
        { type: 'slideIn', direction: 'top', duration: 600, easing: 'ease-out', startTime: 0 }
      ]
    },
    {
      elementId: 'icon_nutrition',
      type: 'shape',
      content: { shape: 'circle', fill: '#10b981' },
      position: { unit: '%', x: 5, y: 25 },
      size: { unit: '%', width: 6, height: 6 },
      style: { backgroundColor: '#10b981', opacity: 1 },
      animations: [
        { type: 'slideIn', direction: 'left', duration: 500, easing: 'ease-out', startTime: 800 }
      ]
    },
    {
      elementId: 'feature_1',
      type: 'text',
      content: { text: '1. 需营养：植物光合作用制造有机物，动物直接或间接依赖植物' },
      position: { unit: '%', x: 15, y: 25 },
      size: { unit: '%', width: 70, height: 100 },
      style: {},
      animations: [
        { type: 'slideIn', direction: 'left', duration: 500, easing: 'ease-out', startTime: 800 }
      ]
    },
    {
      elementId: 'icon_respiration',
      type: 'shape',
      content: { shape: 'circle', fill: '#3b82f6' },
      position: { unit: '%', x: 5, y: 33 },
      size: { unit: '%', width: 6, height: 6 },
      style: { backgroundColor: '#3b82f6', opacity: 1 },
      animations: [
        { type: 'slideIn', direction: 'left', duration: 500, easing: 'ease-out', startTime: 1600 }
      ]
    },
    {
      elementId: 'feature_2',
      type: 'text',
      content: { text: '2. 能呼吸：多数生物吸氧、呼二氧化碳' },
      position: { unit: '%', x: 15, y: 33 },
      size: { unit: '%', width: 70, height: 100 },
      style: {},
      animations: [
        { type: 'slideIn', direction: 'left', duration: 500, easing: 'ease-out', startTime: 1600 }
      ]
    },
    {
      elementId: 'icon_excretion',
      type: 'shape',
      content: { shape: 'circle', fill: '#f59e0b' },
      position: { unit: '%', x: 5, y: 41 },
      size: { unit: '%', width: 6, height: 6 },
      style: { backgroundColor: '#f59e0b', opacity: 1 },
      animations: [
        { type: 'slideIn', direction: 'left', duration: 500, easing: 'ease-out', startTime: 2400 }
      ]
    },
    {
      elementId: 'feature_3',
      type: 'text',
      content: { text: '3. 能排泄：人体通过尿液、汗液、呼气排出废物；植物通过落叶排废' },
      position: { unit: '%', x: 15, y: 41 },
      size: { unit: '%', width: 70, height: 100 },
      style: {},
      animations: [
        { type: 'slideIn', direction: 'left', duration: 500, easing: 'ease-out', startTime: 2400 }
      ]
    },
    {
      elementId: 'icon_irritability',
      type: 'shape',
      content: { shape: 'circle', fill: '#8b5cf6' },
      position: { unit: '%', x: 5, y: 49 },
      size: { unit: '%', width: 6, height: 6 },
      style: { backgroundColor: '#8b5cf6', opacity: 1 },
      animations: [
        { type: 'slideIn', direction: 'left', duration: 500, easing: 'ease-out', startTime: 3200 }
      ]
    },
    {
      elementId: 'feature_4',
      type: 'text',
      content: { text: '4. 能应激：含羞草受触合拢、植物向光生长' },
      position: { unit: '%', x: 15, y: 49 },
      size: { unit: '%', width: 70, height: 100 },
      style: {},
      animations: [
        { type: 'slideIn', direction: 'left', duration: 500, easing: 'ease-out', startTime: 3200 }
      ]
    },
    {
      elementId: 'icon_growth',
      type: 'shape',
      content: { shape: 'circle', fill: '#06b6d4' },
      position: { unit: '%', x: 5, y: 57 },
      size: { unit: '%', width: 6, height: 6 },
      style: { backgroundColor: '#06b6d4', opacity: 1 },
      animations: [
        { type: 'slideIn', direction: 'left', duration: 500, easing: 'ease-out', startTime: 4000 }
      ]
    },
    {
      elementId: 'feature_5',
      type: 'text',
      content: { text: '5. 能生长与繁殖' },
      position: { unit: '%', x: 15, y: 57 },
      size: { unit: '%', width: 70, height: 100 },
      style: {},
      animations: [
        { type: 'slideIn', direction: 'left', duration: 500, easing: 'ease-out', startTime: 4000 }
      ]
    },
    {
      elementId: 'icon_heredity',
      type: 'shape',
      content: { shape: 'circle', fill: '#ef4444' },
      position: { unit: '%', x: 5, y: 65 },
      size: { unit: '%', width: 6, height: 6 },
      style: { backgroundColor: '#ef4444', opacity: 1 },
      animations: [
        { type: 'slideIn', direction: 'left', duration: 500, easing: 'ease-out', startTime: 4800 }
      ]
    },
    {
      elementId: 'feature_6',
      type: 'text',
      content: { text: '6. 具遗传与变异：亲子相似为遗传，差异为变异' },
      position: { unit: '%', x: 15, y: 65 },
      size: { unit: '%', width: 70, height: 100 },
      style: {},
      animations: [
        { type: 'slideIn', direction: 'left', duration: 500, easing: 'ease-out', startTime: 4800 }
      ]
    },
    {
      elementId: 'icon_cell',
      type: 'shape',
      content: { shape: 'circle', fill: '#84cc16' },
      position: { unit: '%', x: 5, y: 73 },
      size: { unit: '%', width: 6, height: 6 },
      style: { backgroundColor: '#84cc16', opacity: 1 },
      animations: [
        { type: 'slideIn', direction: 'left', duration: 500, easing: 'ease-out', startTime: 5600 }
      ]
    },
    {
      elementId: 'feature_7',
      type: 'text',
      content: { text: '7. 除病毒外，均由细胞构成' },
      position: { unit: '%', x: 15, y: 73 },
      size: { unit: '%', width: 70, height: 100 },
      style: {},
      animations: [
        { type: 'slideIn', direction: 'left', duration: 500, easing: 'ease-out', startTime: 5600 }
      ]
    },
    {
      elementId: 'summary_box',
      type: 'shape',
      content: { shape: 'rounded-rect', fill: '#f9fafb' },
      position: { unit: '%', x: 20, y: 82 },
      size: { unit: '%', width: 60, height: 12 },
      style: { backgroundColor: '#f9fafb', opacity: 0.95 },
      animations: [
        { type: 'slideIn', direction: 'bottom', duration: 600, easing: 'ease-out', startTime: 6400 }
      ]
    },
    {
      elementId: 'summary_text',
      type: 'text',
      content: { text: '七大特征共同定义生命现象' },
      position: { unit: '%', x: 50, y: 86 },
      size: { unit: '%', width: 50, height: 100 },
      style: {},
      animations: [
        { type: 'fadeIn', duration: 400, easing: 'ease-in-out', startTime: 6600 }
      ]
    }
  ]
}
