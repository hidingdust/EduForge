<template>
  <div class="page-wrap">
    <Header />

    <main class="main-content">
      <div class="page-title">
        <h1>我的下载</h1>
        <p>查看所有已下载的教育资源记录</p>
      </div>

      <!-- 统计栏 -->
      <div class="stat-bar">
        <div class="stat-item">
          <span class="num">{{ downloadList.length }}</span>
          <span class="text">已下载资源</span>
        </div>
      </div>

      <!-- 下载内容网格 -->
      <div class="card-grid">
        <div v-for="item in downloadList" :key="item.id" class="download-card glass">
          <div class="card-img">
            <img :src="item.img" alt="">
          </div>
          <div class="card-body">
            <h3 class="title">{{ item.title }}</h3>
            <div class="category-tag">{{ item.category }}<span v-if="item.type"> / {{ item.type }}</span></div>
            <div class="time">下载时间：{{ item.downloadTime }}</div>
          </div>
          <div class="card-action">
            <el-button size="small" @click="handleRedownload(item)">再次下载</el-button>
            <el-button size="small" type="primary" @click="viewDetail(item)">查看详情</el-button>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="downloadList.length === 0" class="empty-tip">
          <p>暂无下载记录</p>
          <el-button type="primary" @click="$router.push('/')">前往资源广场浏览</el-button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import Header from './../components/Header.vue'

const router = useRouter()

// 模拟下载历史数据，和首页资源格式统一
const downloadList = ref([
  {
    id: 1,
    title: "初中数学教学课件资源",
    img: "https://picsum.photos/id/24/400/300",
    category: "教育",
    type: "初中",
    downloadTime: "2026-07-25 14:22"
  },
  {
    id: 2,
    title: "人工智能基础学习资料",
    img: "https://picsum.photos/id/84/400/300",
    category: "科技",
    type: "人工智能",
    downloadTime: "2026-07-23 09:15"
  },
  {
    id: 3,
    title: "小学英语听力素材包",
    img: "https://picsum.photos/id/66/400/300",
    category: "教育",
    type: "小学",
    downloadTime: "2026-07-20 16:40"
  },
  {
    id: 4,
    title: "前端IT开发入门文档",
    img: "https://picsum.photos/id/106/400/300",
    category: "科技",
    type: "IT科技",
    downloadTime: "2026-07-18 11:06"
  },
  {
    id: 5,
    title: "生活创意手工教程",
    img: "https://picsum.photos/id/88/400/300",
    category: "生活",
    type: "",
    downloadTime: "2026-07-15 10:33"
  }
])

// 再次下载
const handleRedownload = (item) => {
  console.log("重新下载资源", item)
}

// 跳转资源详情
const viewDetail = (item) => {
  console.log("查看详情", item)
  // 后续可以跳详情页，传资源id
}
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
  font-size: 42px;
  margin: 0 0 8px;
  color: #111;
}

.page-title p {
  font-size: 19px;
  color: #666;
}

.stat-bar {
  margin-bottom: 24px;
  padding-left: 8px;
}

.stat-item .num {
  font-size: 26px;
  font-weight: bold;
  color: #2563eb;
  margin-right: 8px;
}

.stat-item .text {
  font-size: 16px;
  color: #555;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.download-card {
  border-radius: 20px;
  overflow: hidden;
  padding-bottom: 16px;
  transition: 0.24s ease;
}

.download-card:hover {
  box-shadow: 0 6px 22px rgba(37, 99, 235, 0.15);
}

.card-img {
  width: 100%;
  height: 180px;
  overflow: hidden;
}

.card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-body {
  padding: 14px 16px;
}

.title {
  font-size: 17px;
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-tag {
  font-size: 13px;
  color: #2563eb;
  margin-bottom: 4px;
}

.time {
  font-size: 13px;
  color: #777;
}

.card-action {
  padding: 0 16px;
  display: flex;
  gap: 10px;
}

.empty-tip {
  grid-column: 1 / -1;
  text-align: center;
  padding: 80px 0;
  font-size: 18px;
  color: #888;
}
</style>

<style>
.glass {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(10px);
}
</style>