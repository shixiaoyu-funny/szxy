<template>
  <div class="profile-page">
    <div class="profile-main">
      <div class="info-card">
        <div class="info-header">
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
            <p v-if="vgHeadHint" class="vghead-hint">{{ vgHeadHint }}</p>
          </div>
          <div class="info-actions">
            <button class="edit-btn" @click="editProfile">编辑个人信息</button>
            <button
              v-if="showApplyEntry"
              class="apply-btn"
              type="button"
              @click="goApplyFarmer"
            >
              {{ applyBtnText }}
            </button>
            <button
              v-if="showVgHeadEntry"
              class="apply-btn"
              type="button"
              :disabled="vgHeadApplying || !canApplyVgHead"
              @click="onApplyVgHead"
            >
              {{ vgHeadBtnText }}
            </button>
          </div>
        </div>

        <div v-if="userStore.isLoggedIn" class="activity-row">
          <button
            v-for="item in activityEntries"
            :key="item.path"
            type="button"
            class="activity-card"
            @click="goActivity(item.path)"
          >
            <span class="activity-icon" :class="item.iconClass">{{ item.icon }}</span>
            <span class="activity-title">{{ item.title }}</span>
            <span class="activity-desc">{{ item.desc }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '../stores/user';
import { getUserInfo } from '../api/user';
import { getMyFarmerAccess } from '../api/farmerAccess';
import { applyVgHead, getMyVgHeadAccess } from '../api/vghead';

const router = useRouter();
const userStore = useUserStore();
const defaultAvatar = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=default%20user%20avatar%20simple&image_size=square';

const activityEntries = [
  {
    title: '我的订单',
    desc: '查看与管理订单',
    path: '/orders',
    icon: '📦',
    iconClass: 'icon-collect'
  },
  {
    title: '购物车',
    desc: '结算已加购商品',
    path: '/cart',
    icon: '🛒',
    iconClass: 'icon-like'
  },
  {
    title: '我的钱包',
    desc: '余额充值与流水',
    path: '/wallet',
    icon: '💰',
    iconClass: 'icon-comment'
  },
  {
    title: '收货地址',
    desc: '管理收货地址',
    path: '/address',
    icon: '📍',
    iconClass: 'icon-collect'
  },
  {
    title: '我的收藏',
    desc: '查看收藏的景点',
    path: '/my-collections',
    icon: '⭐',
    iconClass: 'icon-collect'
  },
  {
    title: '我的点赞',
    desc: '查看点赞的景点',
    path: '/my-likes',
    icon: '👍',
    iconClass: 'icon-like'
  },
  {
    title: '我的评论',
    desc: '查看发表的评论',
    path: '/my-comments',
    icon: '💬',
    iconClass: 'icon-comment'
  }
];

const userInfo = computed(() => {
  return userStore.userInfo || {};
});

const applyStatus = ref<number | null>(null);
const vgHeadStatus = ref<number | null>(null);
const vgHeadApplying = ref(false);

const isVisitor = computed(() => Number((userInfo.value as Record<string, unknown>).role) === 1);
const isFarmer = computed(() => Number((userInfo.value as Record<string, unknown>).role) === 2);
const showApplyEntry = computed(() => isVisitor.value && applyStatus.value !== 1);
const applyBtnText = computed(() => {
  if (applyStatus.value === 0) return '修改申请';
  if (applyStatus.value === 2) return '重新申请';
  return '申请成为农户';
});

const showVgHeadEntry = computed(() => isFarmer.value);
const canApplyVgHead = computed(() => {
  const s = vgHeadStatus.value;
  // 仅无记录或已拒绝可再申请；已通过后角色会变为村长，入口本身会隐藏
  return s === null || s === 3;
});
const vgHeadBtnText = computed(() => {
  if (vgHeadStatus.value === 0) return '村长申请审核中';
  if (vgHeadStatus.value === 1) return '等待管理员终审';
  if (vgHeadStatus.value === 2) return '申请已通过';
  if (vgHeadStatus.value === 3) return '重新申请村长';
  return '申请成为村长';
});
const vgHeadHint = computed(() => {
  if (!isFarmer.value) return '';
  if (vgHeadStatus.value === 0) return '您的村长申请待村长/管理员审批';
  if (vgHeadStatus.value === 1) return '村长已通过，等待管理员终审';
  if (vgHeadStatus.value === 3) return '上次申请未通过，可重新申请';
  return '';
});

const editProfile = () => {
  router.push('/edit-profile');
};

const goApplyFarmer = () => {
  router.push('/apply-farmer');
};

const goActivity = (path: string) => {
  router.push(path);
};

const onApplyVgHead = async () => {
  if (!canApplyVgHead.value) return;
  try {
    await ElMessageBox.confirm(
      '确认申请成为本村村长？提交后将进入审批流程，无需填写表单。',
      '申请成为村长',
      { confirmButtonText: '确认申请', cancelButtonText: '取消', type: 'info' }
    );
  } catch {
    return;
  }
  vgHeadApplying.value = true;
  try {
    await applyVgHead();
    ElMessage.success('申请已提交');
    await loadVgHeadStatus();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '申请失败');
  } finally {
    vgHeadApplying.value = false;
  }
};

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

const USER_APP_DENIED_ROLE = 4;

const loadApplyStatus = async () => {
  if (!isVisitor.value) {
    applyStatus.value = null;
    return;
  }
  try {
    const res = await getMyFarmerAccess();
    const data = res.data as { status?: number } | null;
    applyStatus.value = data?.status == null ? null : Number(data.status);
  } catch {
    applyStatus.value = null;
  }
};

const loadVgHeadStatus = async () => {
  if (!isFarmer.value) {
    vgHeadStatus.value = null;
    return;
  }
  try {
    const res = await getMyVgHeadAccess();
    const data = res.data as { status?: number } | null;
    vgHeadStatus.value = data?.status == null ? null : Number(data.status);
  } catch {
    vgHeadStatus.value = null;
  }
};

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
      await Promise.all([loadApplyStatus(), loadVgHeadStatus()]);
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
  min-height: calc(100vh - 132px);
  padding: 0 16px;
}

.profile-main {
  width: 80%;
  max-width: 1200px;
  margin: 30px auto 0;
}

.info-card {
  display: flex;
  flex-direction: column;
  gap: 0;
  width: 100%;
  min-height: 360px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px;
  box-shadow: 0 8px 24px rgba(139, 195, 74, 0.18);
  position: relative;
  border-left: 4px solid #8BC34A;
  transition: all 0.3s ease;
}

.info-header {
  display: flex;
  gap: 32px;
  align-items: flex-start;
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

.vghead-hint {
  margin: 12px 0 0;
  font-size: 13px;
  color: #888;
}

.info-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.edit-btn,
.apply-btn {
  padding: 10px 22px;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.edit-btn {
  background: linear-gradient(135deg, #8BC34A 0%, #66BB6A 100%);
  color: #fff;
}

.apply-btn {
  background: #fff;
  color: #558B2F;
  border: 1px solid #8BC34A;
  margin-top: 10px;
}

.apply-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.edit-btn:hover,
.apply-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(139, 195, 74, 0.35);
}

.activity-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
  margin-top: 36px;
  padding-top: 28px;
  border-top: 1px solid #eef4e6;
}

.activity-card {
  min-width: 0;
  min-height: 132px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px 16px;
  border: 1px solid #e8f0e0;
  border-radius: 12px;
  background: linear-gradient(180deg, #fbfcf9 0%, #f3f8ee 100%);
  cursor: pointer;
  transition: all 0.25s ease;
}

.activity-card:hover {
  transform: translateY(-3px);
  border-color: #8BC34A;
  box-shadow: 0 8px 20px rgba(139, 195, 74, 0.22);
}

.activity-icon {
  font-size: 28px;
  line-height: 1;
}

.activity-title {
  font-size: 16px;
  font-weight: 600;
  color: #2d331f;
}

.activity-desc {
  font-size: 12px;
  color: #888;
  text-align: center;
}

@media (max-width: 768px) {
  .activity-row {
    flex-direction: column;
    align-items: center;
    gap: 16px;
  }

  .activity-card {
    width: 100%;
    max-width: none;
  }
}
</style>
