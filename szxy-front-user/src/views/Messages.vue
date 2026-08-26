<template>
  <div class="messages-page">
    <section class="msg-section">
      <h2 class="section-title">申请消息</h2>
      <div v-loading="applyLoading" class="apply-list">
        <div
          v-for="item in applyList"
          :key="item.id"
          class="apply-row"
          @click="goApplyDetail(item.id)"
        >
          <div class="apply-main">
            <span class="apply-title">申请成为「{{ item.villageName || '本村' }}」村长</span>
            <el-tag :type="statusTag(item.status)" size="small">{{ statusText(item.status) }}</el-tag>
          </div>
          <span class="apply-time">{{ item.updateTime || item.createTime || '' }}</span>
        </div>
        <el-empty v-if="!applyLoading && applyList.length === 0" description="暂无申请消息" :image-size="72" />
      </div>
    </section>

    <section class="msg-section likes-section">
      <h2 class="section-title">收到的赞</h2>
      <div v-loading="likeLoading" class="like-list">
        <div v-for="(item, idx) in likeList" :key="idx" class="like-row">
          <img class="like-avatar" :src="item.avatar || defaultAvatar" :alt="item.username" />
          <div class="like-body">
            <p class="like-line">
              <span class="like-name">{{ item.username || '用户' }}</span>
              {{ item.actionText || '赞了你的景点' }}
            </p>
            <span class="like-time">{{ item.createTime || '' }}</span>
          </div>
        </div>
        <el-empty v-if="!likeLoading && likeList.length === 0" description="暂无点赞动态" :image-size="72" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getMyVgHeadList } from '../api/vghead';
import { getLikesReceived } from '../api/user';

const router = useRouter();
const defaultAvatar =
  'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=default%20user%20avatar%20simple&image_size=square';

type ApplyItem = {
  id: number;
  villageName?: string;
  status?: number;
  createTime?: string;
  updateTime?: string;
};

type LikeItem = {
  username?: string;
  avatar?: string;
  actionText?: string;
  createTime?: string;
};

const applyLoading = ref(false);
const likeLoading = ref(false);
const applyList = ref<ApplyItem[]>([]);
const likeList = ref<LikeItem[]>([]);

function statusText(v: unknown) {
  const n = Number(v);
  if (n === 0) return '待审';
  if (n === 1) return '村长已审';
  if (n === 2) return '已通过';
  if (n === 3) return '已拒绝';
  return '—';
}

function statusTag(v: unknown): 'warning' | 'success' | 'info' | 'danger' {
  const n = Number(v);
  if (n === 0) return 'warning';
  if (n === 1) return 'info';
  if (n === 2) return 'success';
  if (n === 3) return 'danger';
  return 'info';
}

const goApplyDetail = (id: number) => {
  router.push(`/messages/apply/${id}`);
};

const loadApplies = async () => {
  applyLoading.value = true;
  try {
    const res = await getMyVgHeadList();
    applyList.value = (res.data || []) as ApplyItem[];
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载申请消息失败');
  } finally {
    applyLoading.value = false;
  }
};

const loadLikes = async () => {
  likeLoading.value = true;
  try {
    const res = await getLikesReceived();
    likeList.value = (res.data || []) as LikeItem[];
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载点赞动态失败');
  } finally {
    likeLoading.value = false;
  }
};

onMounted(() => {
  loadApplies();
  loadLikes();
});
</script>

<style scoped>
.messages-page {
  max-width: 720px;
  margin: 24px auto;
  padding: 0 16px 40px;
}

.msg-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px 22px;
  box-shadow: 0 6px 20px rgba(139, 195, 74, 0.12);
  border-left: 4px solid #8bc34a;
}

.likes-section {
  margin-top: 20px;
  border-left-color: #66bb6a;
}

.section-title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.apply-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 8px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.apply-row:last-child {
  border-bottom: none;
}

.apply-row:hover {
  background: #f9fbf5;
}

.apply-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.apply-title {
  font-size: 14px;
  color: #333;
}

.apply-time,
.like-time {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
}

.like-row {
  display: flex;
  gap: 12px;
  padding: 12px 4px;
  border-bottom: 1px solid #f5f5f5;
}

.like-row:last-child {
  border-bottom: none;
}

.like-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.like-body {
  min-width: 0;
  flex: 1;
}

.like-line {
  margin: 0 0 4px;
  font-size: 14px;
  color: #555;
  line-height: 1.45;
}

.like-name {
  font-weight: 600;
  color: #333;
  margin-right: 4px;
}
</style>
