<template>
  <div v-loading="loading" class="detail-page">
    <el-card v-if="detail" shadow="hover" class="detail-card">
      <template #header>
        <div class="card-head">
          <span>村长申请详情</span>
          <el-tag :type="statusTag(detail.status)" size="small">{{ statusText(detail.status) }}</el-tag>
        </div>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="申请村落">{{ detail.villageName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.username || '—' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detail.createTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updateTime || '—' }}</el-descriptions-item>
      </el-descriptions>
      <p class="hint">{{ statusHint(detail.status) }}</p>
    </el-card>
    <el-empty v-else-if="!loading" description="申请不存在" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getVgHeadDetail } from '../api/vghead';

const route = useRoute();
const loading = ref(false);
const detail = ref<Record<string, unknown> | null>(null);

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

function statusHint(v: unknown) {
  const n = Number(v);
  if (n === 0) return '申请已提交，等待村长或管理员审批。';
  if (n === 1) return '本村村长已通过，等待管理员终审。';
  if (n === 2) return '管理员已通过，您已成为本村村长。';
  if (n === 3) return '申请未通过，可在个人主页重新申请。';
  return '';
}

onMounted(async () => {
  const id = Number(route.params.id);
  if (!id) {
    ElMessage.error('无效的申请ID');
    return;
  }
  loading.value = true;
  try {
    const res = await getVgHeadDetail(id);
    detail.value = (res.data || null) as Record<string, unknown> | null;
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败');
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.detail-page {
  max-width: 640px;
  margin: 24px auto;
  padding: 0 16px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hint {
  margin: 16px 0 0;
  font-size: 13px;
  color: #888;
}
</style>
