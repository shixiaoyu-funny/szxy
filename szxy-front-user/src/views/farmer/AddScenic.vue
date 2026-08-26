<template>
  <div class="add-scenic-container">
    <div class="form-container">
      <div class="form-group">
        <label class="form-label">景点名称</label>
        <input v-model="form.name" class="input" placeholder="请输入景点名称" />
      </div>
      <div class="form-group">
        <label class="form-label">景点介绍</label>
        <textarea v-model="form.intro" class="input textarea" placeholder="请输入景点介绍" rows="4"></textarea>
      </div>
      <div class="form-group">
        <label class="form-label">门票价格（元，0免费）</label>
        <input v-model.number="form.price" class="input" type="number" placeholder="0" />
      </div>
      <div class="form-group">
        <label class="form-label">景点类型</label>
        <select v-model="form.type" class="input">
          <option :value="1">自然景观</option>
          <option :value="2">人文景观</option>
          <option :value="3">娱乐体验</option>
          <option :value="4">民俗体验</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">是否提供住宿</label>
        <select v-model.number="form.hasAccommodation" class="input">
          <option :value="0">否</option>
          <option :value="1">是</option>
        </select>
      </div>
      <div class="form-group" v-if="form.hasAccommodation === 1">
        <label class="form-label">住宿详情</label>
        <input v-model="form.accommodationInfo" class="input" placeholder="房型/价格/联系方式等" />
      </div>
      <div class="form-group">
        <label class="form-label">图片URL</label>
        <input v-model="form.image" class="input" placeholder="图片地址（多张用逗号分隔）" />
      </div>

      <button class="btn btn-primary submit-btn" @click="submit" :disabled="loading">
        {{ loading ? '提交中...' : '提交' }}
      </button>

      <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { registerScenic } from '../../api/scenic';
import { getMyVillage } from '../../api/farmer';

const router = useRouter();
const loading = ref(false);
const errorMessage = ref('');
const villageId = ref<number | null>(null);

const form = reactive({
  name: '',
  intro: '',
  price: 0,
  type: 1,
  hasAccommodation: 0,
  accommodationInfo: '',
  image: ''
});

const goBack = () => router.back();

const loadVillage = async () => {
  try {
    const res = await getMyVillage();
    villageId.value = res.data?.id;
    if (!villageId.value) {
      errorMessage.value = '您还没有所属村落，无法新增景点';
    }
  } catch (e: any) {
    errorMessage.value = e?.response?.data?.message || '获取所属村失败';
  }
};

const submit = async () => {
  if (!villageId.value) {
    errorMessage.value = '您还没有所属村落，无法新增景点';
    return;
  }
  if (!form.name) {
    errorMessage.value = '请输入景点名称';
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  try {
    await registerScenic({
      villageId: villageId.value,
      name: form.name,
      intro: form.intro,
      price: form.price || 0,
      type: form.type,
      hasAccommodation: form.hasAccommodation,
      accommodationInfo: form.accommodationInfo,
      image: form.image
    });
    ElMessage.success('景点新增成功');
    router.back();
  } catch (e: any) {
    errorMessage.value = e?.response?.data?.message || '新增失败，请重试';
  } finally {
    loading.value = false;
  }
};

onMounted(loadVillage);
</script>

<style scoped>
.add-scenic-container {
  min-height: 100vh;
  background: transparent;
}

.my-header {
  background: linear-gradient(-45deg, #11998e, #38ef7d, #11998e, #38ef7d);
  padding: 16px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.back-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #fff;
  cursor: pointer;
  padding: 0 8px 0 0;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  margin: 0;
}

.form-container {
  padding: 16px;
  background: white;
  border-radius: 12px;
  margin: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 14px;
  color: #333;
  margin-bottom: 6px;
}

.input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 15px;
  box-sizing: border-box;
  outline: none;
}

.input:focus {
  border-color: #8BC34A;
}

.textarea {
  resize: vertical;
}

.submit-btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
}

.btn-primary {
  background: linear-gradient(135deg, #11998e, #38ef7d);
  color: #fff;
  cursor: pointer;
}

.error-message {
  margin-top: 12px;
  color: #f44336;
  font-size: 14px;
  text-align: center;
}
</style>
