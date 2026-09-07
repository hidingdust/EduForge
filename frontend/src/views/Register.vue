<template>
  <div class="register-wrap">
    <div class="register-card glass-card">
      <div class="card-header">
        <h2>账号注册</h2>
        <p>创建你的 Education Platform 账号，开启资源分享之旅</p>
      </div>

      <el-form
        ref="registerRef"
        :model="registerForm"
        :rules="rules"
      >
        <!-- 移除用户名表单项，只保留手机号 -->
        <el-form-item prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="手机号"
          ></el-input>
        </el-form-item>

        <div class="pwd-row">
          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              show-password
              placeholder="密码（6~16位）"
            ></el-input>
          </el-form-item>
          <el-form-item prop="confirmPwd">
            <el-input
              v-model="registerForm.confirmPwd"
              show-password
              placeholder="确认密码"
            ></el-input>
          </el-form-item>
        </div>

        <el-form-item>
          <el-button class="submit-btn" @click="onRegister" :loading="loading">
            立即注册
          </el-button>
        </el-form-item>

        <div class="login-tip">
          已有账号？<span @click="goLogin">前往登录</span>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const registerRef = ref(null)
const loading = ref(false)

// 移除userName字段
const registerForm = reactive({
  phone: '',
  password: '',
  confirmPwd: ''
})

// 删除userName校验规则
const rules = reactive({
  phone: [
    { required: true, message: '请填写手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码长度6~16位', trigger: 'blur' }
  ],
  confirmPwd: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入密码不一致'))
        } else callback()
      },
      trigger: 'blur'
    }
  ]
})

const onRegister = async () => {
  await registerRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请完善表单信息')
      return
    }
    try {
      loading.value = true
      const res = await request.post('/user/register', registerForm)
      if (res.code === 200) {
        ElMessage.success(res.msg || '注册成功！')
        router.push('/login')
      } else {
        ElMessage.error(res.msg || '注册失败')
      }
    } catch (err) {
      ElMessage.error('网络异常，请稍后重试')
      console.error(err)
    } finally {
      loading.value = false
    }
  })
}

const goLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register-wrap {
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

.register-card {
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

.pwd-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
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

.login-tip {
  margin-top: 22px;
  text-align: center;
  font-size: 15px;
}
.login-tip span {
  color: #2563eb;
  cursor: pointer;
  font-weight: 500;
}
</style>