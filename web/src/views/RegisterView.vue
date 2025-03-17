<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h2>用户注册</h2>
      </div>
      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" class="register-form">
        <el-form-item prop="username">
          <el-input v-model="registerForm.username" placeholder="用户名" prefix-icon="el-icon-user">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="密码" prefix-icon="el-icon-lock" show-password>
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" prefix-icon="el-icon-lock" show-password>
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="registerForm.email" placeholder="邮箱" prefix-icon="el-icon-message">
            <template #prefix>
              <el-icon><Message /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="register-button" @click="handleRegister">注册</el-button>
        </el-form-item>
        <div class="register-options">
          <router-link to="/login">已有账号？立即登录</router-link>
        </div>
      </el-form>
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
        callback(new Error('请再次输入密码'))
      } else if (value !== registerForm.password) {
        callback(new Error('两次输入密码不一致'))
      } else {
        callback()
      }
    }

    const registerRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请再次输入密码', trigger: 'blur' },
        { validator: validatePassword, trigger: 'blur' }
      ],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
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
            ElMessage.success('注册成功，请登录')
            // 跳转到登录页
            router.push('/login')
          }).catch(error => {
            ElMessage.error(error.message || '注册失败')
          }).finally(() => {
            loading.value = false
          })
        }
      })
    }

    return {
      registerForm,
      registerRules,
      registerFormRef,
      loading,
      handleRegister
    }
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}

.register-box {
  width: 400px;
  padding: 30px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-header h2 {
  font-weight: 500;
  color: #303133;
}

.register-form {
  margin-top: 20px;
}

.register-button {
  width: 100%;
}

.register-options {
  margin-top: 15px;
  text-align: right;
  font-size: 14px;
}

.register-options a {
  color: #409eff;
  text-decoration: none;
}
</style> 