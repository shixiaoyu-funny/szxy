<template>
  <div class="user-container">
    <h2 class="page-title">用户管理</h2>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>用户列表</span>
          <div class="header-actions">
            <el-input
              v-model="keyword"
              placeholder="用户名 / 手机号 / 邮箱"
              clearable
              class="keyword-input"
              @keyup.enter="onSearch"
            />
            <el-select v-model="roleFilter" placeholder="角色" clearable class="filter-select" @change="onFilterChange">
              <el-option :value="1" label="游客" />
              <el-option :value="2" label="农户" />
              <el-option :value="3" label="村长" />
              <el-option :value="4" label="管理员" />
            </el-select>
            <el-select v-model="statusFilter" placeholder="状态" clearable class="filter-select" @change="onFilterChange">
              <el-option :value="1" label="正常" />
              <el-option :value="0" label="禁用" />
            </el-select>
            <el-button type="primary" @click="onSearch">查询</el-button>
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
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="ID" width="72">
          <template #default="{ row }">{{ pick(row, 'id', 'id') }}</template>
        </el-table-column>
        <el-table-column label="用户名" min-width="120">
          <template #default="{ row }">{{ pick(row, 'username', 'username') || '—' }}</template>
        </el-table-column>
        <el-table-column label="手机号" min-width="130">
          <template #default="{ row }">{{ pick(row, 'phone', 'phone') || '—' }}</template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="180">
          <template #default="{ row }">{{ pick(row, 'email', 'email') || '—' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="96">
          <template #default="{ row }">{{ userRoleText(pick(row, 'role', 'role')) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="88">
          <template #default="{ row }">
            <el-tag :type="userStatusText(pick(row, 'status', 'status')).type" size="small">
              {{ userStatusText(pick(row, 'status', 'status')).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="168">
          <template #default="{ row }">{{ pick(row, 'createTime', 'create_time') || '—' }}</template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="168">
          <template #default="{ row }">{{ pick(row, 'updateTime', 'update_time') || '—' }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { userApi } from '../api';
import { pick as pickField, userRoleText, userStatusText } from '../utils/adminFields';

const loading = ref(false);
const list = ref<Record<string, unknown>[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(10);
const keyword = ref('');
const roleFilter = ref<number | undefined>(undefined);
const statusFilter = ref<number | undefined>(undefined);

function pick(row: Record<string, unknown>, camel: string, snake: string) {
  return pickField(row, camel, snake);
}

async function fetchList() {
  loading.value = true;
  try {
    const params: {
      pageNo: number;
      pageSize: number;
      keyword?: string;
      role?: number;
      status?: number;
    } = {
      pageNo: pageNo.value,
      pageSize: pageSize.value
    };
    const kw = keyword.value.trim();
    if (kw) params.keyword = kw;
    if (roleFilter.value != null) params.role = roleFilter.value;
    if (statusFilter.value != null) params.status = statusFilter.value;

    const res = await userApi.list(params);
    list.value = (res.data as Record<string, unknown>[]) || [];
    total.value = Number(res.total) || 0;
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载用户列表失败');
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  pageNo.value = 1;
  fetchList();
}

function onFilterChange() {
  pageNo.value = 1;
  fetchList();
}

onMounted(() => {
  fetchList();
});
</script>

<style scoped>
.user-container {
  max-width: 1400px;
}

.page-title {
  margin: 0 0 20px;
  font-size: 22px;
  font-weight: 600;
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
  gap: 8px;
  flex-wrap: wrap;
}

.keyword-input {
  width: 220px;
}

.filter-select {
  width: 120px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
</style>
