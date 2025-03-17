import { createStore } from 'vuex'
import { login, register, getUserInfo } from '@/api/auth'

export default createStore({
  state: {
    token: localStorage.getItem('token') || '',
    user: null,
    isAuthenticated: !!localStorage.getItem('token')
  },
  getters: {
    // 获取用户信息
    getUser: state => state.user,
    // 判断是否已登录
    isAuthenticated: state => state.isAuthenticated
  },
  mutations: {
    // 设置token
    SET_TOKEN(state, token) {
      state.token = token
      state.isAuthenticated = !!token
      // 持久化token到localStorage
      if (token) {
        localStorage.setItem('token', token)
      } else {
        localStorage.removeItem('token')
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
        commit('SET_TOKEN', res.data.token)
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
