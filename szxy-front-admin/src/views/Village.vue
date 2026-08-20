<template>
  <div class="village-container">
    <h2 class="page-title">农村信息管理</h2>
    <el-card class="village-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-actions">
            <el-button type="primary" @click="openAdd">
              <el-icon><Plus /></el-icon>
              新增农村
            </el-button>
            <el-button plain @click="fetchVillageList">
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
          @size-change="fetchVillageList"
          @current-change="fetchVillageList"
        />
      </div>

      <el-table v-loading="loading" :data="villageList" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column label="名称" min-width="120">
          <template #default="{ row }">{{ cell(row, 'name') }}</template>
        </el-table-column>
        <el-table-column label="省" width="90">
          <template #default="{ row }">{{ cell(row, 'province') }}</template>
        </el-table-column>
        <el-table-column label="市" width="90">
          <template #default="{ row }">{{ cell(row, 'city') }}</template>
        </el-table-column>
        <el-table-column label="区县" width="100">
          <template #default="{ row }">{{ cell(row, 'county') }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ villageTypeText(pick(row, 'type', 'type')) }}</template>
        </el-table-column>
        <el-table-column label="村长用户ID" width="110">
          <template #default="{ row }">{{ pick(row, 'manageId', 'manage_id') ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openView(row)">查看</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" :disabled="dialogMode === 'view'">
        <el-form-item label="农村名称" prop="name">
          <el-input v-model="form.name" placeholder="村落名称（唯一）" />
        </el-form-item>
        <el-form-item label="村长用户ID" prop="manageId">
          <el-input-number v-model="form.manageId" :min="0" :controls="false" placeholder="可选，对应 user.id" style="width: 100%" />
        </el-form-item>
        <el-form-item label="省 / 市 / 区县">
          <div class="row-3">
            <el-input v-model="form.province" placeholder="省" />
            <el-input v-model="form.city" placeholder="市" />
            <el-input v-model="form.county" placeholder="区县" />
          </div>
        </el-form-item>
        <el-form-item label="经度 / 纬度">
          <div class="row-2">
            <el-input v-model="form.longitude" placeholder="经度" />
            <el-input v-model="form.latitude" placeholder="纬度" />
          </div>
        </el-form-item>
        <el-form-item label="村落类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
            <el-option :value="1" label="古村落" />
            <el-option :value="2" label="生态村" />
            <el-option :value="3" label="民俗村" />
            <el-option :value="4" label="文旅村" />
          </el-select>
        </el-form-item>
        <el-form-item label="村落介绍" prop="intro">
          <el-input v-model="form.intro" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="封面图片" prop="image">
          <el-input v-model="form.image" placeholder="URL，多图逗号分隔" />
        </el-form-item>
        <el-form-item label="最佳游玩时间" prop="bestTime">
          <el-input v-model="form.bestTime" placeholder="如 3-5月" />
        </el-form-item>
        <el-form-item label="季节性活动" prop="activity">
          <el-input v-model="form.activity" />
        </el-form-item>
        <el-form-item label="联系方式" prop="contact">
          <el-input v-model="form.contact" placeholder="电话/邮箱，逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ dialogMode === 'view' ? '关闭' : '取消' }}</el-button>
        <el-button v-if="dialogMode !== 'view'" type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { Plus, Refresh } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { villageApi } from '../api';
import { pick as pickField, villageTypeText } from '../utils/adminFields';

const loading = ref(false);
const saving = ref(false);
const villageList = ref<Record<string, unknown>[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(10);

const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit' | 'view'>('add');
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();

const form = reactive({
  name: '',
  manageId: undefined as number | undefined,
  province: '',
  city: '',
  county: '',
  longitude: '' as string | number | '',
  latitude: '' as string | number | '',
  type: undefined as number | undefined,
  intro: '',
  image: '',
  bestTime: '',
  activity: '',
  contact: ''
});

const rules: FormRules = {
  name: [{ required: true, message: '请输入农村名称', trigger: 'blur' }]
};

const dialogTitle = computed(() => {
  if (dialogMode.value === 'view') return '农村详情';
  return dialogMode.value === 'add' ? '新增农村' : '编辑农村';
});

function pick(row: Record<string, unknown>, camel: string, snake: string) {
  return pickField(row, camel, snake);
}

function cell(row: Record<string, unknown>, key: string) {
  return pickField(row, key, key) ?? '—';
}

const fetchVillageList = async () => {
  loading.value = true;
  try {
    const result = await villageApi.getVillageList({ pageNo: pageNo.value, pageSize: pageSize.value });
    const raw = result as { total?: number; data?: unknown[] };
    villageList.value = (raw.data || []) as Record<string, unknown>[];
    total.value = raw.total ?? 0;
  } catch (e) {
    console.error(e);
    ElMessage.error('获取农村列表失败');
  } finally {
    loading.value = false;
  }
};

function rowToForm(row: Record<string, unknown>) {
  form.name = String(pickField(row, 'name', 'name') ?? '');
  form.manageId = (pickField(row, 'manageId', 'manage_id') as number | undefined) ?? undefined;
  form.province = String(pickField(row, 'province', 'province') ?? '');
  form.city = String(pickField(row, 'city', 'city') ?? '');
  form.county = String(pickField(row, 'county', 'county') ?? '');
  form.longitude = pickField(row, 'longitude', 'longitude') ?? '';
  form.latitude = pickField(row, 'latitude', 'latitude') ?? '';
  form.type = (pickField(row, 'type', 'type') as number | undefined) ?? undefined;
  form.intro = String(pickField(row, 'intro', 'intro') ?? '');
  form.image = String(pickField(row, 'image', 'image') ?? '');
  form.bestTime = String(pickField(row, 'bestTime', 'best_time') ?? '');
  form.activity = String(pickField(row, 'activity', 'activity') ?? '');
  form.contact = String(pickField(row, 'contact', 'contact') ?? '');
}

function resetForm() {
  editingId.value = null;
  form.name = '';
  form.manageId = undefined;
  form.province = '';
  form.city = '';
  form.county = '';
  form.longitude = '';
  form.latitude = '';
  form.type = undefined;
  form.intro = '';
  form.image = '';
  form.bestTime = '';
  form.activity = '';
  form.contact = '';
}

function openAdd() {
  dialogMode.value = 'add';
  resetForm();
  dialogVisible.value = true;
}

function openView(row: Record<string, unknown>) {
  dialogMode.value = 'view';
  editingId.value = Number(row.id);
  rowToForm(row);
  dialogVisible.value = true;
}

function openEdit(row: Record<string, unknown>) {
  dialogMode.value = 'edit';
  editingId.value = Number(row.id);
  rowToForm(row);
  dialogVisible.value = true;
}

function buildPayload(): Record<string, unknown> {
  const lon = form.longitude === '' ? undefined : Number(form.longitude);
  const lat = form.latitude === '' ? undefined : Number(form.latitude);
  return {
    name: form.name,
    manageId: form.manageId,
    province: form.province || undefined,
    city: form.city || undefined,
    county: form.county || undefined,
    longitude: lon != null && !Number.isNaN(lon) ? lon : undefined,
    latitude: lat != null && !Number.isNaN(lat) ? lat : undefined,
    type: form.type,
    intro: form.intro || undefined,
    image: form.image || undefined,
    bestTime: form.bestTime || undefined,
    activity: form.activity || undefined,
    contact: form.contact || undefined
  };
}

const submitForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  saving.value = true;
  try {
    const body = buildPayload();
    if (dialogMode.value === 'add') {
      await villageApi.addVillage(body);
      ElMessage.success('新增成功');
    } else if (editingId.value != null) {
      await villageApi.updateVillage(editingId.value, body);
      ElMessage.success('保存成功');
    }
    dialogVisible.value = false;
    await fetchVillageList();
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败');
  } finally {
    saving.value = false;
  }
};

const onDelete = (row: Record<string, unknown>) => {
  const id = Number(row.id);
  ElMessageBox.confirm(`确定删除农村 ID=${id}？删除前请确认无关联数据。`, '删除确认', { type: 'warning' })
    .then(async () => {
      await villageApi.deleteVillage(id);
      ElMessage.success('已删除');
      await fetchVillageList();
    })
    .catch(() => {});
};

onMounted(() => {
  fetchVillageList();
});
</script>

<style scoped>
.village-container {
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  margin: 0 0 20px;
  color: #333;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.pagination-bar {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.row-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
  width: 100%;
}

.row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  width: 100%;
}
</style>
