<template>
  <div class="profile-page">
    <div class="container">
      <div class="layout-wrap">
        <!-- 左侧信息栏 -->
        <div class="left-sidebar glass-card">
          <div class="top-info">
            <div class="avatar-area">
              <el-avatar :size="110" :src="userStore.userInfo.avatarUrl || defaultAvatarUrl">
                {{ userStore.userInfo.userName?.charAt(0) || 'U' }}
              </el-avatar>
            </div>
            <div class="info-text">
              <h2 class="username">{{ userStore.userInfo.userName || "匿名用户" }}</h2>
              <p class="gender-icon">
                <el-icon v-if="userStore.userInfo.gender === '女'" color="#f472b6"><Female /></el-icon>
                <el-icon v-if="userStore.userInfo.gender === '男'" color="#409eff"><Male /></el-icon>
                <span v-if="!userStore.userInfo.gender" style="color:#999">未设置性别</span>
              </p>
              <p class="phone">📱 手机号：{{ userStore.userInfo.phone || "未填写" }}</p>
              <p class="phone">📧 邮箱：{{ userStore.userInfo.email || "未填写" }}</p>
              <p class="quote">💬 个人语录：{{ userStore.userInfo.introduction || "暂无个人语录" }}</p>
              <p class="score">
                ⭐ 我的积分：<span class="score-number">{{ safeNum(userStore.score) }}</span>
              </p>
              <p class="coin">
                💰 我的金币：<span class="coin-number">{{ safeNum(userStore.gold) }}</span>
              </p>
              <p class="subject">📚 所选分区：{{ userStore.userInfo.subject || "未选择" }}</p>
            </div>

            <div class="btn-group">
              <el-button class="gradient-btn" @click="openEdit">✏️ 编辑资料</el-button>
            </div>
          </div>

          <div class="bottom-btns">
            <el-button class="gradient-btn" @click="$router.push('/')">
              ← 返回首页
            </el-button>
            <el-button class="logout-btn" @click="handleLogout">🚪 退出登录</el-button>
          </div>
        </div>

        <!-- 右侧内容区域 -->
        <div class="right-content glass-card">
          <el-tabs v-model="activeTab" @tab-change="onTabChange">
            <el-tab-pane label="🎨 我的作品" name="work">
              <template #label>
                <span>🎨 我的作品</span>
              </template>
            </el-tab-pane>
            <el-tab-pane label="❤️ 我的点赞" name="liked">
              <template #label>
                <span>❤️ 我的点赞</span>
              </template>
            </el-tab-pane>
          </el-tabs>

          <div class="content-grid">
            <!-- 我的作品 -->
            <div v-if="activeTab === 'work'" class="grid-container" v-loading="workLoading">
              <WorkCard
                v-for="item in workList"
                :key="item.id"
                :info="item"
                :isUpload="false"
                :showDelete="true"
                @delete="deleteWork(item.id)"
              />
              <div v-if="workList.length === 0" class="empty-tip">
                <el-empty description="暂无作品，快去创作吧！" />
              </div>
            </div>

            <!-- 我的点赞 -->
            <div v-if="activeTab === 'liked'" class="grid-container" v-loading="likedLoading">
              <WorkCard
                v-for="item in likedList"
                :key="item.id"
                :info="item"
                :isUpload="false"
                :showDelete="false"
                @unlike="handleUnlike"
              />
              <div v-if="likedList.length === 0" class="empty-tip">
                <el-empty description="还没有点赞任何作品，去首页逛逛吧！" />
              </div>
            </div>

            <!-- 分页区域 -->
            <div class="pagination-wrap" style="margin-top:24px; text-align:center;">
              <el-pagination
                v-model:current-page="query.pageNum"
                v-model:page-size="query.pageSize"
                :total="activeTab === 'work' ? workTotal : likedTotal"
                :page-sizes="[6, 12, 18]"
                layout="total, sizes, prev, pager, next, jumper"
                @current-change="loadData"
                @size-change="loadData"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import WorkCard from '../components/WorkCard.vue'
import { Female, Male } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { getUserProfile } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('work')

// ✅ 统一安全数字函数
const safeNum = (val) => {
  if (val === undefined || val === null) return 0
  const num = Number(val)
  return isNaN(num) ? 0 : num
}

// 默认头像
const defaultAvatarUrl = "https://cbu01.alicdn.com/img/ibank/O1CN01OX9ONz1SqzSapEHll_!!2216982922299-0-cib.jpg"

// 分页查询参数
const query = ref({
  keyWord: '',
  categoryId: null,
  pageNum: 1,
  pageSize: 6,
  sortType: 'createTime'
})

// 列表数据
const workList = ref([])
const likedList = ref([])
const workLoading = ref(false)
const likedLoading = ref(false)
const workTotal = ref(0)
const likedTotal = ref(0)

// Tab切换
const onTabChange = () => {
  query.value.pageNum = 1
  query.value.keyWord = ''
  query.value.categoryId = null
  loadData()
}

// 加载列表数据
const loadData = async () => {
  if (activeTab.value === 'work') {
    workLoading.value = true
  } else {
    likedLoading.value = true
  }

  try {
    let res
    if (activeTab.value === 'work') {
      res = await request.post('/works/profile/myWorks', {
        keyWord: query.value.keyWord,
        categoryId: query.value.categoryId,
        pageNum: query.value.pageNum,
        pageSize: query.value.pageSize,
        sortType: query.value.sortType
      })
    } else {
      res = await request.post('/works/profile/myLiked', {
        keyWord: query.value.keyWord,
        categoryId: query.value.categoryId,
        pageNum: query.value.pageNum,
        pageSize: query.value.pageSize,
        sortType: query.value.sortType
      })
    }
    const pageData = res.data || {}
    const list = pageData.content || []

    if (activeTab.value === 'work') {
      workList.value = list
      workTotal.value = pageData.totalElements || 0
    } else {
      likedList.value = list
      likedTotal.value = pageData.totalElements || 0
    }
  } catch (err) {
    ElMessage.error('数据加载失败：' + err.message)
    console.error(err)
  } finally {
    workLoading.value = false
    likedLoading.value = false
  }
}

// 加载用户信息
const loadUserProfile = async () => {
  try {
    const userRes = await getUserProfile()
    const userData = userRes.data
    if (userData && userData.userName) {
      userStore.setUserInfo(userData)
    }
    const signRes = await request.get('/user/sign/info')
    if (signRes.data?.data?.totalScore !== undefined) {
      userStore.setScore(safeNum(signRes.data.data.totalScore))
    }
    await loadCoinData()
  } catch (err) {
    console.error('加载用户信息错误:', err)
  }
}

// ✅【已修正】加载金币数据，适配 request 已解包一层 data 的结构
const loadCoinData = async () => {
  try {
    const res = await request.get('/gold/info')
    console.log('金币原始响应:', res)

    let rawVal
    // 核心修正：res 直接是后端响应体 {code, data:{gold:xxx}, msg}
    // 优先级1：标准结构 res.data.gold
    if (res?.data?.gold !== undefined) {
      rawVal = res.data.gold
    }
    // 优先级2：兼容 res.data 直接是数字
    else if (res?.data !== undefined) {
      rawVal = res.data
    }
    // 优先级3：兜底
    else {
      rawVal = res
    }

    // 数值规范化
    let goldNum = Number(rawVal)
    if (isNaN(goldNum) || !isFinite(goldNum)) goldNum = 0
    goldNum = Math.max(Math.floor(goldNum), 0)

    console.log('✅ 最终金币数值:', goldNum)
    userStore.setGold(goldNum)
  } catch (err) {
    console.error('❌ 加载金币数据失败:', err)
    userStore.setGold(0)
  }
}

// 编辑资料跳转
const openEdit = () => router.push('/editProfile')

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出当前账号吗？', '提示', {
    confirmButtonText: '确认退出',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }).catch(() => {})
}

// 删除作品
const deleteWork = async (id) => {
  ElMessageBox.confirm('确定删除这条作品？删除后无法恢复！', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/works/delete/${id}`)
      ElMessage.success('删除成功')
      loadData()
    } catch (err) {
      ElMessage.error(err.message || '删除失败')
    }
  }).catch(() => {})
}

// 取消点赞（仅前端临时移除）
const handleUnlike = (workId) => {
  likedList.value = likedList.value.filter(item => item.id !== workId)
  likedTotal.value = Math.max(0, likedTotal.value - 1)
  ElMessage.warning('⚠️ 当前版本仅本地临时取消，刷新页面会恢复')
}

// 页面挂载
onMounted(async () => {
  await loadUserProfile()
  loadData()
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  padding: 32px 24px;
  background:
    radial-gradient(ellipse at bottom left, #93c5fd 0%, transparent 62%),
    radial-gradient(ellipse at bottom right, #81b4fc 0%, transparent 62%),
    #ffffff;
}

.container {
  max-width: 1720px;
  margin: 0 auto;
}

.layout-wrap {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 24px;
  align-items: stretch;
}

.left-sidebar {
  min-height: 760px;
  padding: 36px 28px;
  border-radius: 24px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.top-info {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.avatar-area {
  text-align: center;
}

.info-text {
  text-align: center;
}

.username {
  font-size: 30px;
  margin: 0 0 12px;
  background: linear-gradient(90deg, #2563eb, #7c3aed);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.info-text p {
  margin: 6px 0;
  font-size: 15px;
  color: #666;
}

.gender-icon {
  font-size: 22px;
  margin: 4px 0 !important;
}

/* 积分样式 */
.info-text .score {
  color: #ffd700;
  font-weight: 600;
  font-size: 17px;
  margin: 8px 0;
}

.info-text .score-number {
  font-size: 24px;
  color: #ffb800;
  text-shadow: 0 0 8px rgba(255, 215, 0, 0.6);
  font-weight: 700;
  letter-spacing: 1px;
}

.info-text .score-number::before {
  content: "✨";
  margin-right: 4px;
}

.info-text .score-number::after {
  content: "✨";
  margin-left: 4px;
}

/* 金币样式 */
.info-text .coin {
  color: #ffd700;
  font-weight: 600;
  font-size: 17px;
  margin: 8px 0;
}

.info-text .coin-number {
  font-size: 24px;
  color: #f59e0b;
  text-shadow: 0 0 8px rgba(245, 158, 11, 0.6);
  font-weight: 700;
  letter-spacing: 1px;
}

.info-text .coin-number::before {
  content: "🪙";
  margin-right: 4px;
}

.info-text .coin-number::after {
  content: "🪙";
  margin-left: 4px;
}

.info-text .subject {
  color: #3b9aff;
  font-weight: 500;
}

.btn-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bottom-btns {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 8px;
}

:deep(.el-button) {
  margin-block: 0 !important;
  margin-inline: 0 !important;
}

.gradient-btn {
  width: 100%;
  height: 44px;
  border-radius: 999px;
  background: linear-gradient(90deg, #3b9aff, #936aff);
  border: none;
  color: #fff;
  font-size: 16px;
  transition: all 0.3s ease;
}

.gradient-btn:hover {
  background: linear-gradient(90deg, #298cf3, #8258ee);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 154, 255, 0.3);
}

.logout-btn {
  width: 100%;
  height: 44px;
  border-radius: 999px;
  border: 1px solid #dd4444;
  color: #dd4444;
  background: #fff;
  font-size: 16px;
  transition: all 0.3s ease;
}

.logout-btn:hover {
  background: #dd4444;
  color: #fff;
}

.right-content {
  padding: 24px;
  border-radius: 24px;
  min-height: 760px;
}

.content-grid {
  margin-top: 24px;
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 20px;
}

.empty-tip {
  width: 100%;
  text-align: center;
  padding: 60px 0;
}

:deep(.el-tabs__header) {
  margin: 0 0 20px;
}

:deep(.el-tabs__item) {
  font-size: 18px;
  font-weight: 500;
}

:deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #3b9aff, #936aff);
  height: 4px;
  border-radius: 4px;
}

:deep(.el-tabs__item.is-active) {
  color: #3b9aff;
}

@media (max-width: 1400px) {
  .grid-container {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 1024px) {
  .layout-wrap {
    grid-template-columns: 1fr;
  }
  .left-sidebar {
    min-height: auto;
  }
  .grid-container {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .grid-container {
    grid-template-columns: repeat(2, 1fr);
  }
  .profile-page {
    padding: 16px 12px;
  }
}

@media (max-width: 480px) {
  .grid-container {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
.glass-card {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 6px 30px rgba(160, 180, 220, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.85);
}
</style>