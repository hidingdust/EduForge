<template>
  <div class="forget-wrap">
    <div class="forget-card glass-card">
      <div class="card-header">
        <h2>重置密码</h2>
        <p>输入信息找回您的账号密码</p>
      </div>

      <el-form
        ref="forgetRef"
        :model="forgetForm"
        :rules="rules"
      >
        <el-form-item prop="phone">
          <el-input
            v-model="forgetForm.phone"
            placeholder="手机号"
          ></el-input>
        </el-form-item>

        <!-- <el-form-item prop="code">
          <div class="code-row">
            <el-input
              v-model="forgetForm.code"
              placeholder="短信验证码"
            ></el-input>
            <el-button class="code-btn" :disabled="countdown > 0" @click="getCode">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item> -->

        <el-form-item prop="newPwd">
          <el-input
            v-model="forgetForm.newPwd"
            show-password
            placeholder="新密码"
          ></el-input>
        </el-form-item>

        <el-form-item prop="confirmPwd">
          <el-input
            v-model="forgetForm.confirmPwd"
            show-password
            placeholder="确认新密码"
          ></el-input>
        </el-form-item>

        <el-form-item>
          <el-button class="submit-btn" @click="onReset">
            确认重置
          </el-button>
        </el-form-item>

        <div class="back-tip">
          <span @click="goLogin">← 返回登录</span>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const forgetRef = ref(null)
const countdown = ref(0)
let timer = null

const forgetForm = reactive({
  phone: '',
  code: '',
  newPwd: '',
  confirmPwd: ''
})

const rules = reactive({
  phone: [
    { required: true, message: '请填写手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ],
  newPwd: [
    { required: true, message: '请设置新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPwd: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== forgetForm.newPwd) {
          callback(new Error('两次输入密码不一致'))
        } else callback()
      },
      trigger: 'blur'
    }
  ]
})

// 获取验证码倒计时
const getCode = () => {
  if (!/^1[3-9]\d{9}$/.test(forgetForm.phone)) {
    ElMessage.warning('请先输入正确手机号')
    return
  }
  ElMessage.success('验证码已发送（模拟）')
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

// 重置密码
const onReset = async () => {
  await forgetRef.value.validate((valid) => {
    if (valid) {
      ElMessage.success('密码重置成功，请前往登录！')
      router.push('/login')
    } else {
      ElMessage.warning('请完善表单信息')
    }
  })
}

const goLogin = () => {
  router.push('/login')
}

// 页面销毁清除定时器，防止内存泄漏
onUnmounted(() => {
  if(timer) clearInterval(timer)
})
</script>

<style scoped>
.forget-wrap {
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

.forget-card {
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

.code-row {
  display: flex;
  gap:12px;
}
.code-row .el-input {
  flex: 1;
}
.code-btn {
  white-space: nowrap;
  border-radius:999px;
  background:#3b9aff;
  color:#fff;
  border:none;
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

.back-tip {
  margin-top: 22px;
  text-align: center;
  font-size: 15px;
}
.back-tip span {
  color: #2563eb;
  cursor: pointer;
  font-weight: 500;
}
</style>