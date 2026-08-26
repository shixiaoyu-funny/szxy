<template>
  <div v-loading="loading" class="detail-page">
    <el-card v-if="detail" shadow="hover">
      <template #header>
        <div class="card-head">
          <span>申请详情</span>
          <el-tag :type="statusTag(detail.status)" size="small">{{ statusText(detail.status) }}</el-tag>
        </div>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="申请人">{{ detail.username || '—' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="村落">{{ detail.villageName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detail.createTime || '—' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="Number(detail.status) === 0" class="actions">
        <el-button type="success" @click="onApprove">通过</el-button>
        <el-button type="danger" plain @click="onReject">拒绝</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { chiefApproveVgHead, chiefRejectVgHead, getVgHeadDetail } from '../../api/vghead';

const route = useRoute();
const router = useRouter();
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

const load = async () => {
  const id = Number(route.params.id);
  if (!id) return;
  loading.value = true;
  try {
    const res = await getVgHeadDetail(id);
    detail.value = (res.data || null) as Record<string, unknown> | null;
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败');
  } finally {
    loading.value = false;
  }
};

const onApprove = async () => {
  try {
    await ElMessageBox.confirm('确认通过？通过后仍需管理员终审。', '通过申请', { type: 'warning' });
    await chiefApproveVgHead(Number(detail.value?.id));
    ElMessage.success('已通过');
    router.push('/farmer/vghead');
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '操作失败');
  }
};

const onReject = async () => {
  try {
    await ElMessageBox.confirm('确认拒绝该申请？', '拒绝申请', { type: 'warning' });
    await chiefRejectVgHead(Number(detail.value?.id));
    ElMessage.success('已拒绝');
    router.push('/farmer/vghead');
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '操作失败');
  }
};

onMounted(load);
</script>

<style scoped>
.detail-page {
  max-width: 640px;
  margin: 16px auto;
  padding: 0 12px 24px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}
</style>
