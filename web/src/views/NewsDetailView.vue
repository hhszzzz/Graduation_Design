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
                  <el-button @click="goBack" size="small">
                    <el-icon><ArrowLeft /></el-icon>
                    返回
                  </el-button>
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
        
        <!-- 评论区域 -->
        <el-card class="comment-section">
          <template #header>
            <div class="comment-header">
              <span>评论 ({{ commentTotal }})</span>
            </div>
          </template>
          
          <!-- 评论输入框 -->
          <div class="comment-form">
            <el-input
              v-model="commentContent"
              type="textarea"
              :rows="3"
              placeholder="写下您的评论..."
              maxlength="500"
              show-word-limit
            />
            <div class="comment-form-footer">
              <el-button 
                type="primary" 
                @click="submitComment" 
                :disabled="!commentContent.trim() || !isLoggedIn">
                {{ isLoggedIn ? '发表评论' : '请先登录' }}
              </el-button>
            </div>
          </div>
          
          <!-- 评论列表 -->
          <div v-if="comments.length > 0" class="comment-list">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <div class="comment-avatar">
                <el-avatar :size="40" :src="comment.userAvatar || defaultAvatar"></el-avatar>
              </div>
              <div class="comment-content">
                <div class="comment-info">
                  <span class="comment-author">{{ comment.username || '匿名用户' }}</span>
                  <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
                </div>
                <div class="comment-text">{{ comment.content }}</div>
                <div class="comment-actions">
                  <span @click="handleReply(comment)" class="action-item">回复</span>
                  <span @click="handleLike(comment)" class="action-item">
                    <i class="el-icon-star-off"></i> {{ comment.likeCount || 0 }}
                  </span>
                </div>
                
                <!-- 回复输入框 -->
                <div v-if="replyingTo === comment.id" class="reply-form">
                  <el-input
                    v-model="replyContent"
                    type="textarea"
                    :rows="2"
                    placeholder="回复..."
                    maxlength="300"
                    show-word-limit
                  />
                  <div class="reply-form-footer">
                    <el-button size="small" @click="cancelReply">取消</el-button>
                    <el-button 
                      size="small" 
                      type="primary" 
                      @click="submitReply(comment)" 
                      :disabled="!replyContent.trim()">
                      回复
                    </el-button>
                  </div>
                </div>
                
                <!-- 子评论列表 -->
                <div v-if="comment.children && comment.children.length > 0" class="reply-list">
                  <div v-for="reply in comment.children" :key="reply.id" class="reply-item">
                    <div class="reply-avatar">
                      <el-avatar :size="30" :src="reply.userAvatar || defaultAvatar"></el-avatar>
                    </div>
                    <div class="reply-content">
                      <div class="reply-info">
                        <span class="reply-author">{{ reply.username || '匿名用户' }}</span>
                        <template v-if="reply.replyToUsername">
                          <span class="reply-to">回复</span>
                          <span class="reply-to-author">{{ reply.replyToUsername }}</span>
                        </template>
                        <span class="reply-time">{{ formatDate(reply.createTime) }}</span>
                      </div>
                      <div class="reply-text">{{ reply.content }}</div>
                      <div class="reply-actions">
                        <span @click="handleReplyToReply(comment, reply)" class="action-item">回复</span>
                        <span @click="handleLike(reply)" class="action-item">
                          <i class="el-icon-star-off"></i> {{ reply.likeCount || 0 }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-else-if="comment.replyCount && comment.replyCount > 0" class="load-replies">
                  <el-button type="text" @click="loadReplies(comment)">
                    加载{{ comment.replyCount }}条回复
                  </el-button>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="no-comments">
            暂无评论，快来抢沙发吧~
          </div>
          
          <!-- 加载更多 -->
          <div v-if="hasMore" class="load-more">
            <el-button type="text" @click="loadMore">加载更多</el-button>
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
import { useStore } from 'vuex'
import NewsDetailSidebar from '@/components/NewsDetailSidebar.vue'
import { Star, StarFilled, ArrowLeft } from '@element-plus/icons-vue'
import { formatDate } from '@/utils/date'
import { getNewsDetail, getNewsContent } from '@/api/news'
import { getComments, getReplies, addComment, likeComment } from '@/api/comment'
import { mapGetters } from 'vuex'

export default {
  name: 'NewsDetailView',
  components: {
    NewsDetailSidebar,
    Star,
    StarFilled,
    ArrowLeft
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const store = useStore()
    const news = ref({})
    const loading = ref(true)
    const crawling = ref(false)
    const crawlError = ref(false)
    const isCollected = ref(false)
    
    // 从路由参数获取新闻ID和类型
    const newsId = computed(() => route.params.id)
    const newsType = computed(() => route.query.type || 'daily')
    
    // 评论相关
    const comments = ref([])
    const commentTotal = ref(0)
    const commentContent = ref('')
    const replyContent = ref('')
    const replyingTo = ref(null)
    const replyToId = ref(null)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const hasMore = ref(false)
    const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
    const isLoadingComments = ref(false)
    
    // 用户登录状态
    const isLoggedIn = computed(() => {
      // console.log('登录状态检查:', store.getters.isLoggedIn, store.getters.isAuthenticated)
      // 使用store中的getter来判断登录状态
      return store.getters.isLoggedIn || store.getters.isAuthenticated
    })
    
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
        // console.log('获取新闻详情，ID:', newsId.value, '类型:', newsTypeMap[newsType.value] || newsType.value)
        const response = await getNewsDetail(newsId.value, newsTypeMap[newsType.value] || newsType.value)
        // console.log('新闻详情响应:', response)
        
        // 处理不同格式的响应
        if (response && response.data) {
          // 标准格式响应
          news.value = response.data
        } else if (response && typeof response === 'object' && !response.data) {
          // 非标准格式：直接返回的对象
          // console.log('收到非标准格式响应:', response)
          news.value = response
        } else {
          console.warn('未找到新闻数据或数据结构不正确:', response)
          ElMessage.warning('未找到新闻数据')
          return
        }
        
        // 如果获取到新闻但没有内容，且有链接，则自动调用爬虫获取内容
        if ((!news.value.content || news.value.content.trim() === '') && news.value.link) {
          await fetchNewsContent(news.value.link, true) // true表示自动模式
        }
      } catch (error) {
        console.error('获取新闻详情出错', error)
        
        // 特殊处理：如果error中包含response和data，可能是后端返回的数据
        if (error && error.response && error.response.data) {
          // 尝试提取有用信息
          const responseData = error.response.data
          if (responseData && (responseData.data || responseData.id)) {
            // console.log('从错误中提取到可用的新闻数据:', responseData)
            news.value = responseData.data || responseData
            
            // 如果获取到新闻但没有内容，且有链接，则自动调用爬虫获取内容
            if ((!news.value.content || news.value.content.trim() === '') && news.value.link) {
              await fetchNewsContent(news.value.link, true) // true表示自动模式
            }
            return // 成功提取到数据，提前返回
          }
        }
        
        // 常规错误处理
        let errorMessage = '获取新闻详情失败';
        
        if (error.message) {
          errorMessage = error.message;
        }
        
        if (error.response) {
          const status = error.response.status;
          if (status !== 401) {
            ElMessage.error(`${errorMessage} (${status})`);
          }
        } else if (error.code && error.code !== 401) {
          ElMessage.error(`${errorMessage} (错误码: ${error.code})`);
        } else if (!error.code && !error.response) {
          ElMessage.error(errorMessage);
        }
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
        ElMessage.info('正在获取文章内容，请稍候...')
      }
      
      // console.log('开始获取文章内容，链接:', link)
      
      try {
        // 使用API模块中的方法获取内容
        const response = await getNewsContent(link)
        // console.log('内容响应类型:', typeof response)
        
        // 从响应中提取内容
        let extractedContent = null
        
        // 1. 如果响应直接是字符串（HTML或文本）
        if (typeof response === 'string') {
          extractedContent = response
          // console.log('直接获取到HTML/文本内容，长度:', response.length)
        }
        // 2. 从标准响应对象中提取
        else if (response && response.data && response.data.content) {
          extractedContent = response.data.content
          // console.log('从标准响应提取内容')
        }
        // 3. 从直接包含content字段的对象提取
        else if (response && response.content) {
          extractedContent = response.content
          // console.log('从对象直接提取content字段')
        }
        // 4. 递归搜索复杂对象
        else if (response && typeof response === 'object') {
          // 深度搜索content字段
          const findContent = (obj, path = '') => {
            if (!obj || typeof obj !== 'object') return null
            
            // 直接检查content字段
            if (obj.content && typeof obj.content === 'string') {
              // console.log(`在路径 ${path}.content 找到内容`)
              return obj.content
            }
            
            // 递归搜索
            for (const key in obj) {
              if (Object.prototype.hasOwnProperty.call(obj, key) && typeof obj[key] === 'object') {
                const currentPath = path ? `${path}.${key}` : key
                const found = findContent(obj[key], currentPath)
                if (found) return found
              }
            }
            
            return null
          }
          
          extractedContent = findContent(response)
          // console.log('递归搜索内容:', extractedContent ? '成功' : '失败')
          
          // 如果找不到content字段，尝试找最长的字符串
          if (!extractedContent) {
            let maxLength = 100 // 阈值：字符串长度至少为100才考虑
            
            const findLongestString = (obj, path = '') => {
              if (!obj || typeof obj !== 'object') return
              
              for (const key in obj) {
                if (Object.prototype.hasOwnProperty.call(obj, key)) {
                  const value = obj[key]
                  const currentPath = path ? `${path}.${key}` : key
                  
                  if (typeof value === 'string' && value.length > maxLength) {
                    maxLength = value.length
                    extractedContent = value
                    // console.log(`在路径 ${currentPath} 找到更长字符串，长度: ${value.length}`)
                  } else if (typeof value === 'object' && value !== null) {
                    findLongestString(value, currentPath)
                  }
                }
              }
            }
            
            findLongestString(response)
          }
        }
        
        // 如果找到了内容
        if (extractedContent) {
          // 处理可能的Unicode编码
          const finalContent = decodeUnicodeContent(extractedContent)
          // console.log('最终内容长度:', finalContent?.length || 0)
          
          // 更新新闻内容
          news.value.content = finalContent
          ElMessage.success('文章内容获取成功')
        } else {
          console.error('内容获取失败，无法找到有效内容:', response)
          throw new Error('获取内容失败，无法解析返回数据')
        }
      } catch (error) {
        console.error('爬取文章内容出错:', error)
        crawlError.value = true
        
        // 尝试从错误中提取内容
        let contentFromError = null
        
        if (error.response && typeof error.response.data === 'string' && 
            error.response.data.length > 200) {
          contentFromError = error.response.data
          // console.log('从错误响应中提取内容，长度:', contentFromError.length)
        } else if (error.data && typeof error.data === 'string') {
          contentFromError = error.data
        } else if (error.message && error.message.length > 200) {
          contentFromError = error.message
        }
        
        if (contentFromError) {
          // 找到了可用内容，更新并不显示错误
          news.value.content = contentFromError
          crawlError.value = false
          ElMessage.success('已成功获取文章内容')
        } else if (!isAuto) {
          // 仅在非自动模式下显示错误
          let errorMsg = '获取文章内容失败'
          ElMessage.error(errorMsg + '，请稍后重试或直接查看原文')
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
        // 1. 先处理可能的Unicode编码
        let processedContent = content;
        if (typeof content === 'string' && content.includes('\\')) {
          try {
            // 包装成JSON格式以使用JSON.parse解码Unicode
            processedContent = JSON.parse(`"${content.replace(/"/g, '\\"')}"`);
          } catch (e) {
            console.warn('JSON解析内容失败:', e);
            processedContent = content;
          }
        }
        
        // 2. 检查是否是HTML内容
        const isHtml = typeof processedContent === 'string' && 
                       (processedContent.includes('<html') || 
                        processedContent.includes('<body') || 
                        processedContent.includes('<div') || 
                        processedContent.includes('<p'));
        
        if (isHtml) {
          // console.log('检测到HTML内容，提取纯文本');
          
          // 提取并清理HTML内容
          try {
            // 移除脚本、样式等非内容元素
            let cleaned = processedContent
              .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '') // 移除script标签
              .replace(/<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>/gi, '')    // 移除style标签
              .replace(/<header\b[^<]*(?:(?!<\/header>)<[^<]*)*<\/header>/gi, '') // 移除header标签
              .replace(/<footer\b[^<]*(?:(?!<\/footer>)<[^<]*)*<\/footer>/gi, '') // 移除footer标签
              .replace(/<nav\b[^<]*(?:(?!<\/nav>)<[^<]*)*<\/nav>/gi, '')         // 移除nav标签
              .replace(/<aside\b[^<]*(?:(?!<\/aside>)<[^<]*)*<\/aside>/gi, '')    // 移除aside标签
              .replace(/<form\b[^<]*(?:(?!<\/form>)<[^<]*)*<\/form>/gi, '')      // 移除form标签
              .replace(/<div class="comments[\s\S]*?<\/div>/gi, '')              // 移除评论区
              .replace(/<([^>]+)>/g, ' ')                                        // 移除其他所有标签但保留内容
              .replace(/\s+/g, ' ')                                              // 压缩多余空格
              .trim();
            
            // 提取文本内容同时保留段落结构
            const paragraphs = [];
            
            // 提取<p>标签内容
            const pMatches = cleaned.match(/<p\b[^>]*>(.*?)<\/p>/gi);
            if (pMatches && pMatches.length > 0) {
              pMatches.forEach(p => {
                let text = p.replace(/<p\b[^>]*>(.*?)<\/p>/i, '$1')
                           .replace(/<[^>]+>/g, '') // 移除嵌套标签
                           .trim();
                if (text) paragraphs.push(text);
              });
            }
            
            // 如果没有段落，尝试提取<div>标签内容
            if (paragraphs.length === 0) {
              const divMatches = cleaned.match(/<div\b[^>]*>(.*?)<\/div>/gi);
              if (divMatches && divMatches.length > 0) {
                divMatches.forEach(div => {
                  let text = div.replace(/<div\b[^>]*>(.*?)<\/div>/i, '$1')
                             .replace(/<[^>]+>/g, '') // 移除嵌套标签
                             .trim();
                  if (text) paragraphs.push(text);
                });
              }
            }
            
            // 如果仍然没有内容，移除所有HTML标签
            if (paragraphs.length === 0) {
              let plainText = cleaned.replace(/<[^>]+>/g, ' ')
                                  .replace(/\s+/g, ' ')
                                  .trim();
              
              // 按句号、问号等分割成段落
              let sentences = plainText.split(/\.|\?|!|。|？|！/);
              sentences.forEach(sentence => {
                let trimmed = sentence.trim();
                if (trimmed) paragraphs.push(trimmed);
              });
            }
            
            // 如果提取到段落，创建格式化的HTML
            if (paragraphs.length > 0) {
              return paragraphs
                .map(para => `<div class="article-paragraph">${para}</div>`)
                .join('');
            }
          } catch (e) {
            console.error('处理HTML内容失败:', e);
          }
        }
        
        // 3. 处理普通文本
        // 删除可能存在的HTML标签
        let cleanContent = processedContent.replace(/<\/?[^>]+(>|$)/g, '');
        
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
        // 如果所有处理都失败，至少显示原始内容
        return `<div class="article-paragraph">${String(content).substr(0, 5000)}</div>`;
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
    
    // 获取评论列表
    const fetchComments = async (page = 1) => {
      if (isLoadingComments.value) return
      
      isLoadingComments.value = true
      currentPage.value = page
      
      try {
        // console.log('获取评论列表:', {
        //   newsId: newsId.value,
        //   newsType: newsTypeMap[newsType.value] || 'daily_news',
        //   page: currentPage.value,
        //   size: pageSize.value
        // })
        
        const { data } = await getComments({
          newsId: newsId.value,
          newsType: newsTypeMap[newsType.value] || 'daily_news',
          page: currentPage.value,
          size: pageSize.value
        })
        
        // console.log('评论列表响应:', data)
        
        if (page === 1) {
          comments.value = data.comments || []
        } else {
          comments.value = [...comments.value, ...(data.comments || [])]
        }
        
        commentTotal.value = data.total || 0
        hasMore.value = comments.value.length < commentTotal.value
      } catch (error) {
        console.error('获取评论失败:', error)
        // 如果不是401错误才显示错误消息
        if (!(error.response && error.response.status === 401) && 
            !(error.code === 401)) {
          ElMessage.error('获取评论失败')
        }
      } finally {
        isLoadingComments.value = false
      }
    }
    
    // 加载更多评论
    const loadMore = () => {
      fetchComments(currentPage.value + 1)
    }
    
    // 提交评论
    const submitComment = async () => {
      if (!commentContent.value.trim()) return
      if (!isLoggedIn.value) {
        ElMessage.warning('请先登录')
        return
      }
      
      try {
        // console.log('提交评论:', {
        //   newsId: newsId.value,
        //   newsType: newsTypeMap[newsType.value] || 'daily_news',
        //   userId: store.getters.getUser?.id,
        //   content: commentContent.value
        // })
        
        await addComment({
          newsId: newsId.value,
          newsType: newsTypeMap[newsType.value] || 'daily_news',
          userId: store.getters.getUser?.id,
          content: commentContent.value
        })
        
        // console.log('评论成功，响应:', response)
        ElMessage.success('评论成功')
        commentContent.value = ''
        fetchComments(1) // 重新加载第一页评论
      } catch (error) {
        console.error('评论失败:', error)
        ElMessage.error('评论失败，请稍后重试')
      }
    }
    
    // 处理回复
    const handleReply = (comment) => {
      if (!isLoggedIn.value) {
        ElMessage.warning('请先登录')
        return
      }
      
      // 设置当前正在回复的评论
      replyingTo.value = comment.id
      replyToId.value = comment.id
      replyContent.value = ''
      
      // console.log('开始回复评论:', comment.id, '用户:', comment.username)
    }
    
    // 处理回复中的回复
    const handleReplyToReply = (parentComment, reply) => {
      if (!isLoggedIn.value) {
        ElMessage.warning('请先登录')
        return
      }
      
      replyingTo.value = parentComment.id
      replyToId.value = reply.id
      replyContent.value = ''
    }
    
    // 取消回复
    const cancelReply = () => {
      replyingTo.value = null
      replyToId.value = null
      replyContent.value = ''
    }
    
    // 提交回复
    const submitReply = async (parentComment) => {
      if (!replyContent.value.trim()) return
      if (!isLoggedIn.value) {
        ElMessage.warning('请先登录')
        return
      }
      
      try {
        // 准备回复数据
        const replyData = {
          newsId: newsId.value,
          newsType: newsTypeMap[newsType.value] || 'daily_news',
          userId: store.getters.getUser?.id,
          content: replyContent.value,
          parentId: parentComment.id,
          replyToId: replyToId.value
        }
        
        // console.log('提交回复:', replyData)
        
        // 发送回复请求
        await addComment(replyData)
        // console.log('回复成功，响应:', response)
        
        // 重置表单状态
        ElMessage.success('回复成功')
        replyContent.value = ''
        replyingTo.value = null
        replyToId.value = null
        
        // 重新获取该评论的回复列表
        // console.log('获取评论回复列表, 父评论ID:', parentComment.id)
        const repliesResponse = await getReplies(parentComment.id)
        // console.log('获取回复列表响应:', repliesResponse)
        
        // 更新评论的回复列表
        const index = comments.value.findIndex(c => c.id === parentComment.id)
        if (index !== -1) {
          // console.log('找到父评论索引:', index)
          
          // 提取回复数据，支持多种响应结构
          let replies = null
          
          if (repliesResponse && repliesResponse.data && Array.isArray(repliesResponse.data)) {
            // 标准格式: { data: [...] }
            replies = repliesResponse.data
            // console.log('从标准响应格式提取回复数组')
          } else if (repliesResponse && Array.isArray(repliesResponse)) {
            // 直接数组格式
            replies = repliesResponse
            // console.log('从直接数组格式提取回复')
          } else if (repliesResponse && typeof repliesResponse === 'object') {
            // 尝试其他字段
            for (const key in repliesResponse) {
              if (Array.isArray(repliesResponse[key])) {
                replies = repliesResponse[key]
                // console.log(`从响应的${key}字段提取回复数组`)
                break
              }
            }
          }
          
          if (replies) {
            // console.log('成功提取回复列表, 数量:', replies.length)
            comments.value[index].children = replies
          } else {
            console.error('无法从响应中提取回复列表:', repliesResponse)
            // 如果无法提取，重新加载整个评论列表
            fetchComments(1)
          }
        } else {
          console.warn('未找到对应的父评论，ID:', parentComment.id)
          // 父评论未找到，重新加载整个评论列表
          fetchComments(1)
        }
      } catch (error) {
        console.error('回复失败:', error)
        ElMessage.error('回复失败，请稍后重试')
      }
    }
    
    // 处理点赞
    const handleLike = async (comment) => {
      if (!isLoggedIn.value) {
        ElMessage.warning('请先登录')
        return
      }
      
      try {
        await likeComment(comment.id)
        
        // 更新点赞数
        comment.likeCount = (comment.likeCount || 0) + 1
        ElMessage.success('点赞成功')
      } catch (error) {
        console.error('点赞失败:', error)
        ElMessage.error('点赞失败')
      }
    }

    // 手动加载单个评论的回复
    const loadReplies = async (comment) => {
      if (!comment || !comment.id) return
      
      try {
        // console.log('手动加载评论回复, 评论ID:', comment.id)
        const response = await getReplies(comment.id)
        // console.log('回复列表响应:', response)
        
        // 提取回复数据
        let replies = null
        
        if (response && response.data && Array.isArray(response.data)) {
          replies = response.data
        } else if (response && Array.isArray(response)) {
          replies = response
        } else if (response && typeof response === 'object') {
          for (const key in response) {
            if (Array.isArray(response[key])) {
              replies = response[key]
              break
            }
          }
        }
        
        if (replies && replies.length > 0) {
          // 找到当前评论并更新children
          const index = comments.value.findIndex(c => c.id === comment.id)
          if (index !== -1) {
            comments.value[index].children = replies
            ElMessage.success(`已加载${replies.length}条回复`)
          }
        } else {
          ElMessage.info('没有更多回复')
        }
      } catch (error) {
        console.error('加载回复失败:', error)
        ElMessage.error('加载回复失败')
      }
    }

    onMounted(async () => {
      if (newsId.value) {
        // 处理token和用户信息
        if (store.getters.isAuthenticated) {
          // 如果当前认为已认证，先检查token是否有效
          try {
            // 尝试获取用户信息来验证token
            if (!store.state.user) {
              await store.dispatch('getUserInfo');
              // console.log('用户信息已加载:', store.getters.getUser);
            }
          } catch (error) {
            console.error('获取用户信息失败，可能是token无效:', error);
            // 如果获取用户信息失败，清除认证状态和token
            store.dispatch('logout');
          }
        }
        
        // 然后获取新闻详情和评论
        await fetchNewsDetail();
        checkIfCollected();
        recordViewHistory();
        fetchComments();
      }
    });

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
      toggleCollection,
      // 评论相关
      comments,
      commentTotal,
      commentContent,
      replyContent,
      replyingTo,
      replyToId,
      currentPage,
      pageSize,
      hasMore,
      defaultAvatar,
      isLoadingComments,
      formatDate,
      fetchComments,
      loadMore,
      submitComment,
      handleReply,
      handleReplyToReply,
      cancelReply,
      submitReply,
      handleLike,
      loadReplies,
      isLoggedIn,
      // 用户相关
      store,
      mapGetters
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

/* 评论区样式 */
.comment-section {
  margin-top: 20px;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.comment-form {
  margin-bottom: 20px;
}

.comment-form-footer {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.comment-list {
  margin-top: 20px;
}

.comment-item {
  display: flex;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.comment-avatar {
  margin-right: 16px;
}

.comment-content {
  flex: 1;
}

.comment-info {
  margin-bottom: 8px;
}

.comment-author {
  font-weight: 500;
  margin-right: 8px;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-text {
  margin-bottom: 8px;
  line-height: 1.5;
}

.comment-actions {
  display: flex;
  font-size: 14px;
  color: #909399;
}

.action-item {
  margin-right: 20px;
  cursor: pointer;
}

.action-item:hover {
  color: #409eff;
}

.reply-form {
  margin: 10px 0;
}

.reply-form-footer {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.reply-list {
  background-color: #f9f9f9;
  border-radius: 4px;
  padding: 10px;
  margin-top: 10px;
}

.reply-item {
  display: flex;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.reply-item:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.reply-avatar {
  margin-right: 10px;
}

.reply-content {
  flex: 1;
}

.reply-info {
  margin-bottom: 5px;
  font-size: 13px;
}

.reply-author {
  font-weight: 500;
  margin-right: 5px;
}

.reply-to, .reply-to-author {
  margin-right: 5px;
  color: #409eff;
}

.reply-time {
  font-size: 12px;
  color: #999;
}

.reply-text {
  margin-bottom: 5px;
  line-height: 1.5;
}

.reply-actions {
  display: flex;
  font-size: 13px;
  color: #909399;
}

.no-comments {
  color: #909399;
  text-align: center;
  padding: 20px 0;
}

.load-more {
  text-align: center;
  margin-top: 20px;
}

.load-replies {
  margin-top: 10px;
  text-align: center;
  background-color: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  font-size: 13px;
}

.load-replies .el-button {
  font-size: 13px;
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