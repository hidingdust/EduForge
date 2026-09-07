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
                <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.name" />
              </el-select>
              <el-select v-model="formData.subType" placeholder="细分学段/类型" v-if="subCategoryOptions.length">
                <el-option v-for="item in subCategoryOptions" :key="item.id" :label="item.name" :value="item.name" />
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
            <el-button type="primary" size="large" @click="submitForm" class="submit-btn" :disabled="loading"
              :loading="submitting">
              {{ submitting ? '提交中...' : '提交发布' }}
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import Header from '../components/Header.vue'
import { useUserStore } from '../stores/user'
import request from '../utils/request'

const router = useRouter()
const userStore = useUserStore()

const submitting = ref(false)

// 表单数据
const formData = ref({
  title: '',
  desc: '',
  category: '',
  subType: '',
  coverFile: null,
  resourceFile: null
})
const loading = ref(false)

const categoryOptions = ref([])
const selectedCategory = computed(() =>
  categoryOptions.value.find((c) => c.name === formData.value.category)
)
const subCategoryOptions = computed(() => selectedCategory.value?.children || [])

onMounted(async () => {
  try {
    const res = await request.get('/category/cascade')
    const list = res.data || []
    categoryOptions.value = Array.isArray(list) ? list : []
  } catch (err) {
    console.error('加载分类失败：', err)
    ElMessage.warning('分类加载失败，请刷新页面重试')
  }
})

const onCategoryChange = () => {
  formData.value.subType = ''
}

const handleCoverChange = (file) => {
  formData.value.coverFile = file.raw
}

const handleFileChange = (file) => {
  formData.value.resourceFile = file.raw
}

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

  submitting.value = true
  loading.value = true

  const fd = new FormData()
  fd.append('title', formData.value.title)
  fd.append('description', formData.value.desc || '')
  const catPath = formData.value.subType
    ? formData.value.category + '/' + formData.value.subType
    : formData.value.category
  fd.append('categoryId', catPath)
  fd.append('coverImage', formData.value.coverFile)
  fd.append('resourceFile', formData.value.resourceFile)

  try {
    const res = await request.post('/upload/publish', fd)

    if (res.code !== 200) {
      ElMessage.error(res.message || '资源上传失败')
      return
    }

    ElMessage.success('资源文件上传成功')
    console.log('资源上传结果：', res)
    const taskId = res.data

    // ==========调用后端上传加分接口 /user/sign/uploadScore ==========
    try {
      // post请求，无参数！后端从request拿登录用户
      const scoreRes = await request.post('/user/sign/uploadScore')
      console.log('加分接口返回：', scoreRes)
      if (scoreRes.code === 200) {
        // 拿后端返回的数据，不再写死10
        const gainScore = scoreRes.data.gainScore
        const totalScore = scoreRes.data.totalScore
        const msg = scoreRes.data.message
        // 更新pinia里面用户积分
        userStore.setScore(totalScore)
        // 弹窗展示加分信息
        ElMessage.success(`${msg}，当前总积分：${totalScore}`)
      } else {
        ElMessage.warning(`上传成功，但积分未发放：${scoreRes.message}`)
      }
    } catch (scoreErr) {
      console.error('调用加分接口异常：', scoreErr)
      ElMessage.warning('资源上传成功，但获取积分接口异常，请稍后查看积分')
    }

    // 跳转到课堂渲染页面
    router.push({
      path: '/classroom-render',
      query: { taskId }
    })

  } catch (error) {
    console.error('资源上传失败：', error)
    ElMessage.error(error?.response?.data?.message || '资源上传失败，请稍后重试')
  } finally {
    loading.value = false
    submitting.value = false
  }
}

const resetForm = () => {
  formData.value = {
    title: '',
    desc: '',
    category: '',
    subType: '',
    coverFile: null,
    resourceFile: null
  }
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

.file-name {
  margin-top: 8px;
  color: #333;
}

.submit-btn {
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  border: none;
  padding: 0 32px;
}

.full-loading {
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
}

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
}
</style>

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