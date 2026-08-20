<template>
  <div class="farmer-container">
    <h2 class="page-title">农户管理</h2>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>全部农户（{{ farmerList.length }}）</span>
          <div class="header-actions">
            <el-button type="primary" @click="openCreate">
              <el-icon><Plus /></el-icon>
              建档农户
            </el-button>
            <el-button plain @click="fetchFarmers">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loadingFarmers" :data="farmerList" stripe>
        <el-table-column label="档案ID" width="80">
          <template #default="{ row }">{{ pick(row, 'id', 'id') }}</template>
        </el-table-column>
        <el-table-column label="用户ID" width="88">
          <template #default="{ row }">{{ pick(row, 'userId', 'user_id') }}</template>
        </el-table-column>
        <el-table-column label="用户名" min-width="100">
          <template #default="{ row }">{{ pick(row, 'username', 'username') ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="手机号" min-width="120">
          <template #default="{ row }">{{ pick(row, 'phone', 'phone') ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="村落ID" width="88">
          <template #default="{ row }">{{ pick(row, 'villageId', 'village_id') ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="经营类型" width="120">
          <template #default="{ row }">{{ farmerApplyBizText(pick(row, 'businessType', 'business_type')) }}</template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="roleTagType(pick(row, 'role', 'role'))" size="small">
              {{ roleText(pick(row, 'role', 'role')) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="Number(pick(row, 'role', 'role')) === 3" @click="openSetManager(row)">
              任命村长
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 建档弹窗 -->
    <el-dialog v-model="createVisible" title="建档农户" width="520px" destroy-on-close @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属农村" prop="villageId">
          <el-select v-model="form.villageId" placeholder="请选择村落" filterable style="width: 100%">
            <el-option v-for="v in villageOptions" :key="v.id" :label="`${v.name}（ID ${v.id}）`" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="留空默认用手机号" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="必填，唯一" maxlength="11" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCard" placeholder="选填" />
        </el-form-item>
        <el-form-item label="经营类型" prop="businessType">
          <el-select v-model="form.businessType" placeholder="请选择" style="width: 100%">
            <el-option :value="1" label="民宿经营者" />
            <el-option :value="2" label="农产品销售者" />
            <el-option :value="3" label="文旅服务者" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">确定建档</el-button>
      </template>
    </el-dialog>

    <!-- 任命村长弹窗 -->
    <el-dialog v-model="managerVisible" title="任命村长" width="480px">
      <el-form label-width="100px">
        <el-form-item label="农村">
          <span>{{ currentVillageName || '—' }}</span>
        </el-form-item>
        <el-form-item label="目标农户">
          <span>{{ targetFarmerName }}</span>
        </el-form-item>
        <el-alert type="warning" :closable="false" show-icon
          title="将把该农户设为村长；若该村已有村长，旧村长自动降为农户" />
      </el-form>
      <template #footer>
        <el-button @click="managerVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingManager" @click="submitSetManager">确认任命</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { Plus, Refresh } from '@element-plus/icons-vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { farmerApi, villageApi } from '../api';
import { pick as pickField, farmerApplyBizText } from '../utils/adminFields';

const loadingFarmers = ref(false);
const saving = ref(false);
const savingManager = ref(false);
const farmerList = ref<Record<string, unknown>[]>([]);
const villageOptions = ref<Record<string, unknown>[]>([]);

const createVisible = ref(false);
const managerVisible = ref(false);
const formRef = ref<FormInstance>();

const currentVillageName = ref('');
const targetFarmerName = ref('');
const targetVillageId = ref<number | null>(null);
const targetFarmerUserId = ref<number | null>(null);

const form = reactive({
  villageId: undefined as number | undefined,
  username: '',
  phone: '',
  idCard: '',
  businessType: undefined as number | undefined
});

const rules: FormRules = {
  villageId: [{ required: true, message: '请选择所属农村', trigger: 'change' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  businessType: [{ required: true, message: '请选择经营类型', trigger: 'change' }]
};

function pick(row: Record<string, unknown>, camel: string, snake: string) {
  return pickField(row, camel, snake);
}

function roleText(role: unknown): string {
  const n = Number(role);
  const m: Record<number, string> = { 1: '游客', 2: '农户', 3: '村长', 4: '管理员' };
  return m[n] ?? (n === undefined || Number.isNaN(n) ? '—' : `角色${n}`);
}

function roleTagType(role: unknown): 'success' | 'warning' | 'info' | 'danger' {
  const n = Number(role);
  if (n === 3) return 'warning';
  if (n === 2) return 'success';
  if (n === 4) return 'danger';
  return 'info';
}

const fetchFarmers = async () => {
  loadingFarmers.value = true;
  try {
    const res = await farmerApi.getFarmers();
    farmerList.value = (res || []) as Record<string, unknown>[];
  } catch (e) {
    console.error(e);
    ElMessage.error(e instanceof Error ? e.message : '获取农户列表失败');
  } finally {
    loadingFarmers.value = false;
  }
};

const fetchVillages = async () => {
  try {
    const res = await villageApi.getVillageList({ pageNo: 1, pageSize: 200 });
    const raw = res as { total?: number; data?: unknown[] };
    villageOptions.value = (raw.data || []) as Record<string, unknown>[];
  } catch (e) {
    console.error(e);
  }
};

function resetForm() {
  form.villageId = undefined;
  form.username = '';
  form.phone = '';
  form.idCard = '';
  form.businessType = undefined;
}

function openCreate() {
  resetForm();
  createVisible.value = true;
}

const submitCreate = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  saving.value = true;
  try {
    await farmerApi.createFarmer(form.villageId!, {
      username: form.username || undefined,
      phone: form.phone,
      id_card: form.idCard || undefined,
      business_type: form.businessType
    });
    ElMessage.success('建档成功，初始密码 123456');
    createVisible.value = false;
    await fetchFarmers();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '建档失败');
  } finally {
    saving.value = false;
  }
};

function openSetManager(row: Record<string, unknown>) {
  const villageId = pick(row, 'villageId', 'village_id');
  const userId = pick(row, 'userId', 'user_id');
  if (villageId == null) {
    ElMessage.warning('该农户未绑定村落，无法任命');
    return;
  }
  targetVillageId.value = Number(villageId);
  targetFarmerUserId.value = Number(userId);
  targetFarmerName.value = String(pick(row, 'username', 'username') ?? '');
  const v = villageOptions.value.find((x) => Number(x.id) === Number(villageId));
  currentVillageName.value = String(v ? pick(v, 'name', 'name') : `村ID ${villageId}`);
  managerVisible.value = true;
}

const submitSetManager = async () => {
  if (targetVillageId.value == null || targetFarmerUserId.value == null) return;
  savingManager.value = true;
  try {
    await farmerApi.setManager(targetVillageId.value, targetFarmerUserId.value);
    ElMessage.success('任命成功');
    managerVisible.value = false;
    await fetchFarmers();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '任命失败');
  } finally {
    savingManager.value = false;
  }
};

onMounted(() => {
  fetchFarmers();
  fetchVillages();
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}
</style>
