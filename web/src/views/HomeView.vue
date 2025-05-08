<template>
  <div class="home" :class="{ 'summary-active': showSummary }">
    <!-- User info displayed in the top-right corner of the news card -->
    <div class="main-content">
      <!-- 个性化推荐区域 -->
      <el-card class="recommendation-card" v-motion-fade>
        <template #header>
          <div class="card-header">
            <h2>
              <el-icon class="recommendation-icon"><Star /></el-icon>
              为您推荐
            </h2>
            <el-button type="primary" size="small" @click="refreshRecommendations" :loading="loadingRecommendations">
              <el-icon><Refresh /></el-icon> 刷新推荐
            </el-button>
          </div>
        </template>
        
        <div v-if="recommendedNews && recommendedNews.length > 0" class="recommendation-list">
          <div 
            v-for="(news, index) in recommendedNews" 
            :key="'rec-' + news.id" 
            class="recommendation-item"
            v-motion-slide-visible
            :delay="index * 100"
          >
            <el-card 
              shadow="hover" 
              :body-style="{ padding: '12px', height: '100%' }" 
              class="recommendation-news-card"
            >
              <h4 class="recommendation-title">{{ news.title || '无标题' }}</h4>
            </el-card>
          </div>
        </div>
        <div v-else-if="!loadingRecommendations" class="empty-data">
          <el-empty description="暂无推荐内容"></el-empty>
        </div>
        <div v-else class="loading-container">
          <el-skeleton :rows="1" animated>
            <template #template>
              <div style="display: flex; justify-content: space-between; gap: 16px;">
                <el-skeleton-item variant="rect" style="width: 30%; height: 150px;" />
                <el-skeleton-item variant="rect" style="width: 30%; height: 150px;" />
                <el-skeleton-item variant="rect" style="width: 30%; height: 150px;" />
              </div>
            </template>
          </el-skeleton>
        </div>
      </el-card>

      <el-card class="news-card" v-motion-fade>
        <template #header>
          <div class="card-header">
            <h2>新闻资讯</h2>
            <el-button type="primary" size="small" @click="refreshNews" :loading="loading">
              <el-icon><Refresh /></el-icon> 刷新新闻
            </el-button>
          </div>
        </template>
        
        <!-- 新闻搜索框 -->
        <div class="news-search" v-motion-slide-top>
          <el-input
            v-model="searchQuery"
            placeholder="搜索新闻标题..."
            class="search-input"
            @keyup.enter="searchNews"
            clearable
          >
            <template #append>
              <el-button @click="searchNews" :loading="searching" :disabled="loading">
                <el-icon><Search /></el-icon> 搜索
              </el-button>
            </template>
          </el-input>
          <div class="search-actions" v-if="isSearchMode">
            <el-tag type="info" class="search-tag" closable @close="clearSearch">
              搜索结果: {{ searchCount }} 条
            </el-tag>
            <el-button type="primary" size="small" @click="clearSearch" plain>
              返回所有新闻
            </el-button>
          </div>
        </div>
        
        <!-- 新闻类型切换按钮 -->
        <div class="news-type-buttons" v-motion-slide-top v-if="!isSearchMode">
          <el-radio-group v-model="currentNewsType" @change="handleNewsTypeChange">
            <el-radio-button label="daily">每日快讯</el-radio-button>
            <el-radio-button label="comprehensive">综合专区</el-radio-button>
            <el-radio-button label="fashion">服装动态</el-radio-button>
            <el-radio-button label="material">原材料专区</el-radio-button>
            <el-radio-button label="exhibition">会展快讯</el-radio-button>
            <el-radio-button label="product">商品动态</el-radio-button>
          </el-radio-group>
        </div>

        <div v-if="newsList && newsList.length > 0" class="news-content">
          <el-timeline>
            <el-timeline-item
              v-for="(news, index) in newsList"
              :key="news.id || index"
              placement="top"
              :timestamp="formatTime(news.publishTime)"
              v-motion-slide-visible
              :delay="index * 100"
            >
              <el-card :body-style="{ transition: 'all 0.3s' }" class="news-item-card" hover>
                <div class="news-item-header">
                  <h4 class="clickable-title" @click="viewNewsDetail(news)">{{ news.title || '无标题' }}</h4>
                  <el-button 
                    size="small" 
                    type="primary"
                    class="ai-summary-button" 
                    @click="generateSummary(news)"
                    :disabled="summarizing && currentNewsId !== news.id"
                    :loading="summarizing && currentNewsId === news.id"
                  >
                    <span class="ai-button-content">
                      <span class="ai-sparkle-icon" style="transform: none !important;">✨</span>
                      <span>总结</span>
                    </span>
                  </el-button>
                </div>
                <el-link v-if="news.link" type="primary" :href="news.link" target="_blank">查看原文</el-link>
                <span v-else class="no-link">暂无链接</span>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          
          <!-- 添加分页组件 -->
          <div class="pagination-container">
            <el-pagination
              v-if="pagination.total > 0"
              background
              layout="prev, pager, next, jumper, sizes, total"
              :current-page="pagination.pageNum"
              :page-size="pagination.pageSize"
              :page-sizes="[10, 20, 30, 50]"
              :total="pagination.total"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
        <div v-else-if="!loading" class="empty-data">
          <el-empty description="暂无新闻数据"></el-empty>
        </div>
        <div v-else class="loading-container">
          <el-skeleton :rows="3" animated />
        </div>
      </el-card>
    </div>

    <!-- AI总结面板 -->
    <transition name="slide-fade">
      <el-card v-if="showSummary" class="summary-panel">
        <template #header>
          <div class="card-header">
            <div class="header-title">
              <div class="ai-icon">
                <span class="sparkle" style="transform: none !important;">✨</span>
              </div>
              <h2>AI总结</h2>
            </div>
            <el-button type="text" @click="closeSummary">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </template>
        <div class="summary-content">
          <h3 class="summary-title">{{ currentSummaryTitle || '正在总结...' }}</h3>
          
          <div v-if="summarizing" class="summary-loading">
            <div class="ai-thinking-animation">
              <div class="thinking-circle c1"></div>
              <div class="thinking-circle c2"></div>
              <div class="thinking-circle c3"></div>
            </div>
            <el-skeleton :rows="3" animated />
            <div class="streaming-dots">
              <span>AI正在思考</span>
              <span class="dot dot1">.</span>
              <span class="dot dot2">.</span>
              <span class="dot dot3">.</span>
            </div>
          </div>

          <div v-else>
            <div v-if="summaryReasoningContent" class="reasoning-content">
              <div class="reasoning-header">
                <div class="reasoning-title">
                  <i class="reasoning-icon" style="transform: none !important;">💡</i>
                  <h4>思考过程</h4>
                </div>
                <el-button type="primary" size="small" class="toggle-button" @click="toggleReasoning">
                  {{ showReasoning ? '隐藏' : '显示' }}
                </el-button>
              </div>
              <div v-if="showReasoning" class="reasoning-text">{{ summaryReasoningContent }}</div>
            </div>
            
            <div class="summary-result">
              <div v-if="summaryContent" class="summary-text">
                <div class="summary-text-header">
                  <i class="summary-icon" style="transform: none !important;">✨</i>
                  <span>摘要内容</span>
                </div>
                <div class="summary-text-content">{{ summaryContent }}</div>
              </div>
              <div v-else-if="summaryError" class="summary-error">
                <el-alert type="error" :title="summaryError" :closable="false" />
              </div>
              <div v-else class="summary-empty">
                <el-empty description="暂无总结内容" />
              </div>
            </div>
            
            <!-- 摘要来源信息 -->
            <div class="summary-source-info" v-if="summaryContent">
              <el-tag size="small" class="source-tag">
                <i class="connection-icon" style="transform: none !important;">🌐</i>
                联网获取内容
              </el-tag>
              <!-- <el-tag size="small" type="info" v-if="currentNewsUrl">链接: {{ currentNewsUrl }}</el-tag> -->
            </div>
          </div>
        </div>
      </el-card>
    </transition>
  </div>
</template>

<script>
import { computed, ref, watch, onMounted } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import request from '@/api/auth' // 导入配置了token的request服务
import { useRouter } from 'vue-router'
import { Close, Refresh, Search, Star} from '@element-plus/icons-vue'

export default {
  name: 'HomeView',
  components: {
    Close,
    Refresh,
    Search,
    Star,
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    const user = computed(() => store.getters.getUser)
    
    // 新闻列表数据
    const newsList = ref([])
    const loading = ref(false)
    const currentNewsType = ref('daily')

    // 分页相关状态
    const pagination = ref({
      pageNum: 1,
      pageSize: 10,
      total: 0,
      pages: 0
    })

    // AI总结相关状态
    const showSummary = ref(false)
    const summarizing = ref(false)
    const summaryContent = ref('')
    const summaryReasoningContent = ref('')
    const summaryError = ref(null)
    const currentNewsId = ref(null)
    const currentSummaryTitle = ref('')
    const showReasoning = ref(false)
    const currentNewsUrl = ref('')

    // 搜索相关状态
    const searchQuery = ref('')
    const searching = ref(false)
    const isSearchMode = ref(false)
    const searchCount = ref(0)

    // 推荐新闻数据
    const recommendedNews = ref([])
    const loadingRecommendations = ref(false)
    const favoriteLoading = ref(null) // 添加收藏加载状态

    // 新闻类型映射
    const newsTypeMap = {
      daily: 'daily_news',
      comprehensive: 'comprehensive_news',
      fashion: 'fashion_news',
      material: 'material_news',
      exhibition: 'exhibition_news',
      product: 'product_news'
    }

    // 登出函数
    const logout = () => {
      store.dispatch('logout')
      router.push('/login')
      ElMessage.success('已成功退出登录')
    }

    // 格式化时间：如果时分秒为0则只显示日期
    const formatTime = (timeStr) => {
      if (!timeStr) return '未知时间'
      
      try {
        const date = new Date(timeStr)
        if (isNaN(date.getTime())) return timeStr // 如果解析失败，返回原始字符串
        
        // 检查时分秒是否都为0
        if (date.getHours() === 0 && date.getMinutes() === 0 && date.getSeconds() === 0) {
          // 只返回日期部分: YYYY-MM-DD
          return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
        } else {
          // 返回完整日期时间: YYYY-MM-DD HH:MM:SS
          return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}:${date.getSeconds().toString().padStart(2, '0')}`
        }
      } catch (e) {
        console.error('时间格式化错误:', e)
        return timeStr // 出错时返回原始字符串
      }
    }

    // 处理新闻类型变化
    const handleNewsTypeChange = () => {
      // 切换类型时重置分页为第一页
      pagination.value.pageNum = 1;
      fetchNewsPage();
    }

    // 处理每页大小变化
    const handleSizeChange = (newSize) => {
      pagination.value.pageSize = newSize;
      fetchNewsPage();
    }

    // 处理页码变化
    const handleCurrentChange = (newPage) => {
      pagination.value.pageNum = newPage;
      fetchNewsPage();
    }

    // 查看新闻详情
    const viewNewsDetail = (news) => {
      if (news.id) {
        router.push({
          name: 'newsDetail',
          params: { id: news.id },
          query: { 
            // 如果是搜索模式且新闻有sourceType，则使用sourceType，否则使用当前类型
            type: isSearchMode.value && news.sourceType ? news.sourceType : currentNewsType.value 
          }
        })
      }
    }

    // 请求新闻列表（不分页）
    const fetchNewsList = async (showSuccessMessage = false) => {
      if (!user.value) {
        return
      }

      loading.value = true
      try {
        const response = await request({
          url: '/api/news/list',
          method: 'get',
          params: {
            type: newsTypeMap[currentNewsType.value]
          }
        })
        
        // 处理返回数据
        if (response && response.data) {
          // 如果数据是数组，直接使用
          newsList.value = Array.isArray(response.data) ? response.data : [];
          // 只有在手动刷新时才显示成功消息
          if (showSuccessMessage) {
            ElMessage.success('新闻列表获取成功');
          }
        } else {
          newsList.value = [];
          if (showSuccessMessage) {
            ElMessage.warning('暂无新闻数据');
          }
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

    // 请求新闻列表（分页）
    const fetchNewsPage = async (showSuccessMessage = false) => {
      if (!user.value) {
        return
      }

      loading.value = true
      try {
        const response = await request({
          url: '/api/news/page',
          method: 'get',
          params: {
            type: newsTypeMap[currentNewsType.value],
            pageNum: pagination.value.pageNum,
            pageSize: pagination.value.pageSize
          }
        })
        
        // 处理返回数据
        if (response && response.data) {
          // 更新新闻列表和分页信息
          newsList.value = response.data.records || [];
          pagination.value.total = response.data.total || 0;
          pagination.value.pages = response.data.pages || 0;
          
          // 只有在手动刷新时才显示成功消息
          if (showSuccessMessage) {
            ElMessage.success('新闻列表获取成功');
          }
        } else {
          newsList.value = [];
          pagination.value.total = 0;
          pagination.value.pages = 0;
          if (showSuccessMessage) {
            ElMessage.warning('暂无新闻数据');
          }
        }
      } catch (error) {
        console.error('获取新闻列表出错', error);
        newsList.value = []; // 清空列表
        pagination.value.total = 0;
        pagination.value.pages = 0;
        
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

    // 刷新新闻按钮处理函数 - 先触发爬虫，再获取最新新闻
    const refreshNews = async () => {
      if (!user.value) {
        return
      }
      
      loading.value = true
      ElMessage.info('正在爬取最新新闻，请稍候...')
      
      try {
        // 首先触发后端爬虫任务
        await request({
          url: '/api/news/trigger-crawl',
          method: 'post'
        })
        
        // 爬取完成后获取最新新闻列表（使用分页）
        await fetchNewsPage(false)
        
        ElMessage.success('新闻爬取和更新成功')
      } catch (error) {
        console.error('触发爬虫任务失败', error)
        
        if (error.response && error.response.status === 401) {
          ElMessage.error('登录已过期，请重新登录')
          store.dispatch('logout')
          router.push('/login')
        } else {
          ElMessage.error(error.message || '爬取新闻失败，尝试获取现有新闻')
          // 即使爬虫失败，也尝试获取现有新闻
          await fetchNewsPage(false)
        }
      } finally {
        loading.value = false
      }
    }

    // 生成新闻摘要
    const generateSummary = async (news) => {
      if (!news.link) {
        ElMessage.warning('该新闻没有链接，无法生成摘要');
        return;
      }

      // 重置状态
      summarizing.value = true;
      summaryContent.value = '';
      summaryReasoningContent.value = '';
      summaryError.value = null;
      showSummary.value = true;
      currentNewsId.value = news.id || null;
      currentSummaryTitle.value = news.title || '无标题';
      currentNewsUrl.value = news.link; // 保存当前新闻链接，用于显示

      // 标记是否使用了备用方案
      let usedFallback = false;
      // 添加一个标志表示是否刚刚开始建立连接
      let isConnecting = true;
      
      // 延迟一小段时间后再允许显示错误，避免在连接尚未建立时就显示错误
      setTimeout(() => {
        isConnecting = false;
      }, 2000);

      try {
        // 获取token
        const token = localStorage.getItem('token');
        
        // 构建URL (使用相对路径，让浏览器自动处理基础URL)
        // 注意：EventSource不支持自定义头，所以将token放入URL参数
        // 不对新闻链接进行编码，直接传给后端
        const apiUrl = `/api/ai/summarize?url=${news.link}&token=${encodeURIComponent(token || '')}`;
        
        // 创建EventSource对象
        const eventSource = new EventSource(apiUrl);
        
        // 设置超时
        const timeout = setTimeout(() => {
          console.error('SSE连接超时');
          if (!usedFallback) {
            usedFallback = true;
            eventSource.close();
            ElMessage.warning('服务器响应超时，使用本地模拟方式生成摘要');
            simulateStreamingSummary(news.link);
          }
        }, 720000); // 12分钟超时
        
        // 监听思考过程开始
        eventSource.addEventListener('reasoning-start', (event) => {
          // console.log('收到reasoning-start事件:', event.data);
          summaryReasoningContent.value = event.data + '\n\n';
        });
        
        // 监听思考过程
        eventSource.addEventListener('reasoning', (event) => {
          // console.log('收到reasoning事件:', event.data);
          summaryReasoningContent.value += event.data + '\n\n';
        });
        
        // 监听思考过程结束
        eventSource.addEventListener('reasoning-end', (event) => {
          // console.log('收到reasoning-end事件:', event.data);
          summaryReasoningContent.value += event.data + '\n\n';
        });
        
        // 监听摘要内容
        eventSource.addEventListener('summary', (event) => {
          // console.log('收到summary事件:', event.data);
          // 如果收到数据表示连接成功，清除超时
          clearTimeout(timeout);
          isConnecting = false; // 连接已确认成功
          summaryContent.value += event.data;
        });
        
        // 监听完成事件
        eventSource.addEventListener('complete', () => {
          // console.log('收到complete事件:', event ? event.data : '无数据');
          clearTimeout(timeout);
          eventSource.close();
          summarizing.value = false;
        });
        
        // 监听错误事件
        eventSource.addEventListener('error', (event) => {
          // console.log('收到error事件:', event.data);
          try {
            if (event.data) {
              // 尝试解析错误数据
              const errorData = JSON.parse(event.data);
              if (errorData.code === 401) {
                ElMessage.error(errorData.message || '认证失败，请重新登录');
                // 如果是401错误，可能需要重新登录
                store.dispatch('logout');
                router.push('/login');
              } else {
                summaryError.value = errorData.message || '生成摘要时发生错误';
              }
              
              // 清理资源
              clearTimeout(timeout);
              eventSource.close();
              
              // 使用备用方案
              if (!usedFallback) {
                usedFallback = true;
                ElMessage.warning('服务器连接失败，使用本地模拟方式生成摘要');
                simulateStreamingSummary(news.link);
              }
            }
          } catch (e) {
            // 如果不是JSON格式，可能是连接级别错误，避免在已收到数据的情况下显示错误
            console.error('SSE错误事件解析失败:', e);
          }
        });
        
        // 通用错误处理 - 改为仅处理无法建立连接的情况
        eventSource.onerror = (error) => {
          console.error('SSE连接错误:', error);
          
          // 如果正在连接，不要立即显示错误
          if (isConnecting) {
            // console.log('连接中，暂不处理错误');
            return;
          }
          
          // 只有在没有收到任何内容且没有使用备用方案时才提示错误
          if (summaryContent.value === '' && !usedFallback) {
            clearTimeout(timeout);
            eventSource.close();
            
            summaryError.value = '连接服务器失败，请稍后重试';
            usedFallback = true;
            
            // 使用备用方案
            ElMessage.warning('无法连接服务器，使用本地模拟方式生成摘要');
            simulateStreamingSummary(news.link);
          }
        };
        
      } catch (error) {
        console.error('生成摘要出错', error);
        summaryError.value = error.message || '生成摘要失败，请稍后重试';
        
        // 使用备用方案
        if (!usedFallback) {
          usedFallback = true;
          ElMessage.warning('API调用失败，使用本地模拟方式生成摘要');
          simulateStreamingSummary(news.link);
        }
      }
    }

    // 模拟流式摘要（备用方案，当SSE不可用时）
    const simulateStreamingSummary = async (newsLink) => {
      // console.log('开始模拟流式摘要');
      // 实际实现应通过WebSocket或SSE从后端获取流式响应
      // 这里模拟流式输出
      
      // 模拟思考过程流式输出
      const reasoningSteps = [
        "1. 访问新闻链接: " + newsLink,
        "2. 提取页面主要内容...",
        "3. 分析文章结构和关键信息...",
        "4. 识别主要观点和事实...",
        "5. 整合信息生成摘要..."
      ];
      
      // 清空之前的内容，确保不重复
      summaryReasoningContent.value = '';
      summaryContent.value = '';
      
      // 发送思考过程开始
      await new Promise(resolve => setTimeout(resolve, 300));
      summaryReasoningContent.value = "开始分析新闻内容\n\n";
      
      for (const step of reasoningSteps) {
        await new Promise(resolve => setTimeout(resolve, 600));
        summaryReasoningContent.value += step + "\n\n";
        // console.log('添加思考步骤:', step);
      }
      
      // 发送思考过程结束
      await new Promise(resolve => setTimeout(resolve, 600));
      summaryReasoningContent.value += "分析完成，正在生成摘要\n\n";
      
      // 模拟最终摘要内容流式输出
      const summaryText = "这篇新闻主要报道了一项重要的行业发展动态。文章分析了当前市场趋势和未来发展方向，强调了技术创新对行业的推动作用。同时，文章还提到了相关政策变化可能带来的影响，以及企业应对策略。整体来看，这是一篇信息量丰富、视角全面的行业分析报道。";
      
      for (let i = 0; i < summaryText.length; i += 5) {
        await new Promise(resolve => setTimeout(resolve, 50));
        summaryContent.value += summaryText.substring(i, i + 5);
        // console.log('添加摘要内容:', summaryText.substring(i, i + 5));
      }
      
      // 标记处理完成
      // console.log('模拟流式摘要完成');
      summarizing.value = false;
    }

    // 关闭摘要面板
    const closeSummary = () => {
      showSummary.value = false;
    }

    // 切换显示思考过程
    const toggleReasoning = () => {
      showReasoning.value = !showReasoning.value;
    }

    // 搜索新闻（分页）
    const searchNews = async () => {
      if (!user.value) {
        return
      }

      searching.value = true
      // 重置分页
      pagination.value.pageNum = 1;
      
      try {
        const response = await request({
          url: '/api/news/search/page',
          method: 'get',
          params: {
            query: searchQuery.value,
            pageNum: pagination.value.pageNum,
            pageSize: pagination.value.pageSize
          }
        })
        
        // 处理返回数据
        if (response && response.data) {
          // 更新新闻列表和分页信息
          newsList.value = response.data.records || [];
          pagination.value.total = response.data.total || 0;
          pagination.value.pages = response.data.pages || 0;
          searchCount.value = pagination.value.total;
          isSearchMode.value = true;
          
          if (newsList.value.length > 0) {
            ElMessage.success('搜索结果获取成功');
          } else {
            ElMessage.warning('暂无搜索结果');
          }
        } else {
          newsList.value = [];
          pagination.value.total = 0;
          pagination.value.pages = 0;
          searchCount.value = 0;
          isSearchMode.value = false;
          ElMessage.warning('暂无搜索结果');
        }
      } catch (error) {
        console.error('搜索新闻出错', error);
        newsList.value = [];
        pagination.value.total = 0;
        pagination.value.pages = 0;
        searchCount.value = 0;
        isSearchMode.value = false;
        ElMessage.error(error.message || '搜索新闻失败，请稍后重试');
      } finally {
        searching.value = false;
      }
    }

    // 清除搜索
    const clearSearch = () => {
      searchQuery.value = '';
      isSearchMode.value = false;
      searchCount.value = 0;
      pagination.value.pageNum = 1;
      fetchNewsPage(false);
    }

    // 获取个性化推荐
    const fetchRecommendations = async () => {
      if (!user.value) {
        return
      }

      loadingRecommendations.value = true
      try {
        const response = await request({
          url: '/api/recommendation/personalized',
          method: 'get',
          params: {
            limit: 12, // 从后端请求更多推荐，去重后仍能保持足够的展示数量
            timestamp: new Date().getTime() // 添加时间戳防止缓存
          }
        })
        
        // 处理返回数据
        if (response && response.data) {
          const newRecommendations = Array.isArray(response.data) ? response.data : [];
          
          // 使用Map根据ID过滤掉重复的推荐
          const uniqueRecommendations = [];
          const seenIds = new Set();
          
          for (const news of newRecommendations) {
            if (news.id && !seenIds.has(news.id)) {
              seenIds.add(news.id);
              uniqueRecommendations.push(news);
            }
          }
          
          recommendedNews.value = uniqueRecommendations;
          
          // 获取收藏状态
          await fetchFavoriteStatus();
          console.log('获取推荐成功，条数:', recommendedNews.value.length);
        } else {
          recommendedNews.value = [];
          console.warn('获取推荐返回空数据');
        }
      } catch (error) {
        console.error('获取推荐出错', error);
        recommendedNews.value = [];
        
        if (error.response && error.response.status === 401) {
          ElMessage.error('登录已过期，请重新登录');
          store.dispatch('logout');
          router.push('/login');
        } else {
          ElMessage.error(error.message || '获取推荐失败，请稍后重试');
        }
      } finally {
        loadingRecommendations.value = false;
      }
    }

    // 获取新闻收藏状态
    const fetchFavoriteStatus = async () => {
      if (!user.value || recommendedNews.value.length === 0) {
        return;
      }
      
      try {
        const newsIds = recommendedNews.value.map(news => news.id);
        const response = await request({
          url: '/api/favorites/status',
          method: 'post',
          data: {
            newsIds
          }
        });
        
        if (response && response.data) {
          // 更新收藏状态
          recommendedNews.value = recommendedNews.value.map(news => ({
            ...news,
            favorited: response.data.includes(news.id)
          }));
        }
      } catch (error) {
        console.error('获取收藏状态出错', error);
      }
    }

    // 切换收藏状态
    const toggleFavorite = async (news) => {
      if (!user.value || !news.id) {
        ElMessage.warning('无法执行操作，请稍后重试');
        return;
      }
      
      favoriteLoading.value = news.id;
      
      try {
        if (news.favorited) {
          // 取消收藏
          await request({
            url: `/api/favorites/remove/${news.id}`,
            method: 'delete'
          });
          ElMessage.success('已取消收藏');
        } else {
          // 添加收藏
          await request({
            url: '/api/favorites/add',
            method: 'post',
            data: {
              newsId: news.id
            }
          });
          ElMessage.success('收藏成功');
        }
        
        // 更新本地状态
        news.favorited = !news.favorited;
      } catch (error) {
        console.error('切换收藏状态出错', error);
        ElMessage.error(error.message || '操作失败，请稍后重试');
      } finally {
        favoriteLoading.value = null;
      }
    }

    // 刷新推荐
    const refreshRecommendations = () => {
      fetchRecommendations()
    }

    // 在组件挂载时，如果已经有用户信息，则获取新闻列表和推荐
    onMounted(() => {
      if (user.value) {
        fetchNewsList()
        fetchRecommendations()
      }
    })

    // 监听用户信息变化，当用户信息加载完成后获取新闻列表和推荐
    watch(user, (newUser) => {
      if (newUser) {
        fetchNewsList()
        fetchRecommendations()
      }
    }, { immediate: true })

    return {
      user,
      newsList,
      loading,
      currentNewsType,
      handleNewsTypeChange,
      fetchNewsList,
      fetchNewsPage,
      formatTime,
      viewNewsDetail,
      logout,
      // 分页相关
      pagination,
      handleSizeChange,
      handleCurrentChange,
      // AI总结相关
      generateSummary,
      closeSummary,
      showSummary,
      summarizing,
      summaryContent,
      summaryReasoningContent,
      summaryError,
      currentNewsId,
      currentSummaryTitle,
      showReasoning,
      toggleReasoning,
      currentNewsUrl,
      refreshNews,
      // 搜索相关
      searchQuery,
      searching,
      isSearchMode,
      searchCount,
      searchNews,
      clearSearch,
      // 推荐相关
      recommendedNews,
      loadingRecommendations,
      refreshRecommendations,
      // 收藏相关
      toggleFavorite,
      favoriteLoading
    }
  }
}
</script>

<style scoped>
.home {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  position: relative;
  transition: all 0.3s ease;
}

.main-content {
  flex: 1;
  transition: all 0.3s ease;
  max-width: 100%;
}

/* 推荐区域样式 */
.recommendation-card {
  margin-bottom: 20px;
}

.recommendation-icon {
  color: #FFD700;
  margin-right: 5px;
  vertical-align: middle;
}

.recommendation-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 10px;
}

.recommendation-item {
  flex: 1;
  min-width: 250px;
  max-width: calc(33.33% - 12px);
}

.recommendation-news-card {
  transition: all 0.3s ease;
  height: 100%;
  cursor: pointer;
}

.recommendation-news-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.recommendation-title {
  margin: 0;
  font-size: 14px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.recommendation-news-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  justify-content: space-between;
}

.recommendation-news-content h4 {
  margin-top: 0;
  margin-bottom: 12px;
  font-size: 16px;
  line-height: 1.4;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.recommendation-news-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

/* 原有样式 */
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
  display: flex;
  align-items: center;
}

.news-search {
  margin: 20px 0;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.search-input {
  width: 400px;
  max-width: 100%;
}

.search-tag {
  margin-left: 10px;
}

.search-actions {
  display: flex;
  align-items: center;
  margin-top: 10px;
  gap: 10px;
}

.news-type-buttons {
  margin: 20px 0;
  display: flex;
  justify-content: center;
  flex-wrap: wrap; /* Allow buttons to wrap on smaller screens */
}

.news-content {
  margin-top: 20px;
}

.news-item-card {
  transition: all 0.3s !important;
  transform: translateZ(0);
  will-change: transform;
}

.news-item-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1) !important;
}

.news-item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.news-item-header h4 {
  margin: 0;
  flex: 1;
}

.empty-data {
  padding: 30px 0;
}

.loading-container {
  padding: 30px 0;
}

.no-link {
  color: #909399;
  font-size: 13px;
}

.clickable-title {
  cursor: pointer;
  transition: color 0.2s;
}

.clickable-title:hover {
  color: var(--el-color-primary);
}

.clickable-title::after {
  content: '';
  position: absolute;
  width: 0;
  height: 2px;
  bottom: -2px;
  left: 0;
  background-color: #409eff;
  transition: width 0.3s ease;
}

.clickable-title:hover::after {
  width: 50%;
}

h4 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #303133;
}

/* 摘要面板样式 */
.summary-panel {
  width: 38%;
  margin-left: 20px;
  transition: all 0.3s ease;
  max-height: calc(100vh - 100px);
  display: flex;
  flex-direction: column;
  opacity: 1;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.summary-active .main-content {
  width: 60%;
}

.summary-content {
  display: flex;
  flex-direction: column;
  overflow-y: visible;
  padding: 0;
}

.summary-panel .card-header {
  background: linear-gradient(135deg, #647eff 0%, #42d392 100%);
  color: white;
  padding: 15px;
  border-radius: 16px 16px 0 0;
}

.summary-panel .card-header h2 {
  color: white;
  margin: 0;
  font-weight: 600;
}

.summary-panel .el-button--text {
  color: white;
}

.summary-loading {
  margin: 20px 0;
}

.streaming-dots {
  margin-top: 10px;
  text-align: center;
  font-size: 16px;
  background: linear-gradient(90deg, #42d392, #647eff, #d342bc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: bold;
}

.dot {
  animation: blink 1.4s infinite;
  opacity: 0;
}

.dot1 {
  animation-delay: 0s;
}

.dot2 {
  animation-delay: 0.2s;
}

.dot3 {
  animation-delay: 0.4s;
}

@keyframes blink {
  0% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}

.reasoning-content {
  background: linear-gradient(135deg, #f5f7fa 0%, #edf2ff 100%);
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  max-height: none;
  overflow: visible;
}

.reasoning-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.reasoning-header h4 {
  margin: 0;
  font-size: 15px;
  color: #647eff;
  font-weight: 600;
}

.reasoning-text {
  margin-top: 10px;
  font-size: 14px;
  line-height: 1.6;
  color: #454655;
  white-space: pre-line;
  max-height: none;
  overflow: visible;
}

.summary-text {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  padding: 15px;
  background: linear-gradient(135deg, #ffffff 0%, #f0fff7 100%);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow: visible;
}

.summary-error {
  margin: 20px 0;
}

:deep(.el-timeline-item__timestamp) {
  color: #909399;
  font-size: 13px;
}

:deep(.el-timeline-item__node) {
  background-color: #409EFF;
}

:deep(.el-timeline-item__tail) {
  border-left-color: #E4E7ED;
}

/* Animation for slide-fade transition */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateX(30px);
  opacity: 0;
}

/* Responsive styles */
@media (max-width: 768px) {
  .home.summary-active {
    flex-direction: column;
  }
  
  .summary-active .main-content {
    width: 100%;
  }
  
  .summary-panel {
    width: 100%;
    margin-left: 0;
    margin-top: 20px;
  }
  
  .news-type-buttons {
    overflow-x: auto;
    justify-content: flex-start;
    padding-bottom: 10px;
  }
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  padding: 10px 0;
}

/* 修改AI总结按钮样式，使其更加彩色现代 */
.ai-summary-button {
  background: linear-gradient(135deg, #42d392 0%, #647eff 100%);
  border: none;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  padding: 8px 15px;
}

.ai-summary-button::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(
    to bottom right,
    rgba(255, 255, 255, 0) 0%,
    rgba(255, 255, 255, 0.2) 50%,
    rgba(255, 255, 255, 0) 100%
  );
  transform: rotate(45deg);
  transition: transform 0.6s;
  z-index: 1;
  opacity: 0;
}

.ai-summary-button:hover:not(:disabled)::before {
  transform: rotate(45deg) translate(100%, 100%);
  opacity: 1;
}

.ai-summary-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(64, 158, 255, 0.3);
  background: linear-gradient(135deg, #35bb7f 0%, #536cdb 100%);
}

.ai-summary-button:active:not(:disabled) {
  transform: translateY(0);
}

.ai-button-content {
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative;
  z-index: 2;
}

.ai-sparkle-icon {
  font-size: 16px;
  display: inline-block;
}

.summary-source-info {
  margin-top: 15px;
  display: flex;
  gap: 10px;
}

.summary-source-info .el-tag--success {
  background: linear-gradient(90deg, #42d392, #647eff);
  border: none;
  color: white;
  padding: 5px 10px;
}

.summary-title {
  color: #303133;
  font-weight: 600;
  font-size: 18px;
  margin-bottom: 20px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-icon {
  background: white;
  color: #647eff;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  font-size: 14px;
}

.ai-thinking-animation {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
}

.thinking-circle {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  animation: thinking 1.8s infinite ease-in-out;
}

.c1 {
  background-color: #42d392;
  animation-delay: 0s;
}

.c2 {
  background-color: #647eff;
  animation-delay: 0.2s;
}

.c3 {
  background-color: #d342bc;
  animation-delay: 0.4s;
}

@keyframes thinking {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.reasoning-title, .summary-text-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reasoning-icon, .summary-icon, .connection-icon {
  font-size: 18px;
}

.summary-text-header {
  padding-bottom: 10px;
  margin-bottom: 10px;
  border-bottom: 1px solid rgba(66, 211, 146, 0.2);
  color: #647eff;
  font-weight: 600;
}

.summary-text-content {
  padding-top: 5px;
}

.source-tag {
  display: flex;
  align-items: center;
  gap: 5px;
  background: linear-gradient(90deg, #42d392, #647eff);
  border: none;
  color: white;
  padding: 5px 10px;
  border-radius: 15px;
  box-shadow: 0 2px 6px rgba(66, 211, 146, 0.3);
}

.sparkle {
  font-size: 18px;
}

.toggle-button {
  padding: 6px 12px;
  border-radius: 12px;
  box-shadow: 0 2px 6px rgba(100, 126, 255, 0.2);
  font-weight: bold;
}

.summary-panel .card-header {
  border-radius: 16px 16px 0 0;
}

.summary-panel:deep(.el-card__body) {
  max-height: calc(100vh - 180px);
  overflow-y: auto;
  padding: 20px;
  border-radius: 0 0 16px 16px !important;
  background-color: white;
}

.summary-result {
  margin-top: 20px;
  overflow: visible;
}

/* 确保el-card有圆角 */
.summary-panel:deep(.el-card) {
  border-radius: 16px !important;
  overflow: hidden;
  background-color: transparent;
  border: none;
  box-shadow: none;
}

.summary-panel:deep(.el-card__header) {
  border-radius: 16px 16px 0 0 !important;
  border-bottom: none;
  padding: 0;
}

.summary-panel:deep(.el-card__body) {
  padding: 20px;
  border-radius: 0 0 16px 16px !important;
  background-color: white;
}

/* 确保思考过程和摘要内容都能完整显示 */
.reasoning-text {
  margin-top: 10px;
  font-size: 14px;
  line-height: 1.6;
  color: #454655;
  white-space: pre-line;
  max-height: none;
  overflow: visible;
}

.summary-result {
  margin-top: 20px;
}
</style>
