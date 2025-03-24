<template>
  <div class="user-profile">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="user-info-card">
          <div class="user-header">
            <el-avatar :size="80" icon="el-icon-user">{{ user?.username?.charAt(0) }}</el-avatar>
            <div class="user-details">
              <h2>{{ user?.username || '用户' }}</h2>
              <p>{{ user?.email || '暂无邮箱' }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-row">
      <!-- 收藏新闻 -->
      <el-col :span="12">
        <el-card class="section-card">
          <template #header>
            <div class="card-header">
              <h3>收藏新闻</h3>
              <el-button type="text" @click="refreshCollections">刷新</el-button>
            </div>
          </template>
          <div v-if="collections.length > 0" class="card-content">
            <div 
              v-for="item in collections" 
              :key="item.id" 
              class="content-item"
              @click="viewNewsDetail(item)"
            >
              <div class="item-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="item-details">
                <div class="item-title">{{ item.news?.title || '无标题' }}</div>
                <div class="item-meta">{{ formatTime(item.collectTime) }}</div>
              </div>
            </div>
          </div>
          <div v-else-if="loadingCollections" class="loading-container">
            <el-skeleton :rows="3" animated />
          </div>
          <div v-else class="empty-data">
            <el-empty description="暂无收藏新闻"></el-empty>
          </div>
        </el-card>
      </el-col>

      <!-- 最近浏览 -->
      <el-col :span="12">
        <el-card class="section-card">
          <template #header>
            <div class="card-header">
              <h3>最近浏览</h3>
            </div>
          </template>
          <div v-if="recentlyViewed.length > 0" class="card-content">
            <div 
              v-for="item in recentlyViewed" 
              :key="item.id" 
              class="content-item"
              @click="viewNewsDetail(item)"
            >
              <div class="item-icon">
                <el-icon><View /></el-icon>
              </div>
              <div class="item-details">
                <div class="item-title">{{ item.news?.title || '无标题' }}</div>
                <div class="item-meta">{{ formatTime(item.viewTime) }}</div>
              </div>
            </div>
          </div>
          <div v-else-if="loadingRecent" class="loading-container">
            <el-skeleton :rows="3" animated />
          </div>
          <div v-else class="empty-data">
            <el-empty description="暂无最近浏览记录"></el-empty>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { computed, ref, onMounted, onActivated, watch } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import request from '@/api/auth'
import { Document, View } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

export default {
  name: 'UserProfileView',
  components: {
    Document,
    View
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    const user = computed(() => store.getters.getUser)
    
    // 收藏新闻
    const collections = ref([])
    const loadingCollections = ref(false)
    
    // 最近浏览
    const recentlyViewed = ref([])
    const loadingRecent = ref(false)

    // 格式化时间
    const formatTime = (timeStr) => {
      if (!timeStr) return '未知时间'
      
      try {
        const date = new Date(timeStr)
        if (isNaN(date.getTime())) return timeStr // 如果解析失败，返回原始字符串
        
        return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
      } catch (e) {
        console.error('时间格式化错误:', e)
        return timeStr
      }
    }

    // 获取收藏新闻
    const fetchCollections = async () => {
      // 不再检查登录状态，直接尝试获取数据
      loadingCollections.value = true
      try {
        const response = await request({
          url: '/api/user/collections',
          method: 'get'
        })
        
        if (response && response.data) {
          collections.value = response.data
          // console.log('获取收藏成功:', collections.value)
        } else {
          collections.value = []
        }
      } catch (error) {
        console.error('获取收藏新闻失败', error)
        collections.value = []
        // 不显示错误消息，因为可能是未登录导致的
      } finally {
        loadingCollections.value = false
      }
    }
    
    // 获取最近浏览
    const fetchRecentlyViewed = async () => {
      // 不再检查登录状态，直接尝试获取数据
      loadingRecent.value = true
      try {
        const response = await request({
          url: '/api/user/history',
          method: 'get'
        })
        
        if (response && response.data) {
          recentlyViewed.value = response.data
          // console.log('获取历史成功:', recentlyViewed.value)
        } else {
          recentlyViewed.value = []
        }
      } catch (error) {
        console.error('获取最近浏览失败', error)
        recentlyViewed.value = []
        // 不显示错误消息，因为可能是未登录导致的
      } finally {
        loadingRecent.value = false
      }
    }
    
    // 刷新收藏
    const refreshCollections = () => {
      fetchCollections()
      ElMessage.success('已刷新收藏新闻')
    }
    
    // 查看新闻详情
    const viewNewsDetail = (item) => {
      if (item.news && item.news.id) {
        router.push({
          name: 'newsDetail',
          params: { id: item.news.id },
          query: { 
            type: item.newsType ? mapNewsTypeToRoute(item.newsType) : 'daily'
          }
        });
      }
    }

    // 将数据库中的newsType映射回路由参数
    const mapNewsTypeToRoute = (newsType) => {
      const typeMap = {
        'daily_news': 'daily',
        'comprehensive_news': 'comprehensive',
        'fashion_news': 'fashion',
        'material_news': 'material',
        'exhibition_news': 'exhibition',
        'product_news': 'product'
      };
      return typeMap[newsType] || 'daily';
    }
    
    // 加载用户数据的方法，用于多处调用
    const loadUserData = async () => {
      // 先尝试获取用户信息，不管isLoggedIn状态如何
      try {
        await store.dispatch('getUserInfo')
        
        // 获取用户信息成功后，直接获取收藏和历史数据
        await Promise.all([fetchCollections(), fetchRecentlyViewed()])
      } catch (error) {
        console.error('获取用户信息失败', error)
        if (!store.getters.isLoggedIn) {
          ElMessage.warning('未检测到登录状态，请重新登录')
          router.push('/login')
        }
      }
    }
    
    // 监听用户状态变化
    watch(() => store.getters.isLoggedIn, (newValue) => {
      if (newValue) {
        loadUserData()
      }
    })
    
    onMounted(() => {
      loadUserData()
    })
    
    // 使用onActivated钩子，确保每次组件被激活时都重新加载数据
    // 这对于从缓存中恢复的组件特别有用
    onActivated(() => {
      loadUserData()
    })
    
    return {
      user,
      collections,
      recentlyViewed,
      loadingCollections,
      loadingRecent,
      formatTime,
      refreshCollections,
      viewNewsDetail
    }
  }
}
</script>

<style scoped>
.user-profile {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.user-info-card {
  margin-bottom: 20px;
}

.user-header {
  display: flex;
  align-items: center;
}

.user-details {
  margin-left: 20px;
}

.user-details h2 {
  margin: 0 0 5px 0;
  font-size: 24px;
}

.user-details p {
  margin: 0;
  color: #606266;
}

.content-row {
  margin-top: 20px;
}

.section-card {
  height: 100%;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.card-content {
  max-height: 400px;
  overflow-y: auto;
}

.content-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.3s;
}

.content-item:hover {
  background-color: #f5f7fa;
}

.item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background-color: #f2f6fc;
  border-radius: 8px;
  margin-right: 12px;
}

.item-details {
  flex: 1;
}

.item-title {
  font-size: 16px;
  color: #303133;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-meta {
  font-size: 12px;
  color: #909399;
}

.empty-data {
  padding: 30px 0;
}

.loading-container {
  padding: 30px 0;
}
</style> 