<template>
  <div class="news-sidebar">
    <h3 class="sidebar-title">相关推荐</h3>
    <div class="recommended-news-list">
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>
      <el-empty v-else-if="recommendedNews.length === 0" description="暂无推荐新闻" />
      <div 
        v-else
        v-for="(news, index) in recommendedNews" 
        :key="index"
        class="news-item"
        @click="goToNewsDetail(news)"
      >
        <div class="news-title text-ellipsis">{{ news.title }}</div>
        <div class="news-time">{{ formatTime(news.publishTime) }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import request from '@/api/auth'

export default {
  name: 'NewsDetailSidebar',
  props: {
    // 当前新闻ID，用于排除自身
    currentNewsId: {
      type: [Number, String],
      required: true
    },
    // 当前新闻类型
    newsType: {
      type: String,
      required: true
    }
  },
  setup(props) {
    const recommendedNews = ref([])
    const loading = ref(true)

    // 新闻类型映射
    const newsTypeMap = {
      daily: 'daily_news',
      comprehensive: 'comprehensive_news',
      fashion: 'fashion_news',
      material: 'material_news',
      exhibition: 'exhibition_news',
      product: 'product_news'
    }

    // 直接跳转到新闻详情页
    const goToNewsDetail = (news) => {
      if (news && news.id) {
        console.log('点击了相关推荐新闻:', news.id)
        // 使用window.location.href直接跳转
        window.location.href = `/news/${news.id}?type=${props.newsType}`
      }
    }

    // 格式化时间
    const formatTime = (timeStr) => {
      if (!timeStr) return '未知时间'
      
      try {
        const date = new Date(timeStr)
        if (isNaN(date.getTime())) return timeStr
        
        // 只返回日期部分: YYYY-MM-DD
        return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
      } catch (e) {
        console.error('时间格式化错误:', e)
        return timeStr
      }
    }

    // 获取推荐新闻列表
    const fetchRecommendedNews = async () => {
      loading.value = true
      try {
        const response = await request({
          url: '/api/news/list',
          method: 'get',
          params: {
            type: newsTypeMap[props.newsType] || props.newsType
          }
        })
        
        if (response && response.data) {
          // 过滤掉当前查看的新闻
          const filteredNews = Array.isArray(response.data) 
            ? response.data.filter(news => news.id !== props.currentNewsId)
            : []
          
          // 只取前5条
          recommendedNews.value = filteredNews.slice(0, 5)
          console.log('相关推荐新闻数据:', recommendedNews.value)
          console.log('当前新闻ID:', props.currentNewsId, '类型:', props.newsType)
        } else {
          recommendedNews.value = []
        }
      } catch (error) {
        console.error('获取推荐新闻列表出错', error)
        recommendedNews.value = []
      } finally {
        loading.value = false
      }
    }

    onMounted(() => {
      fetchRecommendedNews()
    })

    return {
      recommendedNews,
      loading,
      formatTime,
      goToNewsDetail
    }
  }
}
</script>

<style scoped>
.news-sidebar {
  padding: 15px;
  width: 100%;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.sidebar-title {
  margin-top: 0;
  margin-bottom: 15px;
  padding-bottom: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
}

.recommended-news-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.news-item {
  cursor: pointer;
  padding: 10px;
  border-radius: 4px;
  transition: all 0.2s;
  position: relative;
  z-index: 1;
  border: 1px solid transparent;
  display: block;
  text-decoration: none;
  color: inherit;
}

.news-item:hover {
  background-color: #f5f7fa;
  border: 1px solid #ebeef5;
  text-decoration: none;
  color: #409EFF;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.news-item:active {
  transform: translateY(0);
  box-shadow: none;
}

.news-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 5px;
}

.news-item:hover .news-title {
  color: #409EFF;
}

.news-time {
  font-size: 12px;
  color: #909399;
}

.text-ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.loading-container {
  padding: 10px 0;
}
</style> 