<template>
  <div class="scenic-container">
    <h2 class="page-title">景点管理</h2>

    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>景点列表</span>
          <div class="header-actions">
            <el-button type="primary" @click="openAdd">
              <el-icon><Plus /></el-icon>
              新增景点
            </el-button>
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
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column label="景点名称" min-width="120">
          <template #default="{ row }">{{ pick(row, 'name', 'name') }}</template>
        </el-table-column>
        <el-table-column label="村落ID" width="88">
          <template #default="{ row }">{{ pick(row, 'villageId', 'village_id') }}</template>
        </el-table-column>
        <el-table-column label="创建人" width="88">
          <template #default="{ row }">{{ pick(row, 'userId', 'user_id') }}</template>
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
        <el-table-column label="点赞/收藏" width="100">
          <template #default="{ row }">{{ pick(row, 'likes', 'likes') ?? 0 }} / {{ pick(row, 'collections', 'collections') ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" :disabled="dialogMode === 'view'">
        <el-form-item v-if="dialogMode === 'add'" label="所属农村" prop="villageId">
          <el-select v-model="form.villageId" placeholder="请选择村落" filterable style="width: 100%">
            <el-option v-for="v in villageOptions" :key="v.id" :label="`${v.name}（ID ${v.id}）`" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="景点名称" prop="name">
          <el-input v-model="form.name" placeholder="必填" />
        </el-form-item>
        <el-form-item label="介绍" prop="intro">
          <el-input v-model="form.intro" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="图片 URL">
          <el-input v-model="form.image" placeholder="多图逗号分隔" />
        </el-form-item>
        <el-form-item label="门票价格">
          <el-input-number v-model="form.price" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="景点类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
            <el-option :value="1" label="自然景观" />
            <el-option :value="2" label="人文景观" />
            <el-option :value="3" label="娱乐体验" />
            <el-option :value="4" label="民俗体验" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否住宿">
          <el-select v-model="form.hasAccommodation" style="width: 100%">
            <el-option :value="0" label="否" />
            <el-option :value="1" label="是" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.hasAccommodation === 1" label="住宿详情">
          <el-input v-model="form.accommodationInfo" placeholder="房型/价格/联系方式等" />
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
import { scenicApi, villageApi } from '../api';
import { pick as pickField, scenicTypeText } from '../utils/adminFields';

const loading = ref(false);
const saving = ref(false);
const list = ref<Record<string, unknown>[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(10);
const villageOptions = ref<Record<string, unknown>[]>([]);

const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit' | 'view'>('add');
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();

const form = reactive({
  villageId: undefined as number | undefined,
  name: '',
  intro: '',
  image: '',
  price: 0,
  type: undefined as number | undefined,
  hasAccommodation: 0,
  accommodationInfo: ''
});

const rules: FormRules = {
  name: [{ required: true, message: '请输入景点名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择景点类型', trigger: 'change' }]
};

const dialogTitle = computed(() => {
  if (dialogMode.value === 'view') return '景点详情';
  return dialogMode.value === 'add' ? '新增景点' : '编辑景点';
});

function pick(row: Record<string, unknown>, camel: string, snake: string) {
  return pickField(row, camel, snake);
}

const fetchList = async () => {
  loading.value = true;
  try {
    const res = await scenicApi.getList({ pageNo: pageNo.value, pageSize: pageSize.value });
    const raw = res as { total?: number; data?: unknown[] };
    list.value = (raw.data || []) as Record<string, unknown>[];
    total.value = raw.total ?? 0;
  } catch (e) {
    console.error(e);
    ElMessage.error(e instanceof Error ? e.message : '获取景点列表失败');
  } finally {
    loading.value = false;
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

function rowToForm(row: Record<string, unknown>) {
  form.name = String(pickField(row, 'name', 'name') ?? '');
  form.intro = String(pickField(row, 'intro', 'intro') ?? '');
  form.image = String(pickField(row, 'image', 'image') ?? '');
  form.price = Number(pickField(row, 'price', 'price') ?? 0);
  form.type = (pickField(row, 'type', 'type') as number | undefined) ?? undefined;
  form.hasAccommodation = Number(pickField(row, 'hasAccommodation', 'has_accommodation') ?? 0);
  form.accommodationInfo = String(pickField(row, 'accommodationInfo', 'accommodation_info') ?? '');
}

function resetForm() {
  editingId.value = null;
  form.villageId = undefined;
  form.name = '';
  form.intro = '';
  form.image = '';
  form.price = 0;
  form.type = undefined;
  form.hasAccommodation = 0;
  form.accommodationInfo = '';
}

function openAdd() {
  dialogMode.value = 'add';
  resetForm();
  dialogVisible.value = true;
}

function openDetail(row: Record<string, unknown>) {
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

const submitForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  saving.value = true;
  try {
    const body: Record<string, unknown> = {
      name: form.name,
      intro: form.intro || undefined,
      image: form.image || undefined,
      price: form.price || 0,
      type: form.type,
      has_accommodation: form.hasAccommodation,
      accommodation_info: form.accommodationInfo || undefined
    };
    if (dialogMode.value === 'add') {
      if (form.villageId == null) {
        ElMessage.warning('请选择所属农村');
        return;
      }
      await scenicApi.add(form.villageId, body);
      ElMessage.success('新增成功');
    } else if (editingId.value != null) {
      await scenicApi.update(editingId.value, body);
      ElMessage.success('保存成功');
    }
    dialogVisible.value = false;
    await fetchList();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败');
  } finally {
    saving.value = false;
  }
};

const onDelete = (row: Record<string, unknown>) => {
  const id = Number(row.id);
  ElMessageBox.confirm(`确定删除景点 ID=${id}？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await scenicApi.remove(id);
      ElMessage.success('已删除');
      await fetchList();
    })
    .catch(() => {});
};

onMounted(() => {
  fetchList();
  fetchVillages();
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
</style>
