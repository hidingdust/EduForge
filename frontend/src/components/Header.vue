<template>
  <header class="header-wrap glass">
    <!-- Logo区域 - 添加点击事件 -->
    <div class="logo-box" @click="goHome" style="cursor: pointer;">
      <svg viewBox="0 0 100 100" width="38" height="38">
        <path d="M20 25 L50 10 L80 25 L80 75 L50 90 L20 75 Z M50 10 L50 50 M20 25 L50 50 L80 25 M20 75 L50 50 L80 75" fill="none" stroke="#111" stroke-width="3"/>
      </svg>
      <span class="logo-text">Education Platform</span>
    </div>

    <div class="nav-container">
      <div
        v-for="item in navList"
        :key="item.path"
        class="nav-item"
        @click="navClick(item)"
        :class="{active: activeNavName === item.name}"
      >
        {{ item.name }}
      </div>
    </div>

    <!-- 登录右侧：签到悬浮 + 用户下拉 横向排列 -->
    <div v-if="isLoggedIn" class="header-right-group">
      <!-- 签到下拉 -->
      <el-dropdown
        trigger="hover"
        popper-class="sign-popper"
      >
        <div
          class="nav-item sign-trigger"
          :class="{active: isSignToday}"
          @click="handleQuickSign"
        >
          {{ isSignToday ? '✅ 已签到' : '📝 每日签到' }}
        </div>
        <template #dropdown>
          <el-dropdown-menu class="sign-menu">
            <div class="sign-panel">
              <!-- 顶部统计信息 -->
              <div class="sign-stat-row">
                <div class="stat-item">
                  <div class="stat-label">本月签到</div>
                  <div class="stat-num">{{ monthSignCount }}天</div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">连续签到</div>
                  <div class="stat-num">{{ continueSignDay }}天</div>
                </div>
                <div class="stat-item">
                  <div class="stat-label">累计积分</div>
                  <div class="stat-num">{{ totalScore }}</div>
                </div>
              </div>
              <!-- 星期头部 -->
              <div class="week-title-row">
                <span v-for="week in ['日','一','二','三','四','五','六']" :key="week">{{ week }}</span>
              </div>
              <!-- 整月日历格子 -->
              <div class="calendar-grid">
                <div
                  v-for="dayObj in monthDayList"
                  :key="dayObj.day"
                  class="calendar-cell"
                  :class="{
                    signed: dayObj.isSign,
                    today: dayObj.isToday,
                    future: dayObj.isFuture
                  }"
                >
                  {{ dayObj.day }}
                </div>
              </div>
              <div class="sign-tip">
                {{ isSignToday ? '✅ 今日已完成签到' : '💡 点击上方「每日签到」即可一键签到' }}
              </div>
            </div>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <!-- 用户下拉菜单 -->
      <el-dropdown trigger="hover" popper-class="user-dropdown-popper">
        <div class="user-row">
          <span class="user-name">{{ userStore.userName }}</span>
          <el-avatar :size="52" :src="userStore.userAvatar">
            {{ userStore.userName?.charAt(0) || 'U' }}
          </el-avatar>
          <el-icon class="arrow-icon"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="$router.push('/profile')">👤 个人信息</el-dropdown-item>
            <el-dropdown-item @click="$router.push('/gold')">📚 充值入口</el-dropdown-item>
            <el-dropdown-item divided @click="logout">🚪 退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 未登录展示登录按钮 -->
    <div v-else class="not-login-box">
      <el-button type="primary" link size="large" @click="$router.push('/login')">登录 / 注册</el-button>
    </div>
  </header>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUserProfile  } from '@/api/user'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// ========== 用户状态 ==========
const isLoggedIn = computed(() => userStore.isLoggedIn)

// ========== 导航列表 ==========
const navList = [
  { name: '首页', path: '/' },
  { name: '上传', path: '/upload' },
  { name: '下载', path: '/download' },
  { name: '帮助', path: '/help' }
]

const activeNavName = computed(() => {
  const matched = navList.find(nav => nav.path === route.path)
  return matched ? matched.name : '首页'
})

const navClick = (item) => router.push(item.path)

// ✅ 新增：点击 Logo 跳转首页
const goHome = () => {
  router.push('/')
}

// ========== 退出登录 ==========
const logout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

// ========== 签到&积分 ==========
const isSignToday = ref(false)
const continueSignDay = ref(0)
const totalScore = ref(0)
const monthSignCount = ref(0)
const monthDayList = ref([])

// ========== 加载签到数据 ==========
const loadSignData = async () => {
  if (!isLoggedIn.value) {
    return
  }
  
  try {
    const res = await request.get('/user/sign/info')
    console.log('签到数据响应:', res)
    
    const data = res.data?.data || res.data
    
    if (data) {
      isSignToday.value = !!data.signedToday
      continueSignDay.value = data.continuousDays || 0
      totalScore.value = data.totalScore || 0
      monthSignCount.value = data.monthSignDays || 0
      
      if (data.totalScore !== undefined) {
        userStore.setScore(data.totalScore)
      }
      
      const signDays = data.signDays || []
      const now = new Date()
      const year = now.getFullYear()
      const month = now.getMonth() + 1
      const daysInMonth = new Date(year, month, 0).getDate()
      const todayDate = now.getDate()
      
      const newMonthDayList = []
      for (let d = 1; d <= daysInMonth; d++) {
        newMonthDayList.push({
          day: d,
          isSign: signDays.includes(d),
          isToday: d === todayDate,
          isFuture: d > todayDate
        })
      }
      monthDayList.value = newMonthDayList
    }
  } catch (err) {
    console.error('加载签到数据失败:', err)
  }
}

// ========== 一键签到 ==========
const handleQuickSign = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  if (isSignToday.value) {
    ElMessage.info('今日已经签到，无需重复签到')
    return
  }
  
  try {
    const res = await request.post('/user/sign/in')
    console.log('签到响应:', res)
    
    const data = res.data?.data || res.data
    
    if (data) {
      const gainScore = data.gainScore || 10
      ElMessage.success(`签到成功！+${gainScore}积分`)
      await loadSignData()
    } else {
      ElMessage.error('签到失败，请稍后重试')
    }
  } catch (err) {
    console.error('签到失败:', err)
    ElMessage.error(err.response?.data?.msg || err.response?.data?.message || '签到失败，请稍后重试')
  }
}

// ========== 组件挂载时加载数据 ==========
onMounted(async () => {
  if (isLoggedIn.value) {
    try {
      const profileRes = await getUserProfile ()
      if (profileRes.code === 200) {
        userStore.setUserInfo(profileRes.data)
      }
    } catch(e) {
      console.error('获取用户资料失败', e)
    }

    loadSignData()
  }
})
</script>

<style scoped>
.header-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 32px;
  border-radius: 20px;
  box-shadow: 0 4px 18px rgba(160, 175, 200, 0.18);
  gap: 24px;
}

.glass {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.logo-box {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;  /* ✅ 鼠标悬停显示手型 */
}

.logo-box:hover .logo-text {
  opacity: 0.8;
}

.logo-text {
  font-size: 24px;
  font-weight: bold;
  background: linear-gradient(90deg, #2563eb, #7c3aed);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  transition: opacity 0.2s ease;
}

.nav-container {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 8px;
  background: rgba(255,255,255,0.6);
  border-radius: 999px;
  box-shadow: 0 3px 12px rgba(150, 165, 190, 0.16);
}

.nav-item {
  padding: 8px 20px;
  border-radius: 999px;
  font-size: 16px;
  color: #444;
  cursor: pointer;
  transition: all 0.22s ease;
  background: rgba(255,255,255,0.35);
  border: 1px solid rgba(255,255,255,0.6);
  user-select: none;
}

.nav-item.active {
  background: linear-gradient(90deg, rgba(219, 234, 254, 0.92), rgba(224, 242, 254, 0.95), rgba(233, 213, 255, 0.95));
  color: #2563eb;
  font-weight: 500;
  box-shadow: 0 0 14px rgba(37, 99, 235, 0.16);
}

.nav-item:hover:not(.active) {
  background: rgba(255,255,255,0.65);
  color: #111;
}

.header-right-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  outline: none !important;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  color: #111;
}

.arrow-icon {
  font-size: 18px;
  color: #666;
}

.not-login-box {
  display: flex;
  align-items: center;
}

.sign-trigger {
  background: linear-gradient(90deg, rgba(255, 215, 0, 0.15), rgba(255, 200, 0, 0.1));
  border-color: rgba(255, 215, 0, 0.3);
}

.sign-trigger.active {
  background: linear-gradient(90deg, rgba(34, 197, 94, 0.2), rgba(34, 197, 94, 0.1));
  color: #22c55e;
  border-color: rgba(34, 197, 94, 0.3);
}

.sign-trigger:hover:not(.active) {
  background: linear-gradient(90deg, rgba(255, 215, 0, 0.25), rgba(255, 200, 0, 0.2));
}

@media (max-width: 1024px) {
  .header-wrap {
    padding: 12px 20px;
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .nav-container {
    order: 3;
    width: 100%;
    justify-content: center;
    flex-wrap: wrap;
  }
  
  .logo-text {
    font-size: 20px;
  }
}

@media (max-width: 768px) {
  .header-wrap {
    padding: 10px 16px;
  }
  
  .nav-item {
    padding: 6px 14px;
    font-size: 14px;
  }
  
  .user-name {
    font-size: 15px;
  }
  
  .logo-text {
    font-size: 17px;
  }
}

@media (max-width: 480px) {
  .header-wrap {
    padding: 8px 12px;
  }
  
  .nav-item {
    padding: 4px 10px;
    font-size: 12px;
  }
  
  .user-name {
    display: none;
  }
  
  .logo-text {
    font-size: 14px;
  }
  
  .header-right-group {
    gap: 8px;
  }
}
</style>

<style>
.user-dropdown-popper {
  padding: 4px !important;
  border-radius: 18px !important;
  background: linear-gradient(145deg, rgba(255,255,255,0.96), rgba(246,250,254,0.96)) !important;
  backdrop-filter: blur(12px);
  box-shadow: 0 8px 24px rgba(175, 190, 220, 0.18) !important;
  border: 1px solid rgba(255,255,255,0.75) !important;
}

.user-dropdown-popper .el-dropdown-menu__item {
  padding: 12px 20px !important;
  font-size: 16px;
  color: #333;
  background: transparent !important;
}

.user-dropdown-popper .el-dropdown-menu__item:hover {
  background: linear-gradient(90deg, rgba(219,234,254,0.6), rgba(233,213,255,0.6)) !important;
  color: #2563eb;
}

.user-dropdown-popper .el-dropdown-menu__divider {
  margin: 4px 12px !important;
  background: rgba(200,210,230,0.35);
}

.sign-popper {
  padding: 6px !important;
  border-radius: 22px !important;
  background: rgba(255,255,255,0.76) !important;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 8px 28px rgba(160, 175, 200, 0.22) !important;
  border: 1px solid rgba(255, 255, 255, 0.85) !important;
}

.sign-menu {
  min-width: 400px;
  padding: 8px;
}

.sign-panel {
  padding: 12px;
}

.sign-stat-row {
  display: flex;
  justify-content: space-around;
  margin-bottom: 16px;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: #666;
}

.stat-num {
  font-size: 22px;
  font-weight: bold;
  color: #2563eb;
  margin-top: 3px;
}

.week-title-row {
  display: flex;
  text-align: center;
  margin-bottom: 8px;
}

.week-title-row span {
  flex: 1;
  font-size: 14px;
  color: #555;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
  margin-bottom: 16px;
}

.calendar-cell {
  height: 42px;
  line-height: 42px;
  text-align: center;
  border-radius: 10px;
  font-size: 14px;
  background: rgba(235, 242, 255, 0.5);
  color: #444;
}

.calendar-cell.signed {
  background: linear-gradient(90deg, #2563eb, #8b5cf6);
  color: #fff;
  font-weight: 500;
}

.calendar-cell.today:not(.signed) {
  border: 2px solid #2563eb;
}

.calendar-cell.future {
  opacity: 0.4;
}

.calendar-cell:empty {
  background: transparent;
}

.sign-tip {
  text-align: center;
  font-size: 14px;
  color: #666;
  padding-top: 8px;
  border-top: 1px solid rgba(220,230,245,0.4);
}

.el-dropdown__trigger {
  outline: none !important;
}

.el-dropdown__trigger:focus {
  outline: none !important;
}

@media (max-width: 480px) {
  .sign-menu {
    min-width: 320px;
  }
  
  .calendar-cell {
    height: 36px;
    line-height: 36px;
    font-size: 12px;
  }
  
  .stat-num {
    font-size: 18px;
  }
}
</style>