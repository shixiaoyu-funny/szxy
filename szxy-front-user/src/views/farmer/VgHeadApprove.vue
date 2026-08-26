<template>
  <div class="vghead-page">
    <h2 class="page-title">村长审批</h2>
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>本村待审申请</span>
          <el-button plain @click="fetchList">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="申请人" min-width="120">
          <template #default="{ row }">
            <div class="user-cell">
              <img class="avatar" :src="row.avatar || defaultAvatar" alt="" />
              <span>{{ row.username || '—' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" min-width="120">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="村落" min-width="120">
          <template #default="{ row }">{{ row.villageName || '—' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="160">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">详情</el-button>
            <el-button link type="success" @click="onApprove(row)">通过</el-button>
            <el-button link type="danger" @click="onReject(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无待审申请" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { chiefApproveVgHead, chiefRejectVgHead, getPendingChiefList } from '../../api/vghead';

const router = useRouter();
const defaultAvatar =
  'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=default%20user%20avatar%20simple&image_size=square';

const loading = ref(false);
const list = ref<Record<string, unknown>[]>([]);

const fetchList = async () => {
  loading.value = true;
  try {
    const res = await getPendingChiefList();
    list.value = (res.data || []) as Record<string, unknown>[];
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败');
  } finally {
    loading.value = false;
  }
};

const goDetail = (id: unknown) => {
  router.push(`/farmer/vghead/${id}`);
};

const onApprove = async (row: Record<string, unknown>) => {
  try {
    await ElMessageBox.confirm('确认通过该申请？通过后仍需管理员终审。', '通过申请', {
      type: 'warning'
    });
    await chiefApproveVgHead(Number(row.id));
    ElMessage.success('已通过');
    fetchList();
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '操作失败');
    }
  }
};

const onReject = async (row: Record<string, unknown>) => {
  try {
    await ElMessageBox.confirm('确认拒绝该申请？', '拒绝申请', { type: 'warning' });
    await chiefRejectVgHead(Number(row.id));
    ElMessage.success('已拒绝');
    fetchList();
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '操作失败');
    }
  }
};

onMounted(fetchList);
</script>

<style scoped>
.vghead-page {
  padding: 8px 4px 24px;
}

.page-title {
  margin: 0 0 16px;
  font-size: 20px;
  color: #303133;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}
</style>
