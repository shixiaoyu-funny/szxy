<template>
  <div class="edit-profile-container">
    <header class="edit-profile-header">
      <button type="button" class="back-btn" @click="goBack">←</button>
      <h1 class="header-title">编辑个人信息</h1>
      <button type="button" class="save-btn" @click="saveProfile" :disabled="saving">
        {{ saving ? '保存中...' : '保存' }}
      </button>
    </header>

    <div class="edit-profile-content">
      <!-- 头像上传 -->
      <div class="avatar-section">
        <div class="user-avatar">
          <!-- 这里优先显示本地预览 -->
          <img :src="localPreviewUrl || formData.avator || defaultAvatar" :alt="formData.username" />
          <button type="button" class="avatar-edit" @click="triggerFileInput">
            <span>📷</span>
          </button>
        </div>
        <input ref="fileInput" type="file" accept="image/*" class="file-input" @change.prevent="handleAvatarUpload" />
        <p class="avatar-tip">点击更换头像</p>
      </div>

      <!-- 信息表单 -->
      <form class="profile-form" @submit.prevent>
        <div class="form-item">
          <label class="form-label">用户名</label>
          <input type="text" v-model="formData.username" class="form-input" placeholder="请输入用户名" :disabled="saving" />
        </div>

        <div class="form-item">
          <label class="form-label">密码</label>
          <input type="password" v-model="formData.password" class="form-input" placeholder="请输入新密码（留空表示不修改）"
            :disabled="saving" />
        </div>

        <div class="form-item">
          <label class="form-label">手机号</label>
          <input type="tel" v-model="formData.phone" class="form-input" placeholder="请输入手机号" :disabled="saving" />
        </div>

        <div class="form-item">
          <label class="form-label">邮箱</label>
          <input type="email" v-model="formData.email" class="form-input" placeholder="请输入邮箱" :disabled="saving" />
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '../stores/user';
import { setUserInfo } from '../api/login';
import { uploadFile } from '../api/upload';

const router = useRouter();
const userStore = useUserStore();
const fileInput = ref<HTMLInputElement>();
const defaultAvatar = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=default%20user%20avatar%20simple&image_size=square';

const saving = ref(false);
// 本地预览地址
const localPreviewUrl = ref('');

const formData = ref({
  id: 0,
  username: '',
  password: '',
  phone: '',
  email: '',
  avator: ''
});

const goBack = () => {
  router.push({ name: 'profile' });
};

const triggerFileInput = () => {
  fileInput.value?.click();
};

const handleAvatarUpload = async (event: Event) => {
  event.preventDefault();
  event.stopPropagation();
  const target = event.target as HTMLInputElement;
  if (!target.files?.length) return;

  const file = target.files[0];

  // ✅ 第一步：本地预览（选图立刻显示，手机/电脑都正常）
  if (file) {
    localPreviewUrl.value = URL.createObjectURL(file);
  }

  try {
    if (!file) return;
    const res = await uploadFile(file as File);
    console.log('上传返回:', res);

    let imageUrl = '';
    if (typeof res === 'string') {
      imageUrl = res;
    } else if (res) {
      imageUrl = (res as any).url || (res as any).data || '';
    }

    // ✅ 第二步：绝对不写死 localhost！自动拼接当前域名
    if (imageUrl && !imageUrl.startsWith('http')) {
      imageUrl = `${window.location.origin}/${imageUrl}`;
    }

    // ✅ 第三步：赋值并清理预览
    formData.value.avator = imageUrl;
    ElMessage.success('头像上传成功！');

  } catch (error) {
    console.error('上传失败:', error);
    ElMessage.error('上传失败，请重试');
    localPreviewUrl.value = '';
  } finally {
    target.value = '';
  }
};

const saveProfile = async () => {
  if (!formData.value.username) {
    ElMessage.warning('用户名不能为空');
    return;
  }

  saving.value = true;

  try {
    formData.value.id = userStore.userInfo?.id || 1;

    await setUserInfo(formData.value);

    // ✅ 第四步：保存后更新全局状态，刷新不丢失
    userStore.setUserInfo({ ...userStore.userInfo, ...formData.value });

    ElMessage.success('保存成功！');
    setTimeout(goBack, 1500);

  } catch (err) {
    console.error('保存失败:', err);
    ElMessage.error('保存失败，请重试');
  } finally {
    saving.value = false;
  }
};

onMounted(() => {
  if (userStore.userInfo) {
    formData.value = {
      id: userStore.userInfo.id || 0,
      username: userStore.userInfo.username || '',
      password: '',
      phone: userStore.userInfo.phone || '',
      email: userStore.userInfo.email || '',
      avator: userStore.userInfo.avatar || ''
    };
  }
});
</script>

<style scoped>
/* 样式完全不变，你原来的样式保留 */
.edit-profile-container {
  min-height: 100vh;
  background: transparent;
}

.edit-profile-header {
  display: flex;
  align-items: center;
  padding: 16px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: all 0.3s ease;
  margin-right: 16px;
}

.back-btn:hover {
  background: #f0f9e8;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #2E7D32;
  flex: 1;
}

.save-btn {
  background: #8BC34A;
  border: none;
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.save-btn:hover {
  background: #7CB342;
}

.save-btn:disabled {
  background: #BDBDBD;
  cursor: not-allowed;
}

.edit-profile-content {
  padding: 20px 16px;
}

.avatar-section {
  text-align: center;
  margin-bottom: 30px;
}

.user-avatar {
  position: relative;
  display: inline-block;
  margin-bottom: 12px;
}

.user-avatar img {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #8BC34A;
}

.avatar-edit {
  position: absolute;
  bottom: 0;
  right: 0;
  background: white;
  border: 2px solid #8BC34A;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.3s ease;
}

.avatar-edit:hover {
  transform: scale(1.1);
  background: #f0f9e8;
}

.file-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  opacity: 0;
  cursor: pointer;
  z-index: 1;
}

.avatar-tip {
  font-size: 14px;
  color: #666;
  margin-top: 8px;
}

.profile-form {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.form-item {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 16px;
  transition: all 0.3s ease;
}

.form-input:focus {
  outline: none;
  border-color: #8BC34A;
  box-shadow: 0 0 0 2px rgba(139, 195, 74, 0.2);
}

.form-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

@media (max-width: 480px) {
  .edit-profile-content {
    padding: 16px 12px;
  }

  .profile-form {
    padding: 16px;
  }

  .user-avatar img {
    width: 80px;
    height: 80px;
  }

  .form-input {
    padding: 10px;
    font-size: 15px;
  }
}
</style>