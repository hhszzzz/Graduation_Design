<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-illustration">
        <img src="../assets/register-illustration.svg" alt="Register illustration" />
      </div>
      <div class="register-form-container">
        <h1 class="welcome-text">Create Account</h1>
        
        <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" class="register-form">
          <div class="form-group">
            <label>Username</label>
            <el-form-item prop="username">
              <el-input 
                v-model="registerForm.username" 
                placeholder="Choose a username"
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </div>
          
          <div class="form-group">
            <label>Email Address</label>
            <el-form-item prop="email">
              <el-input 
                v-model="registerForm.email" 
                placeholder="your@email.com"
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
                v-model="registerForm.password" 
                type="password" 
                placeholder="Create a password" 
                show-password
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </div>
          
          <div class="form-group">
            <label>Confirm Password</label>
            <el-form-item prop="confirmPassword">
              <el-input 
                v-model="registerForm.confirmPassword" 
                type="password" 
                placeholder="Confirm your password" 
                show-password
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </div>
          
          <div class="action-buttons">
            <el-button type="primary" :loading="loading" class="register-button" @click="handleRegister">CREATE ACCOUNT</el-button>
            <el-button class="login-button" @click="goToLogin">Back to Login</el-button>
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
import { User, Lock, Message } from '@element-plus/icons-vue'
import store from '@/store'

export default {
  name: 'RegisterView',
  components: {
    User,
    Lock,
    Message
  },
  setup() {
    const router = useRouter()
    const registerFormRef = ref(null)
    const loading = ref(false)

    const registerForm = reactive({
      username: '',
      password: '',
      confirmPassword: '',
      email: ''
    })

    // 校验两次密码是否一致
    const validatePassword = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('Please confirm your password'))
      } else if (value !== registerForm.password) {
        callback(new Error('The two passwords do not match'))
      } else {
        callback()
      }
    }

    const registerRules = {
      username: [
        { required: true, message: 'Please enter a username', trigger: 'blur' },
        { min: 3, max: 20, message: 'Length should be 3 to 20 characters', trigger: 'blur' }
      ],
      password: [
        { required: true, message: 'Please enter a password', trigger: 'blur' },
        { min: 6, max: 20, message: 'Length should be 6 to 20 characters', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: 'Please confirm your password', trigger: 'blur' },
        { validator: validatePassword, trigger: 'blur' }
      ],
      email: [
        { required: true, message: 'Please enter your email', trigger: 'blur' },
        { type: 'email', message: 'Please enter a valid email address', trigger: ['blur', 'change'] }
      ]
    }

    // 注册方法
    const handleRegister = () => {
      if (!registerFormRef.value) return
      
      registerFormRef.value.validate(valid => {
        if (valid) {
          loading.value = true
          // 创建注册数据对象，移除确认密码字段（后端不需要）
          const registerData = {
            username: registerForm.username,
            password: registerForm.password,
            email: registerForm.email
          }
          
          store.dispatch('register', registerData).then(() => {
            ElMessage.success('Registration successful. Please log in.')
            // 跳转到登录页
            router.push('/login')
          }).catch(error => {
            ElMessage.error(error.message || 'Registration failed')
          }).finally(() => {
            loading.value = false
          })
        }
      })
    }
    
    // 跳转到登录页
    const goToLogin = () => {
      router.push('/login')
    }

    return {
      registerForm,
      registerRules,
      registerFormRef,
      loading,
      handleRegister,
      goToLogin
    }
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #e6f7ff;
  background: linear-gradient(135deg, #e6f7ff 0%, #cce9ff 100%);
  padding: 20px;
}

.register-card {
  display: flex;
  width: 1000px;
  height: 600px;
  background-color: white;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.register-illustration {
  flex: 1;
  background-color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.register-illustration img {
  max-width: 100%;
  max-height: 100%;
}

.register-form-container {
  flex: 1;
  padding: 40px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
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
  box-shadow: 0 0 0 1px #409EFF inset;
}

.action-buttons {
  display: flex;
  gap: 15px;
  margin: 20px 0 30px;
}

.register-button {
  flex: 1;
  height: 45px;
  border-radius: 8px;
  background-color: #409EFF;
  font-weight: 600;
  font-size: 16px;
  border: none;
}

.register-button:hover {
  background-color: #337ab7;
}

.login-button {
  flex: 1;
  height: 45px;
  border-radius: 8px;
  background-color: white;
  border: 1px solid #e1e1e1;
  color: #333;
  font-weight: 600;
  font-size: 16px;
}

.login-button:hover {
  border-color: #409EFF;
  color: #409EFF;
}

/* 响应式调整 */
@media (max-width: 992px) {
  .register-card {
    flex-direction: column;
    height: auto;
    width: 100%;
    max-width: 500px;
  }
  
  .register-illustration {
    padding: 30px;
    max-height: 250px;
  }
  
  .register-form-container {
    padding: 30px;
  }
}

@media (max-width: 576px) {
  .action-buttons {
    flex-direction: column;
  }
  
  .register-form-container {
    padding: 20px;
  }
  
  .welcome-text {
    font-size: 24px;
    margin-bottom: 20px;
  }
}
</style> 