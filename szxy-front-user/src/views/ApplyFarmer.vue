<template>
  <div class="apply-page">
    <div class="apply-card">
      <h2 class="apply-title">申请成为农户</h2>
      <p class="apply-hint">基础信息不可修改；请补充身份证、所属农村与经营类型后提交。</p>

      <form class="apply-form" @submit.prevent="onSubmit">
        <div class="avatar-row">
          <img :src="form.avatar || defaultAvatar" alt="头像" class="avatar" />
        </div>

        <div class="form-item">
          <label>用户名</label>
          <input v-model="form.username" type="text" disabled />
        </div>
        <div class="form-item">
          <label>手机号</label>
          <input v-model="form.phone" type="text" disabled />
        </div>
        <div class="form-item">
          <label>邮箱</label>
          <input v-model="form.email" type="text" disabled />
        </div>
        <div class="form-item">
          <label>当前角色</label>
          <input value="游客" type="text" disabled />
        </div>

        <div class="form-item">
          <label>身份证号 <span class="req">*</span></label>
          <input v-model="form.idCard" type="text" maxlength="18" placeholder="请输入身份证号" :disabled="saving" />
        </div>
        <div class="form-item">
          <label>所属农村 <span class="req">*</span></label>
          <select v-model="form.villageId" :disabled="saving || villageLoading">
            <option :value="undefined" disabled>请选择村落</option>
            <option v-for="v in villages" :key="v.id" :value="v.id">{{ v.name }}</option>
          </select>
        </div>
        <div class="form-item">
          <label>经营类型 <span class="req">*</span></label>
          <select v-model="form.businessType" :disabled="saving">
            <option :value="undefined" disabled>请选择经营类型</option>
            <option :value="1">民宿经营者</option>
            <option :value="2">农产品销售者</option>
            <option :value="3">文旅服务者</option>
          </select>
        </div>

        <div class="form-actions">
          <button type="button" class="btn-cancel" :disabled="saving" @click="goBack">取消</button>
          <button type="submit" class="btn-submit" :disabled="saving">
            {{ saving ? '提交中...' : (isModify ? '重新提交' : '提交申请') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getUserInfo } from '../api/user';
import { getVillageList } from '../api/village';
import { applyFarmerAccess, getMyFarmerAccess } from '../api/farmerAccess';
import { useUserStore } from '../stores/user';

const router = useRouter();
const userStore = useUserStore();
const defaultAvatar =
  'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=default%20user%20avatar%20simple&image_size=square';

const saving = ref(false);
const villageLoading = ref(false);
const isModify = ref(false);
const villages = ref<{ id: number; name: string }[]>([]);

const form = reactive({
  username: '',
  phone: '',
  email: '',
  avatar: '',
  idCard: '',
  villageId: undefined as number | undefined,
  businessType: undefined as number | undefined
});

const role = computed(() => Number((userStore.userInfo as Record<string, unknown>)?.role ?? 0));

const goBack = () => router.push('/profile');

const fillReadonlyFromUser = (u: Record<string, unknown>) => {
  form.username = String(u.username ?? '');
  form.phone = String(u.phone ?? '');
  form.email = String(u.email ?? '');
  form.avatar = String(u.avatar ?? '');
};

const loadVillages = async () => {
  villageLoading.value = true;
  try {
    const res = (await getVillageList(1, 200)) as { data?: { data?: unknown[] } | unknown[]; };
    const raw = res.data;
    const list = Array.isArray(raw)
      ? raw
      : ((raw as { data?: unknown[] })?.data ?? []);
    villages.value = (list as Record<string, unknown>[]).map((v) => ({
      id: Number(v.id),
      name: String(v.name ?? '')
    }));
  } catch (e) {
    console.error(e);
    ElMessage.error('加载村落列表失败');
  } finally {
    villageLoading.value = false;
  }
};

const loadPage = async () => {
  try {
    const infoRes = await getUserInfo();
    if (infoRes.data) {
      if (Number(infoRes.data.role) !== 1) {
        ElMessage.warning('仅游客可申请成为农户');
        router.replace('/profile');
        return;
      }
      userStore.setUserInfo(infoRes.data);
      fillReadonlyFromUser(infoRes.data as Record<string, unknown>);
    }

    const mineRes = await getMyFarmerAccess();
    const mine = mineRes.data as Record<string, unknown> | null;
    if (mine) {
      const status = Number(mine.status);
      if (status === 1) {
        ElMessage.info('您已通过农户审批');
        router.replace('/profile');
        return;
      }
      if (status === 0) {
        isModify.value = true;
      }
      const cache = (mine.userCacheInfo || mine.user_cache_info) as Record<string, unknown> | undefined;
      if (cache) {
        fillReadonlyFromUser(cache);
      }
      form.idCard = String(mine.idCard ?? mine.id_card ?? '');
      form.villageId = Number(mine.villageId ?? mine.village_id) || undefined;
      const bt = mine.businessType ?? mine.business_type;
      form.businessType = bt == null ? undefined : Number(bt);
    }
  } catch (e) {
    console.error(e);
  }
};

const onSubmit = async () => {
  if (!form.idCard.trim()) {
    ElMessage.warning('请填写身份证号');
    return;
  }
  if (form.villageId == null) {
    ElMessage.warning('请选择所属农村');
    return;
  }
  if (form.businessType == null) {
    ElMessage.warning('请选择经营类型');
    return;
  }
  saving.value = true;
  try {
    await applyFarmerAccess({
      idCard: form.idCard.trim(),
      villageId: form.villageId,
      businessType: form.businessType
    });
    ElMessage.success(isModify.value ? '申请已更新' : '申请已提交，请等待审批');
    router.push('/profile');
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '提交失败');
  } finally {
    saving.value = false;
  }
};

onMounted(async () => {
  if (role.value && role.value !== 1) {
    ElMessage.warning('仅游客可申请成为农户');
    router.replace('/profile');
    return;
  }
  await Promise.all([loadVillages(), loadPage()]);
});
</script>

<style scoped>
.apply-page {
  padding: 24px 16px 40px;
  display: flex;
  justify-content: center;
}

.apply-card {
  width: 100%;
  max-width: 560px;
  background: #fff;
  border-radius: 12px;
  padding: 28px 24px;
  box-shadow: 0 8px 24px rgba(139, 195, 74, 0.15);
  border-left: 4px solid #8bc34a;
}

.apply-title {
  margin: 0 0 8px;
  font-size: 20px;
  color: #2d331f;
}

.apply-hint {
  margin: 0 0 20px;
  font-size: 13px;
  color: #888;
}

.avatar-row {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #8bc34a;
}

.apply-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-item label {
  font-size: 13px;
  color: #666;
}

.req {
  color: #e53935;
}

.form-item input,
.form-item select {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
}

.form-item input:disabled,
.form-item select:disabled {
  background: #f3f4f6;
  color: #9aa0a6;
  cursor: not-allowed;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.btn-cancel,
.btn-submit {
  flex: 1;
  height: 42px;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.btn-cancel {
  background: #f0f0f0;
  color: #666;
}

.btn-submit {
  background: linear-gradient(135deg, #8bc34a 0%, #66bb6a 100%);
  color: #fff;
}

.btn-submit:disabled,
.btn-cancel:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
