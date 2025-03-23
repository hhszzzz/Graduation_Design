<template>
  <div class="news-detail-page">
    <el-row :gutter="20">
      <!-- 主内容区域 -->
      <el-col :span="18">
        <el-card class="news-detail-card">
          <template #header>
            <div class="card-header">
              <h2>{{ news.title || '无标题' }}</h2>
              <div class="news-meta">
                <span class="news-time">{{ formatTime(news.publishTime) }}</span>
                <div class="actions">
                  <el-button 
                    type="primary" 
                    @click="toggleCollection"
                    size="small"
                    class="collection-btn"
                  >
                    <el-icon v-if="isCollected"><StarFilled /></el-icon>
                    <el-icon v-else><Star /></el-icon>
                    {{ isCollected ? '取消收藏' : '收藏' }}
                  </el-button>
                  <el-button @click="goBack" size="small">返回列表</el-button>
                </div>
              </div>
            </div>
          </template>
          
          <div v-if="loading" class="loading-container">
            <el-skeleton :rows="10" animated />
          </div>
          <div v-else-if="crawling" class="loading-container">
            <el-skeleton :rows="10" animated />
            <div class="crawling-text">正在获取文章内容...</div>
          </div>
          <div v-else class="news-content">
            <div v-if="news.content" v-html="formatContent(news.content)"></div>
            <div v-else-if="crawlError" class="crawl-error">
              <el-alert
                title="获取内容失败"
                type="error"
                description="无法自动获取文章内容，请点击下方按钮查看原文"
                show-icon
                :closable="false"
              />
              <div class="external-link-container">
                <el-button type="primary" @click="openExternalLink" size="large" class="mt-3">
                  查看原文
                </el-button>
                <el-button type="info" @click="retryFetchContent" size="large" class="mt-3 ml-2">
                  重试
                </el-button>
              </div>
            </div>
            <div v-else-if="news.link" class="external-link">
              <p>此新闻内容需要从原文获取</p>
              <div class="external-link-container">
                <el-button type="primary" @click="fetchNewsContent(news.link)" size="large">
                  获取内容
                </el-button>
                <el-button type="info" @click="openExternalLink" size="large" class="ml-2">
                  查看原文
                </el-button>
              </div>
            </div>
            <el-empty v-else description="暂无内容"></el-empty>
          </div>
        </el-card>
      </el-col>
      
      <!-- 侧边栏 -->
      <el-col :span="6">
        <NewsDetailSidebar 
          :current-news-id="newsId" 
          :news-type="newsType" 
        />
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/auth'
import NewsDetailSidebar from '@/components/NewsDetailSidebar.vue'
import { Star, StarFilled } from '@element-plus/icons-vue'

export default {
  name: 'NewsDetailView',
  components: {
    NewsDetailSidebar,
    Star,
    StarFilled
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const news = ref({})
    const loading = ref(true)
    const crawling = ref(false)
    const crawlError = ref(false)
    const isCollected = ref(false)
    
    // 从路由参数获取新闻ID和类型
    const newsId = computed(() => route.params.id)
    const newsType = computed(() => route.query.type || 'daily')
    
    // 新闻类型映射
    const newsTypeMap = {
      daily: 'daily_news',
      comprehensive: 'comprehensive_news',
      fashion: 'fashion_news',
      material: 'material_news',
      exhibition: 'exhibition_news',
      product: 'product_news'
    }

    // 格式化时间
    const formatTime = (timeStr) => {
      if (!timeStr) return '未知时间'
      
      try {
        const date = new Date(timeStr)
        if (isNaN(date.getTime())) return timeStr
        
        return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
      } catch (e) {
        console.error('时间格式化错误:', e)
        return timeStr
      }
    }

    // 获取新闻详情
    const fetchNewsDetail = async () => {
      if (!newsId.value) {
        ElMessage.error('未找到新闻ID')
        return
      }

      loading.value = true
      crawlError.value = false
      
      try {
        const response = await request({
          url: `/api/news/detail/${newsId.value}`,
          method: 'get',
          params: {
            type: newsTypeMap[newsType.value] || newsType.value
          }
        })
        
        if (response && response.data) {
          news.value = response.data
          
          // 如果获取到新闻但没有内容，且有链接，则自动调用爬虫获取内容
          if ((!news.value.content || news.value.content.trim() === '') && news.value.link) {
            await fetchNewsContent(news.value.link, true) // true表示自动模式
          }
        } else {
          ElMessage.warning('未找到新闻数据')
        }
      } catch (error) {
        console.error('获取新闻详情出错', error)
        ElMessage.error(error.message || '获取新闻详情失败')
      } finally {
        loading.value = false
      }
    }

    // 爬取新闻内容
    const fetchNewsContent = async (link, isAuto = false) => {
      if (!link) {
        ElMessage.warning('新闻链接不存在')
        return
      }
      
      crawling.value = true
      crawlError.value = false
      
      if (!isAuto) {
        ElMessage.info('正在爬取文章内容，请稍候...')
      }
      
      try {
        const response = await request({
          url: '/api/news/crawl_content',
          method: 'post',
          data: { url: link }
        })
        
        if (response && response.data && response.data.content) {
          // 解码Unicode编码的内容
          const decodedContent = decodeUnicodeContent(response.data.content)
          // 更新新闻内容
          news.value.content = decodedContent
          ElMessage.success('文章内容获取成功')
        } else {
          throw new Error('获取内容失败')
        }
      } catch (error) {
        console.error('爬取文章内容出错:', error)
        crawlError.value = true
        if (!isAuto) {
          ElMessage.error('获取文章内容失败，请稍后重试或直接查看原文')
        }
      } finally {
        crawling.value = false
      }
    }
    
    // 解码Unicode编码的内容
    const decodeUnicodeContent = (unicodeString) => {
      // 检查字符串是否可能是Unicode编码
      if (typeof unicodeString !== 'string') {
        return unicodeString
      }
      
      // 如果内容中包含\u开头的Unicode编码
      if (unicodeString.includes('\\u')) {
        try {
          // 使用JSON.parse解析Unicode字符串
          // 需要将字符串用双引号包裹才能正确解析
          return JSON.parse(`"${unicodeString.replace(/"/g, '\\"')}"`)
        } catch (e) {
          console.error('Unicode解码失败:', e)
          // 解码失败则返回原字符串
          return unicodeString
        }
      }
      
      return unicodeString
    }

    // 重试获取内容
    const retryFetchContent = () => {
      if (news.value.link) {
        fetchNewsContent(news.value.link)
      } else {
        ElMessage.warning('无法重试：缺少新闻链接')
      }
    }

    // 返回上一页
    const goBack = () => {
      router.go(-1)
    }

    // 打开外部链接
    const openExternalLink = () => {
      if (news.value.link) {
        window.open(news.value.link, '_blank')
      }
    }

    // 格式化内容
    const formatContent = (content) => {
      if (!content) return content
      
      try {
        // 先尝试一次JSON解析以处理可能被双重编码的内容
        let rawContent = content;
        if (content.includes('\\')) {
          try {
            // 包装成JSON格式以使用JSON.parse
            rawContent = JSON.parse(`"${content.replace(/"/g, '\\"')}"`);
          } catch (e) {
            // 解析失败则使用原始内容
            console.warn('JSON解析内容失败:', e);
          }
        }
        
        // 删除HTML标签
        let cleanContent = rawContent.replace(/<\/?[^>]+(>|$)/g, '');
        
        // 删除引号
        cleanContent = cleanContent.replace(/^["']|["']$/g, '');
        
        // 分割段落并创建HTML结构
        return cleanContent
          .split(/\\n|\n/)
          .filter(para => para && para.trim())
          .map(para => `<div class="article-paragraph">${para.trim()}</div>`)
          .join('');
          
      } catch (error) {
        console.error('格式化内容出错:', error);
        return content;
      }
    }

    // 检查新闻是否已收藏
    const checkIfCollected = async () => {
      if (!newsId.value) return
      
      try {
        const response = await request({
          url: `/api/user/is-collected/${newsId.value}`,
          method: 'get',
          params: {
            newsType: newsTypeMap[newsType.value] || 'daily_news'
          }
        })
        
        if (response && response.data) {
          isCollected.value = response.data.isCollected
        }
      } catch (error) {
        console.error('检查收藏状态失败', error)
      }
    }

    // 切换收藏状态
    const toggleCollection = async () => {
      if (!newsId.value) return
      
      try {
        if (isCollected.value) {
          // 取消收藏
          await request({
            url: `/api/user/collections/${newsId.value}`,
            method: 'delete',
            params: {
              newsType: newsTypeMap[newsType.value] || 'daily_news'
            }
          })
          ElMessage.success('已取消收藏')
          isCollected.value = false
        } else {
          // 添加收藏
          await request({
            url: `/api/user/collections/${newsId.value}`,
            method: 'post',
            params: {
              newsType: newsTypeMap[newsType.value] || 'daily_news'
            }
          })
          ElMessage.success('收藏成功')
          isCollected.value = true
        }
      } catch (error) {
        console.error('操作收藏失败', error)
        ElMessage.error('操作失败，请稍后重试')
      }
    }

    // 记录浏览历史
    const recordViewHistory = async () => {
      if (!newsId.value) return
      
      try {
        await request({
          url: `/api/user/history/${newsId.value}`,
          method: 'post',
          params: {
            newsType: newsTypeMap[newsType.value] || 'daily_news'
          }
        })
      } catch (error) {
        console.error('记录浏览历史失败', error)
      }
    }

    onMounted(async () => {
      if (newsId.value) {
        await fetchNewsDetail()
        checkIfCollected()
        recordViewHistory()
      }
    })

    return {
      news,
      loading,
      crawling,
      crawlError,
      newsId,
      newsType,
      isCollected,
      formatTime,
      goBack,
      openExternalLink,
      fetchNewsContent,
      retryFetchContent,
      decodeUnicodeContent,
      formatContent,
      toggleCollection
    }
  }
}
</script>

<style scoped>
.news-detail-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.news-detail-card {
  margin-bottom: 20px;
}

.card-header {
  position: relative;
}

.card-header h2 {
  margin: 0;
  padding: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.news-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  flex-wrap: wrap;
}

.news-time {
  color: #909399;
  font-size: 14px;
  flex: 1;
  min-width: 120px;
}

.actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-left: auto;
}

.collection-btn {
  transition: all 0.3s;
}

.news-content {
  padding: 10px 0;
  line-height: 1.8;
  font-size: 16px;
}

/* 文章内容段落样式 */
.news-content p {
  text-indent: 2em; /* 段落首行缩进2个字符 */
  margin-bottom: 16px; /* 段落间距 */
  text-align: justify; /* 两端对齐 */
  line-height: 1.8;
}

.external-link,
.crawl-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  gap: 15px;
}

.external-link-container {
  margin-top: 15px;
  display: flex;
  justify-content: center;
}

.loading-container {
  padding: 20px 0;
  position: relative;
}

.crawling-text {
  text-align: center;
  color: #909399;
  margin-top: 15px;
  font-size: 14px;
}

.mt-3 {
  margin-top: 12px;
}

.ml-2 {
  margin-left: 8px;
}

@media (max-width: 768px) {
  .el-col-18 {
    width: 100%;
  }
  .el-col-6 {
    width: 100%;
    margin-top: 20px;
  }
}
</style>

<style>
/* 全局样式，确保应用到v-html内容 */
.news-content .article-paragraph {
  text-indent: 2em !important; /* 段落首行缩进2个字符 */
  margin-bottom: 16px !important; /* 段落间距 */
  text-align: justify !important; /* 两端对齐 */
  line-height: 1.8 !important;
  display: block !important; /* 确保是块级元素 */
  font-size: 16px !important;
}

/* 确保最后一个段落也有适当的边距 */
.news-content .article-paragraph:last-child {
  margin-bottom: 0 !important;
}

/* 保留旧样式兼容性 */
.news-content p {
  text-indent: 2em !important;
  margin-bottom: 16px !important;
  text-align: justify !important;
  line-height: 1.8 !important;
  display: block !important;
}
</style> 