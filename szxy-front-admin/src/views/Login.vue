<template>
  <div class="login-container">
    <div class="login-bg-sky" aria-hidden="true" />
    <div class="login-form-wrapper">
      <div class="login-header">
        <h1 class="logo">数智乡约</h1>
        <p class="sub-title">AI 驱动乡村振兴服务平台 - 管理端</p>
      </div>
      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="账密登录" name="pwLogin">
          <el-form :model="pwLoginForm" :rules="pwLoginRules" ref="pwLoginRef" class="login-form" label-width="88px">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="pwLoginForm.username" placeholder="请输入用户名" prefix-icon="User" class="login-input" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="pwLoginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password class="login-input" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="login-button" @click="handlePwLogin" :loading="loading">
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="手机号登录" name="phoneLogin">
          <el-form :model="phoneLoginForm" :rules="phoneLoginRules" ref="phoneLoginRef" class="login-form" label-width="88px">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="phoneLoginForm.phone" placeholder="请输入手机号" prefix-icon="Phone" class="login-input" />
            </el-form-item>
            <el-form-item label="验证码" prop="code">
              <el-input v-model="phoneLoginForm.code" placeholder="请输入验证码" prefix-icon="Message" class="login-input login-input-append">
                <template #append>
                  <el-button @click="sendCode('phone')" :disabled="countdown > 0">
                    {{ countdown > 0 ? `${countdown}s后重新发送` : '发送验证码' }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="login-button" @click="handlePhoneLogin" :loading="loading">
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="邮箱登录" name="emailLogin">
          <el-form :model="emailLoginForm" :rules="emailLoginRules" ref="emailLoginRef" class="login-form" label-width="88px">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="emailLoginForm.email" placeholder="请输入邮箱" prefix-icon="Message" class="login-input" />
            </el-form-item>
            <el-form-item label="验证码" prop="code">
              <el-input v-model="emailLoginForm.code" placeholder="请输入验证码" prefix-icon="Message" class="login-input login-input-append">
                <template #append>
                  <el-button @click="sendCode('email')" :disabled="countdown > 0">
                    {{ countdown > 0 ? `${countdown}s后重新发送` : '发送验证码' }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="login-button" @click="handleEmailLogin" :loading="loading">
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { User, Lock, Phone, Message } from '@element-plus/icons-vue';
import { loginApi, userApi } from '../api';
import store from '../store';
import { ElMessage } from 'element-plus';

const router = useRouter();
const route = useRoute();
const activeTab = ref('pwLogin');
const loading = ref(false);
const countdown = ref(0);

// 账密登录表单
const pwLoginForm = reactive({
  username: '',
  password: ''
});

const pwLoginRules = reactive({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
});

// 手机号登录表单
const phoneLoginForm = reactive({
  phone: '',
  code: ''
});

const phoneLoginRules = reactive({
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
});

// 邮箱登录表单
const emailLoginForm = reactive({
  email: '',
  code: ''
});

const emailLoginRules = reactive({
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
});

// 发送验证码
const sendCode = async (type: 'phone' | 'email') => {
  const content = type === 'phone' ? phoneLoginForm.phone : emailLoginForm.email;
  if (!content) {
    return;
  }
  
  try {
    await loginApi.sendCode(content);
    // 开始倒计时
    countdown.value = 60;
    const timer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) {
        clearInterval(timer);
      }
    }, 1000);
  } catch (error) {
    console.error('发送验证码失败:', error);
  }
};

// 账密登录
const handlePwLogin = async () => {
  loading.value = true;
  try {
    const token = await loginApi.pwLogin(pwLoginForm.username, pwLoginForm.password);
    store.actions.login(token);
    try {
      const info = await userApi.getInfo();
      store.actions.updateUserInfo(info);
      // 仅管理员（role=4）可进入管理端
      if (info?.role !== 4) {
        ElMessage.error('仅管理员可登录管理端');
        store.actions.logout();
        loading.value = false;
        return;
      }
    } catch {
      /* 非致命 */
    }
    // 根据 redirect 参数跳转到用户原本想去的页面
    const redirect = route.query.redirect as string;
    router.push(redirect || '/');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败');
  } finally {
    loading.value = false;
  }
};

// 手机号登录
const handlePhoneLogin = async () => {
  loading.value = true;
  try {
    const token = await loginApi.phoneLogin(phoneLoginForm.phone, phoneLoginForm.code);
    store.actions.login(token);
    try {
      const info = await userApi.getInfo();
      store.actions.updateUserInfo(info);
      // 仅管理员（role=4）可进入管理端
      if (info?.role !== 4) {
        ElMessage.error('仅管理员可登录管理端');
        store.actions.logout();
        loading.value = false;
        return;
      }
    } catch {
      /* 非致命 */
    }
    // 根据 redirect 参数跳转到用户原本想去的页面
    const redirect = route.query.redirect as string;
    router.push(redirect || '/');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败');
  } finally {
    loading.value = false;
  }
};

// 邮箱登录
const handleEmailLogin = async () => {
  loading.value = true;
  try {
    const token = await loginApi.emailLogin(emailLoginForm.email, emailLoginForm.code);
    store.actions.login(token);
    try {
      const info = await userApi.getInfo();
      store.actions.updateUserInfo(info);
      // 仅管理员（role=4）可进入管理端
      if (info?.role !== 4) {
        ElMessage.error('仅管理员可登录管理端');
        store.actions.logout();
        loading.value = false;
        return;
      }
    } catch {
      /* 非致命 */
    }
    // 根据 redirect 参数跳转到用户原本想去的页面
    const redirect = route.query.redirect as string;
    router.push(redirect || '/');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  overflow: hidden;
  background: linear-gradient(155deg, #e3f2fd 0%, #fafdff 42%, #bbdefb 100%);
}

/* 蓝天白云质感 + 右上→左下流动（加大位移与层次，便于感知） */
.login-bg-sky {
  position: absolute;
  inset: -55% -35% -45% -35%;
  pointer-events: none;
  z-index: 0;
  background:
    radial-gradient(ellipse 90% 58% at 90% 5%, rgba(255, 255, 255, 1) 0%, rgba(255, 255, 255, 0) 52%),
    radial-gradient(ellipse 80% 55% at 8% 95%, rgba(100, 181, 246, 0.55) 0%, rgba(255, 255, 255, 0) 58%),
    radial-gradient(ellipse 70% 45% at 50% 40%, rgba(179, 229, 252, 0.45) 0%, rgba(255, 255, 255, 0) 65%),
    linear-gradient(
      145deg,
      rgba(187, 222, 251, 0.92) 0%,
      rgba(255, 255, 255, 0.65) 38%,
      rgba(227, 242, 253, 0.88) 72%,
      rgba(144, 202, 249, 0.55) 100%
    );
  background-size: 120% 120%;
  animation: skyFlow 22s ease-in-out infinite;
  will-change: transform;
}

@keyframes skyFlow {
  0% {
    transform: translate(14%, -12%) rotate(0deg) scale(1);
    background-position: 0% 0%;
  }
  50% {
    transform: translate(-18%, 16%) rotate(1.2deg) scale(1.09);
    background-position: 100% 100%;
  }
  100% {
    transform: translate(14%, -12%) rotate(0deg) scale(1);
    background-position: 0% 0%;
  }
}

.login-form-wrapper {
  position: relative;
  z-index: 1;
  width: 400px;
  max-width: calc(100vw - 32px);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(30, 136, 229, 0.12), 0 2px 0 rgba(255, 255, 255, 0.8) inset;
  border: 1px solid rgba(255, 255, 255, 0.9);
  padding: 30px;
  transition: box-shadow 0.35s ease, transform 0.35s ease;
}

.login-form-wrapper:hover {
  box-shadow: 0 16px 48px rgba(30, 136, 229, 0.16), 0 2px 0 rgba(255, 255, 255, 0.85) inset;
  transform: translateY(-2px);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo {
  font-size: 28px;
  font-weight: bold;
  color: #1565c0;
  margin: 0;
  margin-bottom: 10px;
}

.sub-title {
  font-size: 14px;
  color: #546e7a;
  margin: 0;
}

.login-tabs {
  margin-top: 20px;
}

.login-form {
  margin-top: 20px;
}

/* 统一表单项与输入框宽度，避免标签长短不一导致错位 */
.login-form :deep(.el-form-item__content) {
  flex: 1;
  min-width: 0;
}

.login-input {
  width: 100%;
}

.login-input-append {
  width: 100%;
}

.login-input-append :deep(.el-input__wrapper) {
  width: 100%;
}

.login-button {
  width: 100%;
  height: 40px;
  font-size: 16px;
  background: linear-gradient(135deg, #42a5f5 0%, #1e88e5 100%);
  border: none;
  transition: all 0.3s ease;
}

.login-button:hover {
  transform: scale(1.01);
  box-shadow: 0 6px 18px rgba(30, 136, 229, 0.35);
}

.login-button:loading {
  background: linear-gradient(135deg, #1976d2 0%, #1565c0 100%);
}

.login-tabs :deep(.el-tabs__item.is-active) {
  color: #1565c0;
}

.login-tabs :deep(.el-tabs__active-bar) {
  background-color: #42a5f5;
}
</style>