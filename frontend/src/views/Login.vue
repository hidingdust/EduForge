<template>
  <div class="login-wrap">
    <div class="login-card glass-card">
      <div class="card-header">
        <h2>账号登录</h2>
        <p>欢迎回到 Education Platform</p>
      </div>

      <el-form
        ref="loginRef"
        :model="loginForm"
        :rules="rules"
        @submit.prevent
      >
        <el-form-item prop="phone">
          <el-input
            v-model="loginForm.phone"
            placeholder="手机号"
            maxlength="11"
          ></el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            show-password
            placeholder="密码"
          ></el-input>
        </el-form-item>

        <!-- 忘记密码 -->
        <div class="forget-pwd-line">
          <span @click="goForgot">忘记密码？</span>
        </div>

        <el-form-item>
          <el-button 
            class="submit-btn" 
            @click="onLogin"
            :loading="loading"
          >
            立即登录
          </el-button>
        </el-form-item>

        <div class="register-tip">
          还没有账号？<span @click="goRegister">前往注册</span>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { loginApi } from '@/api/user'
// 👉引入pinia仓库
import { useUserStore } from '@/stores/user'

const router = useRouter()
// 👉实例化
const userStore = useUserStore()

const loginRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  phone: '',
  password: ''
})

const rules = reactive({
  phone: [
    { required: true, message: '请填写手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
})

const onLogin = async () => {
  if (loading.value) {
    return
  }

  const valid = await loginRef.value.validate().catch(() => false);
  if (!valid) {
    ElMessage.warning('请完善表单信息')
    return
  }

  loading.value = true
  try {
    console.log("提交登录参数", {...loginForm})
    const res = await loginApi(loginForm)
    console.log(res);
    
    if (res.code === 200) {
  userStore.setToken(res.data.token)
  userStore.setUserInfo({
    phone: res.data.phone,
    userName: res.data.userName || '',
    avatarUrl: res.data.avatarUrl || ''
  })
  ElMessage.success(res.msg || '登录成功！')
  router.push('/home')
} else {
      ElMessage.error(res.msg || '手机号或密码错误')
    }
  } catch (err) {
    ElMessage.error('网络请求失败，请检查服务')
    console.error('登录接口异常：', err)
  } finally {
    loading.value = false
  }
}

// 跳转注册页
const goRegister = () => {
  router.push('/register')
}
// 跳转忘记密码页
const goForgot = () => {
  router.push('/forget')
}
</script>

<style scoped>
.login-wrap {
  width: 100%;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  background:
    radial-gradient(ellipse at bottom left, #93c5fd 0%, transparent 62%),
    radial-gradient(ellipse at bottom right, #81b4fc 0%, transparent 62%),
    #ffffff;
}

.login-card {
  width: 480px;
  padding: 40px 36px;
  border-radius: 32px;
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 6px 30px rgba(160,180,220,0.14);
  border: 1px solid rgba(255,255,255,0.85);
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}
.card-header h2 {
  font-size: 32px;
  margin: 0 0 10px;
  color: #111;
}
.card-header p {
  font-size: 16px;
  color: #555;
  margin: 0;
}

:deep(.el-input__wrapper) {
  border-radius: 999px !important;
  padding: 10px 18px !important;
  box-shadow: 0 1px 6px rgba(170,185,210,0.12) !important;
}
:deep(.el-form-item) {
  margin-bottom: 18px !important;
}

.forget-pwd-line {
  text-align: right;
  margin-bottom: 8px;
}
.forget-pwd-line span {
  color: #2563eb;
  cursor: pointer;
  font-size: 14px;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 999px;
  background: linear-gradient(90deg, #3b9aff, #936aff);
  border: none;
  color: #fff;
  font-size: 17px;
}
.submit-btn:hover {
  background: linear-gradient(90deg, #298cf3, #8258ee);
}

.register-tip {
  margin-top: 22px;
  text-align: center;
  font-size: 15px;
}
.register-tip span {
  color: #2563eb;
  cursor: pointer;
  font-weight: 500;
}
</style>