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
        <el-table-column label="村长" min-width="120">
          <template #default="{ row }">
            {{ pick(row, 'managerName', 'manager_name') || pick(row, 'manageId', 'manage_id') || '—' }}
          </template>
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
        <el-form-item label="村长" prop="manageId">
          <el-select
            v-model="form.manageId"
            clearable
            filterable
            placeholder="请选择村长（可空）"
            style="width: 100%"
          >
            <el-option
              v-for="c in chiefOptions"
              :key="c.userId"
              :label="`${c.username}（ID ${c.userId}）`"
              :value="c.userId"
            />
          </el-select>
          <p v-if="!chiefOptions.length" class="field-tip">暂无村长，可先在「农户管理」建档并任命</p>
        </el-form-item>
        <el-form-item label="省 / 市 / 区县">
          <RegionCascader
            v-model:province="form.province"
            v-model:city="form.city"
            v-model:county="form.county"
            :disabled="dialogMode === 'view'"
          />
        </el-form-item>
        <el-form-item label="经度 / 纬度">
          <div class="row-2">
            <!-- 回填后锁定：coordLocked 为 true 时灰色不可改 -->
            <el-input
              v-model="form.longitude"
              placeholder="经度（选完省市区自动回填）"
              :disabled="coordLocked || dialogMode === 'view'"
            />
            <el-input
              v-model="form.latitude"
              placeholder="纬度（选完省市区自动回填）"
              :disabled="coordLocked || dialogMode === 'view'"
            />
          </div>
          <p v-if="coordLoading" class="field-tip">正在获取经纬度…</p>
          <p v-else-if="coordLocked && dialogMode !== 'view'" class="field-tip">经纬度已自动回填并锁定；重选省市区将重新获取</p>
        </el-form-item>
        <el-form-item label="村落类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="opt in VILLAGE_TYPE_OPTIONS"
              :key="opt.value"
              :value="opt.value"
              :label="opt.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="村落介绍" prop="intro">
          <el-input v-model="form.intro" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="封面图片" prop="image">
          <ImageUploader v-model="form.image" :disabled="dialogMode === 'view'" />
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
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { Plus, Refresh } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { villageApi, farmerApi, positionApi } from '../api';
import { pick as pickField, villageTypeText, VILLAGE_TYPE_OPTIONS } from '../utils/adminFields';
import { geocodeByRegion } from '../utils/amapGeocode';
import ImageUploader from '../components/ImageUploader.vue';
import RegionCascader from '../components/RegionCascader.vue';

const loading = ref(false);
const saving = ref(false);
const villageList = ref<Record<string, unknown>[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(10);
const chiefOptions = ref<{ userId: number; username: string }[]>([]);

const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit' | 'view'>('add');
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();

/** 经纬度是否锁定（自动回填后不可手改） */
const coordLocked = ref(false);
/** 正在请求 getPos / 高德 */
const coordLoading = ref(false);
/** 防并发：记录当前正在处理的省市区签名 */
let coordRequestToken = 0;
let lastRegionKey = '';
/** 打开弹窗时跳过一次 watch（避免编辑回填触发重复请求） */
let skipRegionWatch = false;

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

function roleOf(row: Record<string, unknown>): number {
  return Number(pickField(row, 'role', 'role'));
}

function regionKey(p: string, c: string, d: string) {
  return `${p}|${c}|${d}`;
}

/**
 * 省市区选完后的前端回调：
 * 1) 优先 GET /pos/getPos 读 Redis
 * 2) 经纬度为 null 则调高德地理编码
 * 3) 回填并锁定输入框；miss 时再 POST /pos/savePos 写缓存
 */
async function onRegionReady(province: string, city: string, county: string) {
  if (dialogMode.value === 'view') return;
  const key = regionKey(province, city, county);
  if (key === lastRegionKey && coordLocked.value) return;

  const token = ++coordRequestToken;
  coordLoading.value = true;
  try {
    // --- 1. 后端 Redis 缓存 ---
    const cached = await positionApi.getPos(province, city, county);
    if (token !== coordRequestToken) return;

    const lon = cached?.longitude;
    const lat = cached?.latitude;
    if (lon != null && lat != null && String(lon) !== '' && String(lat) !== '') {
      form.longitude = String(lon);
      form.latitude = String(lat);
      coordLocked.value = true;
      lastRegionKey = key;
      return;
    }

    // --- 2. 缓存未命中：前端调高德 Web 服务地理编码 ---
    const geo = await geocodeByRegion(province, city, county);
    if (token !== coordRequestToken) return;

    form.longitude = geo.longitude;
    form.latitude = geo.latitude;
    coordLocked.value = true;
    lastRegionKey = key;

    // --- 3. 回写 Redis，供下次 getPos 命中 ---
    await positionApi.savePos({
      province,
      city,
      county,
      longitude: geo.longitude,
      latitude: geo.latitude
    });
  } catch (e) {
    if (token !== coordRequestToken) return;
    console.error(e);
    ElMessage.warning(e instanceof Error ? e.message : '获取经纬度失败');
    coordLocked.value = false;
  } finally {
    if (token === coordRequestToken) {
      coordLoading.value = false;
    }
  }
}

/** 省市区变化：齐全则回调；清空则解锁并清空坐标 */
watch(
  () => [form.province, form.city, form.county] as const,
  ([p, c, d]) => {
    if (skipRegionWatch) return;
    if (!p || !c || !d) {
      // 未选全或清空：解锁，允许手动输入（或等待重新选择）
      coordLocked.value = false;
      form.longitude = '';
      form.latitude = '';
      lastRegionKey = '';
      return;
    }
    onRegionReady(p, c, d);
  }
);

const fetchChiefs = async (villageId?: number | null) => {
  try {
    const res = await farmerApi.getFarmers();
    const list = (res || []) as Record<string, unknown>[];
    chiefOptions.value = list
      .filter((r) => {
        const role = roleOf(r);
        // 候选 = 本村农户/村长（任命须满足"已在该村建档"）
        if (role !== 2 && role !== 3) return false;
        if (villageId == null) return false;
        const vId = Number(pickField(r, 'villageId', 'village_id'));
        return vId === villageId;
      })
      .map((r) => ({
        userId: Number(pickField(r, 'userId', 'user_id')),
        username: String(pickField(r, 'username', 'username') ?? `用户${pickField(r, 'userId', 'user_id')}`)
      }))
      .filter((c) => Number.isFinite(c.userId));
  } catch (e) {
    console.error(e);
    chiefOptions.value = [];
  }
};

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
  coordLocked.value = false;
  lastRegionKey = '';
}

async function openDialog(mode: 'add' | 'edit' | 'view', row?: Record<string, unknown>) {
  dialogMode.value = mode;
  skipRegionWatch = true;
  if (mode === 'add') {
    resetForm();
  } else if (row) {
    editingId.value = Number(row.id);
    rowToForm(row);
    // 编辑/查看：库中已有经纬度则直接锁定
    const hasCoord =
      form.longitude !== '' &&
      form.longitude != null &&
      form.latitude !== '' &&
      form.latitude != null;
    coordLocked.value = !!hasCoord;
    if (form.province && form.city && form.county) {
      lastRegionKey = regionKey(form.province, form.city, form.county);
    }
  }
  await fetchChiefs(mode === 'add' ? null : Number(row?.id));
  if (form.manageId != null && !chiefOptions.value.some((c) => c.userId === form.manageId)) {
    const name = row ? String(pickField(row, 'managerName', 'manager_name') || `用户${form.manageId}`) : `用户${form.manageId}`;
    chiefOptions.value = [...chiefOptions.value, { userId: form.manageId, username: name }];
  }
  dialogVisible.value = true;
  // 下一拍再允许 watch，避免打开时误触发高德
  queueMicrotask(() => {
    skipRegionWatch = false;
  });
}

function openAdd() {
  openDialog('add');
}

function openView(row: Record<string, unknown>) {
  openDialog('view', row);
}

function openEdit(row: Record<string, unknown>) {
  openDialog('edit', row);
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

.row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  width: 100%;
}

.field-tip {
  margin: 6px 0 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}
</style>
