<template>
  <div class="village-farmers-container">
    <header class="my-header">
      <button class="back-btn" @click="goBack">←</button>
      <h1 class="header-title">本村农户</h1>
      <button class="add-btn" @click="openAdd">＋</button>
    </header>

    <div v-if="loading" class="loading-container">
      <div class="loading"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="error-container">
      <p>{{ error }}</p>
      <button class="btn btn-primary" @click="fetchFarmers">重试</button>
    </div>

    <div v-else-if="farmers.length === 0" class="empty-container">
      <p>本村暂无农户，点击右上角＋新增</p>
    </div>

    <div v-else class="farmers-list">
      <div v-for="item in farmers" :key="item.id" class="farmer-item">
        <div class="farmer-info">
          <h3 class="farmer-name">{{ item.username }}</h3>
          <p class="farmer-phone">{{ item.phone }}</p>
          <p class="farmer-type">{{ businessTypeText(item.business_type) }}</p>
        </div>
        <div class="farmer-actions">
          <button class="btn-edit" @click="openEdit(item)">改</button>
          <button class="btn-del" @click="removeFarmer(item)">删</button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div v-if="dialogVisible" class="dialog-mask" @click.self="dialogVisible = false">
      <div class="dialog">
        <h3 class="dialog-title">{{ editingId ? '修改农户' : '新增农户' }}</h3>
        <div class="form-group">
          <input v-model="form.username" class="input" placeholder="用户名（留空默认手机号）" />
        </div>
        <div class="form-group">
          <input v-model="form.phone" class="input" placeholder="手机号（必填）" />
        </div>
        <div class="form-group">
          <input v-model="form.id_card" class="input" placeholder="身份证号" />
        </div>
        <div class="form-group">
          <select v-model.number="form.business_type" class="input">
            <option :value="1">民宿经营者</option>
            <option :value="2">农产品销售者</option>
            <option :value="3">文旅服务者</option>
          </select>
        </div>
        <div class="dialog-actions">
          <button class="btn btn-cancel" @click="dialogVisible = false">取消</button>
          <button class="btn btn-primary" @click="submit" :disabled="loading">确定</button>
        </div>
        <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getVillageFarmers, addVillageFarmer, updateVillageFarmer, deleteVillageFarmer, getMyVillage } from '../../api/farmer';

interface FarmerVO {
  id: number;
  user_id: number;
  village_id: number;
  username: string;
  phone: string;
  business_type: number;
}

const router = useRouter();
const farmers = ref<FarmerVO[]>([]);
const loading = ref(true);
const error = ref('');
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const villageId = ref<number | null>(null);

const form = reactive({
  username: '',
  phone: '',
  id_card: '',
  business_type: 1
});

const goBack = () => router.back();

const businessTypeText = (type: number) => {
  switch (type) {
    case 1: return '民宿经营者';
    case 2: return '农产品销售者';
    case 3: return '文旅服务者';
    default: return '未知';
  }
};

const openAdd = () => {
  editingId.value = null;
  form.username = '';
  form.phone = '';
  form.id_card = '';
  form.business_type = 1;
  errorMessage.value = '';
  dialogVisible.value = true;
};

const openEdit = (item: FarmerVO) => {
  editingId.value = item.user_id;
  form.username = item.username;
  form.phone = item.phone;
  form.business_type = item.business_type;
  form.id_card = '';
  errorMessage.value = '';
  dialogVisible.value = true;
};

const errorMessage = ref('');

const loadVillage = async () => {
  try {
    const res = await getMyVillage();
    villageId.value = res.data?.id;
  } catch (e) {
    villageId.value = null;
  }
};

const submit = async () => {
  if (!form.phone) {
    errorMessage.value = '手机号不能为空';
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  try {
    const payload = {
      username: form.username,
      phone: form.phone,
      id_card: form.id_card,
      business_type: form.business_type
    };
    if (editingId.value) {
      await updateVillageFarmer(editingId.value, payload);
      ElMessage.success('修改成功');
    } else {
      if (!villageId.value) {
        errorMessage.value = '无法确定所属村';
        return;
      }
      await addVillageFarmer(villageId.value, payload);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    fetchFarmers();
  } catch (e: any) {
    errorMessage.value = e?.response?.data?.message || '操作失败';
  } finally {
    loading.value = false;
  }
};

const removeFarmer = async (item: FarmerVO) => {
  if (!confirm(`确定删除农户「${item.username}」？其账号将降为游客`)) return;
  try {
    await deleteVillageFarmer(item.user_id);
    ElMessage.success('删除成功');
    fetchFarmers();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '删除失败');
  }
};

const fetchFarmers = async () => {
  loading.value = true;
  error.value = '';
  try {
    const res = await getVillageFarmers();
    farmers.value = res.data;
  } catch (e: any) {
    error.value = e?.response?.data?.message || '获取失败';
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadVillage();
  fetchFarmers();
});
</script>

<style scoped>
.village-farmers-container {
  min-height: 100vh;
  background: transparent;
}

.my-header {
  background: linear-gradient(-45deg, #11998e, #38ef7d, #11998e, #38ef7d);
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.back-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #fff;
  cursor: pointer;
}

.add-btn {
  background: rgba(255, 255, 255, 0.25);
  border: none;
  color: #fff;
  font-size: 22px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  margin: 0;
}

.farmers-list {
  padding: 16px;
}

.farmer-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: white;
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 12px;
}

.farmer-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.farmer-phone {
  font-size: 13px;
  color: #999;
  margin-bottom: 4px;
}

.farmer-type {
  font-size: 12px;
  color: #8BC34A;
}

.farmer-actions {
  display: flex;
  gap: 8px;
}

.btn-edit,
.btn-del {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.btn-edit {
  background: #e8f5e9;
  color: #2E7D32;
}

.btn-del {
  background: #ffebee;
  color: #f44336;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  width: 90%;
  max-width: 360px;
  background: white;
  border-radius: 12px;
  padding: 20px;
}

.dialog-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.form-group {
  margin-bottom: 12px;
}

.input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
  outline: none;
}

.input:focus {
  border-color: #8BC34A;
}

.dialog-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.btn {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.btn-primary {
  background: linear-gradient(135deg, #11998e, #38ef7d);
  color: #fff;
}

.btn-cancel {
  background: #f0f0f0;
  color: #333;
}

.error-message {
  margin-top: 12px;
  color: #f44336;
  font-size: 13px;
  text-align: center;
}

.loading-container,
.error-container,
.empty-container {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}
</style>
