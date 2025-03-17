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
      config.headers['Authorization'] = 'Bearer ' + token;
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
    // 如果响应成功
    if (res.code === 200) {
      return res;
    } else {
      // 处理错误
      return Promise.reject(new Error(res.message || 'Error'));
    }
  },
  error => {
    console.log('err' + error);
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