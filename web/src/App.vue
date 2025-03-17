<template>
  <div id="app">
    <el-container v-if="$store.getters.isAuthenticated">
      <el-header>
        <el-menu mode="horizontal" :router="true" class="nav-menu">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/about">关于</el-menu-item>
          <div class="flex-grow"></div>
          <el-menu-item @click="handleLogout">退出登录</el-menu-item>
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
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'App',
  setup() {
    const store = useStore()
    const router = useRouter()

    // 在组件挂载时获取用户信息
    onMounted(() => {
      if (store.getters.isAuthenticated && !store.getters.getUser) {
        store.dispatch('getUserInfo').catch(() => {
          ElMessage.error('登录已过期，请重新登录')
          router.push('/login')
        })
      }
    })

    // 退出登录
    const handleLogout = () => {
      store.dispatch('logout')
      router.push('/login')
      ElMessage.success('已退出登录')
    }

    return {
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
</style>
