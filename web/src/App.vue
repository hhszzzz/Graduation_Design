<template>
  <div id="app">
    <el-container v-if="$store.getters.isAuthenticated">
      <el-header>
        <el-menu mode="horizontal" :router="true" class="nav-menu" :ellipsis="false">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/about">关于</el-menu-item>
          <div class="flex-grow"></div>
          <!-- User profile dropdown -->
          <div class="user-dropdown" v-if="user">
            <el-dropdown trigger="click">
              <div class="user-dropdown-link">
                <el-avatar :size="32" icon="el-icon-user">{{ user.username.charAt(0) }}</el-avatar>
                <span class="username">{{ user.username }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>
                    <span>{{ user.email }}</span>
                  </el-dropdown-item>
                  <el-dropdown-item divided>
                    <router-link to="/profile" class="dropdown-link">个人主页</router-link>
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
                    <span>退出登录</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-menu>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
    <router-view v-else />
  </div>
</template>

<script>
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { onMounted, ref, computed } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'

export default {
  name: 'App',
  components: {
    ArrowDown
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    const isLoading = ref(false)
    const user = computed(() => store.getters.getUser)

    // 在组件挂载时获取用户信息
    onMounted(async () => {
      const token = localStorage.getItem('token')
      
      // 如果localStorage中有token但Vuex中没有用户信息，则尝试获取用户信息
      if (token && !store.getters.getUser) {
        // 验证token格式
        if (!token.startsWith('Bearer ') || !token.includes('.')) {
          // token格式不正确，直接清除并跳转登录页
          store.dispatch('logout')
          router.push('/login')
          return
        }
        
        const loading = ElLoading.service({
          lock: true,
          text: '加载中...',
          background: 'rgba(0, 0, 0, 0.7)'
        })
        
        isLoading.value = true
        
        try {
          await store.dispatch('getUserInfo')
        } catch (error) {
          console.error('获取用户信息失败', error)
          // 清除token并跳转到登录页
          store.dispatch('logout')
          router.push('/login')
          ElMessage.error('登录已过期，请重新登录')
        } finally {
          loading.close()
          isLoading.value = false
        }
      }
    })

    // 退出登录
    const handleLogout = () => {
      store.dispatch('logout')
      router.push('/login')
      ElMessage.success('已退出登录')
    }

    return {
      user,
      isLoading,
      handleLogout
    }
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100vh;
}

.el-container {
  height: 100%;
}

.el-header {
  padding: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12);
  z-index: 1;
}

.nav-menu {
  display: flex;
  justify-content: flex-start;
  border-bottom: none;
}

.flex-grow {
  flex-grow: 1;
}

.el-main {
  background-color: #f5f7fa;
  padding: 20px;
}

/* User dropdown styles */
.user-dropdown {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 15px;
}

.user-dropdown-link {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 20px;
  transition: all 0.3s;
  background-color: #f5f7fa;
}

.user-dropdown-link:hover {
  background-color: #ecf5ff;
}

.username {
  margin: 0 8px;
  font-weight: 500;
  color: #303133;
}

.dropdown-link {
  text-decoration: none;
  color: #303133;
  display: block;
  width: 100%;
}
</style>
