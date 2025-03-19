<template>
  <div class="home">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <h2>欢迎回来</h2>
        </div>
      </template>
      <div class="user-info" v-if="user">
        <h3>{{ user.username }}</h3>
        <p>邮箱：{{ user.email }}</p>
      </div>
    </el-card>

    <el-card class="news-card">
      <template #header>
        <div class="card-header">
          <h2>新闻列表</h2>
          <el-button type="primary" size="small" @click="fetchNewsList" :loading="loading">
            刷新新闻
          </el-button>
        </div>
      </template>
      <div v-if="newsList && newsList.length > 0">
        <el-timeline>
          <el-timeline-item
            v-for="(news, index) in newsList"
            :key="news.id || index"
            placement="top"
          >
            <el-card>
              <h4>{{ news.title || '无标题' }}</h4>
              <el-link v-if="news.link" type="primary" :href="news.link" target="_blank">查看详情</el-link>
              <span v-else class="no-link">暂无链接</span>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
      <div v-else-if="!loading" class="empty-data">
        <el-empty description="暂无新闻数据"></el-empty>
      </div>
      <div v-else class="loading-container">
        <el-skeleton :rows="3" animated />
      </div>
    </el-card>
  </div>
</template>

<script>
import { computed, ref, watch, onMounted } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import request from '@/api/auth' // 导入配置了token的request服务
import { useRouter } from 'vue-router'

export default {
  name: 'HomeView',
  setup() {
    const store = useStore()
    const router = useRouter()
    const user = computed(() => store.getters.getUser)
    
    // 新闻列表数据
    const newsList = ref([])
    const loading = ref(false)

    // 请求新闻列表
    const fetchNewsList = async () => {
      if (!user.value) {
        return // 如果用户信息不存在，不请求数据
      }

      loading.value = true
      try {
        const response = await request({
          url: '/api/news/list',
          method: 'get'
        })
        
        // 处理返回数据
        if (response && response.data) {
          // 如果数据是数组，直接使用
          newsList.value = Array.isArray(response.data) ? response.data : [];
          ElMessage.success('新闻列表获取成功');
        } else {
          newsList.value = [];
          ElMessage.warning('暂无新闻数据');
        }
      } catch (error) {
        console.error('获取新闻列表出错', error);
        newsList.value = []; // 清空列表
        
        if (error.response && error.response.status === 401) {
          ElMessage.error('登录已过期，请重新登录');
          store.dispatch('logout');
          router.push('/login');
        } else {
          ElMessage.error(error.message || '获取新闻列表失败，请稍后重试');
        }
      } finally {
        loading.value = false;
      }
    }

    // 监听用户信息变化，当用户信息加载完成后获取新闻列表
    watch(user, (newUser) => {
      if (newUser) {
        fetchNewsList()
      }
    }, { immediate: true })

    // 在组件挂载时，如果已经有用户信息，则获取新闻列表
    onMounted(() => {
      if (user.value) {
        fetchNewsList()
      }
    })

    return {
      user,
      newsList,
      loading,
      fetchNewsList
    }
  }
}
</script>

<style scoped>
.home {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.welcome-card, .news-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-weight: 500;
  color: #303133;
}

.user-info {
  padding: 20px 0;
}

.user-info h3 {
  margin: 0 0 10px 0;
  color: #303133;
}

.user-info p {
  margin: 5px 0;
  color: #606266;
}

.empty-data {
  padding: 30px 0;
}

.loading-container {
  padding: 30px 0;
}

.no-link {
  color: #909399;
  font-size: 14px;
  font-style: italic;
}

h4 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #303133;
}
</style>
