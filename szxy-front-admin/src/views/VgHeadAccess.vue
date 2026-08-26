<template>
  <div class="access-container">
    <h2 class="page-title">村长审批</h2>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>申请列表</span>
          <div class="header-actions">
            <el-radio-group v-model="statusFilter" size="small" @change="onFilterChange">
              <el-radio-button :label="0">待审</el-radio-button>
              <el-radio-button :label="1">村长已审</el-radio-button>
              <el-radio-button :label="2">已通过</el-radio-button>
              <el-radio-button :label="3">已拒绝</el-radio-button>
              <el-radio-button :label="null">全部</el-radio-button>
            </el-radio-group>
            <el-button plain @click="fetchList">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pageNo"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="ID" width="72">
          <template #default="{ row }">{{ row.id }}</template>
        </el-table-column>
        <el-table-column label="申请人" min-width="110">
          <template #default="{ row }">{{ pick(row, 'username', 'username') || '—' }}</template>
        </el-table-column>
        <el-table-column label="手机号" min-width="120">
          <template #default="{ row }">{{ pick(row, 'phone', 'phone') || '—' }}</template>
        </el-table-column>
        <el-table-column label="所属村落" min-width="120">
          <template #default="{ row }">{{ pick(row, 'villageName', 'village_name') || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(pick(row, 'status', 'status'))" size="small">
              {{ statusText(pick(row, 'status', 'status')) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="160">
          <template #default="{ row }">{{ pick(row, 'updateTime', 'update_time') || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="canAdminAct(pick(row, 'status', 'status'))">
              <el-button link type="success" @click="onApprove(row)">通过</el-button>
              <el-button link type="danger" @click="onReject(row)">拒绝</el-button>
            </template>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { vgHeadAccessApi } from '../api';
import { pick as pickField } from '../utils/adminFields';

const loading = ref(false);
const list = ref<Record<string, unknown>[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(10);
/** 默认看村长已审，便于终审；也可切到待审（无村长村落） */
const statusFilter = ref<number | null>(1);

function pick(row: Record<string, unknown>, camel: string, snake: string) {
  return pickField(row, camel, snake);
}

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

/** 待审(无村长直审) 或 村长已审 可操作 */
function canAdminAct(v: unknown) {
  const n = Number(v);
  return n === 0 || n === 1;
}

const fetchList = async () => {
  loading.value = true;
  try {
    const params: { pageNo: number; pageSize: number; status?: number } = {
      pageNo: pageNo.value,
      pageSize: pageSize.value
    };
    if (statusFilter.value !== null && statusFilter.value !== undefined) {
      params.status = statusFilter.value;
    }
    const res = await vgHeadAccessApi.list(params);
    list.value = (res.data || []) as Record<string, unknown>[];
    total.value = res.total ?? 0;
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败');
  } finally {
    loading.value = false;
  }
};

const onFilterChange = () => {
  pageNo.value = 1;
  fetchList();
};

const onApprove = async (row: Record<string, unknown>) => {
  try {
    await ElMessageBox.confirm(
      '确认终审通过？通过后申请人将成为该村村长，原村长降为农户。',
      '通过申请',
      { type: 'warning' }
    );
    await vgHeadAccessApi.approve(Number(row.id));
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
    await vgHeadAccessApi.reject(Number(row.id));
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
.access-container {
  padding: 8px;
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
  gap: 12px;
  flex-wrap: wrap;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.pagination-bar {
  margin-bottom: 12px;
}

.muted {
  color: #c0c4cc;
}
</style>
