<template>
  <div class="game-container">
    <div class="game-card">
      <div class="header">
        <h2 class="title">AI知识闯关答题</h2>
        <div class="score">得分：{{ score }}</div>
      </div>

      <el-progress :percentage="progressPercent" stroke-width="10" color="#4158D0" />

      <div class="question-text" v-if="currentQuestion">
        第{{ currentIndex + 1 }}题：{{ currentQuestion.question }}
      </div>
      <div class="empty-tip" v-else>题目加载中，请等待后端数据...</div>

      <div class="option-group" v-if="currentQuestion">
        <div
          v-for="key in ['A','B','C','D']"
          :key="key"
          class="option-item"
          :class="getOptionClass(key)"
          @click="handleSelect(key)"
        >
          {{ key }}. {{ currentQuestion[key] }}
        </div>
      </div>

      <div class="btn-group">
        <el-button @click="resetGame" size="large">重新答题</el-button>
        <el-button type="primary" @click="exportPPT" size="large">导出PPT课件</el-button>
      </div>
    </div>

    <el-dialog title="答题完成" v-model="dialogVisible" width="420px">
      <div class="stat-item">总题目：{{ rawList.length }} 道</div>
      <div class="stat-item">答对：{{ rightCount }} 道</div>
      <div class="stat-item">答错：{{ wrongCount }} 道</div>
      <div class="stat-item">最终得分：{{ score }} 分</div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="resetGame">重新作答</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import pptxgen from 'pptxgenjs'
import { ElMessage } from 'element-plus'

// 原始后端外层结构
const resData = ref({
  version: "1.0",
  questionList: []
})
// 提取题目数组
const rawList = computed(() => resData.value.questionList)

// 答题状态
const currentIndex = ref(0)
const score = ref(0)
const rightCount = ref(0)
const wrongCount = ref(0)
const selectKey = ref('')
const isAnswered = ref(false)
const dialogVisible = ref(false)

// 当前题目
const currentQuestion = computed(() => {
  if (!rawList.value.length) return null
  return rawList.value[currentIndex.value]
})
// 进度百分比
const progressPercent = computed(() => {
  if (!rawList.value.length) return 0
  return ((currentIndex.value + 1) / rawList.value.length * 100).toFixed(0)
})

// 选项样式判断
const getOptionClass = (key) => {
  if (!isAnswered.value) return ''
  const correct = currentQuestion.value.answer
  if (key === selectKey.value && key === correct) return 'right'
  if (key === selectKey.value && key !== correct) return 'user-wrong'
  if (key === correct) return 'right'
  return ''
}

// 选择答案
const handleSelect = (key) => {
  if (isAnswered.value || !currentQuestion.value) return
  isAnswered.value = true
  selectKey.value = key
  const correct = currentQuestion.value.answer
  if (key === correct) {
    score.value += 10
    rightCount.value += 1
  } else {
    wrongCount.value += 1
  }
  setTimeout(nextQuestion, 1100)
}

// 切换下一题
const nextQuestion = () => {
  currentIndex.value += 1
  selectKey.value = ''
  isAnswered.value = false
  if (currentIndex.value >= rawList.value.length) {
    dialogVisible.value = true
  }
}

// 重置游戏
const resetGame = () => {
  currentIndex.value = 0
  score.value = 0
  rightCount.value = 0
  wrongCount.value = 0
  selectKey.value = ''
  isAnswered.value = false
  dialogVisible.value = false
}

// 导出标准pptx文件
const exportPPT = async () => {
  const list = rawList.value
  if (!list.length) {
    ElMessage.warning('暂无题目数据，无法导出PPT')
    return
  }
  const ppt = new pptxgen()
  ppt.layout = pptxgen.Layout.LAYOUT_16x9
  // 封面页
  const cover = ppt.addSlide()
  cover.addText('AI问答练习题课件', {
    x: 2, y: 2, w: 6, fontSize: 36, bold: true, align: 'center'
  })
  cover.addText('后端智能文本生成题目整理', {
    x: 2, y: 3.4, w: 6, fontSize: 20, align: 'center', color: '666666'
  })
  // 逐一生成题目页面
  list.forEach((item, idx) => {
    const slide = ppt.addSlide()
    slide.addText(`第${idx + 1}题：${item.question}`, {
      x: 0.4, y: 0.3, w: 9.2, fontSize: 24, bold: true
    })
    slide.addText(`A.${item.A}\nB.${item.B}\nC.${item.C}\nD.${item.D}`, {
      x: 0.4, y: 1.2, w: 9.2, fontSize: 18, lineSpacing: 26
    })
    slide.addText(`标准答案：${item.answer}`, {
      x: 0.4, y: 4.1, fontSize: 20, bold: true, color: 'FF0000'
    })
  })
  await ppt.writeFile({ fileName: 'AI题库.pptx' })
  ElMessage.success('PPTX文件下载完成')
}

// 【对接后端接口核心方法】挂载时拉取接口
const loadBackendData = async () => {
  try {
    // 替换为你的后端请求地址
    const res = await fetch('/api/doc/game')
    const json = await res.json()
    resData.value = json
    resetGame()
  } catch (err) {
    ElMessage.error('题目加载失败，请检查后端接口')
    console.error(err)
  }
}

onMounted(() => {
  loadBackendData()
})
</script>

<style scoped>
.game-container {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #4158D0, #C850C0);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 30px;
}
.game-card {
  width: 100%;
  max-width: 760px;
  background: #fff;
  border-radius: 20px;
  padding: 36px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.title {
  font-size: 26px;
  font-weight: bold;
  color: #333;
}
.score {
  font-size: 18px;
  color: #666;
}
.question-text {
  font-size: 22px;
  line-height: 1.6;
  margin: 30px 0;
  color: #222;
  min-height: 72px;
}
.empty-tip {
  text-align: center;
  font-size: 18px;
  color: #999;
  margin: 40px 0;
}
.option-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 36px;
}
.option-item {
  padding: 16px 20px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  font-size: 17px;
  cursor: pointer;
  transition: all 0.25s ease;
}
.option-item:hover {
  border-color: #4158D0;
  background-color: #f0f4ff;
}
.right {
  border-color: #10b981;
  background-color: #ecfdf5;
  color: #047857;
}
.user-wrong {
  border-color: #ef4444;
  background-color: #fef2f2;
  color: #b91c1c;
}
.btn-group {
  display: flex;
  gap: 16px;
  justify-content: center;
}
.stat-item {
  font-size: 18px;
  margin: 10px 0;
  text-align: center;
}
</style>