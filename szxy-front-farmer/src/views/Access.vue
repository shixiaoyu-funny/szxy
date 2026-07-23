<template>
  <div class="access-container">
    <header class="page-header">
      <h1 class="header-title">景点上报</h1>
      <p class="sub">私人景点（农户）/ 公共景点（村长）</p>
    </header>

    <div class="tabs">
      <button
        type="button"
        :class="['tab', { active: kind === 'private' }]"
        @click="setKind('private')"
      >
        农家乐 / 私人景点
      </button>
      <button
        type="button"
        :class="['tab', { active: kind === 'public' }]"
        :disabled="!isChief"
        @click="setKind('public')"
      >
        景点（公共）
      </button>
    </div>
    <p v-if="kind === 'public' && !isChief" class="warn">公共景点仅村长可申请，请先获得村长资质。</p>

    <div class="content">
      <form class="apply-form" @submit.prevent="submitForm">
        <div class="form-group">
          <label class="form-label">景区名称</label>
          <input v-model="formData.name" type="text" class="form-input" required placeholder="请输入景区名称" />
        </div>

        <div class="form-group">
          <label class="form-label">景区介绍</label>
          <textarea v-model="formData.intro" class="form-textarea" rows="4" required placeholder="详细介绍"></textarea>
        </div>

        <div class="form-group">
          <label class="form-label">景区图片</label>
          <input v-model="formData.image" type="text" class="form-input" placeholder="上传后自动填入 URL，或多张逗号分隔" />
          <input ref="fileRef" type="file" accept="image/*" class="file" @change="onFile" />
          <button type="button" class="btn-upload" @click="fileRef?.click()">上传图片</button>
        </div>

        <div class="form-group">
          <label class="form-label">门票价格（元，0 为免费）</label>
          <input v-model.number="formData.price" type="number" min="0" class="form-input" required />
        </div>

        <div class="form-group">
          <span class="form-label">景区类型</span>
          <div class="radio-group">
            <label v-for="opt in scenicTypes" :key="opt.v" class="radio-item">
              <input v-model.number="formData.type" type="radio" :value="opt.v" required />
              <span>{{ opt.l }}</span>
            </label>
          </div>
        </div>

        <div class="form-group">
          <span class="form-label">是否提供住宿</span>
          <div class="radio-group">
            <label class="radio-item">
              <input v-model.number="formData.hasAccommodation" type="radio" :value="0" />
              <span>否</span>
            </label>
            <label class="radio-item">
              <input v-model.number="formData.hasAccommodation" type="radio" :value="1" />
              <span>是</span>
            </label>
          </div>
        </div>

        <div v-if="formData.hasAccommodation === 1" class="form-group">
          <label class="form-label">住宿信息</label>
          <textarea v-model="formData.accommodationInfo" class="form-textarea" rows="3" placeholder="房型、价格、联系方式等"></textarea>
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="submitting || (kind === 'public' && !isChief)">
            {{ submitting ? '提交中...' : '提交申请' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { registerPrivateScenic, registerPublicScenic } from '../api/scenic';
import { getVillageInfo } from '../api/user';
import { uploadFile } from '../api/upload';
import { useFarmerContext } from '../composables/useFarmerContext';

const route = useRoute();
const router = useRouter();
const { isChief, refreshFarmerContext } = useFarmerContext();

const kind = ref<'private' | 'public'>('private');
const submitting = ref(false);
const fileRef = ref<HTMLInputElement | null>(null);
const villageId = ref<number | null>(null);

const scenicTypes = [
  { v: 1, l: '自然景观' },
  { v: 2, l: '人文景观' },
  { v: 3, l: '娱乐体验' },
  { v: 4, l: '民俗体验' }
];

const formData = ref({
  name: '',
  intro: '',
  image: '',
  price: 0,
  type: 1,
  hasAccommodation: 0 as number,
  accommodationInfo: ''
});

const setKind = (k: 'private' | 'public') => {
  if (k === 'public' && !isChief.value) {
    ElMessage.warning('仅村长可上报公共景点');
    return;
  }
  kind.value = k;
  router.replace({ query: { ...route.query, kind: k } });
};

const syncRouteQuery = () => {
  const q = route.query.kind as string | undefined;
  if (q === 'public' && isChief.value) {
    kind.value = 'public';
  } else {
    kind.value = 'private';
  }
};

watch(
  () => route.query.kind,
  () => syncRouteQuery()
);

onMounted(async () => {
  await refreshFarmerContext();
  try {
    const res = await getVillageInfo();
    const d = res.data as { id?: number } | null;
    villageId.value = d?.id ?? null;
  } catch {
    villageId.value = null;
  }
  syncRouteQuery();
});

const onFile = async (e: Event) => {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  try {
    const res = (await uploadFile(file)) as { data?: string; url?: string };
    let url = res?.data || res?.url || '';
    if (url && !url.startsWith('http')) {
      url = `${window.location.origin}${url.startsWith('/') ? '' : '/'}${url}`;
    }
    if (url) {
      formData.value.image = formData.value.image ? `${formData.value.image},${url}` : url;
      ElMessage.success('已上传');
    }
  } catch {
    ElMessage.error('上传失败');
  } finally {
    input.value = '';
  }
};

const submitForm = async () => {
  if (!villageId.value) {
    ElMessage.error('未获取到所属村落，请先完成农户资质并通过审核');
    return;
  }
  if (kind.value === 'public' && !isChief.value) {
    ElMessage.warning('仅村长可上报公共景点');
    return;
  }

  submitting.value = true;
  try {
    const body: Record<string, unknown> = {
      villageName: villageId.value,
      name: formData.value.name,
      intro: formData.value.intro,
      image: formData.value.image,
      price: formData.value.price,
      type: formData.value.type,
      hasAccommodation: formData.value.hasAccommodation,
      accommodationInfo: formData.value.accommodationInfo || ''
    };

    if (kind.value === 'private') {
      await registerPrivateScenic(body);
    } else {
      await registerPublicScenic(body);
    }
    ElMessage.success('申请已提交，请等待管理端审核');
    router.push('/attraction-manage');
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '提交失败');
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.access-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 88px;
}

.page-header {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  padding: 16px;
  text-align: center;
  color: #fff;
}

.header-title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
}

.sub {
  margin: 0;
  font-size: 12px;
  opacity: 0.95;
}

.tabs {
  display: flex;
  gap: 0;
  margin: 0;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.tab {
  flex: 1;
  padding: 12px;
  border: none;
  background: #fafafa;
  font-size: 14px;
  cursor: pointer;
}

.tab.active {
  background: #fff;
  font-weight: 600;
  color: #11998e;
  border-bottom: 2px solid #11998e;
}

.tab:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.warn {
  margin: 0;
  padding: 10px 16px;
  font-size: 13px;
  color: #c62828;
  background: #ffebee;
}

.content {
  padding: 20px;
  max-width: 560px;
  margin: 0 auto;
}

.apply-form {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.form-group {
  margin-bottom: 18px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-textarea {
  resize: vertical;
}

.file {
  display: none;
}

.btn-upload {
  margin-top: 8px;
  padding: 8px 14px;
  font-size: 13px;
  border: 1px solid #11998e;
  background: #fff;
  color: #11998e;
  border-radius: 6px;
  cursor: pointer;
}

.radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  cursor: pointer;
}

.form-actions {
  margin-top: 8px;
}

.btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
}

.btn-primary {
  background: #11998e;
  color: #fff;
}

.btn-primary:disabled {
  background: #b0c4c1;
  cursor: not-allowed;
}
</style>
