<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-illustration">
        <img src="../assets/login-illustration.svg" alt="Login illustration" />
      </div>
      <div class="login-form-container">
        <h1 class="welcome-text">Welcome Back :)</h1>
        
        <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" class="login-form">
          <div class="form-group">
            <label>Username</label>
            <el-form-item prop="username">
              <el-input 
                v-model="loginForm.username" 
                placeholder="Enter your username"
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><Message /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </div>
          
          <div class="form-group">
            <label>Password</label>
            <el-form-item prop="password">
              <el-input 
                v-model="loginForm.password" 
                type="password" 
                placeholder="Enter your password" 
                show-password
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </div>
          
          <div class="form-options">
            <el-checkbox v-model="rememberMe">Remember me</el-checkbox>
            <a href="#" class="forgot-password">Forgot password?</a>
          </div>
          
          <div class="action-buttons">
            <el-button type="primary" :loading="loading" class="login-button" @click="handleLogin">LOGIN</el-button>
            <el-button class="register-button" @click="goToRegister">Create Account</el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message } from '@element-plus/icons-vue'
import store from '@/store'

export default {
  name: 'LoginView',
  components: {
    Lock,
    Message
  },
  setup() {
    const router = useRouter()
    const loginFormRef = ref(null)
    const loading = ref(false)
    const rememberMe = ref(false)

    const loginForm = reactive({
      username: '',
      password: ''
    })

    const loginRules = {
      username: [
        { required: true, message: 'Please enter your username', trigger: 'blur' }
      ],
      password: [
        { required: true, message: 'Please enter your password', trigger: 'blur' }
      ]
    }

    // 登录方法
    const handleLogin = () => {
      if (!loginFormRef.value) return
      
      loginFormRef.value.validate(valid => {
        if (valid) {
          loading.value = true
          
          store.dispatch('login', loginForm)
            .then(() => {
              // 登录成功后获取用户信息
              return store.dispatch('getUserInfo')
            })
            .then(() => {
              ElMessage.success('Login successful!')
              // 登录成功后强制刷新页面，解决登录后状态不正确的问题
              window.location.href = '/'
            })
            .catch(error => {
              ElMessage.error(error.message || 'Login failed')
            })
            .finally(() => {
              loading.value = false
            })
        }
      })
    }
    
    // 跳转到注册页
    const goToRegister = () => {
      router.push('/register')
    }

    return {
      loginForm,
      loginRules,
      loginFormRef,
      loading,
      rememberMe,
      handleLogin,
      goToRegister
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #e6f7ff;
  background: linear-gradient(135deg, #e6f7ff 0%, #cce9ff 100%);
  padding: 20px;
}

.login-card {
  display: flex;
  width: 1000px;
  height: 600px;
  background-color: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.login-illustration {
  flex: 1;
  background-color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-illustration img {
  max-width: 100%;
  max-height: 100%;
}

.login-form-container {
  flex: 1;
  padding: 40px;
  display: flex;
  flex-direction: column;
}

.welcome-text {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 30px;
  color: #333;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
}

.custom-input :deep(.el-input__wrapper) {
  border-radius: 8px !important;
  padding: 12px 15px;
  box-shadow: 0 0 0 1px #e1e1e1 inset;
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #3498db inset;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.forgot-password {
  color: #3498db;
  text-decoration: none;
  font-size: 14px;
}

.forgot-password:hover {
  text-decoration: underline;
}

.action-buttons {
  display: flex;
  gap: 15px;
  margin-bottom: 30px;
}

.login-button {
  flex: 1;
  height: 45px;
  border-radius: 8px;
  background-color: #409EFF;
  font-weight: 600;
  font-size: 16px;
  border: none;
}

.login-button:hover {
  background-color: #337ab7;
}

.register-button {
  flex: 1;
  height: 45px;
  border-radius: 8px;
  background-color: white;
  border: 1px solid #e1e1e1;
  color: #333;
  font-weight: 600;
  font-size: 16px;
}

.register-button:hover {
  border-color: #409EFF;
  color: #409EFF;
}

/* 响应式调整 */
@media (max-width: 992px) {
  .login-card {
    flex-direction: column;
    height: auto;
    width: 100%;
    max-width: 500px;
  }
  
  .login-illustration {
    padding: 30px;
    max-height: 250px;
  }
  
  .login-form-container {
    padding: 30px;
  }
}

@media (max-width: 576px) {
  .action-buttons {
    flex-direction: column;
  }
  
  .login-form-container {
    padding: 20px;
  }
  
  .welcome-text {
    font-size: 24px;
    margin-bottom: 20px;
  }
  
  .form-options {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style> 