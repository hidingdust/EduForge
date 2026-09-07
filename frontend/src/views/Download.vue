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
        <div v-for="item in downloadList" :key="item.worksId" class="download-card glass">
          <div class="card-img">
            <img
              v-if="item.coverUrl && !imgErrorMap[item.worksId]"
              :src="item.coverUrl"
              alt=""
              @error="imgErrorMap[item.worksId] = true"
            >
            <div v-else class="img-placeholder">
              <span>暂无封面</span>
            </div>
          </div>
          <div class="card-body">
            <h3 class="title">{{ item.title }}</h3>
            <!-- 后端已经拼接好完整分类文本，直接渲染 -->
            <div class="category-tag">{{ item.categoryFullName }}</div>
            <div class="time">下载时间：{{ formatTime(item.downloadTime) }}</div>
          </div>
          <div class="card-action">
            <el-button size="small" @click="handleRedownload(item)">再次下载</el-button>
            <el-button size="small" type="primary" @click="viewDetail(item)">查看详情</el-button>
          </div>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="empty-tip">
          <p>加载中……</p>
        </div>

        <!-- 空状态 -->
        <div v-if="!loading && downloadList.length === 0" class="empty-tip">
          <p>暂无下载记录</p>
          <el-button type="primary" @click="$router.push('/')">前往资源广场浏览</el-button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Header from './../components/Header.vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()

// 真实后端数据
const downloadList = ref([])
const loading = ref(false)
// 记录哪些封面图加载失败，用于显示占位图
const imgErrorMap = ref({})

/**
 * 获取我的下载列表
 */
const fetchDownloadList = async () => {
  try {
    loading.value = true
    // request 封装已自带 baseURL + Authorization 请求头注入，并会自动解包 response.data
    const res = await request({
      url: '/mydownload/myDownload',
      method: 'GET'
    })
    if (res.code === 200) {
      downloadList.value = res.data
    }
  } catch (err) {
    console.error('获取下载列表失败', err)
    ElMessage.error('获取下载列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 格式化后端返回的LocalDateTime：2026-07-25T14:22:00 → 2026-07-25 14:22
 */
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').substring(0, 16)
}

// 页面挂载，自动请求接口
onMounted(() => {
  fetchDownloadList()
})

// 再次下载，向后端传递worksId
const handleRedownload = (item) => {
  console.log("再次下载，资源worksId：", item.worksId)
  // todo: 调用再次下载接口，参数 item.worksId
}

// 跳转课堂渲染页面，携带 taskId
const viewDetail = (item) => {
  console.log("查看详情 taskId", item.taskId)
  router.push({
    path: '/classroom-render',
    query: { taskId: item.taskId }
  })
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

.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0e7ff 0%, #d1e0fd 100%);
  color: #4b5563;
  font-size: 14px;
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