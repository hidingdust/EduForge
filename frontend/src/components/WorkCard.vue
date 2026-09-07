<template>
  <div class="work-card" @click="$router.push({ path: '/workDetail', query: { taskId: info.taskId } })">
    <div class="card-img-box">
      <img :src="info.docUrl" alt="封面" @error="handleImgError" />
    </div>
    <div class="card-title">{{ info.title }}</div>

    <div v-if="isUpload" class="upload-ext-info">
      <p class="time-text">上传时间：{{ info.uploadTime }}</p>
      <p class="score-text">获得积分：✨{{ info.gainScore }}✨</p>
    </div>

    <div class="meta-row">
      <span
        class="like-btn"
        :class="{ liked: info.liked }"
        @click.stop="handleLike(info)"
        :style="{ cursor: info.loading ? 'not-allowed' : 'pointer' }"
      >
        {{ info.liked ? '❤️' : '🤍' }} {{ info.likeCount }}
      </span>
      <span>💬 {{ info.commentCount }}</span>
      <span>⬇️ {{ info.downloadCount || 0 }}</span>
    </div>

    <el-button
      v-if="showDelete"
      class="capsule-del-btn"
      type="danger"
      size="small"
      @click.stop="$emit('delete')"
    >
      删除
    </el-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

defineProps({
  info: {
    type: Object,
    required: true
  },
  isUpload: {
    type: Boolean,
    default: false
  },
  showDelete: {
    type: Boolean,
    default: false
  }
})
// 重点：接收emit对象
const emit = defineEmits(['delete', 'unlike'])

const router = useRouter()

// 图片加载失败兜底
function handleImgError(e) {
  e.target.src = '/default-cover.png'
}

// 点赞/取消点赞
const handleLike = async (item) => {
  if (item.loading) return
  item.loading = true

  try {
    if (!item.liked) {
      // 点赞
      await request.post('/works/like', { taskId: item.taskId })
      item.likeCount += 1
      item.liked = true
      ElMessage.success('点赞成功')
    } else {
      // 取消点赞
      await request.post('/works/cancelLike', { taskId: item.taskId })
      item.likeCount -= 1
      item.liked = false
      ElMessage.success('取消点赞')
      emit('unlike', item.id)
    }
  } catch (err) {
    console.error('点赞操作失败：', err)
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    item.loading = false
  }
}
</script>

<style scoped>
.work-card {
  cursor: pointer;
  background: #fff;
  border-radius: 18px;
  padding: 16px;
  height: 340px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 2px 12px rgba(145, 165, 210, 0.08);
}

.work-card:hover {
  box-shadow: 0 6px 18px rgba(145, 165, 210, 0.15);
}

.card-img-box {
  width: 100%;
  height: 160px;
  border-radius: 12px;
  overflow: hidden;
}
.card-img-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-title {
  font-size: 15px;
  color: #222;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
}

.upload-ext-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.time-text {
  font-size: 12px;
  color: #666;
  margin: 0;
}
.score-text {
  font-size: 13px;
  color: #ffb800;
  font-weight: 600;
  margin: 0;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #777;
  margin-top: auto;
}

.like-btn {
  padding: 2px 4px;
  transition: color 0.2s;
}
.like-btn:hover:not(.liked) {
  color: #f56c6c;
}
.like-btn.liked {
  color: #f56c6c;
}

.capsule-del-btn {
  width: 100%;
  border-radius: 999px !important;
  background-color: #f56c6c;
  border: none;
  font-size: 13px;
  margin-top: 8px;
}
.capsule-del-btn:hover {
  background-color: #e65555 !important;
}
</style>