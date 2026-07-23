<template>
  <div class="village-manage-container">
    <header class="page-header">
      <h1 class="header-title">农村管理</h1>
    </header>
    <div class="content">
      <div v-if="!villageInfo" class="apply-section">
        <h2 class="section-title">申请正式农户资质</h2>
        <form class="apply-form" @submit.prevent="submitApply">
          <div class="form-group">
            <label class="form-label">农村名称</label>
            <input type="text" v-model="applyForm.villageName" class="form-input" placeholder="与系统备案名称一致，如：竹筏水乡村；也可填村落编号" required />
          </div>
          <div class="form-group">
            <label class="form-label">农户姓名</label>
            <input type="text" v-model="applyForm.farmName" class="form-input" placeholder="请输入农户真实姓名" required />
          </div>
          <div class="form-group">
            <label class="form-label">身份证号码</label>
            <input type="text" v-model="applyForm.idCard" class="form-input" placeholder="请输入身份证号码" required />
          </div>
          <div class="form-group">
            <label class="form-label">农户类型</label>
            <select v-model.number="applyForm.type" class="form-select" required>
              <option :value="0">请选择农户类型</option>
              <option :value="1">民宿经营者</option>
              <option :value="2">农产品销售者</option>
              <option :value="3">文旅服务者</option>
            </select>
          </div>
          <button type="submit" class="btn btn-primary submit-btn" :disabled="submitLoading">
            {{ submitLoading ? '提交中...' : '提交申请' }}
          </button>
        </form>
      </div>
      <div v-else class="village-info">
        <div v-if="showChiefApplyForm" class="chief-apply card-block">
          <h2 class="section-title">村长资质申请</h2>
          <p class="tip">正式农户可申请本村村长资质，提交后由管理端审核。</p>
          <form class="apply-form compact" @submit.prevent="submitChiefApply">
            <div class="form-group">
              <label class="form-label">真实姓名</label>
              <input v-model="chiefForm.farmName" type="text" class="form-input" required placeholder="与农户信息一致" />
            </div>
            <div class="form-group">
              <label class="form-label">身份证号</label>
              <input v-model="chiefForm.idCard" type="text" class="form-input" required placeholder="用于资质审核" />
            </div>
            <button type="submit" class="btn btn-primary submit-btn" :disabled="chiefLoading">
              {{ chiefLoading ? '提交中...' : '提交村长资质申请' }}
            </button>
          </form>
        </div>

        <div v-if="isChief" class="card-block manage-link">
          <router-link to="/village-farmers" class="link-btn">本村农户管理</router-link>
        </div>

        <div class="village-card">
          <h2 class="village-name">{{ villageInfo.name || '未命名农村' }}</h2>
          
          <!-- 农村图片 -->
          <div v-if="villageInfo.image" class="village-image">
            <img :src="villageInfo.image.split(',')[0]" :alt="villageInfo.name" class="image" />
          </div>
          
          <div class="village-meta">
            <div class="meta-row">
              <span class="meta-label">村长姓名：</span>
              <span class="meta-value">{{ villageInfo.managerName || '未设置' }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">所在地区：</span>
              <span class="meta-value">{{ villageInfo.province || '' }} {{ villageInfo.city || '' }} {{ villageInfo.county || '' }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">村落类型：</span>
              <span class="meta-value">{{ getVillageTypeText(villageInfo.type) }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">村落介绍：</span>
              <span class="meta-value">{{ villageInfo.intro || '暂无介绍' }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">最佳游玩时间：</span>
              <span class="meta-value">{{ villageInfo.bestTime || '未设置' }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">季节性活动：</span>
              <span class="meta-value">{{ villageInfo.activity || '暂无活动' }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">联系方式：</span>
              <span class="meta-value">{{ villageInfo.contact || '未设置' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { getVillageInfo, applyFarmer } from '../api/user';
import { applyManager } from '../api/farmer';
import { ElLoading, ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { refreshFarmerContext, useFarmerContext } from '../composables/useFarmerContext';
import { normalizeVillageVO } from '../utils/villageApi';

const router = useRouter();
const { isChief } = useFarmerContext();
const error = ref('');
const villageInfo = ref<any>(null);

/** 本村信息里已有村长姓名时，不允许再申请村长资质；仅当村长姓名为空时才展示申请表 */
const showChiefApplyForm = computed(() => {
  if (isChief.value) {
    return false;
  }
  const name = villageInfo.value?.managerName;
  const hasChief = typeof name === 'string' && name.trim().length > 0;
  return !hasChief;
});
const submitLoading = ref(false);
const chiefLoading = ref(false);
const chiefForm = ref({ farmName: '', idCard: '' });
const applyForm = ref({
  villageName: '',
  farmName: '',
  idCard: '',
  type: 0 as number
});

const getVillageTypeText = (type: number) => {
  switch (type) {
    case 1:
      return '古村落';
    case 2:
      return '生态村';
    case 3:
      return '民俗村';
    case 4:
      return '文旅村';
    default:
      return '未设置';
  }
};

const loadVillageInfo = async () => {
  const loadingInstance = ElLoading.service({
    lock: true,
    text: '加载中...',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  
  error.value = '';
  try {
    await refreshFarmerContext();
    const res = await getVillageInfo();
    villageInfo.value = normalizeVillageVO(res.data as Record<string, unknown> | undefined);
  } catch (err: any) {
    error.value = '获取农村信息失败，请重试';
    console.error('获取农村信息失败:', err);
  } finally {
    loadingInstance.close();
  }
};

const submitChiefApply = async () => {
  chiefLoading.value = true;
  try {
    await applyManager({
      farmName: chiefForm.value.farmName,
      idCard: chiefForm.value.idCard
    });
    ElMessage.success('村长资质申请已提交，请等待管理端审核');
    chiefForm.value = { farmName: '', idCard: '' };
    await loadVillageInfo();
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '提交失败');
  } finally {
    chiefLoading.value = false;
  }
};

const submitApply = async () => {
  if (!applyForm.value.type) {
    ElMessage.warning('请选择农户类型');
    return;
  }
  const loadingInstance = ElLoading.service({
    lock: true,
    text: '提交中...',
    background: 'rgba(0, 0, 0, 0.7)',
  });
  
  try {
    await applyFarmer({
      villageName: applyForm.value.villageName.trim(),
      farmName: applyForm.value.farmName.trim(),
      idCard: applyForm.value.idCard.trim(),
      type: applyForm.value.type
    });
    ElMessage.success('申请提交成功，等待审核');
    // 重置表单
    applyForm.value = {
      villageName: '',
      farmName: '',
      idCard: '',
      type: 0
    };
    // 重新加载农村信息
    await loadVillageInfo();
    
    // 延迟刷新页面以更新底部导航栏状态
    setTimeout(() => {
      router.go(0);
    }, 1500);
  } catch (err: any) {
    ElMessage.error(err?.message || '申请提交失败，请重试');
    console.error('提交申请失败:', err);
  } finally {
    loadingInstance.close();
  }
};

onMounted(() => {
  loadVillageInfo();
});
</script>

<style scoped>
.village-manage-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 80px;
}

.page-header {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  padding: 16px;
  text-align: center;
}

.header-title {
  color: white;
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.content {
  padding: 20px;
}

.error {
  text-align: center;
  padding: 40px 0;
  color: #f44336;
}

.error button {
  margin-top: 16px;
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  background: #11998e;
  color: white;
  cursor: pointer;
}

.apply-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 20px 0;
  color: #333;
}

.apply-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.form-input,
.form-select {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
}

.form-input:focus,
.form-select:focus {
  border-color: #11998e;
}

.submit-btn {
  padding: 12px;
  border: none;
  border-radius: 4px;
  background: #11998e;
  color: white;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s;
}

.submit-btn:hover {
  background: #0e857a;
}

.submit-btn:disabled {
  background: #9E9E9E;
  cursor: not-allowed;
}

.card-block {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chief-apply .tip {
  font-size: 13px;
  color: #666;
  margin: 0 0 16px;
}

.compact .form-group {
  margin-bottom: 12px;
}

.manage-link {
  text-align: center;
  padding: 16px;
}

.link-btn {
  display: inline-block;
  padding: 12px 24px;
  background: #11998e;
  color: #fff;
  text-decoration: none;
  border-radius: 8px;
  font-weight: 500;
}

.village-info {
  background: transparent;
  border-radius: 8px;
  padding: 0;
}

.village-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.village-name {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #333;
  text-align: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.village-image {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  margin: 10px 0;
}

.village-image .image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.village-meta {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meta-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.meta-label {
  font-size: 14px;
  font-weight: 500;
  color: #666;
  min-width: 100px;
}

.meta-value {
  font-size: 14px;
  color: #333;
  flex: 1;
  line-height: 1.4;
}
</style>