<template>
  <div class="classroom-page">
    <Header />
    <div class="container">
      <h2>课堂生成中</h2>

      <!-- 缓冲圆圈：在数据完整返回之前一直旋转 -->
      <div v-if="loading && !classroomPpt" class="loading-wrap">
        <div class="spinner"></div>
        <div class="loading-tip">{{ loadingText || '正在生成课堂内容，请稍候...' }}</div>
        <!-- SSE实时状态，覆盖更新只显示最新一条，不弹弹窗 -->
        <div v-if="currentNodeMsg" class="node-log">{{ currentNodeMsg }}</div>
      </div>

      <div v-else-if="classroomPpt" class="classroom-view">
        <h3>{{ classroomPpt.mainTitle }}</h3>
        <div class="ppt-wrap">
          <div v-for="(page, pageIndex) in classroomPpt.pages" :key="pageIndex" class="ppt-page">
            <h4>第 {{ pageIndex + 1 }} 页 / {{ classroomPpt.pages.length }}</h4>
            <div class="page-content" v-html="formatPageContent(page.pageContent)"></div>

            <div v-if="page.classQuestion" class="question-block">
              <h5>课堂习题</h5>
              <p class="q-text">{{ page.classQuestion.question }}</p>
              <div class="q-options">
                <div>A. {{ page.classQuestion.A }}</div>
                <div>B. {{ page.classQuestion.B }}</div>
                <div>C. {{ page.classQuestion.C }}</div>
                <div>D. {{ page.classQuestion.D }}</div>
              </div>
              <p class="q-answer">答案：{{ page.classQuestion.answer }}</p>
            </div>
          </div>
        </div>

        <!-- 题目数 > 页数时，把没分配到页面的题目集中在底部展示，确保 5 道题全部可见 -->
        <div v-if="extraQuestions.length" class="extra-questions">
          <h3>课堂习题（补充）</h3>
          <div v-for="(q, i) in extraQuestions" :key="'extra-' + i" class="question-block">
            <h5>习题 {{ i + 1 }}</h5>
            <p class="q-text">{{ q.question }}</p>
            <div class="q-options">
              <div>A. {{ q.A }}</div>
              <div>B. {{ q.B }}</div>
              <div>C. {{ q.C }}</div>
              <div>D. {{ q.D }}</div>
            </div>
            <p class="q-answer">答案：{{ q.answer }}</p>
          </div>
        </div>

        <!-- 课堂动画：把后端返回的 animation_json 真正渲染出来 -->
        <div v-if="hasAnimation" class="animation-section">
          <div class="section-head">
            <h3>课堂动画演示</h3>
            <el-button size="small" plain @click="replayAnimation">重新播放</el-button>
          </div>
          <AnimationViewer ref="animViewerRef" :animation-json="classroomPpt.globalAnimation" />
        </div>
      </div>

      <div v-else class="empty-tip">
        等待任务执行推送数据……
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import Header from '@/components/Header.vue'
import AnimationViewer from '@/components/AnimationViewer.vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const route = useRoute()
const userStore = useUserStore()
const taskId = ref('')
const loadingText = ref('')
// SSE实时节点消息，只保存最新一条
const currentNodeMsg = ref('')
const loading = ref(true)
const classroomPpt = ref(null)
const animationRaw = ref(null)
// 动画播放器引用，用于触发重新播放
const animViewerRef = ref(null)

// 仅当 globalAnimation 中确实存在可渲染的动画数据时才展示动画区块
const hasAnimation = computed(() => {
  const g = classroomPpt.value?.globalAnimation
  if (!g) return false
  if (Array.isArray(g.elements)) return g.elements.length > 0
  return !!g.globalConfig
})

// 页面数少于题目数时，多出来的题目在页面下方统一展示，保证全部题目可见
const extraQuestions = computed(() => {
  const cp = classroomPpt.value
  if (!cp || !Array.isArray(cp.allQuestions)) return []
  const pageCount = Array.isArray(cp.pages) ? cp.pages.length : 0
  return cp.allQuestions.slice(pageCount)
})

function replayAnimation() {
  animViewerRef.value?.replay()
}

// 兼容后端把数组/对象序列化成字符串的情况
function safeParseArray(str) {
  try {
    const v = JSON.parse(str)
    return Array.isArray(v) ? v : []
  } catch {
    return []
  }
}

watch(
  () => route.query.taskId,
  (val) => {
    if (val) {
      taskId.value = val
      startTask()
    }
  },
  { immediate: true }
)

onMounted(() => {
  if (!taskId.value) {
    loading.value = false
    ElMessage.error('缺少任务编号 taskId')
  }
})

async function startTask() {
  const tid = taskId.value
  loading.value = true
  classroomPpt.value = null
  animationRaw.value = null
  currentNodeMsg.value = ''
  loadingText.value = '检查任务状态...'

  try {
    const res = await request.get(`/upload/result/${tid}`)
    if (res && res.code === 200 && res.data) {
      loadingText.value = '任务已完成，加载结果'
      await loadClassroomResult(tid)
      loading.value = false
      return
    }
  } catch (e) {
    console.log("任务还没有结果，准备开启SSE监听")
  }

  // ===== 断点续传：启动前查询任务状态 + 断点信息 =====
  // resumable=true 且 hasCheckpoint=true → 后端会从上次 checkpoint 续跑，前端提示用户
  try {
    const st = await request.get(`/upload/status/${tid}`)
    const d = st && st.data
    if (d && d.resumable && d.hasCheckpoint) {
      loadingText.value = '检测到上次未完成的任务，正在断点续跑...'
    } else if (d && d.resumable) {
      // FAIL 但无断点：从头重新执行
      loadingText.value = '检测到未完成的任务，重新执行...'
    } else {
      loadingText.value = '正在启动任务...'
    }
  } catch (e) {
    // 状态接口异常不影响主流程，按首次启动处理
    console.warn('查询断点状态失败，按首次启动处理', e)
    loadingText.value = '正在启动任务...'
  }

  await listenTaskSse(tid)
  await loadClassroomResult(tid)
  // 无论结果轮询还是 SSE 流程，拿到完整数据后才结束缓冲圆圈
  loading.value = false
}

// SSE 断线重连计数（指数退避：3s / 6s / 9s）
let sseRetry = 0
const SSE_MAX_RETRY = 3
// 当前活动的 EventSource，组件卸载时主动关闭
let currentEs = null

onUnmounted(() => {
  if (currentEs) {
    currentEs.close()
    currentEs = null
  }
})

async function listenTaskSse(tid) {
  return new Promise((resolve) => {
    const es = new EventSource(`/api/upload/start?taskId=${tid}`);
    currentEs = es

    es.onmessage = async (ev) => {
      try {
        const event = JSON.parse(ev.data);
        await handleSseEvent(event, () => {
          es.close();
          if (currentEs === es) currentEs = null
          sseRetry = 0
          resolve();
        })
      } catch (e) {
        console.warn("sse解析失败", e)
      }
    }

    es.onerror = () => {
      es.close();
      if (currentEs === es) currentEs = null
      // 断线自动重连：重连后后端 startTask 检测 checkpoint 自动续跑，前端无需区分首次/续跑
      if (sseRetry < SSE_MAX_RETRY) {
        sseRetry++
        const waitSec = sseRetry * 3
        loadingText.value = `连接中断，${waitSec}s 后自动重连续跑...`
        setTimeout(() => {
          listenTaskSse(tid).then(resolve)
        }, waitSec * 1000)
      } else {
        sseRetry = 0
        loadingText.value = ''
        currentNodeMsg.value = ''
        resolve();
      }
    }
  })
}

async function handleSseEvent(event, onFinishCallback) {
  const { type, msg, node } = event
  switch (type) {
    case 'agent_finish':
      // 只更新页面单行文本，不再弹ElMessage弹窗
      currentNodeMsg.value = `节点完成：${node} — ${msg}`
      break
    case 'notice':
      currentNodeMsg.value = msg
      break
    case 'task_finish':
      // 只有全部任务结束才弹提示
      ElMessage.success('全部工作流执行完毕')
      // SSE任务完成，执行积分加分
      try {
        const scoreRes = await request.post('/user/sign/uploadScore')
        console.log('加分返回：', scoreRes.data)
        if (scoreRes.data.code === 200) {
          const newScore = scoreRes.data.data || userStore.score + 10
          userStore.setScore(newScore)
          ElMessage.success(`🎉上传成功！积分+10，当前积分：${newScore}`)
        } else {
          ElMessage.warning('任务完成，但积分加分失败，请联系管理员')
        }
      } catch (scoreErr) {
        console.error('积分接口异常', scoreErr)
        ElMessage.warning('任务完成，但积分加分失败，请联系管理员')
      }
      onFinishCallback()
      break
    case 'task_error':
      ElMessage.error(`任务异常：${msg}`)
      loadingText.value = ''
      currentNodeMsg.value = ''
      // 业务错误直接结束本次流程（不触发断线重连）；任务为 FAIL 状态，
      // 下次进入页面会通过 /upload/status 检测断点并自动续跑
      onFinishCallback()
      break
  }
}

async function loadClassroomResult(tid) {
  try {
    const apiData = await request.get(`/upload/result/${tid}`)
    console.log('后端返回完整响应', apiData)

    if (!apiData || apiData.code !== 200 || !apiData.data) {
      ElMessage.error('结果为空或查询失败')
      return
    }

    const bigJsonStr = apiData.data
    const resultObj = JSON.parse(bigJsonStr)

    if (!resultObj.PPT_JSON) {
      ElMessage.error('PPT数据缺失')
      return
    }
    const pptObj = JSON.parse(resultObj.PPT_JSON)
    // animation_json 可能是对象，也可能是被二次序列化的 JSON 字符串，统一规整为对象
    let animObj = resultObj.animation_json
    if (typeof animObj === 'string') {
      try { animObj = JSON.parse(animObj) } catch (e) { animObj = {} }
    }
    animObj = animObj || {}
    // questions 同样兼容对象/字符串两种形态
    const questionsRaw = resultObj.questions
    const questionsArr = Array.isArray(questionsRaw)
      ? questionsRaw
      : (typeof questionsRaw === 'string' ? safeParseArray(questionsRaw) : [])

    animationRaw.value = animObj
    classroomPpt.value = mergeToClassroomPpt(pptObj, animObj, questionsArr)
    console.log('✅合并完成课堂数据：', classroomPpt.value)
  } catch (e) {
    console.error('获取课堂结果失败', e)
    ElMessage.error('获取课堂数据失败')
  }
}

function mergeToClassroomPpt(pptBase, animJson, questions) {
  const classroomPpt = JSON.parse(JSON.stringify(pptBase))
  const pages = Array.isArray(classroomPpt.pages) ? classroomPpt.pages : []
  classroomPpt.pages = pages
  classroomPpt.globalAnimation = animJson
  // 保留全部题目（防止“页数 < 题数”时多出的题目被静默丢弃）
  classroomPpt.allQuestions = Array.isArray(questions) ? questions : []

  if (Array.isArray(questions)) {
    questions.forEach((q, idx) => {
      const targetIdx = idx
      if (pages[targetIdx]) {
        pages[targetIdx].classQuestion = q
      }
    })
  }
  return classroomPpt
}

function formatPageContent(text) {
  if (!text) return ''
  return text.replaceAll('\n', '<br/>')
}
</script>

<style scoped>
.classroom-page {
  padding: 24px;
}

.container {
  max-width: 1200px;
  margin: 40px auto 0;
}

.loading-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  gap: 18px;
}

.spinner {
  width: 56px;
  height: 56px;
  border: 6px solid rgba(64, 158, 255, 0.2);
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-tip {
  margin: 0;
  font-size: 16px;
  color: #409eff;
  text-align: center;
}

.node-log {
  font-size: 14px;
  color: #606266;
  background: #f5f7fa;
  padding: 8px 16px;
  border-radius: 8px;
  max-width: 600px;
  text-align: center;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-tip {
  margin-top: 30px;
  color: #888;
}

.ppt-wrap {
  margin-top: 20px;
}

.ppt-page {
  border: 1px solid #e5e7eb;
  padding: 24px;
  margin-bottom: 20px;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.ppt-page h4 {
  margin: 0 0 12px;
  color: #333;
}

.page-content {
  line-height: 1.75;
  font-size: 15px;
  color: #222;
}

.question-block {
  margin-top: 16px;
  padding: 16px;
  background: #f7f8fa;
  border-radius: 8px;
}

.question-block h5 {
  margin: 0 0 10px;
}

.q-text {
  margin: 0 0 10px;
}

.q-options>div {
  margin: 6px 0;
}

.q-answer {
  margin-top: 10px;
  font-weight: 500;
  color: #0969da;
}

.animation-section {
  margin-top: 32px;
  padding: 24px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.extra-questions {
  margin-top: 32px;
  padding: 24px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.extra-questions h3 {
  margin: 0 0 16px;
  color: #333;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-head h3 {
  margin: 0;
  color: #333;
}
</style>