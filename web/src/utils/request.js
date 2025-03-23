import axios from 'axios'
import { ElMessage } from 'element-plus'
import store from '@/store'
import router from '@/router'

// 创建axios实例
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || '', // url基础路径
  timeout: 30000, // 请求超时时间
  withCredentials: true // 跨域请求时发送cookie
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 在发送请求之前做一些处理
    if (store.state.token) {
      // 检查并确保token格式正确
      let token = store.state.token;
      
      // 如果token包含Bearer前缀，直接使用
      if (token.startsWith('Bearer ')) {
        config.headers['Authorization'] = token;
      } else {
        // 如果token不包含Bearer前缀，添加前缀
        config.headers['Authorization'] = 'Bearer ' + token;
      }
      
      // 记录发送的token用于调试
      console.log('发送请求使用的token:', config.headers['Authorization']);
    }
    return config
  },
  error => {
    // 处理请求错误
    console.log(error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    // 检查响应的Content-Type
    const contentType = response.headers['content-type'] || ''
    
    // 如果是HTML或纯文本内容，直接返回数据
    if (contentType.includes('text/html') || 
        contentType.includes('text/plain') ||
        typeof response.data === 'string' && 
        (response.data.includes('<html') || response.data.includes('<!DOCTYPE'))) {
      console.log('检测到HTML内容响应，直接返回数据，长度:', response.data.length)
      return response.data
    }
    
    const res = response.data
    
    // 检查响应是否有code字段
    if (res && res.code === undefined) {
      console.log('响应没有code字段，尝试处理:', typeof res, res ? Object.keys(res).join(',') : '无值')
      
      // 如果响应是对象且没有code字段，可能是直接返回的数据
      if (typeof res === 'object' && res !== null) {
        return res
      }
      
      // 如果响应是字符串，可能是HTML内容
      if (typeof res === 'string' && res.length > 0) {
        return res
      }
    }
    
    // 标准响应处理
    if (res.code !== 200) {
      // 401: 未登录或token过期，不显示错误消息，只处理登录状态
      if (res.code === 401) {
        // 重新登录
        store.dispatch('logout').then(() => {
          router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
        })
      } else {
        // 非401错误才显示错误消息
        ElMessage({
          message: res.message || '系统错误',
          type: 'error',
          duration: 5 * 1000
        })
      }
      
      return Promise.reject(new Error(res.message || '系统错误'))
    } else {
      return res
    }
  },
  error => {
    console.log('请求错误:', error)
    
    // 如果错误对象包含响应
    if (error.response) {
      // 检查Content-Type是否为HTML
      const contentType = error.response.headers && error.response.headers['content-type'] || ''
      
      // 如果是HTML内容或较长字符串，可能是有用内容
      if ((contentType.includes('text/html') || contentType.includes('text/plain')) && 
          typeof error.response.data === 'string' && error.response.data.length > 200) {
        console.log('错误响应中检测到HTML内容，长度:', error.response.data.length)
        return error.response.data // 直接返回HTML内容
      }
      
      // 处理标准HTTP错误码
      if (error.response.status !== 401) {
        ElMessage({
          message: error.message || `请求失败 (${error.response.status})`,
          type: 'error',
          duration: 5 * 1000
        })
      }
    } else if (!error.response) {
      // 没有响应对象的错误（如网络错误）
      ElMessage({
        message: '网络错误，请检查您的网络连接',
        type: 'error',
        duration: 5 * 1000
      })
    }
    
    return Promise.reject(error)
  }
)

export default service 