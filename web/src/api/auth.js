import axios from 'axios';

// 创建axios实例
const service = axios.create({
  baseURL: '',  // 移除baseURL前缀，直接使用完整路径
  timeout: 5000
});

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 如果存在token，请求携带token
    const token = localStorage.getItem('token');
    if (token) {
      try {
        // 检查token是否基本格式有效
        if (typeof token !== 'string' || token.trim() === '') {
          console.error('本地存储的token无效，已清除');
          localStorage.removeItem('token');
          return config;
        }
        
        // 检查token格式
        const tokenValue = token.replace('Bearer ', '');
        if (!/^[a-zA-Z0-9\-_.]+$/.test(tokenValue)) {
          console.error('本地存储的token格式不正确，已清除');
          localStorage.removeItem('token');
          return config;
        }
        
        // 检查token是否已包含Bearer前缀
        if (token.startsWith('Bearer ')) {
          config.headers['Authorization'] = token;
        } else {
          config.headers['Authorization'] = 'Bearer ' + token;
        }
        
        console.log('请求头中的认证信息:', config.headers['Authorization']);
      } catch (e) {
        console.error('处理token时发生错误:', e);
        localStorage.removeItem('token');
      }
    }
    return config;
  },
  error => {
    console.log(error);
    return Promise.reject(error);
  }
);

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data;
    // 处理不同的响应格式
    if (res === null || res === undefined) {
      return Promise.reject(new Error('响应数据为空'));
    }
    
    // 如果是数组类型，直接返回
    if (Array.isArray(res)) {
      return { data: res, code: 200 };
    }
    
    // 如果是对象类型且有code字段
    if (typeof res === 'object' && res.code !== undefined) {
      if (res.code === 200) {
        return res;
      } else {
        return Promise.reject(new Error(res.message || '请求失败'));
      }
    }
    
    // 其他情况直接返回数据
    return { data: res, code: 200 };
  },
  error => {
    console.log('err', error);
    // 如果是401错误，可能是token过期
    if (error.response && error.response.status === 401) {
      // 清除本地token和登录状态
      localStorage.removeItem('token');
      // 如果使用了store，也可以清除store中的状态
      if (window.$store) {
        window.$store.commit('CLEAR_AUTH');
      }
    }
    return Promise.reject(error);
  }
);

// 登录方法
export function login(data) {
  return service({
    url: '/api/auth/login',
    method: 'post',
    data
  });
}

// 注册方法
export function register(data) {
  return service({
    url: '/api/auth/register',
    method: 'post',
    data
  });
}

// 获取用户信息
export function getUserInfo() {
  return service({
    url: '/api/auth/info',
    method: 'get'
  });
}

export default service; 