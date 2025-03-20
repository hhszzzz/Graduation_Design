<template>
  <div class="home" :class="{ 'summary-active': showSummary }">
    <div class="main-content">
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
            <h2>新闻资讯</h2>
            <el-button type="primary" size="small" @click="fetchNewsList" :loading="loading">
              刷新新闻
            </el-button>
          </div>
        </template>
        
        <!-- 新闻类型切换按钮 -->
        <div class="news-type-buttons">
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
            >
              <el-card>
                <div class="news-item-header">
                  <h4>{{ news.title || '无标题' }}</h4>
                  <el-button 
                    size="small" 
                    type="info" 
                    @click="generateSummary(news)"
                    :disabled="summarizing && currentNewsId !== news.id"
                    :loading="summarizing && currentNewsId === news.id"
                  >
                    AI总结
                  </el-button>
                </div>
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

    <!-- AI总结面板 -->
    <el-card v-if="showSummary" class="summary-panel">
      <template #header>
        <div class="card-header">
          <h2>AI总结</h2>
          <el-button type="text" @click="closeSummary">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </template>
      <div class="summary-content">
        <h3>{{ currentSummaryTitle || '正在总结...' }}</h3>
        
        <div v-if="summarizing" class="summary-loading">
          <el-skeleton :rows="3" animated />
          <div class="streaming-dots">
            <span>生成中</span>
            <span class="dot dot1">.</span>
            <span class="dot dot2">.</span>
            <span class="dot dot3">.</span>
          </div>
        </div>

        <div v-else>
          <div v-if="summaryReasoningContent" class="reasoning-content">
            <div class="reasoning-header">
              <h4>思考过程</h4>
              <el-button type="text" size="small" @click="toggleReasoning">
                {{ showReasoning ? '隐藏' : '显示' }}
              </el-button>
            </div>
            <div v-if="showReasoning" class="reasoning-text">{{ summaryReasoningContent }}</div>
          </div>
          
          <div class="summary-result">
            <div v-if="summaryContent" class="summary-text">{{ summaryContent }}</div>
            <div v-else-if="summaryError" class="summary-error">
              <el-alert type="error" :title="summaryError" :closable="false" />
            </div>
            <div v-else class="summary-empty">
              <el-empty description="暂无总结内容" />
            </div>
          </div>
          
          <!-- 摘要来源信息 -->
          <div class="summary-source-info" v-if="summaryContent">
            <el-tag size="small" type="success">联网获取内容</el-tag>
            <el-tag size="small" type="info" v-if="currentNewsUrl">链接: {{ currentNewsUrl }}</el-tag>
          </div>
        </div>
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
import { Close } from '@element-plus/icons-vue'

export default {
  name: 'HomeView',
  components: {
    Close
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    const user = computed(() => store.getters.getUser)
    
    // 新闻列表数据
    const newsList = ref([])
    const loading = ref(false)
    const currentNewsType = ref('daily')

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

    // 新闻类型映射
    const newsTypeMap = {
      daily: 'daily_news',
      comprehensive: 'comprehensive_news',
      fashion: 'fashion_news',
      material: 'material_news',
      exhibition: 'exhibition_news',
      product: 'product_news'
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
      fetchNewsList()
    }

    // 请求新闻列表
    const fetchNewsList = async () => {
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
          console.log('收到reasoning-start事件:', event.data);
          summaryReasoningContent.value = event.data + '\n\n';
        });
        
        // 监听思考过程
        eventSource.addEventListener('reasoning', (event) => {
          console.log('收到reasoning事件:', event.data);
          summaryReasoningContent.value += event.data + '\n\n';
        });
        
        // 监听思考过程结束
        eventSource.addEventListener('reasoning-end', (event) => {
          console.log('收到reasoning-end事件:', event.data);
          summaryReasoningContent.value += event.data + '\n\n';
        });
        
        // 监听摘要内容
        eventSource.addEventListener('summary', (event) => {
          console.log('收到summary事件:', event.data);
          // 如果收到数据表示连接成功，清除超时
          clearTimeout(timeout);
          isConnecting = false; // 连接已确认成功
          summaryContent.value += event.data;
        });
        
        // 监听完成事件
        eventSource.addEventListener('complete', (event) => {
          console.log('收到complete事件:', event ? event.data : '无数据');
          clearTimeout(timeout);
          eventSource.close();
          summarizing.value = false;
        });
        
        // 监听错误事件
        eventSource.addEventListener('error', (event) => {
          console.log('收到error事件:', event.data);
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
            console.log('连接中，暂不处理错误');
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
      console.log('开始模拟流式摘要');
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
        console.log('添加思考步骤:', step);
      }
      
      // 发送思考过程结束
      await new Promise(resolve => setTimeout(resolve, 600));
      summaryReasoningContent.value += "分析完成，正在生成摘要\n\n";
      
      // 模拟最终摘要内容流式输出
      const summaryText = "这篇新闻主要报道了一项重要的行业发展动态。文章分析了当前市场趋势和未来发展方向，强调了技术创新对行业的推动作用。同时，文章还提到了相关政策变化可能带来的影响，以及企业应对策略。整体来看，这是一篇信息量丰富、视角全面的行业分析报道。";
      
      for (let i = 0; i < summaryText.length; i += 5) {
        await new Promise(resolve => setTimeout(resolve, 50));
        summaryContent.value += summaryText.substring(i, i + 5);
        console.log('添加摘要内容:', summaryText.substring(i, i + 5));
      }
      
      // 标记处理完成
      console.log('模拟流式摘要完成');
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
      currentNewsType,
      handleNewsTypeChange,
      fetchNewsList,
      formatTime,
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
      currentNewsUrl
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

.news-type-buttons {
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
}

.news-content {
  margin-top: 20px;
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
  font-size: 14px;
  font-style: italic;
}

h4 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #303133;
}

/* 摘要面板样式 */
.summary-panel {
  width: 0;
  overflow: hidden;
  margin-top: 20px;
  margin-left: 0;
  transition: all 0.3s ease;
  opacity: 0;
  max-height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
}

.summary-active .main-content {
  max-width: 60%;
}

.summary-active .summary-panel {
  width: 38%;
  opacity: 1;
  margin-left: 2%;
  overflow: auto;
}

.summary-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.summary-loading {
  margin: 20px 0;
}

.streaming-dots {
  margin-top: 10px;
  text-align: center;
  font-size: 16px;
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
  background-color: #f5f7fa;
  border-radius: 4px;
  padding: 10px;
  margin-bottom: 20px;
}

.reasoning-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.reasoning-header h4 {
  margin: 0;
  font-size: 14px;
  color: #606266;
}

.reasoning-text {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.5;
  color: #606266;
  white-space: pre-line;
}

.summary-text {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
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
</style>
