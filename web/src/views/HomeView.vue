<template>
  <div class="home">
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
  </div>
</template>

<script>
import { computed } from 'vue'
import { useStore } from 'vuex'

export default {
  name: 'HomeView',
  setup() {
    const store = useStore()
    const user = computed(() => store.getters.getUser)

    return {
      user
    }
  }
}
</script>

<style scoped>
.home {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.welcome-card {
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
</style>
