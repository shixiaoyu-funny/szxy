<template>
  <div class="profile-container">
    <header class="profile-header">
      <h1 class="header-title">个人中心</h1>
    </header>

    <div class="profile-content">
      <!-- 用户信息 -->
      <div class="user-info-card">
        <div class="user-avatar">
          <img :src="userInfo.avatar || defaultAvatar" :alt="userInfo.username" />
        </div>
        <h2 class="user-name">{{ userInfo.username || '未登录' }}</h2>
        <p class="user-email">{{ userInfo.email || userInfo.phone || '未设置' }}</p>
        <div class="user-meta">
          <span class="meta-item">
            <span class="meta-label">账号状态：</span>
            <span :class="['meta-value', userInfo.status === 1 ? 'status-normal' : 'status-disabled']">
              {{ userInfo.status === 1 ? '正常' : '禁用' }}
            </span>
          </span>
          <span class="meta-item">
            <span class="meta-label">角色：</span>
            <span class="meta-value">{{ getRoleText(userInfo.role) }}</span>
          </span>
        </div>
      </div>

      <!-- 功能列表 -->
      <div class="function-list">
        <div class="function-item" @click="editProfile">
          <span class="function-icon">✏️</span>
          <span class="function-text">编辑个人信息</span>
          <span class="function-arrow">→</span>
        </div>
        <div v-if="isFarmerOrChief" class="function-item" @click="myVillage">
          <span class="function-icon">🏘️</span>
          <span class="function-text">我的村</span>
          <span class="function-arrow">→</span>
        </div>
        <div v-if="isFarmerOrChief" class="function-item" @click="myScenics">
          <span class="function-icon">🗻</span>
          <span class="function-text">我的景点</span>
          <span class="function-arrow">→</span>
        </div>
        <div v-if="isChief" class="function-item" @click="villageFarmers">
          <span class="function-icon">👥</span>
          <span class="function-text">本村农户管理</span>
          <span class="function-arrow">→</span>
        </div>
        <div class="function-item" @click="viewCollections">
          <span class="function-icon">⭐</span>
          <span class="function-text">我的收藏</span>
          <span class="function-arrow">→</span>
        </div>
        <div class="function-item" @click="viewLikes">
          <span class="function-icon">👍</span>
          <span class="function-text">我的点赞</span>
          <span class="function-arrow">→</span>
        </div>
        <div class="function-item" @click="viewComments">
          <span class="function-icon">💬</span>
          <span class="function-text">我的评论</span>
          <span class="function-arrow">→</span>
        </div>
        <div class="function-item" @click="logout" v-if="userStore.isLoggedIn">
          <span class="function-icon">🚪</span>
          <span class="function-text" style="color: #f44336;">退出登录</span>
          <span class="function-arrow">→</span>
        </div>
        <div class="function-item" @click="goToLogin" v-else>
          <span class="function-icon">🔑</span>
          <span class="function-text" style="color: #8BC34A;">登录/注册</span>
          <span class="function-arrow">→</span>
        </div>
      </div>

      <!-- 关于我们 -->
      <div class="about-section">
        <h3 class="section-title">关于我们</h3>
        <div class="about-content">
          <p>数智乡约 —— AI 驱动乡村振兴服务平台</p>
          <p>版本：1.0.0</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '../stores/user';
import { getUserInfo } from '../api/user';

const router = useRouter();
const userStore = useUserStore();
const defaultAvatar = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=default%20user%20avatar%20simple&image_size=square';

const userInfo = computed(() => {
  return userStore.userInfo || {};
});

const role = computed(() => userInfo.value.role);

const isFarmerOrChief = computed(() => role.value === 2 || role.value === 3);
const isChief = computed(() => role.value === 3);

const goBack = () => {
  router.back();
};

const editProfile = () => {
  // 跳转到编辑个人信息页面
  router.push('/edit-profile');
};

const myVillage = () => {
  router.push('/farmer/village');
};

const myScenics = () => {
  router.push('/farmer/scenics');
};

const villageFarmers = () => {
  router.push('/farmer/farmers');
};

const viewCollections = () => {
  router.push('/my-collections');
};

const viewLikes = () => {
  router.push('/my-likes');
};

const viewComments = () => {
  router.push('/my-comments');
};

const logout = () => {
  if (confirm('确定要退出登录吗？')) {
    userStore.logout();
    router.push('/login');
  }
};

const goToLogin = () => {
  router.push('/login');
};

// 获取角色文本
const getRoleText = (roleValue: number) => {
  switch (roleValue) {
    case 1:
      return '游客';
    case 2:
      return '农户';
    case 3:
      return '村长';
    case 4:
      return '管理员';
    default:
      return '未知';
  }
};

// 用户端不允许管理员（role=4）；游客/农户/村长均可使用
const USER_APP_DENIED_ROLE = 4;

// 加载用户信息
const loadUserInfo = async () => {
  console.log('开始加载用户信息，登录状态:', userStore.isLoggedIn);
  try {
    const res = await getUserInfo();
    console.log('用户信息响应:', res);
    if (res.data) {
      if (res.data.role === USER_APP_DENIED_ROLE) {
        ElMessage.error('权限不足，无法访问！');
        userStore.logout();
        router.replace('/login');
        return;
      }
      // 更新用户信息到store
      userStore.setUserInfo(res.data);
      console.log('用户信息更新成功:', res.data);
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
  }
};

onMounted(() => {
  loadUserInfo();
});
</script>

<style scoped>
.profile-container {
  min-height: 100vh;
  background: transparent;
}

.profile-header {
  background: linear-gradient(-45deg, #11998e, #38ef7d, #11998e, #38ef7d);
  padding: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.back-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 5px;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: #f0f9e8;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: #ffffff;
  margin: 5px;
}

.profile-content {
  padding: 16px;
}

.user-info-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
  position: relative;
}

.user-avatar {
  display: inline-block;
  margin-bottom: 16px;
}

.user-avatar img {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #8BC34A;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
}

.user-email {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
}

.user-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  justify-content: center;
  margin-top: 12px;
}

.meta-item {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-label {
  color: #666;
}

.meta-value {
  color: #333;
  font-weight: 500;
}

.status-normal {
  color: #4CAF50;
}

.status-disabled {
  color: #f44336;
}

.function-list {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
  overflow: hidden;
}

.function-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.3s ease;
  -webkit-tap-highlight-color: transparent;
}

.function-item:last-child {
  border-bottom: none;
}

.function-item:active {
  background-color: #cacaca;
}

.function-icon {
  font-size: 20px;
  margin-right: 12px;
  width: 24px;
  text-align: center;
}

.function-text {
  flex: 1;
  font-size: 16px;
  color: #333;
}

.function-arrow {
  font-size: 16px;
  color: #999;
}

.about-section {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #333;
}

.about-content p {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

@media (max-width: 480px) {
  .profile-content {
    padding: 12px;
  }

  .user-info-card {
    padding: 20px;
  }

  .user-avatar img {
    width: 70px;
    height: 70px;
  }

  .function-item {
    padding: 14px;
  }

  .function-text {
    font-size: 15px;
  }
}
</style>