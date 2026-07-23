<template>
  <div class="scenic-container">
    <h2 class="page-title">景点资质审批</h2>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>景点注册申请</span>
          <el-button type="primary" plain @click="fetchList">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="申请ID" width="80">
          <template #default="{ row }">{{ pick(row, 'id', 'id') }}</template>
        </el-table-column>
        <el-table-column label="申请人(user)" width="100">
          <template #default="{ row }">{{ pick(row, 'userId', 'user_id') }}</template>
        </el-table-column>
        <el-table-column label="村落ID" width="88">
          <template #default="{ row }">{{ pick(row, 'villageId', 'village_id') }}</template>
        </el-table-column>
        <el-table-column label="景点名称" min-width="120">
          <template #default="{ row }">{{ pick(row, 'name', 'name') }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ scenicTypeText(pick(row, 'type', 'type')) }}</template>
        </el-table-column>
        <el-table-column label="价格" width="72">
          <template #default="{ row }">{{ pick(row, 'price', 'price') ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="住宿" width="72">
          <template #default="{ row }">
            {{ Number(pick(row, 'hasAccommodation', 'has_accommodation')) === 1 ? '是' : '否' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="formatAuditStatus(pick(row, 'status', 'status')).type" size="small">
              {{ formatAuditStatus(pick(row, 'status', 'status')).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <template v-if="Number(pick(row, 'status', 'status')) === 0">
              <el-button link type="success" @click="doApprove(row, 1)">通过</el-button>
              <el-button link type="danger" @click="doApprove(row, 2)">拒绝</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="申请详情" width="560px">
      <el-descriptions v-if="detailRow" :column="1" border>
        <el-descriptions-item label="介绍">{{ pick(detailRow, 'intro', 'intro') || '—' }}</el-descriptions-item>
        <el-descriptions-item label="图片 URL">{{ pick(detailRow, 'image', 'image') || '—' }}</el-descriptions-item>
        <el-descriptions-item label="住宿信息">{{ pick(detailRow, 'accommodationInfo', 'accommodation_info') || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Refresh } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { scenicApi, farmerApi } from '../api';
import { pick as pickField, formatAuditStatus, scenicTypeText } from '../utils/adminFields';

const loading = ref(false);
const list = ref<Record<string, unknown>[]>([]);
const detailVisible = ref(false);
const detailRow = ref<Record<string, unknown> | null>(null);

function pick(row: Record<string, unknown>, camel: string, snake: string) {
  return pickField(row, camel, snake);
}

const fetchList = async () => {
  loading.value = true;
  try {
    const res = await scenicApi.getScenicAccessList();
    list.value = (res || []) as Record<string, unknown>[];
  } catch (e) {
    console.error(e);
    ElMessage.error('获取景点申请列表失败');
  } finally {
    loading.value = false;
  }
};

function openDetail(row: Record<string, unknown>) {
  detailRow.value = row;
  detailVisible.value = true;
}

async function doApprove(row: Record<string, unknown>, status: number) {
  const id = Number(pick(row, 'id', 'id'));
  const act = status === 1 ? '通过' : '拒绝';
  try {
    await ElMessageBox.confirm(`确定「${act}」该景点资质申请？`, '审批确认', { type: status === 1 ? 'success' : 'warning' });
    await farmerApi.processScenicAccess(id, status);
    ElMessage.success('已提交审批');
    await fetchList();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '操作失败');
  }
}

onMounted(() => {
  fetchList();
});
</script>

<style scoped>
.scenic-container {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  margin: 0 0 8px;
  color: #333;
}

.sub {
  font-size: 13px;
  color: #888;
  margin: 0 0 20px;
}

.sub code {
  font-size: 12px;
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
