<!--
  PresentationViewer.vue — 全屏PPT演示模式
  将 PPT页面 + 动画课件 + 问答题目 整合为连续幻灯片流
  支持：点击翻页、飞入效果、动画播放、点击显答、PPTX下载
-->
<template>
  <div class="presentation-root" :style="{ background: slideBackground }">
    <!-- 工具栏：和预览页布局统一，左返回、右操作 -->
    <div class="pres-toolbar">
      <button class="back-home-btn" @click="$emit('close')">← 返回首页</button>
      <div class="top-spacer"></div>
      <div class="pres-toolbar-right">
        <span class="pres-page-num">{{ currentSlideIdx + 1 }} / {{ slides.length }}</span>
        <button class="pres-tool-btn" @click.stop="downloadPPTX">下载PPTX</button>
        <button v-if="hasAnimation" class="pres-tool-btn" @click.stop="downloadVideo">
          {{ isRecording ? '录制中...' : '下载视频' }}
        </button>
        <button class="pres-tool-btn" @click.stop="toggleFullscreen">
          {{ isFullscreen ? '退出全屏' : '全屏' }}
        </button>
      </div>
    </div>

    <!-- 幻灯片区 -->
    <div class="pres-stage" ref="stageRef" @click="handleStageClick">
      <!-- ====== PPT文本页 ====== -->
      <div v-if="currentSlide.type === 'ppt'" class="pres-slide ppt-slide">
        <!-- 渐变艺术字标题：卡片上方居中，和预览页效果完全一致 -->
        <h1 class="art-title">{{ parsedPpt?.mainTitle || '教学课件' }}</h1>

        <div class="ppt-card">
          <div class="ppt-accent-bar" :style="{ background: currentPPT.titleColor || mainColor }"></div>
          <div class="ppt-card-inner">
            <div class="ppt-head">
              <h2 class="ppt-heading" :style="{ color: currentPPT.titleColor || mainColor }">
                {{ currentPPT.pageTitle }}
              </h2>
              <p v-if="pptSubtitle" class="ppt-subtitle">{{ pptSubtitle }}</p>
            </div>

            <div class="ppt-card-body">
              <!-- 双栏对比 -->
              <div v-if="isTwoColumn" class="ppt-compare">
                <div
                  v-for="(section, sIdx) in pptSections"
                  :key="sIdx"
                  class="ppt-compare-card"
                  :class="'compare-' + (sIdx === 0 ? 'left' : 'right')"
                >
                  <div class="ppt-compare-title" :style="{ color: sIdx === 0 ? '#ea580c' : '#16a34a' }">
                    {{ section.title }}
                  </div>
                  <ul class="ppt-compare-list">
                    <li v-for="(item, iIdx) in section.items" :key="iIdx">{{ item }}</li>
                  </ul>
                </div>
              </div>

              <!-- 普通列表 -->
              <div v-else class="ppt-list">
                <div v-for="(section, sIdx) in pptSections" :key="sIdx" class="ppt-section">
                  <div v-if="section.title" class="ppt-section-title">{{ section.title }}</div>
                  <ul>
                    <li v-for="(item, iIdx) in section.items" :key="iIdx">{{ item }}</li>
                  </ul>
                </div>
                <div v-if="!pptSections.length" class="ppt-empty">本页暂无内容</div>
              </div>

              <div v-if="pptConclusion" class="ppt-conclusion" :style="{ background: currentPPT.titleColor || mainColor }">
                <span>{{ pptConclusion }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="ppt-hint" v-if="hasNextSlide">点击任意位置继续 →</div>
      </div>

      <!-- ====== 动画课件页 ====== -->
      <div v-else-if="currentSlide.type === 'animation'" class="pres-slide anim-slide">
        <div class="anim-container" :class="{ recording: isRecording }">
          <AnimationViewer ref="animViewerRef" :animation-data="businessData?.animation_json" />
        </div>
        <div class="pres-hint">{{ animFinished ? '点击任意位置继续 →' : '动画播放中...' }}</div>
      </div>

      <!-- ====== 问答题页 ====== -->
      <div v-else-if="currentSlide.type === 'quiz'" class="pres-slide quiz-slide">
        <div class="quiz-bar" :style="{ background: mainColor }"></div>
        <div class="quiz-body">
          <div class="quiz-number">第 {{ currentQuizIndex + 1 }} 题</div>
          <div class="quiz-question">{{ currentQuiz.question }}</div>
          <div class="quiz-options" v-if="!quizRevealed">
            <div
              v-for="opt in ['A','B','C','D']"
              :key="opt"
              class="quiz-option"
              :class="{ picked: quizPicked === opt }"
              @click.stop="pickQuizOption(opt)"
            >
              <span class="quiz-opt-letter">{{ opt }}</span>
              <span>{{ currentQuiz[opt] }}</span>
            </div>
          </div>
          <div class="quiz-answer" v-if="quizRevealed">
            <div class="quiz-answer-banner" :class="quizCorrect ? 'correct' : 'wrong'">
              {{ quizCorrect ? '✅ 回答正确！' : '❌ 回答错误' }}
            </div>
            <div class="quiz-options-reveal">
              <div
                v-for="opt in ['A','B','C','D']"
                :key="opt"
                class="quiz-option static"
                :class="{
                  right: opt === currentQuiz.answer,
                  wrong: quizPicked === opt && opt !== currentQuiz.answer
                }"
              >
                <span class="quiz-opt-letter">{{ opt }}</span>
                <span>{{ currentQuiz[opt] }}</span>
              </div>
            </div>
            <div class="quiz-correct-answer">标准答案：{{ currentQuiz.answer }}</div>
          </div>
        </div>
        <div class="pres-hint" v-if="!quizRevealed">点击选项作答</div>
        <div class="pres-hint" v-else>点击任意位置继续 →</div>
      </div>

      <!-- ====== 问答开始页 ====== -->
      <div v-else-if="currentSlide.type === 'quiz-intro'" class="pres-slide quiz-intro-slide">
        <div class="quiz-intro-card">
          <div class="quiz-intro-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <polyline points="12 6 12 12 16 14"></polyline>
            </svg>
          </div>
          <h2 class="art-title purple">随堂测验</h2>
          <p class="quiz-intro-subtitle">检测你的学习成果</p>
          <div class="quiz-intro-stats">
            <div class="quiz-intro-stat">
              <span class="quiz-stat-icon">📝</span>
              <span class="quiz-stat-value">{{ quizStats.total }} 道题</span>
            </div>
            <div class="quiz-intro-stat">
              <span class="quiz-stat-icon">⭐</span>
              <span class="quiz-stat-value">共 {{ quizStats.total * 10 }} 分</span>
            </div>
          </div>
          <button class="quiz-start-btn" @click.stop="startQuiz">
            开始答题 <span class="quiz-start-arrow">›</span>
          </button>
        </div>
        <div class="pres-hint">点击按钮开始答题</div>
      </div>

      <!-- ====== 问答完成页 ====== -->
      <div v-else-if="currentSlide.type === 'quiz-result'" class="pres-slide quiz-result-slide">
        <div class="quiz-result-card">
          <div class="quiz-result-trophy">🏆</div>
          <div class="quiz-result-badge">课程完成</div>
          <h2 class="art-title gold">{{ parsedPpt?.mainTitle || '课程学习' }}</h2>
          <div class="quiz-result-date">{{ todayText }}</div>
          <div class="quiz-result-stats">
            <div class="quiz-result-stat">
              <div class="quiz-result-num">{{ parsedPpt?.pages?.length || 0 }}</div>
              <div class="quiz-result-label">页</div>
            </div>
            <div class="quiz-result-stat">
              <div class="quiz-result-num">{{ quizStats.total }}</div>
              <div class="quiz-result-label">小题</div>
            </div>
            <div class="quiz-result-stat">
              <div class="quiz-result-num">{{ quizStats.correct }}</div>
              <div class="quiz-result-label">答对</div>
            </div>
          </div>
          <div class="quiz-result-rate" :class="{ pass: quizStats.rate >= 60 }">
            <div class="quiz-rate-ring">
              <span class="quiz-rate-num">{{ quizStats.rate }}</span>%
            </div>
            <div class="quiz-rate-text">
              {{ quizStats.rate >= 80 ? '太棒了！掌握得很好' : quizStats.rate >= 60 ? '继续加油，还可以更好' : '万事开头难，回去再练练吧' }}
            </div>
          </div>
          <div class="quiz-result-feedback">
            <div class="quiz-feedback-title">你觉得这门课怎么样？</div>
            <div class="quiz-feedback-btns">
              <button class="quiz-feedback-btn like" @click.stop>
                <span>👍</span> 点赞
              </button>
              <button class="quiz-feedback-btn dislike" @click.stop>
                <span>👎</span> 吐槽
              </button>
            </div>
          </div>
        </div>
        <div class="pres-hint">点击任意位置重新学习</div>
      </div>
    </div>

    <!-- 底部进度条 -->
    <div class="pres-progress">
      <div class="pres-progress-bar" :style="{ width: progressPercent + '%' }"></div>
    </div>

    <!-- 键盘导航提示 -->
    <div class="pres-key-hint">← → 键翻页 | 空格键 下一页</div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import AnimationViewer from '@/components/AnimationViewer.vue'
import PptxGenJS from 'pptxgenjs'
import html2canvas from 'html2canvas'

const props = defineProps({
  businessData: { type: Object, default: null },
  parsedPpt: { type: Object, default: null }
})

const emit = defineEmits(['close', 'download'])

// ============ 统一幻灯片列表 ============
const slides = computed(() => {
  const list = []
  // PPT 页面
  if (props.parsedPpt?.pages && Array.isArray(props.parsedPpt.pages)) {
    props.parsedPpt.pages.forEach((page, idx) => {
      list.push({ type: 'ppt', pageIndex: idx })
    })
  }
  // 动画页
  if (props.businessData?.animation_json?.elements?.length) {
    list.push({ type: 'animation' })
  }
  // 问答题
  const questions = props.businessData?.questions
  if (questions?.length && Array.isArray(questions)) {
    list.push({ type: 'quiz-intro' })
    questions.forEach((q, idx) => {
      list.push({ type: 'quiz', questionIndex: idx })
    })
    list.push({ type: 'quiz-result' })
  }
  return list
})

const mainColor = computed(() => props.parsedPpt?.mainColor || '#409eff')
const hasAnimation = computed(() => !!props.businessData?.animation_json?.elements?.length)
const todayText = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()}`
})

// ============ 导航状态 ============
const currentSlideIdx = ref(0)
const isFullscreen = ref(false)
const isRecording = ref(false)
const animViewerRef = ref(null)

const currentSlide = computed(() => slides.value[currentSlideIdx.value] || { type: 'ppt', pageIndex: 0 })

// PPT 页
const currentPPT = computed(() => {
  if (currentSlide.value.type !== 'ppt') return {}
  return props.parsedPpt?.pages?.[currentSlide.value.pageIndex] || {}
})

const pptLayout = computed(() => parsePageContent(currentPPT.value.pageContent))
const pptSubtitle = computed(() => pptLayout.value.subtitle)
const pptSections = computed(() => pptLayout.value.sections)
const pptConclusion = computed(() => pptLayout.value.conclusion)
const isTwoColumn = computed(() => pptSections.value.length === 2 && pptSections.value.every(s => s.items.length > 0))
const hasNextSlide = computed(() => currentSlideIdx.value < slides.value.length - 1)

// 动画页
const animFinished = ref(false)

// 问答题
const currentQuizIndex = computed(() => currentSlide.value.questionIndex ?? 0)
const currentQuiz = computed(() => props.businessData?.questions?.[currentQuizIndex.value] || {})
const quizRevealed = ref(false)
const quizPicked = ref(null)
const quizCorrect = computed(() => quizPicked.value === currentQuiz.value.answer)
const quizResults = ref([]) // 每题是否正确

// 统计
const quizStats = computed(() => {
  const total = props.businessData?.questions?.length || 0
  const correct = quizResults.value.filter(Boolean).length
  const wrong = quizResults.value.filter(r => r === false).length
  const rate = total ? Math.round((correct / total) * 100) : 0
  return { total, correct, wrong, rate }
})

// 背景色
const slideBackground = computed(() => {
  if (currentSlide.value.type === 'animation') {
    return props.businessData?.animation_json?.globalConfig?.background || '#0a0a1a'
  }
  return '#f6f7fb'
})

const progressPercent = computed(() => {
  if (slides.value.length <= 1) return 0
  return (currentSlideIdx.value / (slides.value.length - 1)) * 100
})

// ============ 点击翻页逻辑 ============
function handleStageClick(e) {
  const t = currentSlide.value.type
  if (t === 'ppt' || t === 'animation') {
    goNext()
  } else if (t === 'quiz') {
    if (quizRevealed.value) goNext()
  } else if (t === 'quiz-intro') {
    // 仅点击按钮进入，点击空白处不翻页
    if (e.target?.closest?.('.quiz-start-btn')) startQuiz()
  } else if (t === 'quiz-result') {
    // 点击结束页回到课件开头
    restartPresentation()
  }
}

function startQuiz() {
  quizResults.value = new Array(props.businessData?.questions?.length || 0).fill(null)
  goNext()
}

function restartPresentation() {
  currentSlideIdx.value = 0
  prepareSlide(slides.value[0])
  quizResults.value = []
}

function goNext() {
  if (currentSlideIdx.value < slides.value.length - 1) {
    const next = slides.value[currentSlideIdx.value + 1]
    prepareSlide(next)
    currentSlideIdx.value++
  }
}

function goPrev() {
  if (currentSlideIdx.value > 0) {
    const prev = slides.value[currentSlideIdx.value - 1]
    prepareSlide(prev)
    currentSlideIdx.value--
  }
}

function prepareSlide(slide) {
  quizRevealed.value = false
  quizPicked.value = null
  animFinished.value = false
}

// ============ PPT 内容解析 ============
/** 工具：彻底清除所有HTML标签，避免<strong>等标签原样显示 */
function stripHtmlTags(str) {
  if (!str) return ''
  return String(str).replace(/<[^>]+>/g, '').replace(/\*\*(.*?)\*\*/g, '$1')
}

function parsePageContent(text) {
  const lines = (text || '').split('\n').map(l => l.trim()).filter(l => l)
  if (!lines.length) return { subtitle: '', sections: [], conclusion: '' }

  let subtitle = ''
  let startIdx = 0
  const first = stripHtmlTags(lines[0])
  // 首行若是概括性短句，视为副标题
  if (first.length <= 48 && !isBulletLine(first)) {
    subtitle = first
    startIdx = 1
  }

  const sections = []
  let current = null
  for (let i = startIdx; i < lines.length; i++) {
    const raw = lines[i]
    const clean = stripHtmlTags(raw)
    if (isBulletLine(raw)) {
      if (!current) current = { title: '', items: [] }
      current.items.push(stripBullet(clean))
    } else {
      if (current) sections.push(current)
      current = { title: clean, items: [] }
    }
  }
  if (current) sections.push(current)

  let conclusion = ''
  if (sections.length && /^(结论|总结|归纳)/.test(sections[sections.length - 1].title)) {
    conclusion = sections.pop().title
  }

  return { subtitle, sections, conclusion }
}

function isBulletLine(line) {
  return /^[\s]*[•\-\*\d][\.\、]?\s+/.test(line)
}

function stripBullet(str) {
  return str.replace(/^[\s]*[•\-\*\d][\.\、]?\s*/, '').trim()
}

// PPTX 专用内容解析（纯文本）
function parsePageContentPptx(text) {
  const lines = (text || '').split('\n').map(l => l.trim()).filter(l => l)
  if (!lines.length) return { subtitle: '', sections: [], conclusion: '' }

  let subtitle = ''
  let startIdx = 0
  const first = stripHtmlTags(lines[0])
  if (first.length <= 48 && !isBulletLine(first)) {
    subtitle = first
    startIdx = 1
  }

  const sections = []
  let current = null
  for (let i = startIdx; i < lines.length; i++) {
    const raw = lines[i]
    const clean = stripHtmlTags(raw)
    if (isBulletLine(raw)) {
      if (!current) current = { title: '', items: [] }
      current.items.push(clean.replace(/^[\s]*[•\-\*\d][\.\、]?\s*/, '').trim())
    } else {
      if (current) sections.push(current)
      current = { title: clean, items: [] }
    }
  }
  if (current) sections.push(current)

  let conclusion = ''
  if (sections.length && /^(结论|总结|归纳)/.test(sections[sections.length - 1].title)) {
    conclusion = sections.pop().title
  }

  return { subtitle, sections, conclusion }
}

function pickQuizOption(opt) {
  if (quizRevealed.value) return
  quizPicked.value = opt
  quizRevealed.value = true
  quizResults.value[currentQuizIndex.value] = opt === currentQuiz.value.answer
}

// ============ 键盘导航 ============
function handleKeyDown(e) {
  if (e.key === 'ArrowRight' || e.key === ' ') {
    e.preventDefault()
    if (currentSlide.value.type === 'quiz' && !quizRevealed.value) return
    handleStageClick()
  } else if (e.key === 'ArrowLeft') {
    e.preventDefault()
    goPrev()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
  prepareSlide(slides.value[0] || {})
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})

// 动画完成追踪
watch(currentSlideIdx, (idx) => {
  if (currentSlide.value.type === 'animation') {
    animFinished.value = false
    const dur = props.businessData?.animation_json?.metadata?.totalDuration || 8000
    setTimeout(() => { animFinished.value = true }, dur + 500)
  }
})

// 全屏切换
function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(() => {})
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

// ============ PPTX 下载（仅保留PPT内容，无问答） ============
const PPT_W = 10
const PPT_H = 5.625

async function downloadPPTX() {
  try {
    const pptx = new PptxGenJS()
    pptx.title = props.parsedPpt?.mainTitle || '教学课件'
    pptx.layout = 'LAYOUT_16x9'
    const mc = mainColor.value.replace('#', '')

    // ========== 封面 ==========
    const cover = pptx.addSlide()
    cover.background = { color: 'FFFFFF' }
    cover.addShape('rect', { x: 0, y: 0, w: PPT_W, h: 0.08, fill: { color: mc } })
    cover.addText(props.parsedPpt?.mainTitle || '教学课件', {
      x: 1, y: 1.8, w: 8, h: 1.2, fontSize: 40, bold: true, color: mc, align: 'center', valign: 'middle'
    })
    if (props.parsedPpt?.subtitle) {
      cover.addText(props.parsedPpt.subtitle, {
        x: 1, y: 3.0, w: 8, h: 0.6, fontSize: 20, color: '888888', align: 'center'
      })
    }
    cover.addText(new Date().toLocaleDateString('zh-CN'), {
      x: 1, y: 4.8, w: 8, h: 0.4, fontSize: 12, color: 'AAAAAA', align: 'center'
    })

    // ========== PPT 文本页 ==========
    if (props.parsedPpt?.pages && Array.isArray(props.parsedPpt.pages)) {
      props.parsedPpt.pages.forEach((page) => {
        const s = pptx.addSlide()
        s.background = { color: 'FFFFFF' }
        const titleColor = (page.titleColor || mc).replace('#', '')
        // 左侧色条
        s.addShape('rect', { x: 0, y: 0, w: 0.2, h: PPT_H, fill: { color: titleColor } })
        // 标题
        s.addText(page.pageTitle || '', {
          x: 0.7, y: 0.35, w: 8.8, h: 0.7, fontSize: 28, bold: true, color: titleColor, valign: 'middle'
        })

        const parsed = parsePageContentPptx(page.pageContent)
        let cursorY = 1.15

        // 副标题
        if (parsed.subtitle) {
          s.addText(parsed.subtitle, {
            x: 0.8, y: cursorY, w: 8.4, h: 0.4, fontSize: 14, color: '888888', valign: 'middle'
          })
          cursorY += 0.5
        }

        // 双栏对比
        if (parsed.sections.length === 2 && parsed.sections.every(sec => sec.items.length > 0)) {
          parsed.sections.forEach((sec, si) => {
            const colX = si === 0 ? 0.7 : 5.1
            const cardColor = si === 0 ? 'EA580C' : '16A34A'
            const cardBg = si === 0 ? 'FFF7ED' : 'F0FDF4'
            s.addShape('roundRect', {
              x: colX, y: cursorY, w: 4.2, h: 3.1, fill: { color: cardBg },
              rectRadius: 0.1, line: { color: cardColor, width: 1 }
            })
            s.addText(sec.title || '', {
              x: colX + 0.2, y: cursorY + 0.12, w: 3.8, h: 0.4, fontSize: 16, bold: true, color: cardColor
            })
            sec.items.forEach((item, ii) => {
              s.addText('• ' + item, {
                x: colX + 0.25, y: cursorY + 0.62 + ii * 0.46, w: 3.7, h: 0.42,
                fontSize: 13, color: '3B4252', valign: 'top'
              })
            })
          })
          cursorY += 3.3
        } else if (parsed.sections.length) {
          // 普通列表
          parsed.sections.forEach((sec) => {
            if (sec.title) {
              s.addText(sec.title, {
                x: 0.8, y: cursorY, w: 8.4, h: 0.4, fontSize: 16, bold: true, color: '1F2937'
              })
              cursorY += 0.45
            }
            sec.items.forEach((item) => {
              s.addText('• ' + item, {
                x: 0.9, y: cursorY, w: 8.2, h: 0.4,
                fontSize: 15, color: '3B4252', valign: 'middle'
              })
              cursorY += 0.42
            })
            cursorY += 0.12
          })
        }

        // 结论横幅
        if (parsed.conclusion) {
          const bannerY = Math.min(cursorY, PPT_H - 0.7)
          s.addShape('roundRect', {
            x: 0.8, y: bannerY, w: 8.4, h: 0.5,
            fill: { color: titleColor }, rectRadius: 0.08
          })
          s.addText(parsed.conclusion, {
            x: 0.8, y: bannerY, w: 8.4, h: 0.5,
            fontSize: 14, bold: true, color: 'FFFFFF', align: 'center', valign: 'middle'
          })
        }
      })
    }

    // 下载文件
    await pptx.writeFile({ fileName: `${props.parsedPpt?.mainTitle || '教学课件'}_演示版.pptx` })
    // 通知父组件记录下载行为
    emit('download')
  } catch (err) {
    console.error('生成PPT失败:', err)
    alert('生成PPT失败，请重试')
  }
}
// ============ 动画工具函数 ============
function pctToInch(pct, base) {
  return (pct / 100) * base
}

function easeOutQuad(p) {
  return p * (2 - p)
}

function calcElementStateAtTime(el, time) {
  const baseX = el.position?.x ?? 50
  const baseY = el.position?.y ?? 50
  let opacity = 0
  let scale = 1
  let offsetX = 0
  let offsetY = 0

  if (!el.animations || el.animations.length === 0) {
    return { x: baseX, y: baseY, opacity: 1, scale: 1, visible: true }
  }

  let visible = false
  for (const anim of el.animations) {
    const start = anim.startTime || 0
    const duration = anim.duration || 500

    if (time < start) continue
    visible = true

    const progress = Math.min(1, (time - start) / duration)
    const eased = easeOutQuad(progress)

    switch (anim.type) {
      case 'fadeIn':
        opacity = eased
        break
      case 'fadeOut':
        opacity = 1 - eased
        break
      case 'zoomIn':
        scale = 0.5 + 0.5 * eased
        opacity = eased
        break
      case 'zoomOut':
        scale = 1 - 0.5 * eased
        opacity = 1 - eased
        break
      case 'move':
        offsetX = (anim.offsetX || 0) * eased
        offsetY = (anim.offsetY || 0) * eased
        break
      case 'slideInLeft':
        offsetX = -30 * (1 - eased)
        opacity = eased
        break
      case 'slideInRight':
        offsetX = 30 * (1 - eased)
        opacity = eased
        break
      case 'slideInUp':
        offsetY = 20 * (1 - eased)
        opacity = eased
        break
      default:
        opacity = 1
        break
    }
  }

  return {
    x: baseX + offsetX,
    y: baseY + offsetY,
    opacity,
    scale,
    visible
  }
}

// ============ 动画导出视频 ============
async function downloadVideo() {
  if (isRecording.value || !animViewerRef.value) return
  const animEl = animViewerRef.value.$el
  if (!animEl) return

  isRecording.value = true
  try {
    const ad = props.businessData?.animation_json
    const duration = ad?.metadata?.totalDuration ||
      Math.max(...ad.elements.filter(e => e).map(el =>
        Math.max(...(el.animations || []).map(a => (a.startTime || 0) + (a.duration || 500)))
      ), 2000)

    const rect = animEl.getBoundingClientRect()
    const maxW = 1280
    const scale = rect.width > maxW ? maxW / rect.width : 1
    const width = Math.round(rect.width * scale)
    const height = Math.round(rect.height * scale)

    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')

    const mimeType = MediaRecorder.isTypeSupported('video/mp4')
      ? 'video/mp4'
      : MediaRecorder.isTypeSupported('video/webm;codecs=vp9')
        ? 'video/webm;codecs=vp9'
        : 'video/webm'

    const stream = canvas.captureStream(10)
    const recorder = new MediaRecorder(stream, { mimeType, videoBitsPerSecond: 4000000 })
    const chunks = []
    recorder.ondataavailable = e => { if (e.data.size > 0) chunks.push(e.data) }

    const stopped = new Promise(resolve => {
      recorder.onstop = () => {
        const blob = new Blob(chunks, { type: mimeType })
        const ext = mimeType.includes('mp4') ? 'mp4' : 'webm'
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `${props.parsedPpt?.mainTitle || '动画课件'}.${ext}`
        a.click()
        URL.revokeObjectURL(url)
        resolve()
      }
    })

    animViewerRef.value.pause()
    animViewerRef.value.seekTo(0)
    await nextTick()
    await new Promise(r => setTimeout(r, 100))

    recorder.start()
    const fps = 10
    const totalFrames = Math.max(1, Math.ceil((duration / 1000) * fps))

    for (let i = 0; i <= totalFrames; i++) {
      const t = Math.min(duration, (i / fps) * 1000)
      animViewerRef.value.seekTo(t)
      await nextTick()
      await new Promise(r => setTimeout(r, 80))

      const captured = await html2canvas(animEl, {
        scale: width / rect.width,
        useCORS: true,
        backgroundColor: null,
        logging: false
      })
      ctx.drawImage(captured, 0, 0, width, height)
      await new Promise(r => setTimeout(r, 1000 / fps))
    }

    recorder.stop()
    await stopped
    // 通知父组件记录下载行为
    emit('download')
  } catch (err) {
    console.error('录制视频失败:', err)
    alert('录制视频失败，请重试')
  } finally {
    isRecording.value = false
    animViewerRef.value?.replay()
  }
}
</script>

<style scoped>
.presentation-root {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  transition: background 0.4s;
}

/* ===== 工具栏：和预览页统一风格 ===== */
.pres-toolbar {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 16px;
  background-color: #e9ebf0;
  gap: 16px;
  flex-shrink: 0;
}

.back-home-btn {
  padding: 6px 14px;
  border-radius: 6px;
  border: 1px solid #d8dbe2;
  background: #ffffff;
  color: #444654;
  cursor: pointer;
  font-size: 14px;
}
.back-home-btn:hover {
  background: #f2f3f7;
}

.top-spacer {
  flex: 1;
}

.pres-toolbar-right {
  display: flex;
  gap: 10px;
  align-items: center;
}
.pres-page-num {
  font-size: 14px;
  color: #555;
}
.pres-tool-btn {
  padding: 5px 12px;
  border-radius: 6px;
  border: 1px solid #d0d3db;
  background: #fff;
  font-size:13px;
  cursor:pointer;
  color: #333;
}
.pres-tool-btn:hover { background: #f0f0f0; }

/* ===== 舞台 ===== */
.pres-stage {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 20px 60px 40px;
  overflow: hidden;
}

.pres-slide {
  width: 100%;
  max-width: 1080px;
  display: flex;
  position: relative;
  animation: slide-enter 0.4s ease-out;
}

@keyframes slide-enter {
  from { opacity: 0; transform: translateY(20px) scale(0.98); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

/* ===== PPT 文本页：纵向布局，标题+卡片 ===== */
.ppt-slide {
  background: transparent;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

/* 渐变艺术字标题：和预览页完全一致 */
.art-title {
  font-size: 48px;
  font-weight: bold;
  background: linear-gradient(90deg,#2b78ff,#9944ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 0 12px rgba(120,140,255,0.4), 0 2px 8px rgba(80,80,180,0.25);
  margin: 0;
  letter-spacing: 2px;
  text-align: center;
}
.art-title.purple {
  font-size: 42px;
  background-image: linear-gradient(135deg, #7c3aed 0%, #db2777 100%);
}
.art-title.gold {
  font-size: 34px;
  background-image: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);
}

.ppt-card {
  width: 100%;
  display: flex;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.08);
  overflow: hidden;
  min-height: 460px;
}
.ppt-accent-bar { width: 8px; flex-shrink: 0; }
.ppt-card-inner {
  flex: 1;
  padding: 32px 40px;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}
.ppt-head { flex-shrink: 0; margin-bottom: 22px; }
.ppt-heading {
  font-size: 30px; margin: 0 0 8px; line-height: 1.3; font-weight: 700;
}
.ppt-subtitle { font-size: 15px; color: #8b94a7; margin: 0; line-height: 1.5; }

.ppt-card-body {
  flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 16px; min-height: 0;
  padding-right: 6px;
}
.ppt-card-body::-webkit-scrollbar { width: 6px; }
.ppt-card-body::-webkit-scrollbar-thumb { background: #d8dee9; border-radius: 3px; }

/* 双栏对比 */
.ppt-compare { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.ppt-compare-card {
  border-radius: 14px; padding: 20px 22px;
  border: 1px solid #eef1f6; background: #fbfcfe;
}
.ppt-compare-card.compare-left { background: linear-gradient(160deg, #fff7ed 0%, #fff 70%); border-color: #fed7aa; }
.ppt-compare-card.compare-right { background: linear-gradient(160deg, #f0fdf4 0%, #fff 70%); border-color: #bbf7d0; }
.ppt-compare-title { font-size: 17px; font-weight: 700; margin-bottom: 12px; }
.ppt-compare-list { list-style: none; padding: 0; margin: 0; }
.ppt-compare-list li {
  font-size: 14px; line-height: 1.7; color: #3b4252; padding-left: 16px; position: relative; margin: 5px 0;
}
.ppt-compare-list li::before {
  content: ''; position: absolute; left: 0; top: 9px; width: 6px; height: 6px; border-radius: 50%;
  background: currentColor; opacity: 0.5;
}

/* 普通列表 */
.ppt-list { display: flex; flex-direction: column; gap: 14px; }
.ppt-section { background: #f8fafc; border-radius: 12px; padding: 16px 20px; border: 1px solid #eef1f6; }
.ppt-section-title { font-size: 16px; font-weight: 600; color: #1f2937; margin-bottom: 8px; }
.ppt-section ul { list-style: none; padding: 0; margin: 0; }
.ppt-section li {
  font-size: 15px; line-height: 1.75; color: #3b4252; padding-left: 18px; position: relative; margin: 4px 0;
}
.ppt-section li::before {
  content: ''; position: absolute; left: 0; top: 10px; width: 7px; height: 7px; border-radius: 50%;
  background: #409eff;
}

.ppt-empty { color: #b0b6c4; font-size: 15px; text-align: center; padding: 30px 0; }

.ppt-conclusion {
  flex-shrink: 0; margin-top: auto; padding: 14px 22px; border-radius: 12px;
  color: #fff; font-size: 15px; font-weight: 600; text-align: center; line-height: 1.5;
  box-shadow: 0 6px 18px rgba(0,0,0,0.12);
}

.ppt-hint {
  font-size: 13px; color: #9aa3b5; animation: hint-pulse 2s infinite; white-space: nowrap;
}
@keyframes hint-pulse {
  0%,100% { opacity: 0.45; } 50% { opacity: 1; }
}

/* ===== 动画页 ===== */
.anim-slide { background: #0a0a1a; flex-direction: column; align-items: center; justify-content: center; padding: 20px; border-radius: 18px; }
.anim-container { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.anim-container :deep(.animation-viewer) { width: 100%; height: 100%; max-height: 560px; border-radius: 12px; }
.anim-container.recording :deep(.controls) { display: none !important; }

.pres-hint {
  margin-top: 16px; font-size: 14px; color: #aaa;
  animation: hint-pulse 2s infinite;
}

/* ===== 问答题页 ===== */
.quiz-slide { background: transparent; flex-direction: row; border-radius: 18px; overflow: hidden; box-shadow: 0 24px 70px rgba(31, 45, 80, 0.18); }
.quiz-bar { width: 7px; flex-shrink: 0; }
.quiz-body { flex: 1; padding: 44px 56px; display: flex; flex-direction: column; justify-content: center; background: #fff; }
.quiz-number { font-size: 16px; color: #888; margin-bottom: 12px; }
.quiz-question { font-size: 28px; font-weight: bold; color: #1f2937; margin-bottom: 36px; line-height: 1.5; }
.quiz-options { display: flex; flex-direction: column; gap: 14px; }
.quiz-option {
  display: flex; align-items: center; gap: 14px; padding: 16px 20px;
  border: 2px solid #e5e7eb; border-radius: 12px; cursor: pointer;
  font-size: 18px; color: #374151; transition: all 0.2s;
}
.quiz-option:hover { border-color: #93c5fd; background: #eff6ff; }
.quiz-option.picked { border-color: #409eff; background: #dbeafe; }
.quiz-option.static { cursor: default; }
.quiz-option.static.right { border-color: #36c66c; background: #e8fbe8; }
.quiz-option.static.wrong { border-color: #f87171; background: #ffe9e9; }
.quiz-opt-letter {
  width: 32px; height: 32px; border-radius: 50%; background: #f0f0f0;
  display: flex; align-items: center; justify-content: center;
  font-weight: bold; font-size: 15px; color: #666; flex-shrink: 0;
}
.quiz-option.picked .quiz-opt-letter { background: #409eff; color: #fff; }
.quiz-option.static.right .quiz-opt-letter { background: #36c66c; color: #fff; }
.quiz-option.static.wrong .quiz-opt-letter { background: #f87171; color: #fff; }
.quiz-answer-banner {
  padding: 16px 24px; border-radius: 12px; font-size: 22px; font-weight: bold; margin-bottom: 20px;
}
.quiz-answer-banner.correct { background: #e8fbe8; color: #00b42a; }
.quiz-answer-banner.wrong { background: #ffe9e9; color: #f53f3f; }
.quiz-options-reveal { display: flex; flex-direction: column; gap: 10px; }
.quiz-correct-answer { margin-top: 20px; font-size: 16px; color: #00b42a; font-weight: bold; }

/* ===== 问答开始/完成页 ===== */
.quiz-intro-slide,
.quiz-result-slide {
  background: transparent;
  display: flex; align-items: center; justify-content: center;
}
.quiz-intro-card,
.quiz-result-card {
  width: 520px;
  background: #fff;
  border-radius: 24px;
  padding: 56px 48px;
  box-shadow: 0 24px 70px rgba(0,0,0,0.08);
  display: flex; flex-direction: column; align-items: center;
  animation: card-enter 0.5s ease-out;
}
@keyframes card-enter {
  from { opacity: 0; transform: translateY(30px) scale(0.96); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}
.quiz-intro-icon {
  width: 72px; height: 72px;
  border-radius: 22px;
  background: linear-gradient(135deg, #ede9fe 0%, #fce7f3 100%);
  color: #7c3aed;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 22px;
}
.quiz-intro-icon svg { width: 36px; height: 36px; }
.quiz-intro-subtitle {
  font-size: 15px; color: #888;
  margin: 10px 0 28px;
}
.quiz-intro-stats {
  display: flex; gap: 28px; margin-bottom: 34px;
}
.quiz-intro-stat {
  display: flex; align-items: center; gap: 8px;
  font-size: 14px; color: #666;
  padding: 8px 14px; background: #f7f8fa; border-radius: 20px;
}
.quiz-start-btn {
  padding: 14px 38px;
  border: none; border-radius: 28px;
  background: linear-gradient(135deg, #7c3aed 0%, #db2777 100%);
  color: #fff; font-size: 16px; font-weight: bold;
  cursor: pointer; box-shadow: 0 10px 28px rgba(124, 58, 237, 0.28);
  transition: transform 0.15s, box-shadow 0.15s;
}
.quiz-start-btn:hover { transform: translateY(-2px); box-shadow: 0 14px 34px rgba(124, 58, 237, 0.36); }
.quiz-start-arrow { margin-left: 4px; font-size: 20px; }

.quiz-result-card {
  width: 560px;
  background: linear-gradient(180deg, #fffbf0 0%, #ffffff 60%);
  border: 1px solid #fef3c7;
}
.quiz-result-trophy {
  font-size: 72px;
  filter: drop-shadow(0 8px 20px rgba(245, 158, 11, 0.25));
  margin-bottom: 6px;
  animation: trophy-bounce 1.2s ease-in-out infinite;
}
@keyframes trophy-bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
.quiz-result-badge {
  padding: 6px 16px; border-radius: 16px;
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  color: #fff; font-size: 13px; font-weight: bold;
  margin-bottom: 14px;
}
.quiz-result-date {
  font-size: 13px; color: #b45309; margin-top: 6px; margin-bottom: 28px;
}
.quiz-result-stats {
  display: flex; gap: 16px; margin-bottom: 24px; width: 100%;
}
.quiz-result-stat {
  flex: 1; text-align: center;
  background: #fff; border-radius: 16px;
  padding: 18px 10px;
  box-shadow: 0 4px 14px rgba(0,0,0,0.04);
  border: 1px solid #fef3c7;
}
.quiz-result-num {
  font-size: 28px; font-weight: 800;
  background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.quiz-result-label { font-size: 13px; color: #888; margin-top: 4px; }
.quiz-result-rate {
  width: 100%; display: flex; align-items: center; gap: 18px;
  background: #fff8e6; border-radius: 16px;
  padding: 16px 20px; margin-bottom: 24px;
  border: 1px solid #fde68a;
}
.quiz-result-rate.pass { background: #ecfdf5; border-color: #a7f3d0; }
.quiz-rate-ring {
  width: 56px; height: 56px; border-radius: 50%;
  background: #fff;
  border: 4px solid #fbbf24;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; color: #b45309; font-weight: bold;
  flex-shrink: 0;
}
.quiz-result-rate.pass .quiz-rate-ring { border-color: #34d399; color: #059669; }
.quiz-rate-num { font-size: 20px; }
.quiz-rate-text { font-size: 15px; color: #92400e; font-weight: 600; }
.quiz-result-rate.pass .quiz-rate-text { color: #047857; }
.quiz-result-feedback {
  width: 100%; text-align: center;
  background: #fff; border-radius: 16px;
  padding: 18px 16px;
  box-shadow: 0 4px 14px rgba(0,0,0,0.04);
}
.quiz-feedback-title { font-size: 14px; color: #666; margin-bottom: 12px; }
.quiz-feedback-btns { display: flex; gap: 12px; justify-content: center; }
.quiz-feedback-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 18px; border-radius: 20px; border: 1px solid #e5e7eb;
  background: #fff; color: #555; font-size: 14px; cursor: pointer;
  transition: all 0.2s;
}
.quiz-feedback-btn:hover { background: #f7f8fa; border-color: #d1d5db; }
.quiz-feedback-btn.like:hover { color: #f59e0b; border-color: #fcd34d; }
.quiz-feedback-btn.dislike:hover { color: #6b7280; border-color: #d1d5db; }

/* ===== 进度条 ===== */
.pres-progress {
  height: 4px; background: #e5e7eb; flex-shrink: 0;
}
.pres-progress-bar {
  height: 100%; background: #409eff; transition: width 0.3s ease;
}

/* ===== 键盘提示 ===== */
.pres-key-hint {
  position: absolute; bottom: 20px; right: 24px;
  font-size: 11px; color: #ccc; pointer-events: none;
}
</style>