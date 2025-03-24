import request from '@/utils/request'

/**
 * 获取新闻详情
 * @param {Number} id 新闻ID
 * @param {String} type 新闻类型
 * @returns {Promise} 请求结果
 */
export function getNewsDetail(id, type) {
  return request({
    url: `/api/news/detail/${id}`,
    method: 'get',
    params: { type }
  }).then(response => {
    // console.log('getNewsDetail原始响应:', response);
    
    // 处理系统错误响应但包含数据的情况
    if (response && response.message === '系统错误' && response.code === undefined) {
      // console.log('新闻详情返回系统错误但尝试提取数据');
      // 尝试从response中提取数据
      if (response.data) {
        return {
          code: 200,
          message: 'OK',
          data: response.data
        };
      } else if (response.response && response.response.data) {
        return {
          code: 200,
          message: 'OK',
          data: response.response.data
        };
      } else if (response.id) {
        // 如果数据在根级别
        return {
          code: 200,
          message: 'OK',
          data: response
        };
      }
    }

    // 检查响应格式是否正确
    if (response && !response.code && response.data) {
      // 如果响应没有code字段但有data字段，手动构造正确格式的响应
      return {
        code: 200,
        message: 'OK',
        data: response.data || response
      };
    }
    return response;
  }).catch(error => {
    console.error('getNewsDetail捕获错误:', error);
    
    // 如果error包含有用数据，尝试提取
    if (error && error.data) {
      // console.log('从错误中提取数据:', error.data);
      return {
        code: 200,
        message: 'OK',
        data: error.data
      };
    }
    throw error;
  });
}

/**
 * 获取新闻内容
 * @param {String} url 新闻链接
 * @returns {Promise} 请求结果
 */
export function getNewsContent(url) {
  if (!url) {
    return Promise.reject(new Error('缺少URL参数'))
  }
  
  // console.log('获取文章内容，URL:', url)
  
  return request({
    url: '/api/news/crawl_content',
    method: 'post',
    data: { url }
  }).then(response => {
    // 如果是直接的文本/HTML内容，直接返回
    if (typeof response === 'string' && response.length > 0) {
      return response
    }
    
    // 正常响应格式
    if (response && response.data) {
      return response.data
    }
    
    // 数据直接在response
    if (response && (response.content || typeof response === 'object')) {
      return response
    }
    
    console.warn('未知的响应格式:', response)
    return response
  }).catch(error => {
    console.error('获取内容API错误:', error)
    
    // 尝试从错误对象中提取有用的数据
    if (error.response && error.response.data) {
      // 如果错误响应中包含HTML内容
      if (typeof error.response.data === 'string' && 
          (error.response.data.includes('<html') || error.response.data.length > 500)) {
        // console.log('从错误响应中提取有效内容')
        return error.response.data
      }
      
      // 如果是JSON对象，可能包含有用信息
      if (typeof error.response.data === 'object') {
        return error.response.data
      }
    }
    
    throw error
  })
} 