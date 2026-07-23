<template>
  <div class="vf-page">
    <header class="page-header">
      <h1 class="header-title">本村农户管理</h1>
      <p class="hint">村长专属：维护本村农户（后台审核通过后生效）</p>
    </header>

    <div v-if="!allowed" class="deny">
      <p>您不是本村村长，无权使用本功能。</p>
      <router-link to="/village-manage" class="link">返回农村管理</router-link>
    </div>

    <div v-else class="content">
      <button type="button" class="btn-add" @click="openAdd">+ 新增本村农户</button>

      <div v-if="farmers.length === 0" class="empty">暂无农户数据</div>
      <div v-else class="list">
        <div v-for="row in farmers" :key="row.id" class="row">
          <div class="row-main">
            <div class="name">{{ row.farmName || row.username }}</div>
            <div class="sub">{{ row.phone }} · {{ farmerTypeText(row.type) }}</div>
          </div>
          <div class="actions">
            <button type="button" class="btn-sm" @click="openEdit(row)">编辑</button>
            <button type="button" class="btn-sm danger" @click="remove(row)">移除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增 -->
    <div v-if="showAdd" class="modal" @click.self="showAdd = false">
      <div class="modal-card">
        <h3>新增农户</h3>
        <label>真实姓名</label>
        <input v-model="addForm.farmName" class="inp" placeholder="与登录展示名可一致" />
        <label>手机号</label>
        <input v-model="addForm.phone" class="inp" placeholder="用于登录" />
        <label>经营类型</label>
        <select v-model.number="addForm.type" class="inp">
          <option :value="1">民宿经营者</option>
          <option :value="2">农产品销售者</option>
          <option :value="3">文旅服务者</option>
        </select>
        <div class="modal-actions">
          <button type="button" class="btn-cancel" @click="showAdd = false">取消</button>
          <button type="button" class="btn-ok" :disabled="saving" @click="submitAdd">保存</button>
        </div>
      </div>
    </div>

    <!-- 编辑 -->
    <div v-if="editRow" class="modal" @click.self="editRow = null">
      <div class="modal-card">
        <h3>编辑农户</h3>
        <label>用户名</label>
        <input v-model="editForm.username" class="inp" />
        <label>手机号</label>
        <input v-model="editForm.phone" class="inp" />
        <label>农户姓名</label>
        <input v-model="editForm.farmName" class="inp" />
        <label>经营类型</label>
        <select v-model.number="editForm.type" class="inp">
          <option :value="1">民宿经营者</option>
          <option :value="2">农产品销售者</option>
          <option :value="3">文旅服务者</option>
        </select>
        <div class="modal-actions">
          <button type="button" class="btn-cancel" @click="editRow = null">取消</button>
          <button type="button" class="btn-ok" :disabled="saving" @click="submitEdit">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getFarmerList, addFarmerToVillage, updateFarmerInVillage, deleteFarmerFromVillage } from '../api/farmer';
import { getVillageInfo } from '../api/user';
import { refreshFarmerContext, useFarmerContext } from '../composables/useFarmerContext';

const { isChief } = useFarmerContext();

const allowed = ref(false);
const farmers = ref<any[]>([]);
const villageId = ref<number | null>(null);
const saving = ref(false);
const showAdd = ref(false);
const addForm = ref({ farmName: '', phone: '', type: 1 });

const editRow = ref<any>(null);
const editForm = ref({ username: '', phone: '', farmName: '', type: 1 });

const farmerTypeText = (t: number) => {
  const m: Record<number, string> = { 1: '民宿经营者', 2: '农产品销售者', 3: '文旅服务者' };
  return m[t] || '—';
};

const load = async () => {
  await refreshFarmerContext();
  if (!isChief.value) {
    allowed.value = false;
    return;
  }
  allowed.value = true;
  const v = await getVillageInfo();
  villageId.value = (v.data as { id?: number })?.id ?? null;
  const res = (await getFarmerList()) as { data?: any[] };
  farmers.value = res.data || [];
};

const openAdd = () => {
  addForm.value = { farmName: '', phone: '', type: 1 };
  showAdd.value = true;
};

const submitAdd = async () => {
  if (!villageId.value || !addForm.value.phone || !addForm.value.farmName) {
    ElMessage.warning('请填写姓名与手机号');
    return;
  }
  saving.value = true;
  try {
    await addFarmerToVillage(villageId.value, {
      farmName: addForm.value.farmName,
      phone: addForm.value.phone,
      type: addForm.value.type
    });
    ElMessage.success('已添加');
    showAdd.value = false;
    await load();
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '添加失败');
  } finally {
    saving.value = false;
  }
};

const openEdit = (row: any) => {
  editRow.value = row;
  editForm.value = {
    username: row.username || '',
    phone: row.phone || '',
    farmName: row.farmName || '',
    type: row.type ?? 1
  };
};

const submitEdit = async () => {
  if (!editRow.value) return;
  const uid = editRow.value.id as number;
  saving.value = true;
  try {
    await updateFarmerInVillage(uid, {
      username: editForm.value.username,
      phone: editForm.value.phone,
      farmName: editForm.value.farmName,
      type: editForm.value.type
    });
    ElMessage.success('已保存');
    editRow.value = null;
    await load();
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败');
  } finally {
    saving.value = false;
  }
};

const remove = (row: any) => {
  ElMessageBox.confirm('确定将该农户从本村移除？账号将变为普通用户。', '确认', {
    type: 'warning'
  })
    .then(async () => {
      try {
        await deleteFarmerFromVillage(row.id as number);
        ElMessage.success('已移除');
        await load();
      } catch (e: unknown) {
        ElMessage.error(e instanceof Error ? e.message : '操作失败');
      }
    })
    .catch(() => {});
};

onMounted(() => {
  load();
});
</script>

<style scoped>
.vf-page {
  min-height: 100vh;
  padding-bottom: 88px;
  background: #f5f5f5;
}

.page-header {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  padding: 16px;
  color: #fff;
}

.header-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
}

.hint {
  margin: 0;
  font-size: 12px;
  opacity: 0.95;
}

.deny {
  padding: 32px 20px;
  text-align: center;
  color: #666;
}

.link {
  display: inline-block;
  margin-top: 12px;
  color: #11998e;
}

.content {
  padding: 16px;
}

.btn-add {
  width: 100%;
  padding: 12px;
  margin-bottom: 16px;
  border: none;
  border-radius: 8px;
  background: #11998e;
  color: #fff;
  font-size: 15px;
  cursor: pointer;
}

.empty {
  text-align: center;
  color: #999;
  padding: 24px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.name {
  font-weight: 600;
  color: #333;
}

.sub {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
}

.actions {
  display: flex;
  gap: 8px;
}

.btn-sm {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
}

.btn-sm.danger {
  color: #c62828;
  border-color: #ffcdd2;
}

.modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 2000;
  padding: 16px;
}

.modal-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 12px 12px 0 0;
  padding: 20px;
}

.modal-card h3 {
  margin: 0 0 12px;
}

.modal-card label {
  display: block;
  font-size: 13px;
  color: #666;
  margin: 10px 0 4px;
}

.inp {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 15px;
  box-sizing: border-box;
}

.modal-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.btn-cancel,
.btn-ok {
  flex: 1;
  padding: 10px;
  border-radius: 8px;
  border: none;
  font-size: 15px;
  cursor: pointer;
}

.btn-cancel {
  background: #eee;
}

.btn-ok {
  background: #11998e;
  color: #fff;
}
</style>
