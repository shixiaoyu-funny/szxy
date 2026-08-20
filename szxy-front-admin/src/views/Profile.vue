<template>
  <div class="profile-container" v-loading="loading">
    <h2 class="page-title">个人中心</h2>
    <p class="page-desc">
    </p>

    <el-card shadow="hover" class="main-card">
      <template #header>
        <div class="card-header">
          <span>账户概览</span>
          <div class="card-header-actions">
            <el-button type="primary" plain @click="fetchProfile">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
            <el-button type="primary" @click="openEdit">
              <el-icon><EditPen /></el-icon>
              编辑基本信息
            </el-button>
          </div>
        </div>
      </template>

      <div class="profile-hero">
        <el-avatar class="hero-avatar" :size="96" :src="avatarUrl">
          <el-icon :size="40"><UserFilled /></el-icon>
        </el-avatar>
        <div class="hero-meta">
          <h3 class="hero-name">{{ displayName }}</h3>
          <p class="hero-sub">{{ contactLine }}</p>
          <div class="hero-tags">
            <el-tag :type="statusTag.type" effect="light" round>{{ statusTag.text }}</el-tag>
            <el-tag type="info" effect="plain" round>{{ userTypeText }}</el-tag>
          </div>
        </div>
      </div>

      <el-descriptions v-if="detail" :column="2" border class="desc-block">
        <el-descriptions-item label="用户 ID">
          {{ pick(detail, 'id', 'id') ?? '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="用户名">
          {{ pick(detail, 'username', 'username') ?? '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          {{ pick(detail, 'phone', 'phone') || '未设置' }}
        </el-descriptions-item>
        <el-descriptions-item label="邮箱">
          {{ pick(detail, 'email', 'email') || '未设置' }}
        </el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="statusTag.type" size="small">{{ statusTag.text }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="用户类型">
          {{ userTypeText }}
        </el-descriptions-item>
        <el-descriptions-item label="头像地址" :span="2">
          <span class="mono-ellipsis">{{ pick(detail, 'avatar', 'avatar') || '未设置' }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无用户信息，请尝试刷新或重新登录" />
    </el-card>

    <el-card shadow="hover" class="about-card">
      <template #header>
        <span>关于平台</span>
      </template>
      <p class="about-line">数智乡约 —— AI 驱动乡村振兴服务平台</p>
      <p class="about-meta">管理端 · 版本 1.0.0</p>
    </el-card>

    <el-dialog v-model="editVisible" title="编辑基本信息" width="560px" destroy-on-close @closed="onEditDialogClosed">
      <el-form ref="formRef" :model="editForm" :rules="editRules" label-width="88px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" placeholder="登录用户名" clearable />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="手机号" clearable />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="邮箱" clearable />
        </el-form-item>
        <el-form-item label="头像">
          <div class="avatar-uploader">
            <el-avatar class="avatar-uploader-preview" :size="88" :src="editAvatarDisplay" />
            <div class="avatar-uploader-actions">
              <el-button type="primary" :loading="avatarUploading" @click="avatarInputRef?.click()">
                <el-icon><Upload /></el-icon>
                上传图片
              </el-button>
              <input
                ref="avatarInputRef"
                type="file"
                accept="image/*"
                class="avatar-file-input"
                @change="onAvatarFileChange"
              />
              <p class="avatar-uploader-tip">本地选择图片后上传至服务器（<code>POST /upload</code>），支持 JPG / PNG / GIF / WebP，单张不超过 2MB</p>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="图片地址" prop="avatar">
          <el-input
            v-model="editForm.avatar"
            type="textarea"
            :rows="2"
            placeholder="上传成功后自动填入；也可手动粘贴外链地址"
            clearable
          />
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="editForm.password" type="password" placeholder="不修改请留空" show-password clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { Refresh, EditPen, UserFilled, Upload } from '@element-plus/icons-vue';
import { userApi, uploadApi } from '../api';
import store from '../store';
import { pick } from '../utils/adminFields';

const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const detail = ref<Record<string, unknown> | null>(null);
const editVisible = ref(false);
const formRef = ref<FormInstance>();
const avatarInputRef = ref<HTMLInputElement>();
const editLocalPreview = ref('');
const avatarUploading = ref(false);

const MAX_AVATAR_BYTES = 2 * 1024 * 1024;

function parseUploadUrl(res: unknown): string {
  if (typeof res === 'string') return res.trim();
  if (res && typeof res === 'object') {
    const o = res as Record<string, unknown>;
    const u = o.url ?? o.data;
    if (typeof u === 'string') return u.trim();
  }
  return '';
}

function normalizeImageUrl(path: string): string {
  const p = path.trim();
  if (!p) return '';
  if (/^https?:\/\//i.test(p)) return p;
  return `${window.location.origin}/${p.replace(/^\//, '')}`;
}

function revokeEditPreview() {
  if (editLocalPreview.value) {
    URL.revokeObjectURL(editLocalPreview.value);
    editLocalPreview.value = '';
  }
}

const editAvatarDisplay = computed(() => {
  if (editLocalPreview.value) return editLocalPreview.value;
  const u = editForm.value.avatar?.trim();
  return u || defaultAvatar;
});

const defaultAvatar =
  'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=default%20user%20avatar%20simple&image_size=square';

/** 管理端仅允许管理员（role === 4） */
const ADMIN_EXPECTED_ROLE = 4;

const displayName = computed(() => {
  const u = pick(detail.value, 'username', 'username');
  return u ? String(u) : '未命名';
});

const avatarUrl = computed(() => {
  const a = pick(detail.value, 'avatar', 'avatar');
  return (a && String(a)) || defaultAvatar;
});

const contactLine = computed(() => {
  const phone = pick(detail.value, 'phone', 'phone');
  const email = pick(detail.value, 'email', 'email');
  if (phone && email) return `${phone} · ${email}`;
  if (phone) return String(phone);
  if (email) return String(email);
  return '未设置联系方式';
});

const statusTag = computed(() => {
  const s = Number(pick(detail.value, 'status', 'status'));
  if (s === 1) return { text: '正常', type: 'success' as const };
  return { text: '禁用', type: 'danger' as const };
});

const userTypeText = computed(() => {
  const r = Number(pick(detail.value, 'role', 'role'));
  switch (r) {
    case 1:
      return '游客';
    case 2:
      return '农户';
    case 3:
      return '村长';
    case 4:
      return '管理员';
    default:
      return r ? `角色 ${r}` : '未知';
  }
});

const editForm = ref({
  id: undefined as number | undefined,
  username: '',
  phone: '',
  email: '',
  avatar: '',
  password: ''
});

const editRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
};

function assertAdminRole(info: Record<string, unknown>) {
  const r = Number(pick(info, 'role', 'role'));
  if (r !== ADMIN_EXPECTED_ROLE) {
    ElMessage.error('权限不足，无法访问！');
    store.actions.logout();
    router.replace('/login');
    return false;
  }
  return true;
}

async function fetchProfile() {
  loading.value = true;
  try {
    const info = (await userApi.getInfo()) as Record<string, unknown>;
    if (!assertAdminRole(info)) return;
    detail.value = info;
    store.actions.updateUserInfo(info);
  } catch (e) {
    console.error(e);
    ElMessage.error('加载个人信息失败，请检查登录态与后端服务');
  } finally {
    loading.value = false;
  }
}

function openEdit() {
  if (!detail.value) {
    ElMessage.warning('暂无用户信息，请先刷新');
    return;
  }
  revokeEditPreview();
  const d = detail.value;
  editForm.value = {
    id: Number(pick(d, 'id', 'id')) || undefined,
    username: String(pick(d, 'username', 'username') ?? ''),
    phone: String(pick(d, 'phone', 'phone') ?? ''),
    email: String(pick(d, 'email', 'email') ?? ''),
    avatar: String(pick(d, 'avatar', 'avatar') ?? ''),
    password: ''
  };
  editVisible.value = true;
}

function onEditDialogClosed() {
  revokeEditPreview();
  formRef.value?.resetFields();
}

async function onAvatarFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件');
    target.value = '';
    return;
  }
  if (file.size > MAX_AVATAR_BYTES) {
    ElMessage.warning('图片请小于 2MB');
    target.value = '';
    return;
  }

  revokeEditPreview();
  editLocalPreview.value = URL.createObjectURL(file);
  avatarUploading.value = true;

  try {
    const fd = new FormData();
    fd.append('file', file);
    const raw = await uploadApi.uploadFile(fd);
    let imageUrl = parseUploadUrl(raw);
    if (imageUrl) {
      imageUrl = normalizeImageUrl(imageUrl);
    }
    if (!imageUrl) {
      ElMessage.error('上传成功但未返回图片地址');
      return;
    }
    editForm.value.avatar = imageUrl;
    revokeEditPreview();
    ElMessage.success('头像上传成功');
  } catch (e) {
    console.error(e);
    ElMessage.error('上传失败，请重试');
    revokeEditPreview();
  } finally {
    avatarUploading.value = false;
    target.value = '';
  }
}

async function submitEdit() {
  await formRef.value?.validate().catch(() => Promise.reject());
  saving.value = true;
  try {
    const payload: Record<string, unknown> = {
      id: editForm.value.id,
      username: editForm.value.username,
      phone: editForm.value.phone || undefined,
      email: editForm.value.email || undefined,
      avatar: editForm.value.avatar || undefined
    };
    if (editForm.value.password) {
      payload.password = editForm.value.password;
    }
    await userApi.infoSet(payload);
    ElMessage.success('保存成功');
    editVisible.value = false;
    await fetchProfile();
  } catch (e) {
    console.error(e);
    ElMessage.error(e instanceof Error ? e.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  fetchProfile();
});

onUnmounted(() => {
  revokeEditPreview();
});
</script>

<style scoped>
.profile-container {
  padding: 0 4px 24px;
  max-width: 960px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  margin: 0 0 8px;
  color: #333;
}

.page-desc {
  font-size: 13px;
  color: #888;
  margin: 0 0 24px;
  line-height: 1.6;
}

.page-desc code {
  font-size: 12px;
  padding: 2px 6px;
  background: #f0f2f5;
  border-radius: 4px;
  color: #546e7a;
}

.main-card,
.about-card {
  border-radius: 10px;
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.card-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.profile-hero {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.hero-avatar {
  flex-shrink: 0;
  border: 3px solid rgba(76, 175, 80, 0.35);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
}

.hero-meta {
  flex: 1;
  min-width: 0;
}

.hero-name {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: #222;
}

.hero-sub {
  margin: 0 0 12px;
  font-size: 14px;
  color: #666;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.desc-block {
  margin-top: 4px;
}

.mono-ellipsis {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}

.about-card .about-line {
  margin: 0 0 8px;
  font-size: 15px;
  color: #333;
}

.about-card .about-meta {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.avatar-uploader {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  flex-wrap: wrap;
}

.avatar-uploader-preview {
  flex-shrink: 0;
  border: 2px solid rgba(76, 175, 80, 0.35);
}

.avatar-uploader-actions {
  flex: 1;
  min-width: 200px;
}

.avatar-uploader-tip {
  margin: 10px 0 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.avatar-uploader-tip code {
  font-size: 11px;
  padding: 1px 4px;
  background: #f0f2f5;
  border-radius: 3px;
}

.avatar-file-input {
  display: none;
}
</style>
