<template>
  <!-- 右上角退出登录（SVG 图标） -->
  <button v-if="userStore.isLoggedIn" class="logout-btn" title="退出登录" @click="logout">
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <path d="M16 17l5-5-5-5" />
      <path d="M21 12H9" />
    </svg>
  </button>
  <div class="profile-page">
    <div class="profile-main">
      <div class="info-card">
        <div class="info-avatar">
          <img :src="userInfo.avatar || defaultAvatar" :alt="userInfo.username" />
        </div>
        <div class="info-detail">
          <h2 class="info-name">{{ userInfo.username || '未登录' }}</h2>
          <p class="info-contact">{{ userInfo.email || userInfo.phone || '未设置联系方式' }}</p>
          <div class="info-tags">
            <span class="info-tag" :class="userInfo.status === 1 ? 'tag-normal' : 'tag-disabled'">
              {{ userInfo.status === 1 ? '正常' : '禁用' }}
            </span>
            <span class="info-tag tag-role">{{ getRoleText(userInfo.role) }}</span>
          </div>
        </div>
        <div class="info-actions">
          <button class="edit-btn" @click="editProfile">编辑个人信息</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '../stores/user';
import { getUserInfo } from '../api/user';

const router = useRouter();
const userStore = useUserStore();
const defaultAvatar = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=default%20user%20avatar%20simple&image_size=square';

const userInfo = computed(() => {
  return userStore.userInfo || {};
});

const editProfile = () => {
  router.push('/edit-profile');
};

const logout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
      confirmButtonText: '确认退出',
      cancelButtonText: '取消',
      type: 'warning',
      customClass: 'logout-confirm'
    });
    userStore.logout();
    router.push('/login');
  } catch {
    // 用户取消
  }
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
  try {
    const res = await getUserInfo();
    if (res.data) {
      if (res.data.role === USER_APP_DENIED_ROLE) {
        ElMessage.error('权限不足，无法访问！');
        userStore.logout();
        router.replace('/login');
        return;
      }
      userStore.setUserInfo(res.data);
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
.profile-page {
  position: relative;
  height: calc(100vh - 132px);
}

/* 右上角退出 */
.logout-btn {
  position: fixed;
  margin-top: 5px;
  margin-right: 55px;
  top: 0;
  right: 0;
  width: 80px;
  height: 50px;
  border: none;
  border-radius: 10%;
  background: linear-gradient(135deg, #8BC34A 0%, #66BB6A 100%);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  z-index: 10;
}

.logout-btn:hover {
  background: rgba(255, 110, 99, 0.71);
  box-shadow: 0 4px 12px rgba(244, 67, 54, 0.2);
  transform: scale(1.05);
}

/* 个人信息大容器：占个人主页内部面积的 80%（宽 80% × 高 80%），左上角对齐 */
.profile-main {
  width: 80%;
  height: 80%;
  max-width: 1200px;
  margin: 30px 0 0 20px;
}

.info-card {
  display: flex;
  gap: 32px;
  height: 120%;
  width: 120%;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px;
  box-shadow: 0 8px 24px rgba(139, 195, 74, 0.18);
  position: relative;
  border-left: 4px solid #8BC34A;
  transition: all 0.3s ease;
}

.info-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(139, 195, 74, 0.28);
}

.info-avatar {
  flex-shrink: 0;
}

.info-avatar img {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #8BC34A;
}

.info-detail {
  flex: 1;
  min-width: 0;
}

.info-name {
  font-size: 22px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
}

.info-contact {
  font-size: 14px;
  color: #666;
  margin: 0 0 16px;
}

.info-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.info-tag {
  padding: 3px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.tag-normal {
  background: #e8f5e9;
  color: #2E7D32;
}

.tag-disabled {
  background: #ffebee;
  color: #f44336;
}

.tag-role {
  background: #e8f5e9;
  color: #558B2F;
}

.info-actions {
  flex-shrink: 0;
}

.edit-btn {
  padding: 10px 22px;
  border: none;
  border-radius: 20px;
  background: linear-gradient(135deg, #8BC34A 0%, #66BB6A 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.edit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(139, 195, 74, 0.35);
}
</style>

<style>
/* 退出登录确认弹窗：绿色过渡主题（ElMessageBox Teleport 到 body，需全局样式） */
.logout-confirm {
  border-radius: 12px;
}

.logout-confirm .el-message-box__header {
  padding: 20px 24px 12px;
}

.logout-confirm .el-message-box__title {
  font-size: 17px;
  font-weight: 600;
  color: #2E7D32;
}

.logout-confirm .el-message-box__content {
  padding: 16px 24px;
}

.logout-confirm .el-message-box__message {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
}

.logout-confirm .el-message-box__btns {
  padding: 12px 24px 20px;
}

.logout-confirm .el-message-box__btns .el-button--primary {
  background: linear-gradient(135deg, #8BC34A 0%, #66BB6A 100%);
  border: none;
  transition: all 0.3s ease;
}

.logout-confirm .el-message-box__btns .el-button--primary:hover {
  background: linear-gradient(135deg, #7CB342 0%, #558B2F 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(139, 195, 74, 0.4);
}

.logout-confirm .el-message-box__btns .el-button:not(.el-button--primary):hover {
  color: #558B2F;
  border-color: #8BC34A;
  background: #f0f9e8;
}
</style>
