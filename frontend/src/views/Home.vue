<template>
  <div class="page-wrap">
    <Header />

    <main class="main-content">
      <div class="page-title">
        <h1>精选教育资源</h1>
        <p>发现课程、作品、资料与优秀创意</p>
      </div>

      <!-- 搜索框 -->
      <div class="search-box">
        <div class="search-wrapper">
          <el-icon class="icon-left"><Search /></el-icon>
          <el-input
            v-model="searchVal"
            placeholder="搜索课程、作品、资料与创意"
            @keyup.enter="loadWorkData"
          ></el-input>
          <div class="search-btn-circle" @click="loadWorkData">
            <el-icon class="search-btn-icon"><Search /></el-icon>
          </div>
        </div>
      </div>

      <!-- 一级标签组（动态从后端加载） -->
      <div class="tag-group">
        <div
          v-for="category in rootCategoryList"
          :key="category.id"
          class="tag-item"
          :class="{ active: activeRootCategory?.id === category.id }"
          @click="handleRootCategoryClick(category)"
        >
          {{ category.name }}
        </div>
      </div>

      <!-- 二级分类标签（动态加载） -->
      <div v-if="showSubTag" class="sub-tag-group">
        <div
          v-for="subCategory in childCategoryList"
          :key="subCategory.id"
          class="tag-item"
          :class="{ active: activeChildCategory?.id === subCategory.id }"
          @click="handleChildCategoryClick(subCategory)"
        >
          {{ subCategory.name }}
        </div>
      </div>

      <div class="sort-bar">
        <h2>{{ currentTitle }}</h2>
        <el-dropdown @command="handleSort">
          <el-button size="small">
            {{ currentSortText }}
            <el-icon><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="createTime">最新发布</el-dropdown-item>
              <el-dropdown-item command="viewCount">按浏览量排序</el-dropdown-item>
              <el-dropdown-item command="likeCount">按点赞量排序</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <div class="card-grid">
        <WorkCard v-for="item in workList" :key="item.id" :info="item"/>
        <div v-if="workList.length === 0" class="empty-tip">暂无对应资源</div>
      </div>

      <!-- 分页组件 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[12,20,30]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadWorkData"
          @current-change="loadWorkData"
        />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import Header from './../components/Header.vue'
import WorkCard from './../components/WorkCard.vue'

// ========== 搜索 ==========
const searchVal = ref('')

// ========== 分类 ==========
const rootCategoryList = ref([])
const activeRootCategory = ref(null)
const childCategoryList = ref([])
const activeChildCategory = ref(null)

// ========== 排序 ==========
const sortType = ref('createTime')
const sortTextMap = {
  createTime: '最新发布',
  viewCount: '按浏览量排序',
  likeCount: '按点赞量排序'
}
const currentSortText = computed(() => sortTextMap[sortType.value])

// ========== 分页 ==========
// ========= 修改这里：默认每页12条 =========
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

// ========== 作品列表（后端返回） ==========
const workList = ref([])

// ========== 初始化 ==========
onMounted(() => {
  loadRootCategories()
  loadWorkData()
})

/**
 * 加载一级分类
 */
const loadRootCategories = async () => {
  try {
    const res = await request.post('/category/getRootList')
    console.log('一级分类响应:', res)
    let data = []
    if (res.code === 200) {
      data = Array.isArray(res.data) ? res.data :
             Array.isArray(res.data?.data) ? res.data.data : []
    } else if (Array.isArray(res)) {
      data = res
    } else if (Array.isArray(res.data)) {
      data = res.data
    }
    // 添加全部选项
    const allItem = { id: 0, name: '全部', parentId: 0 }
    rootCategoryList.value = [allItem, ...data]
    activeRootCategory.value = rootCategoryList.value[0]
  } catch (error) {
    console.error('加载一级分类失败：', error)
    ElMessage.error('加载分类失败，请检查网络连接')
    rootCategoryList.value = [
      { id: 0, name: '全部', parentId: 0 },
      { id: 1, name: '学前教育', parentId: 0 },
      { id: 2, name: '小学教育', parentId: 0 },
      { id: 3, name: '中学教育', parentId: 0 },
      { id: 4, name: '职业教育', parentId: 0 },
      { id: 5, name: '兴趣课程', parentId: 0 }
    ]
    activeRootCategory.value = rootCategoryList.value[0]
  }
}

/**
 * 点击一级分类
 */
const handleRootCategoryClick = (category) => {
  activeRootCategory.value = category
  activeChildCategory.value = null
  pageNum.value = 1
  if (category.id === 0) {
    childCategoryList.value = []
    loadWorkData()
    return
  }
  loadChildCategories(category.id).then(() => {
    loadWorkData()
  })
}

/**
 * 加载二级分类
 */
const loadChildCategories = async (parentId) => {
  try {
    const res = await request.post('/category/getChild', null, {
      params: { parentId: parentId }
    })
    console.log('二级分类响应:', res)
    let data = []
    if (res.code === 200) {
      data = Array.isArray(res.data) ? res.data :
             Array.isArray(res.data?.data) ? res.data.data : []
    } else if (Array.isArray(res)) {
      data = res
    } else if (Array.isArray(res.data)) {
      data = res.data
    }
    const allChildItem = {
      id: 0,
      name: `全部${activeRootCategory.value.name}`,
      parentId: parentId
    }
    childCategoryList.value = [allChildItem, ...data]
    activeChildCategory.value = childCategoryList.value[0]
  } catch (error) {
    console.error('加载二级分类失败：', error)
    const mockChildData = [
      { id: 0, name: `全部${activeRootCategory.value.name}`, parentId: parentId },
      { id: parentId * 10 + 1, name: '子分类一', parentId: parentId },
      { id: parentId * 10 + 2, name: '子分类二', parentId: parentId },
      { id: parentId * 10 + 3, name: '子分类三', parentId: parentId }
    ]
    childCategoryList.value = mockChildData
    activeChildCategory.value = childCategoryList.value[0]
  }
}

/**
 * 点击二级分类
 */
const handleChildCategoryClick = (subCategory) => {
  activeChildCategory.value = subCategory
  pageNum.value = 1
  loadWorkData()
}

// 是否展示二级标签
const showSubTag = computed(() => {
  return activeRootCategory.value &&
    activeRootCategory.value.id !== 0 &&
    childCategoryList.value.length > 1
})

/**
 * 下拉排序选择
 */
const handleSort = (val) => {
  sortType.value = val
  pageNum.value = 1
  loadWorkData()
}

// 页面标题
const currentTitle = computed(() => {
  const root = activeRootCategory.value
  const child = activeChildCategory.value
  if (!root || root.id === 0) {
    return '全部作品'
  }
  if (child && child.id !== 0) {
    return `${child.name}作品区`
  } else {
    return `${root.name}作品区`
  }
})

/**
 * 请求后端获取作品（搜索、分类、排序、分页全部交给后端）
 */
const loadWorkData = async () => {
  let categoryId = null
  if (activeChildCategory?.value && activeChildCategory.value.id !== 0) {
    categoryId = activeChildCategory.value.id
  } else if (activeRootCategory?.value && activeRootCategory.value.id !== 0) {
    categoryId = activeRootCategory.value.id
  }

  const dto = {
    keyWord: searchVal.value || null,
    categoryId: categoryId,
    isPublic: true,
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    sortType: sortType.value
  }
  console.log("请求dto参数", dto)
  try {
    const res = await request.post("/works/findByKeyword", dto)
    if (res.code === 200) {
      const pageData = res.data
      workList.value = pageData.content
      total.value = pageData.totalElements
    } else {
      ElMessage.warning(res.msg || "获取作品失败")
      workList.value = []
      total.value = 0
    }
  } catch (err) {
    console.error("获取作品异常", err)
    ElMessage.error("网络异常，获取作品列表失败")
    workList.value = []
    total.value = 0
  }
}
</script>

<style scoped>
.page-wrap {
  max-width: 1720px;
  margin: 0 auto;
  padding: 24px;
}

.main-content {
  margin-top: 32px;
}

.page-title {
  text-align: center;
  margin-bottom: 32px;
}

.page-title h1 {
  font-size: 48px;
  margin: 0 0 8px;
  color: #111;
}

.page-title p {
  font-size: 20px;
  color: #666;
}

.search-box {
  max-width: 900px;
  margin: 0 auto 32px;
}

.search-wrapper {
  position: relative;
  width: 100%;
}

.icon-left {
  position: absolute;
  left: 22px;
  top: 50%;
  transform: translateY(-50%);
  color: #7c8db8;
  font-size: 20px;
  z-index: 2;
}

.search-btn-circle {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 42px;
  height: 42px;
  border-radius: 999px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.35);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.search-btn-circle:hover {
  transform: translateY(-50%) scale(1.05);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.45);
}

.search-btn-icon {
  font-size: 18px;
}

.search-wrapper :deep(.el-input__wrapper) {
  border-radius: 999px;
  height: 56px;
  background-color: #ffffff !important;
  box-shadow:
    0 8px 24px rgba(59, 130, 246, 0.28),
    0 0 0 1px rgba(255, 255, 255, 0.8) inset !important;
  padding: 0 64px !important;
  transition: box-shadow 0.3s ease, transform 0.2s ease;
}

.search-wrapper :deep(.el-input__wrapper:hover) {
  transform: translateY(-1px);
  box-shadow:
    0 12px 28px rgba(59, 130, 246, 0.34),
    0 0 0 1px rgba(255, 255, 255, 0.8) inset !important;
}

.search-wrapper :deep(.el-input__wrapper.is-focus) {
  transform: translateY(-2px);
  box-shadow:
    0 14px 32px rgba(59, 130, 246, 0.4),
    0 0 0 2px rgba(59, 130, 246, 0.15) inset !important;
}

.search-wrapper :deep(.el-input__inner) {
  height: 56px;
  font-size: 16px;
  color: #1e293b;
}

.search-wrapper :deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

.tag-group {
  display: flex;
  gap: 18px;
  justify-content: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.sub-tag-group {
  display: flex;
  gap: 18px;
  justify-content: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.tag-item {
  padding: 8px 22px;
  border-radius: 999px;
  font-size: 14px;
  color: #555862;
  cursor: pointer;
  transition: all 0.22s ease;
  background: linear-gradient(90deg, rgba(255,255,255,0.4), rgba(248,250,255,0.4));
  border: 1px solid rgba(255, 255, 255, 0.7);
  white-space: nowrap;
}

.tag-item:hover {
  background: linear-gradient(90deg, rgba(255,255,255,0.7), rgba(248,250,255,0.7));
  color: #1f2937;
}

.tag-item.active {
  background: linear-gradient(90deg, #dbeafe, #e0f2fe, #f3e8ff);
  color: #2563eb;
  font-weight: 500;
  box-shadow: 0 0 12px rgba(37, 99, 235, 0.18);
}

.sort-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 8px;
}

.sort-bar h2 {
  font-size: 22px;
  margin: 0;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 20px;
}

.empty-tip {
  width: 100%;
  text-align: center;
  padding: 60px 0;
  font-size: 18px;
  color: #888;
}

.pagination-wrap{
  margin-top:30px;
  display:flex;
  justify-content:center;
}

/* ========== 响应式布局 ========== */
@media (max-width: 1400px) {
  .card-grid {
    grid-template-columns: repeat(5, 1fr);
  }
}

@media (max-width: 1200px) {
  .card-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 992px) {
  .card-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .page-wrap {
    padding: 12px;
  }

  .page-title h1 {
    font-size: 32px;
  }

  .page-title p {
    font-size: 16px;
  }

  .card-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .tag-group {
    gap: 10px;
  }

  .tag-item {
    padding: 6px 14px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .card-grid {
    grid-template-columns: 1fr;
  }

  .search-wrapper :deep(.el-input__wrapper) {
    height: 48px;
    padding: 0 52px !important;
  }

  .search-btn-circle {
    width: 36px;
    height: 36px;
  }

  .sort-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>