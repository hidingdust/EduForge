<template>
  <div class="page-wrap">
    <Header />

    <main class="main-content">
      <!-- 页面标题区域 -->
      <div class="page-title">
        <h1>平台介绍 & 使用帮助</h1>
        <p>文档一键转化课件，趣味化高效学习，资源自由上传下载</p>
      </div>

      <!-- 全局搜索框（和首页样式完全一致） -->
      <div class="search-box">
        <div class="search-wrapper">
          <el-icon class="icon-left"><Search /></el-icon>
          <el-input
            v-model="searchVal"
            placeholder="搜索功能介绍、使用教程、常见问题"
            @keyup.enter="handleSearch"
          ></el-input>
          <div class="search-btn-circle" @click="handleSearch">
            <el-icon class="search-btn-icon"><Search /></el-icon>
          </div>
        </div>
      </div>

      <!-- 平台总览卡片 -->
      <div class="intro-card glass">
        <h2>Education Platform 是什么？</h2>
        <p>
          本平台主打**文档智能二次创作**，你上传Word、PDF、TXT、讲义等各类学习文档，系统可自动解析文本知识点，快速生成精致PPT课件、趣味答题小游戏、动态演示动画三类学习素材，告别枯燥文字阅读，用可视化、互动化内容提升学习效率。
          所有生成内容支持本地下载存档，也可一键上传至平台资源库分享；同时平台按照学前、小学、初中、高中、大学等人群精细化划分内容板块，精准匹配不同年龄段、学习阶段用户的学习需求。
        </p>
      </div>

      <!-- 三大核心功能模块 -->
      <div class="module-title">
        <h2>三大核心学习创作能力</h2>
      </div>
      <div class="func-grid">
        <div class="func-card glass">
          <div class="card-icon">📑</div>
          <h3>智能PPT一键生成</h3>
          <p>导入学习文档，自动梳理大纲、排版美化，一键生成逻辑清晰、版式精致的教学PPT，支持自由微调样式，适合备课、课堂展示、汇报使用。</p>
        </div>
        <div class="func-card glass">
          <div class="card-icon">❓</div>
          <h3>知识点问答小游戏</h3>
          <p>提取文档考点，自动生成单选题库，制作轻量化答题小游戏，自测知识点掌握程度，刷题复习更轻松，适合课后巩固、考前自测。</p>
        </div>
        <div class="func-card glass">
          <div class="card-icon">🎬</div>
          <h3>知识点演示动画</h3>
          <p>将抽象概念、理科原理转化为简短动态动画，难懂知识点可视化讲解，降低理解门槛，课堂教学、自主学习都适配。</p>
        </div>
      </div>

      <!-- 人群分类说明 -->
      <div class="module-title">
        <h2>分阶段精细化内容分类</h2>
      </div>
      <div class="tag-group">
        <div
          v-for="tag in stageList"
          :key="tag"
          class="tag-item"
          :class="{ active: activeStage === tag }"
          @click="activeStage = tag"
        >
          {{ tag }}
        </div>
      </div>
      <div class="stage-desc glass">
        <p>{{ stageDesc[activeStage] }}</p>
      </div>

      <!-- 上传&下载说明 -->
      <div class="module-title">
        <h2>资源上传与下载规则</h2>
      </div>
      <div class="upload-download-card glass">
        <div class="two-col">
          <div class="col-item">
            <h3>📤 上传资源</h3>
            <ul>
              <li>文档、PPT、课件、动画、题库均可上传分享</li>
              <li>可设置公开/私密，私密内容仅本人查看</li>
              <li>优质公开资源可获得积分奖励，用于兑换下载额度</li>
            </ul>
          </div>
          <div class="col-item">
            <h3>📥 下载资源</h3>
            <ul>
              <li>个人创作的PPT、动画、题库可免费本地下载</li>
              <li>平台他人公开资源消耗积分下载</li>
              <li>下载内容无水印，可二次编辑、自由使用</li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 常见FAQ -->
      <div class="module-title">
        <h2>常见使用问题</h2>
      </div>
      <div class="faq-list">
        <div
          v-for="(item, index) in filterFaqList"
          :key="index"
          class="faq-card glass"
          @click="toggleExpand(index)"
        >
          <div class="faq-header">
            <span class="faq-title">{{ item.title }}</span>
            <el-icon class="expand-icon" :class="{rotate: expandIndex === index}">
              <ArrowDown />
            </el-icon>
          </div>
          <div v-show="expandIndex === index" class="faq-content">
            {{ item.desc }}
          </div>
        </div>
      </div>

      <!-- 底部客服 -->
      <div class="service-box glass">
        <h3>仍有疑问？</h3>
        <p>工作日 9:00-18:00 在线客服随时为你解答功能使用、创作报错、积分问题</p>
        <el-button type="primary" size="large" class="contact-btn">联系在线客服</el-button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search, ArrowDown } from '@element-plus/icons-vue'
import Header from './../components/Header.vue'

// 搜索
const searchVal = ref('')
const handleSearch = () => {
  console.log('检索关键词：', searchVal.value)
}

// 学习阶段标签
const stageList = ['学前', '小学', '初中', '高中', '大学']
const activeStage = ref('小学')
const stageDesc = {
  学前: '偏向启蒙绘本、简单动画、趣味小游戏，侧重认知启蒙，内容浅显生动。',
  小学: '课堂课件、生字题库、科普动画，贴合小学课本知识点，作业、预习素材丰富。',
  初中: '理化演示动画、考点题库、复习PPT，聚焦中考考点，梳理重难点知识。',
  高中: '重难点专题课件、刷题题库、原理动画，适配高考复习，深度梳理知识点。',
  大学: '专业课课件、论文排版PPT、专业概念解析动画，满足课业、答辩需求。'
}

// FAQ 折叠控制
const expandIndex = ref(-1)
const toggleExpand = (index) => {
  expandIndex.value = expandIndex.value === index ? -1 : index
}

// 问答数据
const faqData = ref([
  {
    title: "支持哪些文档格式生成课件？",
    desc: "支持PDF、Word、TXT、Markdown、笔记文本，单文件最大200MB，过长文档可分段上传生成。"
  },
  {
    title: "生成的PPT、动画可以修改内容吗？",
    desc: "PPT支持文字、版式、颜色自由编辑；动画、题库支持题干与选项修改，可按需二次调整。"
  },
  {
    title: "生成内容是否可以永久保存？",
    desc: "上传至平台云端永久保存，下载到本地也可长期留存，账号不注销不会丢失云端作品。"
  },
  {
    title: "积分不够无法下载资源怎么办？",
    desc: "每日签到、上传优质原创课件、作品被点赞收藏都可以免费获取积分。"
  }
])

// 搜索过滤FAQ
const filterFaqList = computed(() => {
  if (!searchVal.value) return faqData.value
  return faqData.value.filter(item => item.title.includes(searchVal.value) || item.desc.includes(searchVal.value))
})
</script>

<style scoped>
.page-wrap {
  max-width: 1720px;
  margin: 0 auto;
  padding: 24px;
}
.main-content {
  margin-top: 32px;
}
.page-title {
  text-align: center;
  margin-bottom: 32px;
}
.page-title h1 {
  font-size: 48px;
  margin: 0 0 8px;
  color: #111;
}
.page-title p {
  font-size: 20px;
  color: #666;
}

/* 搜索框 全局统一样式 */
.search-box {
  max-width: 900px;
  margin: 0 auto 36px;
}
.search-wrapper {
  position: relative;
  width: 100%;
}
.icon-left {
  position: absolute;
  left: 22px;
  top: 50%;
  transform: translateY(-50%);
  color: #7c8db8;
  font-size: 20px;
  z-index: 2;
}
.search-btn-circle {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 42px;
  height: 42px;
  border-radius: 999px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.35);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.search-btn-circle:hover {
  transform: translateY(-50%) scale(1.05);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.45);
}
.search-btn-icon {
  font-size: 18px;
}
.search-wrapper :deep(.el-input__wrapper) {
  border-radius: 999px;
  height: 56px;
  background-color: #ffffff !important;
  box-shadow:
    0 8px 24px rgba(59, 130, 246, 0.28),
    0 0 0 1px rgba(255, 255, 255, 0.8) inset !important;
  padding: 0 64px !important;
  transition: box-shadow 0.3s ease, transform 0.2s ease;
}
.search-wrapper :deep(.el-input__wrapper:hover) {
  transform: translateY(-1px);
  box-shadow:
    0 12px 28px rgba(59, 130, 246, 0.34),
    0 0 0 1px rgba(255, 255, 255, 0.8) inset !important;
}
.search-wrapper :deep(.el-input__wrapper.is-focus) {
  transform: translateY(-2px);
  box-shadow:
    0 14px 32px rgba(59, 130, 246, 0.4),
    0 0 0 2px rgba(59, 130, 246, 0.15) inset !important;
}
.search-wrapper :deep(.el-input__inner) {
  height: 56px;
  font-size: 16px;
  color: #1e293b;
}
.search-wrapper :deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

/* 通用标签样式 */
.tag-group {
  display: flex;
  gap: 18px;
  justify-content: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.tag-item {
  padding: 8px 22px;
  border-radius: 999px;
  font-size: 14px;
  color: #555862;
  cursor: pointer;
  transition: all 0.22s ease;
  background: linear-gradient(90deg, rgba(255,255,255,0.4), rgba(248,250,255,0.4));
  border: 1px solid rgba(255, 255, 255, 0.7);
  white-space: nowrap;
}
.tag-item:hover {
  background: linear-gradient(90deg, rgba(255,255,255,0.7), rgba(248,250,255,0.7));
  color: #1f2937;
}
.tag-item.active {
  background: linear-gradient(90deg, #dbeafe, #e0f2fe, #f3e8ff);
  color: #2563eb;
  font-weight: 500;
  box-shadow: 0 0 12px rgba(37, 99, 235, 0.18);
}

/* 模块标题 */
.module-title {
  text-align: center;
  margin: 40px 0 24px;
}
.module-title h2 {
  font-size: 26px;
  color: #1e293b;
}

/* 玻璃卡片通用 */
.glass {
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 4px 16px rgba(160, 175, 200, 0.12);
  border-radius: 18px;
  transition: all 0.25s ease;
}
.glass:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 22px rgba(160, 175, 200, 0.18);
}

/* 平台介绍卡片 */
.intro-card {
  max-width: 1100px;
  margin: 0 auto 30px;
  padding: 30px 36px;
}
.intro-card h2 {
  font-size: 24px;
  margin: 0 0 14px;
  color: #111;
}
.intro-card p {
  line-height: 1.8;
  font-size: 16px;
  color: #334155;
}

/* 三大功能网格 */
.func-grid {
  max-width: 1300px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.func-card {
  padding: 28px 24px;
  text-align: center;
}
.card-icon {
  font-size: 46px;
  margin-bottom: 12px;
}
.func-card h3 {
  font-size: 20px;
  margin: 0 0 10px;
  color: #1e293b;
}
.func-card p {
  font-size: 15px;
  line-height: 1.7;
  color: #475569;
}

/* 阶段描述卡片 */
.stage-desc {
  max-width: 900px;
  margin: 0 auto 30px;
  padding: 20px 26px;
  text-align: center;
  font-size: 16px;
  color: #334155;
  line-height: 1.7;
}

/* 上传下载双栏 */
.upload-download-card {
  max-width: 1100px;
  margin: 0 auto 30px;
  padding: 28px;
}
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 30px;
}
.col-item h3 {
  font-size: 20px;
  margin: 0 0 12px;
  color: #1e293b;
}
.col-item ul {
  padding-left: 18px;
  margin: 0;
}
.col-item li {
  line-height: 1.8;
  font-size: 15px;
  color: #475569;
  margin: 6px 0;
}

/* FAQ卡片 */
.faq-list {
  max-width: 1100px;
  margin: 0 auto 40px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.faq-card {
  padding: 20px 26px;
  cursor: pointer;
}
.faq-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.faq-title {
  font-size: 17px;
  font-weight: 500;
  color: #1e293b;
}
.expand-icon {
  font-size: 20px;
  color: #64748b;
  transition: transform 0.24s ease;
}
.expand-icon.rotate {
  transform: rotate(180deg);
}
.faq-content {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(220, 228, 240, 0.6);
  font-size: 15px;
  line-height: 1.7;
  color: #475569;
}

/* 客服卡片 */
.service-box {
  max-width: 1100px;
  margin: 0 auto;
  text-align: center;
  padding: 36px 24px;
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.6), rgba(233, 213, 255, 0.6));
}
.service-box h3 {
  font-size: 24px;
  margin: 0 0 8px;
  color: #1e293b;
}
.service-box p {
  font-size: 16px;
  color: #475569;
  margin-bottom: 24px;
}
.contact-btn {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border: none;
  padding: 10px 32px;
  font-size: 16px;
}

/* 适配平板手机 */
@media (max-width: 1024px) {
  .func-grid {
    grid-template-columns: repeat(2,1fr);
  }
  .two-col {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 640px) {
  .func-grid {
    grid-template-columns: 1fr;
  }
  .page-title h1 {
    font-size: 32px;
  }
}
</style>