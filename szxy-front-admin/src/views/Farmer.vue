<template>
  <div class="farmer-container">
    <h2 class="page-title">农户与村长资质</h2>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>数据面板</span>
          <el-button type="primary" plain @click="refreshAll">
            <el-icon><Refresh /></el-icon>
            全部刷新
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="全部农户" name="farmers">
          <el-table v-loading="loadingFarmers" :data="farmerList" stripe>
            <el-table-column label="用户ID" width="88">
              <template #default="{ row }">{{ pick(row, 'id', 'id') }}</template>
            </el-table-column>
            <el-table-column label="登录名" min-width="100">
              <template #default="{ row }">{{ pick(row, 'username', 'username') ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="手机号" min-width="120">
              <template #default="{ row }">{{ pick(row, 'phone', 'phone') ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="农户姓名" min-width="100">
              <template #default="{ row }">{{ pick(row, 'farmName', 'farm_name') ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="村落ID" width="88">
              <template #default="{ row }">{{ pick(row, 'villageId', 'village_id') ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="角色" width="88">
              <template #default="{ row }">{{ farmerRoleText(pick(row, 'type', 'type')) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="88">
              <template #default="{ row }">{{ farmerStatusText(pick(row, 'status', 'status')) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="农户资质申请" name="farmerAccess">
          <el-table v-loading="loadingFa" :data="farmerAccessList" stripe>
            <el-table-column label="申请ID" width="80">
              <template #default="{ row }">{{ pick(row, 'id', 'id') }}</template>
            </el-table-column>
            <el-table-column label="申请人" min-width="90">
              <template #default="{ row }">{{ pick(row, 'username', 'username') }}</template>
            </el-table-column>
            <el-table-column label="手机" min-width="110">
              <template #default="{ row }">{{ pick(row, 'phone', 'phone') }}</template>
            </el-table-column>
            <el-table-column label="村ID" width="72">
              <template #default="{ row }">{{ pick(row, 'villageId', 'village_id') }}</template>
            </el-table-column>
            <el-table-column label="姓名" min-width="90">
              <template #default="{ row }">{{ pick(row, 'farmName', 'farm_name') }}</template>
            </el-table-column>
            <el-table-column label="身份证" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ pick(row, 'idCard', 'id_card') }}</template>
            </el-table-column>
            <el-table-column label="经营类型" width="110">
              <template #default="{ row }">{{ farmerApplyBizText(pick(row, 'type', 'type')) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="formatAuditStatus(pick(row, 'status', 'status')).type" size="small">
                  {{ formatAuditStatus(pick(row, 'status', 'status')).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="Number(pick(row, 'status', 'status')) === 0"
                  link
                  type="success"
                  @click="approveFarmer(row, 1)"
                >
                  通过
                </el-button>
                <el-button
                  v-if="Number(pick(row, 'status', 'status')) === 0"
                  link
                  type="danger"
                  @click="approveFarmer(row, 2)"
                >
                  拒绝
                </el-button>
                <span v-if="Number(pick(row, 'status', 'status')) !== 0" class="muted">已处理</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="村长资质申请" name="managerAccess">
          <el-table v-loading="loadingMa" :data="managerAccessList" stripe>
            <el-table-column label="申请ID" width="80">
              <template #default="{ row }">{{ pick(row, 'id', 'id') }}</template>
            </el-table-column>
            <el-table-column label="申请人" min-width="90">
              <template #default="{ row }">{{ pick(row, 'username', 'username') }}</template>
            </el-table-column>
            <el-table-column label="手机" min-width="110">
              <template #default="{ row }">{{ pick(row, 'phone', 'phone') }}</template>
            </el-table-column>
            <el-table-column label="村ID" width="72">
              <template #default="{ row }">{{ pick(row, 'villageId', 'village_id') }}</template>
            </el-table-column>
            <el-table-column label="姓名" min-width="90">
              <template #default="{ row }">{{ pick(row, 'farmName', 'farm_name') }}</template>
            </el-table-column>
            <el-table-column label="身份证" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ pick(row, 'idCard', 'id_card') }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="formatAuditStatus(pick(row, 'status', 'status')).type" size="small">
                  {{ formatAuditStatus(pick(row, 'status', 'status')).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="Number(pick(row, 'status', 'status')) === 0"
                  link
                  type="success"
                  @click="approveManager(row, 1)"
                >
                  通过
                </el-button>
                <el-button
                  v-if="Number(pick(row, 'status', 'status')) === 0"
                  link
                  type="danger"
                  @click="approveManager(row, 2)"
                >
                  拒绝
                </el-button>
                <span v-if="Number(pick(row, 'status', 'status')) !== 0" class="muted">已处理</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Refresh } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { farmerApi } from '../api';
import {
  pick as pickField,
  formatAuditStatus,
  farmerRoleText,
  farmerApplyBizText
} from '../utils/adminFields';

const activeTab = ref('farmers');
const loadingFarmers = ref(false);
const loadingFa = ref(false);
const loadingMa = ref(false);

const farmerList = ref<Record<string, unknown>[]>([]);
const farmerAccessList = ref<Record<string, unknown>[]>([]);
const managerAccessList = ref<Record<string, unknown>[]>([]);

function pick(row: Record<string, unknown>, camel: string, snake: string) {
  return pickField(row, camel, snake);
}

function farmerStatusText(s: unknown): string {
  const n = Number(s);
  const m: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已拒绝', 3: '已禁用' };
  return m[n] ?? (n === undefined || Number.isNaN(n) ? '—' : `状态${n}`);
}

const fetchFarmers = async () => {
  loadingFarmers.value = true;
  try {
    const res = await farmerApi.getFarmers();
    farmerList.value = (res || []) as Record<string, unknown>[];
  } catch (e) {
    console.error(e);
    ElMessage.error('权限不足，无法获取农户列表');
  } finally {
    loadingFarmers.value = false;
  }
};

const fetchFarmerAccess = async () => {
  loadingFa.value = true;
  try {
    const res = await farmerApi.getFarmerAccessList();
    farmerAccessList.value = (res || []) as Record<string, unknown>[];
  } catch (e) {
    console.error(e);
    ElMessage.error('权限不足，无法获取农户资质申请列表');
  } finally {
    loadingFa.value = false;
  }
};

const fetchManagerAccess = async () => {
  loadingMa.value = true;
  try {
    const res = await farmerApi.getManagerAccessList();
    managerAccessList.value = (res || []) as Record<string, unknown>[];
  } catch (e) {
    console.error(e);
    ElMessage.error('权限不足，无法获取村长资质申请列表');
  } finally {
    loadingMa.value = false;
  }
};

function refreshAll() {
  fetchFarmers();
  fetchFarmerAccess();
  fetchManagerAccess();
}

function onTabChange(name: string | number) {
  if (name === 'farmers') fetchFarmers();
  if (name === 'farmerAccess') fetchFarmerAccess();
  if (name === 'managerAccess') fetchManagerAccess();
}

async function approveFarmer(row: Record<string, unknown>, status: number) {
  const id = Number(pick(row, 'id', 'id'));
  const act = status === 1 ? '通过' : '拒绝';
  try {
    await ElMessageBox.confirm(`确定要「${act}」该农户资质申请吗？`, '审批确认', { type: status === 1 ? 'success' : 'warning' });
    await farmerApi.processFarmerAccess(id, status);
    ElMessage.success('已提交审批');
    await fetchFarmerAccess();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '操作失败');
  }
}

async function approveManager(row: Record<string, unknown>, status: number) {
  const id = Number(pick(row, 'id', 'id'));
  const act = status === 1 ? '通过' : '拒绝';
  try {
    await ElMessageBox.confirm(`确定要「${act}」该村长资质申请吗？`, '审批确认', { type: status === 1 ? 'success' : 'warning' });
    await farmerApi.processManagerAccess(id, status);
    ElMessage.success('已提交审批');
    await fetchManagerAccess();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e instanceof Error ? e.message : '操作失败');
  }
}

onMounted(() => {
  refreshAll();
});
</script>

<style scoped>
.farmer-container {
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

.muted {
  color: #999;
  font-size: 13px;
}
</style>
