<template>
  <div class="my-village-container">
    <header class="my-header">
      <button class="back-btn" @click="goBack">←</button>
      <h1 class="header-title">我的村</h1>
    </header>

    <div v-if="loading" class="loading-container">
      <div class="loading"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="error-container">
      <p>{{ error }}</p>
      <button class="btn btn-primary" @click="fetchVillage">重试</button>
    </div>

    <div v-else-if="village" class="village-card">
      <div class="village-image">
        <img :src="getMainImage(village.image)" :alt="village.name" />
      </div>
      <div class="village-info">
        <h3 class="village-name">{{ village.name }}</h3>
        <p class="village-location">{{ village.province }} {{ village.city }} {{ village.county }}</p>
        <p class="village-desc">{{ village.intro }}</p>
        <div class="feature-item" v-if="village.manager_name">
          <span class="feature-label">村长：</span>
          <span class="feature-value">{{ village.manager_name }}</span>
        </div>
        <div class="feature-item" v-if="village.best_time">
          <span class="feature-label">最佳游玩时间：</span>
          <span class="feature-value">{{ village.best_time }}</span>
        </div>
        <div class="feature-item" v-if="village.activity">
          <span class="feature-label">特色活动：</span>
          <span class="feature-value">{{ village.activity }}</span>
        </div>
        <div class="feature-item" v-if="village.contact">
          <span class="feature-label">联系电话：</span>
          <span class="feature-value">{{ village.contact }}</span>
        </div>
        <div class="action-bar">
          <button class="btn btn-primary" @click="addScenic">新增景点</button>
          <button class="btn btn-secondary" @click="myScenics">我的景点</button>
        </div>
      </div>
    </div>

    <div v-else class="empty-container">
      <p>您还没有所属村落</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getMyVillage } from '../../api/farmer';

interface VillageVO {
  id: number;
  name: string;
  province: string;
  city: string;
  county: string;
  intro: string;
  image: string;
  best_time: string;
  activity: string;
  contact: string;
  manager_name: string;
}

const router = useRouter();
const village = ref<VillageVO | null>(null);
const loading = ref(true);
const error = ref('');

const goBack = () => {
  router.back();
};

const getMainImage = (imageStr: string | undefined) => {
  if (!imageStr) return '';
  return imageStr.split(',')[0];
};

const addScenic = () => {
  router.push('/farmer/scenic/add');
};

const myScenics = () => {
  router.push('/farmer/scenics');
};

const fetchVillage = async () => {
  loading.value = true;
  error.value = '';
  try {
    const res = await getMyVillage();
    village.value = res.data;
  } catch (e: any) {
    error.value = e?.response?.data?.message || '获取失败';
  } finally {
    loading.value = false;
  }
};

onMounted(fetchVillage);
</script>

<style scoped>
.my-village-container {
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

.village-card {
  margin: 16px;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.village-image {
  width: 100%;
  height: 220px;
}

.village-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.village-info {
  padding: 16px;
}

.village-name {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
}

.village-location {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
}

.village-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 12px;
}

.feature-item {
  font-size: 14px;
  color: #666;
  margin-bottom: 6px;
}

.feature-label {
  color: #8BC34A;
}

.feature-value {
  color: #333;
}

.action-bar {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.btn {
  flex: 1;
  padding: 12px;
  border-radius: 8px;
  border: none;
  font-size: 15px;
  cursor: pointer;
}

.btn-primary {
  background: linear-gradient(135deg, #11998e, #38ef7d);
  color: #fff;
}

.btn-secondary {
  background: #f0f0f0;
  color: #333;
}

.loading-container,
.error-container,
.empty-container {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}
</style>
