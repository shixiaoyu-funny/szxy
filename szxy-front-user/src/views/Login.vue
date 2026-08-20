<template>
  <div class="login-container">
    <div class="login-bg-sky" aria-hidden="true" />
    <div class="login-card">
      <h1 class="login-title">数智乡约</h1>
      <p class="login-subtitle">AI 驱动乡村振兴服务平台 - 用户端</p>

      <div class="login-tabs">
        <button v-for="tab in tabs" :key="tab.value" :class="['tab-btn', { active: activeTab === tab.value }]"
          @click="activeTab = tab.value">
          {{ tab.label }}
        </button>
      </div>

      <div class="login-form">
        <!-- 手机号登录 -->
        <div v-if="activeTab === 'phone'" class="form-tab">
          <div class="form-group">
            <input type="tel" v-model="phoneForm.phone" class="input" placeholder="请输入手机号" maxlength="11" @keyup.enter="sendPhoneCode" />
          </div>
          <div class="form-group code-group">
            <input type="text" v-model="phoneForm.code" class="input code-input" placeholder="请输入验证码" maxlength="6" @keyup.enter="phoneLogin" />
            <button class="btn btn-secondary code-btn" :disabled="countdown > 0" @click="sendPhoneCode">
              {{ countdown > 0 ? `${countdown}s后重发` : '获取验证码' }}
            </button>
          </div>
          <button class="btn btn-primary login-btn" @click="phoneLogin" :disabled="loading">
            {{ loading ? '登录中...' : '登录/注册' }}
          </button>
        </div>

        <!-- 邮箱登录 -->
        <div v-else-if="activeTab === 'email'" class="form-tab">
          <div class="form-group">
            <input type="email" v-model="emailForm.email" class="input" placeholder="请输入邮箱" @keyup.enter="sendEmailCode" />
          </div>
          <div class="form-group code-group">
            <input type="text" v-model="emailForm.code" class="input code-input" placeholder="请输入验证码" maxlength="6" @keyup.enter="sendEmailCode" />
            <button class="btn btn-secondary code-btn" :disabled="countdown > 0" @click="sendEmailCode">
              {{ countdown > 0 ? `${countdown}s后重发` : '获取验证码' }}
            </button>
          </div>
          <button class="btn btn-primary login-btn" @click="emailLogin" :disabled="loading">
            {{ loading ? '登录中...' : '登录/注册' }}
          </button>
        </div>

        <!-- 账密登录 -->
        <div v-else-if="activeTab === 'password'" class="form-tab">
          <div class="form-group">
            <input type="text" v-model="pwForm.username" class="input" placeholder="请输入用户名" @keyup.enter="pwLogin" />
          </div>
          <div class="form-group">
            <input type="password" v-model="pwForm.password" class="input" placeholder="请输入密码" @keyup.enter="pwLogin" />
          </div>
          <button class="btn btn-primary login-btn" @click="pwLogin" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </div>

        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';
import { getUserInfo } from '../api/user';
import { sendCode, phoneLogin as apiPhoneLogin, emailLogin as apiEmailLogin, pwLogin as apiPwLogin } from '../api/login';

const router = useRouter();
const userStore = useUserStore();

const activeTab = ref('phone');
const loading = ref(false);
const errorMessage = ref('');
const countdown = ref(0);

const tabs = [
  { label: '手机号', value: 'phone' },
  { label: '邮箱', value: 'email' },
  { label: '账密', value: 'password' }
];

const phoneForm = ref({
  phone: '',
  code: ''
});

const emailForm = ref({
  email: '',
  code: ''
});

const pwForm = ref({
  username: '',
  password: ''
});

const startCountdown = () => {
  countdown.value = 60;
  const timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer);
    }
  }, 1000);
};

const sendPhoneCode = async () => {
  if (!phoneForm.value.phone) {
    errorMessage.value = '请输入手机号';
    return;
  }

  try {
    await sendCode(phoneForm.value.phone);
    errorMessage.value = '';
    startCountdown();
  } catch (error) {
    errorMessage.value = '发送验证码失败，请重试';
  }
};

const sendEmailCode = async () => {
  if (!emailForm.value.email) {
    errorMessage.value = '请输入邮箱';
    return;
  }

  try {
    await sendCode(emailForm.value.email);
    errorMessage.value = '';
    startCountdown();
  } catch (error) {
    errorMessage.value = '发送验证码失败，请重试';
  }
};

const phoneLogin = async () => {
  if (!phoneForm.value.phone || !phoneForm.value.code) {
    errorMessage.value = '请填写完整信息';
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const res = await apiPhoneLogin(phoneForm.value.phone, phoneForm.value.code);
    userStore.setToken(res.data);
    // 获取用户信息并校验权限
    const userRes = await getUserInfo();
    const userInfo = userRes.data;
    userStore.setUserInfo(userInfo);

    // 校验角色：管理员请前往管理端，游客/农户/村长可用本端
    if (userInfo.role === 4) {
      errorMessage.value = '管理员请前往管理端登录';
      userStore.logout();
      loading.value = false;
      return;
    }
    router.push('/');
  } catch (error) {
    errorMessage.value = '登录失败，请重试';
  } finally {
    loading.value = false;
  }
};

const emailLogin = async () => {
  if (!emailForm.value.email || !emailForm.value.code) {
    errorMessage.value = '请填写完整信息';
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const res = await apiEmailLogin(emailForm.value.email, emailForm.value.code);
    userStore.setToken(res.data);
    // 获取用户信息并校验权限
    const userRes = await getUserInfo();
    const userInfo = userRes.data;
    userStore.setUserInfo(userInfo);

    // 校验角色：管理员请前往管理端，游客/农户/村长可用本端
    if (userInfo.role === 4) {
      errorMessage.value = '管理员请前往管理端登录';
      userStore.logout();
      loading.value = false;
      return;
    }
    router.push('/');
  } catch (error) {
    errorMessage.value = '登录失败，请重试';
  } finally {
    loading.value = false;
  }
};

const pwLogin = async () => {
  if (!pwForm.value.username || !pwForm.value.password) {
    errorMessage.value = '请填写完整信息';
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const res = await apiPwLogin(pwForm.value.username, pwForm.value.password);
    userStore.setToken(res.data);
    // 获取用户信息并校验权限
    const userRes = await getUserInfo();
    const userInfo = userRes.data;
    userStore.setUserInfo(userInfo);

    // 校验角色：管理员请前往管理端，游客/农户/村长可用本端
    if (userInfo.role === 4) {
      errorMessage.value = '管理员请前往管理端登录';
      userStore.logout();
      loading.value = false;
      return;
    }
    router.push('/');
  } catch (error) {
    errorMessage.value = '登录失败，请重试';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 20px;
  overflow: hidden;
  background: linear-gradient(155deg, #e8f5e9 0%, #fafff9 42%, #c8e6c9 100%);
}

/* 绿意流动质感 + 右上→左下流动（与管理端蓝天白云同款结构） */
.login-bg-sky {
  position: absolute;
  inset: -55% -35% -45% -35%;
  pointer-events: none;
  z-index: 0;
  background:
    radial-gradient(ellipse 90% 58% at 90% 5%, rgba(255, 255, 255, 1) 0%, rgba(255, 255, 255, 0) 52%),
    radial-gradient(ellipse 80% 55% at 8% 95%, rgba(129, 199, 132, 0.55) 0%, rgba(255, 255, 255, 0) 58%),
    radial-gradient(ellipse 70% 45% at 50% 40%, rgba(200, 230, 201, 0.5) 0%, rgba(255, 255, 255, 0) 65%),
    linear-gradient(
      145deg,
      rgba(165, 214, 167, 0.92) 0%,
      rgba(255, 255, 255, 0.65) 38%,
      rgba(200, 230, 201, 0.88) 72%,
      rgba(139, 195, 74, 0.55) 100%
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

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(76, 175, 80, 0.12), 0 2px 0 rgba(255, 255, 255, 0.8) inset;
  border: 1px solid rgba(255, 255, 255, 0.9);
  padding: 32px;
  text-align: center;
  transition: box-shadow 0.35s ease, transform 0.35s ease;
}

.login-card:hover {
  box-shadow: 0 16px 48px rgba(76, 175, 80, 0.16), 0 2px 0 rgba(255, 255, 255, 0.85) inset;
  transform: translateY(-2px);
}

.login-title {
  font-size: 28px;
  font-weight: bold;
  color: #2E7D32;
  margin-bottom: 8px;
}

.login-subtitle {
  font-size: 16px;
  color: #666;
  margin-bottom: 32px;
}

.login-tabs {
  display: flex;
  margin-bottom: 24px;
  border-bottom: 1px solid #e0e0e0;
}

.tab-btn {
  flex: 1;
  padding: 12px;
  border: none;
  background: none;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  border-bottom: 2px solid transparent;
}

.tab-btn.active {
  color: #8BC34A;
  border-bottom-color: #8BC34A;
  font-weight: 500;
}

.form-group {
  margin-bottom: 20px;
  text-align: left;
}

.code-group {
  display: flex;
  gap: 12px;
}

.code-input {
  flex: 1;
}

.code-btn {
  white-space: nowrap;
  min-width: 120px;
}

.login-btn {
  width: 100%;
  margin-top: 8px;
  padding: 12px;
  font-size: 16px;
}

.error-message {
  margin-top: 16px;
  color: #f44336;
  font-size: 14px;
}

@media (max-width: 480px) {
  .login-card {
    padding: 24px;
  }

  .login-title {
    font-size: 24px;
  }

  .login-subtitle {
    font-size: 14px;
  }
}
</style>