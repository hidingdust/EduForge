<!-- work-detail.vue -->
<template>
  <div class="work-detail">
    <!-- 顶部栏 -->
    <div class="top-bar">
      <button class="back-home-btn" @click="goHome">← 返回首页</button>
      <div class="top-spacer"></div>
      <div class="top-actions">
        <span class="page-index">{{ currentPageIndex + 1 }} / {{ totalPage }}</span>
        <button class="action-btn" @click="downloadPPTX">下载PPTX</button>
        <button class="action-btn" @click="startPlay">开始播放</button>
        <button class="action-btn" @click="toggleFullscreen">全屏</button>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="status-msg">加载中...</div>

    <!-- 任务失败 -->
    <div v-else-if="taskInfo?.taskStatus === 'FAIL'" class="status-msg">
      <p style="color:red;font-size:16px">任务执行失败</p>
    </div>

    <!-- 任务处理中 -->
    <div v-else-if="!taskInfo || taskInfo.taskStatus === 'WAITING' || taskInfo.taskStatus === 'RUNNING'" class="status-msg">
      <p>任务处理中，请等待，自动轮询刷新状态...</p>
    </div>

    <!-- ============ 预览 + 内联编辑 ============ -->
    <div v-else-if="taskInfo.taskStatus === 'SUCCESS' && !isPlaying" class="play-wrap">
      <div class="preview-container">
        <!-- 艺术字标题 -->
        <h1 class="art-title">{{ slideTitle }}</h1>

        <!-- 主标题编辑入口（仅第一页显示） -->
        <div v-if="currentPageIndex === 0" class="main-title-edit-bar">
          <label class="inline-label">课件标题：</label>
          <input
            v-model="editablePpt.mainTitle"
            class="inline-title-input"
            placeholder="请输入课件标题"
          />
        </div>

        <!-- 页面卡片 -->
        <div class="slide-card">
          <!-- 页面顶部操作栏 -->
          <div class="page-toolbar">
            <span class="page-badge">第 {{ currentPageIndex + 1 }} 页</span>
            <span class="page-title-text">{{ currentPage.pageTitle || '未命名' }}</span>
            <button
              class="inline-edit-btn"
              :class="{ active: editingPages[currentPageIndex] }"
              @click="togglePageEdit(currentPageIndex)"
            >
              {{ editingPages[currentPageIndex] ? '✓ 完成' : '✏ 编辑此页' }}
            </button>
          </div>

          <!-- 编辑模式 -->
          <div v-if="editingPages[currentPageIndex]" class="page-edit-area">
            <div class="edit-field">
              <label class="field-label">页面标题</label>
              <input
                v-model="currentPage.pageTitle"
                class="field-input"
                placeholder="请输入页面标题"
              />
            </div>
            <div class="edit-field">
              <label class="field-label">页面内容（每行一条）</label>
              <textarea
                v-model="currentPage.pageContent"
                class="field-textarea"
                rows="10"
                placeholder="请输入页面内容，每行一条"
              ></textarea>
            </div>
          </div>

          <!-- 预览模式 -->
          <div v-else class="slide-content" v-html="renderCurrentSlide"></div>
        </div>

        <!-- 翻页 -->
        <div class="page-nav">
          <button
            class="nav-btn"
            :disabled="currentPageIndex === 0"
            @click="prevPage"
          >← 上一页</button>
          <button
            class="nav-btn"
            :disabled="currentPageIndex >= totalPage - 1"
            @click="nextPage"
          >下一页 →</button>
        </div>

        <!-- 播放入口 -->
        <div class="play-card">
          <div class="play-icon" @click="startPlay">▶</div>
          <div class="play-desc">点击开始播放演示文稿</div>
        </div>
      </div>
    </div>

    <!-- 播放后进入PresentationViewer组件 -->
    <PresentationViewer
      v-else-if="taskInfo.taskStatus === 'SUCCESS' && isPlaying"
      :business-data="businessData"
      :parsed-ppt="editablePpt"
      @close="handleViewerClose"
      @download="handleRecordDownload"
    />
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getWorkDetailApi, recordDownloadApi } from '@/api'
import PresentationViewer from '@/components/PresentationViewer.vue'
import PptxGenJS from 'pptxgenjs'

const route = useRoute()
const router = useRouter()
const taskId = computed(() => route.query.taskId)

const loading = ref(true)
const taskInfo = ref(null)
const businessData = ref(null)
const parsedPpt = ref(null)
let pollTimer = null

const isPlaying = ref(false)

// 可编辑的 PPT 副本
const editablePpt = reactive({
  mainTitle: '',
  subtitle: '',
  mainColor: '',
  pages: []
})

// 当前预览页索引
const currentPageIndex = ref(0)
const totalPage = ref(1)

// 每页是否处于编辑状态
const editingPages = reactive({})

const slideTitle = computed(() => editablePpt.mainTitle || parsedPpt.value?.mainTitle || '演示文稿')
const currentPage = computed(() => editablePpt.pages[currentPageIndex.value] || { pageTitle: '', pageContent: '' })

// ============ 工具函数 ============
function stripHtmlTags(str) {
  if (!str) return ''
  return str.replace(/<[^>]+>/g, '').replace(/\*\*(.*?)\*\*/g, '$1')
}

// ============ 页面编辑切换 ============
function togglePageEdit(idx) {
  editingPages[idx] = !editingPages[idx]
}

// ============ 翻页 ============
function prevPage() {
  if (currentPageIndex.value > 0) currentPageIndex.value--
}
function nextPage() {
  if (currentPageIndex.value < totalPage.value - 1) currentPageIndex.value++
}

// ============ 数据同步 ============
function syncEditableFromParsed() {
  const src = parsedPpt.value
  if (!src) return
  console.log('[WorkDetail] sync editable from parsedPpt:', src)
  editablePpt.mainTitle = src.mainTitle || ''
  editablePpt.subtitle = src.subtitle || ''
  editablePpt.mainColor = src.mainColor || '#409eff'

  const newPages = (src.pages || []).map((p) => ({
    pageTitle: p.pageTitle || '',
    pageContent: p.pageContent || '',
    titleColor: p.titleColor || ''
  }))
  editablePpt.pages.splice(0, editablePpt.pages.length, ...newPages)
  if (editablePpt.pages.length > 0) {
    totalPage.value = editablePpt.pages.length
  }
  console.log('[WorkDetail] after sync, pages.length =', editablePpt.pages.length)
}

watch(parsedPpt, (val) => {
  if (val) syncEditableFromParsed()
}, { immediate: true })

// ============ 预览内容渲染 ============
const renderCurrentSlide = computed(() => {
  const page = currentPage.value
  if (!page) return ''
  const lines = (page.pageContent || '').split('\n').filter(l => l.trim())
  if (!lines.length) return '<div class="empty-hint">暂无内容，点击「编辑此页」添加</div>'
  return lines.map(line => `<div>• ${stripHtmlTags(line)}</div>`).join('')
})

// ============ 导航 ============
function goHome() {
  router.push('/home')
}

function startPlay() {
  isPlaying.value = true
}

function handleViewerClose() {
  isPlaying.value = false
  goHome()
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().catch(() => {})
  } else {
    document.exitFullscreen()
  }
}

// ============ 通知后端记录下载行为 ============
async function handleRecordDownload() {
  try {
    await recordDownloadApi(taskId.value)
  } catch (err) {
    // 记录失败不影响用户本地下载，仅打日志
    console.error('记录下载行为失败:', err)
  }
}

// ============ PPTX 下载 ============
const PPT_W = 10
const PPT_H = 5.625

function parsePageContentPptx(text) {
  const lines = (text || '').split('\n').map(l => l.trim()).filter(l => l)
  if (!lines.length) return { subtitle: '', sections: [], conclusion: '' }

  let subtitle = ''
  let startIdx = 0
  const first = stripHtmlTags(lines[0])
  if (first.length <= 48 && !/^[\s]*[•\-\*\d][\.\、]?\s+/.test(first)) {
    subtitle = first
    startIdx = 1
  }

  const sections = []
  let current = null
  for (let i = startIdx; i < lines.length; i++) {
    const raw = lines[i]
    const clean = stripHtmlTags(raw)
    if (/^[\s]*[•\-\*\d][\.\、]?\s+/.test(raw)) {
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

async function downloadPPTX() {
  try {
    const pptx = new PptxGenJS()
    const mc = (editablePpt.mainColor || '#409eff').replace('#', '')
    pptx.title = editablePpt.mainTitle || '教学课件'
    pptx.layout = 'LAYOUT_16x9'

    // 封面
    const cover = pptx.addSlide()
    cover.background = { color: 'FFFFFF' }
    cover.addShape('rect', { x: 0, y: 0, w: PPT_W, h: 0.08, fill: { color: mc } })
    cover.addText(editablePpt.mainTitle || '教学课件', {
      x: 1, y: 1.8, w: 8, h: 1.2, fontSize: 40, bold: true, color: mc, align: 'center', valign: 'middle'
    })
    if (editablePpt.subtitle) {
      cover.addText(editablePpt.subtitle, {
        x: 1, y: 3.0, w: 8, h: 0.6, fontSize: 20, color: '888888', align: 'center'
      })
    }
    cover.addText(new Date().toLocaleDateString('zh-CN'), {
      x: 1, y: 4.8, w: 8, h: 0.4, fontSize: 12, color: 'AAAAAA', align: 'center'
    })

    // PPT 文本页
    if (editablePpt.pages && Array.isArray(editablePpt.pages)) {
      editablePpt.pages.forEach((page) => {
        const s = pptx.addSlide()
        s.background = { color: 'FFFFFF' }
        const titleColor = (page.titleColor || mc).replace('#', '')
        s.addShape('rect', { x: 0, y: 0, w: 0.2, h: PPT_H, fill: { color: titleColor } })
        s.addText(page.pageTitle || '', {
          x: 0.7, y: 0.35, w: 8.8, h: 0.7, fontSize: 28, bold: true, color: titleColor, valign: 'middle'
        })

        const parsed = parsePageContentPptx(page.pageContent)
        let cursorY = 1.15

        if (parsed.subtitle) {
          s.addText(parsed.subtitle, {
            x: 0.8, y: cursorY, w: 8.4, h: 0.4, fontSize: 14, color: '888888', valign: 'middle'
          })
          cursorY += 0.5
        }

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

    await pptx.writeFile({ fileName: `${editablePpt.mainTitle || '教学课件'}_演示版.pptx` })
    // 通知后端记录下载行为
    handleRecordDownload()
  } catch (err) {
    console.error('生成PPT失败:', err)
    alert('生成PPT失败，请重试')
  }
}

// ============ 数据加载 ============
async function loadDetail() {
  if (!taskId.value) {
    loading.value = false
    return
  }
  try {
    const res = await getWorkDetailApi(taskId.value)
    taskInfo.value = res.data

    if (taskInfo.value.taskStatus === 'SUCCESS' && taskInfo.value.resultJson) {
      businessData.value = JSON.parse(taskInfo.value.resultJson)
      if (businessData.value.PPT_JSON) {
        try {
          parsedPpt.value = JSON.parse(businessData.value.PPT_JSON)
          if (parsedPpt.value?.pages?.length) {
            totalPage.value = parsedPpt.value.pages.length
          }
        } catch (e) {
          console.error('PPT_JSON解析失败', e)
          parsedPpt.value = null
        }
      }
      clearInterval(pollTimer)
    }

    if (taskInfo.value.taskStatus === 'WAITING' || taskInfo.value.taskStatus === 'RUNNING') {
      startPoll()
    } else {
      clearInterval(pollTimer)
    }
  } catch (err) {
    console.error('获取详情失败', err)
  } finally {
    loading.value = false
  }
}

function startPoll() {
  clearInterval(pollTimer)
  pollTimer = setInterval(() => {
    loadDetail()
  }, 2000)
}

onMounted(() => {
  loadDetail()
})

onUnmounted(() => {
  clearInterval(pollTimer)
})
</script>

<style scoped>
.work-detail {
  width: 100%;
  height: 100vh;
  background-color: #f6f7fb;
}

/* 顶部栏 */
.top-bar {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 16px;
  background-color: #e9ebf0;
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

.top-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
.page-index {
  font-size: 14px;
  color: #555;
}
.action-btn {
  padding: 5px 12px;
  border-radius: 6px;
  border: 1px solid #d0d3db;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.action-btn:hover {
  background: #f0f1f5;
}

.status-msg {
  text-align: center;
  padding: 80px 40px;
  color: #666;
  font-size: 16px;
}

/* ============ 预览区域 ============ */
.play-wrap {
  width: 100%;
  height: calc(100vh - 64px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  overflow-y: auto;
  padding: 32px 20px;
  box-sizing: border-box;
}

.preview-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  max-width: 900px;
  width: 100%;
}

.art-title {
  font-size: 42px;
  font-weight: bold;
  background: linear-gradient(90deg, #2b78ff, #9944ee);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  text-shadow: 0 0 12px rgba(120,140,255,0.4), 0 2px 8px rgba(80,80,180,0.25);
  margin: 0;
  letter-spacing: 2px;
}

/* 主标题编辑栏 */
.main-title-edit-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  max-width: 820px;
  background: #fff;
  border-radius: 10px;
  padding: 10px 18px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.inline-label {
  font-size: 14px;
  font-weight: 600;
  color: #555;
  white-space: nowrap;
}
.inline-title-input {
  flex: 1;
  border: 1.5px solid #e0e3eb;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 16px;
  color: #333;
  background: #fff;
  box-sizing: border-box;
  transition: border-color 0.2s;
}
.inline-title-input:focus {
  outline: none;
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64,158,255,0.12);
}

/* 页面卡片 */
.slide-card {
  width: 100%;
  max-width: 820px;
  min-height: 360px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.08);
  border-left: 8px solid #2b62dd;
  overflow: hidden;
}

/* 页面顶部工具栏 */
.page-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  background: #f8f9fc;
  border-bottom: 1px solid #eef0f4;
}
.page-badge {
  font-size: 13px;
  font-weight: bold;
  color: #409eff;
  background: #e8f0ff;
  padding: 3px 12px;
  border-radius: 20px;
  white-space: nowrap;
}
.page-title-text {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.inline-edit-btn {
  padding: 5px 14px;
  border-radius: 6px;
  border: 1.5px solid #409eff;
  background: #e8f0ff;
  color: #409eff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}
.inline-edit-btn:hover {
  background: #d0e3ff;
}
.inline-edit-btn.active {
  background: #36c66c;
  border-color: #36c66c;
  color: #fff;
}

/* 编辑区域 */
.page-edit-area {
  padding: 20px 28px 28px;
}
.edit-field {
  margin-bottom: 18px;
}
.edit-field:last-child {
  margin-bottom: 0;
}
.field-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #555;
  margin-bottom: 6px;
}
.field-input {
  width: 100%;
  padding: 10px 14px;
  border: 1.5px solid #e0e3eb;
  border-radius: 8px;
  font-size: 16px;
  color: #333;
  background: #fff;
  box-sizing: border-box;
  transition: border-color 0.2s;
}
.field-input:focus {
  outline: none;
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64,158,255,0.12);
}
.field-textarea {
  width: 100%;
  min-height: 200px;
  padding: 12px 14px;
  border: 1.5px solid #e0e3eb;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  background: #fff;
  box-sizing: border-box;
  resize: vertical;
  line-height: 1.7;
  font-family: inherit;
  transition: border-color 0.2s;
}
.field-textarea:focus {
  outline: none;
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64,158,255,0.12);
}

/* 预览内容 */
.slide-content {
  padding: 28px 32px;
  font-size: 16px;
  color: #222;
  min-height: 280px;
}
.slide-content > div {
  padding: 8px 0;
  line-height: 1.8;
}
.empty-hint {
  color: #bbb;
  font-style: italic;
  text-align: center;
  padding: 60px 0 !important;
}

/* 翻页 */
.page-nav {
  display: flex;
  gap: 12px;
}
.nav-btn {
  padding: 8px 22px;
  border-radius: 8px;
  border: 1.5px solid #d0d3db;
  background: #fff;
  font-size: 14px;
  font-weight: 600;
  color: #444;
  cursor: pointer;
  transition: all 0.2s;
}
.nav-btn:hover:not(:disabled) {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}
.nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 播放入口 */
.play-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}
.play-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #407bff;
  color: white;
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}
.play-icon:hover {
  background: #2b62dd;
}
.play-desc {
  font-size: 15px;
  color: #444;
}
</style>
