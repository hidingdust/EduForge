// src/stores/user.js
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    // ✅ 保持空对象初始化
    userInfo: {},
    token: localStorage.getItem('token') || '',
    score: 0, // 积分独立状态（不再与 userInfo 绑定）
    gold: 0 // ✅ 新增：金币独立状态
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    userId: (state) => state.userInfo?.userId || null,
    userName: (state) => state.userInfo?.userName || '',
    userAvatar: (state) => state.userInfo?.avatarUrl || '',
    // ✅ 新增：确保手机号能正确读取（兼容多种后端字段名）
    userPhone: (state) => {
      // 优先尝试后端可能返回的字段名
      return state.userInfo?.phone
        || state.userInfo?.mobile
        || state.userInfo?.phoneNumber
        || state.userInfo?.tel
        || ''
    }
  },

  actions: {
    setUserInfo(userInfo) {
      if (!userInfo) {
        this.userInfo = {}
        return
      }

      // 🔑 关键修改1：统一手机号字段名映射
      const formattedUserInfo = {
        ...userInfo,
        // 将后端可能的手机号字段统一映射为 phone
        phone: userInfo.phone
          || userInfo.mobile
          || userInfo.phoneNumber
          || userInfo.tel
          || ''
      }

      this.userInfo = { ...formattedUserInfo }

      // 🔑 关键修改2：只从 userInfo 初始化 score（避免覆盖独立状态）
      if (userInfo.score !== undefined) {
        this.score = userInfo.score
      }
      // ✅ 新增：从 userInfo 初始化 gold
      if (userInfo.gold !== undefined) {
        this.gold = userInfo.gold
      }

      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    },

    setToken(token) {
      this.token = token
      if (token) {
        localStorage.setItem('token', token)
      } else {
        localStorage.removeItem('token')
      }
    },

    // 🔑 关键修改3：彻底移除对 userInfo 的修改
    setScore(score) {
      this.score = score
      // ❌ 删除以下3行（不再修改 userInfo）
      // if (this.userInfo && Object.keys(this.userInfo).length > 0) {
      //   this.userInfo.score = score
      //   localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      // }
    },

    // ✅ 新增：设置金币
    setGold(gold) {
      this.gold = gold
      // 可选：如果需要持久化，可以存到 localStorage
      // localStorage.setItem('userGold', String(gold))
    },

    init() {
      const token = localStorage.getItem('token')
      const userInfoStr = localStorage.getItem('userInfo')

      if (token && userInfoStr) {
        try {
          const userInfo = JSON.parse(userInfoStr)
          this.token = token
          this.userInfo = { ...userInfo }

          // 优先使用独立存储的 score（更可靠）
          const storedScore = localStorage.getItem('userScore')
          this.score = storedScore ? Number(storedScore) : (userInfo.score || 0)

          // ✅ 新增：恢复金币
          const storedGold = localStorage.getItem('userGold')
          this.gold = storedGold ? Number(storedGold) : (userInfo.gold || 0)

          return true
        } catch (e) {
          console.error('恢复用户信息失败:', e)
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          localStorage.removeItem('userScore')
          localStorage.removeItem('userGold') // 同步清理
        }
      }
      return false
    },

    logout() {
      this.userInfo = {}
      this.token = ''
      this.score = 0
      this.gold = 0 // ✅ 新增：清空金币
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('userScore')
      localStorage.removeItem('userGold') // ✅ 新增：清理金币缓存
    }
  }
})