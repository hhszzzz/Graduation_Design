<template>
  <div class="data-visualization">
    <el-row :gutter="20">
      <!-- 页面标题 -->
      <el-col :span="24">
        <el-card class="page-header-card">
          <h1>数据可视化分析</h1>
          <p>展示新闻热点趋势和关键词分析</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 日期选择器 -->
    <el-row :gutter="20" class="date-filter-row">
      <el-col :span="24">
        <el-card class="filter-card">
          <div class="filter-container">
            <div class="filter-item">
              <el-radio-group v-model="dateRange" @change="handleDateRangeChange">
                <el-radio-button label="7">近7天</el-radio-button>
                <el-radio-button label="30">近30天</el-radio-button>
                <el-radio-button label="90">近3个月</el-radio-button>
                <el-radio-button label="custom">自定义</el-radio-button>
              </el-radio-group>
            </div>
            <div class="filter-item" v-if="dateRange === 'custom'">
              <el-date-picker
                v-model="customDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                @change="fetchTrendData"
                :disabled-date="disabledDate"
              />
            </div>
            <!-- <div class="filter-item action-buttons">
              <el-button 
                type="primary" 
                :loading="processingKeywords" 
                @click="processKeywords" 
                size="small"
              >
                <i class="el-icon-refresh-right"></i> 提取关键词
              </el-button>
              <el-tooltip content="从数据库中已有的新闻提取关键词，增加数据可视化内容" placement="top">
                <i class="el-icon-question"></i>
              </el-tooltip>
            </div> -->
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热点趋势折线图 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <h2>热点趋势折线图</h2>
              <el-select v-model="trendCategory" @change="fetchTrendData" placeholder="选择新闻分类">
                <el-option label="全部分类" value="all" />
                <el-option label="每日快讯" value="daily_news" />
                <el-option label="综合专区" value="comprehensive_news" />
                <el-option label="服装动态" value="fashion_news" />
                <el-option label="原材料专区" value="material_news" />
                <el-option label="会展快讯" value="exhibition_news" />
                <el-option label="商品动态" value="product_news" />
              </el-select>
            </div>
          </template>
          <div v-if="loadingTrend" class="loading-container">
            <el-skeleton :rows="8" animated />
          </div>
          <div v-else-if="trendData.dates.length === 0" class="empty-data">
            <el-empty description="暂无趋势数据"></el-empty>
          </div>
          <div v-else>
            <div ref="trendChart" class="chart-container"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热门关键词柱状图 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <h2>热门关键词柱状图</h2>
              <el-select v-model="keywordCategory" @change="fetchKeywordData" placeholder="选择新闻分类">
                <el-option label="全部分类" value="all" />
                <el-option label="每日快讯" value="daily_news" />
                <el-option label="综合专区" value="comprehensive_news" />
                <el-option label="服装动态" value="fashion_news" />
                <el-option label="原材料专区" value="material_news" />
                <el-option label="会展快讯" value="exhibition_news" />
                <el-option label="商品动态" value="product_news" />
              </el-select>
            </div>
          </template>
          <div v-if="loadingKeyword" class="loading-container">
            <el-skeleton :rows="8" animated />
          </div>
          <div v-else-if="keywordData.length === 0" class="empty-data">
            <el-empty description="暂无关键词数据"></el-empty>
          </div>
          <div v-else>
            <div ref="keywordChart" class="chart-container"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热门关键词词云图 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <h2>关键词词云图</h2>
            </div>
          </template>
          <div v-if="loadingKeyword" class="loading-container">
            <el-skeleton :rows="8" animated />
          </div>
          <div v-else-if="keywordData.length === 0" class="empty-data">
            <el-empty description="暂无关键词数据"></el-empty>
          </div>
          <div v-else>
            <div ref="wordCloudChart" class="chart-container"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick, watch, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/auth'
import * as echarts from 'echarts'
import 'echarts-wordcloud'

export default {
  name: 'DataVisualizationView',
  setup() {
    // 图表引用
    const trendChart = ref(null)
    const keywordChart = ref(null)
    const wordCloudChart = ref(null)
    
    // 图表实例
    let trendChartInstance = null
    let keywordChartInstance = null
    let wordCloudChartInstance = null
    
    // 日期过滤
    const dateRange = ref('7') // 默认显示近7天
    const customDateRange = ref([])
    
    // 分类过滤
    const trendCategory = ref('all')
    const keywordCategory = ref('all')
    
    // 加载状态
    const loadingTrend = ref(false)
    const loadingKeyword = ref(false)
    const processingKeywords = ref(false)
    
    // 图表数据
    const trendData = reactive({
      dates: [],
      counts: []
    })
    
    const keywordData = ref([])
    
    // 禁用未来日期
    const disabledDate = (time) => {
      return time.getTime() > Date.now()
    }
    
    // 处理日期范围变化
    const handleDateRangeChange = () => {
      if (dateRange.value !== 'custom') {
        fetchTrendData()
        fetchKeywordData()
      }
    }
    
    // 获取趋势数据
    const fetchTrendData = async () => {
      if (loadingTrend.value) return
      
      loadingTrend.value = true
      
      try {
        let params = {
          category: trendCategory.value
        }
        
        if (dateRange.value === 'custom' && customDateRange.value && customDateRange.value.length === 2) {
          params.startDate = formatDate(customDateRange.value[0])
          params.endDate = formatDate(customDateRange.value[1])
        } else {
          params.days = dateRange.value
        }
        
        const response = await request({
          url: '/api/keywords/trend',
          method: 'get',
          params
        })
        
        if (response && response.data) {
          trendData.dates = response.data.dates || []
          trendData.counts = response.data.counts || []
          
          nextTick(() => {
            initTrendChart()
          })
        }
      } catch (error) {
        console.error('获取趋势数据失败', error)
        ElMessage.error('获取趋势数据失败，请重试：' + (error.response?.data?.error || error.message))
        // 清空数据，避免显示旧数据
        trendData.dates = []
        trendData.counts = []
      } finally {
        loadingTrend.value = false
      }
    }
    
    // 获取关键词数据
    const fetchKeywordData = async () => {
      if (loadingKeyword.value) return
      
      loadingKeyword.value = true
      
      try {
        let params = {
          category: keywordCategory.value
        }
        
        if (dateRange.value === 'custom' && customDateRange.value && customDateRange.value.length === 2) {
          params.startDate = formatDate(customDateRange.value[0])
          params.endDate = formatDate(customDateRange.value[1])
        } else {
          params.days = dateRange.value
        }
        
        const response = await request({
          url: '/api/keywords/top',
          method: 'get',
          params
        })
        
        if (response && response.data) {
          keywordData.value = response.data || []
          
          nextTick(() => {
            initKeywordChart()
            initWordCloudChart()
          })
        }
      } catch (error) {
        console.error('获取关键词数据失败', error)
        ElMessage.error('获取关键词数据失败，请重试：' + (error.response?.data?.error || error.message))
        // 清空数据，避免显示旧数据
        keywordData.value = []
      } finally {
        loadingKeyword.value = false
      }
    }
    
    // 格式化日期
    const formatDate = (date) => {
      const d = new Date(date)
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    }
    
    // 初始化趋势图表
    const initTrendChart = () => {
      if (!trendChart.value) return
      
      // 使用nextTick确保DOM已更新
      nextTick(() => {
        // 每次初始化前先检查实例是否需要重建
        if (trendChartInstance) {
          try {
            trendChartInstance.dispose()
          } catch (e) {
            console.error('趋势图表销毁失败', e)
          }
        }
        
        try {
          // 重新创建实例
          trendChartInstance = echarts.init(trendChart.value)
          
          if (trendData.dates.length === 0 || trendData.counts.length === 0) {
            return
          }
          
          const option = {
            tooltip: {
              trigger: 'axis',
              axisPointer: {
                type: 'shadow'
              }
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              containLabel: true
            },
            xAxis: {
              type: 'category',
              data: trendData.dates,
              axisLabel: {
                rotate: 45
              }
            },
            yAxis: {
              type: 'value',
              name: '热点数量'
            },
            series: [
              {
                name: '热点数量',
                type: 'line',
                data: trendData.counts,
                smooth: true,
                lineStyle: {
                  width: 3,
                  color: '#5470c6'
                },
                itemStyle: {
                  color: '#5470c6'
                },
                areaStyle: {
                  color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                    {
                      offset: 0,
                      color: 'rgba(84, 112, 198, 0.5)'
                    },
                    {
                      offset: 1,
                      color: 'rgba(84, 112, 198, 0.1)'
                    }
                  ])
                }
              }
            ]
          }
          
          trendChartInstance.setOption(option)
        } catch (e) {
          console.error('初始化趋势图表失败', e)
        }
      })
    }
    
    // 初始化关键词柱状图
    const initKeywordChart = () => {
      if (!keywordChart.value) return
      
      // 使用nextTick确保DOM已更新
      nextTick(() => {
        // 每次初始化前先检查实例是否需要重建
        if (keywordChartInstance) {
          try {
            keywordChartInstance.dispose()
          } catch (e) {
            console.error('关键词图表销毁失败', e)
          }
        }
        
        try {
          // 重新创建实例
          keywordChartInstance = echarts.init(keywordChart.value)
          
          // 仅取前10个关键词
          const topKeywords = keywordData.value.slice(0, 10)
          
          if (topKeywords.length === 0) {
            return
          }
          
          const option = {
            tooltip: {
              trigger: 'axis',
              axisPointer: {
                type: 'shadow'
              }
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              containLabel: true
            },
            xAxis: {
              type: 'value',
              name: '出现频率'
            },
            yAxis: {
              type: 'category',
              data: topKeywords.map(item => item.keyword),
              axisLabel: {
                interval: 0,
                rotate: 30
              }
            },
            series: [
              {
                name: '出现频率',
                type: 'bar',
                data: topKeywords.map(item => item.count),
                itemStyle: {
                  color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                    { offset: 0, color: '#83bff6' },
                    { offset: 0.5, color: '#188df0' },
                    { offset: 1, color: '#188df0' }
                  ])
                }
              }
            ]
          }
          
          keywordChartInstance.setOption(option)
        } catch (e) {
          console.error('初始化关键词柱状图失败', e)
        }
      })
    }
    
    // 初始化词云图
    const initWordCloudChart = () => {
      if (!wordCloudChart.value) return
      
      // 使用nextTick确保DOM已更新
      nextTick(() => {
        // 每次初始化前先检查实例是否需要重建
        if (wordCloudChartInstance) {
          try {
            wordCloudChartInstance.dispose()
          } catch (e) {
            console.error('词云图表销毁失败', e)
          }
        }
        
        try {
          // 重新创建实例
          wordCloudChartInstance = echarts.init(wordCloudChart.value)
          
          if (keywordData.value.length === 0) {
            return
          }
          
          const option = {
            tooltip: {
              show: true,
              formatter: function(params) {
                return params.data.name + ': ' + params.data.value
              }
            },
            series: [{
              type: 'wordCloud',
              shape: 'circle',
              left: 'center',
              top: 'center',
              width: '80%',
              height: '80%',
              right: null,
              bottom: null,
              sizeRange: [12, 50],
              rotationRange: [-90, 90],
              rotationStep: 45,
              gridSize: 8,
              drawOutOfBound: false,
              textStyle: {
                fontFamily: 'sans-serif',
                fontWeight: 'bold',
                color: function() {
                  return 'rgb(' + [
                    Math.round(Math.random() * 160),
                    Math.round(Math.random() * 160),
                    Math.round(Math.random() * 160)
                  ].join(',') + ')'
                }
              },
              emphasis: {
                focus: 'self',
                textStyle: {
                  shadowBlur: 10,
                  shadowColor: '#333'
                }
              },
              data: keywordData.value.map(item => ({
                name: item.keyword,
                value: item.count
              }))
            }]
          }
          
          wordCloudChartInstance.setOption(option)
        } catch (e) {
          console.error('初始化词云图失败', e)
        }
      })
    }
    
    // 监听窗口大小变化，重绘图表
    const resizeCharts = () => {
      if (trendChartInstance) trendChartInstance.resize()
      if (keywordChartInstance) keywordChartInstance.resize()
      if (wordCloudChartInstance) wordCloudChartInstance.resize()
    }
    
    // 监听分类变化
    watch(trendCategory, () => {
      fetchTrendData()
    })
    
    watch(keywordCategory, () => {
      fetchKeywordData()
    })
    
    // 监听日期自定义范围变化
    watch(customDateRange, () => {
      if (dateRange.value === 'custom' && customDateRange.value && customDateRange.value.length === 2) {
        fetchTrendData()
        fetchKeywordData()
      }
    })
    
    // 监听数据变化，强制刷新图表
    watch([() => trendData.dates, () => trendData.counts], () => {
      initTrendChart()
    }, { deep: true })
    
    watch(() => keywordData.value, () => {
      initKeywordChart()
      initWordCloudChart()
    }, { deep: true })
    
    // 手动触发关键词提取
    const processKeywords = async () => {
      if (processingKeywords.value) return
      
      processingKeywords.value = true
      try {
        const response = await request({
          url: '/api/news/process-keywords',
          method: 'post'
        })
        
        if (response && response.data && response.data.success) {
          ElMessage.success('关键词提取任务已在后台启动，完成后数据将更新')
          // 提取完成后刷新图表
          setTimeout(() => {
            fetchTrendData()
            fetchKeywordData()
          }, 2000) // 给后端一些处理时间
        } else {
          const errorMsg = response.data?.message || '触发关键词提取失败'
          ElMessage.error(errorMsg)
        }
      } catch (error) {
        console.error('触发关键词提取失败', error)
        ElMessage.error('触发关键词提取失败，请稍后重试：' + (error.response?.data?.error || error.message))
      } finally {
        processingKeywords.value = false
      }
    }
    
    // 组件挂载时初始化
    onMounted(() => {
      fetchTrendData()
      fetchKeywordData()
      
      window.addEventListener('resize', resizeCharts)
    })
    
    // 组件销毁前清理
    onBeforeUnmount(() => {
      window.removeEventListener('resize', resizeCharts)
      if (trendChartInstance) {
        trendChartInstance.dispose()
        trendChartInstance = null
      }
      if (keywordChartInstance) {
        keywordChartInstance.dispose()
        keywordChartInstance = null
      }
      if (wordCloudChartInstance) {
        wordCloudChartInstance.dispose()
        wordCloudChartInstance = null
      }
    })
    
    // 提供手动刷新方法
    const manualRefresh = () => {
      fetchTrendData()
      fetchKeywordData()
    }
    
    return {
      trendChart,
      keywordChart,
      wordCloudChart,
      dateRange,
      customDateRange,
      trendCategory,
      keywordCategory,
      loadingTrend,
      loadingKeyword,
      processingKeywords,
      trendData,
      keywordData,
      disabledDate,
      handleDateRangeChange,
      fetchTrendData,
      fetchKeywordData,
      processKeywords,
      manualRefresh
    }
  }
}
</script>

<style scoped>
.data-visualization {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header-card {
  margin-bottom: 20px;
  text-align: center;
  background: linear-gradient(135deg, #1976d2, #64b5f6);
  color: white;
}

.page-header-card h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
}

.page-header-card p {
  margin-top: 10px;
  font-size: 16px;
  opacity: 0.9;
}

.date-filter-row {
  margin-bottom: 20px;
}

.filter-card {
  background-color: #f5f7fa;
}

.filter-container {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 15px;
}

.filter-item {
  margin-right: 15px;
}

.action-buttons {
  display: flex;
  align-items: center;
  margin-left: auto;
}

.action-buttons .el-icon-question {
  margin-left: 8px;
  color: #909399;
  cursor: pointer;
}

.chart-card {
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.chart-card:hover {
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  width: 200px;
  margin-right: 50px;
  font-size: 18px;
  font-weight: 600;
}

.chart-container {
  height: 400px;
  width: 100%;
}

.loading-container, .empty-data {
  height: 400px;
  display: flex;
  justify-content: center;
  align-items: center;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .filter-container {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .chart-container {
    height: 300px;
  }
}
</style> 