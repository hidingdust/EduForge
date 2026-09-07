<template>
  <div class="page-wrap">
    <Header />
    <main class="upload-container">
      <div class="upload-card glass-card">
        <div class="card-header">
          <h1>生成教育资源</h1>
          <p>上传你用来生成课堂的资料</p>
        </div>

        <el-form :model="formData" label-width="110px" class="form-main">
          <!-- 资源标题 -->
          <el-form-item label="资源标题">
            <el-input v-model="formData.title" placeholder="请输入资源标题" maxlength="60" show-word-limit />
          </el-form-item>

          <!-- 内容介绍 -->
          <el-form-item label="内容介绍">
            <el-input v-model="formData.desc" type="textarea" :rows="4" placeholder="简单介绍这份资源的用途、适用人群" maxlength="350"
              show-word-limit />
          </el-form-item>

          <!-- 选择分区（一级分类 + 二级子分类） -->
          <el-form-item label="选择分区">
            <div class="select-row">
              <el-select v-model="formData.category" placeholder="选择大类" @change="onCategoryChange">
                <el-option label="教育" value="education" />
                <el-option label="科技" value="tech" />
                <el-option label="生活" value="life" />
              </el-select>
              <el-select v-model="formData.subType" placeholder="细分学段/类型" v-if="subTypeList.length">
                <el-option v-for="item in subTypeList" :key="item" :label="item" :value="item" />
              </el-select>
            </div>
          </el-form-item>

          <!-- 封面图片上传 -->
          <el-form-item label="封面图片">
            <el-upload class="cover-upload" action="#" :auto-upload="false" :on-change="handleCoverChange"
              list-type="picture-card" :limit="1">
              <template #default>
                <i class="fa fa-plus upload-icon"></i>
                <div class="upload-text">上传封面</div>
              </template>
              <template #tip>
                <div class="tip-text">建议尺寸 400×300，jpg/png格式</div>
              </template>
            </el-upload>
          </el-form-item>

          <!-- 资源文件上传 -->
          <el-form-item label="资源文件">
            <el-upload class="file-upload" action="#" :auto-upload="false" :on-change="handleFileChange" :limit="1">
              <el-button type="primary" plain>
                <i class="fa fa-upload"></i> 选择文件
              </el-button>
              <template #tip>
                <div class="tip-text">支持pdf、doc等格式，仅可上传单个文件</div>
              </template>
            </el-upload>
            <div v-if="formData.resourceFile" class="file-name">
              📎 已选择：{{ formData.resourceFile.name }}
            </div>
          </el-form-item>

          <!-- 显示本次上传可获得积分 -->
          <el-form-item label=" ">
            <div class="score-tip">
              <span class="score-icon">⭐</span>
              <span>上传成功可获得 <strong class="score-num">+10</strong> 积分</span>
            </div>
          </el-form-item>

          <!-- 提交按钮 -->
          <el-form-item>
            <<<<<<< HEAD <el-button type="primary" size="large" @click="submitForm" class="submit-btn"
              :disabled="loading">
              提交发布
              =======
              <el-button type="primary" size="large" @click="submitForm" class="submit-btn" :loading="submitting">
                {{ submitting ? '提交中...' : '提交发布' }}
                >>>>>>> db2c19d59fdc7b7ed33be03e3c44070ffc525348
              </el-button>
              <el-button size="large" @click="resetForm" :disabled="loading">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </main>

    <!-- 手写全屏loading，完全不依赖element -->
    <div v-if="loading" class="full-loading">
      <div class="loading-box">
        <div class="spinner"></div>
        <div class="loading-text">任务创建中，请不要关闭页面...</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import Header from '../components/Header.vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()

// 提交状态
const submitting = ref(false)

const router = useRouter()

// 表单数据
const formData = ref({
  title: '',
  desc: '',
  category: '',
  subType: '',
  coverFile: null,
  resourceFile: null
})

// 加载状态
const loading = ref(false)

// 二级分类配置
const subTagConfig = {
  education: ['全部学段', '学前', '小学', '初中', '高中', '大学'],
  tech: ['全部科技', '数码科技', 'IT科技', '自动化', '人工智能']
}
const subTypeList = ref([])

// 切换大类更新二级选项
const onCategoryChange = (val) => {
  subTypeList.value = subTagConfig[val] || []
  formData.value.subType = ''
}

// 封面图片回调
const handleCoverChange = (file) => {
  formData.value.coverFile = file.raw
}

// 资源文件：单选，直接覆盖
const handleFileChange = (file) => {
  formData.value.resourceFile = file.raw
}

// ========== ✅ 提交表单（带积分加分） ==========
const submitForm = async () => {
  // 1. 表单校验
  if (!formData.value.title) {
    return ElMessage.warning('请填写资源标题')
  }
  if (!formData.value.category) {
    return ElMessage.warning('请选择分区')
  }
  if (!formData.value.coverFile) {
    return ElMessage.warning('请上传封面图片')
  }
  if (!formData.value.resourceFile) {
    return ElMessage.warning('请上传资源文件')
  }

  // 2. 构建 FormData
  const fd = new FormData()
  fd.append('title', formData.value.title)
  fd.append('description', formData.value.desc || '')
  fd.append('categoryId', formData.value.category + '/' + formData.value.subType)
  fd.append('coverImage', formData.value.coverFile)
  fd.append('resourceFile', formData.value.resourceFile)
<<<<<<< HEAD

  try {
    loading.value = true
    const response = await axios.post('/api/upload/publish', fd)
    ElMessage.success('资源上传成功')
    console.log('资源上传结果：', response.data)
    // 后端返回 {code:200, data:"真实taskId字符串"}
    const taskId = response.data.data
    router.push({
      path: '/classroom-render',
      query: { taskId }
    })
=======

  // 3. 开始提交
  submitting.value = true

  try {
    // 调用上传接口
    const response = await request.post('/api/resource/upload', fd, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    console.log('资源上传结果：', response.data)

    // 4. ✅ 上传成功，调用加分接口
    if (response.data.code === 200) {
      // 调用积分服务加分
      try {
        const scoreRes = await request.post('/user/sign/uploadScore')
        console.log('加分结果：', scoreRes.data)

        if (scoreRes.data.code === 200) {
          const newScore = scoreRes.data.data || userStore.score + 10
          userStore.setScore(newScore)
          ElMessage.success(`上传成功！积分 +10，当前积分：${newScore}`)
        } else {
          ElMessage.warning('上传成功，但积分加分失败，请联系管理员')
        }
      } catch (scoreErr) {
        console.error('加分失败：', scoreErr)
        ElMessage.warning('上传成功，但积分加分失败，请联系管理员')
      }

      // 5. 跳转到我的作品页
      setTimeout(() => {
        router.push('/profile')
      }, 1500)
    } else {
      ElMessage.error(response.data.message || '资源上传失败')
    }

>>>>>>> db2c19d59fdc7b7ed33be03e3c44070ffc525348
  } catch (error) {
    console.error('资源上传失败：', error)
    ElMessage.error(error?.response?.data?.message || '资源上传失败，请稍后重试')
  } finally {
<<<<<<< HEAD
    loading.value = false
=======
    submitting.value = false
>>>>>>> db2c19d59fdc7b7ed33be03e3c44070ffc525348
  }
}

// 重置表单
const resetForm = () => {
  formData.value = {
    title: '',
    desc: '',
    category: '',
    subType: '',
    coverFile: null,
    resourceFile: null
  }
  subTypeList.value = []
}
</script>

<style scoped>
.page-wrap {
  max-width: 1720px;
  margin: 0 auto;
  padding: 24px;
}

.upload-container {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

.glass-card {
  width: 100%;
  max-width: 860px;
  padding: 36px 44px;
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(12px);
  border-radius: 24px;
  box-shadow: 0 4px 22px rgba(160, 175, 200, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.card-header {
  text-align: center;
  margin-bottom: 36px;
}

.card-header h1 {
  font-size: 32px;
  margin: 0 0 6px;
  color: #111;
}

.card-header p {
  font-size: 17px;
  color: #666;
}

.form-main {
  font-size: 16px;
}

.select-row {
  display: flex;
  gap: 16px;
}

.select-row .el-select {
  flex: 1;
}

.cover-upload :deep(.el-upload--picture-card) {
  width: 160px;
  height: 120px;
}

.upload-icon {
  font-size: 28px;
  color: #999;
}

.upload-text {
  font-size: 13px;
  color: #888;
  margin-top: 4px;
}

.tip-text {
  font-size: 13px;
  color: #999;
  margin-top: 6px;
}

<<<<<<< HEAD=======.file-name {
  margin-top: 8px;
  color: #333;
  font-size: 14px;
}

>>>>>>>db2c19d59fdc7b7ed33be03e3c44070ffc525348 .submit-btn {
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  border: none;
  padding: 0 32px;
}

<<<<<<< HEAD .full-loading {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-box {
  text-align: center;
  color: white;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #fff;
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}

.loading-text {
  font-size: 15px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }

  =======

  /* 积分提示 */
  .score-tip {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 20px;
    background: linear-gradient(135deg, #fff8e1, #fff3e0);
    border-radius: 12px;
    border: 1px solid #ffe0b2;
  }

  .score-icon {
    font-size: 24px;
  }

  .score-num {
    color: #f57c00;
    font-size: 20px;
    >>>>>>>db2c19d59fdc7b7ed33be03e3c44070ffc525348
  }</style>

<style>
.el-input__wrapper {
  border-radius: 12px !important;
  box-shadow: 0 2px 10px rgba(180, 190, 220, 0.12) !important;
  background: rgba(255, 255, 255, 0.7) !important;
}

.el-textarea__inner {
  border-radius: 12px !important;
  box-shadow: 0 2px 10px rgba(180, 190, 220, 0.12) !important;
  background: rgba(255, 255, 255, 0.7) !important;
}

.el-select__wrapper {
  border-radius: 12px !important;
  box-shadow: 0 2px 10px rgba(180, 190, 220, 0.12) !important;
  background: rgba(255, 255, 255, 0.7) !important;
}
</style>