<template>
  <div class="edit-page">
    <div class="form-wrap glass-card">
      <h2 class="title">编辑个人资料</h2>
      <el-form :model="formData" label-width="100px">
        <el-form-item label="头像">
          <el-upload
            ref="avatarUploadRef"
            action=""
            :auto-upload="false"
            :on-change="handleAvatarChange"
            list-type="picture-card"
            accept="image/jpeg,image/jpg,image/png,image/webp"
          >
            <template #default>
              <img v-if="formData.avatarUrl" :src="formData.avatarUrl" class="avatar-preview" />
              <div v-else class="upload-placeholder">
                <el-icon><Plus /></el-icon>
              </div>
            </template>
          </el-upload>
          <div v-if="avatarUploadLoading" class="upload-tip">图片上传中...</div>
        </el-form-item>

        <el-form-item label="用户名">
          <el-input v-model="formData.userName" placeholder="请输入昵称"></el-input>
        </el-form-item>

        <el-form-item label="邮箱">
          <el-input v-model="formData.email" placeholder="请输入邮箱"></el-input>
        </el-form-item>

        <el-form-item label="性别">
          <el-radio-group v-model="formData.gender">
            <el-radio value="男">男 ♂</el-radio>
            <el-radio value="女">女 ♀</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="个人语录">
          <el-input
            v-model="formData.introduction"
            type="textarea"
            :rows="3"
            placeholder="填写你的个人语录"
          ></el-input>
        </el-form-item>

        <el-form-item label="所属分区">
          <el-select v-model="formData.subject" placeholder="选择分区">
            <el-option label="学前" value="学前"></el-option>
            <el-option label="小学" value="小学"></el-option>
            <el-option label="初中" value="初中"></el-option>
            <el-option label="高中" value="高中"></el-option>
            <el-option label="大学" value="大学"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item class="btn-wrapper">
          <div class="btn-group">
            <el-button class="gradient-btn" :loading="saveLoading" @click="saveInfo">保存修改</el-button>
            <el-button class="normal-btn" @click="goBack">返回</el-button>
            <el-button class="warning-btn" @click="openPwdDialog">修改密码</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="460px">
      <el-form :model="pwdForm" ref="pwdFormRef" label-width="90px">
        <el-form-item label="旧密码" prop="oldPwd">
          <el-input v-model="pwdForm.oldPwd" show-password placeholder="请输入原始密码"></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPwd">
          <el-input v-model="pwdForm.newPwd" show-password placeholder="请输入新密码"></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPwd">
          <el-input v-model="pwdForm.confirmPwd" show-password placeholder="再次输入新密码"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="pwdDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="pwdSubmitLoading" @click="submitChangePwd">确认修改</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUserProfile, updateUserInfo, updatePassword } from '@/api/user'
import request from '@/utils/request'

const userStore = useUserStore()
const router = useRouter()

// 上传组件ref
const avatarUploadRef = ref(null)

// 编辑资料表单，和后端DTO字段一一对应
const formData = ref({
  userName: '',
  email: '',
  gender: '女',
  introduction: '',
  subject: '学前',
  avatarUrl: ''
})
const saveLoading = ref(false)
const avatarUploadLoading = ref(false)

// 修改密码弹窗
const pwdDialogVisible = ref(false)
const pwdFormRef = ref(null)
const pwdSubmitLoading = ref(false)
const pwdForm = ref({
  oldPwd: '',
  newPwd: '',
  confirmPwd: ''
})

// 页面加载：获取用户信息回填表单
onMounted(async () => {
  const res = await getUserProfile()
  console.log('【getUserProfile 返回完整结果】', res)
  if (res.code === 200) {
    console.log('后端data数据：', res.data)
    Object.assign(formData.value, res.data)
    console.log('赋值完成后formData：', formData.value)
    // 页面加载拿到后端最新用户信息，同步更新pinia
    userStore.setUserInfo(res.data)
  } else {
    ElMessage.warning(res.msg || '获取个人资料失败')
  }
})

// 选中头像文件：上传到OSS，获取url存入formData
const handleAvatarChange = async (fileObj) => {
  const rawFile = fileObj.raw
  if (!rawFile) return
  avatarUploadLoading.value = true
  try {
    const form = new FormData()
    form.append('file', rawFile)
    const res = await request.post('/file/uploadAvatar', form)
    if (res.code === 200) {
      formData.value.avatarUrl = res.data
      ElMessage.success('头像上传成功')
    } else {
      ElMessage.error(res.msg || '头像上传失败')
    }
  } catch (err) {
    ElMessage.error('头像上传异常，请重试')
    console.error(err)
  } finally {
    // 无论成功失败，清空上传列表，解决图片堆积BUG
    avatarUploadRef.value?.clearFiles()
    avatarUploadLoading.value = false
  }
}

// 保存个人资料（携带avatarUrl一起传给后端）
const saveInfo = async () => {
  saveLoading.value = true
  // 直接提交完整表单，包含avatarUrl
  const submitDto = { ...formData.value }
  console.log('提交表单数据：', JSON.parse(JSON.stringify(submitDto)))
  try {
    const res = await updateUserInfo(submitDto)
    if (res.code === 200) {
      ElMessage.success('资料保存成功！')
      // 更新全局用户信息
      userStore.setUserInfo({ ...userStore.userInfo, ...submitDto })
      router.push({ path: '/profile', query: { refresh: 1 } })
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  }catch (err) {
  ElMessage.error('网络异常，保存失败')
} finally {
  saveLoading.value = false
}
}

// 返回个人中心
const goBack = () => {
  router.push({ path: '/profile', query: { refresh: 1 } })
}

// 打开修改密码弹窗，清空表单
const openPwdDialog = () => {
  pwdForm.value.oldPwd = ''
  pwdForm.value.newPwd = ''
  pwdForm.value.confirmPwd = ''
  pwdDialogVisible.value = true
}

// 提交修改密码
const submitChangePwd = async () => {
  // 前端基础校验
  if (!pwdForm.value.oldPwd) {
    return ElMessage.warning('请输入旧密码')
  }
  if (!pwdForm.value.newPwd) {
    return ElMessage.warning('请输入新密码')
  }
  if (!pwdForm.value.confirmPwd) {
    return ElMessage.warning('请确认新密码')
  }
  if (pwdForm.value.newPwd !== pwdForm.value.confirmPwd) {
    return ElMessage.warning('两次输入新密码不一致！')
  }

  pwdSubmitLoading.value = true
  try {
    const res = await updatePassword(pwdForm.value)
    if (res.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      pwdDialogVisible.value = false
      // 清除本地token，跳转到登录页
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      setTimeout(() => {
        router.push('/login')
      }, 1200)
    } else {
      ElMessage.error(res.msg || '密码修改失败')
    }
  } catch (err) {
    ElMessage.error('网络异常')
  } finally {
    pwdSubmitLoading.value = false
  }
}
</script>

<style scoped>
.edit-page {
  min-height: 100vh;
  padding: 40px 20px;
  background:
    radial-gradient(ellipse at bottom left, #93c5fd 0%, transparent 62%),
    radial-gradient(ellipse at bottom right, #81b4fc 0%, transparent 62%),
    #ffffff;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.form-wrap {
  width: 620px;
  padding: 36px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 6px 30px rgba(160, 180, 220, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.85);
}

.title {
  text-align: center;
  margin-bottom: 30px;
  font-size: 24px;
  font-weight: 600;
}

.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.upload-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #c0c4cc;
}
.upload-tip {
  font-size: 13px;
  color: #409eff;
  margin-top: 6px;
}

.btn-wrapper {
  margin-top: 20px !important;
}

.btn-group {
  display: flex;
  gap: 16px;
  width: 100%;
}

.btn-group :deep(.el-button) {
  flex: 1;
  height: 46px;
  border-radius: 999px !important;
  font-size: 16px;
}

.gradient-btn {
  background: linear-gradient(90deg, #3b9aff, #936aff);
  color: #fff;
  border: none;
}

.normal-btn {
  border: 1px solid #dcdfe6;
}

.warning-btn {
  background: linear-gradient(90deg, #ffa566, #ff6b6b);
  color: #fff;
  border: none;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>