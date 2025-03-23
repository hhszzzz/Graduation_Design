import { createStore } from 'vuex'
import { login, register, getUserInfo } from '@/api/auth'

const store = createStore({
  state: {
    token: '',  // 初始为空，将在created中检查
    user: null,
    isAuthenticated: false
  },
  getters: {
    // 获取用户信息
    getUser: state => state.user,
    // 判断是否已登录
    isAuthenticated: state => state.isAuthenticated,
    // 添加isLoggedIn getter，确保与UserProfileView中使用的名称一致
    isLoggedIn: state => state.isAuthenticated && !!state.token
  },
  mutations: {
    // 设置token
    SET_TOKEN(state, token) {
      // 验证token是否有效
      if (!token || typeof token !== 'string') {
        console.error('无效的token:', token);
        token = '';
      }
      
      // 如果token格式不正确，或者包含非ASCII字符，清空token
      if (token && !/^[a-zA-Z0-9\-_.]+$/.test(token.replace('Bearer ', ''))) {
        console.error('Token格式不正确，可能已损坏:', token);
        token = '';
      }
      
      state.token = token;
      state.isAuthenticated = !!token;
      
      // 持久化token到localStorage
      if (token) {
        localStorage.setItem('token', token);
        console.log('token已保存:', token);
      } else {
        localStorage.removeItem('token');
        console.log('token已清除');
      }
    },
    // 初始化token
    INIT_TOKEN(state) {
      const storedToken = localStorage.getItem('token');
      
      // 检查token是否有效
      if (storedToken && typeof storedToken === 'string') {
        // 检查token格式
        if (/^[a-zA-Z0-9\-_.]+$/.test(storedToken.replace('Bearer ', ''))) {
          state.token = storedToken;
          state.isAuthenticated = true;
          console.log('从localStorage恢复token:', storedToken);
        } else {
          console.error('localStorage中token格式不正确，可能已损坏:', storedToken);
          localStorage.removeItem('token');
          state.token = '';
          state.isAuthenticated = false;
        }
      } else {
        // 无效或不存在的token
        localStorage.removeItem('token');
        state.token = '';
        state.isAuthenticated = false;
      }
    },
    // 设置用户信息
    SET_USER(state, user) {
      state.user = user
    },
    // 清除用户身份信息
    CLEAR_AUTH(state) {
      state.token = ''
      state.user = null
      state.isAuthenticated = false
      localStorage.removeItem('token')
    }
  },
  actions: {
    // 登录
    async login({ commit }, loginData) {
      try {
        const res = await login(loginData)
        // 确保token格式正确
        let token = res.data.token
        // 如果后端返回的token没有Bearer前缀，手动添加
        if (res.data.tokenHead && !token.startsWith(res.data.tokenHead)) {
          token = res.data.tokenHead + ' ' + token
        }
        commit('SET_TOKEN', token)
        return res
      } catch (error) {
        console.error('登录失败', error)
        throw error
      }
    },
    // 注册
    async register(_, registerData) {
      try {
        return await register(registerData)
      } catch (error) {
        console.error('注册失败', error)
        throw error
      }
    },
    // 获取用户信息
    async getUserInfo({ commit }) {
      try {
        const res = await getUserInfo()
        commit('SET_USER', res.data)
        return res.data
      } catch (error) {
        console.error('获取用户信息失败', error)
        // 如果获取用户信息失败，可能是token已经失效
        commit('CLEAR_AUTH')
        throw error
      }
    },
    // 登出
    logout({ commit }) {
      commit('CLEAR_AUTH')
    }
  },
  modules: {
  }
})

// 创建后立即检查token
store.commit('INIT_TOKEN')

export default store
